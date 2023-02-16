// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.philwo.kunoichi.ninja;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.philwo.kunoichi.datamodel.Action;
import de.philwo.kunoichi.datamodel.Environment;
import de.philwo.kunoichi.datamodel.Rules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NinjaFileParserTest {

  private Path tempDir;

  @BeforeEach
  void setUp() throws IOException {
    this.tempDir = Files.createTempDirectory("kunoichi");
  }

  @AfterEach
  void tearDown() throws IOException {
    Files.walk(this.tempDir)
        .sorted(Comparator.reverseOrder())
        .forEach(
            path -> {
              try {
                Files.delete(path);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  @Test
  void testParseRule() throws IOException {
    Files.writeString(
        tempDir.resolve("build.ninja"),
        """
                    rule cc
                      command = clang -c $in -o $out
                      description = CC $out
                    """);
    NinjaFile ninjaFile = NinjaFile.parse(tempDir.resolve("build.ninja"));
    assertEquals("clang -c ${in} -o ${out}", ninjaFile.rules().get("cc").command().toString());
    assertEquals("CC ${out}", ninjaFile.rules().get("cc").description().toString());
  }

  @Test
  void testParseBuildTarget() throws IOException {
    Files.writeString(
        tempDir.resolve("build.ninja"),
        """
                    rule cc
                      command = clang -c $in -o $out
                      description = CC $out
                    build foo.o: cc foo.c
                    """);
    NinjaFile ninjaFile = NinjaFile.parse(tempDir.resolve("build.ninja"));
    Action buildFooO = ninjaFile.actions().get(0);
    assertEquals("cc", buildFooO.mnemonic());
    assertEquals(List.of(Path.of("foo.c")), buildFooO.inputs().toList());
    assertEquals(List.of(Path.of("foo.o")), buildFooO.outputs().toList());
  }

  @Test
  void testParseDefaultTarget() throws IOException {
    Files.writeString(
        tempDir.resolve("build.ninja"),
        """
                    rule cc
                      command = clang -c $in -o $out
                      description = CC $out
                    build foo.o: cc foo.c
                    default foo.o
                    """);
    NinjaFile ninjaFile = NinjaFile.parse(tempDir.resolve("build.ninja"));
    assertEquals(1, ninjaFile.defaultTargets().items().size());
    assertEquals(List.of(Path.of("foo.o")), ninjaFile.defaultTargets().items().stream().toList());
  }

  @Test
  void testParseVariableAssignment() throws IOException {
    Files.writeString(
        tempDir.resolve("build.ninja"),
        """
                    cc = clang
                    cflags = -O3 -funroll-loops
                    cxxflags = $cflags -std=c++11
                    """);
    NinjaFile ninjaFile = NinjaFile.parse(tempDir.resolve("build.ninja"));
    assertEquals("clang", ninjaFile.env().get("cc").toString());
    assertEquals("-O3 -funroll-loops", ninjaFile.env().get("cflags").toString());
    assertEquals("-O3 -funroll-loops -std=c++11", ninjaFile.env().get("cxxflags").toString());
  }

  @Test
  void testParse() throws IOException {
    Files.writeString(
        tempDir.resolve("cc_toolchain.ninja"),
        """
        cc = clang
        cflags = -O3 -funroll-loops
        pool link_pool
          depth = 1
        rule cc
          command = $cc $cflags -c $in -o $out
          description = CC $out
        rule link
          command = $cc $in -o $out
          description = LINK $out
          pool = link_pool
        """);
    Files.writeString(
        tempDir.resolve("build_foobar.ninja"),
        """
        # foobar does not support building with -O3
        cflags = -O2
        build foo.o: cc foo.c
        build bar.o: cc bar.c
        # compile an experimental super-optimized version of bar
        build bar_turbo.o: cc bar.c
          cflags = -O9 -ffast-math -fomit-frame-pointer
        build foobar: link foo.o bar.o
        build foobar_turbo: link foo.o bar_turbo.o
        default foobar foobar_turbo
        """);
    Files.writeString(
        tempDir.resolve("build.ninja"),
        """
        include cc_toolchain.ninja
        # We use subninja so that foobar's cflags don't override our highly optimized ones.
        subninja build_foobar.ninja
        build baz.o: cc baz.c
        build baz: link baz.o
        default baz
        """);
    NinjaFile ninjaFile = NinjaFile.parse(tempDir.resolve("build.ninja"));

    Environment env = ninjaFile.env();
    // cc = clang
    assertEquals("clang", env.get("cc").toString());
    // cflags = -O3 -funroll-loops
    assertEquals("-O3 -funroll-loops", env.get("cflags").toString());

    Rules rules = ninjaFile.rules();
    // rule cc
    //  command = $cc $cflags -c $in -o $out
    //  description = CC $out
    assertEquals("${cc} ${cflags} -c ${in} -o ${out}", rules.get("cc").command().toString());
    assertEquals("CC ${out}", rules.get("cc").description().toString());
    // rule link
    //   command = $cc $in -o $out
    //   description = LINK $out
    //   pool = link_pool
    assertEquals("${cc} ${in} -o ${out}", rules.get("link").command().toString());
    assertEquals("LINK ${out}", rules.get("link").description().toString());
    assertEquals("link_pool", rules.get("link").pool().toString());

    Iterator<Action> targets = ninjaFile.actions().stream().iterator();
    // build foo.o: cc foo.c
    Action compileFoo = targets.next();
    assertEquals("clang -O2 -c foo.c -o foo.o", compileFoo.command());
    // build bar.o: cc bar.c
    Action compileBar = targets.next();
    assertEquals("clang -O2 -c bar.c -o bar.o", compileBar.command());
    // build bar_turbo.o: cc bar.c
    //     cflags = -O9 -ffast-math -fomit-frame-pointer
    Action compileBarTurbo = targets.next();
    assertEquals(
        "clang -O9 -ffast-math -fomit-frame-pointer -c bar.c -o bar_turbo.o",
        compileBarTurbo.command());
    // build foobar: link foo.o bar.o
    Action linkFooBar = targets.next();
    assertEquals("clang foo.o bar.o -o foobar", linkFooBar.command());
    // build foobar_turbo: link foo.o bar_turbo.o
    Action linkFooBarTurbo = targets.next();
    assertEquals("clang foo.o bar_turbo.o -o foobar_turbo", linkFooBarTurbo.command());
    // build baz.o: cc baz.c
    Action compileBaz = targets.next();
    assertEquals("clang -O3 -funroll-loops -c baz.c -o baz.o", compileBaz.command());
    // build baz: link baz.o
    Action linkBaz = targets.next();
    assertEquals("clang baz.o -o baz", linkBaz.command());
    // default foobar foobar_turbo
    // default baz
    assertEquals(3, ninjaFile.defaultTargets().items().size());
    assertEquals(
        List.of(Path.of("foobar"), Path.of("foobar_turbo"), Path.of("baz")),
        ninjaFile.defaultTargets().items());
  }
}

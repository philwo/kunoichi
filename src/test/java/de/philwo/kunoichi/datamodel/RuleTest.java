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

package de.philwo.kunoichi.datamodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.philwo.kunoichi.datamodel.Rule.RuleBuilder;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RuleTest {

  private RuleBuilder builder;

  @BeforeEach
  void setUp() throws IOException {
    this.builder = Rule.builder().name("cc").command(FormatString.fromPlainText("clang"));
  }

  @Test
  void testName() {
    assertEquals("cc", builder.build().name());
    assertThrows(NullPointerException.class, () -> builder.name(null));
  }

  @Test
  void testCommand() {
    assertEquals("clang", builder.build().command().toString());
    assertThrows(NullPointerException.class, () -> builder.command(null));
  }

  @Test
  void testDepfile() {
    builder.depFile(FormatString.fromPlainText("foo"));
    assertEquals("foo", builder.build().depFile().toString());
    assertThrows(NullPointerException.class, () -> builder.depFile(null));
  }

  @Test
  void testDeps() throws IOException {
    //    assertThat(builder.build().deps()).isEqualTo(Deps.NONE);
    //    builder.deps(FormatString.fromPlainText("gcc"));
    //    assertThat(builder.build().deps()).isEqualTo(Deps.GCC);
    //    builder.deps(FormatString.fromPlainText("msvc"));
    //    assertThat(builder.build().deps()).isEqualTo(Deps.MSVC);
    //    assertThrows(
    //        IllegalArgumentException.class, () ->
    // builder.deps(FormatString.fromPlainText("foo")));
    //    assertThrows(NullPointerException.class, () -> builder.deps(null));
  }

  @Test
  void testMsvcDepsPrefix() throws IOException {
    builder.msvcDepsPrefix(FormatString.fromPlainText("foo"));
    assertEquals("foo", builder.build().msvcDepsPrefix().toString());
    assertThrows(NullPointerException.class, () -> builder.msvcDepsPrefix(null));
  }

  @Test
  void testDescription() throws IOException {
    builder.description(FormatString.fromPlainText("foo"));
    assertEquals("foo", builder.build().description().toString());
    assertThrows(NullPointerException.class, () -> builder.description(null));
  }

  @Test
  void testDynDep() throws IOException {
    builder.dynDep(FormatString.fromPlainText("foo"));
    assertEquals("foo", builder.build().dynDep().toString());
    assertThrows(NullPointerException.class, () -> builder.dynDep(null));
  }

  @Test
  void testGenerator() {
    assertFalse(builder.build().generator());
    builder.generator(true);
    assertTrue(builder.build().generator());
  }

  @Test
  void testRestat() {
    assertFalse(builder.build().restat());
    builder.restat(true);
    assertTrue(builder.build().restat());
  }

  @Test
  void testRspFile() throws IOException {
    builder.rspFile(FormatString.fromPlainText("foo"));
    IllegalStateException e = assertThrows(IllegalStateException.class, builder::build);
    assertEquals("rspfile and rspfile_content must be set together", e.getMessage());

    builder.rspFileContent(FormatString.fromPlainText("bar"));
    assertEquals("foo", builder.build().rspFile().toString());
    assertEquals("bar", builder.build().rspFileContent().toString());

    assertThrows(NullPointerException.class, () -> builder.rspFile(null));
    assertThrows(NullPointerException.class, () -> builder.rspFileContent(null));
  }

  @Test
  void testPool() throws IOException {
    builder.pool(FormatString.fromPlainText("foo"));
    assertEquals("foo", builder.build().pool().toString());
    assertThrows(NullPointerException.class, () -> builder.pool(null));
  }
}

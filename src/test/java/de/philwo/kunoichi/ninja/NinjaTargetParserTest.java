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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NinjaTargetParserTest {

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
  void parseBuildTargetWorks() throws IOException {
    Files.writeString(
        tempDir.resolve("build.ninja"),
        """
            rule compile
              command = cc $in -o $out
            build out1: compile in1
              pdb = out1.pdb
            """);
    NinjaFile ninjaFile = NinjaFile.parse(tempDir.resolve("build.ninja"));
    Action target = ninjaFile.actions().get(0);
    assertEquals("compile", target.mnemonic());
    assertEquals(List.of(Path.of("out1")), target.outputs().toList());
    assertEquals(List.of(Path.of("in1")), target.inputs().toList());
    assertEquals("out1.pdb", target.env().get("pdb"));
  }
}

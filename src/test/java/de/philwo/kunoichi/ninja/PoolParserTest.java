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
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.philwo.kunoichi.datamodel.Environment;
import de.philwo.kunoichi.datamodel.Pool;
import de.philwo.kunoichi.ninja.NinjaToken.Type;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class PoolParserTest {

  @Test
  void parsePoolWorks() throws IOException {
    NinjaFileLexer lexer =
        new NinjaFileLexer("""
        pool link_pool
          depth = 4
        """);
    assertEquals(Type.POOL, lexer.readToken().type());
    Pool pool = new NinjaPoolParser(lexer, Environment.EMPTY).parse();
    assertEquals("link_pool", pool.name());
    assertEquals(4, pool.depth());
  }

  @Test
  void parsePoolFailsOnMissingDepth() throws IOException {
    NinjaFileLexer lexer = new NinjaFileLexer("""
        pool link_pool
        """);
    assertEquals(Type.POOL, lexer.readToken().type());
    assertThrows(
        NinjaParserException.class, () -> new NinjaPoolParser(lexer, Environment.EMPTY).parse());
  }
}

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class EnvironmentTest {

  @Test
  void testSimplePutGet() throws IOException {
    Environment env = new Environment();
    env.put("foo", FormatString.fromPlainText("bar"));
    assertEquals("bar", env.get("foo"));
  }

  //  @Test
  //  void testExceptionOnMissingKey() {
  //    Environment env = new Environment();
  //    Exception e = assertThrows(IllegalArgumentException.class, () -> env.get("foo"));
  //    assertTrue(e.getMessage().contains("Unknown variable"));
  //  }

  @Test
  void testExceptionOnDuplicateVariable() throws IOException {
    Environment env = new Environment();
    env.put("foo", FormatString.fromPlainText("bar"));
    Exception e =
        assertThrows(
            IllegalArgumentException.class,
            () -> env.put("foo", FormatString.fromPlainText("baz")));
    assertTrue(e.getMessage().contains("Duplicate variable"));
  }

  @Test
  void testCanGetVariableFromParent() throws IOException {
    Environment parent = new Environment();
    parent.put("foo", FormatString.fromPlainText("bar"));
    Environment env = new Environment(parent);
    assertEquals("bar", env.get("foo"));
  }

  @Test
  void testCanShadowVariable() throws IOException {
    Environment parent = new Environment();
    parent.put("foo", FormatString.fromPlainText("bar"));
    Environment env = new Environment(parent);
    env.put("foo", FormatString.fromPlainText("baz"));
    assertEquals("baz", env.get("foo"));
  }
}

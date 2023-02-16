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

import de.philwo.kunoichi.datamodel.Environment;
import de.philwo.kunoichi.datamodel.FormatString;
import de.philwo.kunoichi.datamodel.Pool;
import de.philwo.kunoichi.ninja.NinjaToken.Type;
import java.io.IOException;

class NinjaPoolParser {

  private final NinjaFileLexer lexer;
  private final Environment env;

  NinjaPoolParser(NinjaFileLexer lexer, Environment env) {
    this.lexer = lexer;
    this.env = env;
  }

  Pool parse() throws IOException {
    String poolName = lexer.readIdentifier();
    if (poolName.isEmpty()) {
      throw new NinjaParserException("Expected pool name, but not found or invalid");
    }

    NinjaToken token = lexer.readToken();
    if (token.type() != Type.NEWLINE) {
      throw new NinjaParserException("Expected newline after rule name, got: " + token.type());
    }

    Integer depth = null;
    while (lexer.nextLineIsIndented()) {
      token = lexer.readToken();
      if (token.type() != Type.INDENT) {
        throw new NinjaParserException("Expected indent, got: " + token.type());
      }
      String key = lexer.readIdentifier();
      if (lexer.readToken().type() != Type.EQUALS) {
        throw new NinjaParserException("Expected '=', got: " + token);
      }
      FormatString value = lexer.readVarValue();
      switch (key) {
        case "depth" -> depth = Integer.valueOf(value.evaluate(env));
        default -> throw new NinjaParserException("Unknown pool key: " + key);
      }
    }

    if (depth == null) {
      throw new NinjaParserException("Expected 'depth' key in pool");
    }

    token = lexer.readToken();
    if (token.type() != Type.EOF && token.type() != Type.NEWLINE) {
      throw new NinjaParserException("Unexpected trailing characters: " + token.type());
    }

    return new Pool(poolName, depth);
  }
}

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
import de.philwo.kunoichi.datamodel.Rule;
import de.philwo.kunoichi.datamodel.Rule.Deps;
import de.philwo.kunoichi.datamodel.Rule.RuleBuilder;
import de.philwo.kunoichi.ninja.NinjaToken.Type;
import java.io.IOException;

class NinjaRuleParser {

  private final NinjaFileLexer lexer;

  NinjaRuleParser(NinjaFileLexer lexer) {
    this.lexer = lexer;
  }

  Rule parse() throws NinjaParserException, IOException {
    String ruleName = lexer.readIdentifier();
    if (ruleName.isEmpty()) {
      throw new NinjaParserException("Expected rule name, but not found or invalid");
    }

    NinjaToken token = lexer.readToken();
    if (token.type() != Type.EOF && token.type() != Type.NEWLINE) {
      throw new NinjaParserException("Unexpected trailing characters: " + token.type());
    }

    RuleBuilder rule = new RuleBuilder();
    rule.name(ruleName);
    while (lexer.nextLineIsIndented()) {
      token = lexer.readToken();
      if (token.type() != Type.INDENT) {
        throw new NinjaParserException("Expected indent, got: " + token);
      }
      String key = lexer.readIdentifier();
      if (lexer.readToken().type() != Type.EQUALS) {
        throw new NinjaParserException("Expected '=', got: " + token);
      }
      FormatString value = lexer.readVarValue();
      switch (key) {
        case "command" -> rule.command(value);
        case "depfile" -> rule.depFile(value);
        case "deps" -> rule.deps(Deps.valueOf(value.evaluate(Environment.EMPTY).toUpperCase()));
        case "msvc_deps_prefix" -> rule.msvcDepsPrefix(value);
        case "description" -> rule.description(value);
        case "dyndep" -> rule.dynDep(value);
        case "generator" -> rule.generator(true);
        case "restat" -> rule.restat(true);
        case "rspfile" -> rule.rspFile(value);
        case "rspfile_content" -> rule.rspFileContent(value);
        case "pool" -> rule.pool(value);
        default -> throw new NinjaParserException("Unknown rule key: " + key);
      }
      token = lexer.readToken();
      if (token.type() != Type.EOF && token.type() != Type.NEWLINE) {
        throw new NinjaParserException("Unexpected trailing characters: " + token.type());
      }
    }

    return rule.build();
  }
}

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

import static de.philwo.kunoichi.datamodel.Rule.PHONY_RULE;

import de.philwo.kunoichi.datamodel.Action;
import de.philwo.kunoichi.datamodel.DefaultTargets;
import de.philwo.kunoichi.datamodel.Environment;
import de.philwo.kunoichi.datamodel.FormatString;
import de.philwo.kunoichi.datamodel.Pools;
import de.philwo.kunoichi.datamodel.Rules;
import de.philwo.kunoichi.ninja.NinjaToken.Type;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

public final class NinjaFile {

  final Path filePath;
  final Environment env;
  final Rules rules;
  final DefaultTargets defaultTargets;
  final Pools pools;
  final ArrayList<Action> actions;

  public static NinjaFile parse(Path filePath) throws IOException {
    NinjaFile ninjaFile = new NinjaFile(filePath);
    ninjaFile.rules().add(PHONY_RULE);
    try (NinjaFileParser parser = ninjaFile.new NinjaFileParser(filePath)) {
      parser.parse();
    }
    return ninjaFile;
  }

  private NinjaFile(Path filePath) {
    this.filePath = filePath;
    this.env = new Environment();
    this.rules = new Rules();
    this.defaultTargets = new DefaultTargets();
    this.pools = new Pools();
    this.actions = new ArrayList<>();
  }

  private NinjaFile(Path filePath, NinjaFile parent) {
    this.filePath = filePath;
    this.env = new Environment(parent.env);
    this.rules = new Rules(parent.rules);
    this.defaultTargets = parent.defaultTargets;
    this.pools = parent.pools;
    this.actions = parent.actions;
  }

  public Environment env() {
    return env;
  }

  public Rules rules() {
    return rules;
  }

  public DefaultTargets defaultTargets() {
    return defaultTargets;
  }

  public Pools pools() {
    return pools;
  }

  public ArrayList<Action> actions() {
    return actions;
  }

  private final class NinjaFileParser implements AutoCloseable {
    private final NinjaFileLexer lexer;
    private final NinjaRuleParser ruleParser;
    private final NinjaTargetParser targetParser;
    private final NinjaPoolParser poolParser;

    NinjaFileParser(Path filePath) throws IOException {
      this.lexer = new NinjaFileLexer(filePath);
      this.ruleParser = new NinjaRuleParser(lexer);
      this.targetParser = new NinjaTargetParser(lexer, rules(), env());
      this.poolParser = new NinjaPoolParser(lexer, env());
    }

    void parse() throws IOException {
      NinjaToken token = lexer.readToken();
      while (token.type() != Type.EOF) {
        switch (token.type()) {
          case NEWLINE -> {
            /* Ignore empty lines. */
          }
          case RULE -> rules().add(ruleParser.parse());
          case BUILD -> actions().add(targetParser.parse());
          case DEFAULT -> defaultTargets().addAll(parseDefaultTargets());
          case POOL -> pools().add(poolParser.parse());
          case INCLUDE -> parseInclude(NinjaFile.this);
          case SUBNINJA -> parseInclude(new NinjaFile(filePath, NinjaFile.this));
          case VARIABLE_NAME -> env().add(parseVariableAssignment(token));
          default -> throw new IOException("Unexpected token: " + token.type());
        }
        token = lexer.readToken();
      }
    }

    private Stream<Path> parseDefaultTargets() {
      Stream.Builder<FormatString> defaultTargets = Stream.builder();
      FormatString defaultTarget = lexer.readPath();
      if (defaultTarget.isEmpty()) {
        throw new NinjaParserException("Expected target after 'default', but not found");
      }
      while (!defaultTarget.isEmpty()) {
        defaultTargets.add(defaultTarget);
        defaultTarget = lexer.readPath();
      }
      verifyNoTrailingChars();
      return defaultTargets.build().map(env()::evaluate).map(Path::of);
    }

    private void parseInclude(NinjaFile ninjaFile) throws IOException {
      String filename = lexer.readPath().evaluate(ninjaFile.env());
      try (NinjaFileParser parser =
          ninjaFile.new NinjaFileParser(filePath.resolveSibling(filename))) {
        parser.parse();
      }
    }

    private Entry<String, FormatString> parseVariableAssignment(NinjaToken token) {
      if (token.type() != Type.VARIABLE_NAME) {
        throw new NinjaParserException("Expected variable name, but found: " + token.type());
      }
      String key = token.text();
      if (lexer.readToken().type() != Type.EQUALS) {
        throw new NinjaParserException("Expected '=' after variable name");
      }
      FormatString value = lexer.readVarValue();
      verifyNoTrailingChars();
      return Map.entry(key, value);
    }

    private void verifyNoTrailingChars() {
      NinjaToken token = lexer.readToken();
      if (token.type() != Type.EOF && token.type() != Type.NEWLINE) {
        throw new NinjaParserException("Unexpected trailing characters: " + token.type());
      }
    }

    @Override
    public void close() throws IOException {
      lexer.close();
    }
  }
}

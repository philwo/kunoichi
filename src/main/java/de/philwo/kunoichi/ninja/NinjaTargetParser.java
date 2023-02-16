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

import de.philwo.kunoichi.datamodel.Action;
import de.philwo.kunoichi.datamodel.Environment;
import de.philwo.kunoichi.datamodel.FormatString;
import de.philwo.kunoichi.datamodel.Rule;
import de.philwo.kunoichi.datamodel.Rules;
import de.philwo.kunoichi.ninja.NinjaToken.Type;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class NinjaTargetParser {

  private final NinjaFileLexer lexer;
  private final Rules rules;
  private final Environment env;

  private NinjaToken token;

  NinjaTargetParser(NinjaFileLexer lexer, Rules rules, Environment env) {
    this.lexer = lexer;
    this.rules = rules;
    this.env = env;
  }

  Action parse() throws NinjaParserException, IOException {
    // Parse the explicit outputs.
    Stream<FormatString> rawExplicitOuts = prefetchStreamAndNextToken(lexer.readPaths());

    // Parse the implicit outputs, if present.
    Stream<FormatString> rawImplicitOuts =
        switch (token.type()) {
          case PIPE -> prefetchStreamAndNextToken(lexer.readPaths());
          default -> Stream.empty();
        };

    // After the outputs, we expect the ':' separator.
    if (token.type() != Type.COLON) {
      throw new NinjaParserException("Expected ':', but found '" + token.text() + "'");
    }

    // The rule name follows the ':' separator.
    Rule rule = rules.get(lexer.readIdentifier());

    // The inputs follow right after the rule name without any separator.
    Stream<FormatString> rawExplicitIns = prefetchStreamAndNextToken(lexer.readPaths());

    // Parse the implicit dependencies ("|"), if present.
    Stream<FormatString> rawImplicitIns =
        switch (token.type()) {
          case PIPE -> prefetchStreamAndNextToken(lexer.readPaths());
          default -> Stream.empty();
        };

    // Parse the order-only dependencies ("||"), if present.
    Stream<FormatString> rawOrderOnlyIns =
        switch (token.type()) {
          case DOUBLE_PIPE -> prefetchStreamAndNextToken(lexer.readPaths());
          default -> Stream.empty();
        };

    // Parse the validations ("|@"), if present.
    Stream<FormatString> rawValidations =
        switch (token.type()) {
          case PIPE_AT -> prefetchStreamAndNextToken(lexer.readPaths());
          default -> Stream.empty();
        };

    if (token.type() != Type.EOF && token.type() != Type.NEWLINE) {
      throw new NinjaParserException("Unexpected trailing characters: " + token.type());
    }

    // Parse the environment variables, if present.
    Environment env = new Environment(this.env);
    if (lexer.nextLineIsIndented()) {
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
        env.put(key, value);

        token = lexer.readToken();
        if (token.type() != Type.EOF && token.type() != Type.NEWLINE) {
          throw new NinjaParserException("Unexpected trailing characters: " + token.type());
        }
      }
    }

    Set<String> outputSet = new HashSet<>();
    Set<String> inputSet = new HashSet<>();
    List<Path> explicitOuts = dedupAndNormalize(rawExplicitOuts, outputSet);
    List<Path> implicitOuts = dedupAndNormalize(rawImplicitOuts, outputSet);
    List<Path> explicitIns = dedupAndNormalize(rawExplicitIns, inputSet);
    List<Path> implicitIns = dedupAndNormalize(rawImplicitIns, inputSet);
    List<Path> orderOnlyIns = dedupAndNormalize(rawOrderOnlyIns, inputSet);
    List<Path> validations = dedupAndNormalize(rawValidations, new HashSet<>());

    env.put(
        "in",
        () ->
            explicitIns.stream()
                .map(Path::toString)
                .collect(
                    Collectors.joining(
                        " "))); // TODO (philwo): This is not correct. We need to escape spaces in
                                // paths.
    env.put(
        "in_newline",
        () -> explicitIns.stream().map(Path::toString).collect(Collectors.joining("\n")));
    env.put(
        "out",
        () ->
            explicitOuts.stream()
                .map(Path::toString)
                .collect(
                    Collectors.joining(
                        " "))); // TODO (philwo): This is not correct. We need to escape spaces in
                                // paths.
    env.put("rspfile", () -> Paths.get(env.evaluate(rule.rspFile())).normalize().toString());

    return new Action(
        rule, env, explicitOuts, implicitOuts, explicitIns, implicitIns, orderOnlyIns, validations);
  }

  private List<Path> dedupAndNormalize(Stream<FormatString> stream, Set<String> uniques) {
    return stream
        .map(env::evaluate)
        .filter(uniques::add)
        .map(Path::of)
        .map(Path::normalize)
        .toList();
  }

  private Stream<FormatString> prefetchStreamAndNextToken(Stream<FormatString> stream) {
    try {
      Stream.Builder<FormatString> builder = Stream.builder();
      stream.forEach(builder::add);
      return builder.build();
    } finally {
      this.token = lexer.readToken();
    }
  }
}

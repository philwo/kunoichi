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
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.philwo.kunoichi.datamodel.FormatString;
import de.philwo.kunoichi.datamodel.FormatString.Chunk.Kind;
import de.philwo.kunoichi.ninja.NinjaToken.Type;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NinjaFileLexerTest {
  @Test
  void readRuleToken() throws IOException {
    NinjaFileLexer lexer =
        new NinjaFileLexer("""
          rule echo
            command = echo
          """);
    assertEquals(Type.RULE, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.NEWLINE, lexer.readToken().type());
    assertEquals(Type.INDENT, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.EQUALS, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.NEWLINE, lexer.readToken().type());
    assertEquals(Type.EOF, lexer.readToken().type());
  }

  @Test
  void readBuildToken() throws IOException {
    NinjaFileLexer lexer =
        new NinjaFileLexer(
            """
          build out1 | out2: input1 | input2 || input3 |@ validation1
          """);
    assertEquals(Type.BUILD, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.PIPE, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.COLON, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.PIPE, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.DOUBLE_PIPE, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.PIPE_AT, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.NEWLINE, lexer.readToken().type());
    assertEquals(Type.EOF, lexer.readToken().type());
  }

  @Test
  void readDefaultPoolIncludeSubninjaTokens() throws IOException {
    NinjaFileLexer lexer =
        new NinjaFileLexer(
            """
          default foo

          pool link_pool
            depth = 1

          include foo
          subninja bar
          """);
    assertEquals(Type.DEFAULT, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.NEWLINE, lexer.readToken().type());
    assertEquals(Type.NEWLINE, lexer.readToken().type());
    assertEquals(Type.POOL, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.NEWLINE, lexer.readToken().type());
    assertEquals(Type.INDENT, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.EQUALS, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.NEWLINE, lexer.readToken().type());
    assertEquals(Type.NEWLINE, lexer.readToken().type());
    assertEquals(Type.INCLUDE, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.NEWLINE, lexer.readToken().type());
    assertEquals(Type.SUBNINJA, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.NEWLINE, lexer.readToken().type());
    assertEquals(Type.EOF, lexer.readToken().type());
  }

  @Test
  void skipDollarEscapedNewline() throws IOException {
    NinjaFileLexer lexer =
        new NinjaFileLexer("""
          default target1$
            target2
          """);
    assertEquals(Type.DEFAULT, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.NEWLINE, lexer.readToken().type());
    assertEquals(Type.EOF, lexer.readToken().type());
  }

  @Test
  void catchesInvalidVariableName() throws IOException {
    NinjaFileLexer lexer =
        new NinjaFileLexer("""
                rule !invalid
                """);
    assertEquals(Type.RULE, lexer.readToken().type());
    NinjaParserException e = Assertions.assertThrows(NinjaParserException.class, lexer::readToken);
    assertEquals("Invalid character while parsing token: !", e.getMessage());
  }

  @Test
  void readIdentifierUntilEOF() throws IOException {
    NinjaFileLexer lexer = new NinjaFileLexer("rule echo");
    assertEquals(Type.RULE, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
  }

  @Test
  void readTokenSkipsComments() throws IOException {
    NinjaFileLexer lexer =
        new NinjaFileLexer(
            """
                # comment
                rule echo
                # comment
                build out: input
                # comment
                """);
    assertEquals(Type.RULE, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.NEWLINE, lexer.readToken().type());
    assertEquals(Type.BUILD, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.COLON, lexer.readToken().type());
    assertEquals(Type.VARIABLE_NAME, lexer.readToken().type());
    assertEquals(Type.NEWLINE, lexer.readToken().type());
    assertEquals(Type.EOF, lexer.readToken().type());
  }

  @Test
  void createCorrectlyParsesString() throws IOException {
    String input =
        "Question$: How many SEO experts does it take to change a $light $bulb, ${lightbulb},$"
            + " light, b${ul}b, lamp, lighting, $light$switch, switch, $energy";
    NinjaFileLexer lexer = new NinjaFileLexer(input);
    FormatString formatString = lexer.readVarValue();
    assertEquals(Type.EOF, lexer.readToken().type());
    assertEquals(
        "Question: How many SEO experts does it take to change a ",
        formatString.chunks().get(0).text());
    assertEquals(Kind.TEXT, formatString.chunks().get(0).kind());
    assertEquals("light", formatString.chunks().get(1).text());
    assertEquals(Kind.VARIABLE, formatString.chunks().get(1).kind());
    assertEquals(" ", formatString.chunks().get(2).text());
    assertEquals(Kind.TEXT, formatString.chunks().get(2).kind());
    assertEquals("bulb", formatString.chunks().get(3).text());
    assertEquals(Kind.VARIABLE, formatString.chunks().get(3).kind());
    assertEquals(", ", formatString.chunks().get(4).text());
    assertEquals(Kind.TEXT, formatString.chunks().get(4).kind());
    assertEquals("lightbulb", formatString.chunks().get(5).text());
    assertEquals(Kind.VARIABLE, formatString.chunks().get(5).kind());
    assertEquals(", light, b", formatString.chunks().get(6).text());
    assertEquals(Kind.TEXT, formatString.chunks().get(6).kind());
    assertEquals("ul", formatString.chunks().get(7).text());
    assertEquals(Kind.VARIABLE, formatString.chunks().get(7).kind());
    assertEquals("b, lamp, lighting, ", formatString.chunks().get(8).text());
    assertEquals(Kind.TEXT, formatString.chunks().get(8).kind());
    assertEquals("light", formatString.chunks().get(9).text());
    assertEquals(Kind.VARIABLE, formatString.chunks().get(9).kind());
    assertEquals("switch", formatString.chunks().get(10).text());
    assertEquals(Kind.VARIABLE, formatString.chunks().get(10).kind());
    assertEquals(", switch, ", formatString.chunks().get(11).text());
    assertEquals(Kind.TEXT, formatString.chunks().get(11).kind());
    assertEquals("energy", formatString.chunks().get(12).text());
    assertEquals(Kind.VARIABLE, formatString.chunks().get(12).kind());
  }

  @Test
  void exceptionOnUnexpectedEndOfFileWhileParsing() {
    String input = "foobar$";
    NinjaFileLexer lexer = new NinjaFileLexer(input);
    NinjaParserException e =
        Assertions.assertThrows(NinjaParserException.class, lexer::readVarValue);
    assertEquals("Unexpected end of file while parsing $-escape", e.getMessage());
  }

  @Test
  void exceptionOnMissingVariableName() {
    String input = "foobar${}";
    NinjaFileLexer lexer = new NinjaFileLexer(input);
    NinjaParserException e =
        Assertions.assertThrows(NinjaParserException.class, lexer::readVarValue);
    assertEquals("Expected variable name after '${'", e.getMessage());
  }

  @Test
  void exceptionOnInvalidDollarEscape() {
    String input = "foobar$!";
    NinjaFileLexer lexer = new NinjaFileLexer(input);
    NinjaParserException e =
        Assertions.assertThrows(NinjaParserException.class, lexer::readVarValue);
    assertEquals("Invalid dollar escape: $!", e.getMessage());
  }

  @Test
  void exceptionOnMissingClosingCurlyBrace() {
    String input = "foobar${bazbaz foobar";
    NinjaFileLexer lexer = new NinjaFileLexer(input);
    NinjaParserException e =
        Assertions.assertThrows(NinjaParserException.class, lexer::readVarValue);
    assertEquals("Expected '}' after variable name", e.getMessage());
  }

  @Test
  void readPath() throws IOException {
    String input = "build $builddir/$ out1 ${builddir}/$:out2: compile in1 $foo${bar}in2";
    NinjaFileLexer lexer = new NinjaFileLexer(input);

    FormatString str = lexer.readPath();
    assertEquals(1, str.chunks().size());
    assertEquals("build", str.chunks().get(0).text());
    assertEquals(Kind.TEXT, str.chunks().get(0).kind());
    assertEquals("build", str.toString());

    str = lexer.readPath();
    assertEquals(2, str.chunks().size());
    assertEquals("builddir", str.chunks().get(0).text());
    assertEquals(Kind.VARIABLE, str.chunks().get(0).kind());
    assertEquals("/ out1", str.chunks().get(1).text());
    assertEquals(Kind.TEXT, str.chunks().get(1).kind());
    assertEquals("${builddir}/ out1", str.toString());

    str = lexer.readPath();
    assertEquals(2, str.chunks().size());
    assertEquals("builddir", str.chunks().get(0).text());
    assertEquals(Kind.VARIABLE, str.chunks().get(0).kind());
    assertEquals("/:out2", str.chunks().get(1).text());
    assertEquals(Kind.TEXT, str.chunks().get(1).kind());
    assertEquals("${builddir}/:out2", str.toString());

    str = lexer.readPath();
    assertEquals(0, str.chunks().size());
    assertTrue(str.isEmpty());

    assertEquals(Type.COLON, lexer.readToken().type());

    str = lexer.readPath();
    assertEquals(1, str.chunks().size());
    assertEquals("compile", str.chunks().get(0).text());
    assertEquals(Kind.TEXT, str.chunks().get(0).kind());
    assertEquals("compile", str.toString());

    str = lexer.readPath();
    assertEquals(1, str.chunks().size());
    assertEquals("in1", str.chunks().get(0).text());
    assertEquals(Kind.TEXT, str.chunks().get(0).kind());
    assertEquals("in1", str.toString());

    str = lexer.readPath();
    assertEquals(3, str.chunks().size());
    assertEquals("foo", str.chunks().get(0).text());
    assertEquals(Kind.VARIABLE, str.chunks().get(0).kind());
    assertEquals("bar", str.chunks().get(1).text());
    assertEquals(Kind.VARIABLE, str.chunks().get(1).kind());
    assertEquals("in2", str.chunks().get(2).text());
    assertEquals(Kind.TEXT, str.chunks().get(2).kind());
    assertEquals("${foo}${bar}in2", str.toString());

    assertEquals(Type.EOF, lexer.readToken().type());
  }
}

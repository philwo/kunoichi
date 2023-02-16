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

import de.philwo.kunoichi.datamodel.FormatString;
import de.philwo.kunoichi.datamodel.FormatString.FormatStringBuilder;
import de.philwo.kunoichi.ninja.NinjaToken.Type;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

class NinjaFileLexer implements AutoCloseable {

  // Returns true if the character matches [a-zA-Z0-9_].
  private static boolean validSimpleVariableNameChar(char c) {
    return (c >= 'a' && c <= 'z')
        || (c >= 'A' && c <= 'Z')
        || (c >= '0' && c <= '9')
        || c == '_'
        || c == '-';
  }

  // Returns true if the character matches [a-zA-Z0-9_.].
  private static boolean validVariableNameChar(char c) {
    return validSimpleVariableNameChar(c) || c == '.';
  }

  private static final NinjaToken EOF_TOKEN = new NinjaToken(Type.EOF);
  private static final NinjaToken INDENT_TOKEN = new NinjaToken(Type.INDENT);
  private static final NinjaToken NEWLINE_TOKEN = new NinjaToken(Type.NEWLINE);
  private static final NinjaToken RULE_TOKEN = new NinjaToken(Type.RULE);
  private static final NinjaToken BUILD_TOKEN = new NinjaToken(Type.BUILD);
  private static final NinjaToken DEFAULT_TOKEN = new NinjaToken(Type.DEFAULT);
  private static final NinjaToken POOL_TOKEN = new NinjaToken(Type.POOL);
  private static final NinjaToken INCLUDE_TOKEN = new NinjaToken(Type.INCLUDE);
  private static final NinjaToken SUBNINJA_TOKEN = new NinjaToken(Type.SUBNINJA);
  private static final NinjaToken EQUALS_TOKEN = new NinjaToken(Type.EQUALS);
  private static final NinjaToken COLON_TOKEN = new NinjaToken(Type.COLON);
  private static final NinjaToken DOUBLE_PIPE_TOKEN = new NinjaToken(Type.DOUBLE_PIPE);
  private static final NinjaToken PIPE_AT_TOKEN = new NinjaToken(Type.PIPE_AT);
  private static final NinjaToken PIPE_TOKEN = new NinjaToken(Type.PIPE);

  private final BufferedReader reader;

  NinjaFileLexer(Path filePath) throws IOException {
    Charset cs = Charset.forName("UTF-8");
    CharsetDecoder decoder =
        cs.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
    InputStream inputStream = Files.newInputStream(filePath);
    this.reader = new BufferedReader(new InputStreamReader(inputStream, decoder));
  }

  NinjaFileLexer(String s) {
    this.reader = new BufferedReader(new StringReader(s));
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }

  boolean nextLineIsIndented() throws IOException {
    return peek() == ' ';
  }

  /** Reads a single character from the stream, transparently skipping over comments. */
  int read() throws IOException {
    int c = reader.read();
    if (c == '#') {
      reader.readLine();
      return read();
    } else {
      return c;
    }
  }

  private int peek() throws IOException {
    reader.mark(1);
    int c = read();
    reader.reset();
    return c;
  }

  /** Skip over whitespace and $-escaped newlines. */
  private void skipWhitespace() throws NinjaParserException {
    try {
      do {
        reader.mark(2);
        int c = read();
        if (c == ' ') {
          // Skip whitespace.
        } else if (c == '$') {
          // Skip over $-escaped newlines.
          c = read();
          if (c != '\n') {
            reader.reset();
            break;
          }
        } else {
          reader.reset();
          break;
        }
      } while (true);
    } catch (IOException e) {
      throw new NinjaParserException("I/O error while skipping whitespace", e);
    }
  }

  /** Returns the next NinjaToken. */
  NinjaToken readToken() throws NinjaParserException {
    NinjaToken result;
    try {
      int c = read();
      result =
          switch (c) {
            case -1 -> EOF_TOKEN;
            case ' ' -> INDENT_TOKEN;
            case '\n' -> NEWLINE_TOKEN;
            case '=' -> EQUALS_TOKEN;
            case ':' -> COLON_TOKEN;
            case '|' -> {
              reader.mark(1);
              yield switch (read()) {
                case '|' -> DOUBLE_PIPE_TOKEN;
                case '@' -> PIPE_AT_TOKEN;
                default -> {
                  reader.reset();
                  yield PIPE_TOKEN;
                }
              };
            }
            default -> {
              if (!validVariableNameChar((char) c)) {
                throw new NinjaParserException(
                    "Invalid character while parsing token: " + (char) c);
              }
              String identifier = readIdentifier(c);
              yield switch (identifier) {
                case "rule" -> RULE_TOKEN;
                case "build" -> BUILD_TOKEN;
                case "default" -> DEFAULT_TOKEN;
                case "pool" -> POOL_TOKEN;
                case "include" -> INCLUDE_TOKEN;
                case "subninja" -> SUBNINJA_TOKEN;
                default -> new NinjaToken(Type.VARIABLE_NAME, identifier);
              };
            }
          };
    } catch (IOException e) {
      throw new NinjaParserException("I/O error while reading token", e);
    }

    if (result == null) {
      throw new NinjaParserException("Unexpected end of file");
    }

    // Skip over any whitespace after the token, except when the token is a newline.
    // This is necessary to be able to distinguish between a newline that is followed by an
    // indented line and a newline that is followed by a non-indented line.
    if (result.type() != Type.NEWLINE) {
      skipWhitespace();
    }

    return result;
  }

  private String readIdentifier(int c) throws NinjaParserException {
    StringBuilder sb = new StringBuilder();
    if (c != -1) {
      sb.append((char) c);
    }
    try {
      do {
        reader.mark(1);
        c = read();
        if (c == -1) {
          break;
        }
        if (!validVariableNameChar((char) c)) {
          reader.reset();
          break;
        }
        sb.append((char) c);
      } while (true);
    } catch (IOException e) {
      throw new NinjaParserException("I/O error while reading identifier", e);
    }
    skipWhitespace();
    return sb.toString();
  }

  String readIdentifier() throws NinjaParserException {
    return readIdentifier(-1);
  }

  FormatString readVarValue() throws NinjaParserException {
    return readEvalString(false);
  }

  FormatString readPath() throws NinjaParserException {
    return readEvalString(true);
  }

  Stream<FormatString> readPaths() throws NinjaParserException {
    return Stream.iterate(readPath(), path -> !path.isEmpty(), path -> readPath());
  }

  private FormatString readEvalString(boolean isPath) throws NinjaParserException {
    FormatStringBuilder sb = FormatString.builder();

    try {
      reader.mark(1);
      int c = read();
      readLoop:
      while (c != -1) {
        switch (c) {
          case ' ', ':', '|', '\n' -> {
            if (isPath || c == '\n') {
              reader.reset();
              break readLoop;
            }
            sb.addText((char) c);
          }
          case '$' -> {
            c = read();
            switch (c) {
              case -1 -> throw new NinjaParserException(
                  "Unexpected end of file while parsing $-escape");
              case '$', ' ', ':' -> sb.addText((char) c);
              case '\n' -> skipWhitespace();
              case '{' -> {
                String identifier = readIdentifier();
                if (identifier.isEmpty()) {
                  throw new NinjaParserException("Expected variable name after '${'");
                }
                if (read() != '}') {
                  throw new NinjaParserException("Expected '}' after variable name");
                }
                sb.addVariable(identifier);
              }
              default -> {
                if (!validSimpleVariableNameChar((char) c)) {
                  throw new NinjaParserException("Invalid dollar escape: $" + (char) c);
                }
                StringBuilder varName = new StringBuilder();
                do {
                  varName.append((char) c);
                  reader.mark(1);
                  c = read();
                } while (c != -1 && validSimpleVariableNameChar((char) c));
                reader.reset();
                sb.addVariable(varName.toString());
              }
            }
          }
          default -> sb.addText((char) c);
        }
        reader.mark(1);
        c = read();
      }
      if (isPath) {
        skipWhitespace();
      }
    } catch (IOException e) {
      throw new NinjaParserException("I/O error while parsing string", e);
    }

    return sb.build();
  }
}

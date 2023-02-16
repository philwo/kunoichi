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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record FormatString(List<Chunk> chunks) {
  static final FormatString EMPTY = new FormatString(List.of());

  public static FormatString fromPlainText(String text) {
    if (text.contains("$")) {
      throw new IllegalArgumentException("Text must not contain '$' characters: " + text);
    }
    return new FormatString(List.of(new Chunk(Chunk.Kind.TEXT, text)));
  }

  public static FormatStringBuilder builder() {
    return new FormatStringBuilder();
  }

  public String evaluate(Environment env) {
    return chunks.stream()
        .map(
            chunk ->
                switch (chunk.kind()) {
                  case TEXT -> chunk.text();
                  case VARIABLE -> env.get(chunk.text());
                })
        .collect(Collectors.joining());
  }

  public boolean isEmpty() {
    return chunks.isEmpty();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Chunk chunk : chunks) {
      sb.append(chunk.toQuotedString());
    }
    return sb.toString();
  }

  public record Chunk(Kind kind, String text) {
    public enum Kind {
      TEXT,
      VARIABLE
    }

    private String toQuotedString() {
      return switch (kind) {
        case TEXT -> text.replace("$", "$$");
        case VARIABLE -> "${" + text + "}";
      };
    }
  }

  public static class FormatStringBuilder {
    private List<Chunk> chunks = new ArrayList<>();
    private StringBuilder textBuilder = new StringBuilder();

    private FormatStringBuilder() {}

    public FormatStringBuilder addText(String s) {
      textBuilder.append(s);
      return this;
    }

    public FormatStringBuilder addText(char c) {
      textBuilder.append(c);
      return this;
    }

    public FormatStringBuilder addVariable(String s) {
      ensurePendingTextIsCommitted();
      chunks.add(new Chunk(Chunk.Kind.VARIABLE, s));
      return this;
    }

    private FormatStringBuilder ensurePendingTextIsCommitted() {
      if (!textBuilder.isEmpty()) {
        String text = textBuilder.toString();
        chunks.add(new Chunk(Chunk.Kind.TEXT, text));
        textBuilder = new StringBuilder();
      }
      return this;
    }

    public FormatString build() {
      ensurePendingTextIsCommitted();
      return new FormatString(chunks);
    }
  }
}

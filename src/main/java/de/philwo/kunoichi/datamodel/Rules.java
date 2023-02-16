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

import java.util.HashMap;
import java.util.Map;

public final class Rules {

  public static final Rules EMPTY = new Rules();

  private final Rules parent;
  private final Map<String, Rule> rules = new HashMap<>();

  public Rules(Rules parent) {
    this.parent = parent;
  }

  public Rules() {
    this(null);
  }

  public Rule get(String name) {
    Rule rule = rules.get(name);
    if (rule == null) {
      if (parent != null) {
        return parent.get(name);
      }
      throw new IllegalArgumentException("Unknown rule: ${" + name + "}");
    }
    return rule;
  }

  public void add(Rule rule) {
    if (rules.put(rule.name(), rule) != null) {
      throw new IllegalArgumentException("Duplicate rule: ${" + rule.name() + "}");
    }
  }
}

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
import java.util.Map.Entry;
import java.util.function.Supplier;

public final class Environment {

  public static final Environment EMPTY = new Environment();

  private final Environment parent;
  private final Map<String, FormatString> vars = new HashMap<>();
  private final Map<String, Supplier<String>> computedVars = new HashMap<>();

  public Environment(Environment parent) {
    this.parent = parent;
  }

  public Environment() {
    this(null);
  }

  public String evaluate(FormatString formatString) {
    return formatString.evaluate(this);
  }

  public String get(String key) {
    FormatString value = vars.get(key);
    if (value == null) {
      Supplier<String> supplier = computedVars.get(key);
      if (supplier != null) {
        return supplier.get();
      }
    }
    if (value == null) {
      if (parent != null) {
        return parent.get(key);
      }
      // I would have thought that undefined variables should be an error, but
      // Ninja seems to be fine with it and just return an empty string.
      System.err.println("WARNING: Unknown variable ${" + key + "}");
      return "";
    }
    return value.evaluate(this);
  }

  public void put(String key, FormatString value) {
    if (computedVars.containsKey(key)) {
      throw new IllegalArgumentException("Duplicate variable: ${" + key + "}");
    }
    if (vars.put(key, value) != null) {
      throw new IllegalArgumentException("Duplicate variable: ${" + key + "}");
    }
  }

  public void put(String key, Supplier<String> value) {
    if (vars.containsKey(key)) {
      throw new IllegalArgumentException("Duplicate variable: ${" + key + "}");
    }
    if (computedVars.put(key, value) != null) {
      throw new IllegalArgumentException("Duplicate variable: ${" + key + "}");
    }
  }

  public void add(Entry<String, FormatString> entry) {
    put(entry.getKey(), entry.getValue());
  }
}

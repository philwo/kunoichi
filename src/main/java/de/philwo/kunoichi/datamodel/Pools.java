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

import java.util.LinkedHashMap;

public final class Pools {

  private final LinkedHashMap<String, Pool> pools = new LinkedHashMap<>();

  public Pool get(String name) {
    Pool pool = pools.get(name);
    if (pool == null) {
      throw new IllegalArgumentException("Unknown pool: " + name);
    }
    return pool;
  }

  public void add(Pool pool) {
    if (pools.put(pool.name(), pool) != null) {
      throw new IllegalArgumentException("Duplicate pool: " + pool.name());
    }
  }
}

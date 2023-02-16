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

import de.philwo.kunoichi.utils.Preconditions;

public final class Rule {

  public static final Rule PHONY_RULE = Rule.builder().name("phony").build();

  private final String name;
  private final FormatString command;
  private final FormatString depFile;
  private final Deps deps;
  private final FormatString msvcDepsPrefix;
  private final FormatString description;
  private final FormatString dynDep;
  private final boolean generator;
  private final boolean restat;
  private final FormatString rspFile;
  private final FormatString rspFileContent;
  private final FormatString pool;

  public Rule(
      String name,
      FormatString command,
      FormatString depFile,
      Deps deps,
      FormatString msvcDepsPrefix,
      FormatString description,
      FormatString dynDep,
      boolean generator,
      boolean restat,
      FormatString rspFile,
      FormatString rspFileContent,
      FormatString pool) {
    this.name = Preconditions.checkNotNull(name);
    this.command = Preconditions.checkNotNull(command);
    this.depFile = Preconditions.checkNotNull(depFile);
    this.deps = Preconditions.checkNotNull(deps);
    this.msvcDepsPrefix = Preconditions.checkNotNull(msvcDepsPrefix);
    this.description = Preconditions.checkNotNull(description);
    this.dynDep = Preconditions.checkNotNull(dynDep);
    this.generator = generator;
    this.restat = restat;
    this.rspFile = Preconditions.checkNotNull(rspFile);
    this.rspFileContent = Preconditions.checkNotNull(rspFileContent);
    this.pool = Preconditions.checkNotNull(pool);
  }

  public static RuleBuilder builder() {
    return new RuleBuilder();
  }

  public enum Deps {
    NONE,
    GCC,
    MSVC
  }

  public String name() {
    return name;
  }

  public FormatString command() {
    return command;
  }

  public FormatString depFile() {
    return depFile;
  }

  public Deps deps() {
    return deps;
  }

  public FormatString msvcDepsPrefix() {
    return msvcDepsPrefix;
  }

  public FormatString description() {
    return description;
  }

  public FormatString dynDep() {
    return dynDep;
  }

  public boolean generator() {
    return generator;
  }

  public boolean restat() {
    return restat;
  }

  public FormatString rspFile() {
    return rspFile;
  }

  public FormatString rspFileContent() {
    return rspFileContent;
  }

  public FormatString pool() {
    return pool;
  }

  public static class RuleBuilder {
    private String name;
    private FormatString command = FormatString.EMPTY;
    private FormatString depFile = FormatString.EMPTY;
    private Deps deps = Deps.NONE;
    private FormatString msvcDepsPrefix = FormatString.EMPTY;
    private FormatString description = FormatString.EMPTY;
    private FormatString dynDep = FormatString.EMPTY;
    private boolean generator = false;
    private boolean restat = false;
    private FormatString rspFile = FormatString.EMPTY;
    private FormatString rspFileContent = FormatString.EMPTY;
    private FormatString pool = FormatString.EMPTY;

    public RuleBuilder name(String name) {
      this.name = Preconditions.checkNotNull(name);
      return this;
    }

    public RuleBuilder command(FormatString command) {
      this.command = Preconditions.checkNotNull(command);
      return this;
    }

    public RuleBuilder depFile(FormatString depFile) {
      this.depFile = Preconditions.checkNotNull(depFile);
      return this;
    }

    public RuleBuilder deps(Deps deps) {
      this.deps = Preconditions.checkNotNull(deps);
      return this;
    }

    public RuleBuilder msvcDepsPrefix(FormatString msvcDepsPrefix) {
      this.msvcDepsPrefix = Preconditions.checkNotNull(msvcDepsPrefix);
      return this;
    }

    public RuleBuilder description(FormatString description) {
      this.description = Preconditions.checkNotNull(description);
      return this;
    }

    public RuleBuilder dynDep(FormatString dynDep) {
      this.dynDep = Preconditions.checkNotNull(dynDep);
      return this;
    }

    public RuleBuilder generator(boolean generator) {
      this.generator = generator;
      return this;
    }

    public RuleBuilder restat(boolean restat) {
      this.restat = restat;
      return this;
    }

    public RuleBuilder rspFile(FormatString rspFile) {
      this.rspFile = Preconditions.checkNotNull(rspFile);
      return this;
    }

    public RuleBuilder rspFileContent(FormatString rspFileContent) {
      this.rspFileContent = Preconditions.checkNotNull(rspFileContent);
      return this;
    }

    public RuleBuilder pool(FormatString pool) {
      this.pool = Preconditions.checkNotNull(pool);
      return this;
    }

    public Rule build() {
      if (name == null || name.isEmpty()) {
        throw new IllegalStateException("name must be set");
      }
      if ((!rspFile.isEmpty() && rspFileContent.isEmpty())
          || (rspFile.isEmpty() && !rspFileContent.isEmpty())) {
        throw new IllegalStateException("rspfile and rspfile_content must be set together");
      }
      return new Rule(
          name,
          command,
          depFile,
          deps,
          msvcDepsPrefix,
          description,
          dynDep,
          generator,
          restat,
          rspFile,
          rspFileContent,
          pool);
    }
  }
}

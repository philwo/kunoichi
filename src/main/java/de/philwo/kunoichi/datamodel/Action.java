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
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/** An action is a piece of work that has a set of inputs and outputs. */
public class Action {

  private final Rule rule;
  private final Environment env;

  /**
   * Explicit outputs are used to expand the ${out} variable in the command line of the rule when
   * executing the action.
   */
  private final List<Path> explicitOutputs;

  /** Implicit outputs are not part of the command line, but otherwise treated the same. */
  private final List<Path> implicitOutputs;

  /**
   * Explicit inputs are used to expand the ${in} variable in the command line of the rule when
   * executing the action.
   */
  private final List<Path> explicitInputs;

  /** Implicit inputs are not part of the command line, but otherwise treated the same. */
  private final List<Path> implicitInputs;

  /**
   * Order-only inputs are guaranteed to be built before the action is executed, but will not cause
   * the action to be re-executed if they change.
   */
  private final List<Path> orderOnlyInputs;

  /**
   * Validations listed on the build line cause the specified files to be added to the top level of
   * the build graph (as if they were specified on the Ninja command line) whenever the build line
   * is a transitive dependency of one of the targets specified on the command line or a default
   * target.
   */
  private final List<Path> validations;

  public Action(
      Rule rule,
      Environment env,
      List<Path> explicitOutputs,
      List<Path> implicitOutputs,
      List<Path> explicitInputs,
      List<Path> implicitInputs,
      List<Path> orderOnlyInputs,
      List<Path> validations) {
    this.rule = Preconditions.checkNotNull(rule);
    this.env = Preconditions.checkNotNull(env);
    this.explicitOutputs = explicitOutputs;
    this.implicitOutputs = implicitOutputs;
    this.explicitInputs = explicitInputs;
    this.implicitInputs = implicitInputs;
    this.orderOnlyInputs = orderOnlyInputs;
    this.validations = validations;
  }

  /** A short mnemonic that identifies this action. */
  public String mnemonic() {
    return rule.name();
  }

  /** The command that is executed by this action. */
  public String command() {
    return env.evaluate(rule.command());
  }

  /**
   * Outputs are the files that are created by this action.
   *
   * <p>Before executing an action, all of its outputs are deleted in case they already exist. After
   * executing an action, we check that all outputs exist and otherwise fail the build.
   */
  public Stream<Path> outputs() {
    Stream<Path> outputs = explicitOutputs.stream();
    if (!implicitOutputs.isEmpty()) {
      outputs = Stream.concat(outputs, implicitOutputs.stream());
    }
    return outputs;
  }

  /**
   * Inputs are the files that are read by this action.
   *
   * <p>Before executing an action, we ensure that all inputs exist, either by running the actions
   * that generate them, or verifying that the files are available on disk. In case of a missing
   * input, we fail the build.
   */
  public Stream<Path> inputs() {
    Stream<Path> inputs = explicitInputs.stream();
    if (!implicitInputs.isEmpty()) {
      inputs = Stream.concat(inputs, implicitInputs.stream());
    }
    if (!orderOnlyInputs.isEmpty()) {
      inputs = Stream.concat(inputs, orderOnlyInputs.stream());
    }
    return inputs;
  }

  public Environment env() {
    return env;
  }

  public boolean hasRspFile() {
    return !rule.rspFile().isEmpty();
  }

  public Path rspFile() {
    return Path.of(env.evaluate(rule.rspFile()));
  }

  public String rspFileContent() {
    return env.evaluate(rule.rspFileContent());
  }

  public Rule rule() {
    return rule;
  }
}

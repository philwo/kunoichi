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

package de.philwo.kunoichi;

import de.philwo.kunoichi.build.BuildCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "kn",
    description = "Kunoichi is a Ninja-compatible build system",
    subcommands = {BuildCommand.class},
    mixinStandardHelpOptions = true)
public class Main {
  public static void main(String[] args) {
    int exitCode = new CommandLine(new Main()).execute(args);
    System.exit(exitCode);
  }
}

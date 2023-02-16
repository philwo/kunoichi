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

package de.philwo.kunoichi.build;

import de.philwo.kunoichi.datamodel.Action;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class LocalSpawnStrategy {

  private final Set<Path> createdDirectories = Collections.synchronizedSet(new HashSet<>());

  public void spawn(Action action) {
    String command = action.command();
    System.err.println("[" + action.mnemonic() + "]: " + command);
    ensureFilesExist(action.inputs(), "Input file missing");
    ensureOutputDirectoriesExist(action);
    try {
      try {
        if (action.hasRspFile()) {
          Files.writeString(action.rspFile(), action.rspFileContent());
        }
        runCommand(command);
      } finally {
        if (action.hasRspFile()) {
          Files.delete(action.rspFile());
        }
      }
    } catch (IOException e) {
      throw new CommandFailedException("I/O error during command execution: " + e.getMessage(), e);
    }
    ensureFilesExist(action.outputs(), "Command did not produce expected output");
  }

  private void runCommand(String command) throws IOException {
    ProcessBuilder pb = new ProcessBuilder();
    pb.inheritIO();
    pb.command("/bin/sh", "-c", command);
    int exitCode;
    try {
      exitCode = pb.start().waitFor();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CommandFailedException("Interrupted during command execution", e);
    }
    if (exitCode != 0) {
      throw new CommandFailedException("Command failed with exit code" + exitCode);
    }
  }

  private void ensureFilesExist(Stream<Path> target, String errorMsg) {
    target
        .filter(Files::notExists)
        .findAny()
        .ifPresent(
            path -> {
              throw new CommandFailedException("%s: %s".formatted(errorMsg, path));
            });
  }

  private void ensureOutputDirectoriesExist(Action target) {
    target
        .outputs()
        .map(Path::getParent)
        .filter(path -> path != null)
        .filter(path -> createdDirectories.add(path))
        .forEach(
            path -> {
              try {
                Files.createDirectories(path);
              } catch (IOException e) {
                throw new CommandFailedException("Failed to create directory: " + path, e);
              }
            });
  }

  private static class CommandFailedException extends RuntimeException {
    public CommandFailedException(String message) {
      super(message);
    }

    public CommandFailedException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}

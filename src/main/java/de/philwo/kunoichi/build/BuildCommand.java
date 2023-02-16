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
import de.philwo.kunoichi.datamodel.ActionGraph;
import de.philwo.kunoichi.ninja.NinjaFile;
import de.philwo.kunoichi.utils.Preconditions;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "build", description = "Build target(s)")
public class BuildCommand implements Callable<Integer> {

  private final FileSystem fs;
  private final LocalSpawnStrategy spawnStrategy;

  @Parameters(paramLabel = "<target>", description = "target(s) to build")
  private List<Path> targetsToBuild;

  @Option(
      names = {"-j", "--jobs"},
      description = "number of parallel jobs")
  private int jobs = Runtime.getRuntime().availableProcessors();

  @Option(
      names = {"-n", "--nobuild"},
      description = "skip execution phase")
  private boolean noBuild = false;

  public BuildCommand() {
    fs = FileSystems.getDefault();
    spawnStrategy = new LocalSpawnStrategy();
  }

  @Override
  public Integer call() throws Exception {
    NinjaFile ninjaFile = load();
    if (targetsToBuild == null || targetsToBuild.isEmpty()) {
      targetsToBuild = ninjaFile.defaultTargets().items();
    }
    ActionGraph actionGraph = analyze(ninjaFile);
    if (!noBuild) {
      execute(actionGraph);
    }
    return 0;
  }

  private NinjaFile load() throws IOException {
    Instant start = Instant.now();
    System.err.println("Loading...");
    try {
      return NinjaFile.parse(Path.of("build.ninja"));
    } finally {
      long timeElapsed = Duration.between(start, Instant.now()).toMillis();
      System.err.println("Parsing took " + timeElapsed + "ms.");
    }
  }

  private ActionGraph analyze(NinjaFile ninjaFile) {
    System.err.println("Analyzing...");
    Instant start = Instant.now();
    try {
      ActionGraph actionGraph = new ActionGraph(ninjaFile.actions().size());
      ninjaFile.actions().stream().forEach(action -> actionGraph.addAction(action));
      ninjaFile.actions().stream()
          .forEach(
              action ->
                  action
                      .inputs()
                      .forEach(
                          input -> {
                            Action generatingAction = actionGraph.getGeneratingAction(input);
                            if (generatingAction != null) {
                              actionGraph.putEdge(action, generatingAction);
                            } else {
                              if (!Files.exists(input)) {
                                throw new IllegalArgumentException(
                                    "Build target for '" + input + "' not found.");
                              }
                            }
                          }));
      return actionGraph;
    } finally {
      long timeElapsed = Duration.between(start, Instant.now()).toMillis();
      System.err.println("Building the graph took " + timeElapsed + "ms.");
    }
  }

  private void execute(ActionGraph buildGraph) throws ExecutionException, InterruptedException {
    Instant start = Instant.now();
    System.err.println("Executing...");
    try {
      HashMap<Action, CompletableFuture<Void>> futures = new HashMap<>();
      HashSet<Action> temporaryMarks = new HashSet<>();
      CompletableFuture<Void> result =
          CompletableFuture.allOf(
              targetsToBuild.stream()
                  .map(buildGraph::getGeneratingAction)
                  .peek(Preconditions::checkNotNull)
                  .map(target -> visitAsync(target, buildGraph, futures, temporaryMarks))
                  .toArray(CompletableFuture[]::new));
      result.get();
    } finally {
      long timeElapsed = Duration.between(start, Instant.now()).toMillis();
      System.err.println("Execution took " + timeElapsed + "ms.");
    }
  }

  private CompletableFuture<Void> visitAsync(
      Action action,
      ActionGraph actionGraph,
      HashMap<Action, CompletableFuture<Void>> futures,
      HashSet<Action> temporaryMarks) {
    CompletableFuture<Void> future = futures.get(action);
    if (future != null) {
      return future;
    }
    if (!temporaryMarks.add(action)) {
      throw new IllegalArgumentException("Cycle detected in build graph.");
    }
    future =
        CompletableFuture.allOf(
                actionGraph.getDependencies(action).stream()
                    .map(
                        predecessor ->
                            visitAsync(predecessor, actionGraph, futures, temporaryMarks))
                    .toArray(CompletableFuture[]::new))
            .thenRunAsync(() -> spawnStrategy.spawn(action));
    temporaryMarks.remove(action);
    futures.put(action, future);
    return future;
  }
}

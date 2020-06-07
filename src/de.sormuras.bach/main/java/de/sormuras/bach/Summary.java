/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach;

import de.sormuras.bach.internal.Logbook;
import de.sormuras.bach.internal.Paths;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/** A build summary. */
public class Summary {

  private final Bach bach;
  private final Logbook logbook;

  public Summary(Bach bach) {
    this.bach = bach;
    this.logbook = (Logbook) bach.getLogger();
  }

  /** Return silently if everything's okay, throwing an {@link AssertionError} otherwise. */
  public Summary assertSuccessful() {
    var entries = logbook.entries(Level.WARNING).collect(Collectors.toList());
    if (entries.isEmpty()) return this;
    var lines = new StringJoiner(System.lineSeparator());
    lines.add(String.format("Collected %d error(s)", entries.size()));
    for (var entry : entries) lines.add("\t- " + entry.message());
    lines.add("");
    lines.add(String.join(System.lineSeparator(), toMarkdown()));
    var error = new AssertionError(lines.toString());
    for (var entry : entries) if (entry.exception() != null) error.addSuppressed(entry.exception());
    throw error;
  }

  public Summary printModuleStats() {
    var project = bach.getProject();
    var directory = project.base().modules(project.realms().get(0).name());
    return printModuleStats(System.out::println, directory);
  }

  public Summary printModuleStats(Consumer<String> printer, Path directory) {
    var uri = directory.toUri().toString();
    var files = Paths.list(directory, Files::isRegularFile);
    printer.accept(String.format("Directory %s contains", uri));
    try {
      for (var file : files) {
        printer.accept(String.format("%s [%,d bytes]", file.getFileName(), Files.size(file)));
        if (!Paths.isJarFile(file)) continue;
        var string = new StringWriter();
        var writer = new PrintWriter(string);
        var jar = ToolProvider.findFirst("jar").orElseThrow();
        jar.run(writer, writer, "--describe-module", "--file", file.toString());
        var trim = string.toString().trim().replace(uri, "${DIRECTORY}");
        printer.accept(trim.replaceAll("(?m)^", "\t"));
      }
    } catch (Exception e) {
      throw new AssertionError("Analyzing JAR files failed", e);
    }
    return this;
  }

  /** Write summary as markdown */
  public void writeMarkdown(Path file, boolean createCopyWithTimestamp) {
    var markdown = toMarkdown();
    try {
      Files.createDirectories(file.getParent());
      Files.write(file, markdown);
      if (createCopyWithTimestamp) {
        @SuppressWarnings("SpellCheckingInspection")
        var pattern = "yyyyMMdd_HHmmss";
        var formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneOffset.UTC);
        var timestamp = formatter.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        var summaries = Files.createDirectories(file.resolveSibling("summaries"));
        Files.copy(file, summaries.resolve("summary-" + timestamp + ".md"));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public List<String> toMarkdown() {
    var md = new ArrayList<String>();
    md.add("# Summary for " + bach.getProject().toTitleAndVersion());
    md.addAll(projectDescription());
    md.addAll(toolCallOverview());
    md.addAll(logbookEntries());
    return md;
  }

  private List<String> projectDescription() {
    var md = new ArrayList<String>();
    var project = bach.getProject();
    md.add("");
    md.add("## Project");
    md.add("- title: " + project.info().title());
    md.add("- version: " + project.info().version());
    md.add("");
    md.add("```text");
    md.addAll(project.toStrings());
    md.add("```");
    return md;
  }

  private List<String> toolCallOverview() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Tool Call Overview");
    md.add("|    |Thread|Duration|Tool|Arguments");
    md.add("|----|-----:|-------:|----|---------");
    md.addAll(logbook.tools());
    return md;
  }

  private List<String> logbookEntries() {
    var md = new ArrayList<String>();
    md.add("");
    md.add("## Logbook");
    for (var entry : logbook.entries(Level.ALL).collect(Collectors.toList())) {
      md.add("- " + entry.level());
      var one = entry.message().lines().count() == 1;
      md.add((one ? "`" : "```text\n") + entry.message() + (one ? "`" : "\n```"));
    }
    return md;
  }
}

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

package de.sormuras.bach.project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/** A source directory potentially targeting a specific Java SE release. */
public final class SourceDirectory {

  private static final Pattern RELEASE_PATTERN = Pattern.compile(".*?(\\d+)$");

  public static SourceDirectory of(Path path) {
    if (Files.isRegularFile(path)) throw new IllegalArgumentException("Not a directory: " + path);
    var file = path.normalize().getFileName();
    var name = file != null ? file : path.toAbsolutePath().getFileName();
    return new SourceDirectory(path, parseRelease(name.toString()));
  }

  static int parseRelease(String name) {
    if (name == null || name.isEmpty()) return 0;
    var matcher = RELEASE_PATTERN.matcher(name);
    return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
  }

  private final Path path;
  private final int release;

  /**
   * Initialize this source directory instance with the given components.
   *
   * @param path The path to this source directory, usually relative to project's base directory
   * @param release The Java SE release to compile sources for
   */
  public SourceDirectory(Path path, int release) {
    this.path = path;
    this.release = release;
  }

  public Path path() {
    return path;
  }

  public int release() {
    return release;
  }

  public boolean isTargeted() {
    return release != 0;
  }

  public boolean isModuleInfoJavaPresent() {
    return Files.isRegularFile(path.resolve("module-info.java"));
  }
}

/*
 * Bach - Java Shell Builder
 * Copyright (C) 2019 Christian Stein
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

package it;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.sormuras.bach.Bach;
import de.sormuras.bach.Call;
import de.sormuras.bach.Task;
import de.sormuras.bach.project.Folder;
import de.sormuras.bach.project.Library;
import de.sormuras.bach.project.Project;
import de.sormuras.bach.project.Realm;
import de.sormuras.bach.project.Structure;
import de.sormuras.bach.project.Unit;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BachTests {
  @Test
  void moduleDescriptorParsesVersion() {
    var pattern = DateTimeFormatter.ofPattern("yyyy.MM.dd.HHmmss").withZone(ZoneId.of("UTC"));
    assertDoesNotThrow(() -> Version.parse(pattern.format(Instant.now())));
    assertThrows(IllegalArgumentException.class, () -> Version.parse(""));
    assertThrows(IllegalArgumentException.class, () -> Version.parse("-"));
    assertThrows(IllegalArgumentException.class, () -> Version.parse("master"));
    assertThrows(IllegalArgumentException.class, () -> Version.parse("ea"));
  }

  @Test
  void executeRuntimeExceptionThrowingTaskIsReportedAsAnError() {
    var exception = new RuntimeException("!");
    class RuntimeExceptionThrowingTask implements Task {
      @Override
      public void execute(Bach bach) {
        throw exception;
      }
    }

    var structure = new Structure(Folder.of(), Library.of(), List.of(), List.of());
    var project = new Project("zero", Version.parse("0"), structure, null);
    var log = new Log();
    var bach = new Bach(log, project);

    var error = assertThrows(Error.class, () -> bach.execute(new RuntimeExceptionThrowingTask()));
    assertSame(exception, error.getCause());
    assertEquals("Task failed to execute: java.lang.RuntimeException: !", error.getMessage());
  }

  @Test
  void executeNonZeroToolProviderIsReportedAsAnError() {
    var structure = new Structure(Folder.of(), Library.of(), List.of(), List.of());
    var project = new Project("zero", Version.parse("0"), structure, null);
    var log = new Log();
    var bach = new Bach(log, project);

    var error = assertThrows(Error.class, () -> bach.execute(new Call("javac", "*")));
    assertEquals(
        "Call exited with non-zero status code: 2 <- Call{name='javac', arguments=[*]}",
        error.getMessage());
    assertLinesMatch(List.of("Bach.java (.+) initialized.", "| javac(*)"), log.lines());
    assertLinesMatch(
        List.of(
            "error: invalid flag: *",
            "Usage: javac <options> <source files>",
            "use --help for a list of possible options"),
        log.errors());
  }

  @Test
  void buildProjectInEmptyDirectoryThrowsError(@TempDir Path temp) {
    var main = new Realm("main", Set.of(), List.of(), List.of(), Map.of());
    var unit = unit(main, "unit", 0, Unit.Type.JIGSAW);
    var structure = new Structure(Folder.of(temp), Library.of(), List.of(main), List.of(unit));
    var project = new Project("empty", Version.parse("0"), structure, null);
    var log = new Log();
    var bach = new Bach(log, project);

    var error = assertThrows(Error.class, () -> bach.execute(Task.build()));
    assertEquals("Base directory is empty: " + temp.toUri(), error.getMessage());
  }

  static Unit unit(Realm realm, String name, int version, Unit.Type type) {
    var info = Path.of("module-info.java");
    return new Unit(realm, descriptor(name, version), info, type, List.of(), List.of(), List.of());
  }

  static ModuleDescriptor descriptor(String name, int version) {
    return ModuleDescriptor.newModule(name).version("" + version).build();
  }
}

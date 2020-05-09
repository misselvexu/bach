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

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

/**
 * Bach's own build program.
 *
 * <p>Uses single-file source-code {@code Bach.java} to build module {@code de.sormuras.bach}.
 */
class Build {

  public static void main(String... args) {
    var base = Bach.Project.Base.of();
    var library = Bach.Project.Library.of();
    var realms = List.of(mainRealm(base), testRealm(base));
    var project =
        new Bach.Project(
            Bach.Project.Base.of(),
            new Bach.Project.Info(
                "\uD83C\uDFBC Java Shell Builder - Build modular Java projects with JDK tools",
                Bach.VERSION),
            new Bach.Project.Structure(library, realms));
    var bach = Bach.of(project);
    bach.build().assertSuccessful();
  }

  static Bach.Project.Realm mainRealm(Bach.Project.Base base) {
    var units =
        List.of(
            new Bach.Project.Unit(
                Bach.Modules.describe(Path.of("src/de.sormuras.bach/main/java/module-info.java")),
                List.of(
                    createJar(
                        base.modules("main").resolve("de.sormuras.bach.jar"),
                        base.classes("main", "de.sormuras.bach")))));
    var moduleNames = units.stream().map(Bach.Project.Unit::name).collect(Collectors.joining(","));
    var moduleSourcePath = "src/*/main/java".replace('/', File.separatorChar);
    var javac =
        runTool(
            "javac",
            "--module",
            moduleNames,
            "--module-source-path",
            moduleSourcePath,
            "-encoding",
            "UTF-8",
            "-parameters",
            "-Werror",
            "-X" + "lint",
            "-d",
            base.classes("main"));
    var javadoc =
        Bach.Task.sequence(
            "Create API documentation",
            List.of(
                new Bach.Task.CreateDirectories(base.api()),
                runTool(
                    "javadoc",
                    "--module",
                    moduleNames,
                    "--module-source-path",
                    moduleSourcePath,
                    "-quiet",
                    "-encoding",
                    "UTF-8",
                    "-locale",
                    "en",
                    "-Xdoclint:-missing", // TODO Add missing javadoc elements.
                    "-link",
                    "https://docs.oracle.com/en/java/javase/11/docs/api",
                    "-d",
                    base.api())));
    var jlink =
        Bach.Task.sequence(
            "Create custom runtime image",
            List.of(
                new Bach.Task.DeleteDirectories(base.image()),
                runTool(
                    "jlink",
                    "--launcher",
                    "bach=de.sormuras.bach/de.sormuras.bach.Main",
                    "--add-modules",
                    moduleNames,
                    "--module-path",
                    base.modules("main"),
                    "--compress",
                    "2",
                    "--strip-debug",
                    "--no-header-files",
                    "--no-man-pages",
                    "--output",
                    base.image())));
    return new Bach.Project.Realm("main", units, javac, List.of(javadoc, jlink));
  }

  static Bach.Project.Realm testRealm(Bach.Project.Base base) {
    var units =
        List.of(
            new Bach.Project.Unit(
                Bach.Modules.describe(Path.of("src/test.base/test/java/module-info.java")),
                List.of(
                    createJar(
                        base.modules("test").resolve("test.base.jar"),
                        base.classes("test", "test.base")))));
    var moduleNames = units.stream().map(Bach.Project.Unit::name).collect(Collectors.joining(","));
    var moduleSourcePath = "src/*/test/java".replace('/', File.separatorChar);
    var javac =
        runTool(
            "javac",
            "--module",
            moduleNames,
            "--module-source-path",
            moduleSourcePath,
            "--module-path",
            base.directory().resolve("lib"),
            "-encoding",
            "UTF-8",
            "-parameters",
            "-Werror",
            "-X" + "lint",
            "-d",
            base.classes("test"));
    return new Bach.Project.Realm("test", units, javac, List.of());
  }

  static Bach.Task runTool(String name, Object... arguments) {
    var tool = ToolProvider.findFirst(name).orElseThrow();
    var args = new String[arguments.length];
    for (int i = 0; i < args.length; i++) args[i] = arguments[i].toString();
    return new Bach.Task.RunTool(tool, args);
  }

  static Bach.Task createJar(Path jar, Path classes) {
    var jarCreate = new Bach.Jar();
    jarCreate.getAdditionalArguments().add("--create").add("--file", jar).add("-C", classes, ".");
    var jarDescribe = new Bach.Jar();
    jarDescribe.getAdditionalArguments().add("--describe-module").add("--file", jar);
    return Bach.Task.sequence(
        "Create modular JAR file " + jar.getFileName(),
        new Bach.Task.CreateDirectories(jar.getParent()),
        jarCreate.toolTask(),
        jarDescribe.toolTask());
  }
}

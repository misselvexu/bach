package com.github.sormuras.bach.api;

import com.github.sormuras.bach.Command;
import com.github.sormuras.bach.Logbook;
import com.github.sormuras.bach.project.Flag;
import com.github.sormuras.bach.project.ModuleDeclaration;
import com.github.sormuras.bach.project.ModuleDeclarations;
import com.github.sormuras.bach.project.SourceFolder;
import com.github.sormuras.bach.tool.Jar;
import com.github.sormuras.bach.tool.Javac;
import com.github.sormuras.bach.util.Paths;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;

/** Methods related to building and running test modules. */
public interface ProjectBuildTestSpaceAPI extends API {

  /**
   * Build modules of the test code space and launch JUnit Platform for each of them.
   *
   * <ul>
   *   <li>{@code javac}
   *   <li>{@code jar}
   *   <li>{@code test}
   *   <li>{@code junit}
   * </ul>
   */
  default void buildProjectTestSpace() {
    var test = bach().project().spaces().test();
    var modules = test.declarations();
    if (modules.isEmpty()) {
      log("Test module list is empty, nothing to build here.");
      return;
    }
    var s = modules.size() == 1 ? "" : "s";
    say("Build %d test module%s: %s", modules.size(), s, modules.toNames(", "));

    var feature = Runtime.version().feature();
    var testClasses = bach().folders().workspace("classes-test-" + feature);
    var testModules = bach().folders().workspace("modules-test");

    Paths.deleteDirectories(testModules);
    bach().run(buildProjectTestJavac(testClasses)).requireSuccessful();

    Paths.createDirectories(testModules);
    var jars =
        modules.map().values().stream()
            .map(module -> buildProjectTestJar(testModules, module, testClasses));
    bach().run(jars).requireSuccessful();

    buildProjectTestRuns(testModules, modules);
  }

  default Javac buildProjectTestJavac(Path classes) {
    var main = bach().project().spaces().main();
    var mainModules = bach().folders().workspace("modules");
    var test = bach().project().spaces().test();
    var tests = test.declarations();
    return Command.javac()
        .add("--module", tests.toNames(","))
        .forEach(
            tests.toModuleSourcePaths(false),
            (javac, path) -> javac.add("--module-source-path", path))
        .add("--module-path", List.of(mainModules, bach().folders().externalModules()))
        .forEach(
            tests.toModulePatches(main.declarations()).entrySet(),
            (javac, patch) -> javac.add("--patch-module", patch.getKey() + '=' + patch.getValue()))
        .addAll(test.tweaks().arguments("javac"))
        .add("-d", classes);
  }

  default String buildProjectTestJarFileName(String module) {
    return module + '@' + bach().project().versionNumberAndPreRelease() + "+test.jar";
  }

  default Jar buildProjectTestJar(Path testModules, ModuleDeclaration declaration, Path classes) {
    var name = declaration.name();
    var project = bach().project();
    var test = project.spaces().test();
    var jar =
        Command.jar()
            .ifTrue(bach().is(Flag.VERBOSE), command -> command.add("--verbose"))
            .add("--create")
            .add("--file", testModules.resolve(buildProjectTestJarFileName(name)))
            .addAll(test.tweaks().arguments("jar"))
            .addAll(test.tweaks().arguments("jar(" + name + ")"));
    var baseClasses = classes.resolve(name);
    jar = jar.add("-C", baseClasses, ".");
    // include base test resources
    for (var folder : declaration.resources().list()) {
      if (folder.isTargeted()) continue; // handled later
      jar = jar.add("-C", folder.path(), ".");
    }
    // include targeted test resources in ascending order
    for (int release = 9; release <= Runtime.version().feature(); release++) {
      var paths = new ArrayList<Path>();
      declaration.resources().stream(release).map(SourceFolder::path).forEach(paths::add);
      if (paths.isEmpty()) continue;
      jar = jar.add("--release", release);
      for (var path : paths) jar = jar.add("-C", path, ".");
    }
    return jar;
  }

  default void buildProjectTestRuns(Path testModules, ModuleDeclarations modules) {
    var tools = bach().project().settings().tools();
    var testsEnabled = tools.enabled("test");
    var junitEnabled = tools.enabled("junit");
    var junitPresent = bach().computeToolProvider("junit").isPresent();

    if (!testsEnabled && !junitEnabled) {
      log("Test runs are disabled");
      return;
    }

    say("Execute each test module");
    var runs = new ArrayList<Logbook.Run>();
    for (var name : modules.toNames().toList()) {
      log("Test module %s", name);
      // "test"
      var finder = buildProjectTestModuleFinder(testModules, name);
      if (testsEnabled)
        bach()
            .computeToolProviders(finder, true, name)
            .filter(provider -> provider.getClass().getModule().getName().equals(name))
            .filter(provider -> provider.name().equals("test"))
            .map(this::buildProjectTestRun)
            .forEach(runs::add);
      // "junit"
      if (junitEnabled && junitPresent) runs.add(buildProjectTestJUnitRun(testModules, name));
    }

    new Logbook.Runs(runs).requireSuccessful();
  }

  default ModuleFinder buildProjectTestModuleFinder(Path testModules, String module) {
    return ModuleFinder.of(
        testModules.resolve(buildProjectTestJarFileName(module)), // module under test
        bach().folders().workspace("modules"), // main modules
        testModules, // (more) test modules
        bach().folders().externalModules());
  }

  private Logbook.Run buildProjectTestRun(ToolProvider provider, String... args) {
    var providerClass = provider.getClass();
    var description = providerClass.getModule().getName() + "/" + providerClass.getName();
    return bach().run(provider, List.of(args), description);
  }

  default Logbook.Run buildProjectTestJUnitRun(Path testModules, String module) {
    var finder = buildProjectTestModuleFinder(testModules, module);
    var junit =
        Command.of("junit")
            .add("--select-module", module)
            .addAll(bach().project().spaces().test().tweaks().arguments("junit"))
            .addAll(bach().project().spaces().test().tweaks().arguments("junit(" + module + ")"))
            .add("--reports-dir", bach().folders().workspace("reports", "junit", module));

    return bach().run(junit, finder, module);
  }
}

package run.demo;

import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import jdk.jfr.consumer.RecordingStream;
import run.bach.ModuleLocator;
import run.bach.ModuleResolver;
import run.bach.ToolFinder;
import run.bach.workflow.Folders;
import run.info.bach.JavaFX;
import run.info.org.jreleaser.JReleaser;
import run.info.org.junit.JUnit;

public class ModuleResolverDemo {
  public static void main(String... args) throws Exception {
    var libraries = ModuleLocator.compose(JUnit.modules(), JavaFX.version("22.0.1"));

    var lib = Path.of("lib");
    try (var recording = new RecordingStream()) {
      recording.onEvent("run.bach.ModuleResolverResolvedModule", System.out::println);
      recording.startAsync();
      var resolver = ModuleResolver.ofSingleDirectory(lib, libraries);
      resolver.resolveModule("org.junit.jupiter"); // to write and discover tests
      resolver.resolveModule("org.junit.platform.console"); // to run tests
      resolver.resolveMissingModules();
      recording.stop();
    }

    // "jreleaser" via the tool provider SPI
    var jreleaserHome = Folders.ofCurrentWorkingDirectory().tool("jreleaser@" + JReleaser.VERSION);
    var jreleaserResolver = ModuleResolver.ofSingleDirectory(jreleaserHome, JReleaser.MODULES);
    jreleaserResolver.resolveModule("org.jreleaser.tool");
    jreleaserResolver.resolveMissingModules();

    var tools =
        ToolFinder.compose(
            ToolFinder.of("jar"), // provides "jar" tool
            ToolFinder.of("java"), // provides "java" tool
            ToolFinder.of(ModuleFinder.of(lib)), // provides "junit" tool
            ToolFinder.of(ModuleFinder.of(jreleaserHome)), // provides "jreleaser" tool
            ToolFinder.ofInstaller().withJavaApplication("demo/release@uri", JReleaser.URI));

    var junit = tools.get("junit");
    junit.run("--version");
    junit.run("engines");

    tools.get("jreleaser").run("--version");
    tools.get("release@uri").run("--version");
  }
}
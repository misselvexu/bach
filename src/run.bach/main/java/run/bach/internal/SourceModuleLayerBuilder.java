package run.bach.internal;

import java.lang.module.ModuleFinder;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.spi.ToolProvider;

public record SourceModuleLayerBuilder(Path source, Path target) {

  public SourceModuleLayerBuilder(Path source) {
    this(source, source.resolve("out/.bach/classes-" + Runtime.version().feature()));
  }

  public Path bin() {
    var source = SourceModuleLayerBuilder.class.getProtectionDomain().getCodeSource();
    var location = Path.of(URI.create(source.getLocation().toExternalForm()));
    return location.getParent();
  }

  public ModuleLayer build() {
    return build(ModuleLayer.boot(), ClassLoader.getSystemClassLoader());
  }

  public ModuleLayer build(ModuleLayer parentLayer, ClassLoader parentLoader) {
    compile();
    var beforeFinder = ModuleFinder.of(target);
    var afterFinder = ModuleFinder.of();
    var parentConfiguration = parentLayer.configuration();
    var newConfiguration = parentConfiguration.resolveAndBind(beforeFinder, afterFinder, roots());
    return parentLayer.defineModulesWithOneLoader(newConfiguration, parentLoader);
  }

  public Set<String> roots() {
    if (Files.notExists(source)) return Set.of();
    var roots = new TreeSet<String>();
    try (var stream = Files.newDirectoryStream(source, Files::isDirectory)) {
      for (var directory : stream) {
        if (Files.notExists(directory.resolve("module-info.java"))) continue;
        roots.add(directory.getFileName().toString());
      }
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
    return roots;
  }

  public void compile() {
    var roots = roots();
    if (roots.isEmpty()) return;
    var javac = ToolProvider.findFirst("javac").orElseThrow();
    var code =
        javac.run(
            System.out,
            System.err,
            "--module=" + String.join(",", roots),
            "--module-source-path=" + source,
            "--module-path=" + bin(),
            "-d",
            target.toString());
    if (code != 0) throw new RuntimeException("javac returned exit code: " + code);
  }
}

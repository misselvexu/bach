package run.bach.project;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/** Modular project model. */
public record Project(
    ProjectName name, ProjectVersion version, ProjectSpaces spaces, ProjectExternals externals) {

  /** {@return an {@code "unnamed 0-ea"} project with empty init, main, and test module spaces} */
  public static Project ofDefaults() {
    var name = new ProjectName("unnamed");
    var version = new ProjectVersion("0-ea", ZonedDateTime.now());
    var init = new ProjectSpace("init");
    var main = new ProjectSpace("main", "init");
    var test = new ProjectSpace("test", "main");
    var spaces = new ProjectSpaces(init, main, test);
    var externals = new ProjectExternals();
    return new Project(name, version, spaces, externals);
  }

  @SuppressWarnings("PatternVariableHidesField")
  private Project with(ProjectComponent component) {
    return new Project(
        component instanceof ProjectName name ? name : name,
        component instanceof ProjectVersion version ? version : version,
        component instanceof ProjectSpaces spaces ? spaces : spaces,
        component instanceof ProjectExternals externals ? externals : externals);
  }

  public Project with(ProjectSpace space) {
    return with(spaces.with(space));
  }

  /** {@return new project instance using the given string as the project name} */
  public Project withName(String string) {
    return with(new ProjectName(string));
  }

  /** {@return new project instance using the given string as the project version} */
  public Project withVersion(String string) {
    return with(version.with(string));
  }

  /** {@return new project instance using the given string as the project version date} */
  public Project withVersionDate(String string) {
    return with(version.withDate(string));
  }

  /**
   * {@return new project instance setting main space's Java release feature number to the integer
   * value of the given string}
   */
  public Project withTargetsJava(String string) {
    return withTargetsJava(Integer.parseInt(string));
  }

  /** {@return new project instance setting main space's Java release feature number} */
  public Project withTargetsJava(int release) {
    return withTargetsJava("main", release);
  }

  /** {@return new project instance setting specified space's Java release feature number} */
  public Project withTargetsJava(String space, int release) {
    return withTargetsJava(spaces.space(space), release);
  }

  /** {@return new project instance setting specified space's Java release feature number} */
  public Project withTargetsJava(ProjectSpace space, int release) {
    return with(space.withTargetsJava(release));
  }

  /** {@return new project instance setting main space's launcher} */
  public Project withLauncher(String launcher) {
    return withLauncher("main", launcher);
  }

  /** {@return new project instance setting specified space's launcher} */
  public Project withLauncher(String space, String launcher) {
    return withLauncher(spaces.space(space), launcher);
  }

  /** {@return new project instance setting specified space's launcher} */
  public Project withLauncher(ProjectSpace space, String launcher) {
    return with(space.withLauncher(launcher));
  }

  public Project withModule(String module) {
    return withModule("main", module);
  }

  public Project withModule(String space, String module) {
    return withModule(space, ModuleDescriptor.newModule(module).build());
  }

  public Project withModule(String space, ModuleDescriptor module) {
    return withModule(
        spaces.space(space),
        new DeclaredModule(
            Path.of(module.name()),
            Path.of(module.name(), "src", space, "java", "module-info.java"),
            module,
            DeclaredFolders.of(Path.of(module.name(), "src", space, "java")),
            Map.of()));
  }

  /** {@return new project instance with a new module declaration added to the specified space} */
  public Project withModule(String space, Path root, String info) {
    return withModule(spaces.space(space), DeclaredModule.of(root, Path.of(info)));
  }

  /** {@return new project instance with a new module declaration added to the specified space} */
  public Project withModule(ProjectSpace space, DeclaredModule module) {
    return with(spaces.with(space.withModules(space.modules().with(module))));
  }

  /** {@return new project instance with one or more additional modular dependences} */
  public Project withRequiresModule(String name, String... more) {
    return with(externals.withRequires(name).withRequires(more));
  }

  /**
   * {@return new project instance configured by finding {@code module-info.java} files below the
   * specified root directory matching the given {@link
   * java.nio.file.FileSystem#getPathMatcher(String) syntaxAndPattern}}
   */
  public Project withWalkingDirectory(Path directory, String syntaxAndPattern) {
    var project = this;
    var name = directory.normalize().toAbsolutePath().getFileName();
    if (name != null) project = project.withName(name.toString());
    var matcher = directory.getFileSystem().getPathMatcher(syntaxAndPattern);
    try (var stream = Files.find(directory, 9, (p, a) -> matcher.matches(p))) {
      for (var path : stream.toList()) {
        var uri = path.toUri().toString();
        if (uri.contains("/.bach/")) continue; // exclude project-local modules
        if (uri.matches(".*?/java-\\d+.*")) continue; // exclude non-base modules
        var module = DeclaredModule.of(directory, path);
        if (uri.contains("/init/")) {
          project = project.withModule(project.spaces().init(), module);
          continue;
        }
        if (uri.contains("/test/")) {
          project = project.withModule(project.spaces().test(), module);
          continue;
        }
        project = project.withModule(project.spaces().main(), module);
      }
    } catch (Exception exception) {
      throw new RuntimeException("Find with %s failed".formatted(syntaxAndPattern), exception);
    }
    return project;
  }

  /** {@return a list of all modules declared by this project} */
  public List<DeclaredModule> modules() {
    return spaces.list().stream().flatMap(space -> space.modules().list().stream()).toList();
  }
}

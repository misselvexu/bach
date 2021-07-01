package com.github.sormuras.bach.internal;

import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.RequiresTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.InvalidModuleDescriptorException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.spi.ToolProvider;

/** {@link ModuleDescriptor}-related utilities. */
public class ModuleDescriptors {

  private static final Map<Path, ModuleDescriptor> DESCRIPTORS = new ConcurrentHashMap<>();

  /**
   * Reads the source form of a module declaration from a file as a module descriptor.
   *
   * @param info the path to a {@code module-info.java} file to parse
   * @return the module descriptor
   * @implNote For the time being, only the {@code name} of a module and its {@code requires}
   *     directives are parsed.
   */
  public static ModuleDescriptor parse(Path info) {
    if (!Path.of("module-info.java").equals(info.getFileName()))
      throw new IllegalArgumentException("Path must end with 'module-info.java': " + info);

    var javac = ToolProvider.findFirst("javac").orElseThrow();
    var text = new StringWriter();
    var writer = new PrintWriter(text);
    javac.run(writer, writer, "-proc:only", "-Xplugin:" + Parser.NAME, info.toString());
    var descriptor = DESCRIPTORS.get(info);
    if (descriptor == null)
      throw new InvalidModuleDescriptorException("Descriptor is null: " + info + "\n" + text);
    return descriptor;
  }

  /** A {@code javac} plug-in that transforms module tree nodes into module descriptors. */
  public record Parser() implements Plugin, TaskListener {

    public static final String NAME = "ModuleDescriptors.Parser";

    @Override
    public String getName() {
      return NAME;
    }

    @Override
    public void init(JavacTask task, String... args) {
      task.addTaskListener(this);
    }

    @Override
    public void finished(TaskEvent event) {
      if (event.getKind() != TaskEvent.Kind.PARSE) return;
      var unit = event.getCompilationUnit();
      if (unit == null) return;
      var path = Path.of(unit.getSourceFile().getName());
      for (var declaration : unit.getTypeDecls()) {
        if (declaration instanceof ModuleTree module) DESCRIPTORS.put(path, describe(module));
      }
    }

    private ModuleDescriptor describe(ModuleTree module) {
      var moduleName = module.getName().toString();
      var builder = ModuleDescriptor.newModule(moduleName);
      for (var directive : module.getDirectives()) {
        if (directive instanceof RequiresTree requires) {
          var requiresModuleName = requires.getModuleName().toString();
          builder.requires(requiresModuleName);
        }
      }
      return builder.build();
    }
  }

  /** Hidden default constructor. */
  private ModuleDescriptors() {}
}
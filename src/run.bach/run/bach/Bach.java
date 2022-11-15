package run.bach;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import jdk.jfr.Recording;
import run.bach.internal.FlightRecorderEvent;
import run.bach.internal.LoadValidator;
import run.bach.internal.PathSupport;
import run.bach.internal.StringPrintWriterMirror;

public class Bach implements ToolRunner {

  @FunctionalInterface
  public interface Factory {
    Bach createBach(CLI cli, Printer printer, Recording recording);
  }

  public static Bach of(CLI cli, Printer printer, Recording recording) {
    var factory = ServiceLoader.load(Factory.class).findFirst().orElse(Bach::new);
    var bach = factory.createBach(cli, printer, recording);
    bach.debug("Initialized instance of " + bach.getClass());
    bach.debug(bach.toString(0));
    return bach;
  }

  private final CLI cli;
  private final Printer printer;
  private final Recording recording;
  private final Paths paths;
  private final Browser browser;
  private final ExternalModulesLocators locators;
  private final Tools tools;
  private final Project project;

  public Bach(CLI cli, Printer printer, Recording recording) {
    this.cli = cli;
    this.printer = printer;
    this.recording = recording;
    this.paths = createPaths();
    this.browser = createBrowser();
    this.locators = createExternalModulesLocators();
    this.tools = createTools();
    this.project = createProject();
  }

  protected Browser createBrowser() {
    return new Browser(new LoadValidator(this));
  }

  protected ExternalModulesLocators createExternalModulesLocators() {
    var locators = new ArrayList<ExternalModulesLocator>();
    ServiceLoader.load(ExternalModulesLocator.class).forEach(locators::add);
    PathSupport.list(paths().externalModules(), PathSupport::isPropertiesFile).stream()
        .map(ExternalModulesLocator::ofProperties)
        .forEach(locators::add);
    return new ExternalModulesLocators(locators);
  }

  protected Paths createPaths() {
    return Paths.ofRoot(cli.rootPath());
  }

  protected Tools createTools() {
    var operators = new ArrayList<Tool>();
    ServiceLoader.load(ToolOperator.class).forEach(it -> operators.add(Tool.ofToolOperator(it)));
    var providers = new ArrayList<Tool>();
    ServiceLoader.load(ToolProvider.class).forEach(it -> providers.add(Tool.ofToolProvider(it)));

    var javaHome = paths.javaHome();
    var finders = new ArrayList<ToolFinder>();
    finders.add(ToolFinder.ofTools("Tool Operator Services", operators));
    finders.add(ToolFinder.ofTools("Tool Provider Services", providers));
    finders.add(
        ToolFinder.ofToolProviders(
            "Tool Providers in " + paths.externalModules().toUri(), paths.externalModules()));
    finders.add(
        ToolFinder.ofJavaPrograms(
            "Java Programs in " + paths.externalTools().toUri(),
            paths.externalTools(),
            javaHome.resolve("bin").resolve("java")));
    finders.add(
        ToolFinder.ofNativeTools(
            "Native Tools in ${JAVA_HOME} -> " + javaHome.toUri(),
            name -> "java.home/" + name, // ensure stable names with synthetic prefix
            javaHome.resolve("bin"),
            "java",
            "jfr",
            "jdeprscan"));

    var tweaks = new ArrayList<ToolTweak>();
    ServiceLoader.load(ToolTweak.class).forEach(tweaks::add);

    return new Tools("All Tools", new ToolFinders(finders), new ToolTweaks(tweaks));
  }

  protected Project createProject() {
    var factory = ServiceLoader.load(Project.Factory.class).findFirst().orElse(Project::ofDefaults);
    return factory.createProject(this);
  }

  public final CLI cli() {
    return cli;
  }

  public final Paths paths() {
    return paths;
  }

  public final Browser browser() {
    return browser;
  }

  public final ExternalModulesLocators locators() {
    return locators;
  }

  public final Tools tools() {
    return tools;
  }

  public final Project project() {
    return project;
  }

  public void debug(Object message) {
    log(System.Logger.Level.DEBUG, message);
  }

  public void info(Object message) {
    log(System.Logger.Level.INFO, message);
  }

  public void log(System.Logger.Level level, Object message) {
    var text = String.valueOf(message);
    printer.printMessage(level, text);
  }

  @Override
  public void run(ToolCall call) {
    run(tools, call, System.Logger.Level.INFO);
  }

  public void run(ToolFinder toolFinder, ToolCall toolCall, System.Logger.Level logLevel) {
    var call = toolCall.withTweaks(tools().tweaks().list());
    log(logLevel, "+ %s".formatted(call.toCommandLine(" ")));
    var name = call.name();
    var tool = toolFinder.findFirst(name).orElseThrow(() -> new ToolNotFoundException(name));
    var arguments = call.arguments();
    runTool(tool, arguments);
  }

  private void runTool(Tool tool, List<String> arguments) {
    if (tool instanceof Tool.ToolOperatorTool it) {
      runToolOperator(it.operator(), arguments);
      return;
    }
    if (tool instanceof Tool.ToolProviderTool it) {
      var provider = it.provider();
      Thread.currentThread().setContextClassLoader(provider.getClass().getClassLoader());
      var code = runToolProvider(provider, arguments);
      if (code == 0) return;
      var name = tool.name();
      throw new RuntimeException("Tool %s returned non-zero exit code: %d".formatted(name, code));
    }
    throw new Error(tool.getClass().getCanonicalName());
  }

  void runToolOperator(ToolOperator operator, List<String> arguments) {
    var event = new FlightRecorderEvent.ToolOperatorRun();
    event.name = operator.name();
    event.args = String.join(" ", arguments);
    try {
      event.begin();
      operator.run(new ToolOperator.Operation(this, arguments));
      event.end();
    } catch (RuntimeException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    } finally {
      event.commit();
    }
  }

  private int runToolProvider(ToolProvider provider, List<String> arguments) {
    var event = new FlightRecorderEvent.ToolProviderRun();
    event.name = provider.name();
    event.args = String.join(" ", arguments);
    var args = arguments.toArray(String[]::new);
    try (var out = new StringPrintWriterMirror(printer.out());
        var err = new StringPrintWriterMirror(printer.err())) {
      event.begin();
      event.code = provider.run(out, err, args);
      event.end();
      event.out = out.toString().strip();
      event.err = err.toString().strip();
    } finally {
      event.commit();
    }
    return event.code;
  }

  public String toString(int indent) {
    return """
            Command-Line Interface
            %s
            Printer
            %s
            Paths
            %s
            Tool Finders
            %s
            Project
            %s
            """
        .formatted(
            cli.toString(2),
            printer.toString(2),
            paths.toString(indent + 2),
            tools.finders().toString(indent + 2),
            project)
        .indent(indent)
        .stripTrailing();
  }

  public void writeLogbook() throws Exception {
    writeLogbook(paths.out());
  }

  public void writeLogbook(Path path) throws Exception {
    var dir = Files.createDirectories(path);
    Files.writeString(dir.resolve("bach-printer.log"), printer.toHistoryString(0));
    recording.dump(dir.resolve("bach-events.jfr"));
  }
}
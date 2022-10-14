package run.bach;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import run.bach.internal.LoadValidator;
import run.bach.internal.PathSupport;
import run.bach.internal.StringPrintWriterMirror;

public class Bach implements ToolRunner {

  public static final String VERSION = "2022.10.14-ea+1446";

  private final Configuration configuration;
  private final Paths paths;
  private final Browser browser;
  private final Locators locators;
  private final Tools tools;

  public Bach(Configuration configuration) {
    this.configuration = configuration;
    this.paths = createPaths();
    this.browser = createBrowser();
    this.locators = createLocators();
    this.tools = createTools();
  }

  protected Browser createBrowser() {
    return new Browser(new LoadValidator(this));
  }

  protected Locators createLocators() {
    var locators = new ArrayList<Locator>();
    ServiceLoader.load(Locator.class).forEach(locators::add);
    PathSupport.list(paths().externalModules(), PathSupport::isPropertiesFile).stream()
        .map(Locator::ofProperties)
        .forEach(locators::add);
    return new Locators(locators);
  }

  protected Paths createPaths() {
    return Paths.ofRoot(configuration.cli().rootPath());
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
    return new Tools(new ToolFinders(finders));
  }

  public final Configuration configuration() {
    return configuration;
  }

  public final Paths paths() {
    return paths;
  }

  public final Browser browser() {
    return browser;
  }

  public final Locators locators() {
    return locators;
  }

  public final Tools tools() {
    return tools;
  }

  public void debug(Object message) {
    log(System.Logger.Level.DEBUG, message);
  }

  public void info(Object message) {
    log(System.Logger.Level.INFO, message);
  }

  public void log(System.Logger.Level level, Object message) {
    var text = String.valueOf(message);
    configuration().printer().printMessage(level, text);
  }

  @Override
  public void run(ToolCall call) {
    run(call, System.Logger.Level.INFO);
  }

  void run(ToolCall call, System.Logger.Level level) {
    log(level, "+ %s".formatted(call.toCommandLine(" ")));
    var tool = tools().get(call.name());
    var arguments = call.arguments();
    runTool(tool, arguments);
  }

  void runTool(Tool tool, List<String> arguments) {
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
      operator.operate(this, arguments);
      event.end();
    } catch (RuntimeException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    } finally {
      event.commit();
    }
  }

  int runToolProvider(ToolProvider provider, List<String> arguments) {
    var event = new FlightRecorderEvent.ToolProviderRun();
    event.name = provider.name();
    event.args = String.join(" ", arguments);
    var printer = configuration.printer();
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
            Paths
            %s
            Tool Finders
            %s
            """
        .formatted(paths.toString(indent + 2), tools.finders().toString(indent + 2))
        .indent(indent)
        .stripTrailing();
  }
}
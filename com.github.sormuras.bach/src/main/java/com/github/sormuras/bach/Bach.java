package com.github.sormuras.bach;

import com.github.sormuras.bach.internal.MirroringStringPrintWriter;
import com.github.sormuras.bach.internal.StringSupport;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.spi.ToolProvider;
import jdk.jfr.Recording;

/** Java Shell Builder. */
public final class Bach implements ToolRunner {

  /** Bach's main program running the initial seed tool call. */
  public static void main(String... args) {
    var bach = Bach.of(args);
    var code = bach.main();
    if (code != 0) System.exit(code);
  }

  /** {@return an instance with "standard" streams and configured from the given arguments array} */
  public static Bach of(String... args) {
    var out = new PrintWriter(System.out, true);
    var err = new PrintWriter(System.err, true);
    return new Bach(Configuration.of(out, err, args));
  }

  private final Configuration configuration;
  private final ThreadLocal<Deque<String>> stackedToolCallNames;

  /** Initialize this {@code Bach} instance. */
  public Bach(Configuration configuration) {
    this.configuration = configuration;
    this.stackedToolCallNames = ThreadLocal.withInitial(ArrayDeque::new);
  }

  /** {@return the immutable configuration object} */
  public Configuration configuration() {
    return configuration;
  }

  /** {@return the result of running the initial seed tool call} */
  private int main() {
    var remnants = configuration.remnants();
    if (remnants.isEmpty()) {
      run(ToolCall.of("info"));
      return 1;
    }
    var verbose = configuration.isVerbose();
    var printer = configuration.printer();
    var paths = configuration.paths();
    try (var recording = new Recording()) {
      recording.start();
      try {
        if (verbose) printer.out("BEGIN");
        run(ToolCall.of(remnants));
        if (verbose) printer.out("END.");
        return 0;
      } catch (RuntimeException exception) {
        printer.err(exception.getClass().getSimpleName() + ": " + exception.getMessage());
        return 2;
      } finally {
        recording.stop();
        var jfr = Files.createDirectories(paths.out()).resolve("bach-logbook.jfr");
        recording.dump(jfr);
      }
    } catch (Exception exception) {
      exception.printStackTrace(printer.err());
      return -2;
    }
  }

  @Override
  public void run(ToolCall call, Set<RunModifier> modifiers) {
    run(configuration.finder(), call, modifiers);
  }

  @Override
  public void run(ToolFinder finder, ToolCall call, Set<RunModifier> modifiers) {
    var name = call.name();
    var arguments = call.arguments();
    var verbose = configuration.isVerbose();
    var printer = configuration.printer();
    var visible = !modifiers.contains(RunModifier.HIDDEN);
    var tccl = modifiers.contains(RunModifier.RUN_WITH_PROVIDERS_CLASS_LOADER);

    var tools = finder.find(name);
    if (tools.isEmpty()) throw new ToolNotFoundException(name);
    if (tools.size() != 1) throw new ToolNotUniqueException(name, tools);
    var provider = tools.get(0).provider();

    var stack = stackedToolCallNames.get();
    var thread = Thread.currentThread();
    var loader = thread.getContextClassLoader();
    try {
      stack.addLast(name);
      if (visible) {
        if (verbose) {
          printer.out("[%2X] %s".formatted(thread.getId(), String.join(" | ", stack)));
        }
        var flat = verbose ? name : name.substring(name.indexOf('/') + 1);
        var text = arguments.isEmpty() ? flat : flat + ' ' + StringSupport.join(arguments);
        printer.out(provider instanceof ToolOperator ? text : "  " + text);
      }
      if (tccl) {
        thread.setContextClassLoader(provider.getClass().getClassLoader());
      }
      var code = run(provider, name, arguments);
      if (code != 0) {
        throw new RuntimeException("%s returned non-zero exit code: %d".formatted(name, code));
      }
    } finally {
      stack.removeLast();
      thread.setContextClassLoader(loader);
    }
  }

  private int run(ToolProvider provider, String name, List<String> arguments) {
    var event = new ToolRunEvent();
    event.name = name;
    event.args = String.join(" ", arguments);
    var printer = configuration.printer();
    try (var out = new MirroringStringPrintWriter(printer.out());
        var err = new MirroringStringPrintWriter(printer.err())) {
      var args = arguments.toArray(String[]::new);
      event.begin();
      if (provider instanceof ToolOperator operator) {
        event.code = operator.run(this, out, err, args);
      } else {
        event.code = provider.run(out, err, args);
      }
      event.end();
      event.out = out.toString().strip();
      event.err = err.toString().strip();
      event.commit();
      return event.code;
    } finally {
      printer.out().flush();
      printer.err().flush();
    }
  }
}

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

/** Composing tool finders. */
class Tool4 {
  public static void main(String... args) {
    /* Empty args array given? Show usage message and exit. */ {
      if (args.length == 0) {
        System.err.printf("Usage: %s TOOL-NAME TOOL-ARGS...%n", Tool4.class.getSimpleName());
        return;
      }
    }

    var finder =
        ToolFinder.compose(
            //
            ToolFinder.of(new Banner()),
            //
            ToolFinder.ofSystem());

    /* Handle special case: --list-tools */ {
      if (args[0].equals("--list-tools")) {
        finder.findAll().forEach(tool -> System.out.printf("%9s by %s%n", tool.name(), tool));
        return;
      }
    }

    /* Run an arbitrary tool. */ {
      var runner = ToolRunner.of(finder);
      runner.run(args[0], Arrays.copyOfRange(args, 1, args.length));
    }
  }

  interface ToolFinder {

    List<ToolProvider> findAll();

    default Optional<ToolProvider> find(String name) {
      return streamAll().filter(tool -> tool.name().equals(name)).findFirst();
    }

    default Stream<ToolProvider> streamAll() {
      return findAll().stream();
    }

    static ToolFinder compose(ToolFinder... finders) {
      record CompositeToolFinder(List<ToolFinder> finders) implements ToolFinder {
        public List<ToolProvider> findAll() {
          return finders.stream().flatMap(ToolFinder::streamAll).toList();
        }
      }
      return new CompositeToolFinder(List.of(finders));
    }

    static ToolFinder of(ToolProvider... providers) {
      record ListToolFinder(List<ToolProvider> findAll) implements ToolFinder {}
      return new ListToolFinder(List.of(providers));
    }

    static ToolFinder ofSystem() {
      return () ->
          ServiceLoader.load(ToolProvider.class, ClassLoader.getSystemClassLoader()).stream()
              .map(ServiceLoader.Provider::get)
              .toList();
    }
  }

  interface ToolRunner {
    void run(String name, String... args);

    static ToolRunner of(ToolFinder finder) {
      return (name, args) -> {
        var tool = finder.find(name).orElseThrow(() -> new RuntimeException(name + " not found"));
        var code = tool.run(System.out, System.err, args);
        if (code != 0) throw new RuntimeException(name + " returned non-zero code: " + code);
      };
    }
  }

  record Banner(String name) implements ToolProvider {

    Banner() {
      this("banner");
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
      var text = "USAGE: %s TEXT...".formatted(name());
      var line = args.length != 0 ? String.join(" ", args) : text;
      var dash = "=".repeat(line.length());
      out.printf(
          """
          %s
          %s
          %s
          """,
          dash, line.toUpperCase(), dash);
      return 0;
    }
  }
}

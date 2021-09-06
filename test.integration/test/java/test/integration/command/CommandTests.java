package test.integration.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.sormuras.bach.command.AdditionalArgumentsOption;
import com.github.sormuras.bach.command.Command;
import com.github.sormuras.bach.command.Composer;
import com.github.sormuras.bach.command.VerboseOption;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CommandTests {
  @Test
  void defaultCommand() {
    var command = Command.of("default");

    assertEquals("default", command.name());
    assertArguments(command);

    assertArguments(command.add(1), "1");
    assertArguments(command.add("1", 2, 3), "1", "2", "3");

    assertArguments(command.addAll(1, 2, 3, 4), "1", "2", "3", "4");
    assertArguments(command.addAll(List.of(1, 2, 3, 4)), "1", "2", "3", "4");

    assertArguments(command.option(AdditionalArgumentsOption.empty()));
    assertArguments(command.add(1).option(AdditionalArgumentsOption.empty()));
    assertArguments(command.option(AdditionalArgumentsOption.empty().add(1)), "1");
  }

  @Test
  void defaultCommandWithVerboseOptionFails() {
    var command = Command.of("default");
    var option = VerboseOption.empty();
    assertThrows(UnsupportedOperationException.class, () -> command.option(option));
  }

  @Test
  void composer() {
    var command = Command.of("default");
    assertSame(command, command.composing(Composer.identity()));
    assertSame(command.composing(composer -> composer), command);

    assertArguments(command.composing(composer -> composer.add("1")), "1");
  }

  @Test
  void jar() {
    var jar = Command.jar();
    assertEquals("jar", jar.name());

    assertArguments(jar);
    assertArguments(jar.verbose(null));
    assertArguments(jar.verbose(false));

    assertArguments(jar.verbose(true), "--verbose");
    assertArguments(jar.verbose(true).add("--no-compress"), "--verbose", "--no-compress");

    assertArguments(
        jar.mode("--create")
            .file(Path.of("file.zip"))
            .main("com.greetings.Main")
            .verbose(true)
            .add("--no-compress")
            .add("--no-manifest")
            .withFiles(Path.of("."))
            .withFiles(9, Path.of("nine"))
            .withFiles(11, Path.of("elf"))
            .withFiles(9, Path.of("neuf"))
            .withFiles(Path.of("base")),
        """
        --create
        --file
        file.zip
        --main-class
        com.greetings.Main
        --verbose
        --no-compress
        --no-manifest
        -C
        .
        .
        base
        --release
        9
        nine
        neuf
        --release
        11
        elf
        """
            .lines());
  }

  @Test
  void javac() {
    var javac = Command.javac();
    assertEquals("javac", javac.name());

    assertArguments(
        javac.modules("foo.bar", "foo.baz").verbose(true).add("-g"),
        """
        --module
        foo.bar.foo.baz
        --verbose
        -g
        """
            .lines());
  }

  static void assertArguments(Command<?> command, Stream<String> expected) {
    assertLinesMatch(expected, command.toArguments().stream());
  }

  static void assertArguments(Command<?> command, String... expected) {
    assertLinesMatch(List.of(expected), command.toArguments());
  }
}

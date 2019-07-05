package de.sormuras.bach;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;

public class ProjectTests {
  public static void main(String[] args) {
    System.out.println(Project.class + " is in " + Project.class.getModule());
    checkProjectProperties();
    checkHelp();
  }

  private static void checkProjectProperties() {
    var project = Project.of(Path.of("bach.properties"));
    assert "bach".equals(project.name) : "expected 'bach' as name, but got: " + project.name;
    assert "2-ea".equals(project.version.toString());
    assert Path.of("").equals(project.paths.home);
    assert Path.of("src/modules").equals(project.paths.sources);
    assert List.of("de.sormuras.bach", "integration").equals(project.options.modules);
  }

  private static void checkHelp() {
    var string = new StringWriter();
    Project.help(new PrintWriter(string, true));
    assert string.toString().contains("1.0.0-SNAPSHOT");
  }
}

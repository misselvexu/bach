/**
 * Defines the API of 🎼 Bach, the Java Shell Builder.
 *
 * <h2>Links</h2>
 *
 * <ul>
 *   <li>Bach's <a href="https://github.com/sormuras/bach">Code &amp; Issues</a>
 *   <li>Bach's <a href="https://sormuras.github.io/bach">User Guide</a>
 *   <li>Java® Development Kit Version 15 <a
 *       href="https://docs.oracle.com/en/java/javase/15/docs/specs/man/">Tool Specifications</a>
 * </ul>
 *
 * @uses java.util.spi.ToolProvider
 */
@com.github.sormuras.bach.ProjectInfo(name = "🎶")
module com.github.sormuras.bach {
  exports com.github.sormuras.bach;
  exports com.github.sormuras.bach.module;
  exports com.github.sormuras.bach.project;
  exports com.github.sormuras.bach.tool;

  requires transitive java.net.http;
  requires jdk.compiler;
  requires jdk.crypto.ec; // https://stackoverflow.com/questions/55439599
  requires jdk.jartool;
  requires jdk.javadoc;
  requires jdk.jdeps;
  requires jdk.jlink;

  uses com.github.sormuras.bach.BuildProgram;
  uses java.util.spi.ToolProvider;

  provides java.util.spi.ToolProvider with
      com.github.sormuras.bach.internal.BachToolProvider;
}

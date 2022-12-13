package run.bach.tool;

import java.io.PrintWriter;
import run.bach.ProjectTool;
import run.bach.ProjectToolRunner;
import run.duke.Duke;

public class CleanTool extends ProjectTool {
  public CleanTool(ProjectToolRunner runner) {
    super("clean", runner);
  }

  @Override
  public int run(PrintWriter out, PrintWriter err, String... args) {
    run(Duke.treeDelete(folders().out()));
    return 0;
  }
}

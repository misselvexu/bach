package run.duke;

import java.util.List;

@FunctionalInterface
public interface ToolRunner {
  Workbench workbench();

  default void run(String tool, String... args) {
    workbench().run(new ToolCall(tool, List.of(args)));
  }

  default void run(String tool, ToolCall.Tweak tweak) {
    workbench().run(new ToolCall(tool).withTweak(tweak));
  }

  default void run(ToolCall call) {
    workbench().run(call);
  }
}

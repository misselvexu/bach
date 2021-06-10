package com.github.sormuras.bach.trait;

import com.github.sormuras.bach.Trait;
import com.github.sormuras.bach.api.Workflow;
import com.github.sormuras.bach.api.UnsupportedWorkflowException;

public /*sealed*/ interface WorkflowTrait extends Trait {

  default void run(Workflow workflow) {
    bach().log("run(%s)".formatted(workflow));
    switch (workflow) {
      case BUILD -> build();
      case CLEAN -> clean();
      case RESOLVE -> resolve();
      case COMPILE_MAIN -> compileMainCodeSpace();
      case COMPILE_TEST -> compileTestCodeSpace();
      case EXECUTE_TESTS -> executeTests();
      case GENERATE_DOCUMENTATION -> generateDocumentation();
      case GENERATE_IMAGE -> generateImage();
      case WRITE_LOGBOOK -> writeLogbook();
      default -> throw new UnsupportedWorkflowException(workflow.toString());
    }
  }

  default void build() {
    bach().core().factory().newBuildWorkflow(bach()).build();
  }

  default void clean() {
    bach().core().factory().newCleanWorkflow(bach()).clean();
  }

  default void resolve() {
    bach().core().factory().newResolveWorkflow(bach()).resolve();
  }

  default void compileMainCodeSpace() {
    bach().core().factory().newCompileMainCodeSpaceWorkflow(bach()).compile();
  }

  default void compileTestCodeSpace() {
    bach().core().factory().newCompileTestCodeSpaceWorkflow(bach()).compile();
  }

  default void executeTests() {
    bach().core().factory().newExecuteTestsWorkflow(bach()).execute();
  }

  default void generateDocumentation() {
    bach().core().factory().newGenerateDocumentationWorkflow(bach()).generate();
  }

  default void generateImage() {
    bach().core().factory().newGenerateImageWorkflow(bach()).generate();
  }

  default void writeLogbook() {
    bach().core().factory().newWriteLogbookWorkflow(bach()).write();
  }
}
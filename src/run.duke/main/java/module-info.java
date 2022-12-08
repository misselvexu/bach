/** Defines the foundational APIs of Duke, the Tool Finder and Runner SPI. */
module run.duke {
  exports run.duke;

  uses java.util.spi.ToolProvider;

  provides run.duke.ToolFinder with
      run.duke.DukeTool.Finder;
}

package com.github.sormuras.bach;

import java.util.Set;

public record Configuration(Printer printer, Flags flags, Paths paths, ToolFinder finder) {

  public static Configuration ofDefaults() {
    return new Configuration(
        Printer.ofSystem(),
        new Flags(Set.of()),
        Paths.ofCurrentWorkingDirectory(),
        ToolFinder.ofSystemTools());
  }

  public Configuration with(Printer printer) {
    return new Configuration(printer, flags, paths, finder);
  }

  public Configuration with(Flags flags) {
    return new Configuration(printer, flags, paths, finder);
  }

  public Configuration with(Paths paths) {
    return new Configuration(printer, flags, paths, finder);
  }

  public Configuration with(ToolFinder finder) {
    return new Configuration(printer, flags, paths, finder);
  }

  public boolean isDryRun() {
    return flags.set().contains(Flag.DRY_RUN);
  }

  public boolean isVerbose() {
    return flags.set().contains(Flag.VERBOSE);
  }
}
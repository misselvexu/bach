package com.github.sormuras.bach.internal;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;

/** Internal {@link Path}-related utilities. */
public class Paths {

  public static Path createDirectories(Path directory) {
    try {
      return Files.createDirectories(directory);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Path deleteDirectories(Path directory) {
    return deleteDirectories(directory, __ -> true);
  }

  public static Path deleteDirectories(Path directory, Predicate<Path> filter) {
    // trivial case: delete existing empty directory or single file
    try {
      Files.deleteIfExists(directory);
      return directory;
    } catch (DirectoryNotEmptyException ignored) {
      // fall-through
    } catch (Exception e) {
      throw new RuntimeException("Delete directories failed: " + directory, e);
    }
    // default case: walk the tree...
    try (var stream = Files.walk(directory)) {
      var selected = stream.filter(filter).sorted((p, q) -> -p.compareTo(q));
      for (var path : selected.toArray(Path[]::new)) Files.deleteIfExists(path);
    } catch (Exception e) {
      throw new RuntimeException("Delete directories failed: " + directory, e);
    }
    return directory;
  }

  public static boolean deleteIfExists(Path path) {
    if (Files.notExists(path)) return false;
    try {
      if (Files.isDirectory(path)) {
        Paths.deleteDirectories(path, __ -> true);
        return true;
      } else return Files.deleteIfExists(path);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean isViewSupported(Path file, String view) {
    return file.getFileSystem().supportedFileAttributeViews().contains(view);
  }

  public static boolean isVisible(Path path) {
    try {
      for (int endIndex = 1; endIndex <= path.getNameCount(); endIndex++) {
        var subpath = path.subpath(0, endIndex);
        // work around https://bugs.openjdk.java.net/browse/JDK-8255576
        var probe = subpath.toString().isEmpty() ? path.toAbsolutePath() : subpath;
        if (Files.isHidden(probe)) return false;
      }
      return true;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Optional<String> readString(Path path) {
    try{
      return Optional.of(Files.readString(path));
    } catch (Exception exception) {
      return Optional.empty();
    }
  }

  public static String slashed(Path path) {
    return path.toString().replace('\\', '/');
  }

  /** Hidden default constructor. */
  private Paths() {}
}

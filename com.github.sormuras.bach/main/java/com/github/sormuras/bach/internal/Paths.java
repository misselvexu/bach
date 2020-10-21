package com.github.sormuras.bach.internal;

import java.io.File;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** {@link Path}-related utilities. */
public final class Paths {

  public static Path createDirectories(Path directory) {
    try {
      return Files.createDirectories(directory);
    } catch (Exception e) {
      throw new RuntimeException("Create directories failed: " + directory, e);
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

  /** Convert path element names of the given unit into a reversed deque. */
  public static Deque<String> deque(Path path) {
    var deque = new ArrayDeque<String>();
    path.forEach(name -> deque.addFirst(name.toString()));
    return deque;
  }

  /** Test supplied path for pointing to a Java Archive file. */
  public static boolean isJarFile(Path path) {
    return Files.isRegularFile(path) && name(path).endsWith(".jar");
  }

  /** Test supplied path for pointing to a Java compilation unit. */
  public static boolean isJavaFile(Path path) {
    return Files.isRegularFile(path) && name(path).endsWith(".java");
  }

  /** Test supplied path for pointing to a Java module declaration compilation unit. */
  public static boolean isModuleInfoJavaFile(Path path) {
    return isJavaFile(path) && name(path).equals("module-info.java");
  }

  /** Test supplied path for pointing to a Java module declaration for a given realm. */
  public static boolean isModuleInfoJavaFileForRealm(Path info, String realm) {
    return isModuleInfoJavaFile(info) && Collections.frequency(deque(info), realm) == 1;
  }

  /** Test for a path pointing to a file system root like {@code /} or {@code C:\}. */
  public static boolean isRoot(Path path) {
    return path.toAbsolutePath().normalize().getNameCount() == 0;
  }

  /** Return {@code true} if the given name of the view is supported by the given file. */
  public static boolean isViewSupported(Path file, String view) {
    return file.getFileSystem().supportedFileAttributeViews().contains(view);
  }

  /** List content of specified directory in natural order with the given filter applied. */
  public static List<Path> list(Path directory, DirectoryStream.Filter<? super Path> filter) {
    var paths = new TreeSet<>(Comparator.comparing(Path::toString));
    try (var directoryStream = Files.newDirectoryStream(directory, filter)) {
      directoryStream.forEach(paths::add);
    } catch (Exception e) {
      throw new Error("Stream directory '" + directory + "' failed: " + e, e);
    }
    return List.copyOf(paths);
  }

  /** Walk all trees to find matching paths the given filter starting at given root paths. */
  public static List<Path> find(Collection<Path> roots, int maxDepth, Predicate<Path> filter) {
    var paths = new TreeSet<>(Comparator.comparing(Path::toString));
    for (var root : roots) {
      try (var stream = Files.walk(root, maxDepth)) {
        stream.filter(filter).forEach(paths::add);
      } catch (Exception e) {
        throw new Error("Walk directory '" + root + "' failed: " + e, e);
      }
    }
    return List.copyOf(paths);
  }

  public static List<Path> findModuleInfoJavaFiles(Path directory, int limit) {
    if (isRoot(directory)) throw new IllegalStateException("Root directory: " + directory);
    var units = find(List.of(directory), limit, Paths::isModuleInfoJavaFile);
    if (units.isEmpty()) throw new IllegalStateException("No module-info.java: " + directory);
    return List.copyOf(units);
  }

  /** Join a collection of path objects to a string using the system-dependent separator. */
  public static String join(Collection<Path> paths) {
    return paths.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
  }

  public static Optional<String> joinExisting(Path... elements) {
    var paths = retainExisting(elements);
    return paths.isEmpty() ? Optional.empty() : Optional.of(join(paths));
  }

  public static List<Path> retainExisting(Path... elements) {
    var paths = new ArrayList<Path>();
    for (var element : elements) if (Files.exists(element)) paths.add(element);
    return List.copyOf(paths);
  }

  /** Return path's file name as a {@link String}. */
  public static String name(Path path) {
    return path.getNameCount() == 0 ? "" : path.getFileName().toString();
  }

  public static String replaceBackslashes(Path path) {
    return path.toString().replace('\\', '/');
  }

  public static String quote(Path path) {
    return '"' + replaceBackslashes(path) + '"';
  }

  /** Return the size of a file in bytes. */
  public static long size(Path path) {
    try {
      return Files.size(path);
    } catch (Exception e) {
      throw new Error("Size of file failed: " + e, e);
    }
  }

  private Paths() {}
}
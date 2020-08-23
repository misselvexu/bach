/*
 * Bach - Java Shell Builder
 * Copyright (C) 2020 Christian Stein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.sormuras.bach.project;

import de.sormuras.bach.internal.Factory;
import de.sormuras.bach.internal.Factory.Kind;
import de.sormuras.bach.internal.Maven;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.TreeMap;

/** A link is module-uri pair used to resolve external modules. */
public final class Link {

  //
  // Record API
  //

  private final String module;
  private final String uri;

  public Link(String module, String uri) {
    this.module = module;
    this.uri = uri;
  }

  public String module() {
    return module;
  }

  public String uri() {
    return uri;
  }

  //
  // Configuration API
  //

  /**
   * Create a new module link pointing to an artifact hosted at the given location.
   *
   * @param module The module to used as the nominal part of the pair
   * @param uri The string representation of the URI
   * @return A new {@code Link} instance
   */
  @Factory
  public static Link of(String module, String uri) {
    return new Link(module, uri);
  }

  /**
   * Create a new module link pointing to an artifact hosted at Maven Central.
   *
   * @param module The module to used as the nominal part of the pair
   * @param group Maven Group ID
   * @param artifact Maven Artifact ID
   * @param version The version string
   * @return A new Maven Central-based {@code Link} instance
   * @see <a href="https://search.maven.org">search.maven.org</a>
   */
  @Factory
  public static Link ofCentral(String module, String group, String artifact, String version) {
    return of(module, Maven.central(group, artifact, version)).withVersion(version);
  }

  /**
   * Create a new module link pointing to an artifact hosted at Maven Central.
   *
   * @param module The module to used as the nominal part of the pair
   * @param gav Maven groupId + ':' + artifactId + ':' version [+ ':' + classifier]
   * @return A new Maven Central-based {@code Link} instance
   * @see <a href="https://search.maven.org">search.maven.org</a>
   */
  @Factory
  public static Link ofCentral(String module, String gav) {
    var split = gav.split(":");
    if (split.length < 3) throw new IllegalArgumentException();
    var version = split[2];
    var joiner = new Maven.Joiner().group(split[0]).artifact(split[1]).version(version);
    joiner.classifier(split.length < 4 ? "" : split[3]);
    return of(module, joiner.toString()).withVersion(version);
  }

  /**
   * Create a new module link pointing to an artifact of a single-module project built by JitPack.
   *
   * @param module The module to be used as the nominal part of the pair
   * @param user GitHub username or the complete group like {@code "com.azure.${USER}"}
   * @param repository Name of the repository or project
   * @param version The version string of the repository or project, which is either a release tag,
   *     a commit hash, or {@code "${BRANCH}-SNAPSHOT"} for a version that has not been released.
   * @return A new JitPack-based {@code Link} instance
   * @see <a href="https://jitpack.io/docs">jitpack.io</a>
   */
  @Factory
  public static Link ofJitPack(String module, String user, String repository, String version) {
    return ofJitPack(module, user, repository, version, false);
  }

  /**
   * Create a new module link pointing to an artifact built by JitPack.
   *
   * @param module The module to be used as the nominal part of the pair
   * @param user GitHub username or the complete group like {@code "com.azure.${USER}"}
   * @param repository Name of the repository or project
   * @param version The version string of the repository or project, which is either a release tag,
   *     a commit hash, or {@code "${BRANCH}-SNAPSHOT"} for a version that has not been released.
   * @param multiModuleProject Pass {@code true} for a multi-module project or {@code false} for a
   *     project that declares a single module.
   * @return A new JitPack-based {@code Link} instance
   * @see <a href="https://jitpack.io/docs">JitPack Documentation</a>
   * @see <a href="https://jitpack.io/docs/BUILDING/#multi-module-projects">JitPack Multi-Module
   *     Projects</a>
   */
  @Factory
  public static Link ofJitPack(
      String module, String user, String repository, String version, boolean multiModuleProject) {
    var group = user.indexOf('.') == -1 ? "com.github." + user : user;
    var jar = (multiModuleProject ? module : repository) + "-" + version + ".jar";
    var uri = new StringJoiner("/");
    uri.add("https://jitpack.io");
    uri.add(group.replace('.', '/'));
    uri.add(repository);
    if (multiModuleProject) uri.add(module);
    uri.add(version);
    uri.add(jar);
    return of(module, uri.toString()).withVersion(version);
  }

  /**
   * Create a new link pointing to a modular JUnit Jupiter JAR file hosted at Maven Central.
   *
   * @param suffix The suffix used to complete the module name and the Maven Artifact ID
   * @param version The version string
   * @return A new Maven Central-based {@code Link} instance of JUnit Jupiter
   * @see <a
   *     href="https://search.maven.org/search?q=g:org.junit.jupiter">org.junit.platform[.]$suffix</a>
   */
  @Factory
  public static Link ofJUnitJupiter(String suffix, String version) {
    var module = "org.junit.jupiter" + (suffix.isEmpty() ? "" : '.' + suffix);
    var artifact = "junit-jupiter" + (suffix.isEmpty() ? "" : '-' + suffix);
    return Link.ofCentral(module, "org.junit.jupiter", artifact, version);
  }

  /**
   * Create a new link pointing to a modular JUnit Platform JAR file hosted at Maven Central.
   *
   * @param suffix The suffix used to complete the module name and the Maven Artifact ID
   * @param version The version string
   * @return A new Maven Central-based {@code Link} instance of JUnit Platform
   * @see <a
   *     href="https://search.maven.org/search?q=g:org.junit.platform">org.junit.platform.$suffix</a>
   */
  @Factory
  public static Link ofJUnitPlatform(String suffix, String version) {
    var module = "org.junit.platform." + suffix;
    var artifact = "junit-platform-" + suffix;
    return Link.ofCentral(module, "org.junit.platform", artifact, version);
  }

  @Factory(Kind.OPERATOR)
  public Link withDigest(String algorithm, String digest) {
    var separator = uri.indexOf('#') == -1 ? '#' : '&';
    return new Link(module, uri + separator + "digest-" + algorithm + '=' + digest);
  }

  @Factory(Kind.OPERATOR)
  public Link withSize(long size) {
    var separator = uri.indexOf('#') == -1 ? '#' : '&';
    return new Link(module, uri + separator + "size=" + size);
  }

  @Factory(Kind.OPERATOR)
  public Link withVersion(String version) {
    var separator = uri.indexOf('#') == -1 ? '#' : '&';
    return new Link(module, uri + separator + "version=" + version);
  }

  //
  // Normal API
  //

  public List<String> findFragments(String key) {
    var list = new ArrayList<String>();
    var fragment = URI.create(uri).getFragment();
    if (fragment == null) return List.of();
    var entries = fragment.split("&");
    for (var entry : entries) if (entry.startsWith(key)) list.add(entry.substring(key.length()));
    return list;
  }

  public Map<String, String> findDigests() {
    var map = new TreeMap<String, String>();
    for (var pair : findFragments("digest-")) {
      int separator = pair.indexOf('=');
      if (separator <= 0) throw new IllegalStateException("Digest algorithm not found: " + pair);
      var algorithm = pair.substring(0, separator);
      var digest = pair.substring(pair.indexOf('=') + 1);
      map.put(algorithm, digest);
    }
    return map;
  }

  public Optional<Integer> findSize() {
    var versions = findFragments("size=");
    if (versions.size() == 0) return Optional.empty();
    if (versions.size() == 1) return Optional.of(Integer.valueOf(versions.get(0)));
    throw new IllegalStateException("Multiple versions found in fragment: " + versions);
  }

  public Optional<String> findVersion() {
    var versions = findFragments("version=");
    if (versions.size() == 0) return Optional.empty();
    if (versions.size() == 1) return Optional.of(versions.get(0));
    throw new IllegalStateException("Multiple versions found in fragment: " + versions);
  }

  public URI toURI() {
    return URI.create(uri);
  }

  public String toModularJarFileName() {
    return module + findVersion().map(v -> '@' + v).orElse("") + ".jar";
  }
}

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

package de.sormuras.bach.call;

import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** A call to {@code javac}, the Java compiler. */
public /*static*/ class Javac extends GenericSourcesConsumer<Javac> {

  private Version versionOfModulesThatAreBeingCompiled;
  private List<String> patternsWhereToFindSourceFiles;
  private Map<String, List<Path>> pathsWhereToFindSourceFiles;
  private Map<String, List<Path>> pathsWhereToFindMoreAssetsPerModule;
  private List<Path> pathsWhereToFindApplicationModules;

  private int compileForVirtualMachineVersion;
  private boolean enablePreviewLanguageFeatures;
  private boolean generateMetadataForMethodParameters;
  private boolean outputMessagesAboutWhatTheCompilerIsDoing;
  private boolean outputSourceLocationsOfDeprecatedUsages;
  private boolean terminateCompilationIfWarningsOccur;

  public Javac() {
    super("javac");
  }

  @Override
  public String toLabel() {
    return "Compile module(s): " + getModules();
  }

  @Override
  protected void addConfiguredArguments(Arguments arguments) {
    super.addConfiguredArguments(arguments);

    var version = getVersionOfModulesThatAreBeingCompiled();
    if (assigned(version)) arguments.add("--module-version", version);

    var patterns = getPatternsWhereToFindSourceFiles();
    if (assigned(patterns)) arguments.add("--module-source-path", joinPaths(patterns));

    var specific = getPathsWhereToFindSourceFiles();
    if (assigned(specific))
      for (var entry : specific.entrySet())
        arguments.add("--module-source-path", entry.getKey() + '=' + join(entry.getValue()));

    var patches = getPathsWhereToFindMoreAssetsPerModule();
    if (assigned(patches))
      for (var patch : patches.entrySet())
        arguments.add("--patch-module", patch.getKey() + '=' + join(patch.getValue()));

    var modulePath = getPathsWhereToFindApplicationModules();
    if (assigned(modulePath)) arguments.add("--module-path", join(modulePath));

    var release = getCompileForVirtualMachineVersion();
    if (assigned(release)) arguments.add("--release", release);

    if (isEnablePreviewLanguageFeatures()) arguments.add("--enable-preview");

    if (isGenerateMetadataForMethodParameters()) arguments.add("-parameters");

    if (isOutputSourceLocationsOfDeprecatedUsages()) arguments.add("-deprecation");

    if (isOutputMessagesAboutWhatTheCompilerIsDoing()) arguments.add("-verbose");

    if (isTerminateCompilationIfWarningsOccur()) arguments.add("-Werror");
  }

  public Version getVersionOfModulesThatAreBeingCompiled() {
    return versionOfModulesThatAreBeingCompiled;
  }

  public Javac setVersionOfModulesThatAreBeingCompiled(
      Version versionOfModulesThatAreBeingCompiled) {
    this.versionOfModulesThatAreBeingCompiled = versionOfModulesThatAreBeingCompiled;
    return this;
  }

  public List<String> getPatternsWhereToFindSourceFiles() {
    return patternsWhereToFindSourceFiles;
  }

  public Javac setPatternsWhereToFindSourceFiles(List<String> patterns) {
    this.patternsWhereToFindSourceFiles = patterns;
    return this;
  }

  public Map<String, List<Path>> getPathsWhereToFindSourceFiles() {
    return pathsWhereToFindSourceFiles;
  }

  public Javac setPathsWhereToFindSourceFiles(Map<String, List<Path>> map) {
    this.pathsWhereToFindSourceFiles = map;
    return this;
  }

  public Map<String, List<Path>> getPathsWhereToFindMoreAssetsPerModule() {
    return pathsWhereToFindMoreAssetsPerModule;
  }

  public Javac setPathsWhereToFindMoreAssetsPerModule(Map<String, List<Path>> map) {
    this.pathsWhereToFindMoreAssetsPerModule = map;
    return this;
  }

  public List<Path> getPathsWhereToFindApplicationModules() {
    return pathsWhereToFindApplicationModules;
  }

  public Javac setPathsWhereToFindApplicationModules(
      List<Path> pathsWhereToFindApplicationModules) {
    this.pathsWhereToFindApplicationModules = pathsWhereToFindApplicationModules;
    return this;
  }

  public int getCompileForVirtualMachineVersion() {
    return compileForVirtualMachineVersion;
  }

  public Javac setCompileForVirtualMachineVersion(int release) {
    this.compileForVirtualMachineVersion = release;
    return this;
  }

  public boolean isEnablePreviewLanguageFeatures() {
    return enablePreviewLanguageFeatures;
  }

  public Javac setEnablePreviewLanguageFeatures(boolean preview) {
    this.enablePreviewLanguageFeatures = preview;
    return this;
  }

  public boolean isGenerateMetadataForMethodParameters() {
    return generateMetadataForMethodParameters;
  }

  public Javac setGenerateMetadataForMethodParameters(boolean parameters) {
    this.generateMetadataForMethodParameters = parameters;
    return this;
  }

  public boolean isOutputMessagesAboutWhatTheCompilerIsDoing() {
    return outputMessagesAboutWhatTheCompilerIsDoing;
  }

  public Javac setOutputMessagesAboutWhatTheCompilerIsDoing(boolean verbose) {
    this.outputMessagesAboutWhatTheCompilerIsDoing = verbose;
    return this;
  }

  public boolean isOutputSourceLocationsOfDeprecatedUsages() {
    return outputSourceLocationsOfDeprecatedUsages;
  }

  public Javac setOutputSourceLocationsOfDeprecatedUsages(boolean deprecation) {
    this.outputSourceLocationsOfDeprecatedUsages = deprecation;
    return this;
  }

  public boolean isTerminateCompilationIfWarningsOccur() {
    return terminateCompilationIfWarningsOccur;
  }

  public Javac setTerminateCompilationIfWarningsOccur(boolean error) {
    this.terminateCompilationIfWarningsOccur = error;
    return this;
  }
}

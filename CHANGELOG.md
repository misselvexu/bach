# Changelog
All notable changes to [Bach.java](https://github.com/sormuras/bach) will be documented in this `CHANGELOG.md` file.

The format is based on [Keep a Changelog](https://keepachangelog.com),
and this project adheres to [JEP 223: New Version-String Scheme](https://openjdk.java.net/jeps/223).

## Version [11-ea] unreleased

_nothing, yet_

## Bach.java [11.9] released 2020-10-03

- https://github.com/sormuras/bach/releases/tag/11.9
- https://repo.maven.apache.org/maven2/de/sormuras/bach/de.sormuras.bach/11.9

### New Features And Enhancements
- Support `bach.javac.release` system property
- Support GitHub release-based Java module link computation
- Find system tool providers per default using system's class loader

### Bug Fixes
- Add missing explicit constructors
- Prevent NPE in `CollectingLogger`

## Version [11.8] released 2020-08-25

- https://github.com/sormuras/bach/releases/tag/11.8
- https://repo.maven.apache.org/maven2/de/sormuras/bach/de.sormuras.bach/11.8

### Breaking Changes
- Remove tool call tweaks from code spaces - use `Tweak` instead [#162]
### New Features And Enhancements
- Generate Maven Consumer POM files into `.bach/workspace/deploy/maven` folder
- New JitPack-based factory in `Link` that supports multi-module projects
- New feature flag `bach.clean-classes-directories` (defaults to `false`) added
### Bug Fixes
- Fix running custom build program
- Fix JitPack URI for single module projects

## Version [11.7] released 2020-08-05

- https://github.com/sormuras/bach/releases/tag/11.7
- https://repo.maven.apache.org/maven2/de/sormuras/bach/de.sormuras.bach/11.7

### Breaking Changes
- Drop "code space" from `Project`'s API [#156]
- Promote `MainSpace.Modifier` enumeration to top-level type named `Feature`
### New Features And Enhancements
- Compile and run custom build program
- Add `Tweak` interface added for tweaking all tool calls
### Bug Fixes
- Fix `--class-path` and `--module-path` arguments for MR-JAR builds
- Prevent NPE in class `Scribe` when adding an enumeration constant

## Version [11.6] released 2020-07-23

- https://github.com/sormuras/bach/releases/tag/11.6
- https://repo.maven.apache.org/maven2/de/sormuras/bach/de.sormuras.bach/11.6

### Breaking Changes
- Revise `Bach` API [#151]
- Interface `Scribe` moved to hidden `de.sormuras.bach.internal` package
### New Features And Enhancements
- New `Base.isDefault()` and `Base.isDefaultIgnoreBaseDirectory()` methods
- Warnings for computed links, module-uri pairs, are now emitted en bloc
- Logbook's layout improved and markdown syntax warnings removed
- A copy of `logbook.md` is stored in `.bach/workspace/logbooks` using a file name with a timestamp
- Bach's API documentation now contains sources and use index

## Version [11.5] released 2020-07-17

- https://github.com/sormuras/bach/releases/tag/11.5
- https://repo.maven.apache.org/maven2/de/sormuras/bach/de.sormuras.bach/11.5

### Breaking Changes
- API changes, some here, some there
### New Features And Enhancements
- `Project::toStrings()` now emits Java source code

## Version [11.4] released 2020-07-14

- https://github.com/sormuras/bach/releases/tag/11.4
- https://repo.maven.apache.org/maven2/de/sormuras/bach/de.sormuras.bach/11.4

### Breaking Changes
- Purify `Bach` API [#138]
- Move `Project` to package `de.sormuras.bach`
### New Features And Enhancements
- Normal output is now separated into steps
- Compile `test-preview` modules and run 'em 
### Bug Fixes
- Prevent NPE when parsing URI's fragment in class `Link`

## Version [11.3] released 2020-07-10

- https://github.com/sormuras/bach/releases/tag/11.3
- https://repo.maven.apache.org/maven2/de/sormuras/bach/de.sormuras.bach/11.3

### Breaking Changes
- More API changes - `Bach` is the central builder workflow class

## Version [11.3-M1] released 2020-07-03

- https://github.com/sormuras/bach/releases/tag/11.3-M1
- https://repo.maven.apache.org/maven2/de/sormuras/bach/de.sormuras.bach/11.3-M1

### Breaking Changes
- API redesign - update all custom build programs

## Version [11.2] released 2020-06-10

- https://github.com/sormuras/bach/releases/tag/11.2
- https://repo.maven.apache.org/maven2/de/sormuras/bach/de.sormuras.bach/11.2.0.3

### Breaking Changes
- Replace generated `Bach.java` with module `de.sormuras.bach` [#126]
- Use `@` character as version separator in API documetation JAR file
### New Features And Enhancements
- Add **JLWGL 3.2.3** module mappings https://search.maven.org/search?q=g:org.lwjgl
- Deploy snapshot builds to GitHub Packages https://github.com/sormuras/bach/packages
- Deploy releases to Bintray and publish them to Maven Central https://search.maven.org/search?q=g:de.sormuras.bach
### Bug Fixes
- Fix parsing of `last-modifier` HTTP header when resolving modules

## Version [11.1] released 2020-06-01

https://github.com/sormuras/bach/releases/tag/11.1

- ☕ **Java**, pristine Java
- 🚀 **Zero-installation** build mode
- 📚 **API** documentation
- 💾 Custom runtime **image**
- ✔ Automated Checks: **Test** program and JUnit Platform support
- 🗄 **Multi-Release** modular JAR file support
- 🧩 **3<sup>rd</sup>-party modules** in plain sight
- 📋 Structured build **summary** with history

## Version [2.1] released 2020-02-08

- https://github.com/sormuras/bach/releases/tag/2.1
- https://fosdem.org/2020/schedule/event/bach

## Version [1.9.10] released 2019-10-23

- https://github.com/sormuras/bach/releases/tag/1.9.10

[11-ea]: https://github.com/sormuras/bach/compare/11.9...master
[11.9]: https://github.com/sormuras/bach/compare/11.8...11.9
[11.8]: https://github.com/sormuras/bach/compare/11.7...11.8
[11.7]: https://github.com/sormuras/bach/compare/11.6...11.7
[11.6]: https://github.com/sormuras/bach/compare/11.5...11.6
[11.5]: https://github.com/sormuras/bach/compare/11.4...11.5
[11.4]: https://github.com/sormuras/bach/compare/11.3...11.4
[11.3]: https://github.com/sormuras/bach/compare/11.3-M1...11.3
[11.3-M1]: https://github.com/sormuras/bach/compare/11.2...11.3-M1
[11.2]: https://github.com/sormuras/bach/compare/11.1...11.2
[11.1]: https://github.com/sormuras/bach/commits/11.1
[2.1]: https://github.com/sormuras/bach/compare/2.0...2.1
[1.9.10]: https://github.com/sormuras/bach/compare/1.9.1...1.9.10
[#126]: https://github.com/sormuras/bach/issues/126
[#138]: https://github.com/sormuras/bach/issues/138
[#151]: https://github.com/sormuras/bach/issues/151
[#156]: https://github.com/sormuras/bach/issues/156
[#162]: https://github.com/sormuras/bach/issues/162

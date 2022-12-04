# README Bnd/Bndtools

Bnd/Bndtools is a swiss army knife for OSGi. It creates manifest headers for you based on analyzing the class code, it verifies your settings, it manages project dependencies, diffs jars, and much more.

Information about Bnd can be found at <https://bnd.bndtools.org> and information about Bndtools can be found at <https://bndtools.org>.

## Repository

The git repository contains all the code.

## License

This program and the accompanying materials are made available under the terms of the Apache License, Version 2.0 which is available at <https://www.apache.org/licenses/LICENSE-2.0>, or the Eclipse Public License 2.0 which is available at <https://www.eclipse.org/legal/epl-2.0>.

## API

Though we are usually pretty good at backward compatibility, there is no guarantee.
This is an unpaid project. Bnd properties will be backward compatible if there is any possibility.

If you're building a tool with a general audience  that includes Bnd for a general audience I would appreciate if you got in touch with us so we can keep in touch. We are always interested in ideas.

## Eclipse

Bndtools is the development environment of Bnd.

## Building

We use Gradle and Maven to build and the repo includes `gradlew` and `mvnw` at the necessary versions.
We require at least Java 17.

- `./gradlew :build` - Assembles and tests the Bnd Workspace projects. This must be run before building the Bnd Maven and Gradle plugins.
- `./gradlew :gradle-plugins:build` - Assembles and tests the Bnd Gradle plugins.
- `./mvnw --file=maven install` - Assembles and tests the Bnd Maven plugins.
- `./gradlew :publish` - Assembles and publishes the Bnd Workspace projects into `dist/bundles`.
- `./gradlew :gradle-plugins:publish` - Assembles and publishes the Bnd Gradle plugins into `dist/bundles`.
- `./mvnw -Pdist --file=maven deploy` - Assembles and publishes the Bnd Maven plugins into `dist/bundles`.

Bnd/Bndtools is continuously built on [GitHub Actions](https://github.com/bndtools/bnd/actions/workflows/cibuild.yml).
[![GitHub Actions CI Build Status](https://github.com/bndtools/bnd/actions/workflows/cibuild.yml/badge.svg)](https://github.com/bndtools/bnd/actions/workflows/cibuild.yml)

[CodeQL](https://github.com/bndtools/bnd/security/code-scanning?query=tool%3ACodeQL) is used for continuous security analysis.

A Maven snapshot repository of the latest build is available at <https://bndtools.jfrog.io/bndtools/libs-snapshot-local>.

### Using the latest development SNAPSHOT build of Bnd/Bndtools

* [Bnd Gradle Plugins](gradle-plugins/README.md#using-the-latest-development-snapshot-build-of-the-bnd-gradle-plugins)
* [Bnd Maven Plugins](maven/README.md#using-the-latest-development-snapshot-build-of-the-bnd-maven-plugins)
* Bndtools snapshot p2 update site: <https://bndtools.jfrog.io/bndtools/update-snapshot>

### Using the latest development Milestone/Release Candidate build of Bnd/Bndtools

* [Bnd Gradle Plugins](gradle-plugins/README.md#using-the-latest-milestonerelease-candidate-build-of-the-bnd-gradle-plugins)
* [Bnd Maven Plugins](maven/README.md#using-the-latest-milestonerelease-candidate-build-of-the-bnd-maven-plugins)
* Bndtools Milestone/Release Candidate p2 update site: <https://bndtools.jfrog.io/bndtools/update-rc>

![Powered by Artifactory](https://github.com/bndtools/bnd/raw/master/docs/img/Powered-by-artifactory_04.png)

## Release

Release versions of Bnd artifacts including Maven and Gradle plugins are available from [Maven Central](https://search.maven.org/search?q=g:biz.aQute.bnd). Release versions of the Bnd Gradle plugins are also available from the [Gradle Plugin repository](https://plugins.gradle.org/search?term=biz.aQute.bnd).

Release versions of the Bndtools Eclipse Features are available from the Bndtools p2 update site: <https://bndtools.jfrog.io/bndtools/update-latest>.

To see older versions of the Bndtools Eclipse Features in Eclipse's "Install New Software" dialog, make sure to uncheck "Show only latest versions of available software"

<img src="https://user-images.githubusercontent.com/277682/140074527-388c3cd2-f1ad-4c4f-8fce-3d3ebf98ba61.png" width="300" alt="Uncheck Show only latest versions of available software">

For macOS there is also a [Homebrew formula](https://formulae.brew.sh/formula/bnd).

## Feedback

Feedback is always welcome, for general discussions use the [bnd discourse site](https://bnd.discourse.group) mail list. This site can be used to discuss many different aspects of bnd and the many projects it supports. Including feedback and proposals for new functionality.

Bugs and issues should go to <https://github.com/bndtools/bnd/issues>.


## Contributing

Want to hack on Bnd/Bndtools? There are [instructions](CONTRIBUTING.md) to get you started.

They are probably not perfect, please let us know if anything feels wrong or incomplete.

## Acknowledgments

YourKit supports open source projects with its full-featured Java Profiler. YourKit, LLC is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/index.jsp) and [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/index.jsp), innovative and intelligent tools for profiling Java and .NET applications.

[![YourKit](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/)

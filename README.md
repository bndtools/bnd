# README Bnd/Bndtools

Bnd/Bndtools is a swiss army knife for OSGi. It creates manifest headers for you based on analyzing the class code, it verifies your settings, it manages project dependencies, diffs jars, and much more.

Information about Bnd can be found at <https://bnd.bndtools.org> and information about Bndtools can be found at <https://bndtools.org>.

## Repository

The git repository contains all the code.

## License

This program and the accompanying materials are made available under the terms of the Apache License, Version 2.0 which is available at <https://www.apache.org/licenses/LICENSE-2.0>, or the Eclipse Public License 2.0 which is available at <http://www.eclipse.org/legal/epl-2.0>.

## API

Though we am usually pretty good at backward compatibility, there is no guarantee.
This is an unpaid project. Bnd properties will be backward compatible if there is any possibility.

If you're building a tool with a general audience  that includes Bnd for a general audience I would appreciate if you got in touch with us so we can keep in touch. We are always interested in ideas.

## Eclipse

Bndtools is the development environment of Bnd.

## Building

Gradle is used to build Bnd/Bndtools.
The repo has a `build.gradle` file that builds all projects in proper order.

`./gradlew` - Assembles, tests and releases the non-maven projects into `dist/bundles`.

`./gradlew :build :maven:deploy` - Assembles and releases the projects into `dist/bundles`.

The repo includes the gradle wrapper, `gradlew`, command.

Bnd/Bndtools is continuously built on [GitHub Actions](https://github.com/bndtools/bnd/actions?query=workflow%3A%22CI%20Build%22).
[![GitHub Actions CI Build Status](https://github.com/bndtools/bnd/workflows/CI%20Build/badge.svg)](https://github.com/bndtools/bnd/actions?query=workflow%3A%22CI%20Build%22)

[CodeQL](https://github.com/bndtools/bnd/security/code-scanning?query=tool%3ACodeQL) is used for continuous security analysis.

A Maven snapshot repository of the latest build is available at <https://bndtools.jfrog.io/bndtools/libs-snapshot-local>.

### Using the latest development SNAPSHOT build of Bnd/Bndtools

* [Bnd Gradle Plugins](biz.aQute.bnd.gradle/README.md#using-the-latest-development-snapshot-build-of-the-bnd-gradle-plugins)
* [Bnd Maven Plugins](maven/README.md#using-the-latest-development-snapshot-build-of-the-bnd-maven-plugins)
* Bndtools snapshot p2 update site: <https://bndtools.jfrog.io/bndtools/update-snapshot>

![Powered by Artifactory](https://github.com/bndtools/bnd/raw/master/docs/img/Powered-by-artifactory_04.png)

## Release

Release versions of Bnd artifacts including Maven and Gradle plugins are available from Maven Central. Release versions of the Bnd Gradle plugins are also available from the Gradle Plugin repository.

Release versions of Bndtools are available from the Bndtools p2 update site: <https://bndtools.jfrog.io/bndtools/update-latest>.

## Feedback

Feedback is always welcome, for general discussions use the [bnd discourse site](https://bnd.discourse.group) mail list. This site can be used to discuss many different aspects of bnd and the many projects it supports. Including feedback and proposals for new functionality.

Bugs and issues should go to <https://github.com/bndtools/bnd/issues>.


## Contributing

Want to hack on Bnd/Bndtools? There are [instructions](CONTRIBUTING.md) to get you started.

They are probably not perfect, please let us know if anything feels wrong or incomplete.

## Acknowledgments

YourKit supports open source projects with its full-featured Java Profiler. YourKit, LLC is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/index.jsp) and [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/index.jsp), innovative and intelligent tools for profiling Java and .NET applications.

[![YourKit](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/)

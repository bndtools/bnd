# README Bnd/Bndtools
Bnd/Bndtools is a swiss army knife for OSGi, it creates manifest headers for you based on 
analyzing the class code, it verifies your settings, it manages project dependencies,
gives you quote of the day, diffs jars, and much more. 

Information about Bnd can be found at <https://bnd.bndtools.org> and 
information about Bndtools can be found at <https://bndtools.org>.

## Repository
The git repository contains all code. 

## License
This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
<https://www.apache.org/licenses/LICENSE-2.0>, or
the Eclipse Public License 2.0 which is available at
<http://www.eclipse.org/legal/epl-2.0>.

## API
Though we am usually pretty good at backward compatibility, there is no guarantee. This
is an unpaid project. Bnd properties will be backward compatible
if there is any possibility.

If you're building a tool with a general audience  that includes Bnd 
for a general audience I would appreciate if you got in touch with us so we can keep 
in touch. We are always interested in ideas.

## Eclipse
Bndtools is the development environment of Bnd.

## Building
Gradle is used to build Bnd/Bndtools. The workspace root has a `build.gradle` file that builds all projects in proper order.

`./gradlew`              - Assembles, tests and releases the non-maven projects into `dist/bundles`  
`./gradlew :build :maven:deploy`  - Assembles and releases the projects into `dist/bundles`  

The workspace root includes the gradle wrapper, `gradlew`, command.

[![GitHub Actions CI Build Status](https://github.com/bndtools/bnd/workflows/CI%20Build/badge.svg)](https://github.com/bndtools/bnd/actions?query=workflow%3A%22CI%20Build%22)

## Release
Bnd/Bndtools is continuously built on [GitHub Actions](https://github.com/bndtools/bnd/actions?query=workflow%3A%22CI%20Build%22).

A Maven repository of the latest build is available at <https://bndtools.jfrog.io/bndtools/libs-snapshot>.
A snapshot version of all the bundles and Maven and Gradle plugins is there.

### Using the latest development SNAPSHOT build of Bnd/Bndtools

* [Bnd Gradle Plugins](biz.aQute.bnd.gradle/README.md#using-the-latest-development-snapshot-build-of-the-bnd-gradle-plugins)
* [Bnd Maven Plugins](maven/README.md#using-the-latest-development-snapshot-build-of-the-bnd-maven-plugins)
* [Eclipse p2 update site for Bndtools](https://bndtools.jfrog.io/bndtools/update) <https://bndtools.jfrog.io/bndtools/update>

![Powered by Artifactory](https://github.com/bndtools/bnd/raw/master/docs/img/Powered-by-artifactory_04.png)

## Feedback
Feedback is always welcome, for general discussions use the [bndtools-users@googlegroups.com](https://groups.google.com/d/forum/bndtools-users) mail list. We also have the [bndtools-dev@googlegroups.com](https://groups.google.com/d/forum/bndtools-dev) mail list for discussions on the development of bnd.

Bugs and issues should go to <https://github.com/bndtools/bnd/issues>

Other feedback or specific functionality send to <Peter.Kriens@aQute.biz>

## Contributing

Want to hack on Bnd/Bndtools? There are [instructions](CONTRIBUTING.md) to get you
started.

They are probably not perfect, please let us know if anything feels
wrong or incomplete.

## Acknowledgments
YourKit supports open source projects with its full-featured Java Profiler. YourKit, LLC is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/index.jsp) and [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/index.jsp), innovative and intelligent tools for profiling Java and .NET applications.

[![YourKit](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/)

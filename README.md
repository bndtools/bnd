[![bnd](https://user-images.githubusercontent.com/200494/226292967-963bd722-96d9-4a46-9658-4699962032b0.png)](https://bnd.bndtools.org/)

[![GitHub release](https://img.shields.io/github/v/release/bndtools/bnd)](
https://github.com/bndtools/bnd/releases/latest
)
[![Rebuild](https://github.com/bndtools/bnd/actions/workflows/rebuild.yml/badge.svg)](https://github.com/bndtools/bnd/actions/workflows/rebuild.yml) 
[![CodeQL](https://github.com/bndtools/bnd/actions/workflows/codeql.yml/badge.svg)](https://github.com/bndtools/bnd/actions/workflows/codeql.yml) 
[![GitHub issues](https://img.shields.io/github/issues/bndtools/bnd)](https://github.com/bndtools/bnd/issues) 
[![GitHub commit activity](https://img.shields.io/github/commit-activity/m/bndtools/bnd)](https://github.com/bndtools/bnd/commits/)

# bnd & bndtools 

Bnd/Bndtools is a swiss army knife for OSGi. It creates manifest headers for you based on analyzing the class code, it verifies your settings, it manages project dependencies, diffs jars, and much more. At the core is a library with all the functions. The library is then used in a myriad of subsystems to provide the core functionality to the rest of the world.

* [bndlib and friends](https://bnd.bndtools.org) – The core library plus repository, resolve, etc.
* [maven plugins](maven-plugins/README.md) – A full set of maven plugins that make bnd useful for maven users
* [eclipse](https://bndtools.org) – Bndtools is the plugin for Eclipse that provides full GUI support for bnd. This is a p2 repository.
* [bnd](biz.aQute.bnd) – a command line utility with a hodgepodge of sometimes extremely useful functions. Can even be used instead of a build tool. is available through [Homebrew formula](https://formulae.brew.sh/formula/bnd).
* [gradle plugin(s)](gradle-plugins/README.md) – A bnd workspace plugin that builds identical to Eclipse's bndtool as well as a gradle plugin that provides bnd support for non-workspace projects
* ant – well ...

## Feedback

Well, assuming you were born after 1970 you probably know how to file an [issue](https://github.com/bndtools/bnd/issues)? :-) We got a good record fixing bugs. However, to help us, it always works best if you can provide the smallest possible example as a github repo. If the bug is consistent we tend fix it really fast. At least specify the environment where the bug appears, we got lots of variants.

General feedback is of course also always welcome! The [bnd discourse site](https://bnd.discourse.group) mail list is open for any topic even remotely associated with bnd. And we even love to hear also how people use the product that takes so much of our time.

And the ones we love most are of course [PRs](https://github.com/bndtools/bnd/pulls)! The documentation of bnd is fully included in this workspace. From a typo to a brilliant essay on Launchpad, submit a PR and we probably take it. Don't ask what bnd can do for you, ask what you can do for bnd! 

## License

This program and the accompanying materials are made available under the terms of the Apache License, Version 2.0 which is available at <https://www.apache.org/licenses/LICENSE-2.0>, or the Eclipse Public License 2.0 which is available at <https://www.eclipse.org/legal/epl-2.0>.

## API

We go out of our way to be backward compatible. There are thousands of extensive test cases and we use our own [baselining tool][1] to verify that we do not make gratuitious changes. Since this tool is part of Bndtools we often find out early enough that there are alternatives to breaking changes. We strictly follow OSGi semantic versioning. This means that we mark exported packages with a package version.

We try to keep major versions to a minimum but sometimes we must. This is clearly marked with a major release number. 

The master branch is release 7 currently and the Java release version is 17. Since some libraries are used in the embedded world that have a slower uptake of the Java release train we keep a classic branch that continues a major release 6 on Java 1.8. The classic branch does not include the plugins.

## Contributing

Want to work on Bnd/Bndtools? There are [instructions](CONTRIBUTING.md) to get you started. Please let us know if anything feels wrong or incomplete.

Some more instructions how to get started with bndtools development can be found [here](https://bndtools.org/development.html)

## Repo Structure

### Branches 

We have the following [branches](https://github.com/bndtools/bnd/branches/all):

* `master` – Where the work for the next release takes place. Pushes on this branch release to the snapshot repository
* `classic` – A continuation of 6.4 compiled on Java 1.8 for the poor developers stuck in the limbo of 1.8 and 17.
* `next` – Release branch to Maven central. Is used during the [release process](https://github.com/bndtools/bnd/wiki/Release-Process).

We generally do not use other branches. Issues and features are handled through PR's from forked repos.

### Issues

[Open issues](https://github.com/bndtools/bnd/issues) are the one _actively scheduled_ to go into the next milestone(s). 

_All other issues will be closed_. However, some issues we close because we just lack people that are interested in working on them. We explicitly mark those issues with [`abeyance`](https://github.com/bndtools/bnd/issues?q=is%3Aissue+label%3Aabeyance+). If you got time to spare, do not hesitate to open them and indicate you're willing to work on them. We will then properly schedule and track them. If you need to find them, use this filter: `is:issue label:abeyance`, make sure `is:open` is not in the filter.

### Milestones

We try to have a release every 3-6 months and plan these with [milestones](https://github.com/bndtools/bnd/milestones).

### PRs

[PRs](https://github.com/bndtools/bnd/pulls) should be submitted via another repo. After approval, they will be built to check for our requirements.

### Actions

Actions trigger workflows depending on the branch. PR's for verification require approval when not from a known contributor. We verify the code quality with CodeQL and the [contribution](CONTRIBUTING.md) rules are also checked.


## Installation (Stable Releases)

Use the latest **stable release** for normal development.

- Maven Central (group id: [`biz.aQute.bnd`](https://repo.maven.apache.org/maven2/biz/aQute/bnd)) - [https://repo.maven.apache.org/maven2](https://repo.maven.apache.org/maven2)
- Bndtools Eclipse Plugin p2 update site - [https://bndtools.jfrog.io/bndtools/update-latest](https://bndtools.jfrog.io/bndtools/update-latest)

### Bnd Maven Plugin (stable)

```xml
<plugin>
  <groupId>biz.aQute.bnd</groupId>
  <artifactId>bnd-maven-plugin</artifactId>
  <version>7.2.0</version>
</plugin>
```

### Bnd Gradle Plugin (stable)

```gradle
plugins {
  id "biz.aQute.bnd.builder" version "7.2.0"
}
```


## Installation (Release Candidates)

Release Candidates are **pre-release builds** published for testing.

- RC artifacts - [https://bndtools.jfrog.io/bndtools/libs-release/](https://bndtools.jfrog.io/bndtools/libs-release/)
- Bndtools Eclipse Plugin RC update site - [https://bndtools.jfrog.io/bndtools/update-rc](https://bndtools.jfrog.io/bndtools/update-rc)

```gradle
pluginManagement {
  repositories {
    gradlePluginPortal()
    maven { url "https://bndtools.jfrog.io/bndtools/libs-release/" }
  }
}

plugins {
  id "biz.aQute.bnd.builder" version "X.Y.Z.RC1"
}
```

```xml
<pluginRepositories>
  <pluginRepository>
    <id>bnd-rc</id>
    <url>https://bndtools.jfrog.io/bndtools/libs-release/</url>
  </pluginRepository>
</pluginRepositories>

<plugin>
  <groupId>biz.aQute.bnd</groupId>
  <artifactId>bnd-maven-plugin</artifactId>
  <version>X.Y.Z.RC1</version>
</plugin>
```


## Installation (Development Snapshot Builds — master branch)

Snapshot builds are produced from the **current master branch** and may be unstable.

- Snapshot artifacts - [https://bndtools.jfrog.io/bndtools/libs-snapshot/](https://bndtools.jfrog.io/bndtools/libs-snapshot/)
- Bndtools Eclipse Plugin snapshot update site - [https://bndtools.jfrog.io/bndtools/update-snapshot](https://bndtools.jfrog.io/bndtools/update-snapshot)

```gradle
pluginManagement {
  repositories {
    gradlePluginPortal()
    maven { url "https://bndtools.jfrog.io/bndtools/libs-snapshot/" }
  }
}

plugins {
  id "biz.aQute.bnd.builder" version "X.Y.Z-SNAPSHOT"
}
```

```xml
<pluginRepositories>
  <pluginRepository>
    <id>bnd-snapshots</id>
    <url>https://bndtools.jfrog.io/bndtools/libs-snapshot/</url>
  </pluginRepository>
</pluginRepositories>

<plugin>
  <groupId>biz.aQute.bnd</groupId>
  <artifactId>bnd-maven-plugin</artifactId>
  <version>X.Y.Z-SNAPSHOT</version>
</plugin>
```

## Building

We use Gradle and Maven to build and the repo includes `gradlew` and `mvnw` at the necessary versions.
We require at least Java 17.

- `./gradlew :build` - Assembles and tests the Bnd Workspace projects. This must be run before building the Bnd Maven and Gradle plugins.
- `./gradlew :gradle-plugins:build` - Assembles and tests the Bnd Gradle plugins.
- `./mvnw install` - Assembles and tests the Bnd Maven plugins.
- `./gradlew :publish` - Assembles and publishes the Bnd Workspace projects into `dist/bundles`.
- `./gradlew :gradle-plugins:publish` - Assembles and publishes the Bnd Gradle plugins into `dist/bundles`.
- `./mvnw -Pdist deploy` - Assembles and publishes the Bnd Maven plugins into `dist/bundles`.

## Acknowledgments

YourKit supports open source projects with its full-featured Java Profiler. YourKit, LLC is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/index.jsp) and [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/index.jsp), innovative and intelligent tools for profiling Java and .NET applications.


![Powered by Artifactory](https://github.com/bndtools/bnd/raw/master/docs/img/Powered-by-artifactory_04.png)

[![YourKit](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/)

[icons8](https://icons8.com/) – For some of the icons

[1]: https://bnd.bndtools.org/chapters/180-baselining.html

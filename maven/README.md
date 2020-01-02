# Maven Plugins

The Maven Plugins require at least Maven 3.1.0. This is because bndlib uses SLF4J and Maven 3.1.0 or later [provides the SLF4J API][6].

This README, and the READMEs in the individual Bnd Maven Plugin folders,
represent the capabilities and features of the Bnd Maven Plugins in
the branch containing the READMEs. So for the `master` branch, this will be
the [latest development SNAPSHOT build](#using-the-latest-development-snapshot-build-of-the-bnd-maven-plugins).
See the appropriate Git tag for the README for the
Bnd Maven Plugin version you are using.

## bnd-plugin-parent

This project builds the various Maven plugins provided by the Bnd project,
and defines common dependency information.

These plugins are built using Maven (unlike the rest of Bnd) because it is
very hard to build a Maven plugin unless you use Maven to do it!

## [bnd-maven-plugin][1]

The core plugin, used to generate manifest and other metadata for
projects that build an OSGi bundle.

## [bnd-indexer-maven-plugin][2]

A plugin used to generate an OSGi repository index from a set of Maven
dependencies. The entries in the index will reference the location of
the bundles in the remote repositories to which they have been deployed.

## [bnd-baseline-maven-plugin][3]

A plugin used to validate that a bundle correctly uses semantic versioning
as described by the OSGi Alliance. This plugin will verify that the bundle
and package versions of a module's output artifact are correct based on:

* The bundle and package versions declared by the previously released
version of the module.
* Any changes that have been made to the packages exported by the bundle.
* Any internal changes within the bundle.

## [bnd-export-maven-plugin][4]

A plugin to export bndrun files.

## [bnd-resolver-maven-plugin][7]

A plugin to resolve bndrun files.

## [bnd-testing-maven-plugin][8]

A plugin to run integration tests from bndrun files.

## [bnd-run-maven-plugin][9]

A plugin to run a bndrun file.

## [bnd-reporter-maven-plugin][10]

A plugin to generate and export reports of projects.

# Building the Maven Plugins

You must first run `./gradlew :build` to build the Bnd artifacts and install them in your local
maven repo. You can then run Maven to build the Bnd Maven plugins. You can run
`./gradlew :maven:deploy` to build the Bnd Maven plugins with the `deploy` goal which will deploy
the built Bnd Maven plugins into the releaserepo.

---

# Using the latest development SNAPSHOT build of the Bnd Maven Plugins

If you want to try the latest development SNAPSHOT build of the
Bnd Maven Plugins, you will need to adjust your pom to refer to the snapshot
repository and select the latest version of the plugins. For example, edit the
pom's `pluginManagement` section, to configure the repository:

```xml
<pluginRepositories>
	<pluginRepository>
		<id>bnd-snapshots</id>
		<url>https://bndtools.jfrog.io/bndtools/libs-snapshot/</url>
		<layout>default</layout>
		<releases>
			<enabled>false</enabled>
		</releases>
	</pluginRepository>
</pluginRepositories>
```

[1]: bnd-maven-plugin/README.md
[2]: bnd-indexer-maven-plugin/README.md
[3]: bnd-baseline-maven-plugin/README.md
[4]: bnd-export-maven-plugin/README.md
[6]: https://maven.apache.org/maven-logging.html
[7]: bnd-resolver-maven-plugin/README.md
[8]: bnd-testing-maven-plugin/README.md
[9]: bnd-run-maven-plugin/README.md
[10]: bnd-reporter-maven-plugin/README.md

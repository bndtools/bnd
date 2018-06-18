# Maven Plugins

The Maven Plugins require at least Maven 3.1.0. This is because bndlib uses SLF4J and Maven 3.1.0 or later [provides the SLF4J API][6].

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

# Building the Maven Plugins

See the [.travis.yml][5] file in the root of the repo for the `script` section
detailing the commands to build the Maven plugins. You must first run `./gradlew`
to build the Bnd bundles and install them in your local maven repo. You can then
run Maven to build the Maven plugins.

[1]: bnd-maven-plugin/README.md
[2]: bnd-indexer-maven-plugin/README.md
[3]: bnd-baseline-maven-plugin/README.md
[4]: bnd-export-maven-plugin/README.md
[5]: ../.travis.yml
[6]: https://maven.apache.org/maven-logging.html
[7]: bnd-resolver-maven-plugin/README.md
[8]: bnd-testing-maven-plugin/README.md


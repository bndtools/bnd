bnd maven plugin
================
This plugin enables Maven-based builds on bnd/bndtools projects where the idea is that you use Bndtools in Eclipse as a development environment and have the option to use Maven to build your projects en a headless environment.
The result is a plugin that fully builds any bnd project. It even supports multiple bundle projects, the
multiple jars are then created with classifiers.


The simplest way of using it is as follows

1. Create a bndtools project, selecting the _maven_ project layout.
2. Add a pom.xml, with the following content:
```
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
   <modelVersion>4.0.0</modelVersion>

    <groupId>org.foo.bar</groupId>
    <artifactId>myBundle</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>bundle</packaging>

    <build>
        <plugins>
            <plugin>
                <groupId>biz.aQute.bnd</groupId>
                <artifactId>bnd-maven-plugin</artifactId>
                <version>0.2.0-SNAPSHOT</version> <!-- Make sure to put in the latest version -->
                <extensions>true</extensions>
            </plugin>
        </plugins>
    </build>
</project>
```

This basically builds the project using Bnd. Setting are taken from the bnd.bnd file. To get a Maven-style layout, this file could have content, similar to this:
```
  # These things are edited via bndtools
  -buildpath: osgi.core;version=4.0
  Private-Package: org.foo.bar
  Bundle-Activator: org.foo.bar.TestActivator
  Bundle-Version: 1.0.0.SNAPSHOT

  # Add these to get a maven-like structure on disk and file names in target
  -outputmask = ${@bsn}-${version;===S;${@version}}.jar
  target-dir = target
  bin = target/classes
  src = src/main/java
  testsrc = src/test/java
  testbin = target/test-classes
```

You can also include tests that are run via bnd. These tests are run as part of the integration-test phase in Maven. For example the ExampleTest that is created via the bndtoold 'Integration Testing' template just works with this.


Sample Projects
===============
Sample projects can be found in `bnd-maven-plugin-parent/samples` and subdirectories.

Open Issues
===========

Maven Release Process
---------------------
Typically in Maven you'd develop using -SNAPSHOT versions (note that the bnd-maven-plugin transforms x.y.z.SNAPSHOT into x.y.z-SNAPSHOT for Maven). Then when you're done developing you use the [maven-release-plugin](http://maven.apache.org/maven-release/maven-release-plugin/index.html) to create the non-snapshot poms, tag/release them etc. For people using this process, you'd really want the Bundle-Version in the bnd.bnd file to be updated as part of this as well. Note that the bnd-maven-plugin fails if there is a mismatch between the versions in the pom.xml and bnd.bnd. We need to investigate a little bit more how this can be done, for example by looking at how Tycho does this.

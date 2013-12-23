bnd maven plugin
================
This plugin is based on work started by Toni Menzel and others to enable Maven-based builds on bnd/bndtools projects where the idea is that you use Bndtools in Eclipse as a development environment and have the option to use Maven to build your projects en a headless environment.
The result is a plugin that fully builds any bnd project. It even supports multiple bundle projects, the
multiple jars are then created with classifiers.


The simplest way of using it is as follows

1. Generate a bndtools project.
2. Add a pom.xml, with the following content:
```
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
   <modelVersion>4.0.0</modelVersion>

    <groupId>org.foo.bar</groupId>
    <artifactId>myBundle</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>bnd</packaging>

    <build>
        <plugins>
            <plugin>
                <groupId>biz.aQute.bnd</groupId>
                <artifactId>bnd-maven-plugin</artifactId>
                <version>0.2.0-SNAPSHOT</version>
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
It would be nice if the above files could be generated automatically via bndtools. [Issue 800](https://github.com/bndtools/bndtools/issues/800) is about that.

You can also include tests that are run via bnd. These tests are run as part of the integration-test phase in Maven. For example the ExampleTest that is created via the bndtoold 'Integration Testing' template just works with this.

In the pom.xml above, the packaging type is 'bnd'. This was changed from the previous value 'bundle' to not interfere with the maven-bundle-plugin which uses that value. However, it was suggested to simply use 'jar' as the packaging type. This is possible, but requires quite a lot of configuration in the pom.xml. If we can have the bnd-maven-plugin automatically configure this, it would be a good option, but it needs to be investigated a bit more [(issue 449)](https://github.com/bndtools/bnd/issues/449).

Finally, regarding doing releases in Maven... Typically in Maven you'd develop using -SNAPSHOT versions (note that the bnd-maven-plugin transforms x.y.z.SNAPSHOT into x.y.z-SNAPSHOT for Maven). Then when you're done developing you use the [maven-release-plugin](http://maven.apache.org/maven-release/maven-release-plugin/index.html) to create the non-snapshot poms, tag/release them etc. For people using this process, you'd really want the Bundle-Version in the bnd.bnd file to be updated as part of this as well. Note that the bnd-maven-plugin fails if there is a mismatch between the versions in the pom.xml and bnd.bnd. We need to investigate a little bit more how this can be done, for example by looking at how Tycho does this.

Issues
======
There is a TODO in the cnf/pom.xml (a dep I do not understand)

I do not understand why I need to define the compiler plugin

I do not understand why the plugin creates a life cycle? How are multiple plugins resolved when each
defines its own life cycle.

There are several TODOs in the java source

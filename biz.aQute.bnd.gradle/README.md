# Gradle Plugins

Bnd includes Gradle plugins for [Gradle][1] users to build
Bnd projects in [Workspace builds][20] as well as in
[non-Workspace builds][21].
The [`biz.aQute.bnd.gradle`][2] jar contains the Bnd Gradle Plugins.
These plugins requires at least Gradle 2.0.

# Gradle Plugin for Workspace Builds

The Bnd Gradle Plugin for Workspace builds uses the information
specified in the project's `bnd.bnd` file and the workspace's
`cnf/build.bnd` file to build a project.

The Bnd Gradle Plugin for Workspace builds has the Gradle plugin
name `biz.aQute.bnd`.

## Workspace

When creating the cnf project for a new workspace with Bndtools, by
default, the files for a gradle build will be installed in the
workspace. This includes the following files in the root of the
workspace:

* [`gradle.properties`][11] - Some initial properties to configure the
Gradle build for the workspace. 
* [`settings.gradle`][12] - Initializes
the projects to be included in the Gradle build for the workspace. 
* [`build.gradle`][13] - Configures the projects in the Gradle build for
the workspace.

These files can be modified to customize the overall Gradle build for
the workspace. If special Gradle build behavior is needed, beyond
changes to the project's `bnd.bnd` file, then you should place a
`build.gradle` file in the root of the project and place your
customizations in there.

## Using Bnd Gradle Plugin for Workspace builds

If you are using the Gradle build added by Bndtools when creating the
cnf project in your workspace, you don't need to do anything else. If
you want to use the Bnd Gradle Plugin in your existing Gradle build, you
need to add the Bnd Gradle Plugin to your buildscript classpath and then
apply the plugin to your project. For example:

```groovy
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath 'biz.aQute.bnd:biz.aQute.bnd.gradle:3.2.0'
  }
}

apply plugin: 'biz.aQute.bnd'
```

## Gradle Tasks

The Bnd Gradle Plugin extends the standard [Gradle Java plugin][3]. It
modifies some of the standard Java plugin tasks as necessary and also
adds some additional tasks. Running `gradle tasks --all` in a project
will provide a complete list of the tasks available within the project.

The dependencies for the project are configured from the path
information specified in the `bnd.bnd` file such as [`-buildpath`][4]
and [`-testpath`][5]. These paths are then used by various tasks such as
`compileJava` and `compileTestJava`.

The `jar` task uses Bnd to build the project's bundles.

The `test` task runs any plain JUnit tests in the project.

The `check` task runs any OSGi JUnit tests in the project by launching a
framework and running the tests in the launched framework.

### Additional Tasks

The `release` task releases the project's bundles to the
[`-releaserepo`][6], if one is configured for the project.

The `releaseNeeded` task releases the project and all projects it
depends on.

The `checkNeeded` task runs the `check` task on the project and all
projects it depends on.

The `cleanNeeded` task cleans the project and all projects it depends
on.

The `export.`_name_ tasks, one per bndrun file in the project, exports
the _name_`.bndrun` file to an executable jar.

The `export` task will export all the bndrun files to executable jars.

The `runbundles.`_name_ tasks, one per bndrun file in the project,
creates a distribution of the [-runbundles][7] of the _name_`.bndrun`
file.

The `runbundles` task will create distributions of the runbundles for
all the bndrun files.

The `resolve.`_name_ tasks, one per bndrun file in the project, resolves
the _name_`.bndrun` file and updates the `-runbundles` instruction in the
file.

The `run.`_name_ tasks, one per bndrun file in the project, runs
the _name_`.bndrun` file.

The `echo` task will display some help information on the dependencies,
paths and configuration of the project.

The `bndproperties` task will display the Bnd properties of the project.

## Customizing a project's Gradle build

If you do need to write a `build.gradle` file for a Bnd project, there
are some properties of the Bnd Gradle Plugin you will find useful.

* The `bndWorkspace` property of the `rootProject` contains the
[Workspace][8] object. 
* The `bnd.project` property of the project
contains the [Project][9] object.

Bnd properties for a project can be accessed in several ways. Given the
example property name `foo`, you can use the [`bnd` function][15],
`bnd('foo', 'defaultValue')`, or directly from the [bnd extension][16],
`bnd.foo`. To access Bnd properties without any macro processing you can
use the [`bndUnprocessed` function][15], `bndUnprocessed('foo', 'defaultValue')`.

# Gradle Plugin for non-Workspace Builds

Sometimes developers want to build bundles but they don't use a full Bnd
Workspace based build environment! Go figure?!

So now Bnd offers [Gradle][1] support for building bundles in
_typical_ Gradle build environments. There are two ways this support can
be used. You can apply the `biz.aQute.bnd.builder` plugin to your
project and you can use the new `Bundle` or `Baseline` task types.

## Using Bnd Builder Gradle Plugin

To get the Bnd Builder Gradle Plugin on your `buildscript`, use the following:

```groovy
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath 'biz.aQute.bnd:biz.aQute.bnd.gradle:3.2.0'
  }
}
```

## Apply the `biz.aQute.bnd.builder` plugin to your project

You can apply the `biz.aQute.bnd.builder` plugin to your project. This
plugin extends the normal [`java` plugin][3] by:

* Extending the `jar` task. Some new properties are added to the `jar`
task and the task actions are extended to use Bnd to generate a bundle.
The bundle will contain all the content configured into the `jar` task
plus whatever additional content is included via the Bnd instructions.
* Adds a `baseline` configuration
* Adds a `baseline` task of type `Baseline` which baselines the bundle
generated by the `jar` task against the prior version of that bundle in
the repositories.

```groovy
apply plugin: 'biz.aQute.bnd.builder'
```

## Create a task of the `Bundle` type

You can also create a new task of the `Bundle` type. This task type is
an extension of the [`Jar` task type][18] that adds new properties and
uses Bnd to generate a bundle. The bundle will contain all the content
configured into the task plus whatever additional content is included
via the Bnd instructions. For example:

```groovy
import aQute.bnd.gradle.Bundle

apply plugin: 'java'

task bundle(type: Bundle) {
  from sourceSets.main.output
}
```

In either usage mode, there are three properties, in addition to the
`Jar` task properties, which can be configured.

### bndfile

This is the File object to use for the bnd file to use to create the
bundle. The default value is `project.file('bnd.bnd')`. If the bnd file
does not exist, this is OK. But without some instructions to Bnd, your
bundle will not be very interesting.

### configuration

This is the Configuration object to use as the classpath for the Bnd
builder. The default value is the `project.configurations.compileClasspath`
Configuration (or the `project.configurations.compile` Configuration in
Gradle versions prior to 2.12).
You will only need to specify this property if you want
to use a different Configuration for the classpath or the default
Configuration does not exist.

### sourceSet

This is the SourceSet object to use as the sourcepath for the Bnd
builder. The default value is the `project.sourceSets.main` SourceSet.
You will only need to specify this property if you want to use a different
SourceSet for the sourcepath or the default SourceSet does not exist.

### Example

```groovy
import aQute.bnd.gradle.Bundle

apply plugin: 'java'

configurations {
  bundleCompile
}
sourceSets {
  bundle
}
task bundle(type: Bundle) {
  from sourceSets.bundle.output
  bndfile = project.file('bundle.bnd')
  configuration = configurations.bundleCompile
  sourceSet = sourceSets.bundle
}
```

### Instructing Bnd on how to build your bundle

The normal way to instruct Bnd on how to build the bundle is to use a
bnd file. This file will include the Bnd instructions like
`Export-Package`, etc. However, you can also use the `manifest` property
to instruct Bnd. For example:

```groovy
apply plugin: 'biz.aQute.bnd.builder'

jar {
    manifest {
        attributes('Export-Package': 'com.acme.api.*',
                   '-sources': 'true',
                   '-include': 'other.bnd')
    }
}
```

You can even use a combination of the `manifest` property and a bnd
file. But the bnd file takes priority over the `manifest` property. So
if the same header is in both places, the one in the bnd file will be
used and the one in the `manifest` property will be ignored.

## Create a task of the `Baseline` type

You can also create a new task of the `Baseline` type. This task type
will baseline a bundle against a different version of the bundle. For
example:

```groovy
import aQute.bnd.gradle.Baseline

apply plugin: 'java'

configurations {
  baseline
}
dependencies {
  baseline('group': group, 'name': jar.baseName, 'version': "(,${jar.version})") {
    transitive false
  }
}
task baseline(type: Baseline) {
  bundle jar
  baseline configurations.baseline
}
```
There are four properties which can be configured for a Baseline task:

### ignoreFailures

If `true` the build will not fail due to baseline problems; instead an
error message will be logged. Otherwise, the build will fail. The
default is `false`.

### baselineReportDirName

This is the name of the baseline reports directory. Can be a name or a
path relative to the project's [`reporting.baseDir`][19]. The default name is
`baseline`.

### bundle

This is the bundle to be baselined. It can either be a File or a task
that produces a bundle. This property must be set.

### baseline

This is the baseline bundle. It can either be a File or a Configuration.
If a Configuration is specified, it must contain a single file;
otherwise an exception will fail the build. This property must be set.

---

For full details on what the Bnd Gradle Plugins do, check out the
[source code][10].

[1]: http://gradle.org/
[2]: https://github.com/bndtools/bnd/tree/master/biz.aQute.bnd.gradle
[3]: http://gradle.org/docs/current/userguide/java_plugin.html
[4]: http://bnd.bndtools.org/instructions/buildpath.html
[5]: http://bnd.bndtools.org/instructions/testpath.html
[6]: http://bnd.bndtools.org/instructions/releaserepo.html
[7]: http://bnd.bndtools.org/instructions/runbundles.html
[8]: ../biz.aQute.bndlib/src/aQute/bnd/build/Workspace.java
[9]: ../biz.aQute.bndlib/src/aQute/bnd/build/Project.java
[10]: src/aQute/bnd/gradle
[11]: https://github.com/bndtools/bndtools/blob/master/org.bndtools.headless.build.plugin.gradle/resources/templates/filter/root/gradle.properties
[12]: https://github.com/bndtools/bndtools/blob/master/org.bndtools.headless.build.plugin.gradle/resources/templates/unprocessed/root/settings.gradle
[13]: https://github.com/bndtools/bndtools/blob/master/org.bndtools.headless.build.plugin.gradle/resources/templates/unprocessed/root/build.gradle
[14]: http://gradle.org/docs/current/userguide/plugins.html#sec:plugins_block
[15]: src/aQute/bnd/gradle/BndPluginConvention.groovy
[16]: src/aQute/bnd/gradle/BndProperties.groovy
[18]: https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Jar.html
[19]: https://docs.gradle.org/current/dsl/org.gradle.api.reporting.ReportingExtension.html#org.gradle.api.reporting.ReportingExtension:baseDir
[20]: #gradle-plugin-for-workspace-builds
[21]: #gradle-plugin-for-non-workspace-builds

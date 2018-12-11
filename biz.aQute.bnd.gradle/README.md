# Gradle Plugins

Bnd includes Gradle plugins for [Gradle][1] users to build
Bnd projects in [Workspace builds][20] as well as in
[non-Workspace builds][21].
The [`biz.aQute.bnd.gradle`][2] jar contains the Bnd Gradle Plugins.
These plugins requires at least Gradle 4.0 for Java 8 and at least
Gradle 4.2.1 for Java 9.

# Gradle Plugins for Workspace Builds

The Bnd Gradle Plugins for Workspace builds uses the information
specified in the workspace's `cnf/build.bnd` file and each project's
`bnd.bnd` file to build the projects.

The Bnd Gradle Plugins for Workspace builds consists of two plugins:

* The `biz.aQute.bnd.workspace` Gradle plugin can be applied in the
`settings.gradle` file or the root project's `build.gradle` file.
* The `biz.aQute.bnd` Gradle plugin is applied to each subproject
that is a Bnd project.

## Workspace

A Bnd Workspace is a folder containing a `cnf` project as well as a
number of peer folders each holding a Bnd project. So the Bnd Workspace
folder is the root project of the Gradle build for the Bnd Workspace.
The following files are in the root project:

* [`gradle.properties`][11] - Some initial properties to configure the
Gradle build for the workspace.
* [`settings.gradle`][12] - Initializes
the projects to be included in the Gradle build for the workspace.
* [`build.gradle`][13] - Configures the Gradle build for the workspace.

When creating a new Bnd Workspace with Bndtools, it will put these files
on the root folder of the workspace.

These files can be modified to customize the overall Gradle build for
the workspace. If special Gradle build behavior for a project is needed,
beyond changes to the project's `bnd.bnd` file, then you should place a
`build.gradle` file in the root of the project and place your
customizations in there.

## Using Bnd Gradle Plugins for Workspace builds

If you are using the Gradle build added by Bndtools when creating the
the Bnd Workspace, you don't need to do anything else.

If you want to use the Bnd Gradle Plugins in your existing Gradle build,
there are two approaches you can take. The main
approach is to edit `settings.gradle` as follows:

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'biz.aQute.bnd:biz.aQute.bnd.gradle:4.2.0'
  }
}
apply plugin: 'biz.aQute.bnd.workspace'
```

When you apply the `biz.aQute.bnd.workspace` Gradle plugin in the
`settings.gradle` file, the plugin will determine the Bnd project folders
in the Bnd Workspace, include them in the Gradle build, and apply itself
to the root project. This will result in the `biz.aQute.bnd` Gradle plugin
being applied to each project which is a Bnd project. In this approach, you
don't even need a `build.gradle` file for the root project. But you can
use a `build.gradle` file in the root project to apply common configuration
across all your Bnd projects:

```groovy
subprojects {
  if (plugins.hasPlugin('biz.aQute.bnd')) {
    // additional configuration for Bnd projects
  }
}
```

The second approach, for when you already have a `settings.gradle` file which
includes the desired projects and you don't want the set of projects to
include to be computed, is to edit the root project's `build.gradle` file
to include the following:

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'biz.aQute.bnd:biz.aQute.bnd.gradle:4.2.0'
  }
}
apply plugin: 'biz.aQute.bnd.workspace'
```

While this is the same as the previous `settings.gradle` example, since it
is the root project's `build.gradle` file, it requires that your `settings.gradle`
file has already included the necessary projects in the Gradle build. The plugin
will apply the `biz.aQute.bnd` Gradle plugin to each project which is a
Bnd project.

In general, your Gradle scripts will not apply the `biz.aQute.bnd` Gradle plugin
directly to a project since this is handled by using the `biz.aQute.bnd.workspace` Gradle
plugin in the `settings.gradle` file or the `build.gradle` file in the root project.

## Gradle Tasks

The `biz.aQute.bnd` Gradle Plugin extends the standard [Gradle Java plugin][3]. It
modifies some of the standard Java plugin tasks as necessary and also
adds some additional tasks. Running `gradle tasks --all` in a project
will provide a complete list of the tasks available within the project.

The dependencies for the project are configured from the path
information specified in the `bnd.bnd` file such as [`-buildpath`][4]
and [`-testpath`][5]. These paths are then used by various tasks such as
`compileJava` and `compileTestJava`.

The `jar` task uses Bnd to build the project's bundles.

The `test` task runs any plain JUnit tests in the project.

The `check` task runs all verification tasks in the project,
including `test` and `testOSGi`.

### Additional Tasks

The `release` task releases the project's bundles to the
[`-releaserepo`][6], if one is configured for the project.

The `releaseNeeded` task releases the project and all projects it
depends on.

The `testOSGi` task runs any OSGi JUnit tests in the project's `bnd.bnd`
file by launching a framework and running the tests in the launched
framework. This means the `bnd.bnd` file must have the necessary
`-runfw` and `-runbundles` to support the test bundles built by
the project. The `check` task depends on the `testOSGi` task.

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

The `resolve` task resolves all the bndrun files and updates the `-runbundles`
instruction in each bndrun file.

The `run.`_name_ tasks, one per bndrun file in the project, runs
the _name_`.bndrun` file.

The `testrun.`_name_ tasks, one per bndrun file in the project, runs
the OSGi JUnit tests in the _name_`.bndrun` file.

The `echo` task will display some help information on the dependencies,
paths and configuration of the project.

The `bndproperties` task will display the Bnd properties of the project.

## Customizing a project's Gradle build

If you do need to write a `build.gradle` file for a Bnd project, there
are some properties of the Bnd Gradle Plugins you will find useful.

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
project and you can use the new task types:

* `Bundle`
* `Baseline`
* `Resolve`
* `Export`
* `TestOSGi`
* `Index`
* `Bndrun`

## Using Bnd Builder Gradle Plugin

To get the Bnd Builder Gradle Plugin on your `buildscript`, use the following:

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'biz.aQute.bnd:biz.aQute.bnd.gradle:4.2.0'
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
The `bndfile` property of the `jar` task is set to `'bnd.bnd'`.
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

In either usage mode, there are four properties, in addition to the
`Jar` task properties, which can be configured.

### bndfile

The bnd file to use to create the bundle. If this property is not set or
the bnd file does not exist, then the `bnd` property will be used.

### bnd

The bnd instructions to use to create the
bundle. This property is ignored if the `bndfile` property refers
to a file that exists. If the `bndfile` property is not set or does not
refer to a file that exists, or this property is not set, this is OK.
But without some instructions to Bnd, your bundle will not be very
interesting.

### sourceSet

The SourceSet object to use for the Bnd
builder. The default value is _${project.sourceSets.main}_.
You will only need to specify this property if you want to use a different
SourceSet or the default SourceSet does not exist.

### classpath

The FileCollection object to use as the classpath for the Bnd
builder. The default value is _${sourceSet.compileClasspath}_.
You will only need to specify this property if you want
to use a different classpath or the default SourceSet does not exist.

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
  bndfile = 'bundle.bnd'
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
  baseline('group': group, 'name': jar.baseName, 'version': "(,${jar.version}[") {
    force = true
    transitive = false
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

The name of the baseline reports directory. Can be a name or a
path relative to the project's [`reporting.baseDir`][19]. The default name is
`baseline`.

### bundle

The bundle to be baselined. It can be anything that `Project.files(Object...)`
can accept to result in a single file. This property must be set.

### baseline

The baseline bundle. It can be anything that `Project.files(Object...)`
can accept to result in a single file. This property must be set.

## Create a task of the `Resolve` type

You can also create a new task of the `Resolve` type. This task type
will resolve a standalone bndrun file and set the `-runbundles` property
in the file. For example:

```groovy
import aQute.bnd.gradle.Resolve

task resolve(type: Resolve) {
  bndrun 'my.bndrun'
}
```
There are four properties which can be configured for a Resolve task:

### failOnChanges

If `true` the build will fail if the resolve operation changes the value of the
`-runbundles` property. The default is `false`.

### bndrun

The bndrun to be resolved. It can be anything that `Project.file(Object)`
can accept. This property must be set. The bndrun file must be a standalone bndrun
file since this is not a Workspace Build.

### bundles

The collection of files to use for locating bundles during the
bndrun resolution. The default is _${project.sourceSets.main.runtimeClasspath}_
plus _${project.configurations.archives.artifacts.files}_.

### reportOptional

If `true` failure reports will include optional requirements. The default is
`true`.

## Create a task of the `Export` type

You can also create a new task of the `Export` type. This task type
will export a standalone bndrun file. For example:

```groovy
import aQute.bnd.gradle.Export

task export(type: Export) {
  bndrun 'my.bndrun'
}
```
There are three properties which can be configured for an Export task:

### exporter

Bnd has two built-in exporter plugins. `bnd.executablejar` exports an
executable jar and `bnd.runbundles` exports the `-runbundles` files.
The exporter plugin with the specified name must be an installed exporter
plugin. The default is `bnd.executablejar`.

### bndrun

The bndrun to be exported. It can be anything that `Project.file(Object)`
can accept. This property must be set. The bndrun file must be a standalone bndrun
file since this is not a Workspace Build.

### destinationDir

The directory for the output. The default for destinationDir is
_${project.distsDir}_/executable if the exporter is `bnd.executablejar`,
_${project.distsDir}_/runbundles/_${bndrun.name - '.bndrun'}_ if
the exporter is `bnd.runbundles`, and _${project.distsDir}_/_${task.name}_
for all other exporters.

### bundles

The collection of files to use for locating bundles during the
bndrun export. The default is _${project.sourceSets.main.runtimeClasspath}_
plus _${project.configurations.archives.artifacts.files}_.

## Create a task of the `TestOSGi` type

You can also create a new task of the `TestOSGi` type. This task type
will execute tests in a standalone bndrun file. For example:

```groovy
import aQute.bnd.gradle.TestOSGi

task testOSGi(type: TestOSGi) {
  bndrun 'my.bndrun'
}
```
There are five properties which can be configured for a TestOSGi task:

### ignoreFailures

If `true` the task will not fail due to test case failures; instead an
error message will be logged. Otherwise, the task will fail. The
default is `false`.

### bndrun

The bndrun to be tested. It can be anything that `Project.file(Object)`
can accept. This property must be set. The bndrun file must be a standalone bndrun
file since this is not a Workspace Build.

### workingDir

The directory for the test execution. The default is _${temporaryDir}_.

### bundles

The collection of files to use for locating bundles during the
bndrun execution. The default is _${project.sourceSets.main.runtimeClasspath}_
plus _${project.configurations.archives.artifacts.files}_.

### tests

The list of fully qualified names of test classes to run. If not set, or empty,
Then all the test classes listed in the `Test-Classes` manifest header are
run. In Gradle 4.6 and later, the `--tests` command line option can be used
to set the fully qualified name of a test class to run. This can be repeated
multiple times to specify multiple test classes to run.

Use a colon (`:`) to specify a test method to run on the specified test class.

## Create a task of the `Index` type

You can also create a new task of the `Index` type. This task type
will generate an index for a set of bundles. For
example:

```groovy
import aQute.bnd.gradle.Index
 task index(type: Index) {
   destination = file('bundles')
   gzip = true
   bundles = fileTree(destination) {
    include '**/*.jar'
    exclude '**/*-latest.jar'
    exclude '**/*-sources.jar'
    exclude '**/*-javadoc.jar'
  }
}
```
There are five properties which can be configured for an Index task:

### gzip

If `true`, then a gzip'd copy of the index will be made with a `.gz` extension.
Otherwise, only the uncompressed index will be made. The default is
`false`.

### indexName

The name of the index file. The file is created in the destinationDir.
The default is`index.xml`.

### repositoryName

The name attribute in the generated index. The default is the name of the task.

### destinationDir

The destination directory for the index. This is used as the URI base of the
generated index. The default value is _${project.buildDir}_.

### bundles

This is the bundles to be indexed. It can be anything that `Project.files(Object...)`
can accept. This property must be set.

## Create a task of the `Bndrun` type

You can also create a new task of the `Bndrun` type. This task type
will execute a standalone bndrun file. For example:

```groovy
import aQute.bnd.gradle.Bndrun

task run(type: Bndrun) {
  bndrun 'my.bndrun'
}
```
There are four properties which can be configured for a Bndrun task:

### ignoreFailures

If `true` the task will not fail due to execution failures; instead an
error message will be logged. Otherwise, the task will fail. The
default is `false`.

### bndrun

The bndrun to be executed. It can be anything that `Project.file(Object)`
can accept. This property must be set. The bndrun file must be a standalone bndrun
file since this is not a Workspace Build.

### workingDir

The directory for the execution. The default is _${temporaryDir}_.

### bundles

The collection of files to use for locating bundles during the
bndrun execution. The default is _${project.sourceSets.main.runtimeClasspath}_
plus _${project.configurations.archives.artifacts.files}_.

---

## Using the latest development SNAPSHOT build of the Bnd Gradle Plugins

If you want to try the latest development SNAPSHOT build of the 
Bnd Gradle Plugins, you will need to adjust your `buildscript` classpath 
to refer to the snapshot repository and select the latest version
(`+`) of the plugin. For example, edit the `buildscript` script block (in
`settings.gradle` for Workspace builds), to configure the repository and
version of the plugin jar:

```groovy
buildscript {
  repositories {
    mavenCentral()
    maven {
      url 'https://oss.sonatype.org/content/repositories/snapshots'
    }
  }
  dependencies {
    classpath 'biz.aQute.bnd:biz.aQute.bnd.gradle:+'
  }
}
```

If you want to use the latest development SNAPSHOT version on a regular basis, you
will need to also need to add the following to the `buildscript` block to ensure
Gradle checks more frequently for updates:

```groovy
buildscript {
  ...
  /* Since the files in the repository change with each build, we need to recheck for changes */
  configurations.classpath {
    resolutionStrategy {
      cacheChangingModulesFor 30, 'minutes'
      cacheDynamicVersionsFor 30, 'minutes'
    }
  }
  dependencies {
    components {
      all { ComponentMetadataDetails details ->
        details.changing = true
      }
    }
  }
}
```

Remember, if you are using the Gradle daemon, you will need to stop it 
after making the change to use the development SNAPSHOT versions to ensure
Gradle stops using the prior version of the plugin.

---

## Using other JVM languages with the Bnd Gradle Plugins for Workspace builds

When you use the Gradle plugin for another JVM language like Groovy, Scala, or Kotlin
with your Bnd Workspace build, the Bnd Gradle Plugin will configure the language's
`srcDirs` for the `main` and `test` source sets to use the Bnd source and test source
folders.

---

For full details on what the Bnd Gradle Plugins do, check out the
[source code][10].

[1]: http://gradle.org/
[2]: https://github.com/bndtools/bnd/tree/master/biz.aQute.bnd.gradle
[3]: http://gradle.org/docs/current/userguide/java_plugin.html
[4]: https://bnd.bndtools.org/instructions/buildpath.html
[5]: https://bnd.bndtools.org/instructions/testpath.html
[6]: https://bnd.bndtools.org/instructions/releaserepo.html
[7]: https://bnd.bndtools.org/instructions/runbundles.html
[8]: ../biz.aQute.bndlib/src/aQute/bnd/build/Workspace.java
[9]: ../biz.aQute.bndlib/src/aQute/bnd/build/Project.java
[10]: src/aQute/bnd/gradle
[11]: https://github.com/bndtools/bndtools/blob/master/org.bndtools.headless.build.plugin.gradle/resources/templates/filter/root/gradle.properties
[12]: https://github.com/bndtools/bndtools/blob/master/org.bndtools.headless.build.plugin.gradle/resources/templates/unprocessed/root/settings.gradle
[13]: https://github.com/bndtools/bndtools/blob/master/org.bndtools.headless.build.plugin.gradle/resources/templates/unprocessed/root/build.gradle
[15]: src/aQute/bnd/gradle/BndPluginConvention.groovy
[16]: src/aQute/bnd/gradle/BndProperties.groovy
[18]: https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling.Jar.html
[19]: https://docs.gradle.org/current/dsl/org.gradle.api.reporting.ReportingExtension.html#org.gradle.api.reporting.ReportingExtension:baseDir
[20]: #gradle-plugins-for-workspace-builds
[21]: #gradle-plugin-for-non-workspace-builds

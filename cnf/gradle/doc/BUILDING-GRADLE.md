# <a name="Introduction"/>Introduction

This bnd workspace is setup to be built with [Gradle](http://www.gradle.org).

The build is setup in such a way that bnd projects are automatically
included in the build; no editing of Gradle build scripts is needed.

# <a name="InstallingGradle"/>Installing Gradle

## On The System

Obviously, Gradle must be installed on the system before the workspace can be
built with Gradle.

This description assumes a Linux machine. Details may vary on other OSes.

* Download Gradle from [http://www.gradle.org](http://www.gradle.org).

* Unpack the downloaded archive and put it in ```/usr/local/lib```
  as ```/usr/local/lib/gradle-1.11``` (assuming Gradle 1.11 was downloaded).

* Put the Gradle executable ```/usr/local/lib/gradle-1.11/bin/gradle``` on
  the search path by linking to it from ```/usr/local/bin```:

  ```
  rm -f /usr/local/bin/gradle
  ln -s /usr/local/lib/gradle-1.11/bin/gradle /usr/local/bin/
  ```

## <a name="InstallingGradleWorkspace"/>In The Workspace

Gradle can be installed in the workspace so that the workspace can be built on
systems that do not have Gradle installed (like build servers).

The procedure is:

* Open a shell and go into the workspace root directory.

* Assuming Gradle is properly installed on the system, run:

  ```
  gradle wrapper
  ```

* Commit the files that were created in the workspace to your version control
  system.


# <a name="GradleDaemon"/>Configuring The Gradle Daemon

Startup times of a Gradle build can be much improved by using the Gradle daemon.

The Gradle daemon works well when the Gradle build scripts are not changed,
which makes it well suited to regular (where the build scripts are not changed!)
development but **not** for build servers.

The daemon can be easily setup by adding the following line
to ```~/.gradle/gradle.properties```:

```
org.gradle.daemon=true
```


# <a name="Projects"/>Projects & Workspaces

## <a name="RootProject"/>Root Project

The Gradle root project is the directory that contains the ```settings.gradle```
file.

Gradle locates the root project by first looking for the ```settings.gradle```
file in the directory from which it was run, and - when not found - then by
searching up in the directory tree.

## <a name="SubProjects"/>Sub-Projects

The build will include all projects in the build that are:

* **bnd** projects: Directories directly below the root project with
                    a ```bnd.bnd``` file.

* **Gradle** projects: Directories directly below the root project with
                       a ```build.gradle``` file.

## <a name="GradleWorkspace"/>Gradle Workspace

The Gradle workspace is rooted in the root project and consists of all included
projects - **bnd** *and* **Gradle** projects.

## <a name="BndWorkspace"/>Bnd Workspace

The bnd workspace is rooted in the root project and contains a single
configuration project, and zero or more **bnd** projects.

For it to be a *useful* bnd workspace, it will have to contain at least one bnd
project.

## <a name="Cnf"/>Configuration Project

The configuration project is the directory that contains the ```build.bnd```
file, and is - by default - the ```cnf``` directory.

It contains:

* Placeholder source and classes directories (```src``` and ```bin```
  respectively).

* Bnd workspace configuration.

  * &nbsp;```ext/*.bnd```

    The ```ext``` directory contains bnd settings files that are loaded
    **before** the ```build.bnd``` file.

    The directory typically contains:

    * &nbsp;```junit.bnd```

      This file defines a bnd variable for the libraries that are needed on the
      classpath when running junit tests.

    * &nbsp;```pluginpaths.bnd```

      This file instructs bnd to load a number of plugin libraries when it
      runs. Typically it will instruct bnd to load repository plugins. However,
      custom plugins can also be loaded by bnd by adding them to
      the ```-pluginpath``` instruction in this file.

    * &nbsp;```repositories.bnd```

      This file configures the plugins that bnd loads. Typically it will
      configure the repository plugins that are loaded. However, if any built-in
      plugins or custom plugins are loaded then these also will have to be
      configured here. This file also defines which repository is the release
      repository.

  * &nbsp;```build.bnd```

    This file contains workspace-wide settings for bnd and will override
    settings that are defined in either of the ```ext/*.bnd``` files.

* Repositories.

  * &nbsp;```buildrepo```

    This repository contains libraries that are intended **only for build-time**
    usage. None are intended to be deployed as bundles into a running OSGi
    framework, and indeed they may cause unexpected errors if they are used
    in such a fashion.

  * &nbsp;```localrepo```

    This repository contains no libraries by default. It is intended for
    external libraries that are needed by one or more of the projects.

  * &nbsp;```releaserepo```

    This repository contains no libraries by default. Bundles end up in this
    repository when they are released.

* Cache.

  The ```cache``` directory contains libraries that are downloaded by the build.
  If the build is self-contained then this cache only contain libraries that are
  retrieved from the workspace itself (during the build).

* Build files.

  * <a name="BuildProperties"/>```build.gradle.properties```

    This file is used to bootstrap the build and defines the build dependencies:

    * All ```*.location``` and ```*.url``` settings are considered to be build
      dependencies.

    * A ```*.location``` setting has priority over the corresponding ```*.url```
      setting, which means that the ```example.url``` setting will be ignored
      if ```example.location``` is also specified.

    * An ```example.location``` setting will make the build script add the
      specified location (path) to the build dependencies.

    * An ```example.url``` setting will make the build script download
      the ```example``` build dependency from the specified URL into
      the ```cnf/cache/gradle``` directory and add it to the build dependencies.

      Using a ```*.url``` setting is not **not recommended** since the build
      will then no longer be self-contained (because it needs network access).

  * &nbsp;```gradle```

    This directory contains all build script files that are used by the build,
    and documentation pertaining to the build.

    * &nbsp;```template```

      This directory contains build script files that define the build. These
      are **not** meant to be changed.

    * &nbsp;```custom```

      This directory contains build script files that allow specification of
      overrides for various settings and and additions to the build. The build
      script files are effectively hooks into the build setup.

      These **are** meant to be changed (when the build customisations are
      needed).

    * &nbsp;```dependencies```

      This directory contains libraries that are used by the build.

    * &nbsp;```doc```

      This directory contains documentation pertaining to the build. The
      document you're now reading is located in this directory.

      <a name="svg"/>
      Also found in this directory is a diagram ([template.svg](template.svg))
      that provides an overview of the build setup, much like the Gradle User
      Guide shows for the Java Plugin.

      The diagram shows all tasks of the build and their dependencies:

      * The arrows depict **execution flow** (so the dependencies are in the
        reverse direction).

      * The **dotted** arrow depicts a convenience flow/dependency;
        running ```gradle jar``` will build all jars *and* bundles in all
        projects.

      * The **red** arrows depict flows from (dependencies on) dependent
        projects.

        For example:

        The ```compileJava``` task of a project is dependent on the ```bundle```
        task of another project if the latter project is on the build path of
        the former project.

      * The **blue** arrows depict flows/dependencies that are only present
        when the task from which the flows originate is present in the project.

      * The **green** arrows depict flows/dependencies that are disabled by
        default.

## <a name="BndProjectLayout"/>Bnd Project Layout

A bnd project has a well defined layout with two source sets and one output
directory:

* main sources: located in the ```src``` directory. Compiled sources will be
  placed in the ```bin``` directory.

* test sources: located in the ```test``` directory. Compiled sources will be
  placed in the ```bin_test``` directory.

* output directory ```generated```. Built OSGi bundle(s) will be placed here.

All bnd project layout directories can be customised by adjusting the following
settings in the project's ```bnd.bnd``` file:

* &nbsp;```src```: directory for main sources

* &nbsp;```bin```: directory for compiled main sources

* &nbsp;```testsrc```: directory for test sources

* &nbsp;```testbin```: directory for compiled test sources

* &nbsp;```target-dir```: directory for the built bundle(s)


# <a name="BuildFlow" />Build Flow

Understanding the build flow is important if extra tasks must be added to the
build, properties must be overridden, etc.

The build has the following flow:

* Gradle loads the ```settings.gradle``` file from the root project. This file
  instructs Gradle to include all **bnd** projects and all **Gradle** projects
  (see [Sub-Projects](#SubProjects) for an explanation).

* Gradle loads the ```build.gradle``` file from the root project. This file
  sets up the build:

  * Scan for the configuration project (see [Configuration Project](#Cnf)).

  * In order to be able to configure the build itself, the workspace build
    settings are loaded from
    the ```cnf/gradle/template/settings-workspace.gradle``` file, which
    then loads overrides from
    the ```cnf/gradle/custom/settings-workspace.gradle``` file.

  * Build logging is setup, as specified by the workspace build settings.

  * The build dependencies are setup by loading the (bootstrap) build
    properties from the ```cnf/build.gradle.properties``` file
    (see [the explanation of the build properties file](#BuildProperties).

  * The bnd workspace is initialised by loading the
    file ```cnf/gradle/template/bndWorkspace.gradle```.

  * The build template is applied by loading the
    file ```cnf/gradle/template/template.gradle```.

    The build template has 3 distinct sections which are applied in the order:

    * All projects
    * Sub-Projects
    * Root Project

    **All Projects**

    This section sets up the build (defaults) for all included projects by
    iterating over all included projects and performing the actions described
    below.

    * Load the build settings from
      the ```cnf/gradle/template/settings-allProjects.gradle``` file, which
      then loads overrides from
      the ```cnf/gradle/custom/settings-allProjects.gradle``` file.
      Finally, the project specific ```build-settings.gradle``` file is loaded
      if it's present.

      A project specific ```build-settings.gradle``` is placed in the
      root of an included project and allows overrides of the build
      settings on an project-by-project basis.

    * Findbugs (placeholder) tasks, index tasks and clean (placeholder) tasks
      are added to the project.

    * Build customisations are loaded from
      the ```cnf/gradle/custom/allProjects.gradle``` file.

    **Sub-Projects**

    This section sets up the build (defaults) for all included projects,
    (excluding the root project) by iterating over all included sub-projects.

    A distinction is made between **bnd** projects and **Gradle** projects.

    * Gradle projects

      * The default tasks are setup (specified by
        the ```gradleBuild_nonBndProjectsDefaultTasks``` property).

      * Build customisations are loaded from
        the ```cnf/gradle/custom/nonBndProjects.gradle``` file.

    * bnd projects

      * The default tasks are setup (specified by
        the ```gradleBuild_bndProjectsDefaultTasks``` property).

      * The bnd project build is setup by loading
        the ```cnf/gradle/template/bndProject.gradle``` file.

        * The bnd project is initialised (prepared) by bnd.

        * The bnd properties are loaded from bnd.

        * Properties (source and output directories, classpath
          directories, compiler options, etc) are setup for the project by
          using the bnd properties retrieved in the previous step.

        * The Java plugin is applied.

          Refer to the Gradle User Guide for more information on the Java
          plugin.

        * The project layout is applied by loading
          the ```cnf/gradle/template/bndLayout.gradle``` file, which
          sets up relevant Gradle project directories and Java source sets
          as dictated by the project's properties.

        * The Java compiler is configured by applying relevant properties
          to the source sets and their compiler options.

        * The ```bundle```, ```release```, ```releaseNeeded```, ```export```
          , ```bundleTest```, ```bndproperties``` and ```clean``` tasks are
          setup and their dependencies are configured in such a way that they
          hook into the tasks that are setup by the Java plugin
          (see [the tasks diagram](#svg)).

        * Build customisations are loaded from
          the ```cnf/gradle/custom/bndProjects.gradle``` file.

      * Build customisations are loaded from
        the ```cnf/gradle/custom/subProjects.gradle``` file.

    **Root Project**

    This section sets up the build (defaults) for the root project by loading
    the ```cnf/gradle/template/rootProject.gradle```, which performs the
    actions described below.

    * Load the build settings from
      the ```cnf/gradle/template/settings-rootProject.gradle``` file, which
      then loads overrides from
      the ```cnf/gradle/custom/settings-rootProject.gradle``` file.

    * The default tasks are setup (specified by
      the ```gradleBuild_rootProjectDefaultTasks``` property).

    * The wrapper and distclean tasks are setup.

    * Build customisations are loaded from
      the ```cnf/gradle/custom/rootProject.gradle``` file.

* For every included project with a ```build.gradle``` file Gradle loads that
  file.

* Gradle resolves the build setup.

* Gradle can now build the project by running the specified (or default) tasks.


# <a name="BuildTasks"/>Build Tasks

The discussion of the build tasks below is split per project type/category.

## Bnd Projects

### bundle

This task instructs bnd to construct an OSGi bundle.

This is comparable to the ```jar``` task that is defined by the Java plugin,
which instructs the Java compiler to construct a standard jar.

A bnd project effectively replaces the ```jar``` task with the ```bundle``` task
by disabling the ```jar``` task and setting up dependencies for the ```bundle```
task that are equivalent to those of the ```jar``` task.

The ```bnd.bnd``` file describes how the OSGi bundle must be constructed and is
therefore taken as input by bnd.

### bundleTest

This task instructs bnd to run bundle (integration) tests.

This is comparable to the ```test``` task that is defined by the Java plugin,
which instructs the Java runtime to run unit tests.

Refer to the bnd manual/website for more details on how to setup bundle tests.

This task is automatically disabled when no bundle tests have been defined.

### release

This task instructs bnd to copy the constructed OSGi bundle into the release
repository.

This task is automatically disabled when no release repository is defined.

### releaseNeeded

This task will invoke the ```release``` task on all projects on which the
project is dependent, after which the ```release``` task is invoked on the
project itself.

### export

This task will export all ```*.bndrun``` files in the project to executable
jars.

This task is automatically disabled when the project contains no ```*.bndrun```
files.

### bndproperties

This task - analogous to the Gradle ```properties``` task - displays the bnd
properties that are defined for the project.

These properties are defined in the ```bnd.bnd``` file in the root of the
project (and optionally other ```*.bnd``` files when using the ```-sub```
instruction for sub-bundles).

Properties that are defined in workspace-wide ```*.bnd``` files that are loaded
from the configuration project (```cnf```) are also displayed as they obviously
also apply to the project (unless overridden by the project, in which case the
overridden values are shown).

### clean

This task instructs bnd to clean up the project, which removes
the output directory and the directories that hold the class files.

This is in addition to the ```clean``` task that is defined by the Java plugin.


## All Projects

### index

This task can create one or more of the following:
* an uncompressed OBR index
* an uncompressed R5 index
* a compressed OBR index
* a compressed R5 index

These indexes are generated from/for one or more configured directories.

Which directories are indexed is controlled by
the ```gradleBuild_indexDirectories``` property. Its **syntax** is:

```
<root directory>;<name>;<name of fileTree property>, ...
```

* &nbsp;```root directory```: This is the root/base directory
  from where the relative URLs in the index file are calculated, and where
  the index file will be placed. Must be specified but doesn't need to exist.

* &nbsp;```name```: This is the name of the repository. Can be empty, in which
  case the name (*basename*) of the ```root directory``` is used.

* &nbsp;```name of fileTree property```: This is the name of a project property
  that must be an instance of a FileTree. This file tree determines which
  files will be indexed. If not specified then the all ```*.jar``` files
  below the ```root directory``` are indexed.

Multiple indexes can be generated by specifying (syntax as displayed in the box
above):

```
syntax,syntax,...
```

This task is automatically disabled when no index directories have been defined
or when no OBR indexes **and** no R5 indexes are configured to be created
(either uncompressed or compressed).

OBR index generation is controlled by the properties

* &nbsp;```gradleBuild_indexOBRUncompressed```: if set to ```true``` then an
  uncompressed OBR index is generated.

* &nbsp;```gradleBuild_indexOBRCompressed``` if set to ```true``` then a
  compressed OBR index is generated.

R5 index generation is controlled by the properties

* &nbsp;```gradleBuild_indexR5Uncompressed```: if set to ```true``` then an
  uncompressed R5 index is generated.

* &nbsp;```gradleBuild_indexR5Compressed``` if set to ```true``` then a
  compressed R5 index is generated.

### cleanNeeded

This task will invoke the ```clean``` task on all projects on which the
project is dependent, after which the ```clean``` task is invoked on the
project itself.

Note that invoking this task on the root project will invoke the task on all
projects.

### distClean

This task performs additional cleanup compared to the ```clean``` task, but is
empty by default.

For bnd projects and Java projects it removes:

* The class files output directory of all defined sourcesets.

* The resources output directory of all defined sourcesets.

For the root project it removes:

* The cache directory in the configuration project.

* The Gradle cache directory.

### distcleanNeeded

This task will invoke the ```distClean``` task on all projects on which the
project is dependent, after which the ```distClean``` task is invoked on the
project itself.

Note that invoking this task on the root project will invoke the task on all
projects.


## Java Projects

### Findbugs

The findbugs plugin is applied to all bnd projects and to all Java projects.
The plugin adds the tasks ```findbugsMain``` and ```findbugsTest```.

These two tasks are disabled by default since running findbugs is an expensive
operation and is not needed for most builds.

Note that the reports that are generated by the findbugs tasks will only have
line numbers when the tasks are run on a build that produces artefacts with
debug information.

#### findbugsMain

This task will run findbugs on the main source code.

#### findbugsTest

This task will run findbugs on the test source code.

#### findbugs

Specifying this task will **enable** the ```findbugsMain``` task.

Note: it is needed to specify a task that has a dependency on
the ```findbugsMain``` that to actually run the task. The tasks ```check```
and ```build``` are examples is such a task.

#### findbugstest

Specifying this task will **enable** the ```findbugsTest``` task.

Note: it is needed to specify a task that has a dependency on
the ```findbugsTest``` that to actually run the task. The tasks ```check```
and ```build``` are examples is such a task.

### javadoc

This task generates javadoc for the main source code.


## Root Project

### wrapper

This task downloads Gradle and installs it in the workspace,
see [Installing Gradle In The Workspace](#InstallingGradleWorkspace).


# Build Options

## Bnd Projects

* The ```bundle``` task can be disabled by:

  * Presence of the ```-nobundles``` instruction in the ```bnd.bnd``` file.

* The ```test``` task can be disabled by:

  * Presence of the ```-nojunit``` instruction in the ```bnd.bnd``` file.

  * Presence of the ```no.junit```  property in the ```bnd.bnd``` file.

* The ```bundleTest``` task can be disabled by:

  * Presence of the ```-nojunitosgi``` instruction in the ```bnd.bnd``` file.

## Findbugs

The findbugs task will - by default - generate HTML reports, but can be
instructed to generate XML reports by setting the ```CI``` Gradle system
property (```-PCI``` on the command line).

&nbsp;```CI``` = ```C```ontinous ```I```ntegration
                 (since XML reports are typically used on build servers)


# Customising The Build

## Gradle

The build be can easily customised by putting overrides and  additions in any of
the ```cnf/gradle/custom/*.gradle``` build script files,
see [Build Flow](#BuildFlow).

Also, any project can - on an individual basis - customise build settings or
specify additions by placing a ```build-settings.gradle``` file in its
root directory.

The ```build-settings.gradle``` file is meant for settings and their overrides,
the ```build.gradle``` file is meant for tasks.

An example of a ```build-settings.gradle``` file is shown below. This example
originates from the bnd project and shows how its ```dist``` project instructs
the build to index its ```bundles``` directory to generate indexes
named ```bnd```.

```
assert(project != rootProject)

/* Index task overrides */
ext.bnd_distIndexRoot                = "bundles"
ext.bnd_distIndexDirectories         = fileTree(bnd_distIndexRoot).include("**/*.jar").exclude("**/*-latest.jar")
ext.gradleBuild_indexDirectories     = "$bnd_distIndexRoot;bnd;bnd_distIndexDirectories"
ext.gradleBuild_indexOBRUncompressed = true
ext.gradleBuild_indexOBRCompressed   = true
ext.gradleBuild_indexR5Uncompressed  = true
ext.gradleBuild_indexR5Compressed    = true
```

## Bnd

The bnd default settings are shown in the ```cnf/build.bnd``` file.
Overrides to workspace-wide bnd settings can be placed in that same file.

If a setting must be overridden or defined for a specific project, then that
setting must be defined in the ```bnd.bnd``` file of that specific project.


# Adding Java Projects To The Build

The build automatically includes all bnd projects.

However, regular Java projects are not included automatically,
a ```build.gradle``` file in the root directory of the project is needed to make
that happen.

The ```build.gradle``` file shown below can be used as the basis. This will
setup the Java project with the default bnd layout and add tasks that are
relevant to a Java project (```javadoc```, findbugs tasks and ```distclean```).

```
/*
 * A java project with bnd layout
 */

assert(project != rootProject)

apply plugin: "java"

assert(rootProject.hasProperty("bnd_cnf"))

/* Setup the bnd project layout */
apply from: rootProject.file("$rootProject.bnd_cnf/gradle/template/bndLayout.gradle")

/* Add tasks that are relevant to Java projects */
apply from: rootProject.file("$rootProject.bnd_cnf/gradle/template/javaProject.gradle")
```

As an example, if the project should use the Maven layout, then add
a ```build-settings.gradle``` file in the root directory of the project as
shown below.

```
ext.bnd_srcDir        = "src/main/java"
ext.bnd_srcBinDir     = "target/classes"
ext.bnd_testSrcDir    = "src/main/test"
ext.bnd_testSrcBinDir = "target/test-classes"
ext.bnd_targetDir     = "target"
```


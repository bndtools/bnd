---
order: 150
title: Build
layout: default
---

This chapter lays out the rules of the file system for bnd projects. It discusses the workspace layout and the projects layout as well as the properties.

## Workspace
A Workspace is a single directory with all its sub-directories and their files, similar to a git workspace. The core idea of the workspace is that it is easy to move around, that is, it allows the use of relative file names. It also prevents a lot of potential problems that occur when you allow projects to be anywhere on the file system. KISS!

Workspaces should be named according to the bundle symbolic names of its projects. Using such a naming strategy will simplify finding the correct namespace given a bundle symbolic name. 

A bndlib workspace is a _valid_ workspace when it contains a `cnf` file. If this is a text file, its content is read and interpreted as a path to the `cnf` directory (which can again be a path to a cnf directory, ad infinitum). The retrieved path is retrieved and trimmed after which it is resolved relative to its parent directory.   

However, the advised model is to use a directory with a `cnf/build.bnd` file. The purpose of the `cnf` directory is to provide a place for shared information. Though this includes bndlib setup information, it also can be used to define for example shared licensing, copyright, and vendor headers for your organization.

The `cnf` directory can have an `ext` directory, this directory contains any extensions to bnd.

To cache some intermediate files, bndlib will create a `cnf/cache/` directory, this file should not be under source control. E.g. in Git it should be defined in the `.gitignore` file. 

The root of the workspace is generally used to hold other files. For example the `.git` directory for Git, or gradle and ant files for continuous integration. However, designers that add functionality to the workspace should strive to minimize the clutter in the root. For example, the bnd gradle support adds a few files to the root but these link to a `cnf/gradle` directory for their actual content.

Other directories in the workspace represent _projects_. The name of the project is the bundle symbolic name of the bundle that it produces (or the prefix of the bundle symbolic name when it produces multiple bundles).

Overall, this gives us the following layout for a workspace:

	com.acme.prime/                     workspace
	  cnf/                              configuration/setup
	    ext/                            extensions
	      maven.bnd                     maven setup extension
	    build.bnd                       organization setup
	    plugins/                        directory for plugins
	    cache/                          bnd cache directory, should not be saved in an scm
	  com.acme.prime.speaker/           project directory	  

### Workspace Properties

Properties are used for headers, macros, and instructions in bndlib, they are quite fundamental. To simplify maintenance, bndlib provides an elaborate mechanism to provide these properties from different places and _inherit_ them. The workspace resides at the top of this inheritance chain (ok, after the built-in defaults).

When a workspace is created, it will first read in the properties in the `.bnd` files in the `cnf/ext` directory. These are called the _extension files_ since they in general setup plugins and other extensions. The order in which they are read is the lexical sorting order of their file names.

### Extension Files

Extension files allow you to separate configuration concerns. Its primary purpose is to allow third party extensions. These extensions can then put their properties in one place. The contents of these files should therefore not be touched so that a new version can override them. Each extension file is read as a bnd file, this means that full power of bndlib is available. The bnd command line tool has facilities to add and remove files from this directory.

For example, the Maven plugin that is built-in to bndlib has an extension file called maven.bnd which looks as follows:

	#
	# Plugin maven setup
	#
	-plugin.maven = aQute.bnd.plugin.maven.MavenPlugin
	
	
	#
	# Change disk layout to fit maven
	#
	
	-outputmask = ${@bsn}-${versionmask;===S;${@version}}.jar
	src=src/main/java
	bin=target/classes
	testsrc=src/test/java
	testbin=target/test-classes
	target-dir=target

We will not explain this plugin here (you can find it in the plugin sections), it only illustrates here how it is possible to setup the environment for a specific optional functionality like a plugin.

If you create local extension files then you should use a prefix to identify this is your file, like:

	com.acme-local.bnd

It is possible to use file links to maintain these files in one place when you have many workspaces.

### Local customizations

After reading the extension files, bndlib reads the `cnf/build.bnd` file, this file is supposed to hold the organization specific properties. Out of the box, this file comes empty, ready to be filled by you.

### Workspace Plugins

A _plugin_ is a piece of code that runs inside bnd. The workspace provides a number of standard built-in plugins like an Executor, a Randum number generator, itself, etc. Additional plugins can be added with the `-plugin.*` instructions.  

## Project Properties
There are a number of built in properties that are set by bnd:

|-----------------+-----------------------------------------------------------------|
| Property name   | Description                                                     | 
|-----------------|:----------------------------------------------------------------|
|`project`		  | Name of the project. This is the name of the bnd file without   |
|                 | the .bnd extension. If this name is bnd.bnd, then the directory |
|                 | name is used.                                                   |
|-----------------+-----------------------------------------------------------------|
|`project.file`   | Absolute path of the main bnd file.                             |
|-----------------+-----------------------------------------------------------------|
|`project.name`   | Just the name part of the file path                             |
|-----------------+-----------------------------------------------------------------|
|`project.dir`    | The absolute path of the directory in which the bnd file resides. |
|-----------------+-----------------------------------------------------------------|

## Run instructions
Run instructions are used to start OSGi tests and OSGi runs.

|`-runbundles`       |`LIST SPEC`    |Additional bundles that will be installed and started when the framework is launched. This property is normally part of the project's `bnd.bnd` file.|
|`-runvm`            |`PROPERTIES`   |Properties given to the VM before launching. This property is normally set in the `cnf/build.bnd` file and only in rare cases overridden in the `bnd.bnd` file.|
|`-runproperties`    |`PROPERTIES`   |Properties given to the framework when launching. Usually project specific.|
|`-runsystempackages`|`PACKAGES`     |A declaration like Import-Package that specifies additional system packages to import from the class path. Usually given in the `cnf/build.bnd` file.|
|`-runpath`          |`LIST SPEC`    |A path description of artifacts that must be on the classpath of the to be launched framework.  Usually given in the `cnf/build.bnd` file. This path should contain the framework. Any packages that a bundle on the `-runpath` should specify should be listed in the `export` attribute.|
|`-runtrace`         |`true|false`   |Trace the startup of the framework to the console. Usually used during testing and development so project specific.|
|`-runframework`     |`none|services`|`NONE` indicates that a mini built in framework should be used. `SERVICES` indicates that the `META-INF/services` model must be followed for the `org.osgi.framework.launch.FrameworkFactory` class. Project specific.|
|`-testpath`         |`LIST SPEC`    |A path used to specify the test plugin.|

## Launching
Launching is needed when the project's `run` action or `test` action is executed. The project creates a Project Launcher. A Project Launcher must launch a new VM and set up this VM correctly. The VM is launched with the following information:

* `java` - The command to launch a new VM is by default `java`. However, this can be overridden by setting a property called `java`.
* `classpath` - The classpath set for the VM is derived from the `-runpath` property. Notice that this is supposed to contain the JAR with the framework. The `-runpath` requires bundle symbolic names for the JAR and an optional version range. bnd will use the latest version found in the repository. Any packages that should be exported by the system bundle should have an `export` attribute containing the exported packages, like `junit.osgi;version=3.8;export="junit.framework;version=3.8,junit.misc;version=3.8"`.
* VM options - These options can be set in the `-runvm` property. They are usually in the form of `-Dxya=15` or `-X:agent=bla`. Options should be separated by commas.
* `main` - The class implementing the main type is defined by the launcher plugin.

An example of a launcher set is:

	-runvm:   -Xmn100M, -Xms500M, -Xmx500M
	-runpath: \
		org.apache.felix.framework; version=3.0, \
		junit.osgi;export="junit.framework;version=3.8"
	-runtrace: true
	-runproperties: launch=42, trace.xyz=true
	-runbundles: org.apache.felix.configadmin,\
		org.apache.felix.log,\
		org.apache.felix.scr,\
		org.apache.felix.http.jetty,\
		org.apache.felix.metatype,\
		org.apache.felix.webconsole

Debugging the launcher is greatly simplified with the `-runtrace` property set to `true`. This provides a lot of feedback what the launcher is doing.

### Access to arguments
When the launcher is ready it will register itself as a service with the following properties:

|`launcher.arguments`|The command line arguments|
|`launcher.ready`|Indicating the launcher is read|

### Access to main thread
In certain cases it is necessary to grab the main thread after launching. The default launcher will launch all the bundles and then wait for any of those bundles to register a `Runnable` service with a service property `main.thread=true`. If such  service is registered, the launcher will call the run method and exit when this method returns.

### Timeout
The launcher will timeout after an hour. There is currently no way to override this timeout.

### Mini Framework
The bnd launcher contains a mini framework that implements the bare bones of an OSGi framework. The purpose of this mini framework is to allow tests and runs that want to launch their own framework. A launch that wants to take advantage of this can launch with the following property:

	-runframework: none

### Ant
In Ant, the following task provides the run facility.

	<target name="run" depends="compile">
		<bnd command="run" exceptions="true" basedir="${project}" />
	</target>

These targets provide commands for `ant run`.

## Testing
Testing is in principle the same as launching, it actually uses the launcher. Testing commences with the `test` action in the project. This creates a Project Tester. bnd carries a default Project Tester but this can be overridden.

The basic model of the default Project Tester plugin is to look for bundles that have a `Test-Cases` manifest header after launching. The value of this header is a comma separated list of JUnit test case class names. For example:

	Test-Cases: test.LaunchTest, test.OtherTest

Maintaining this list can be cumbersome and for that reason the `${classes}` macro can be used to calculate its contents:

	Test-Cases: ${classes;extending;junit.framework.TestCase;concrete}

See [classes macro](../macros/classes.html) for more information.

### Ant

	<target name="test" depends="compile">
		<bnd command="test" exceptions="true" basedir="${project}" />
	</target>

## Overriding the plugins
Both the Project Launcher and Project Tester are plugins. Defaults are provided by bnd itself (bnd carries a mini cache repo that is expanded in the `cnf` directory), it is possible to add new launchers and testers as needed.

Launcher or tester plugins are found on the `-runpath` or the `-testpath` properties, respectively. To detect a plugin, bnd will look for an appropriate manifest header. The header value is a class name. Bnd will then instantiate the class and use it as a launcher/tester. The classes must extend an abstract base class. Each plugin has access to the `Project` object, containing all the details of the project.

|Plugin             |Manifest header|Base Class     |Where searched|
|------------------------------------------------------------------|
|Project Launcher   |Launcher-Plugin|ProjectLauncher|-runpath|
|Project Tester     |Tester-Plugin  |ProjectLauncher|-testpath|

The plugin gets complete control and can implement many different strategies.

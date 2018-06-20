---
title: Guided Tour
layout: default
---

The purpose of this com.acme.prime is to show build administrators how to setup _workspaces_ and what features bndlib provides to automate common tasks. This section is not intended to be used by people wanting to learn OSGi, please use a bndtools tutorial for this. 

This section will go through the process of creating a workspace and a few projects while explaining what functions are useful in each phase. It will remain at a rather high level to keep the text flowing, details can be found in the different reference sections.

Since this section provides a tool independent view of bndlib, we use the [bnd command line application](120-install.html) to demonstrate the features. Though this is an excellent way to show the low level functionality (the porcelain in git terms), it is not the normal way bndlib is used. In general, a build tool like gradle, ant, make, or maven drives this process from the command line an IDE like bndtools handles the user interaction. So please, please, do not take this com.acme.prime as a guide how to create a build. However handy bnd is, it falls far short of a real build tool like for example gradle.

## Workspace

A workspace is a directory with the following mandatory files/directories:

	./
		cnf/
			build.bnd
			ext/

That's all! So let's create one:

	$ bnd add workspace com.acme.prime
	$ cd com.acme.prime
	$ ls
	cnf

The `cnf` directory is the 'magic' directory that makes a directory a workspace, just like the `.git` directory does for git. 

In the `cnf` directory you find the following files:

	$ ls cnf
	bnd.bnd         build.bnd       ext

The `build.bnd` is part of the magic to make something a workspace, it contains *your* workspace properties. The `ext` directory contains more properties. The `bnd.bnd` is the last piece of magic, it makes bndlib recognize the `cnf` directory as a project.

### Naming

Why such a long name for the workspace? Wouldn't just `tour` suffice? Well, glad you asked. If you work with bnd(tools) for some time you will find that you will get many different workspaces since a workspace is another level of _modularity_. You can see a workspace as a _cohesive_ set of bundles; just like any module it can import and export. Just like any other module, it imports and exports the thing it encapsulates. For example, a bundle imports and exports packages. In case of the workspace this is the _bundle_. A workspace imports and exports bundles through repositories. 

A good module has cohesion; this means that its constituents have a rather strong relation. Since they also tend to come from the same organization they will have very similar bundle symbolic names. Since some of these bundles will escape in the wild it is always beneficial if you can quickly find the source of that bundle. Therefore naming the workspace with the shared prefix of the bundle symbolic names of its constituents is highly recommended.

That said, bndlib does not enforce this guideline in any way, unlike project names. You can name your workspace `foo` if you want to.

### Properties 

A workspace so created is quite empty. However, if we look in the `cnf` directory then we can see the `build.bnd` file. This file  is 100% for you ... Any property, instruction, or header specified in here is available in anything you build in this workspace; all other bnd files will inherit everything from this properties file. In this file should add for example the headers Bundle-Vendor and Bundle-Copyright. However, using the macro language we can also add custom variables and macros that are useful across the workspace.

## Plugins

An important aspect of the workspace is that it hosts _plugins_. A plugin is an extension to bndlib that gets loaded when the workspace is opened. Plugins provide a lot of different functions in bndlib. You can see the currently loaded plugins with bnd:

	$ bnd plugins
	000 Workspace [com.acme.prime]
	001 java.util.concurrent.ThreadPoolExecutor@a4102b8[Running, ...]
	002 java.util.Random@11dc3715
	003 Maven [m2=...]
	004 Settings[/Users/aqute/.bnd/settings.json]
	005 bnd-cache
	006 ResourceRepositoryImpl [... ]
	007 aQute.bnd.osgi.Processor

The plugins you see are the built-in plugins of bnd itself, they always are available. However, the purpose of plugins is to extend the base functionality. As almost everything, the set of external plugins is managed through an instruction, which is a property in the Workspace.

Before bndlib reads the `build.bnd` file to read the workspace properties, it first reads all the files with a `.bnd` extension in the `cnf/ext` folder. The purpose of this folder is to manage setups for plugins. We can add additional plugins by their name. You can see a list of built-in plugins with the add plugin command:

	$ bnd add plugin
	Type                           Description
	ant                            aQute.bnd.plugin.ant.AntPlugin
	blueprint                      aQute.lib.spring.SpringXMLType
	eclipse                        aQute.bnd.plugin.eclipse.EclipsePlugin
	filerepo                       aQute.lib.deployer.FileRepo
	git                            aQute.bnd.plugin.git.GitPlugin
	gradle                         aQute.bnd.plugin.gradle.GradlePlugin
	...

An interesting plugin is the Eclipse plugin that will prepare any projects for Eclipse. We could also add the git plugin that will make sure the proper .gitignore files are in place.

	$ bnd add plugin eclipse
	$ bnd add plugin git
	$ bnd plugins
	...
	007 aQute.bnd.osgi.Processor
	008 EclipsePlugin
	009 GitPlugin
	
Since you likely need to maintain this build it is good to know how this is stored. If you look in the `cnf/ext` directory you should see an `eclipse.bnd` and a `git.bnd` file:


	$ ls cnf/ext
	eclipse.bnd			git.bnd

The `eclipse.bnd` file contains the following:

	$ more cnf/ext/eclipse.bnd
	#
	# Plugin eclipse setup
	#
	-plugin.eclipse = aQute.bnd.plugin.eclipse.EclipsePlugin

So how does this work? When the workspace is opened bndlib will first read all the bnd files in the `cnf/ext` directory in alphabetical order. After that, it will read the `build.bnd` file. The idea of the `cnf/ext` files is that they should not be touched by you, the `build.bnd` file is, however, all yours. You can override any previous setting in the `build.bnd` file since it is read last. 

As you can see, the `eclipse.bnd` file defines the property `-plugin.eclipse`. In most cases that a value should be settable in different places, bndlib uses _merged properties_. When bndlib loads the plugins, it actually gets the property `-plugin`, merged with any other property that has a key that starts with `-plugin.` (ordered alphabetically). This allows you to add to a merged property anywhere in the many places in bndlib where you can set properties.  

So now we can try to build the workspace:

	$ bnd build
	No Projects

Which makes some sense ...

## Project

An empty workspace is not so useful, let's add a project. 

	$ bnd add project com.acme.prime.hello
	$ ls -a com.acme.prime.hello/
	.               .gitignore      bin_test        src
	..              .project        bnd.bnd         test
	.classpath      bin             generated

This classic layout defines separate source folders for the main code and the test code. The `generated` directory is a temporary directory, it contains the artifacts produced by this build. 

## Setup




## Changing the Layout
This is a classical Eclipse layout, with a separate `src` and `test` folder. However, this is not baked into bndlib, it is possible to, for example, use the maven layout with the `src/main/java`, `src/test/java`, and `target` directories. We can try this out with the maven plugin.

	$ bnd add plugin maven
	$ more cnf/ext/maven.bnd
	-plugin.maven = aQute.bnd.plugin.maven.MavenPlugin

	-outputmask = ${@bsn}-${versionmask;===S;${@version}}.jar
	
	src=src/main/java
	bin=target/classes
	testsrc=src/test/java
	testbin=target/test-classes
	target-dir=target

The maven plugin adds a number of properties that are recognized by bndlib and used appropriately. 


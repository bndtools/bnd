---
layout: default
title:  eclipse pde [options] <[repo-dir]> <[...]> 
summary: Import PDE projects into a bnd workspace 
bnd: 4
---

_THIS IS WORK IN PROGRESS_

## Description

{{page.summary}}

The control file (-i) has the following properties: 

	pde.selection = \
	    com.example.extension.diagnostic, \
	    com.example.security.foo.*; \
	    com.example.osgi.*; \
	        -workingset=sample, \
	    com.example.bar; \
		-buildpath="org.apache.commons.lang,com.google.gson;version=3", \

The structure is a normal OSGi Parameters, where they keys of clauses can be connected. Keys can be globbed. This will match the directory of a directory that is a PDE project as signalled by a build.properties file. Notice that you can specify multiple directory globs for one clause. 

The attributes/directives of a clause provide information that is added to the bnd file before analysis.

Although this format is quite powerful it is very error prone. Check your commas and semi-colons, especially when multiple entries on the -buildpath or -testpath are specified.

### Working

The pde command will process the PDE repository directories and scan for PDE projects. A PDE project is a directory that has a `build.properties` file. It will then match the directory name against one of the keys in the `pde.selection` property of the control file. If this matches, it will convert the project.

It will first parse the manifest to establish the bundle symbolic name. It will then create a bnd project in the provided workspace with the symbolic name. It will use the workspace settings for the workspace as defined by the following properties:

	src:                    source directory, e.g. src/main/java
	src.resources:          resources directory, e.g. src/main/resources
	bin:                    output directory main, e.g. target/classes
	testsrc:                test source (singular!), e.g. src/test/java
	testsrc.resources:      test resources, e.g. src/test/resources
	testbin:                test outout, e.g. target/test-classes
	target-dir:             general write directory, e.g. target

It will use the information in the build.properties file to copy these different aspects of a PDE project to the place in a bnd project. If the control file specifies attributes for the given project, then they are added to the bnd.bnd file. The bnd file will be fully setup for Eclipse with a proper .classpath and .project files. Any natures and build command other than bnd, e.g. Groovy, are copied.

After all selected PDE projects are converted, bnd will analyze the newly created projects. It will scan the source code and detect most dependencies. It will then try to find an exporter in either another bnd project in the same workspace or through one of the workspace's repositories. This is done seperately for both the main aspect of the project (-buildpath) as well as the test aspect (-testpath). 

Although it is a very decent start, this analysis is not perfect

* The workspace/repository can have multiple resources with different version. In that case the latest version is selected. (Otherwise no version is specified which defaults to the lowest available version for the build and highest for the test.)
* Dependencies not imported in the Java file are not detected. This happens when you get an object of a non imported type but directly call a method on it.

## Synopsis

## Options

	-w, --workspace <dir>           - The bnd workspace directory
	-c, --clean                     - Clean the project directory before converting
	-s, --set                       - Set Eclipse workingset
	-i, --instructions <file.bnd>*  - Control file what to convert and augment
	-r, --recurse                   - Show references to other classes/packages (>)

## Example

	bnd eclipse pde 
		-c 
		-w com.example.bnd.workspace 
		-s example.workingset 
		-i control.bnd  
		-r git_repo

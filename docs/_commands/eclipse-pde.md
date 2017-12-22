---
layout: default
title:  bnd eclipse pde [options] <<repo-dir>> <[...]>
summary: Import PDE projects into a bnd workspace 
bnd: 4
---

OPTIONS

    -w, --workspace <dir>           - The bnd workspace directory
    -c, --clean                     - Clean the project directory before converting
    -s, --set                       - Set Eclipse workingset
    -i, --instructions <file.bnd>*  - Control file what to convert and augment
    -r, --recurse                   - Show references to other classes/packages (>)

The control file (-i) has the following properties: 

	pde.selection = \
	    com.example.extension.diagnostic, \
	    com.example.security.foo.*; \
	    com.example.osgi.*; \
	        -workingset=sample, \
	    com.example.bar; \
		-buildpath=org.apache.commons.lang, \

The structure is a normal OSGi Parameters, where they keys of clauses can be connected. Keys can be globbed. This will match the directory of a directory that is a PDE project as signalled by a build.properties file. Notice that you can specify multiple directory globs for one clause.

The attributes/directives of a clause provide information that is added to the bnd file before analysis.

## Working

The pde command will process the PDE repository directories and scan for PDE projects. A PDE project is a directory that has a `build.properties` file. It will then match the directory name against one of the keys in the `pde.selection` property of the control file. If this matches, it will convert the project.

It will first parse the manifest to establish the bundle symbolic name. It will then create a bnd project in the provided workspace with the symbolic name. It will use the workspace settings for the workspace as defined by the following properties:

	src:                    source directory, e.g. src/main/java
	src.resources:          resources directory, e.g. src/main/resources
	bin:                    output directory main, e.g. target/classes
	testsrc:                test source (singular!), e.g. src/test/java
	testsrc.resources:      test resources, e.g. src/test/resources
	testbin:                test outout, e.g. target/test-classes
	target-dir:             general write directory, e.g. target

It will use the information in the build.properties file to copy these different aspects of a PDE project to the place in a bnd project. If the control file specifies attributes for the given project, then they are added to the bnd.bnd file.

After all selected PDE projects are converted, 

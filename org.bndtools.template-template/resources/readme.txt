Bndtools Template README
========================

Welcome to your new Bndtools project template! Here are some notes on how to
make your template do what you want.

bnd.bnd
-------

The bnd.bnd file declares a Provide-Capability in the 'org.bndtools.template'
namespace.  This enables Bndtools to find your template bundle in the
repository. The attributes on this capability such as name, category, ranking
etc should be obvious. The 'dir' attribute defines a folder within your bundle
containing the template file (the default is 'template').

Template Files
--------------

Within your template directory, create a folder structure that matches what you
want the template to generate. For example to generate the following layout:

	bnd.bnd
	src/Main.java

Then just create the same layout inside your template directory:

	bnd.bnd
	src/Main.java

If a filename ends with the suffix '.st' then it is processed through
StringTemplate before being written out. The .st suffix will be dropped from
the output file name.  For example the following will generate the same layout
as above, but with template processing of the bnd.bnd file:

	bnd.bnd.st
	src/Main.java

The default delimiter character used with StringTemplate is '$', so you can put
template expressions in the file as follows:

	Bundle-SybolicName: $projectName$

See below for a list of the parameter names passed into the template by Bndtools.

The output file names can also be processed by template. For example:

	$srcDir$/$basePackageDir$/Main.java.st
	bnd.bnd.st

Parameter Names
---------------

Project templates can use any of the following parameter names:

	projectName -- name of the project
	version -- version of the project
	srcDir -- Java source directory
	basePackageDir -- base package name (projectName, with dots replaced by slashes)
	binDir -- output classes directory
	testSrcDir -- Java source directory for tests
	testBinDir -- output classes directory for tests
	targetDir -- bnd's target directory
	 
Customizing the Template Processor
----------------------------------

The processor can be customized by including a special file named
'_template.properties' in the template directory. For example this can be used
to use a different pair of delimiter characters:

	leftDelim: <
	rightDelim: >

These alternative delimiters will be used for filenames and templates.

---
order: 400
title: From the command line
layout: default
---


MAIN OPTIONS

   [ -o, --output <string> ]  - Specify the output file path. The default is
                                output.jar in the current directory


MAIN OPTIONS

   [ -f, --full ]             - Do full
   [ -p, --project <string> ] - Identify another project
   [ -t, --test ]             - Build for test

Available sub-commands: 

	  action                      - Execute an action on a repo, or if no name is
	                                give, list the actions 
	  baseline                    - Compare a newer bundle to a baselined bundle and
	                                provide versioning advice 
	  bnd                         - The swiss army tool for OSGi 
	  bootstrap                   -  
	  bsn2url                     -  
	  build                       - Build a project. This will create the jars
	                                defined in the bnd.bnd and sub-builders. 
	  buildx                      - Build project, is deprecated but here for
	                                backward compatibility 
	  bump                        - Bumps the version of a project 
	  changes                     -  
	  clean                       - Clean a project 
	  convert                     - Converter to different formats 
	  create                      - Create jar, used to support backward compatible
	                                java jar commands 
	  debug                       - Show a lot of info about the project you're in 
	  defaults                    -  
	  deliverables                - Show all deliverables from this workspace. with
	                                their current version and path. 
	  diff                        - Diff jars 
	  digest                      - Digests a number of files 
	  do                          - Execute a file based on its extension. Supported
	                                extensions are: bnd (build), bndrun (run), and
	                                jar (print) 
	  eclipse                     - Show info about the current directory's eclipse
	                                project 
	  ees                         -  
	  enroute                     - OSGi enRoute commands to maintain bnd workspaces
	                                (create workspace, add project, etc) 
	  extract                     - Extract files from a JAR file, equivalent jar
	                                command x[vf] (syntax supported) 
	  find                        -  
	  generate                    - Generate autocompletion file for bash 
	  grep                        - Grep the manifest of bundles/jar files. 
	  identity                    -  
	  info                        - Show key project variables 
	  junit                       - Test a project with plain JUnit 
	  macro                       - Show macro value 
	  maven                       - Maven bundle command 
	  package                     - Package a bnd or bndrun file into a single jar
	                                that executes with java -jar <>.jar 
	  plugins                     -  
	  print                       - Printout the JAR 
	  project                     - Execute a Project action, or if no parms given,
	                                show information about the project 
	  release                     - Release this project 
	  repo                        - Manage the repositories 
	  run                         - Run a project in the OSGi launcher 
	  runtests                    - Run OSGi tests and create report 
	  schema                      - Highly specialized function to create an
	                                overview of package deltas in ees 
	  select                      - Helps finding information in a set of JARs by
	                                filtering on manifest data and printing out
	                                selected information. 
	  settings                    - Set bnd/jpm global variables 
	  source                      - Merge a binary jar with its sources. It is
	                                possible to specify source path 
	  sync                        -  
	  syntax                      - Access the internal bnd database of keywords and
	                                options 
	  test                        - Test a project according to an OSGi test 
	  type                        - List files int a JAR file, equivalent jar
	                                command t[vf] (syntax supported) 
	  verify                      - Verify jars 
	  version                     - Show version information about bnd 
	  view                        - View a resource from a JAR file. 
	  wrap                        - Wrap a jar 
	  xref                        - Show a cross references for all classes in a set
	                                of jars. 




The command line tool can be invoked in several different ways:

* bnd ''general-options'' ''cmd'' ''cmd-options''
* bnd ''general-options'' ''<file>.jar''
* bnd ''general-options'' ''<file>.bnd''

In this text `bnd` is used as if it is a command line program. This should be set up as: 

  java -jar <path to bnd>.jar ...

Work is in progress to simplify this.

### General Options

||
||!General Option ||!Description ||
||-failok ||Same as the property -failok. The current run will create a JAR file even if there were errors. ||
||-exceptions ||Will print the exception when the software has ran into a bad exception and bails out. Normally only a message is printed. For debugging or diagnostic reasons, the exception stack trace can be very helpful. ||

### baseline ( --all | --diff | --fixup STRING | --packages STRING | --quiet | --verbose ) * <newer jar> <older jar>

Perform a baseline operation between the supplied jars. The set of differences will be listed per package. The following arguments may be supplied.

* --all - Show all, also unchanged
* --diff - Show any differences
* --fixup - Output file with fixup info
* --packages - Packages to baseline (comma delimited)
* --quiet - Be quiet, only report errors
* --verbose - On changed, list API changes

 bnd baseline --diff newer.jar older.jar

### print ( -verify | -manifest | -list | -all ) * <file>.jar +

The print function will take a list of JAR file and print one or more aspect of the JAR files. The following aspects can be added.

* -verify - Verify the JAR for consistency with the specification. The print will exit with an error if the verify fails
* -manifest - Show the manifest
* -list - List the entries in the JAR file
* -all - Do all (this is the default)

 bnd print -verify *.jar

### buildx ( -classpath LIST | -eclipse <file> | -noeclipse | -output <file> ) * <file>.bnd +

The build function will assemble a bundle from the bnd specification. The default name of the output bundle is the name of the bnd file with a .jar extension.

* -classpath - A list of JAR files and/or directories that should be placed on the class path before the calculation starts.
* -eclipse - Parse the file as an Eclipse .classpath file, use the information to create an Eclipse's project class path. If this option is used, the default .classpath file is not read.
* -noeclipse - Do not parse the .classpath file of an Eclipse project.
* -output - Override the default output name of the bundle or the directory. If the output is a directory, the name will be derived from the bnd file name.

 bnd buildx -classpath bin -noeclipse -output test.jar xyz.bnd


[[#wrap]]
### wrap ( -classpath (<file>(','<file>)*)-output <file|dir> | -properties <file> ) * \\
  -ignoremanifest? <file>.jar *

The wrap command takes an existing JAR file and guesses the manifest headers that will make this JAR useful for an OSGi Service Platform. If the output file is not overridden, the name of the input file is used with a .bar extension. The default bnd file for the header calculation is:

 Export-Package: * 
 Import-Package: <packages inside the target jar>

If the target bundle has a manifest, the headers are merged with the properties.

The defaults can be overridden with a specific properties file.

* -output - Set the output file or directory
* -classpath - Sets the classpath as a comma separated list
* -properties - Use a special property file for the manifest calculation.
* -ignoremanifest - Do not include the manifest headers from the target bundle

 bnd wrap -classpath osgi.jar *.jar

### eclipse

List the Eclipse information in the current directory.

 bnd eclipse
[[#eclipse]]
## Eclipse Plugin
The bnd.jar file is a complete plugin. To install this plugin, place it in the eclipse/plugin directory (or extension directory) of your Eclipse installation and restart (!). The plugin will provides a 'Make Bundle' context menu when you select a file that ends with .bnd. Two menus are shown when you select a JAR file. You can 'Wrap JAR', turning it into a bundle with all imports and exports set (the extension will be .bar), or you can use 'Verify Bundle', and verify the bundle for compliance to the spec. Any errors or warnings are listed in a dialog box.
 
Additionally, the plugin registers an editor for JAR files. The editor shows the full output of the print command.

---
layout: default
title: exportreport
summary: |
   Generate and export reports of a workspace, a project or of a jar.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: #
	   exportreport  <[list | export | jarexport | readme | jarreadme]> ...

## Available sub-commands #
-  `export` - Export the user defined reports. 
-  `jarexport` - Export a custom report of a Jar. 
-  `jarreadme` - Export a readme file of a Jar (template can be parametrized with system properties starting with 'bnd.reporter.*'). 
-  `list` - List the user defined reports. 
-  `readme` - Export a set of readme files (template can be parametrized with system properties starting with 'bnd.reporter.*'). 

### export #
Export the user defined reports.

#### Synopsis: #
	   export [options]  ...


##### Options: #
- `[ -e --exclude <string;> ]` Exclude files by pattern
- `[ -p --project <string> ]` Identify another project
- `[ -v --verbose ]` prints more processing information
- `[ -w --workspace <string> ]` Use the following workspace

### jarexport #
Export a custom report of a Jar.

#### Synopsis: #
	   jarexport [options]  <jar path> <output path>

##### Options: #
- `[ -c --configName <string> ]` A configuration name defined in the property file (check -reportconfig documentation), if not set a default configuration will be used.
- `[ -l --locale <string> ]` A locale (language-COUNTRY-variant) used to localized the report data.
- `[ -p --parameters <string;> ]` A list of parameters that will be provided to the transformation process if any. eg: --parameters 'param1=value1,param2=value2'
- `[ -P --properties <string> ]` Path to a property file
- `[ -t --template <string> ]` Path or URL to a template file used to transform the generated report (twig or xslt). eg: --template bundle.xslt
- `[ -T --templateType <string> ]` The template type (aka template file extension), must be set if it could not be guess from the template file name.

### jarreadme #
Export a readme file of a Jar (template can be parametrized with system properties starting with 'bnd.reporter.*').

#### Synopsis: #
	   jarreadme [options]  <jar path> <output path>

##### Options: #
- `[ -e --exclude <string;> ]` Exclude files by pattern
- `[ -p --project <string> ]` Identify another project
- `[ -v --verbose ]` prints more processing information
- `[ -w --workspace <string> ]` Use the following workspace

### list #
List the user defined reports.

#### Synopsis: #
	   list [options]  ...


##### Options: #
- `[ -e --exclude <string;> ]` Exclude files by pattern
- `[ -p --project <string> ]` Identify another project
- `[ -v --verbose ]` prints more processing information
- `[ -w --workspace <string> ]` Use the following workspace

### readme #
Export a set of readme files (template can be parametrized with system properties starting with 'bnd.reporter.*').

#### Synopsis: #
	   readme [options]  ...


##### Options: #
- `[ -e --exclude <string;> ]` Exclude files by pattern
- `[ -p --project <string> ]` Identify another project
- `[ -v --verbose ]` prints more processing information
- `[ -w --workspace <string> ]` Use the following workspace


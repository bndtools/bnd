---
layout: default
title:   exportreport <sub-cmd> [options]
summary: Generate and export reports of a workspace, a project or of a Jar.
---

## Description

{{page.summary}}

Reports must first be configured in the project or the workspace with the [-exportreport](../instructions/exportreport.html) intruction and optionaly with the [-reportconfig](../instructions/reportconfig.html) intruction. For an "external" Jar the reports can be configured directly with the command line (replacing the `-exportreport` instruction), however if you need to fine tune the report the `-reportconfig` has to be in a properties file. 

For a general introduction of the feature you can look at the [-exportreport](../instructions/exportreport.html) instruction documentation.

## Synopsis

    exportreport <[sub-cmd]> [options]

## Options
    
    Available sub-commands: 

      list                        - List available reports.
      export                      - Export the reports. 
      jarexport                   - Export a report of a Jar. 
       
## Sub-commands

### List

#### Description

List the reports absolute path which could be exported by the workpace and/or the projects. If this command is applied on a workspace, the command will also list reports of all the projects (except if you exclude them).

#### Synopsis

    list [options]

#### Options

    [ -e, --exclude <string;> ]   - Exclude files by pattern
    [ -p, --project <string> ]    - Identify another project
    [ -v, --verbose ]             - Prints more processing information
    [ -w, --workspace <string> ]  - Use the following workspace

### Export

#### Description

Generate and export the reports which could be exported by the workpace and/or the projects. If this command is applied on a workspace, the command will also export reports of all the projects (except if you exclude them).

#### Synopsis

    export [options]

#### Options
 
    [ -e, --exclude <string;> ]   - Exclude files by pattern
    [ -p, --project <string> ]    - Identify another project
    [ -v, --verbose ]             - Prints more processing information
    [ -w, --workspace <string> ]  - Use the following workspace

### Jar Export

#### Description

Generate and export a reports of a Jar.

#### Synopsis

    jarexport [options] <jar path> <output path>

#### Options

    [ -c, --configName <string> ]   - A configuration name defined in the property
                                      file (check -reportconfig documentation), if not
                                      set a default configuration will be used.
    [ -l, --locale <string> ]       - A locale (language-COUNTRY-variant) used to
                                      localized the report data.
    [ -p, --parameters <string;> ]  - A list of parameters that will be provided
                                      to the transformation process if any.
    [ -P, --properties <string> ]   - Path to a property file
    [ -t, --template <string> ]     - Path or URL to a template file used to
                                      transform the generated report (twig or xslt).
    [ -T, --templateType <string> ] - The template type (aka template file
                                      extension), must be set if it could not be guess
                                      from the template file name.

#### Examples

Create an `XML` report of a Jar:

    $ bnd exportreport jarexport ./m2/.../my.bundle.jar ./my-report.xml

Create a `JSON` report of a Jar:

    $ bnd exportreport jarexport ./m2/.../my.bundle.jar ./my-report.json

Generate a Jar readme:

    $ bnd exportreport jarexport --template ./my-template.twig ./m2/.../my.bundle.jar ./readme.md

Generate a web page from a Jar. Here, we specify the template type because the URL is ambiguous and a locale to get data in French:

    $ bnd exportreport jarexport --locale fr-FR --template https://..../templates/56z5f --templateType xslt ./m2/.../my.bundle.jar ./webpage.html



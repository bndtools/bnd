---
layout: default
title: buildx [options] 
summary: Build project, is deprecated but here for backward compatibility. If you use it, you should know how to use it so no more info is provided.                                 
---
## Description

{{page.summary}}

## Synopsis

    buildx [options] ...

## Options

    [ -c, --classpath <string>* ] - A list of JAR files and/or directories that should be placed on the class path before
                                    the calculation starts.
    [ -e, --eclipse ]             - Parse the file as an Eclipse .classpath file, use the information to create an Eclipse's
                                    project class path. If this option is used, the default .classpath file is not read.
    [ -f, --force ]               - 
    [ -n, --noeclipse ]           - Do not parse the .classpath file of an Eclipse project.
    [ -o, --output <string> ]     - Override the default output name of the bundle or the directory. If the output is a
                                    directory, the name will be derived from the bnd file name.
    [ -p, --pom ]                 - 
    [ -s, --sourcepath <string>* ]- 
    [ -S, --sources ]             - 

## Examples
`bnd buildx -classpath bin -noeclipse -output test.jar xyz.bnd`

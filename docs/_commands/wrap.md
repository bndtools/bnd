---
layout: default
title:  wrap [options] <<jar-file>> <[...]>
summary: Wrap a jar into a bundle. This is a poor man's facility to quickly turn a non-OSGi JAR into an OSGi bundle. It is usually better to write a bnd file and use the bnd <file>.bnd command because that has greater control. Even better is to wrap in bndtools.
---

## Description

{{page.summary}}

## Synopsis

## Options

    [ -b, --bsn <string> ]     - Set the bundle symbolic name to use
    [ -c, --classpath <string>* ] - A classpath specification
    [ -f, --force ]            - Allow override of an existing file
    [ -o, --output <string> ]  - Path to the output, default the name of the
                                input jar with the '.bar' extension. If this is
                                a directory, the output is place there.
    [ -p, --properties <string> ] - A file with properties in bnd format.
    [ -v, --version <version> ] - Set the version to use

## Examples

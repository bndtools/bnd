---
layout: default
title:    package [options] <<bnd|bndrun>> <[...]>
summary: Package a bnd or bndrun file into a single jar that executes with java -jar <>.jar. The JAR contains all dependencies, including the framework and the launcher. A profile can be specified which will be used to find properties. If a property is not found, a property with the name [<profile>]NAME will be looked up. This allows you to make different profiles for testing and runtime.
---

## Description

{{page.summary}}

## Synopsis

## Options

    [ -o, --output <string> ]  - Where to store the resulting file. Default the
                                name of the bnd file with a .jar extension.
    [ -p, --profile <string> ] - Profile name. Default no profile

## Examples



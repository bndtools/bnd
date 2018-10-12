---
layout: default
title:   extract [options] ... 
summary:  Extract files from a JAR file, equivalent jar command x[vf] (syntax supported)
---

## Description

{{page.summary}}

## Synopsis

## Options

    [ -c, --cdir <string> ]    - Directory where to store
    [ -f, --file <string> ]    - Jar file (f option)
    [ -v, --verbose ]          - Verbose (v option)

## Examples

    biz.aQute.bnd (master)$ bnd extract -c generated/tmp generated/biz.aQute.bnd.jar 

---
layout: default
title:     grep [options] <[pattern]> <[file]...> 
summary:  Grep the manifest of bundles/jar files.
---

## Description

{{page.summary}}

## Synopsis

## Options

    [ -b, --bsn ]              - Search in bsn
    [ -e, --exports ]          - Search in exports
    [ -h, --headers <string>* ] - Set header(s) to search, can be wildcarded. The
                                default is all headers (*).
    [ -i, --imports ]          - Search in imports


## Examples
    biz.aQute.bnd (master)$ bnd grep -h "*" "settings" generated/*.jar
                generated/biz.aQute.bnd.jar :      Private-Package ...ute.lib.[settings]...

   

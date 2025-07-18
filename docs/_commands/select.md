---
layout: default
title: select
summary: |
   Helps finding information in a set of JARs by filtering on manifest data and printing out selected information.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: 
	   select [options]  <<jar-path>> <[...]>

#### Options: 
- `[ -h --header <string>* ]` A manifest header to print or: path, name, size, length, modified for information about the file, wildcards are allowed to print multiple headers. 
- `[ -k --key ]` Print the key before the value
- `[ -n --name ]` Print the file name before the value
- `[ -p --path ]` Print the file path before the value
- `[ -w --where <string> ]` A simple assertion on a manifest header (e.g. Bundle-Version=1.0.1) or an OSGi filter that is asserted on all manifest headers. Comparisons are case insensitive. The key 'resources' holds the pathnames of all resources and can also be asserted to check for the presence of a header.

<!-- Manual content from: ext/select.md --><br /><br />

## Examples

    biz.aQute.bnd (master)$ bnd select -h name generated/*.jar
    biz.aQute.bnd.jar
    biz.aQute.bnd (master)$ bnd select -h size generated/*.jar
    2604654

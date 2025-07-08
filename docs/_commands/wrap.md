---
layout: default
title: wrap
summary: |
   Wrap a jar into a bundle. This is a poor man's facility to quickly turn a non-OSGi JAR into an OSGi bundle. It is usually better to write a bnd file and use the bnd <file>.bnd command because that has greater control. Even better is to wrap in bndtools.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: #
	   wrap [options]  <<jar-file>> <[...]>

#### Options: #
- `[ -b --bsn <string> ]` Set the bundle symbolic name to use
- `[ -c --classpath <string>* ]` A classpath specification
- `[ -f --force ]` Allow override of an existing file
- `[ -o --output <string> ]` Path to the output, default the name of the input jar with the '.bar' extension. If this is a directory, the output is place there.
- `[ -p --properties <string> ]` A file with properties in bnd format.
- `[ -v --version <version> ]` Set the version to use

<!-- Manual content from: ext/wrap.md --><br /><br />

The wrap command takes an existing JAR file and guesses the manifest headers that will make this JAR useful for an OSGi Service Platform. If the output file is not overridden, the name of the input file is used with a .bar extension. The default bnd file for the header calculation is:

    Export-Package: * 
    Import-Package: <packages inside the target jar>

If the target bundle has a manifest, the headers are merged with the properties.

The defaults can be overridden with a specific properties file.


## Examples
`bnd wrap -classpath osgi.jar *.jar`

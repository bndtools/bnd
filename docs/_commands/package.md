---
layout: default
title: package
summary: |
   Package a bnd or bndrun file into a single jar that executes with java -jar <>.jar. The JAR contains all dependencies, including the framework and the launcher. A profile can be specified which will be used to find properties. If a property is not found, a property with the name [<profile>]NAME will be looked up. This allows you to make different profiles for testing and runtime.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: 
	   package [options]  <<bnd|bndrun>> <[...]>

#### Options: 
- `[ -o --output <string> ]` Where to store the resulting file. Default the name of the bnd file with a .jar extension.
- `[ -p --profile <string> ]` Profile name. Default no profile


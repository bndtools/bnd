---
layout: default
class: Project
title: -include PATH-SPEC ( ',' PATH-SPEC ) * 
summary:  Include a number of files from the file system
---

You can use -include as follows:

	-include: <path or url>

This will read the path or url as a properties or manifest file (if it ends in `.MF`). 

It is important to realize that the include is not handled by the parser. That is, it is not a normal text include. The properties parser will read all properties in one go and then inspect the Properties object for the `-include` instruction. It then parses the paths or URLs in the `-include` instruction one by one in order. By default, the read properties do not override the properties that were set in the same file as the `-include` instruction.


There are two possible options. The path/URL starts with a:

* `~` – The included properties override the locally set properties.
* `-` – If file or URL or path does not exist then do not report an error.


## Examples

	# Read an optional file in the user's home directory
	-include ${java.home}/.xyz/base.bnd

	# Read a manifest
	-include META-INF/MANIFEST.MF

	# Use a URL
	-include http://example.com/foo/bar/setup.bnd

	# Read several
	
	-include first.bnd, second.properties, ~third.override


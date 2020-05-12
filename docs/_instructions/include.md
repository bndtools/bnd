---
layout: default
class: Project
title: -include PATH-SPEC ( ',' PATH-SPEC ) * 
summary:  Include a number of files from the file system
---

You can use `-include` as follows:

	-include: <path or url>

This will read the path or url as a properties or manifest file (if it ends in `.MF`). 

It is important to realize that the include is not handled by the parser. That is, it is not a normal text include. The properties parser will read all properties in one go and then the Properties object is inspected for the `-include` instruction. The paths or URLs in the `-include` instruction one by one in order. By default, the read properties do not override the properties that were set in the same file as the `-include` instruction.

The `-include` instruction is processed before anything else in the properties. This means the `-include` instruction cannot use any properties defined in the same properties file to define the include paths. It can use properties already defined by a parent. So a Project's `bnd.bnd` file can have an `-include` instruction use properties defined in the Workspace (`cnf/build.bnd` and `cnf/ext/*.bnd`). For the Workspace, `cnf/build.bnd`, `-include` instruction can only use the default properties of Bnd. The Workspace `cnf/ext/*.bnd` files are processed _after_ `cnf/build.bnd`. So `cnf/ext/*.bnd` files can have `-include` instructions which use properties set in `cnf/build.bnd`.

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


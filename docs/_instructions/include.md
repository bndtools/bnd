---
layout: default
title: -include PATH-SPEC ( ',' PATH-SPEC ) *
class: Project
summary: |
   Include a number of files from the file system
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-include: -${java.user}/.bnd`

- Pattern: `.*`

<!-- Manual content from: ext/include.md --><br /><br />

## Explanation of `-include` Instruction

The `-include` instruction allows you to include the contents of other files (such as properties or manifest files) into your current configuration. You specify the path or URL of the file to include. The included file's properties will overwrite existing properties by default, unless you use the `~` prefix, which prevents overwriting. If the file or URL does not exist and you use the `-` prefix, no error will be reported.

The `-include` instruction is processed before any other properties, so it cannot use properties defined later in the same file. However, it can use properties defined by a parent configuration. This makes it possible to share common settings across multiple projects or workspaces.


You can use `-include` as follows:

	-include: <path or url>

This will read the path or url as a properties or manifest file (if it ends in `.MF`). 

It is important to realize that the include is not handled by the parser.
That is, it is not a normal text include.
The properties parser will read all properties in one go and then the Properties object is inspected for the `-include` instruction.
The paths or URLs in the `-include` instruction are processed one by one in order.
By default, the properties in the included file overwrite the properties that were set in the same file as the `-include` instruction.
If a property is already defined and not set to be overwritten (see below), the property will get a namespace assigned.
The namespace will be derived from the filename or the last segment of the URL.

The `-include` instruction is processed before anything else in the properties.
This means the `-include` instruction cannot use any properties defined in the same properties file to define the include paths.
It can use properties already defined by a parent.
So a Project's `bnd.bnd` file can have an `-include` instruction use properties defined in the Workspace (`cnf/build.bnd` and `cnf/ext/*.bnd`).
For the Workspace, `cnf/build.bnd`, `-include` instruction can only use the default properties of Bnd.
The Workspace `cnf/ext/*.bnd` files are processed _after_ `cnf/build.bnd`.
So `cnf/ext/*.bnd` files can have `-include` instructions which use properties set in `cnf/build.bnd`.

There are two possible options. The path/URL starts with a:

* `~` – Included properties do not overwrite any existing properties having the same property names.
* `-` – If file or URL or path does not exist then do not report an error.



## Examples

	# Read an optional file in the user's home directory
	-include -${user.home}/.xyz/base.bnd

	# Read a manifest
	-include META-INF/MANIFEST.MF

	# Use a URL
	-include https://example.com/foo/bar/setup.bnd

	# Read several
	-include first.bnd, second.properties

	# Don't overwrite any existing properties (my.prop, will not be overwritten by my.prop in no.overwrite)
	my.prop = don't overwrite
	-include ~no.overwrite


TODO Needs review - AI Generated content

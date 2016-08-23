---
layout: default
class: Processor
title: -plugin.* plugin-def ( ',' plugin-def )* 
summary: Load plugins and their parameters. 
---
A plugin is a parameterized piece of code that runs inside bndlib. The `-plugin` instruction defines a plugin by specifying its class name and a given set of parameters; a specific class can be instantiated multiple times.

The `-plugin` instruction actually aggregates all properties that start with `-plugin*`. This makes it possible to set plugins in different places, for example include files or with the bndlib workspace extensions. The following sets the Git plugin:

	-plugin.git = aQute.bnd.plugin.git.GitPlugin

Plugins are created at startup using a special _plugin class loader_. This class loader is pre-loaded with any URLs set in the `-pluginpath` instructions. The plugin definition can, however, also add additional URLs to this classloader with the 'path:' directive.

If the plugin implements the `Plugin` interface, it is given the parameters specified in the `-plugin` instruction. It is then registered in the _plugin registry_ and made available to the rest of the system.  

All plugins are (unfortunately) loaded in a single class loader.

## Syntax

	-plugin*       ::= plugin-def ( ',' plugin-def )*
	plugin-def     ::= qname ( ';' ( attribute | directive ) )*

Any attributes are passed to the plugin if it implements the `aQute.bnd.servce.Plugin` interface. Consult the actual plugin for the possible attributes. 

The following directives are architected.

* `path:` – This directive specifies a comma separated list of file paths, the list must be enclosed in quotes when it contains a comma. Each of these files must be a directory or a JAR file and is added to the plugin class loader in the given sequence. 
* `command:` – If this directive is specified errors on initializing this plugin are only reported if this command is an instruction in the current properties. The purpose of this is to allow plugins to be built in the `cnf` directory; since the plugin does not exist during its first compilation errors would be reported. Since this project does not use the command itself, it can safely ignore this error.

## Typical Example

	-plugin.repo.main:\
	  aQute.lib.deployer.FileRepo; \
	  	name='Main'; \
	  	location=${build}/repo/main
	
## Errors & Warnings

* `Problem adding path <path> to loader for plugin <key>. Exception: (<e>)` – An unexpected exceptio occurred while adding a new path to the plugin class loader.
* `Cannot load the plugin <class-name>` – The give plugin cannot be loaded for an unknown reason.
* `Failed to load plugin <class-name>;<attrs>, error: <e>` – An exception occurred trying to add a new plugin.
* `While setting properties <properties> on plugin <plugin>, <e>` – An exception was thrown by the plugin when receiving its properties.
* `Plugin path: <path>, specified url <url> and a sha1 but the file does not match the sha` – The `-pluginpath.*` instruction specified a download URL and a SHA digest; the file was downloaded but the content did not match the given SHA digest.
* `Plugin path: <path>, specified url <url> and a sha1 '<sha>' but this is not a hexadecimal` – The SHA is not in hexadecimal form. 
* `No such file <path> from <plugin> and no 'url' attribute on the path so it can be downloaded` – The given path in `pluginpath*` was not found and there was no `url` attribute specified to download it from.

## Links

* `-pluginpath` – Specifies JARs that populat the plugin class loader
* `-extensions` – An alternative mechanism that can load plugins from repositories

## Caveats

* Use OSGi as the plugin system, it is kind of embarrassing that we have a poor mans plugin system when doing OSGi things. The reason is that bnd was started in 1999 and at that time OSGi was too slow and big to start/stop as often as it is done in bndlib user's environment. Alas ...





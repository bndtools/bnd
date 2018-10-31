---
layout: default
title:  xref [options] <[jar path]> <[...]> 
summary: Show a cross references for all classes in a set of jars.
---

## Description

{{page.summary}}

## Synopsis

## Options

	[ -c, --classes ]          - Show classes instead of packages
	[ -f, --from ]             - Show references from other classes/packages (<)
	[ -m, --match <string>* ]  - Filter for class names, a globbing expression
	[ -t, --to ]               - Show references to other classes/packages (>)

## Examples
   
	   biz.aQute.bnd (master)$ bnd xref generated/*.jar
	                              aQute.bnd.annotation > 
	                    aQute.bnd.annotation.component > 
	                      aQute.bnd.annotation.headers > 
	                     aQute.bnd.annotation.licenses > 
	                     aQute.bnd.annotation.metatype > 
	                                     aQute.bnd.ant > aQute.service.reporter
	                                                     org.apache.tools.ant
	                                                     aQute.libg.reporter
	                                                     org.apache.tools.ant.taskdefs
	                                                     aQute.bnd.osgi
	                                                     aQute.bnd.build
	                                                     aQute.libg.qtokens
	                                                     org.apache.tools.ant.types
	                                                     aQute.bnd.osgi.eclipse
	                                                     aQute.bnd.service.progress
	                                                     aQute.bnd.service
	                                                     aQute.bnd.version
	                                                     aQute.bnd.build.model
	                                                     aQute.bnd.build.model.clauses
	                                   aQute.bnd.build > aQute.service.reporter
	                                                     aQute.bnd.osgi
	                                                     aQute.bnd.service
	                                                     aQute.libg.command
	                                                     aQute.libg.sed
	                                                     aQute.bnd.version
	                                                     aQute.bnd.service.action
	                                                     aQute.bnd.header
	                                                     aQute.lib.io
	                                                     aQute.libg.reporter
	                                                     aQute.bnd.osgi.eclipse
	                                                     aQute.bnd.help
	                                                     aQute.lib.strings
	                                                     aQute.libg.generics
	                                                     aQute.bnd.maven.support
	                                                     aQute.libg.glob
	                                                     aQute.lib.converter
	                                                     aQute.lib.collections
	                                                     aQute.bnd.differ
	                                                     aQute.bnd.service.diff
	                                                     aQute.bnd.service.repository
	                                                     aQute.lib.deployer
	                                                     javax.naming
	                                                     aQute.lib.hex
	                                                     aQute.bnd.resource.repository
	                                                     aQute.bnd.url
	                                                     aQute.lib.settings
	                                                     aQute.bnd.service.url
	                                                     aQute.bnd.service.extension
	                             aQute.bnd.build.model > aQute.bnd.build.model.conversions
	                                                     aQute.libg.tuple
	                                                     aQute.bnd.build.model.clauses
	                                                     aQute.bnd.header
	                                                     aQute.bnd.properties
	                                                     aQute.bnd.build
	                                                     aQute.bnd.version
	                                                     aQute.lib.io
	                                                     org.osgi.resource
	                                                     aQute.bnd.osgi
	                     aQute.bnd.build.model.clauses > aQute.bnd.header
	                 aQute.bnd.build.model.conversions > aQute.bnd.header
	                                                     aQute.libg.tuple
	                                                     aQute.bnd.osgi
	                                                     aQute.bnd.build.model
	                                                     aQute.bnd.build.model.clauses
	                                                     org.osgi.resource
	                                                     aQute.bnd.osgi.resource
	                                                     aQute.libg.qtokens
	                           aQute.bnd.compatibility > aQute.bnd.osgi
	                               aQute.bnd.component > aQute.bnd.osgi
	                                                     aQute.service.reporter
	                                                     aQute.lib.collections
	                                                     org.osgi.service.component.annotations
	                                                     aQute.bnd.version
	                                                     aQute.bnd.component.error
	                                                     aQute.lib.tag
	                                                     aQute.bnd.header
	                                                     aQute.bnd.service
	                         aQute.bnd.component.error > 
	                                  aQute.bnd.differ > aQute.bnd.header
	                                                     aQute.bnd.service.diff
	                                                     aQute.bnd.osgi
	                                                     aQute.bnd.version
	                                                     aQute.service.reporter
	                                                     aQute.libg.generics
	                                                     aQute.libg.cryptography
	                                                     aQute.lib.hex
	                                                     aQute.lib.io
	                                                     aQute.lib.collections
	                                                     aQute.bnd.annotation
	                                                     aQute.bnd.service
	                        aQute.bnd.enroute.commands > aQute.lib.getopt
	                                                     aQute.bnd.osgi
	                                                     aQute.service.reporter
	                                                     aQute.bnd.main
	                                                     aQute.bnd.build
	                                                     aQute.lib.io
	                                aQute.bnd.filerepo > aQute.bnd.version
	                                  aQute.bnd.header > aQute.bnd.version
	                                                     aQute.bnd.osgi
	                                                     aQute.service.reporter
	                                                     aQute.libg.generics
	                                                     aQute.libg.qtokens
	                                                     aQute.lib.collections
	                                    aQute.bnd.help > aQute.bnd.osgi
	   

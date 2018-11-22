---
layout: default
title:      do [options] ... 
summary: Execute a file based on its extension. Supported extensions are bnd (build), bndrun (run), and jar (print) 
---

## Description

{{page.summary}}

## Synopsis

## Options

	[ -f, --force ]            - Force even when there are errors
	[ -o, --output <string> ]  - The output file


## Examples

	biz.aQute.bnd (master)$ bnd do generated/biz.aQute.bnd.jar 
	[MANIFEST biz.aQute.bnd]
	Bnd-LastModified                         1404915822703                           
	Bundle-Copyright                         Copyright (c) aQute (2000, 2014). All Rights Reserved.
	Bundle-Description                       This command line utility is the Swiss army knife of OSGi. It provides you with a breadth
	Bundle-DocURL                            http://www.aQute.biz/Code/Bnd           
	Bundle-License                           http://www.opensource.org/licenses/apache2.0.php; description="Apache License, Version 2.0"; link=http://www.apache.org/licenses/LICENSE-2.0.html
	Bundle-ManifestVersion                   2                                       
	Bundle-Name                              biz.aQute.bnd                           
	Bundle-SCM                               git://github.com/bndtools/bnd.git       
	Bundle-SymbolicName                      biz.aQute.bnd                           
	Bundle-Vendor                            aQute SARL http://www.aQute.biz         
	Bundle-Version                           2.4.0.201407091423                      
	Conditional-Package                      aQute.libg.*,aQute.lib.*,aQute.configurable
	Created-By                               1.8.0 (Oracle Corporation)              
	Export-Package                           aQute.bnd.service;version="4.1.0";uses:="aQute.bnd.build,aQute.bnd.osgi,aQute.bnd.version,aQute.service.reporter",aQute.bnd.service.action;version="2.0.0";uses:="aQute.bnd.build",aQute.bnd.service.classparser;version="1.0";uses:="aQute.bnd.osgi",aQute.bnd.service.diff;version="1.0";uses:="aQute.bnd.osgi",aQute.bnd.service.extension;version="1.0";uses:="aQute.bnd.build",aQute.bnd.service.progress;version="1.0.0",aQute.bnd.service.repository;version="1.2";uses:="aQute.bnd.service,aQute.bnd.version,aQute.service.reporter,org.osgi.resource",aQute.bnd.service.resolve.hook;version="1.0";uses:="org.osgi.resource",aQute.bnd.service.url;version="1.2",aQute.bnd.header;version="1.3.0";uses:="aQute.bnd.version,aQute.service.reporter",aQute.bnd.osgi;version="2.3.0";uses:="aQute.bnd.build,aQute.bnd.header,aQute.bnd.service,aQute.bnd.util.dto,aQute.bnd.version,aQute.service.reporter",aQute.bnd.build;version="2.4.0";uses:="aQute.bnd.maven.support,aQute.bnd.osgi,aQute.bnd.service,aQute.bnd.service.action,aQute.bnd.version,aQute.service.reporter",aQute.bnd.version;version="1.1.0",aQute.bnd.maven.support;version="2.0";uses:="aQute.bnd.service,aQute.bnd.version,aQute.service.reporter,javax.xml.xpath,org.w3c.dom",org.osgi.service.bindex;version="1.0",aQute.service.reporter;version="1.0.1",aQute.bnd.osgi.resource;version="1.4.0";uses:="aQute.bnd.header,aQute.bnd.util.dto,org.osgi.resource",org.osgi.service.repository;version="1.0";uses:="org.osgi.resource",org.osgi.resource;version="1.0",aQute.bnd.util.dto;version="1.0"
	Git-Descriptor                           2.4.0.M1-66-gc1ad07d-dirty              
	Git-SHA                                  c1ad07dfeb4704ce590bd93c1405d7bfe8bef131
	Import-Package                           org.apache.tools.ant;resolution:=optional,org.apache.tools.ant.taskdefs;resolution:=optional,org.apache.tools.ant.types;resolution:=optional,aQute.bnd.service;version="[4.1,5)",aQute.bnd.service.action;version="[2.0,2.1)",aQute.bnd.service.diff;version="[1.0,2)",aQute.bnd.service.progress;version="[1.0,2)",aQute.bnd.service.repository;version="[1.2,2)",aQute.bnd.service.url;version="[1.2,2)",aQute.bnd.version;version="[1.1,2)",aQute.service.reporter;version="[1.0,2)",javax.crypto,javax.crypto.spec,javax.naming,javax.net.ssl,javax.script,javax.xml.namespace,javax.xml.parsers,javax.xml.transform,javax.xml.transform.dom,javax.xml.transform.stream,javax.xml.xpath,org.osgi.framework;version="[1.6,2)",org.osgi.resource;version="[1.0,2)",org.osgi.service.log;version="[1.3,2)",org.w3c.dom,org.xml.sax,org.xml.sax.helpers,junit.framework;resolution:=optional;version="[3.8,4)"
	Main-Class                               aQute.bnd.main.bnd                      
	Manifest-Version                         1.0                                     
	Private-Package                          aQute.bnd.annotation;version="1.43.2",aQute.bnd.annotation.component;version="1.43.1",aQute.bnd.annotation.headers;version="1.0",aQute.bnd.annotation.licenses;version="1.0",aQute.bnd.annotation.metatype;version="1.44.1",aQute.bnd.ant,aQute.bnd.build.model;version="2.6.0",aQute.bnd.build.model.clauses;version=2,aQute.bnd.build.model.conversions,aQute.bnd.compatibility,aQute.bnd.component,aQute.bnd.component.error;version="1.0.0",aQute.bnd.differ;version="1.1.0",aQute.bnd.enroute.commands,aQute.bnd.filerepo;version="1.0",aQute.bnd.gradle,aQute.bnd.help;version="1.1",aQute.bnd.indexer,aQute.bnd.indexer.analyzers,aQute.bnd.main;version="0.9",aQute.bnd.make,aQute.bnd.make.calltree,aQute.bnd.make.component,aQute.bnd.make.coverage,aQute.bnd.make.metatype,aQute.bnd.maven,aQute.bnd.obr,aQute.bnd.osgi.eclipse,aQute.bnd.properties;version="2.0",aQute.bnd.resource.repository,aQute.bnd.signing,aQute.bnd.testing;version="1.0",aQute.bnd.url;version="1.0",aQute.configurable;version="1.0.0",aQute.lib.deployer,embedded-repo.jar,org.osgi.service.component.annotations;version="1.3",org.osgi.service.coordinator;version="1.0",templates,aQute.lib.base64;version="1.2.0",aQute.lib.collections;version="1.2.0",aQute.lib.converter;version="2.0.1",aQute.lib.filter;version="1.1.0",aQute.lib.getopt;version="1.0.0",aQute.lib.hex;version="1.1.0",aQute.lib.io;version="1.4.0",aQute.lib.json;version="3.0.0",aQute.lib.justif;version="1.1.0",aQute.lib.persistentmap;version="1.1.0",aQute.lib.settings;version="1.2.0",aQute.lib.strings;version="1.1.0",aQute.lib.tag;version="1.1",aQute.libg.classdump;version="1.0",aQute.libg.command;version="3.0.0",aQute.libg.cryptography;version="1.1.0",aQute.libg.filelock;version="1.0.0",aQute.libg.filters;version="1.0",aQute.libg.forker;version="1.0",aQute.libg.generics;version="1.0",aQute.libg.glob;version="1.1.1",aQute.libg.map;version="1.2.0",aQute.libg.qtokens;version="1.0",aQute.libg.reporter;version="1.5",aQute.libg.sed;version="1.1.0",aQute.libg.tuple;version="1.0",aQute.lib.markdown
	Require-Capability                       osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.6))"
	Tool                                     Bnd-2.4.1.201406261752                

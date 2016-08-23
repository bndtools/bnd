---
layout: default
class: Project
title: -maven-release ('local'|'remote') ( ',' option )*   
summary:  Set the Maven release options for the Maven Bnd Repository
---

The `-maven-release` is an instruction that provides the context for a release to Maven repository. In the Maven world it is customary that a release has a JAR with sources and a JAR with Javadoc. In the OSGi world this is unnecessary because the sources can be packaged inside the bundle. (Since the source is placed at a standard location, the IDEs can take advantage of this.) However, putting an artifact on Maven Central requires that these extra JARs are included. This instruction allows you to specify additional parameters for this release process.

Though this instruction is not specific for a plugin, it was developed in conjunction with the [Maven Bnd Repository Plugin].


	-maven-release		::= ('local'|'remote' (';' snapshot )?) ( ',' option )*
	snapshot			::= <value to be used for timestamp>   
	option				::= sources | javadoc | pom
	sources				::= 'sources' ( ';' 'path' '=' path-option )?
	javadoc				::= 'javadoc' 
								( ';path=' path-option )? 
								( ';packages=' packages-option )?  
								(';' javadoc-command ) *
	path-option			::= 'NONE' | PATH
	packages-option		::= 'EXPORTS' | 'ALL' 
	javadoc-command		::= '-' NAME '=' VALUE
	pom					::= 'pom' ( ';path=' PATH )
	

	

The `aQute.maven.bnd.MavenBndRepository` is a bnd plugin that represent the local and a remote Maven repository.The locations of both repositories can be configured. The local repository is always used as a cache for the remote repository.

The repository has the following parameters:

* `url` – The remote repository URL. Credentials and proxy can be set with the [Http Client options]. The url should be the base url of a Maven repository. If no URL is configured, there is no remote repository.
* `local`  – (`~/.m2/repository`) The location of the local repository. By default this is `~/.m2/repository`. It is not possible to not have a local repository because it acts as the cache.
* `generate` – (`JAVADOC,SOURCES`) A combination of `JAVADOC` and/or `SOURCES`. If no `-maven-release` instruction is found and the released artifact contains source code, then the given classifiers are used to generate them.
* `readOnly` – (`false`) Indicates if this is a read only repository
* `name` – The name of the repository

If the Maven Bnd Repository is asked to put a file, it will look up the `-maven-release` instruction using merged properties. The property is looked up from the bnd file that built the artifact. However, it should in general be possible to define this header in the workspace using macros like `${project}` to specify relative paths.

The `-maven-release` instruction contains a set of operations

	-maven-release 	::= 'type;' type ( ','  | coordinates | javadoc | pom | sources )*
	type			::= 'local' | 'remote'
	javadoc			::= path | properties
	path			::= PATH | URL
	properties		::= entry ( ';' entry )*
	entry			::= '-' NAME '=' NAME
	pom				::= path
	sources			::= path

	
[settings]: /chapters/880-settings.html

	
	
	
[Maven Bnd Repository Plugin]: /plugins/maven
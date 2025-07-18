---
layout: default
class: Project
title: -buildrepo  repo ( ',' repo ) *
summary:  After building a JAR, release the JAR to the given repositories.  
---

The `-buildrepo` instruction allows you to automatically release JARs built by a project to one or more specified repositories. After building, if `-buildrepo` is set, the resulting JARs are placed into the named repositories, which must already exist in the workspace. You can also provide additional properties to control the release context for each repository.

The syntax is as follows:

	 -buildrepo   ::= repo ( ',' repo )*
	 repo			::= NAME ( ';' NAME ('=' VALUE)? )*
	 
The instruction provides a name of a repository, the repository must exist in the workspace. Any properties added to the name are provided as properties in the release _context_ and thus given to the repository.

This feature was inspired by the Maven Bnd Repository. In the Maven development process, projects are installed in the local repository (usually `~/.m2/repository`) so they can be shared with other Maven projects. Setting the `-buildrepo` to a Maven repository will allow a bnd workspace to participate in this process on equal footing. Every time the project is build, all its JARs are installed in the associated Maven repository.

For example:

	-plugin.maven  \
		aQute.bnd.repository.maven.provider.MavenBndRepository; \
		name=Local
		
	-buildrepo: Local

The install process is taking place in-line with the build process. It is therefore recommended to only use this for local (i.e. file system based) installs.

---
TODO Needs review - AI Generated content
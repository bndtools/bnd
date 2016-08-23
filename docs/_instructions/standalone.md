---
layout: default
class: Run
title: -standalone repo-spec (, repo-spec ) 
summary: Disconnects the bndrun file from the workspace and defines its on repositories
from: 3.0.1
---

A `bndrun` file is by default connected to its workspace, where the workspace defines the context and most important: the repositories. The workspace is by default defined in the workspace's `cnf` directory. 

The `-standalone` instruction tells bnd that this connection should be severed and that all information is contained in the `bndrun` file. The value of the `-standalone` instruction is used to define the repositories. Each `repo-spec` clause defines a repository.

	-standalone 	::= repo-spec ( ',' repo-spec )*
	repo-spec 		::= <relative url> ( '; <fixed index repo attrs> )*
	
The repositories that are created from the `-standalone` instruction are the OSGi Capabilities repositories as implemented by the Fixed Index Repository. For this repository you can add the following attributes:

* `name` – Name of the repository
* `cache` – File path to a cache directory
* `timeout` – Time out for downloading the index

## Example

	-standalone: 	index.html; name=local
	-runfw: 		org.apache.felix.framework;version=5
	-runee: 		JavaSE-1.8
	-runrequires: 	osgi.identity;filter:='(osgi.identity=com.springsource.org.apache.tools.ant)'

	
---
layout: default
class: Macro
title: -buildpath PATH
summary: Provides the class path for building the jar, the entries are references to the repositories.
---

The `-buildpath` instruction is the main mechanism to add build-time dependencies to a project. A dependency is either another project in the workspace, or a bundle in a repository. The `-buildpath` is only used during compile and build time; it is never used to run projects.  Because `-buildpath` dependencies are only used compile time it's recommended to add bundles containing only APIs; you don't need bundles containing implementations.

The `-buildpath
## Example

An example of the `-buildpath` could be the following, where three dependencies are defined: 

	-buildpath: \ 
		some.other.workspace.project;version=project,\
		osgi.core;version=4.3.1,\
		osgi.cmpn;version=4.3.1


## See Also

* [-testpath](testpath.html)


## TODO

- New function wildcards for bsns & additional repos that can limit the repos (done by Neil)


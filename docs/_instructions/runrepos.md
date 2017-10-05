---
layout: default
class: Resolve
title: -runrepos REPO-NAME ( ',' REPO-NAME )* 
summary:  Order and select the repository for resolving against. The default order is all repositories in their plugin creation order.
---

The `-runrepos` instruction is used to restrict or order the available repositories. A `bndrun` file can be based on a workspace or can be standalone. In the workspace case the, the repositories are defined in `build.bnd` or in a `*.bnd` file in the `cnf/ext` directory as bnd plugins. In the standalone case the repositories are either OSGi XML repositories listed in the `-standalone` instruction or they are also defined as plugins but now in the `bndrun` file.

In both cases there is an _ordered_ list of repositories. In the `-standalone` it is easy to change this order or exclude repositories. However, in the workspace case this is harder because the set of repositories is shared with many other projects. The `-runrepos` can then be used to exclude and reorder the list repositories. It simply lists the names of the repositories in the desired order. Each repository has its own name.

**Note** The name of a repository is not well defined. It is either the name of the repository or the `toString()` result. In the later case the name is sometimes a bit messy.

For example:

	-runrepos: Maven Central, Main, Distro

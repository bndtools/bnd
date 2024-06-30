---
layout: default
class: Resolve
title: -runrepos REPO-NAME ( ',' REPO-NAME )* 
summary:  Order and select the repository for resolving against. The default order is all repositories in their plugin creation order.
---

The `-runrepos` instruction is used to restrict or order the available repositories. A `bndrun` file (see [Resolving](/chapters/250-resolving.html#resolving-in-bndtools)) can be based on a workspace or can be standalone. In the workspace case the, the repositories are defined in `build.bnd` or in a `*.bnd` file in the `cnf/ext` directory as bnd plugins. In the standalone case the repositories are either OSGi XML repositories listed in the `-standalone` instruction or they are also defined as plugins but now in the `bndrun` file.

In both cases there is an _ordered_ list of repositories. In the `-standalone` it is easy to change this order or exclude repositories. However, in the workspace case this is harder because the set of repositories is shared with many other projects. The `-runrepos` can then be used to exclude and reorder the list repositories. It simply lists the names of the repositories in the desired order. Each repository has its own name.

If `-runrepos` is ommited then all repositories having either no tags or the tag `resolve` will be included for resolution.
You can exclude certain repositories by assigning it a tag different than `resolve` (e.g. `<<EMTPY>>` or `foobar`).


**Note** The name of a repository is not well defined. It is either the name of the repository or the `toString()` result. In the later case the name is sometimes a bit messy.

**Example 1: include specific repos**

	-runrepos: Maven Central, Main, Distro

This includes exactly the three repositories.

**Example 2: include all repos**

- remove / ommit the `-runrepos` instruction
- give all repositories either no tag or the tag `resolve`

**Example 3: include all repos, except some**

- remove / ommit the `-runrepos` instruction
- give all repositories either no tag or the tag `resolve` which should be included
- give the repo which should be excluded the tag `<<EMTPY>>` or something else than `resolve` e.g. `foobar`

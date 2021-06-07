---
layout: default
class: Macro
title: fileuri ';' PATH
summary: Return a file uri for the specified path. Relative paths are resolved against the domain processor base.
---

The specified path is evaluated against the base path of the domain Processor
and the file uri is returned.

So `${fileuri;.}` will return the file uri of a project base if used in a project's bnd file,
for example `bnd.bnd`, or the file uri of the workspace base if used in a workspace's bnd 
file, for example `cnf/build.bnd`.


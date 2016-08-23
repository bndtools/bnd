---
layout: default
class: Processor
title: uri ';' URI (';' URI)?
summary: Resolve a uri against a base uri.
---

If the specified uri is not absolute or is a `file` scheme uri, 
the specified uri is resolved against either the second uri, if specified, or
the base uri of the Processor and the resolved uri is returned.

Otherwise, the specified uri is returned.

So `${uri;.}` will return the base uri of a project if used in a project's bnd file,
for example `bnd.bnd`, or the base uri of the workspace if used in a workspace's bnd 
file, for example `cnf/build.bnd`.


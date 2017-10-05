---
layout: default
class: Workspace
title: -resolve.effective qname (',' qname )
summary: Set the use effectives for the resolver
---

Each requirement and capability has an `effective` or is `effective=resolve`. An effective of `resolve` is always processed by the resolver. In OSGi enRoute, also any effective of `active` is processed since this is the mode that is compatible with bnd. However, in (very) special cases it is necessary to provide more rules. 

The `-resolve.effective` syntax is as follows:

	-resolve.effective 	::= effective ( ',' effective )*
	effective		::= NAME (';skip:=' skip )
	skip			::= skip = '"' namespace ( ',' namespace ) * '"'

The simplest model is to just list the names, for example:

	-resolve.effective: resolve,active

In this case, the resolver will only look at requirements that are either resolve or active (which is the default in OSGi enRoute). 

Adding a `meta` effective could then be:

	-resolve.effective: resolve,active, meta

However, in very, very rare (usually error) cases it is necessary to exclude certain namespaces. This can be done by using the `skip:` directive.

	-resolve.effective: resolve,active, meta;skip:='osgi.extender,osgi.wiring.package'

---
layout: default
title: -resolve.effective qname (',' qname )
class: Workspace
summary: |
   Set the use effectives for the resolver
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-resolve.effective=resolve,active`

- Values: `qname (',' qname )`

- Pattern: `.*`

<!-- Manual content from: ext/resolve.effective.md --><br /><br />

Each requirement and capability has an `effective` or is `effective=resolve`. An effective of `resolve` is always processed by the resolver.However, in (very) special cases it is necessary to provide more rules.

The `-resolve.effective` syntax is as follows:

	-resolve.effective 	::= effective ( ',' effective )*
	effective		::= NAME (';skip:=' skip )
	skip			::= skip = '"' namespace ( ',' namespace ) * '"'

The simplest model is to just list the names, for example:

	-resolve.effective: resolve,active

In this case, the resolver will only look at requirements that are either resolve or active.

Adding a `meta` effective could then be:

	-resolve.effective: resolve,active, meta

However, in very, very rare (usually error) cases it is necessary to exclude certain namespaces. This can be done by using the `skip:` directive.

	-resolve.effective: resolve,active, meta;skip:='osgi.extender,osgi.wiring.package'

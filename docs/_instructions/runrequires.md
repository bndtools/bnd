---
layout: default
class: Resolve
title: -runrequires REQUIREMENT ( ',' REQUIREMENT )* 
summary: The root requirements for a resolve intended to create a constellation for the -runbundles.
---

The `-runrequires` instruction specifies root requirements for a resolve operation. These requirements are input into the resolver
which generates a list of bundles (the `-runbundles` list) that can satisfy the requirements.

Each requirement must be in the format of an entry in the OSGi standard `Require-Capability` header. See Section 3.3.6 ("Bundle Requirements") of OSGi Core Release 6 specification.

A requirement can specify any arbitrary namespace, including but not limited to those listed in Chapter 8 ("Framework Namespaces Specification") of OSGi Core Release 6 specification and Chapter 135 ("Common Namespaces Specification") of OSGi Compendium Release 6.

Bnd also supports *alias* namespaces -- see below. 

## Example

	-runrequires: \
		osgi.identity; filter:='(osgi.identity=org.example.foo)',\
		osgi.identity; filter:='(&(osgi.identity=org.example.bar)(version>=1.0)(!(version>=2.0)))'

This specifies a requirement for the resource identified as "org.example.foo" (with any version) AND the resource identified as "org.example.bar" with version in the range 1.0 inclusive to 2.0 exclusive.

## Requirement Aliases

To ease manual entry of requirements bnd supports alias namespaces which translate to standard namespaces and filters.

In all cases, attributes and directives that are not consumed by the alias are passed through to the generated requirement. For example if the `bnd.identity` alias is used with a directive of `resolution:=optional` then the generated `osgi.identity` requirement shall also have the directive `resolution:=optional`.

The following aliases are supported:

### `bnd.identity`

The `bnd.identity` namepace alias takes the following attributes:

* `id`: the identity of the resource
* `version`: a version range in conventional OSGi form, e.g. `[1.0, 2.0)`.

Example:

	-runrequires:\
		bnd.identity; id=org.example.foo,\
		bnd.identity; id=org.example.bar; version=1.0,\
		bnd.identity; id=org.example.baz; version='[1.0,2.0)'

is translated to:

	-runrequires:\
		osgi.identity; filter:='(osgi.identity=org.example.foo)',\
		osgi.identity; filter:='(&(osgi.identity=org.example.bar)(version>=1.0))',\
		osgi.identity; filter:='(&(osgi.identity=org.example.baz)(version>=1.0)(!(version>=2.0)))'

### `bnd.literal`

The `bnd.literal` alias can be used if you need to create a literal requirement that has a namespace clashing with one of the aliases. For example if you want to have a requirement with the literal namespace `bnd.identity`, i.e. **not** processed as an alias.

Only one attribute is used:

* `bnd.literal`: specifies the literal namespace

Example:

	-runrequires:\
		bnd.literal; bnd.literal=bnd.identity; filter:='(bnd.identity=foo)',\
		bnd.literal; bnd.literal=bnd.literal; filter:='(bnd.literal=bar)'

is translated to:

	-runrequires:\
		bnd.identity; filter:='(bnd.identity=foo)',\
		bnd.literal; filter:='(bnd.literal=bar)'

## See Also

* [-runbundles](runbundles.html)
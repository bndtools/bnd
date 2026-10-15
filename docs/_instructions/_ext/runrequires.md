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

## Eclipse Features

A requirement can reference an Eclipse feature of a [p2 repository](/plugins/p2repo.html) by adding the `type=org.eclipse.update.feature` attribute. The resolver then resolves the bundles of the feature, i.e. the `<plugin>` references of its `feature.xml` and, recursively, the ones of its included features. A feature can also be put on the container paths, see [-buildpath].

	-runrequires: \
		bnd.identity; id=org.eclipse.e4.rcp; version='[4.35,5)'; type=org.eclipse.update.feature
	-runproperties: \
		osgi.os=win32,\
		osgi.ws=win32,\
		osgi.arch=x86_64

A feature lists the launcher and SWT fragments of all platforms, only the ones of the target platform are resolved. The target platform is given by `osgi.os`, `osgi.ws` and `osgi.arch` in the [-runproperties]; a value that is not set defaults to the platform bnd runs on and `ws` follows `os`. The `os`, `ws` and `arch` of the `<plugin>` and `<includes>` of the feature are matched, and so is the `Eclipse-PlatformFilter` header of the bundles. A [-runblacklist] for the other platforms is therefore not needed. The feature itself is always resolved.

The value `*` matches every platform, which resolves the members of all platforms. The [-runproperties] are also set as system properties when the bndrun is run, so only use `*` in a bndrun that is just resolved.

The `Eclipse-PlatformFilter` header of a bundle is recorded when a p2 repository is indexed, as requirement in the `eclipse.platform` namespace. Refresh a repository that was indexed by an earlier bnd to get it. The resolver provides the target platform as capability in this namespace, unless the [-runsystemcapabilities] define one. A filter that is about more than `osgi.os`, `osgi.ws` and `osgi.arch` is not recorded.

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
* [-buildpath] for Eclipse features on the container paths.
* [-runproperties] for the platform of the resolve.
* [-runblacklist] to exclude bundles from the resolve.

[-buildpath]: buildpath.html#eclipse-features
[-runproperties]: runproperties.html
[-runblacklist]: runblacklist.html
[-runsystemcapabilities]: runsystemcapabilities.html
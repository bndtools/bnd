---
layout: default
class: Workspace
title: -runblacklist requirement (',' requirement)
summary: Blacklist a set of bundles for a resolve operation
---

The blacklist is a set of requirements. These requirements are used to get a set of resources from the repositories that match any of these requirements. This set is then removed from any result from the repositories, effectively making it impossible to use.

For example:

	-runblacklist: \
		osgi.identity;filter:='(osgi.identity=com.foo.bad.bundle)'

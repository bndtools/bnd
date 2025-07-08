---
layout: default
title: -runblacklist requirement (',' requirement)
class: Workspace
summary: |
   Blacklist a set of bundles for a resolve operation
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-runblacklist=osgi.identity;filter:='(osgi.identity=<bsn>)', ...`

- Pattern: `.*`

<!-- Manual content from: ext/runblacklist.md --><br /><br />

The blacklist is a set of requirements. These requirements are used to get a set of resources from the repositories that match any of these requirements. This set is then removed from any result from the repositories, effectively making it impossible to use.

For example:

	-runblacklist: \
		osgi.identity;filter:='(osgi.identity=com.foo.bad.bundle)'

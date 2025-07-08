---
layout: default
title: -runprovidedcapabilities
class: Workspace
summary: |
   Extra capabilities for a distro resolve
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-runprovidedcapabilities=some.namespace; some.namespace=foo`

- Pattern: `.*`

<!-- Manual content from: ext/runprovidedcapabilities.md --><br /><br />

This instruction works only with the `-distro` instruction. It provides the capabilities that are not listed in the distro file but that are still provided by the target system.

	-runprovidedcapabilities: \
		some.namespace; \
			some.namespace=foo

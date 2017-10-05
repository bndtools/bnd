---
layout: default
class: Workspace
title: -runprovidedcapabilities 
summary: Extra capabilities for a distro resolve
---

This instruction works only with the `-distro` instruction. It provides the capabilities that are not listed in the distro file but that are still provided by the target system.

	-runprovidedcapabilities: \
		some.namespace; \
			some.namespace=foo


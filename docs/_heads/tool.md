---
layout: default
title: Tool STRING
class: Header
summary: |
   Bnd version used to build this bundle
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Pattern: `.*`

<!-- Manual content from: ext/tool.md --><br /><br />

			if (!noExtraHeaders) {
				main.putValue(CREATED_BY, System.getProperty("java.version") + " (" + System.getProperty("java.vendor")
						+ ")");
				main.putValue(TOOL, "Bnd-" + getBndVersion());
				main.putValue(BND_LASTMODIFIED, "" + System.currentTimeMillis());
			}

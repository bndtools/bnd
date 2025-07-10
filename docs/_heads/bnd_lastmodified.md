---
layout: default
title: Bnd-LastModified  LONG
class: Header
summary: |
   Timestamp from bnd, aggregated last modified time of its resources
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Pattern: `.*`

<!-- Manual content from: ext/bnd_lastmodified.md --><br /><br />

			if (!noExtraHeaders) {
				main.putValue(CREATED_BY, System.getProperty("java.version") + " (" + System.getProperty("java.vendor")
						+ ")");
				main.putValue(TOOL, "Bnd-" + getBndVersion());
				main.putValue(BND_LASTMODIFIED, "" + System.currentTimeMillis());
			}

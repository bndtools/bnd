---
layout: default
class: Header
title: Bnd-LastModified  LONG
summary: Timestamp from bnd, aggregated last modified time of its resources 
---

			if (!noExtraHeaders) {
				main.putValue(CREATED_BY, System.getProperty("java.version") + " (" + System.getProperty("java.vendor")
						+ ")");
				main.putValue(TOOL, "Bnd-" + getBndVersion());
				main.putValue(BND_LASTMODIFIED, "" + System.currentTimeMillis());
			}


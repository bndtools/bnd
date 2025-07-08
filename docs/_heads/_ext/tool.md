---
layout: default
class: Header
title: Tool STRING
summary: Bnd version used to build this bundle 
---

			if (!noExtraHeaders) {
				main.putValue(CREATED_BY, System.getProperty("java.version") + " (" + System.getProperty("java.vendor")
						+ ")");
				main.putValue(TOOL, "Bnd-" + getBndVersion());
				main.putValue(BND_LASTMODIFIED, "" + System.currentTimeMillis());
			}


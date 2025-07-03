---
layout: default
class: Header
title: Created-By STRING
summary: Java version used in build 
---

			if (!noExtraHeaders) {
				main.putValue(CREATED_BY, System.getProperty("java.version") + " (" + System.getProperty("java.vendor")
						+ ")");
				main.putValue(TOOL, "Bnd-" + getBndVersion());
				main.putValue(BND_LASTMODIFIED, "" + System.currentTimeMillis());
			}


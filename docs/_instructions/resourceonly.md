---
layout: default
title: -resourceonly  BOOLEAN
class: Project
summary: |
   Ignores warning if the bundle only contains resources and no classes.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-resourceonly=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/resourceonly.md --><br /><br />

	public boolean isNoBundle() {
		return isTrue(getProperty(RESOURCEONLY)) || isTrue(getProperty(NOMANIFEST));
	}

	/**
	 * @return
	 */
	boolean isResourceOnly() {
		return isTrue(getProperty(RESOURCEONLY));
	}

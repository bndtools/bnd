---
layout: default
title: -strict BOOLEAN
class: Processor
summary: |
   If strict is true, then extra verification is done.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-strict=true`

- Values: `true,false`

- Pattern: `true|false|TRUE|FALSE`

<!-- Manual content from: ext/strict.md --><br /><br />

	/**
	 * If strict is true, then extra verification is done.
	 */
	boolean isStrict() {
		if (strict == null)
			strict = isTrue(getProperty(STRICT)); // Used in property access
		return strict;
	}

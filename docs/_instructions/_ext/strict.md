---
layout: default
class: Processor
title: -strict BOOLEAN
summary:  If strict is true, then extra verification is done.
---

	/**
	 * If strict is true, then extra verification is done.
	 */
	boolean isStrict() {
		if (strict == null)
			strict = isTrue(getProperty(STRICT)); // Used in property access
		return strict;
	}


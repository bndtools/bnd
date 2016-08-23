---
layout: default
class: Processor
title: basedir
summary: Get the basedirectory of this processor
---
layout: default

	public String _basedir(@SuppressWarnings("unused") String args[]) {
		if (base == null)
			throw new IllegalArgumentException("No base dir set");

		return base.getAbsolutePath();
	}


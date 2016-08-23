---
layout: default
class: Processor
title: -pedantic BOOLEAN
summary: Warn about things that are not really wrong but still not right. 
---

	protected void begin() {
		if (isTrue(getProperty(PEDANTIC)))
			setPedantic(true);
	}


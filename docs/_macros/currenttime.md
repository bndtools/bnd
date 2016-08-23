---
layout: default
class: Macro
title: currenttime
summary: The current epoch time in long integer format
---
layout: default

	public String _currenttime(String args[]) {
		return Long.toString(System.currentTimeMillis());
	}

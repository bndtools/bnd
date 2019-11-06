---
layout: default
class: Macro
title: currenttime
summary: The current epoch time in long integer format
---

	public String _currenttime(String args[]) {
		return Long.toString(System.currentTimeMillis());
	}

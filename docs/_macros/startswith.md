---
layout: default
class: Macro
title: startswith ';' STRING ';' PREFIX
summary: Check if the given string starts with the given prefix
---

	static String	_startswith	= "${startswith;<string>;<prefix>}";
	public String _startswith(String args[]) throws Exception {
		verifyCommand(args, _startswith, null, 3, 3);
		if (args[1].startsWith(args[2]))
			return args[1];
		else
			return "";
	}

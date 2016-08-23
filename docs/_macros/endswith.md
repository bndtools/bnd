---
layout: default
class: Macro
title: endswith ';' STRING ';' SUFFIX
summary: Check if the given string ends with the given prefix
---

	static String	_endswith	= "${endswith;<string>;<suffix>}";

	public String _endswith(String args[]) throws Exception {
		verifyCommand(args, _endswith, null, 3, 3);
		if (args[1].endsWith(args[2]))
			return args[1];
		else
			return "";
	}


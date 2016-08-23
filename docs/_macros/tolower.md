---
layout: default
class: Macro
title: tolower STRING
summary: Turn a string into an lower case string
---

	static String	_tolower	= "${tolower;<target>}";

	public String _tolower(String args[]) throws Exception {
		verifyCommand(args, _tolower, null, 2, 2);

		return args[1].toLowerCase();
	}


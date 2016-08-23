---
layout: default
class: Macro
title: toupper STRING
summary: Turn a string into an uppercase string
---

	static String	_toupper	= "${toupper;<target>}";

	public String _toupper(String args[]) throws Exception {
		verifyCommand(args, _tolower, null, 2, 2);

		return args[1].toUpperCase();
	}


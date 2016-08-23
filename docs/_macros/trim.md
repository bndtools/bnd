---
layout: default
class: Macro
title: trim ';' STRING 
summary: Remove whitespace around the given string
---

	static String	_trim	= "${trim;<target>}";

	public String _trim(String args[]) throws Exception {
		verifyCommand(args, _trim, null, 2, 2);

		return args[1].trim();
	}


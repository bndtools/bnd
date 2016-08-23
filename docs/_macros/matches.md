---
layout: default
class: Macro
title: matches STRING REGEX
summary: Check if the given string matches the regular expression
---

	static String	_matches	= "${matches;<target>;<regex>}";

	public boolean _matches(String args[]) throws Exception {
		verifyCommand(args, _matches, null, 3, 3);

		return args[1].matches(args[2]);
	}



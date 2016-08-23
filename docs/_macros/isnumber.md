---
layout: default
class: Macro
title: isnumber ( ';' STRING )* 
summary: Check if the given strings are numbers
---

	static String	_isnumber	= "${isnumber[;<target>...]}";

	public boolean _isnumber(String args[]) throws Exception {
		verifyCommand(args, _isnumber, null, 2, Integer.MAX_VALUE);

		for (int i = 1; i < args.length; i++)
			if (!NUMERIC_P.matcher(args[i]).matches())
				return false;

		return true;
	}


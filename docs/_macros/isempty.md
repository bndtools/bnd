---
layout: default
class: Macro
title: isempty ( ';' STRING )* 
summary: True if all given strings are empty
---

	static String	_isempty	= "${isempty;[<target>...]}";

	public boolean _isempty(String args[]) throws Exception {
		verifyCommand(args, _isempty, null, 1, Integer.MAX_VALUE);

		for (int i = 1; i < args.length; i++)
			if (!args[i].isEmpty())
				return false;

		return true;
	}

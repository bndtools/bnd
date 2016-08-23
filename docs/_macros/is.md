---
layout: default
class: Macro
title: is ( ';' ANY )* 
summary: Check if the given values are all equal
---

	static String	_is	= "${is;<a>;<b>}";

	public boolean _is(String args[]) throws Exception {
		verifyCommand(args, _is, null, 3, Integer.MAX_VALUE);
		String a = args[1];

		for (int i = 2; i < args.length; i++)
			if (!a.equals(args[i]))
				return false;

		return true;
	}


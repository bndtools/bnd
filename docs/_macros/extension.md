---
layout: default
class: Macro
title: extension STRING
summary: The extension of the given string (the part after the '.') or empty
---

	static String	_extension	= "${extension;<string>}";

	public String _extension(String args[]) throws Exception {
		verifyCommand(args, _extension, null, 2, 2);
		String name = args[1];
		int n = name.indexOf('.');
		if (n < 0)
			return "";
		return name.substring(n + 1);
	}

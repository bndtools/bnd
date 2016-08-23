---
layout: default
class: Macro
title: indexof ';' STRING (';' LIST )*
summary: The index of the given string in the list, or -1 if not found
---

	static String	_indexof	= "${indexof;<value>;<list>[;<list>...]}";

	public int _indexof(String args[]) throws Exception {
		verifyCommand(args, _indexof, null, 3, Integer.MAX_VALUE);

		String value = args[1];
		ExtList<String> list = toList(args, 2, args.length);
		return list.indexOf(value);
	}


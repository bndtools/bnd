---
layout: default
class: Macro
title: lastindexof ';' STRING (';' LIST )*
summary: The last index of the given string in the list, or -1 if not found
---

	static String	_lastindexof	= "${lastindexof;<value>;<list>[;<list>...]}";

	public int _lastindexof(String args[]) throws Exception {
		verifyCommand(args, _indexof, null, 3, Integer.MAX_VALUE);

		String value = args[1];
		ExtList<String> list = toList(args, 1, args.length);
		return list.lastIndexOf(value);
	}


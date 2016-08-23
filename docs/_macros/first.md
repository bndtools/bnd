---
layout: default
class: Macro
title: first (';' LIST )*
summary: First element of a list
---
	static String	_first	= "${first;<list>[;<list>...]}";

	public String _first(String args[]) throws Exception {
		verifyCommand(args, _first, null, 1, Integer.MAX_VALUE);

		List<String> list = toList(args, 1, args.length);
		if (list.isEmpty())
			return "";

		return list.get(0);
	}


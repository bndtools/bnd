---
layout: default
class: Macro
title: last (';' LIST )*
summary: Last element of a list
---

	static String	_last	= "${last;<list>[;<list>...]}";

	public String _last(String args[]) throws Exception {
		verifyCommand(args, _last, null, 1, Integer.MAX_VALUE);

		List<String> list = toList(args, 1, args.length);
		if (list.isEmpty())
			return "";

		return list.get(list.size() - 1);
	}


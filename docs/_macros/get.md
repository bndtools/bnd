---
layout: default
class: Macro
title: get ';' INDEX (';' LIST )*
summary: The element from the concatenated lists at the given index
---

	static String	_get	= "${get;<index>;<list>}";

	public String _get(String args[]) throws Exception {
		verifyCommand(args, _get, null, 3, 3);

		int index = Integer.parseInt(args[1]);
		List<String> list = toList(args, 2, 3);
		if (index < 0)
			index = list.size() + index;
		return list.get(index);
	}


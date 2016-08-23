---
layout: default
class: Macro
title: compare STRING STRING
summary: Compare two strings. 0 is equal, 1 means a > b, -1 is a < b.
---

	static String	_compare	= "${compare;<astring>;<bstring>}";

	public int _compare(String args[]) throws Exception {
		verifyCommand(args, _compare, null, 3, 3);
		int n = args[1].compareTo(args[2]);
		if (n == 0)
			return 0;

		return n > 0 ? 1 : -1;
	}


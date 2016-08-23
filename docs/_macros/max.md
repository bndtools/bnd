---
layout: default
class: Macro
title: max (';' LIST )*
summary: Maximum (string compared) element of a list
---


	static String	_max	= "${max;<list>[;<list>...]}";

	public String _max(String args[]) throws Exception {
		verifyCommand(args, _max, null, 2, Integer.MAX_VALUE);

		List<String> list = toList(args, 1, args.length);
		String a = null;

		for (String s : list) {
			if (a == null || a.compareTo(s) < 0)
				a = s;
		}
		if (a == null)
			return "";

		return a;
	}


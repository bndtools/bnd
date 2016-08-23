---
layout: default
class: Macro
title: min (';' LIST )*
summary: Minimum (string compared) element of a list
---


	static String	_min	= "${min;<list>[;<list>...]}";

	public String _min(String args[]) throws Exception {
		verifyCommand(args, _min, null, 2, Integer.MAX_VALUE);

		List<String> list = toList(args, 1, args.length);
		String a = null;

		for (String s : list) {
			if (a == null || a.compareTo(s) > 0)
				a = s;
		}
		if (a == null)
			return "";

		return a;
	}


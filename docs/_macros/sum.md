---
layout: default
class: Macro
title: sum (';' LIST )*
summary: The sum of a list
---


	static String	_sum	= "${sum;<list>[;<list>...]}";

	public String _sum(String args[]) throws Exception {
		verifyCommand(args, _sum, null, 2, Integer.MAX_VALUE);

		List<String> list = toList(args, 1, args.length);
		double d = 0;

		for (String s : list) {
			double v = Double.parseDouble(s);
			d += v;
		}
		return toString(d);
	}


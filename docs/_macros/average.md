---
layout: default
class: Macro
title: average (';' LIST )*
summary: The average of a list, if no members exception is thrown
---

	static String	_average	= "${average;<list>[;<list>...]}";

	public String _average(String args[]) throws Exception {
		verifyCommand(args, _sum, null, 2, Integer.MAX_VALUE);

		List<String> list = toList(args, 1, args.length);
		if (list.isEmpty())
			throw new IllegalArgumentException("No members in list to calculate average");

		double d = 0;

		for (String s : list) {
			double v = Double.parseDouble(s);
			d += v;
		}
		return toString(d / list.size());
	}


---
layout: default
class: Macro
title: nmin (';' LIST )*
summary: Minimum (numerically compared) element of a list
---


	static String	_nmin	= "${nmin;<list>[;<list>...]}";

	public String _nmin(String args[]) throws Exception {
		verifyCommand(args, _nmin, null, 2, Integer.MAX_VALUE);

		List<String> list = toList(args, 1, args.length);
		double d = Double.NaN;

		for (String s : list) {
			double v = Double.parseDouble(s);
			if (Double.isNaN(d) || v < d)
				d = v;
		}
		return toString(d);
	}


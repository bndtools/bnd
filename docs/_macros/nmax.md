---
layout: default
class: Macro
title: nmax (';' LIST )*
summary: Maximum (numerically compared) element of a list
---


	static String	_nmax	= "${nmax;<list>[;<list>...]}";

	public String _nmax(String args[]) throws Exception {
		verifyCommand(args, _nmax, null, 2, Integer.MAX_VALUE);

		List<String> list = toList(args, 1, args.length);
		double d = Double.NaN;

		for (String s : list) {
			double v = Double.parseDouble(s);
			if (Double.isNaN(d) || v > d)
				d = v;
		}
		return toString(d);
	}


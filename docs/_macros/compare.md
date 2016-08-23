---
layout: default
class: Macro
title: compare NUMBER NUMBER
summary: Compare two numbers. 0 is equal, 1 means a > b, -1 is a < b.
---

	static String	_ncompare	= "${ncompare;<anumber>;<bnumber>}";

	public int _ncompare(String args[]) throws Exception {
		verifyCommand(args, _ncompare, null, 3, 3);
		double a = Double.parseDouble(args[1]);
		double b = Double.parseDouble(args[2]);
		if (a > b)
			return 1;
		if (a < b)
			return -1;
		return 0;
	}


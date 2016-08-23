---
layout: default
class: Macro
title: length STRING
summary: The length of the given string
---

	static String	_length	= "${length;<string>}";

	public int _length(String args[]) throws Exception {
		verifyCommand(args, _length, null, 1, 2);
		if (args.length == 1)
			return 0;

		return args[1].length();
	}

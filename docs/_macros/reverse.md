---
layout: default
class: Macro
title: reverse (';' LIST )*
summary: A reversed list
---

	static String	_reverse	= "${reverse;<list>[;<list>...]}";

	public String _reverse(String args[]) throws Exception {
		verifyCommand(args, _reverse, null, 2, Integer.MAX_VALUE);

		ExtList<String> list = toList(args, 1, args.length);
		Collections.reverse(list);
		return Processor.join(list);
	}

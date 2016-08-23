---
layout: default
class: Macro
title: sublist ';' START ';' END (';' LIST )*
summary: Return a sublist of the list
---

	static String	_sublist	= "${sublist;<start>;<end>;<list>}";

	public String _sublist(String args[]) throws Exception {
		verifyCommand(args, _sublist, null, 4, Integer.MAX_VALUE);

		int start = Integer.parseInt(args[1]);
		int end = Integer.parseInt(args[2]);
		ExtList<String> list = toList(args, 3, Integer.MAX_VALUE);

		if (start < 0)
			start = list.size() + start + 1;

		if (end < 0)
			end = list.size() + end + 1;

		if (start > end) {
			int t = start;
			start = end;
			end = t;
		}

		return Processor.join(list.subList(start, end));
	}


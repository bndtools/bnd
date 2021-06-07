---
layout: default
class: Macro
title: join ( ';' LIST )+
summary: Join a number of list/values into a single list
---

	static String	_joinHelp	= "${join;<list>...}";

	public String _join(String args[]) {

		verifyCommand(args, _joinHelp, null, 1, Integer.MAX_VALUE);

		List<String> result = new ArrayList<String>();
		for (int i = 1; i < args.length; i++) {
			Processor.split(args[i], result);
		}
		return Processor.join(result);
	}


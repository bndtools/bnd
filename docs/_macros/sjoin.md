---
layout: default
class: Macro
title: sjoin ';' SEPARATOR ( ';' LIST )+
summary: Join a number of list/values into a single list with a given separator
---

	static String	_sjoinHelp	= "${sjoin;<separator>;<list>...}";
	public String _sjoin(String args[]) throws Exception {
		verifyCommand(args, _sjoinHelp, null, 2, Integer.MAX_VALUE);

		List<String> result = new ArrayList<String>();
		for (int i = 2; i < args.length; i++) {
			Processor.split(args[i], result);
		}
		return Processor.join(args[1], result);
	}
	

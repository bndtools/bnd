---
layout: default
class: Macro
title: sort (';' LIST )+
summary: Concatenate a set of lists and sort their contents on their string value
---

	public String _sort(String args[]) {
		verifyCommand(args, _sortHelp, null, 2, Integer.MAX_VALUE);

		List<String> result = new ArrayList<String>();
		for (int i = 1; i < args.length; i++) {
			Processor.split(args[i], result);
		}
		Collections.sort(result);
		return Processor.join(result);
	}


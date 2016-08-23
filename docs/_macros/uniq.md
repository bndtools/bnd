---
layout: default
class: Macro
title: uniq (';' LIST )*
summary: Concatenate the lists and then remove any duplicates.
---

Split all the given lists on their commas, combine them in one list and remove any duplicates. The ordering is not preserved, see [${sort}][#sort] For example:
  
  	${unique; 1,2,3,1,2; 1,2,4 } ~ "2,4,3,1"

	static String	_uniqHelp	= "${uniq;<list> ...}";

	public String _uniq(String args[]) {
		verifyCommand(args, _uniqHelp, null, 1, Integer.MAX_VALUE);
		Set<String> set = new LinkedHashSet<String>();
		for (int i = 1; i < args.length; i++) {
			Processor.split(args[i], set);
		}
		return Processor.join(set, ",");
	}

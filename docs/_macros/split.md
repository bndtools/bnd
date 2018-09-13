---
layout: default
class: Macro
title: split ';' REGEX (';' STRING )*
summary: Split a number of strings into a list using a regular expression
---

	static String	_split	= "${split;<regex>[;<target>...]}";

	public String _split(String args[]) throws Exception {
		verifyCommand(args, _split, null, 2, Integer.MAX_VALUE);

		List<String> collected = new ArrayList<String>();
		for (int n = 2; n < args.length; n++) {
			String value = args[n];
			String[] split = value.split(args[1]);
			for (String s : split)
				if (!s.isEmpty())
					collected.add(s);
		}
		return Processor.join(collected);
	}


---
layout: default
class: Macro
title: nsort (';' LIST )+
summary: Concatenate a set of lists and sort their contents nummerically
---

	static String	_nsortHelp	= "${nsort;<list>...}";

	public String _nsort(String args[]) {
		verifyCommand(args, _nsortHelp, null, 2, Integer.MAX_VALUE);

		ExtList<String> result = new ExtList<String>();
		for (int i = 1; i < args.length; i++) {
			result.addAll(ExtList.from(args[i]));
		}
		Collections.sort(result, new Comparator<String>() {

			public int compare(String a, String b) {
				while (a.startsWith("0"))
					a = a.substring(1);

				while (b.startsWith("0"))
					b = b.substring(1);

				if (a.length() == b.length())
					return a.compareTo(b);
				else if (a.length() > b.length())
					return 1;
				else
					return -1;

			}
		});
		return result.join();
	}

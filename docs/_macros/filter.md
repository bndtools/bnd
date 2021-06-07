---
layout: default
class: Macro
title: filter ';' LIST ';' REGEX
summary: Filters entries in a list that matching a regular expression
---

	public String _filter(String args[]) {
		return filter(args, false);
	}

	public String _filterout(String args[]) {
		return filter(args, true);

	}

	static String	_filterHelp	= "${ % s;<list>;<regex>}";

	String filter(String[] args, boolean include) {
		verifyCommand(args, String.format(_filterHelp, args[0]), null, 3, 3);

		Collection<String> list = new ArrayList<String>(Processor.split(args[1]));
		Pattern pattern = Pattern.compile(args[2]);

		for (Iterator<String> i = list.iterator(); i.hasNext();) {
			if (pattern.matcher(i.next()).matches() == include)
				i.remove();
		}
		return Processor.join(list);
	}

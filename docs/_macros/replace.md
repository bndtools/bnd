---
layout: default
class: Macro
title: replace ';' LIST ';' REGEX ';' REPLACE (';' STRING)?
summary: Replace elements in a list when it matches a regular expression
---

Replace all elements of the list that match the regular expression regex with the replacement. The replacement can use the `$[0-9]` back references defined in the regular expressions. The macro uses `item.replaceAll(regex,replacement)` method to do the replacement.For example, to add a `.jar` extension to all files listed, use the following: \\</td>
`List = ${replace;${impls};$;.jar}`

	/**
	 * replace ; <list> ; regex ; replace
	 * 
	 * @param args
	 * @return
	 */
	public String _replace(String args[]) {
		if (args.length < 4 || args.length > 5) {
			domain.warning("Invalid nr of arguments to replace " + Arrays.asList(args));
			return null;
		}
		
		String middle = ", ";
		if ( args.length > 4)
			middle = args[4];
		

		String list[] = args[1].split("\\s*,\\s*");
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (int i = 0; i < list.length; i++) {
			String element = list[i].trim();
			if (!element.equals("")) {
				sb.append(del);
				sb.append(element.replaceAll(args[2], args[3]));
				del = middle;
			}
		}

		return sb.toString();
	}

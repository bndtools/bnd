---
layout: default
class: Macro
title: substring ';' STRING ';' START ( ';' END )?
summary: Return a substring of a given string, negative indexes allowed
---

	static String	_substring	= "${substring;<string>;<start>[;<end>]}";

	public String _substring(String args[]) throws Exception {
		verifyCommand(args, _substring, null, 3, 4);

		String string = args[1];
		int start = Integer.parseInt(args[2].equals("") ? "0" : args[2]);
		int end = string.length();

		if (args.length > 3) {
			end = Integer.parseInt(args[3]);
			if (end < 0)
				end = string.length() + end;
		}

		if (start < 0)
			start = string.length() + start;
		
		if ( start > end ) {
			int t = start;
			start = end;
			end = t;
		}

		return string.substring(start, end);
	}

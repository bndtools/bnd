---
layout: default
class: Macro
title: now ( 'long' | DATEFORMAT )
summary: Current date and time, default is default Date format. The format can be specified as a long or a date format.
---

	public final static String	_nowHelp	= "${now;pattern|'long'}, returns current time";

	public Object _now(String args[]) {
		verifyCommand(args, _nowHelp, null, 1, 2);
		Date now = new Date();

		if (args.length == 2) {
			if ("long".equals(args[1]))
				return now.getTime();

			DateFormat df = new SimpleDateFormat(args[1]);
			return df.format(now);
		}
		return now;
	}


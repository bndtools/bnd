---
layout: default
class: Macro
title: tstamp ( ';' DATEFORMAT ( ';' TIMEZONE ( ';' LONG )? )? )?
summary: Create a timestamp based on a date format. Default format is "yyyyMMddHHmm" 
---

	public String _tstamp(String args[]) {
		String format = "yyyyMMddHHmm";
		long now = System.currentTimeMillis();
		TimeZone tz = TimeZone.getTimeZone("UTC");

		if (args.length > 1) {
			format = args[1];
		}
		if (args.length > 2) {
			tz = TimeZone.getTimeZone(args[2]);
		}
		if (args.length > 3) {
			now = Long.parseLong(args[3]);
		}
		if (args.length > 4) {
			domain.warning("Too many arguments for tstamp: " + Arrays.toString(args));
		}

		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setTimeZone(tz);

		return sdf.format(new Date(now));
	}

---
layout: default
class: Macro
title: long2date
summary: Turn a long time into a date
---

	public String _long2date(String args[]) {
		try {
			return new Date(Long.parseLong(args[1])).toString();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return "not a valid long";
	}


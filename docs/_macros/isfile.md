---
layout: default
class: Macro
title: isfile (';' FILE )+
summary: Returns true if all given files actually exist and are not a directory or special file.
---

	public String _isfile(String args[]) {
		if (args.length < 2) {
			domain.warning("Need at least one file name for ${isfile;...}");
			return null;
		}
		boolean isfile = true;
		for (int i = 1; i < args.length; i++) {
			File f = new File(args[i]).getAbsoluteFile();
			isfile &= f.isFile();
		}
		return isfile ? "true" : "false";

	}

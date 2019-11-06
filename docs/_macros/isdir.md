---
layout: default
class: Macro
title: isdir ( ';' FILE )+
summary: True if all given files are directories, false if no file arguments
---

	public String _isdir(String args[]) {
		boolean isdir = true;
		// If no dirs provided, return false
		if (args.length < 2) {
			isdir = false;
		}
		for (int i = 1; i < args.length; i++) {
			File f = new File(args[i]).getAbsoluteFile();
			isdir &= f.isDirectory();
		}
		return isdir ? "true" : "false";

	}

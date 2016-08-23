---
layout: default
class: Macro
title: env ';' KEY
summary: The given environment variable or an empty string "" if not found
---
layout: default

	public String _env(String args[]) {
		verifyCommand(args, "${env;<name>}, get the environmet variable", null, 2, 2);

		try {
			String ret = System.getenv(args[1]);
			return ret != null ? ret : "";
		}
		catch (Throwable t) {
			return "";
		}
	}

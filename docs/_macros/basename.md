---
layout: default
class: Macro
title: basename ( ';' FILEPATH ) +
summary: A list of the basename (the final part) of a set of file paths.
---

	public String _basename(String args[]) {
		if (args.length < 2) {
			domain.warning("Need at least one file name for ${basename;...}");
			return null;
		}
		String del = "";
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			File f = domain.getFile(args[i]);
			if (f.exists() && f.getParentFile().exists()) {
				sb.append(del);
				sb.append(f.getName());
				del = ",";
			}
		}
		return sb.toString();

	}

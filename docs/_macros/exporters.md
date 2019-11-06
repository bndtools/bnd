---
layout: default
title: exporters ';' PACKAGE
class: Analyzer
summary: The list of jars that export the given package
---

	public String _exporters(String args[]) throws Exception {
		Macro.verifyCommand(args, "${exporters;<packagename>}, returns the list of jars that export the given package",
				null, 2, 2);
		StringBuilder sb = new StringBuilder();
		String del = "";
		String pack = args[1].replace('.', '/');
		for (Jar jar : classpath) {
			if (jar.getDirectories().containsKey(pack)) {
				sb.append(del);
				sb.append(jar.getName());
			}
		}
		return sb.toString();
	}

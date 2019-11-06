---
layout: default
title: ide ';' ( 'javac.target' | 'javac.source' )
class: Project
summary: This reads the source and target settings from the IDE
---


	public String _ide(String[] args) throws IOException {
		if (args.length < 2) {
			error("The ${ide;<>} macro needs an argument");
			return null;
		}
		if (ide == null) {
			ide = new Properties();
			File file = getFile(".settings/org.eclipse.jdt.core.prefs");
			if (!file.isFile()) {
				error("The ${ide;<>} macro requires a .settings/org.eclipse.jdt.core.prefs file in the project");
				return null;
			}
			FileInputStream in = new FileInputStream(file);
			ide.load(in);
		}
		
		String deflt = args.length > 2 ? args[2] : null;
		if ("javac.target".equals(args[1])) {
			return ide.getProperty("org.eclipse.jdt.core.compiler.codegen.targetPlatform", deflt);
		}
		if ("javac.source".equals(args[1])) {
			return ide.getProperty("org.eclipse.jdt.core.compiler.source", deflt);
		}
		return null;
	}

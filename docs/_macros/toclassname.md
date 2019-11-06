---
layout: default
class: Macro
title: toclassname ';' FILES
summary: Translate a list of relative file paths to class names. The files can either end with .class or .java
---

	static String	_toclassnameHelp	= "${classname;<list of class names>}, convert class paths to FQN class names ";

	public String _toclassname(String args[]) {
		verifyCommand(args, _toclassnameHelp, null, 2, 2);
		Collection<String> paths = Processor.split(args[1]);

		List<String> names = new ArrayList<String>(paths.size());
		for (String path : paths) {
			if (path.endsWith(".class")) {
				String name = path.substring(0, path.length() - 6).replace('/', '.');
				names.add(name);
			} else if (path.endsWith(".java")) {
				String name = path.substring(0, path.length() - 5).replace('/', '.');
				names.add(name);
			} else {
				domain.warning("in toclassname, " + args[1] + " is not a class path because it does not end in .class");
			}
		}
		return Processor.join(names, ",");
	}

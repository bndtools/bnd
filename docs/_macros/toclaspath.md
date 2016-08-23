---
class: Macro
title: toclasspath ';' LIST ( ';' BOOLEAN )?
summary: Convert a list of class names to a list of paths.
---


	static String	_toclasspathHelp	= "${toclasspath;<list>[;boolean]}, convert a list of class names to paths";

	public String _toclasspath(String args[]) {
		verifyCommand(args, _toclasspathHelp, null, 2, 3);
		boolean cl = true;
		if (args.length > 2)
			cl = Boolean.valueOf(args[2]);

		Collection<String> names = Processor.split(args[1]);
		Collection<String> paths = new ArrayList<String>(names.size());
		for (String name : names) {
			String path = name.replace('.', '/') + (cl ? ".class" : "");
			paths.add(path);
		}
		return Processor.join(paths, ",");
	}

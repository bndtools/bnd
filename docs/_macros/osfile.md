---
layout: default
class: Macro
title: osfile ';' DIR ';' NAME
summary: Create a path to a file in OS dependent form.
---


	public final static String	_fileHelp	= "${file;<base>;<paths>...}, create correct OS dependent path";

	public String _osfile(String args[]) {
		verifyCommand(args, _fileHelp, null, 3, 3);
		File base = new File(args[1]);
		File f = Processor.getFile(base, args[2]);
		return f.getAbsolutePath();
	}

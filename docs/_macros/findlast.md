---
layout: default
class: Macro
title: findlast ';' VALUE ';' SEARCHED
summary: The starting position of SEARCHED (not a regex) in VALUE when searching from the end
---

	static String	_findlast	= "${findlast;<find>;<target>}";

	public int _findlast(String args[]) throws Exception {
		verifyCommand(args, _findlast, null, 3, 3);

		return args[2].lastIndexOf(args[1]);
	}


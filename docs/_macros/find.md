---
layout: default
class: Macro
title: find ';' VALUE ';' SEARCHED
summary: The starting position ofof SEARCHED (not a regex) in VALUE
---

	static String	_find	= "${find;<target>;<searched>}";

	public int _find(String args[]) throws Exception {
		verifyCommand(args, _find, null, 3, 3);

		return args[1].indexOf(args[2]);
	}


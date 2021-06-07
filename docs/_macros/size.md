---
layout: default
class: Macro
title: size ( ';' LIST )*
summary: Count the number of elements (of all collections combined) 
---

	public final static String	_sizeHelp	= "${size;<collection>;...}, count the number of elements (of all collections combined)";

	public int _size(String args[]) {
		verifyCommand(args, _sizeHelp, null, 2, 16);
		int size = 0;
		for (int i = 1; i < args.length; i++) {
			ExtList<String> l = ExtList.from(args[i]);
			size += l.size();
		}
		return size;
	}

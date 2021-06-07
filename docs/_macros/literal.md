---
layout: default
class: Macro
title: literal ';' STRING
summary: A literal value for the macro, i.e. it surrounds the value with the macro prefix and suffix.
---

	public String _literal(String args[]) {
		if (args.length != 2)
			throw new RuntimeException("Need a value for the ${literal;<value>} macro");
		return "${" + args[1] + "}";
	}


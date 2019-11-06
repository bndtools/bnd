---
layout: default
class: Macro
title: error ( ';' STRING )*
summary: Raise an error consisting of all concatenated strings
---


	public String _error(String args[]) {
		for (int i = 1; i < args.length; i++) {
			domain.error(process(args[i]));
		}
		return "";
	}


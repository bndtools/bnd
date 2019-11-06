---
layout: default
class: Macro
title: warning ( ';' STRING )*
summary: Raise an error consisting of all concatenated strings
---

	public String _warning(String args[]) {
		for (int i = 1; i < args.length; i++) {
			domain.warning(process(args[i]));
		}
		return "";
	}


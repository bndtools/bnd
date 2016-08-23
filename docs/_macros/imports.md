---
layout: default
class: Analyzer
title: imports
summary: A list of the currently imported package names
---
layout: default

	public String _imports(String[] args) {
		return join(filter(getImports().keySet(), args));
	}


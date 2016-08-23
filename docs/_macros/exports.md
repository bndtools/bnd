---
layout: default
class: Analyzer
title: exports
summary: A list if exported packages
---
layout: default-

	public String _exports(String[] args) {
		return join(filter(getExports().keySet(), args));
	}

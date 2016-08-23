---
layout: default
title: workspace
summary: The absolute file path to the current workspace 
class: Workspace
layout: default
---
layout: default-


	public String _workspace(@SuppressWarnings("unused") String args[]) {
		return getBase().getAbsolutePath();
	}


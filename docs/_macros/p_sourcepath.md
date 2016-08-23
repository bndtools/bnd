---
layout: default
class: 	Project
title: p_sourcepath
summary: The path to the project's source directory.
---

	public String _p_sourcepath(String args[]) throws Exception {
		return list(args, getSourcePath());
	}


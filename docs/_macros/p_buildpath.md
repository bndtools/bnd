---
layout: default
class: 	Project
title: 	p_buildpath
summary: The project's buildpath
---
	public String _p_buildpath(String args[]) throws Exception {
		return list(args, getBuildpath());
	}

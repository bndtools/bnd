---
layout: default
class: 	Project
title: 	p_bootclasspath
summary: The project's boot class path
---


	public String _p_bootclasspath(String args[]) throws Exception {
		return list(args, getBootclasspath());
	}


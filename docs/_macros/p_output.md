---
layout: default
class: 	Project
title:  p_output
summary: The absolute path to the project's output/target directory
---

	public String _p_output(String args[]) throws Exception {
		if (args.length != 1)
			throw new IllegalArgumentException("${output} should not have arguments");
		return getOutput().getAbsolutePath();
	}

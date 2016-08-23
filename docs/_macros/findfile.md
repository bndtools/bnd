---
layout: default
class: Project
title:	findfile ';' PATH ( ';' FILTER )
summary: A filtered list of relative paths from a directory and its subdirectories
---


	public String _findfile(String args[]) {
		File f = getFile(args[1]);
		List<String> files = new ArrayList<String>();
		tree(files, f, "", new Instruction(args[2]));
		return join(files);
	}

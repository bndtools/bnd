---
layout: default
class: Macro
title: path ( ';' FILES )+
summary: A list of file paths separated by the platform's path separator.
---


	public String _path(String args[]) {
		List<String> list = new ArrayList<String>();
		for (int i = 1; i < args.length; i++) {
			list.addAll(Processor.split(args[i]));
		}
		return Processor.join(list, File.pathSeparator);
	}


---
layout: default
class: Macro
title: fmodified ( ';' RESOURCE )+
summary:  Latest modification date of a list of resources
---

	public final static String	_fmodifiedHelp	= "${fmodified;<list of filenames>...}, return latest modification date";

	public String _fmodified(String args[]) throws Exception {
		verifyCommand(args, _fmodifiedHelp, null, 2, Integer.MAX_VALUE);

		long time = 0;
		Collection<String> names = new ArrayList<String>();
		for (int i = 1; i < args.length; i++) {
			Processor.split(args[i], names);
		}
		for (String name : names) {
			File f = new File(name);
			if (f.exists() && f.lastModified() > time)
				time = f.lastModified();
		}
		return "" + time;
	}

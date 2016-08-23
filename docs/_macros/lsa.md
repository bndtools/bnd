---
layout: default
class: Macro
title: lsa ';' DIR (';' INSTRUCTION )
summary: A list of absolute file paths from the given directory optionally filtered by a set of instructions.
---
layout: default

	/**
	 * Wildcard a directory. The lists can contain Instruction that are matched
	 * against the given directory ${lsr;<dir>;<list>(;<list>)*}
	 * ${lsa;<dir>;<list>(;<list>)*}
	 * 
	 * @author aqute
	 */

	public String _lsr(String args[]) {
		return ls(args, true);
	}

	public String _lsa(String args[]) {
		return ls(args, false);
	}

	String ls(String args[], boolean relative) {
		if (args.length < 2)
			throw new IllegalArgumentException("the ${ls} macro must at least have a directory as parameter");

		File dir = domain.getFile(args[1]);
		if (!dir.isAbsolute())
			throw new IllegalArgumentException("the ${ls} macro directory parameter is not absolute: " + dir);

		if (!dir.exists())
			throw new IllegalArgumentException("the ${ls} macro directory parameter does not exist: " + dir);

		if (!dir.isDirectory())
			throw new IllegalArgumentException(
					"the ${ls} macro directory parameter points to a file instead of a directory: " + dir);

		Collection<File> files = new ArrayList<File>(new SortedList<File>(dir.listFiles()));

		for (int i = 2; i < args.length; i++) {
			Instructions filters = new Instructions(args[i]);
			files = filters.select(files, true);
		}

		List<String> result = new ArrayList<String>();
		for (File file : files)
			result.add(relative ? file.getName() : file.getAbsolutePath());

		return Processor.join(result, ",");
	}


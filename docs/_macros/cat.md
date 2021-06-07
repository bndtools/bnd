---
layout: default
class: Macro
title: cat ';' FILEPATH
summary: The contents of a file
---



	/**
	 * Get the contents of a file.
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */

	public String _cat(String args[]) throws IOException {
		verifyCommand(args, "${cat;<in>}, get the content of a file", null, 2, 2);
		File f = domain.getFile(args[1]);
		if (f.isFile()) {
			return IO.collect(f);
		} else if (f.isDirectory()) {
			return Arrays.toString(f.list());
		} else {
			try {
				URL url = new URL(args[1]);
				return IO.collect(url, "UTF-8");
			}
			catch (MalformedURLException mfue) {
				// Ignore here
			}
			return null;
		}
	}

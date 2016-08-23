---
layout: default
class: Builder
title: -savemanifest FILE   
summary:  Write out the manifest to a separate file after it has been calculated. 
---

	/**
	 * Get the manifest and write it out separately if -savemanifest is set
	 *
	 * @param dot
	 */
	private void doSaveManifest(Jar dot) throws Exception {
		String output = getProperty(SAVEMANIFEST);
		if (output == null)
			return;

		File f = getFile(output);
		if (f.isDirectory()) {
			f = new File(f, "MANIFEST.MF");
		}
		f.delete();
		File fp = f.getParentFile();
		if (!fp.exists() && !fp.mkdirs()) {
			throw new IOException("Could not create directory " + fp);
		}
		OutputStream out = new FileOutputStream(f);
		try {
			Jar.writeManifest(dot.getManifest(), out);
		}
		finally {
			out.close();
		}
		changedFile(f);
	}

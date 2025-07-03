---
layout: default
title: -savemanifest FILE
class: Builder
summary: |
   Write out the manifest to a separate file after it has been calculated.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-savemanifest=file.txt`

- Pattern: `.*`

<!-- Manual content from: ext/savemanifest.md --><br /><br />

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

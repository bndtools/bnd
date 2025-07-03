---
layout: default
title: -outputmask  TEMPLATE ?
class: Project
summary: |
   If set, is used a template to calculate the output file. It can use any macro but the ${@bsn} and ${@version} macros refer to the current JAR being saved. The default is bsn + ".jar".
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `-outputmask=my_file.zip`

- Pattern: `.*`

<!-- Manual content from: ext/outputmask.md --><br /><br />

	/**
	 * Calculate the file for a JAR. The default name is bsn.jar, but this can
	 * be overridden with an
	 *
	 * @param jar
	 * @return
	 * @throws Exception
	 */
	public File getOutputFile(String bsn, String version) throws Exception {
		if (version == null)
			version = "0";
		Processor scoped = new Processor(this);
		try {
			scoped.setProperty("@bsn", bsn);
			scoped.setProperty("@version", version.toString());
			String path = scoped.getProperty(OUTPUTMASK, bsn + ".jar");
			return IO.getFile(getTarget(), path);
		}
		finally {
			scoped.close();
		}
	}

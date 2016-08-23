---
layout: default
class: Analyzer
title: -output  FILE
summary: Specify the output directory or file.
---

TODO how does this relate to -outputmask?

/**
	 * Calculate the output file for the given target. The strategy is:
	 * 
	 * <pre>
	 * parameter given if not null and not directory
	 * if directory, this will be the output directory
	 * based on bsn-version.jar
	 * name of the source file if exists
	 * Untitled-[n]
	 * </pre>
	 * 
	 * @param output
	 *            may be null, otherwise a file path relative to base
	 */
	public File getOutputFile(String output) {

		if (output == null)
			output = get(Constants.OUTPUT);

		File outputDir;

		if (output != null) {
			File outputFile = getFile(output);
			if (outputFile.isDirectory())
				outputDir = outputFile;
			else
				return outputFile;
		} else
			outputDir = getBase();

		Entry<String,Attrs> name = getBundleSymbolicName();
		if (name != null) {
			String bsn = name.getKey();
			String version = getBundleVersion();
			Version v = Version.parseVersion(version);
			String outputName = bsn + "-" + v.getWithoutQualifier() + Constants.DEFAULT_JAR_EXTENSION;
			return new File(outputDir, outputName);
		}

		File source = getJar().getSource();
		if (source != null) {
			String outputName = source.getName();
			return new File(outputDir, outputName);
		}

		error("Cannot establish an output name from %s, nor bsn, nor source file name, using Untitled", output);
		int n = 0;
		File f = getFile(outputDir, "Untitled");
		while (f.isFile()) {
			f = getFile(outputDir, "Untitled-" + n++);
		}
		return f;
	}
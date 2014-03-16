package org.bndtools.api;

import java.util.HashMap;
import java.util.Map;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;

public class ProjectPaths {

	/*
	 * Static part
	 */

	private static final Map<ProjectLayout, ProjectPaths> map = new HashMap<ProjectLayout, ProjectPaths>();

	static {
		/*
		 * BND
		 */

		/* This call MUST NOT access any remotes */
		Processor defaults = Workspace.getDefaults();

		String title = "bnd";
		String src = defaults.getProperty(Constants.DEFAULT_PROP_SRC_DIR);
		String bin = defaults.getProperty(Constants.DEFAULT_PROP_BIN_DIR);
		String testSrc = defaults.getProperty(Constants.DEFAULT_PROP_TESTSRC_DIR);
		String testBin = defaults.getProperty(Constants.DEFAULT_PROP_TESTBIN_DIR);
		String targetDir = defaults.getProperty(Constants.DEFAULT_PROP_TARGET_DIR);

		map.put(ProjectLayout.BND, new ProjectPaths(title, src, bin, testSrc, testBin, targetDir));
	}

	public static ProjectPaths get(ProjectLayout projectLayout) {
		return map.get(projectLayout);
	}

	/*
	 * Instance part
	 */

	private String title;
	private String src;
	private String bin;
	private String testSrc;
	private String testBin;
	private String targetDir;
	private String toolTip;

	private ProjectPaths(String title, String src, String bin, String testSrc,
			String testBin, String targetDir) {
		super();
		this.title = title;
		this.src = src;
		this.bin = bin;
		this.testSrc = testSrc;
		this.testBin = testBin;
		this.targetDir = targetDir;

		if (!validate()) {
			throw new ExceptionInInitializerError("Could not construct " + title + " ProjectPaths");
		}

		this.toolTip = constructToolTip();
	}

	private String constructToolTip() {
		return String.format(
				  "Main sources directory: %s (%s)%n"
				+ "Test sources directory: %s (%s)%n"
			    + "Target directory: %s",
				src, bin, testSrc, testBin, targetDir);
	}

	private boolean validate() {
		boolean titleValid = (title != null && (title.length() != 0));
		boolean srcValid = (src != null && (src.length() != 0));
		boolean binValid = (bin != null && (bin.length() != 0));
		boolean testSrcValid = (testSrc != null && (testSrc.length() != 0));
		boolean testBinValid = (testBin != null && (testBin.length() != 0));
		boolean targetDirValid = (targetDir != null && (targetDir.length() != 0));

		return titleValid && srcValid && binValid && testSrcValid
				&& testBinValid && targetDirValid;
	}

	public String getTitle() {
		return title;
	}

	public String getSrc() {
		return src;
	}

	public String getBin() {
		return bin;
	}

	public String getTestSrc() {
		return testSrc;
	}

	public String getTestBin() {
		return testBin;
	}

	public String getTargetDir() {
		return targetDir;
	}

	public String getToolTip() {
		return toolTip;
	}
}
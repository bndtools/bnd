package org.bndtools.api;

import java.util.List;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.lib.strings.Strings;

public class ProjectPaths {

	/*
	 * Static part
	 */

	public static final ProjectPaths DEFAULT;
	static {
		/* This call MUST NOT access any remotes */
		Processor defaults = Workspace.getDefaults();
		DEFAULT = new ProjectPaths("bnd", defaults);
	}

	/*
	 * Instance part
	 */

	private final String		title;
	private final List<String>	srcs;
	private final String		bin;
	private final List<String>	testSrcs;
	private final String		testBin;
	private final String		targetDir;
	private final String		toolTip;

	private ProjectPaths(String title, String src, String bin, String testSrc, String testBin, String targetDir) {
		this.title = title;
		this.srcs = Strings.split(src);
		this.bin = bin;
		this.testSrcs = Strings.split(testSrc);
		this.testBin = testBin;
		this.targetDir = targetDir;

		try {
			validate();
		} catch (Exception e) {
			throw new ExceptionInInitializerError("Could not construct Project Paths: " + e.getMessage());
		}

		this.toolTip = constructToolTip();
	}

	private String constructToolTip() {
		return String.format(
			"Main sources directory: %s (%s)%n" + "Test sources directory: %s (%s)%n" + "Target directory: %s", srcs,
			bin, testSrcs, testBin, targetDir);
	}

	public ProjectPaths(String title, Processor workspace) {
		this(title, //
			workspace.getProperty(Constants.DEFAULT_PROP_SRC_DIR), //
			workspace.getProperty(Constants.DEFAULT_PROP_BIN_DIR), //
			workspace.getProperty(Constants.DEFAULT_PROP_TESTSRC_DIR), //
			workspace.getProperty(Constants.DEFAULT_PROP_TESTBIN_DIR), //
			workspace.getProperty(Constants.DEFAULT_PROP_TARGET_DIR));
	}

	private void validate() throws Exception {
		if (title == null || title.length() == 0)
			throw new Exception("Invalid title");

		if (srcs.isEmpty())
			throw new IllegalArgumentException("No source dir specified macro is ${src}");

		srcs.forEach(src -> {
			if (src == null || src.length() == 0)
				throw new IllegalArgumentException("Invalid source dir " + src);
		});

		if (bin == null || bin.length() == 0)
			throw new Exception("Invalid bin dir");

		testSrcs.forEach(testSrc -> {
			if (testSrc == null || testSrc.length() == 0)
				throw new IllegalArgumentException("Invalid test source dir " + testSrc);
		});
		if (testBin == null || testBin.length() == 0)
			throw new Exception("Invalid test bin dir");
		if (targetDir == null || targetDir.length() == 0)
			throw new Exception("Invalid target dir");
	}

	public String getTitle() {
		return title;
	}

	@Deprecated
	public String getSrc() {
		return srcs.get(0);
	}

	public List<String> getSrcs() {
		return srcs;
	}

	public String getBin() {
		return bin;
	}

	@Deprecated
	public String getTestSrc() {
		return testSrcs.isEmpty() ? null : testSrcs.get(0);
	}

	public List<String> getTestSrcs() {
		return testSrcs;
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

package org.bndtools.api;

import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;

public class ProjectPaths {
	public static final String PATH_SRC;
	public static final String PATH_SRC_BIN;

	public static final String PATH_TEST_SRC;
	public static final String PATH_TEST_BIN;

	static {
		/* This call MUST not access any remotes */
		Processor defaults = Workspace.getDefaults();

		PATH_SRC = defaults.getProperty(Constants.DEFAULT_PROP_SRC_DIR, "src");
		PATH_SRC_BIN = defaults.getProperty(Constants.DEFAULT_PROP_BIN_DIR, "bin");
		PATH_TEST_SRC = defaults
				.getProperty(Constants.DEFAULT_PROP_TESTSRC_DIR, "test");
		PATH_TEST_BIN = defaults
				.getProperty(Constants.DEFAULT_PROP_TESTBIN_DIR, "bin_test");
	}
}

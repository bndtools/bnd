---
layout: default
class: Project
title: -runpath REPO-ENTRY ( ',' REPO-ENTRY ) 
summary:  Additional JARs for the remote VM path, should include the framework.
---


	/**
	 * Method to verify that the paths are correct, ie no missing dependencies
	 *
	 * @param test
	 *            for test cases, also adds -testpath
	 * @throws Exception
	 */
	public void verifyDependencies(boolean test) throws Exception {
		verifyDependencies(RUNBUNDLES, getRunbundles());
		verifyDependencies(RUNPATH, getRunpath());
		if (test)
			verifyDependencies(TESTPATH, getTestpath());
		verifyDependencies(BUILDPATH, getBuildpath());
	}

	
						doPath(buildpath, dependencies, parseBuildpath(), bootclasspath, false, BUILDPATH);
					doPath(testpath, dependencies, parseTestpath(), bootclasspath, false, TESTPATH);
					if (!delayRunDependencies) {
						doPath(runfw, dependencies, parseRunFw(), null, false, RUNFW);
						doPath(runpath, dependencies, parseRunpath(), null, false, RUNPATH);
						doPath(runbundles, dependencies, parseRunbundles(), null, true, RUNBUNDLES);
					}
	
	
	
		private List<Container> parseRunpath() throws Exception {
		return getBundles(Strategy.HIGHEST, mergeProperties(Constants.RUNPATH), Constants.RUNPATH);
	}

		public Collection<Container> getRunpath() throws Exception {
		prepare();
		justInTime(runpath, parseRunpath(), false, RUNPATH);
		return runpath;
	}

	
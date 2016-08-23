---
layout: default
class: Project
title: -testpath REPO-SPEC ( ',' REPO-SPEC ) 
summary: The specified JARs from a repository are added to the remote JVM's classpath if the JVM is started in test mode in addition to the -runpath JARs.  
---

	public Collection<Container> getTestpath() throws Exception {
		prepare();
		justInTime(testpath, parseTestpath(), false, TESTPATH);
		return testpath;
	}

	private List<Container> parseTestpath() throws Exception {
		return getBundles(Strategy.HIGHEST, mergeProperties(Constants.TESTPATH), Constants.TESTPATH);
	}

					doPath(buildpath, dependencies, parseBuildpath(), bootclasspath, false, BUILDPATH);
					doPath(testpath, dependencies, parseTestpath(), bootclasspath, false, TESTPATH);
					if (!delayRunDependencies) {
						doPath(runfw, dependencies, parseRunFw(), null, false, RUNFW);
						doPath(runpath, dependencies, parseRunpath(), null, false, RUNPATH);
						doPath(runbundles, dependencies, parseRunbundles(), null, true, RUNBUNDLES);
					}
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

						public void prepare() throws Exception {
		Pattern tests = Pattern.compile(project.getProperty(Constants.TESTSOURCES, "(.*).java"));

		String testDirName = project.getProperty("testsrc", "test");
		File testSrc = project.getFile(testDirName).getAbsoluteFile();
		if (!testSrc.isDirectory()) {
			project.trace("no test src directory");
			return;
		}

		if (!traverse(fqns, testSrc, "", tests)) {
			project.trace("no test files found in %s", testSrc);
			return;
		}

		timeout = Processor.getDuration(project.getProperty(Constants.RUNTIMEOUT), 0);
//		trace = Processor.isTrue(project.getProperty(Constants.RUNTRACE));
		cp = new Classpath(project, "junit");
		addClasspath(project.getTestpath());
		addClasspath(project.getBuildpath());
	}


	public void compile(boolean test) throws Exception {

		Command javac = getCommonJavac(false);
		javac.add("-d", getOutput().getAbsolutePath());

		StringBuilder buildpath = new StringBuilder();

		String buildpathDel = "";
		Collection<Container> bp = Container.flatten(getBuildpath());
		trace("buildpath %s", getBuildpath());
		for (Container c : bp) {
			buildpath.append(buildpathDel).append(c.getFile().getAbsolutePath());
			buildpathDel = File.pathSeparator;
		}

		if (buildpath.length() != 0) {
			javac.add("-classpath", buildpath.toString());
		}

		List<File> sp = new ArrayList<File>(getAllsourcepath());
		StringBuilder sourcepath = new StringBuilder();
		String sourcepathDel = "";

		for (File sourceDir : sp) {
			sourcepath.append(sourcepathDel).append(sourceDir.getAbsolutePath());
			sourcepathDel = File.pathSeparator;
		}

		javac.add("-sourcepath", sourcepath.toString());

		Glob javaFiles = new Glob("*.java");
		List<File> files = javaFiles.getFiles(getSrc(), true, false);

		for (File file : files) {
			javac.add(file.getAbsolutePath());
		}

		if (files.isEmpty()) {
			trace("Not compiled, no source files");
		} else
			compile(javac, "src");

		if (test) {
			javac = getCommonJavac(true);
			javac.add("-d", getTestOutput().getAbsolutePath());

			Collection<Container> tp = Container.flatten(getTestpath());
			for (Container c : tp) {
				buildpath.append(buildpathDel).append(c.getFile().getAbsolutePath());
				buildpathDel = File.pathSeparator;
			}
			if (buildpath.length() != 0) {
				javac.add("-classpath", buildpath.toString());
			}

			sourcepath.append(sourcepathDel).append(getTestSrc().getAbsolutePath());
			javac.add("-sourcepath", sourcepath.toString());

			javaFiles.getFiles(getTestSrc(), files, true, false);
			for (File file : files) {
				javac.add(file.getAbsolutePath());
			}
			if (files.isEmpty()) {
				trace("Not compiled for test, no test src files");
			} else
				compile(javac, "test");
		}
	}
					
					
					
						public ProjectTester getProjectTester() throws Exception {
		return getHandler(ProjectTester.class, getTestpath(), TESTER_PLUGIN, "biz.aQute.junit");
	}

					
					
						public ProjectTester(Project project) throws Exception {
		this.project = project;
		launcher = project.getProjectLauncher();
		launcher.addRunVM("-ea");
		testbundles = project.getTestpath();
		continuous = project.is(Constants.TESTCONTINUOUS);
		
		for (Container c : testbundles) {
			launcher.addClasspath(c);
		}
		reportDir = new File(project.getTarget(), project.getProperty("test-reports", "test-reports"));
	}
					
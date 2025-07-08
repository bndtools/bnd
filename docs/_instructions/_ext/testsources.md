---
layout: default
class: Project
title: -testsources REGEX ( ',' REGEX )* 
summary:  Specification to find JUnit test cases by traversing the test src directory and looking for java classes. The default is (.*).java.
---

TODO this is not working yet

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
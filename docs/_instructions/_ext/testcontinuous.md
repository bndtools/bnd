---
layout: default
class: Test
title: -testcontinuous BOOLEAN 
summary: Do not exit after running the test suites but keep watching the bundles and rerun the test cases if the bundle is updated.
---

	public void test(List<String> tests) throws Exception {

		String testcases = getProperties().getProperty(Constants.TESTCASES);
		if (testcases == null) {
			warning("No %s set", Constants.TESTCASES);
			return;
		}
		clear();

		ProjectTester tester = getProjectTester();
		if ( tests != null) {
			trace("Adding tests %s", tests);
			for ( String test : tests) {
				tester.addTest(test);
			}
		}
		tester.setContinuous(isTrue(getProperty(Constants.TESTCONTINUOUS)));
		tester.prepare();

		if (!isOk()) {
			return;
		}
		int errors = tester.test();
		if (errors == 0) {
			System.err.println("No Errors");
		} else {
			if (errors > 0) {
				System.err.println(errors + " Error(s)");

			} else
				System.err.println("Error " + errors);
		}
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

		@Description("Test a project according to an OSGi test")
	@Arguments(arg = {
		"testclass[:method]..."
	})
	interface testOptions extends Options {
		@Description("Path to another project than the current project")
		String project();

		@Description("Verify all the dependencies before launching (runpath, runbundles, testpath)")
		boolean verify();

		@Description("Launch the test even if this bundle does not contain " + Constants.TESTCASES)
		boolean force();

		@Description("Set the -testcontinuous flag")
		boolean continuous();

		@Description("Set the -runtrace flag")
		boolean trace();
	}

	@Description("Test a project according to an OSGi test")
	public void _test(testOptions opts) throws Exception {
		Project project = getProject(opts.project());
		if (project == null) {
			messages.NoProject();
			return;
		}

		// if (!verifyDependencies(project, opts.verify(), true))
		// return;
		//
		List<String> testNames = opts._();
		if (!testNames.isEmpty())
			project.setProperty(TESTCASES, "");

		if (project.is(NOJUNITOSGI) && !opts.force()) {
			warning("%s is set to true on this bundle. Use -f/--force to try this test anyway", NOJUNITOSGI);
			return;
		}

		if (project.getProperty(TESTCASES) == null)
			if (opts.force())
				project.setProperty(TESTCASES, "");
			else {
				warning("No %s set on this bundle. Use -f/--force to try this test anyway (this works if another bundle provides the testcases)",
						TESTCASES);
				return;
			}

		if (opts.continuous())
			project.setProperty(TESTCONTINUOUS, "true");

		if (opts.trace() || isTrace())
			project.setProperty(RUNTRACE, "true");

		project.test(testNames);
		getInfo(project);
	}
	
		@Override
	public boolean prepare() throws Exception {
		if (!prepared) {
			prepared = true;
			super.prepare();
			ProjectLauncher launcher = getProjectLauncher();
			if (port > 0) {
				launcher.getRunProperties().put(TESTER_PORT, "" + port);
				if (host != null)
					launcher.getRunProperties().put(TESTER_HOST, "" + host);

			}
			launcher.getRunProperties().put(TESTER_UNRESOLVED, project.getProperty(Constants.TESTUNRESOLVED, "true"));

			launcher.getRunProperties().put(TESTER_DIR, getReportDir().getAbsolutePath());
			launcher.getRunProperties().put(TESTER_CONTINUOUS, "" + getContinuous());
			if (Processor.isTrue(project.getProperty(Constants.RUNTRACE)))
				launcher.getRunProperties().put(TESTER_TRACE, "true");

			try {
				// use reflection to avoid NoSuchMethodError due to change in
				// API
				File cwd = (File) getClass().getMethod("getCwd").invoke(this);
				if (cwd != null)
					launcher.setCwd(cwd);
			}
			catch (NoSuchMethodException e) {
				// ignore
			}

			Collection<String> testnames = getTests();
			if (testnames.size() > 0) {
				launcher.getRunProperties().put(TESTER_NAMES, Processor.join(testnames));
			}
			// This is only necessary because we might be picked
			// as default and that implies we're not on the -testpath
			launcher.addDefault(Constants.DEFAULT_TESTER_BSN);
			launcher.prepare();
		}
		return true;
	}
	
	
		public void run() {
		
		continuous = Boolean.valueOf(context.getProperty(TESTER_CONTINUOUS));
		trace = context.getProperty(TESTER_TRACE) != null;
		
		if (thread == null)
			trace("running in main thread");
		
		// We can be started on our own thread or from the main code
		thread = Thread.currentThread();
		

		String testcases = context.getProperty(TESTER_NAMES);
		trace("test cases %s", testcases);
		if (context.getProperty(TESTER_PORT) != null) {
			port = Integer.parseInt(context.getProperty(TESTER_PORT));
			try {
				trace("using port %s", port);
				jUnitEclipseReport = new JUnitEclipseReport(port);
			}
			catch (Exception e) {
				System.err.println("Cannot create link Eclipse JUnit on port " + port);
				System.exit(254);
			}
		}


		//
		// Jenkins does not detect test failures unless reported
		// by JUnit XML output. If we have an unresolved failure
		// we timeout. The following will test if there are any
		// unresolveds and report this as a JUnit failure. It can 
		// be disabled with -testunresolved=false
		//
		
		String unresolved = context.getProperty(TESTER_UNRESOLVED);
		trace("run unresolved %s", unresolved);
		
		if (unresolved == null || unresolved.equalsIgnoreCase("true")) {
			//
			// Check if there are any unresolved bundles.
			// If yes, we run a test case to get a proper JUnit report
			//
			for ( Bundle b : context.getBundles()) {
				if ( b.getState() == Bundle.INSTALLED) {
					//
					// Now do it again but as a test case
					// so we get a proper JUnit report
					//
					int err = test(context.getBundle(), "aQute.junit.UnresolvedTester", null);
					if (err != 0)
						System.exit(err);
				}
			}
		}

		if (testcases == null) {
//			if ( !continuous) {
//				System.err.println("\nThe -testcontinuous property must be set if invoked without arguments\n");
//				System.exit(255);
//			}
				
			trace("automatic testing of all bundles with " + aQute.bnd.osgi.Constants.TESTCASES + " header");
			try {
				automatic();
			}
			catch (IOException e) {
				// ignore
			}
		} else {
			trace("receivednames of classes to test %s", testcases);
			try {
				int errors = test(null, testcases, null);
				System.exit(errors);
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(254);
			}
		}
	}
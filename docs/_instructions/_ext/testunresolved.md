---
layout: default
class: Project
title: -testunresolved BOOLEAN 
summary:  Will execute a JUnit testcase ahead of any other test case that will abort if there are any unresolved bundles. 
---

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
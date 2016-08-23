---
layout: default
class: Launcher
title: -runtrace BOOLEAN 
summary: Trace the launched process in detail
---

	public void run() throws Exception {
		ProjectLauncher pl = getProjectLauncher();
		pl.setTrace(isTrace() || isTrue(getProperty(RUNTRACE)));
		pl.launch();
	}

	public void runLocal() throws Exception {
		ProjectLauncher pl = getProjectLauncher();
		pl.setTrace(isTrace() || isTrue(getProperty(RUNTRACE)));
		pl.start(null);
	}

	
		/**
	 * Collect all the aspect from the project and set the local fields from
	 * them. Should be called
	 * 
	 * @throws Exception
	 */
	protected void updateFromProject() throws Exception {
		// pkr: could not use this because this is killing the runtests.
		// project.refresh();
		runbundles.clear();
		Collection<Container> run = project.getRunbundles();

		for (Container container : run) {
			File file = container.getFile();
			if (file != null && (file.isFile() || file.isDirectory())) {
				runbundles.add(file.getAbsolutePath());
			} else {
				error("Bundle file \"%s\" does not exist, given error is %s", file, container.getError());
			}
		}

		if (project.getRunBuilds()) {
			File[] builds = project.build();
			if (builds != null)
				for (File file : builds)
					runbundles.add(file.getAbsolutePath());
		}

		Collection<Container> runpath = project.getRunpath();
		runsystempackages = new Parameters( project.mergeProperties(Constants.RUNSYSTEMPACKAGES));
		runsystemcapabilities = project.mergeProperties(Constants.RUNSYSTEMCAPABILITIES);
		framework = getRunframework(project.getProperty(Constants.RUNFRAMEWORK));

		timeout = Processor.getDuration(project.getProperty(Constants.RUNTIMEOUT), 0);
		trace = Processor.isTrue(project.getProperty(Constants.RUNTRACE));

		runpath.addAll(project.getRunFw());

		for (Container c : runpath) {
			addClasspath(c);
		}

		runvm.addAll(project.getRunVM());
		runprogramargs.addAll(project.getRunProgramArgs());
		runproperties = project.getRunProperties();

		storageDir = project.getRunStorage();
		if (storageDir == null) {
			storageDir = new File(project.getTarget(), "fw");
		}
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
	
	
		@SuppressWarnings("deprecation")
	public int activate() throws Exception {
		active.set(true);
		Policy.setPolicy(new AllPolicy());

		systemBundle = createFramework();
		if (systemBundle == null)
			return LauncherConstants.ERROR;

		doTimeoutHandler();

		doSecurity();

		// Initialize this framework so it becomes STARTING
		systemBundle.start();
		trace("system bundle started ok");

		BundleContext systemContext = systemBundle.getBundleContext();
		ServiceReference<PackageAdmin> ref = systemContext.getServiceReference(PackageAdmin.class);
		if (ref != null) {
			padmin = systemContext.getService(ref);
		} else
			trace("could not get package admin");

		systemContext.addServiceListener(this, "(&(|(objectclass=" + Runnable.class.getName() + ")(objectclass="
				+ Callable.class.getName() + "))(main.thread=true))");

		// Start embedded activators
		trace("start embedded activators");
		if (parms.activators != null) {
			ClassLoader loader = getClass().getClassLoader();
			for (Object token : parms.activators) {
				try {
					Class< ? > clazz = loader.loadClass((String) token);
					BundleActivator activator = (BundleActivator) clazz.newInstance();
					embedded.add(activator);
					trace("adding activator %s", activator);
				}
				catch (Exception e) {
					throw new IllegalArgumentException("Embedded Bundle Activator incorrect: " + token + ", " + e);
				}
			}
		}

		update(System.currentTimeMillis() + 100);

		if (parms.trace) {
			report(out);
		}

		int result = LauncherConstants.OK;
		for (BundleActivator activator : embedded)
			try {
				trace("starting activator %s", activator);
				activator.start(systemContext);
			}
			catch (Exception e) {
				error("Starting activator %s : %s", activator, e);
				result = LauncherConstants.ERROR;
			}

		return result;
	}
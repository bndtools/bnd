---
layout: default
class: Project
title: -runtimeout DURATION
summary:  
---

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


	public int launch() throws Exception {
		java = new Command();
		java.add(project.getProperty("java", "java"));

		java.add("-cp");
		java.add(cp.toString());
		java.addAll(project.getRunVM());
		java.add(getMainTypeName());
		java.addAll(fqns);
		if (timeout != 0)
			java.setTimeout(timeout + 1000, TimeUnit.MILLISECONDS);

		project.trace("cmd line %s", java);
		try {
			int result = java.execute(System.in, System.err, System.err);
			if (result == Integer.MIN_VALUE)
				return TIMEDOUT;
			reportResult(result);
			return result;
		}
		finally {
			cleanup();
		}

	}

	public static long getDuration(String tm, long dflt) {
		if (tm == null)
			return dflt;

		tm = tm.toUpperCase();
		TimeUnit unit = TimeUnit.MILLISECONDS;
		Matcher m = Pattern
				.compile("\\s*(\\d+)\\s*(NANOSECONDS|MICROSECONDS|MILLISECONDS|SECONDS|MINUTES|HOURS|DAYS)?").matcher(
						tm);
		if (m.matches()) {
			long duration = Long.parseLong(tm);
			String u = m.group(2);
			if (u != null)
				unit = TimeUnit.valueOf(u);
			duration = TimeUnit.MILLISECONDS.convert(duration, unit);
			return duration;
		}
		return dflt;
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
	
	
		public void deactivate() throws Exception {
		if (active.getAndSet(false)) {
			systemBundle.stop();
			systemBundle.waitForStop(parms.timeout);

			ThreadGroup group = Thread.currentThread().getThreadGroup();
			Thread[] threads = new Thread[group.activeCount() + 100];
			group.enumerate(threads);
			{
				for (Thread t : threads) {
					if (t != null && !t.isDaemon() && t.isAlive()) {
						trace("alive thread " + t);
					}
				}
			}
		} else
			errorAndExit("Huh? Already deactivated.");
	}

	
		private void doTimeoutHandler() {
		// Ensure we properly close in a separate thread so that
		// we can leverage the main thread, which is required for macs
		Thread wait = new Thread("FrameworkWaiter") {
			@Override
			public void run() {
				try {
					FrameworkEvent result = systemBundle.waitForStop(parms.timeout);
					if (!active.get()) {
						trace("ignoring timeout handler because framework is already no longer active, shutdown is orderly handled");
						return;
					}

					trace("framework event " + result + " " + result.getType());
					switch (result.getType()) {
						case FrameworkEvent.STOPPED :
							trace("framework event stopped");
							System.exit(LauncherConstants.STOPPED);
							break;

						case FrameworkEvent.WAIT_TIMEDOUT :
							trace("framework event timedout");
							System.exit(LauncherConstants.TIMEDOUT);
							break;

						case FrameworkEvent.ERROR :
							System.exit(ERROR);
							break;

						case FrameworkEvent.WARNING :
							System.exit(WARNING);
							break;

						case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED :
						case FrameworkEvent.STOPPED_UPDATE :
							trace("framework event update");
							System.exit(UPDATE_NEEDED);
							break;
					}
				}
				catch (InterruptedException e) {
					System.exit(CANCELED);
				}
			}
		};
		wait.start();
	}
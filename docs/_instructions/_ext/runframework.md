---
layout: default
class: Launcher
title: -runframework ( 'none' | 'services' | ANY )?
summary: Sets the type of framework to run. If 'none', an internal dummy framework is used. Otherwise the Java META-INF/services model is used for the FrameworkFactory interface name.
---

Note: confusing name due to history, -runfw specifies the framework

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
	
		private int getRunframework(String property) {
		if (Constants.RUNFRAMEWORK_NONE.equalsIgnoreCase(property))
			return NONE;
		else if (Constants.RUNFRAMEWORK_SERVICES.equalsIgnoreCase(property))
			return SERVICES;

		return SERVICES;
	}


	private Framework createFramework() throws Exception {
		Properties p = new Properties();
		p.putAll(properties);
		File workingdir = null;
		if (parms.storageDir != null)
			workingdir = parms.storageDir;
		else if (parms.keep && parms.name != null) {
			workingdir = new File(bnd, parms.name);
		}

		if (workingdir == null) {
			workingdir = File.createTempFile("osgi.", ".fw");
			final File wd = workingdir;
			Runtime.getRuntime().addShutdownHook(new Thread("launcher::delete temp working dir") {
				public void run() {
					deleteFiles(wd);
				}
			});
		}

		trace("using working dir: %s", workingdir);

		if (!parms.keep && workingdir.exists()) {
			trace("deleting working dir %s because not kept", workingdir);
			delete(workingdir);
			p.setProperty(Constants.FRAMEWORK_STORAGE_CLEAN, "true");
		}

		if (!workingdir.exists() && !workingdir.mkdirs()) {
			throw new IOException("Could not create directory " + workingdir);
		}
		if (!workingdir.isDirectory())
			throw new IllegalArgumentException("Cannot create a working dir: " + workingdir);

		p.setProperty(Constants.FRAMEWORK_STORAGE, workingdir.getAbsolutePath());

		if (parms.systemPackages != null) {
			p.setProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, parms.systemPackages);
			trace("system packages used: %s", parms.systemPackages);
		}

		if (parms.systemCapabilities != null) {
			p.setProperty(FRAMEWORK_SYSTEM_CAPABILITIES_EXTRA, parms.systemCapabilities);
			trace("system capabilities used: %s", parms.systemCapabilities);
		}

		Framework systemBundle;

		if (parms.services) {
			trace("using META-INF/services");
			// 3) framework = null, lookup in META-INF/services

			ClassLoader loader = getClass().getClassLoader();

			// 3) Lookup in META-INF/services
			List<String> implementations = getMetaInfServices(loader, FrameworkFactory.class.getName());

			if (implementations.size() == 0)
				error("Found no fw implementation");
			if (implementations.size() > 1)
				error("Found more than one framework implementations: %s", implementations);

			String implementation = implementations.get(0);

			Class< ? > clazz = loader.loadClass(implementation);
			FrameworkFactory factory = (FrameworkFactory) clazz.newInstance();
			trace("Framework factory %s", factory);
			@SuppressWarnings("unchecked")
			Map<String,String> configuration = (Map) p;
			systemBundle = factory.newFramework(configuration);
			trace("framework instance %s", systemBundle);
		} else {
			trace("using embedded mini framework because we were told not to use META-INF/services");
			// we have to use our own dummy framework
			systemBundle = new MiniFramework(p);
		}
		systemBundle.init();

		try {
			systemBundle.getBundleContext().addFrameworkListener(new FrameworkListener() {

				public void frameworkEvent(FrameworkEvent event) {
					switch (event.getType()) {
						case FrameworkEvent.ERROR :
						case FrameworkEvent.WAIT_TIMEDOUT :
							trace("Refresh will end due to error or timeout %s", event.toString());

						case FrameworkEvent.PACKAGES_REFRESHED :
							inrefresh = false;
							trace("refresh ended");
							break;
					}
				}
			});
		}
		catch (Exception e) {
			trace("could not register a framework listener: %s", e);
		}
		trace("inited system bundle %s", systemBundle);
		return systemBundle;
	}

	
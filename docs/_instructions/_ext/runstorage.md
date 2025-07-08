---
layout: default
class: Project
title: -runstorage FILE
summary:  Define the directory to use for the framework's work area.
---

		storageDir = project.getRunStorage();
		if (storageDir == null) {
			storageDir = new File(project.getTarget(), "fw");
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
		
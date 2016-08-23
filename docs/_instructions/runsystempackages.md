---
layout: default
class: Launcher
title: -runsystempackages* PARAMETERS 
summary:  Define extra system packages (packages exported from the remote VM -runpath).
---

	public void addSystemPackage(String packageName) {
		parms.systemPackages = concat(parms.systemPackages, packageName);
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
	
	
		/**
	 * @return
	 * @throws Exception
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private LauncherConstants getConstants(Collection<String> runbundles, boolean exported) throws Exception, FileNotFoundException,
			IOException {
		project.trace("preparing the aQute launcher plugin");

		LauncherConstants lc = new LauncherConstants();
		lc.noreferences = Processor.isTrue(project.getProperty(Constants.RUNNOREFERENCES));
		lc.runProperties = getRunProperties();
		lc.storageDir = getStorageDir();
		lc.keep = isKeep();
		lc.runbundles.addAll(runbundles);
		lc.trace = getTrace();
		lc.timeout = getTimeout();
		lc.services = super.getRunFramework() == SERVICES ? true : false;
		lc.activators.addAll(getActivators());
		lc.name = getProject().getName();
		
		if(!exported && !getNotificationListeners().isEmpty()) {
			if(listenerComms == null) {
				listenerComms = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0));
				new Thread(new Runnable() {
					public void run() {
						DatagramSocket socket = listenerComms;
						DatagramPacket packet = new DatagramPacket(new byte[65536], 65536);
						while(!socket.isClosed()) {
							try {
								socket.receive(packet);
								DataInputStream dai = new DataInputStream(new ByteArrayInputStream(
										packet.getData(), packet.getOffset(), packet.getLength()));
								NotificationType type = NotificationType.values()[dai.readInt()];
								String message = dai.readUTF();
								for(NotificationListener listener : getNotificationListeners()) {
									listener.notify(type, message);
								}
							}
							catch (IOException e) {
							}
						}
					}
				}).start();
			}
			lc.notificationPort = listenerComms.getLocalPort();
		} else {
			lc.notificationPort = -1;
		}

		try {
			// If the workspace contains a newer version of biz.aQute.launcher
			// than the version of bnd(tools) used
			// then this could throw NoSuchMethodError. For now just ignore it.
			Map<String, ? extends Map<String,String>> systemPkgs = getSystemPackages();
			if (systemPkgs != null && !systemPkgs.isEmpty())
				lc.systemPackages = Processor.printClauses(systemPkgs);
		}
		catch (Throwable e) {}

		try {
			// If the workspace contains a newer version of biz.aQute.launcher
			// than the version of bnd(tools) used
			// then this could throw NoSuchMethodError. For now just ignore it.
			String systemCaps = getSystemCapabilities();
			if (systemCaps != null) {
				systemCaps = systemCaps.trim();
				if (systemCaps.length() > 0)
					lc.systemCapabilities = systemCaps;
			}
		}
		catch (Throwable e) {}
		return lc;

	}
	
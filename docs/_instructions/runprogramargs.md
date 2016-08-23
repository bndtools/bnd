---
layout: default
class: Project
title: -runprogramargs  
summary:  Additional arguments for the program invokation.
---

	public Collection<String> getRunProgramArgs() {
		Parameters hdr = getParameters(RUNPROGRAMARGS);
		return hdr.keySet();
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
	
	
		public int launch() throws Exception {
		prepare();
		java = new Command();
		
		
		//
		// Handle the environment
		//
		
		Map<String,String> env = getRunEnv();
		for ( Map.Entry<String,String> e:env.entrySet()) {
			java.var(e.getKey(), e.getValue());
		}
		
		java.add(project.getProperty("java", "java"));
		String javaagent = project.getProperty(Constants.JAVAAGENT);
		if (Processor.isTrue(javaagent)) {
			for (String agent : agents) {
				java.add("-javaagent:" + agent);
			}
		}

		String jdb = getRunJdb();
		if (jdb != null) {
			int port = 1044;
			try {
				port = Integer.parseInt(project.getProperty(Constants.RUNJDB));
			}
			catch (Exception e) {
				// ok, value can also be ok, or on, or true
			}
			String suspend = port > 0 ? "y" : "n";

			java.add("-Xrunjdwp:server=y,transport=dt_socket,address=" + Math.abs(port) + ",suspend=" + suspend);
		}
		
		java.add("-cp");
		java.add(Processor.join(getClasspath(), File.pathSeparator));
		java.addAll(getRunVM());
		java.add(getMainTypeName());
		java.addAll(getRunProgramArgs());
		if (timeout != 0)
			java.setTimeout(timeout + 1000, TimeUnit.MILLISECONDS);

		File cwd = getCwd();
		if (cwd != null)
			java.setCwd(cwd);

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
			listeners.clear();
		}
	}
	
	
		public int start(ClassLoader parent) throws Exception {

		prepare();
		
		//
		// Intermediate class loader to not load osgi framework packages 
		// from bnd's loader. Unfortunately, bnd uses some osgi classes
		// itself that would unnecessarily constrain the framework.
		//
		
		ClassLoader fcl = new ClassLoader(parent) {
			protected Class< ? > loadClass(String name, boolean resolve) throws ClassNotFoundException {
				if ( IGNORE.matcher(name).matches())
					throw new ClassNotFoundException();

				return super.loadClass(name, resolve);
			}
		};
		
		//
		// Load the class that would have gone to the class path
		// i.e. the framework etc.
		//
		
		List<URL> cp = new ArrayList<URL>();
		for (String path : getClasspath()) {
			cp.add(new File(path).toURI().toURL());
		}
		URLClassLoader cl = new URLClassLoader(cp.toArray(new URL[cp.size()]), fcl);

		
		String[] args = getRunProgramArgs().toArray(new String[0]);
		
		Class< ? > main = cl.loadClass(getMainTypeName());
		return invoke(main, args);
	}
	
	
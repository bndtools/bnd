---
layout: default
class: Project
title: -runvm KEYS 
summary:  Additional arguments for the VM invocation. Arguments are added as-is.
---


	public Collection<String> getRunVM() {
		Parameters hdr = getParameters(RUNVM);
		return hdr.keySet();
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
	
	
		protected void report(Map<String,Object> table, boolean isProject) throws Exception {
		if (isProject) {
			table.put("Target", getTarget());
			table.put("Source", getSrc());
			table.put("Output", getOutput());
			File[] buildFiles = getBuildFiles();
			if (buildFiles != null)
				table.put("BuildFiles", Arrays.asList(buildFiles));
			table.put("Classpath", getClasspath());
			table.put("Actions", getActions());
			table.put("AllSourcePath", getAllsourcepath());
			table.put("BootClassPath", getBootclasspath());
			table.put("BuildPath", getBuildpath());
			table.put("Deliverables", getDeliverables());
			table.put("DependsOn", getDependson());
			table.put("SourcePath", getSourcePath());
		}
		table.put("RunPath", getRunpath());
		table.put("TestPath", getTestpath());
		table.put("RunProgramArgs", getRunProgramArgs());
		table.put("RunVM", getRunVM());
		table.put("Runfw", getRunFw());
		table.put("Runbundles", getRunbundles());
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
	
	
		public ProjectLauncherImpl(Project project) throws Exception {
		super(project);
		project.trace("created a aQute launcher plugin");
		this.project = project;
		propertiesFile = File.createTempFile("launch", ".properties", project.getTarget());
		project.trace(MessageFormat.format("launcher plugin using temp launch file {0}",
				propertiesFile.getAbsolutePath()));
		addRunVM("-D" + LauncherConstants.LAUNCHER_PROPERTIES + "=\"" + propertiesFile.getAbsolutePath() + "\"");

		if (project.getRunProperties().get("noframework") != null) {
			setRunFramework(NONE);
			project.warning("The noframework property in -runproperties is replaced by a project setting: '-runframework: none'");
		}

		super.addDefault(Constants.DEFAULT_LAUNCHER_BSN);
	}
	

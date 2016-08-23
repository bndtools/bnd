---
layout: default
class: Launcher
title: -runfw REPO-ENTRY
summary: Specify the framework JAR's entry in a repository.
---

	/**
	 * Return the run framework
	 *
	 * @throws Exception
	 */
	public Collection<Container> getRunFw() throws Exception {
		prepare();
		justInTime(runfw, parseRunFw(), false, RUNFW);
		return runfw;
	}

	
		private List<Container> parseRunFw() throws Exception {
		return getBundles(Strategy.HIGHEST, getProperty(Constants.RUNFW), Constants.RUNFW);
	}

	
						doPath(buildpath, dependencies, parseBuildpath(), bootclasspath, false, BUILDPATH);
					doPath(testpath, dependencies, parseTestpath(), bootclasspath, false, TESTPATH);
					if (!delayRunDependencies) {
						doPath(runfw, dependencies, parseRunFw(), null, false, RUNFW);
						doPath(runpath, dependencies, parseRunpath(), null, false, RUNPATH);
						doPath(runbundles, dependencies, parseRunbundles(), null, true, RUNBUNDLES);
					}

	
	
		private Requirement getFrameworkRequirement() {
		String header = properties.getProperty(Constants.RUNFW);
		if (header == null)
			return null;

		// Get the identity and version of the requested JAR
		Parameters params = new Parameters(header);
		if (params.size() > 1)
			throw new IllegalArgumentException("Cannot specify more than one OSGi Framework.");
		Entry<String,Attrs> entry = params.entrySet().iterator().next();
		String identity = entry.getKey();

		String versionStr = entry.getValue().getVersion();

		// Construct a filter & requirement to find matches
		Filter filter = new SimpleFilter(IdentityNamespace.IDENTITY_NAMESPACE, identity);
		if (versionStr != null)
			filter = new AndFilter().addChild(filter).addChild(new LiteralFilter(Filters.fromVersionRange(versionStr)));
		Requirement frameworkReq = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE).addDirective(
				Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter.toString()).buildSyntheticRequirement();
		return frameworkReq;
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

	
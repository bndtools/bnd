---
layout: default
class: Project
title: -runbuilds BOOLEAN
summary:  Defines if this should add the bundles build by this project to the -runbundles. For a bndrun file this is default false, for a bnd file this is default true.
---

	public boolean getRunBuilds() {
		boolean result;
		String runBuildsStr = getProperty(Constants.RUNBUILDS);
		if (runBuildsStr == null)
			result = !getPropertiesFile().getName().toLowerCase().endsWith(Constants.DEFAULT_BNDRUN_EXTENSION);
		else
			result = Boolean.parseBoolean(runBuildsStr);
		return result;
	}

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
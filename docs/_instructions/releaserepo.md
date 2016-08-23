---
layout: default
class: Project
title: -releaserepo NAME
summary: Define the name of the repository to use for a release. 
---

	public void release(boolean test) throws Exception {
		String name = getProperty(Constants.RELEASEREPO);
		release(name, test);
	}
		public File release(String jarName, InputStream jarStream) throws Exception {
		String name = getProperty(Constants.RELEASEREPO);
		return release(name, jarName, jarStream);
	}

	public URI releaseURI(String jarName, InputStream jarStream) throws Exception {
		String name = getProperty(Constants.RELEASEREPO);
		return releaseURI(name, jarName, jarStream);
	}

	
	RepositoryPlugin getReleaseRepo(String releaserepo) {
		String name = releaserepo == null ? name = getProperty(RELEASEREPO) : releaserepo;

		List<RepositoryPlugin> plugins = getPlugins(RepositoryPlugin.class);

		for (RepositoryPlugin plugin : plugins) {
			if (!plugin.canWrite())
				continue;

			if (name == null)
				return plugin;

			if (name.equals(plugin.getName()))
				return plugin;
		}
		return null;
	}
		private RepositoryPlugin getReleaseRepo() {
		String repoName = getProperty(Constants.RELEASEREPO);

		List<RepositoryPlugin> repos = getPlugins(RepositoryPlugin.class);
		for (RepositoryPlugin r : repos) {
			if (r.canWrite()) {
				if (repoName == null || r.getName().equals(repoName)) {
					return r;
				}
			}
		}
		if (repoName == null)
			error("Could not find a writable repo for the release repo (-releaserepo is not set)");
		else
			error("No such -releaserepo %s found", repoName);

		return null;
	}

		@Description("Release this project")
	public void _release(releaseOptions options) throws Exception {
		Set<Project> projects = new LinkedHashSet<Project>();

		Workspace ws = Workspace.findWorkspace(getBase());
		if (ws == null) {
			error("Workspace option was specified but cannot find a workspace from %s", getBase());
			return;
		}

		if (options.workspace()) {
			projects.addAll(ws.getAllProjects());
		}

		Project project = getProject(options.project());
		if (project != null) {
			projects.add(project);
		}

		if (projects.isEmpty()) {
			error("Cannot find any projects");
			return;
		}

		String repo = options.repo();
		if (repo != null) {
			RepositoryPlugin repository = ws.getRepository(repo);
			if (repository == null) {
				error("No such release repo %s%nFound:%n%s", repository, Strings.join("\n", ws.getRepositories()));
			}

		}
		for (Project p : projects) {
			if (repo != null) {
				p.setProperty(Constants.RELEASEREPO, repo);
			}
			p.release(options.test());
		}
		getInfo(project);
	}

	
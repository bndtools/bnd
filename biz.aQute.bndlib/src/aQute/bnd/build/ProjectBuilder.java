package aQute.bnd.build;

import java.io.*;
import java.util.*;

import aQute.bnd.differ.*;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.bnd.version.*;

public class ProjectBuilder extends Builder {
	private final DiffPluginImpl	differ	= new DiffPluginImpl();
	Project							project;
	boolean							initialized;

	public ProjectBuilder(Project project) {
		super(project);
		this.project = project;
	}

	public ProjectBuilder(ProjectBuilder builder) {
		super(builder);
		this.project = builder.project;
	}

	@Override
	public long lastModified() {
		return Math.max(project.lastModified(), super.lastModified());
	}

	/**
	 * We put our project and our workspace on the macro path.
	 */
	@Override
	protected Object[] getMacroDomains() {
		return new Object[] {
				project, project.getWorkspace()
		};
	}

	@Override
	public Builder getSubBuilder() throws Exception {
		return project.getBuilder(this);
	}

	public Project getProject() {
		return project;
	}

	@Override
	public void init() {
		try {
			if (!initialized) {
				initialized = true;
				for (Container file : project.getClasspath()) {
					addClasspath(file.getFile());
				}

				for (Container file : project.getBuildpath()) {
					addClasspath(file.getFile());
				}

				for (Container file : project.getBootclasspath()) {
					addClasspath(file.getFile());
				}

				for (File file : project.getAllsourcepath()) {
					addSourcepath(file);
				}

			}
		}
		catch (Exception e) {
			msgs.Unexpected_Error_("ProjectBuilder init", e);
		}
	}

	@Override
	public List<Jar> getClasspath() {
		init();
		return super.getClasspath();
	}

	@Override
	protected void changedFile(File f) {
		project.getWorkspace().changedFile(f);
	}

	/**
	 * Compare this builder's JAR with a baseline
	 * 
	 * @throws Exception
	 */
	@Override
	protected void doBaseline(Jar dot) throws Exception {

		Jar jar = getBaselineJar(false);
		if (jar == null) {
			return;
		}
		try {
			Baseline baseline = new Baseline(this, differ);

			Set<Info> infos = baseline.baseline(dot, jar, null);
			for (Info info : infos) {
				if (info.mismatch) {
					error("%s %-50s %-10s %-10s %-10s %-10s %-10s\n", info.mismatch ? '*' : ' ', info.packageName,
							info.packageDiff.getDelta(), info.newerVersion, info.olderVersion, info.suggestedVersion,
							info.suggestedIfProviders == null ? "-" : info.suggestedIfProviders);
				}
			}
		}
		finally {
			jar.close();
		}
	}

	public Jar getBaselineJar(boolean fallback) throws Exception {

		String baseline = getProperty(Constants.BASELINE);
		String baselineRepo = getProperty(Constants.BASELINEREPO);
		if ((baseline == null || baseline.trim().length() == 0) 
				&& (baselineRepo == null || baselineRepo.trim().length() == 0) && !fallback)
			return null;

		File baselineFile = null;
		if ((baseline == null || baseline.trim().length() == 0)) {
			baselineFile = getBaselineFromRepo(fallback);
			if (baselineFile != null)
				trace("baseline %s", baselineFile.getName());
		} else {

			trace("baseline %s", baseline);

			Collection<Container> bundles = project.getBundles(Strategy.LOWEST, baseline);
			for (Container c : bundles) {

				if (c.getError() != null || c.getFile() == null) {
					error("Erroneous baseline bundle %s", c);
					continue;
				}

				baselineFile = c.getFile();
				break;
			}
		}
		if (fallback && baselineFile == null) {
			return new Jar(".");
		}
		return new Jar(baselineFile);
	}

	private File getBaselineFromRepo(boolean fallback) throws Exception {
		String repoName = getProperty(Constants.BASELINEREPO);
		if (repoName == null && !fallback)
			return null;

		if (repoName == null) {
			repoName = getProperty(Constants.RELEASEREPO);
			if (repoName == null) {
				return null;
			}
		}

		List<RepositoryPlugin> repos = getPlugins(RepositoryPlugin.class);
		for (RepositoryPlugin repo : repos) {
			if (repoName.equals(repo.getName())) {
				SortedSet<Version> versions = repo.versions(getBsn());
				if (!versions.isEmpty()) {
					return repo.get(getBsn(), versions.last(), null);
				}
				break;
			}
		}
		return null;

	}

	/** 
	 * Gets the baseline Jar. 
	 * 
	 * @return the baseline jar
	 * @throws Exception
	 */
	public Jar getBaselineJar() throws Exception {
		return getBaselineJar(true);
	}
}

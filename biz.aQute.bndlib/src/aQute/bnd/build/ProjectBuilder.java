package aQute.bnd.build;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import aQute.bnd.differ.*;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.*;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.Strategy;

public class ProjectBuilder extends Builder {
	private final DiffPluginImpl	differ	= new DiffPluginImpl();
	Project	project;
	boolean	initialized;

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
	 * Base line against a previous version
	 * 
	 * @throws Exception
	 */

	public void doBaseline(Jar dot) throws Exception {
		Parameters diffs = parseHeader(getProperty(Constants.BASELINE));
		if (diffs.isEmpty())
			return;

		trace("baseline %s", diffs);

		Jar other = getBaselineJar();
		if (other == null) {
			return;
		}
		Baseline baseline = new Baseline(this, differ);
		Set<Info> infos = baseline.baseline(dot, other, null);
		for (Info info : infos) {
			if (info.mismatch) {
				error("%s %-50s %-10s %-10s %-10s %-10s %-10s\n", info.mismatch ? '*' : ' ', info.packageName,
						info.packageDiff.getDelta(), info.newerVersion, info.olderVersion, info.suggestedVersion,
						info.suggestedIfProviders == null ? "-" : info.suggestedIfProviders);
			}
		}
	}

	public Jar getBaselineJar() throws Exception {

		List<RepositoryPlugin> repos = getPlugins(RepositoryPlugin.class);

		String baseline = getProperty(Constants.BASELINE);
		Parameters params = parseHeader(baseline);
		File baselineFile = null;
		if (baseline == null) {
			String repoName = getProperty(Constants.BASELINEREPO);
			if (repoName == null) {
				repoName = getProperty(Constants.RELEASEREPO);
				if (repoName == null) {
					return null;
				}
			}
			for (RepositoryPlugin repo : repos) {
				if (repoName.equals(repo.getName())) {
					baselineFile = repo.get(getBsn(), null, Strategy.HIGHEST, null);
					break;
				}
			}
		} else {

			String bsn = null;
			String version = null;
			for (Entry<String,Attrs> entry : params.entrySet()) {
				bsn = entry.getKey();
				if ("@".equals(bsn)) {
					bsn = getBsn();
				}
				version = entry.getValue().get(Constants.VERSION_ATTRIBUTE);
				break;
			}
			if ("latest".equals(version)) {
				version = null;
			}
			for (RepositoryPlugin repo : repos) {
				if (version == null) {
					baselineFile = repo.get(bsn, null, Strategy.HIGHEST, null);
				} else {
					baselineFile = repo.get(bsn, version, Strategy.EXACT, null);
				}
				if (baselineFile != null) {
					break;
				}
			}
		}
		if (baselineFile == null) {
			return new Jar(".");
		}
		return new Jar(baselineFile);
	}
}

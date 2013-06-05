package aQute.bnd.build;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import aQute.bnd.differ.*;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.bnd.service.diff.*;
import aQute.bnd.version.*;
import aQute.lib.collections.*;

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

		Jar fromRepo = getBaselineJar();
		if (fromRepo == null) {
			trace("No baseline jar %s", getProperty(Constants.BASELINE));
			return;
		}
		Version newer = new Version(getVersion());
		Version older = new Version(fromRepo.getVersion());
		
		if (!getBsn().equals(fromRepo.getBsn())) {
			error("The symbolic name of this project (%s) is not the same as the baseline: %s", getBsn(), fromRepo.getBsn());
			return;
		}

		trace("baseline %s-%s against: %s", getBsn(), getVersion(), fromRepo.getName());
		try {
			Baseline baseliner = new Baseline(this, differ);

			Set<Info> infos = baseliner.baseline(dot, fromRepo, null);
			if (infos.isEmpty())
				trace("no deltas");

			for (Info info : infos) {
				if (info.mismatch) {
					SetLocation l = error(
							"Baseline mismatch for package %s, %s change. Current is %s, repo is %s, suggest %s or %s\n",
							info.packageName, info.packageDiff.getDelta(), info.newerVersion, info.olderVersion,
							info.suggestedVersion, info.suggestedIfProviders == null ? "-" : info.suggestedIfProviders);
					l.header(Constants.BASELINE);
					if (getPropertiesFile() != null)
						l.file(getPropertiesFile().getAbsolutePath());
					l.details(info);
				}
			}
			if ( newer.getWithoutQualifier().equals(older.getWithoutQualifier())) {
				// We have a special case, the current and repository revisions
				// have the same version, this happens after a release, only want
				// to generate an error when they really differ.
				
				if ( baseliner.getDiff().getDelta() == Delta.UNCHANGED ) 
					return;
			}
			
			// Ok, now our bundle version must be > the suggestedVersion
			if ( newer.getWithoutQualifier().compareTo(baseliner.getSuggestedVersion())< 0) {
				error("The bundle version %s is too low, must be at least %s", newer, baseliner.getSuggestedVersion());
			}
		}
		finally {
			fromRepo.close();
		}
	}

	public Jar getLastRevision() throws Exception {
		RepositoryPlugin releaseRepo = getReleaseRepo();
		SortedSet<Version> versions = releaseRepo.versions(getBsn());
		if (versions.isEmpty())
			return null;
		
		Jar jar = new Jar(releaseRepo.get(getBsn(), versions.last(), null));
		addClose(jar);
		return jar;
	}

	/**
	 * This method attempts to find the baseline jar for the current project. It
	 * reads the -baseline property and treats it as instructions. These
	 * instructions are matched against the bsns of the jars (think sub
	 * builders!). If they match, the sub builder is selected.
	 * <p>
	 * The instruction can then specify the following options:
	 * 
	 * <pre>
	 * 	version : baseline version from repository
	 * 	file    : a file path
	 * </pre>
	 * 
	 * If neither is specified, the current version is used to find the highest
	 * version (without qualifier) that is below the current version. If a
	 * version is specified, we take the highest version with the same base
	 * version.
	 * <p>
	 * Since baselining is expensive and easily generates errors you must enable
	 * it. The easiest solution is to {@code -baseline: *}. This will match all
	 * sub builders and will calculate the version.
	 * 
	 * @return a Jar or null
	 */
	public Jar getBaselineJar() throws Exception {
		String bl = getProperty(Constants.BASELINE);
		if (bl == null || Constants.NONE.equals(bl))
			return null;

		Instructions baselines = new Instructions(getProperty(Constants.BASELINE));
		if (baselines.isEmpty())
			return null; // no baselining

		RepositoryPlugin repo = getReleaseRepo();
		if (repo == null)
			return null; // errors reported already

		String bsn = getBsn();
		Version version = new Version(getVersion());
		SortedSet<Version> versions = removeStagedAndFilter(repo.versions(bsn));

		if (versions.isEmpty()) {
			error("There are no versions for %s in the %s repo", bsn, repo);
			return null;
		}

		
		//
		// Loop over the instructions, first match commits.
		//

		for (Entry<Instruction,Attrs> e : baselines.entrySet()) {
			if (e.getKey().matches(bsn)) {
				Attrs attrs = e.getValue();
				Version target;

				if (attrs.containsKey("version")) {

					// Specified version!

					String v = attrs.get("version");
					if (!Verifier.isVersion(v)) {
						error("Not a valid version in %s %s", Constants.BASELINE, v);
						return null;
					}

					Version base = new Version(v);
					SortedSet<Version> later = versions.tailSet(base);
					if ( later.isEmpty()) {
						error("For baselineing %s-%s, specified version %s not found", bsn, version, base);
						return null;
					}

					// First element is equal or next to the base we desire

					target = later.first();

					// Now, we could end up with a higher version than our
					// current
					// project

					
				} else if (attrs.containsKey("file")) {

					// Can be useful to specify a file
					// for example when copying a bundle with a public api

					File f = getProject().getFile(attrs.get("file"));
					if (f != null && f.isFile()) {
						Jar jar = new Jar(f);
						addClose(jar);
						return jar;
					}
					error("Specified file for baseline but could not find it %s", f);
					return null;
				} else {
					target = versions.last();
				}

				// Fetch the revision

				if (target.getWithoutQualifier().compareTo(version.getWithoutQualifier()) > 0) {
					error("The baseline version %s is higher or equal than the current version %s for %s in %s",
							target, version, bsn, repo);
					return null;
				}
				if (target.getWithoutQualifier().compareTo(version.getWithoutQualifier()) > 0) {
					if ( isPedantic()) {
						warning("Baselining against jar");
					}
				}				
				File file = repo.get(bsn, target, attrs);
				if (file == null || !file.isFile()) {
					error("Decided on version %s-%s but cannot get file from repo %s", bsn, version, repo);
					return null;
				}
				Jar jar = new Jar(file);
				addClose(jar);
				return jar;
			}
		}

		// Ignore, nothing matched
		return null;
	}

	/**
	 * Remove any staging versions that have a variant with a higher qualifier.
	 * 
	 * @param versions
	 * @return
	 */
	private SortedSet<Version> removeStagedAndFilter(SortedSet<Version> versions) {
		List<Version> filtered = new ArrayList<Version>(versions);
		Collections.reverse(filtered);

		Version last = null;
		for (Iterator<Version> i = filtered.iterator(); i.hasNext();) {
			Version current = i.next().getWithoutQualifier();
			if (last != null && current.equals(last))
				i.remove();
			else
				last = current;
		}
		SortedList<Version> set = new SortedList<Version>(filtered);
		trace("filtered for only latest staged: %s from %s in range ", set, versions);
		return set;
	}

	private RepositoryPlugin getReleaseRepo() {
		RepositoryPlugin repo = null;
		String repoName = getProperty(Constants.BASELINEREPO);
		if (repoName == null)
			repoName = getProperty(Constants.RELEASEREPO);

		if (repoName != null && Constants.NONE.equals(repoName))
			return null;

		List<RepositoryPlugin> repos = getPlugins(RepositoryPlugin.class);
		for (RepositoryPlugin r : repos) {
			if (r.canWrite()) {
				if (repoName == null || r.getName().equals(repoName)) {
					repo = r;
					break;
				}
			}
		}

		if (repo == null) {
			if (repoName != null)
				error("No writeable repo with name %s found", repoName);
			else
				error("No writeable repo found");
		}

		return repo;

	}

}

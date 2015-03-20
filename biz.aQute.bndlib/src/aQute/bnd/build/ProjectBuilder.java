package aQute.bnd.build;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import aQute.bnd.differ.*;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.service.*;
import aQute.bnd.service.repository.*;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.*;
import aQute.lib.io.*;

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
	public void doBaseline(Jar dot) throws Exception {

		String diffignore = project.getProperty(Constants.DIFFIGNORE);
		trace("ignore headers & paths %s", diffignore);
		differ.setIgnore(diffignore);

		Jar fromRepo = getBaselineJar();
		if (fromRepo == null) {
			trace("No baseline jar %s", getProperty(Constants.BASELINE));
			return;
		}

		Version newer = new Version(getVersion());
		Version older = new Version(fromRepo.getVersion());

		if (!getBsn().equals(fromRepo.getBsn())) {
			error("The symbolic name of this project (%s) is not the same as the baseline: %s", getBsn(),
					fromRepo.getBsn());
			return;
		}

		//
		// Check if we want to overwrite an equal version that is not staging
		//

		if (newer.getWithoutQualifier().equals(older.getWithoutQualifier())) {
			RepositoryPlugin rr = getBaselineRepo();
			if (rr instanceof InfoRepository) {
				ResourceDescriptor descriptor = ((InfoRepository) rr).getDescriptor(getBsn(), older);
				if (descriptor != null && descriptor.phase != Phase.STAGING) {
					error("Baselining %s against same version %s but the repository says the older repository version is not the required %s but is instead %s",
							getBsn(), getVersion(), Phase.STAGING, descriptor.phase);
					return;
				}
			}
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

					fillInLocationForPackageInfo(l.location(), info.packageName);
					if (getPropertiesFile() != null)
						l.file(getPropertiesFile().getAbsolutePath());
					l.details(info);
				}
			}
			aQute.bnd.differ.Baseline.BundleInfo binfo = baseliner.getBundleInfo();
			if (binfo.mismatch) {
				SetLocation error = error("The bundle version (%s/%s) is too low, must be at least %s",
						binfo.olderVersion, binfo.newerVersion, binfo.suggestedVersion);
				error.context("Baselining");
				error.header(Constants.BUNDLE_VERSION);
				error.details(binfo);
				FileLine fl = getHeader(Pattern.compile("^" + Constants.BUNDLE_VERSION, Pattern.MULTILINE));
				if (fl != null) {
					error.file(fl.file.getAbsolutePath());
					error.line(fl.line);
					error.length(fl.length);
				}
			}
		}
		finally {
			fromRepo.close();
		}
	}

	// *

	public void fillInLocationForPackageInfo(Location location, String packageName) throws Exception {
		Parameters eps = getExportPackage();
		Attrs attrs = eps.get(packageName);
		FileLine fl;

		if (attrs != null && attrs.containsKey(Constants.VERSION_ATTRIBUTE)) {
			fl = getHeader(Pattern.compile(Constants.EXPORT_PACKAGE, Pattern.CASE_INSENSITIVE));
			if (fl != null) {
				location.file = fl.file.getAbsolutePath();
				location.line = fl.line;
				location.length = fl.length;
				return;
			}
		}

		Parameters ecs = getExportContents();
		attrs = ecs.get(packageName);
		if (attrs != null && attrs.containsKey(Constants.VERSION_ATTRIBUTE)) {
			fl = getHeader(Pattern.compile(Constants.EXPORT_CONTENTS, Pattern.CASE_INSENSITIVE));
			if (fl != null) {
				location.file = fl.file.getAbsolutePath();
				location.line = fl.line;
				location.length = fl.length;
				return;
			}
		}

		for (File src : project.getSourcePath()) {
			String path = packageName.replace('.', '/');
			File packageDir = IO.getFile(src, path);
			File pi = IO.getFile(packageDir, "package-info.java");
			if (pi.isFile()) {
				fl = findHeader(pi, Pattern.compile("@Version\\s*([^)]+)"));
				if (fl != null) {
					location.file = fl.file.getAbsolutePath();
					location.line = fl.line;
					location.length = fl.length;
					return;
				}
			}
			pi = IO.getFile(packageDir, "packageinfo");
			if (pi.isFile()) {
				fl = findHeader(pi, Pattern.compile("^\\s*version.*$"));
				if (fl != null) {
					location.file = fl.file.getAbsolutePath();
					location.line = fl.line;
					location.length = fl.length;
					return;
				}
			}

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
		return baselineProcessor.getBaselineJar();
	}

	/**
	 * Create a report of the settings
	 * 
	 * @throws Exception
	 */

	public void report(Map<String,Object> table) throws Exception {
		super.report(table);
		table.put("Baseline repo", getBaselineRepo());
		table.put("Release repo", getReleaseRepo());
	}

	public String toString() {
		return getBsn();
	}

	/**
	 * Return the bndrun files that need to be exported
	 * 
	 * @throws Exception
	 */
	public List<Run> getExportedRuns() throws Exception {
		Instructions runspec = new Instructions(getProperty(EXPORT));
		List<Run> runs = new ArrayList<Run>();

		Map<File,Attrs> files = runspec.select(getBase());

		for (Entry<File,Attrs> e : files.entrySet()) {
			Run run = new Run(project.getWorkspace(), getBase(), e.getKey());
			for (Entry<String,String> ee : e.getValue().entrySet()) {
				run.setProperty(ee.getKey(), ee.getValue());
			}
			runs.add(run);
		}

		return runs;
	}

	/**
	 * Add some extra stuff to the builds() method like exporting.
	 */

	public Jar[] builds() throws Exception {
		project.exportedPackages.clear();
		project.importedPackages.clear();
		project.containedPackages.clear();
		
		Jar[] jars = super.builds();
		if (isOk()) {
			for (Run export : getExportedRuns()) {
				addClose(export);
				if ( export.getProperty(BUNDLE_SYMBOLICNAME) == null) {
					export.setProperty(BUNDLE_SYMBOLICNAME, getBsn() + ".run");
				}
				Jar pack = export.pack(getProperty(PROFILE));
				getInfo(export);
				if ( pack != null) {
					jars = concat(Jar.class,jars, pack);
					addClose(pack);
				}
			}
		}
		return jars;
	}


	/**
	 * Called when we start to build a builder
	 */
	protected void startBuild(Builder builder) {
	}
	
	/**
	 * Called when we 're done with a builder. In this case
	 * we retrieve package information from 
	 */
	protected void doneBuild(Builder builder) {
		project.exportedPackages.putAll(builder.getExports());
		project.importedPackages.putAll(builder.getImports());
		project.containedPackages.putAll(builder.getContained());
	}

	/**
	 * Find the source file for this type
	 * 
	 * @param type
	 * @return
	 * @throws Exception
	 */
	@Override
	public String getSourceFileFor(TypeRef type) throws Exception {
		return super.getSourceFileFor(type, getSourcePath());
	}

}

package aQute.bnd.build;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Predicate;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Container.TYPE;
import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.BundleInfo;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Packages;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.repository.InfoRepository;
import aQute.bnd.service.repository.Phase;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.Version;
import aQute.lib.collections.SortedList;
import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.glob.PathSet;

public class ProjectBuilder extends Builder {
	private static final Predicate<String>	pomPropertiesFilter	= new PathSet("META-INF/maven/*/*/pom.properties")
		.matches();
	private final static Logger				logger				= LoggerFactory.getLogger(ProjectBuilder.class);
	private final DiffPluginImpl			differ				= new DiffPluginImpl();
	Project									project;
	boolean									initialized;

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
				Parameters dependencies = (getProperty(MAVEN_DEPENDENCIES) == null) ? new Parameters() : null;
				doRequireBnd();
				for (Container file : project.getClasspath()) {
					addClasspath(dependencies, file);
				}

				File output = project.getOutput();
				if (output.exists()) {
					addClasspath(output);
				}

				for (Container file : project.getBuildpath()) {
					addClasspath(dependencies, file);
				}

				for (Container file : project.getBootclasspath()) {
					addClasspath(file);
				}

				for (File file : project.getAllsourcepath()) {
					addSourcepath(file);
				}
				if ((dependencies != null) && !dependencies.isEmpty()) {
					setProperty(MAVEN_DEPENDENCIES, dependencies.toString());
				}
			}
		} catch (Exception e) {
			msgs.Unexpected_Error_("ProjectBuilder init", e);
		}
	}

	private void addClasspath(Parameters dependencies, Container c) throws IOException {
		File file = c.getFile();
		if ((c.getType() == TYPE.PROJECT) && !file.exists()) {
			return;
		}
		Jar jar = new Jar(file);
		super.addClasspath(jar);
		project.unreferencedClasspathEntries.put(jar.getName(), c);
		if ((dependencies != null) && !Boolean.parseBoolean(c.getAttributes()
			.getOrDefault("maven-optional", "false"))) {
			jar.getResources(pomPropertiesFilter)
				.forEachOrdered(r -> {
					UTF8Properties pomProperties = new UTF8Properties();
					try (InputStream in = r.openInputStream()) {
						pomProperties.load(in);
					} catch (Exception e) {
						logger.debug("unable to read pom.properties resource {}", r, e);
						return;
					}
					String depVersion = pomProperties.getProperty("version");
					String depGroupId = pomProperties.getProperty("groupId");
					String depArtifactId = pomProperties.getProperty("artifactId");
					if ((depGroupId != null) && (depArtifactId != null) && (depVersion != null)) {
						Attrs attrs = new Attrs();
						attrs.put("groupId", depGroupId);
						attrs.put("artifactId", depArtifactId);
						attrs.put("version", depVersion);
						attrs.put("scope", c.getAttributes()
							.getOrDefault("maven-scope", getProperty(MAVEN_SCOPE, "compile")));
						StringBuilder key = new StringBuilder();
						OSGiHeader.quote(key, IO.absolutePath(file));
						dependencies.put(key.toString(), attrs);
					}
				});
		}
	}

	public void addClasspath(Container c) throws IOException {
		addClasspath(null, c);
	}

	@Override
	public List<Jar> getClasspath() {
		init();
		return super.getClasspath();
	}

	@Override
	protected void changedFile(File f) {
		project.getWorkspace()
			.changedFile(f);
	}

	/**
	 * Compare this builder's JAR with a baseline
	 *
	 * @throws Exception
	 */
	@Override
	public void doBaseline(Jar dot) throws Exception {
		Parameters diffignore = new Parameters(project.getProperty(Constants.DIFFIGNORE), this);
		logger.debug("ignore headers & paths {}", diffignore);
		differ.setIgnore(diffignore);
		Instructions diffpackages = new Instructions(new Parameters(project.getProperty(Constants.DIFFPACKAGES), this));
		logger.debug("diffpackages {}", diffpackages);

		try (Jar fromRepo = getBaselineJar()) {
			if (fromRepo == null) {
				logger.debug("No baseline jar {}", getProperty(Constants.BASELINE));
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
			// Check if we want to overwrite an equal version that is not
			// staging
			//

			if (newer.getWithoutQualifier()
				.equals(older.getWithoutQualifier())) {
				RepositoryPlugin rr = getBaselineRepo();
				if (rr instanceof InfoRepository) {
					ResourceDescriptor descriptor = ((InfoRepository) rr).getDescriptor(getBsn(), older);
					if (descriptor != null && descriptor.phase != Phase.STAGING) {
						error(
							"Baselining %s against same version %s but the repository says the older repository version is not the required %s but is instead %s",
							getBsn(), getVersion(), Phase.STAGING, descriptor.phase);
						return;
					}
				}
			}

			logger.debug("baseline {}-{} against: {}", getBsn(), getVersion(), fromRepo.getName());
			Baseline baseliner = new Baseline(this, differ);

			Set<Info> infos = baseliner.baseline(dot, fromRepo, diffpackages);
			if (infos.isEmpty())
				logger.debug("no deltas");

			StringBuffer sb = new StringBuffer();
			try (Formatter f = new Formatter(sb, Locale.US)) {
				for (Info info : infos) {
					if (!info.mismatch) {
						continue;
					}
					sb.setLength(0);
					Diff packageDiff = info.packageDiff;
					f.format(
						"Baseline mismatch for package %s, %s change. Current is %s, repo is %s, suggest %s or %s%n%#S",
						packageDiff.getName(), packageDiff.getDelta(), info.newerVersion,
						((info.olderVersion != null) && info.olderVersion.equals(Version.LOWEST)) ? '-'
							: info.olderVersion,
						((info.suggestedVersion != null) && info.suggestedVersion.compareTo(info.newerVersion) <= 0)
							? "ok"
							: info.suggestedVersion,
						(info.suggestedIfProviders == null) ? "-" : info.suggestedIfProviders, packageDiff);
					SetLocation l = error("%s", f.toString());
					l.header(Constants.BASELINE);
					fillInLocationForPackageInfo(l.location(), packageDiff.getName());
					if (l.location().file == null) {
						// Default to properties file
						File propertiesFile = getPropertiesFile();
						if (propertiesFile == null) {
							propertiesFile = project.getPropertiesFile();
						}
						l.file(propertiesFile.getAbsolutePath());
					}
					l.details(info);
				}

				BundleInfo binfo = baseliner.getBundleInfo();
				if (binfo.mismatch) {
					sb.setLength(0);
					f.format("The bundle version (%s/%s) is too low, must be at least %s%n%#S", binfo.olderVersion,
						binfo.newerVersion, binfo.suggestedVersion, baseliner.getDiff());
					SetLocation error = error("%s", f.toString());
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
		}
	}

	// *
	private static final Pattern	PATTERN_EXPORT_PACKAGE		= Pattern.compile(Constants.EXPORT_PACKAGE,
		Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
	private static final Pattern	PATTERN_EXPORT_CONTENTS		= Pattern.compile(Constants.EXPORT_CONTENTS,
		Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
	private static final Pattern	PATTERN_VERSION_ANNOTATION	= Pattern
		.compile("@(?:\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*Version\\s*([^)]+)");
	private static final Pattern	PATTERN_VERSION_PACKAGEINFO	= Pattern.compile("^\\s*version\\s.*$");

	public void fillInLocationForPackageInfo(Location location, String packageName) throws Exception {
		Parameters eps = getExportPackage();
		Attrs attrs = eps.get(packageName);
		if (attrs != null && attrs.containsKey(Constants.VERSION_ATTRIBUTE)) {
			FileLine fl = getHeader(PATTERN_EXPORT_PACKAGE);
			if (fl != null) {
				location.file = IO.absolutePath(fl.file);
				location.line = fl.line;
				location.length = fl.length;
				return;
			}
		}

		Parameters ecs = getExportContents();
		attrs = ecs.get(packageName);
		if (attrs != null && attrs.containsKey(Constants.VERSION_ATTRIBUTE)) {
			FileLine fl = getHeader(PATTERN_EXPORT_CONTENTS);
			if (fl != null) {
				location.file = IO.absolutePath(fl.file);
				location.line = fl.line;
				location.length = fl.length;
				return;
			}
		}

		String path = packageName.replace('.', '/');
		for (File src : project.getSourcePath()) {
			File packageDir = IO.getFile(src, path);
			File pi = IO.getFile(packageDir, "package-info.java");
			if (pi.isFile()) {
				FileLine fl = findHeader(pi, PATTERN_VERSION_ANNOTATION);
				if (fl != null) {
					location.file = IO.absolutePath(fl.file);
					location.line = fl.line;
					location.length = fl.length;
					return;
				}
			}
			pi = IO.getFile(packageDir, "packageinfo");
			if (pi.isFile()) {
				FileLine fl = findHeader(pi, PATTERN_VERSION_PACKAGEINFO);
				if (fl != null) {
					location.file = IO.absolutePath(fl.file);
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
	 *  version :
	 * baseline version from repository file : a file path
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

		RepositoryPlugin repo = getBaselineRepo();
		if (repo == null)
			return null; // errors reported already

		String bsn = getBsn();
		Version version = new Version(getVersion());
		SortedSet<Version> versions = removeStagedAndFilter(repo.versions(bsn), repo, bsn);

		if (versions.isEmpty()) {
			// We have a repo
			Version v = Version.parseVersion(getVersion())
				.getWithoutQualifier();
			if (v.compareTo(Version.ONE) > 0) {
				warning(
					"There is no baseline for %s in the baseline repo %s. The build is for version %s, which is higher than 1.0.0 which suggests that there should be a prior version.",
					getBsn(), repo, v);
			}
			return null;
		}

		//
		// Loop over the instructions, first match commits.
		//

		for (Entry<Instruction, Attrs> e : baselines.entrySet()) {
			if (e.getKey()
				.matches(bsn)) {
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
					if (later.isEmpty()) {
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

				if (target.getWithoutQualifier()
					.compareTo(version.getWithoutQualifier()) > 0) {
					error("The baseline version %s is higher than the current version %s for %s in %s", target, version,
						bsn, repo);
					return null;
				}
				if (target.getWithoutQualifier()
					.compareTo(version.getWithoutQualifier()) == 0) {
					if (isPedantic()) {
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
	 * @param repo
	 * @throws Exception
	 */
	private SortedSet<Version> removeStagedAndFilter(SortedSet<Version> versions, RepositoryPlugin repo, String bsn)
		throws Exception {
		List<Version> filtered = new ArrayList<>(versions);
		Collections.reverse(filtered);

		InfoRepository ir = (repo instanceof InfoRepository) ? (InfoRepository) repo : null;

		//
		// Filter any versions that only differ in qualifier
		// The last variable is the last one added. Since we are
		// sorted from high to low, we skip any earlier base versions
		//
		Version last = null;
		for (Iterator<Version> i = filtered.iterator(); i.hasNext();) {
			Version v = i.next();

			// Check if same base version as last
			Version current = v.getWithoutQualifier();
			if (last != null && current.equals(last)) {
				i.remove();
				continue;
			}

			//
			// Check if this is not a master if the repo
			// has a state for each resource
			// /
			if (ir != null && !isMaster(ir, bsn, v))
				i.remove();

			last = current;
		}
		SortedList<Version> set = new SortedList<>(filtered);
		logger.debug("filtered for only latest staged: {} from {} in range ", set, versions);
		return set;
	}

	/**
	 * Check if we have a master phase.
	 *
	 * @param repo
	 * @param bsn
	 * @param v
	 * @throws Exception
	 */
	private boolean isMaster(InfoRepository repo, String bsn, Version v) throws Exception {
		ResourceDescriptor descriptor = repo.getDescriptor(bsn, v);

		//
		// If not there, we assume that is master
		//

		if (descriptor == null)
			return true;

		return descriptor.phase == Phase.MASTER;
	}

	private RepositoryPlugin getReleaseRepo() {
		String repoName = getProperty(Constants.RELEASEREPO);

		List<RepositoryPlugin> repos = getPlugins(RepositoryPlugin.class);
		for (RepositoryPlugin r : repos) {
			if (r.canWrite()) {
				if (repoName == null || r.getName()
					.equals(repoName)) {
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

	private RepositoryPlugin getBaselineRepo() {
		String repoName = getProperty(Constants.BASELINEREPO);
		if (repoName == null)
			return getReleaseRepo();

		List<RepositoryPlugin> repos = getPlugins(RepositoryPlugin.class);
		for (RepositoryPlugin r : repos) {
			if (r.getName()
				.equals(repoName))
				return r;
		}
		error("Could not find -baselinerepo %s", repoName);
		return null;
	}

	/**
	 * Create a report of the settings
	 *
	 * @throws Exception
	 */

	@Override
	public void report(Map<String, Object> table) throws Exception {
		super.report(table);
		table.put("Baseline repo", getBaselineRepo());
		table.put("Release repo", getReleaseRepo());
	}

	@Override
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
		List<Run> runs = new ArrayList<>();
		Set<Instruction> missing = new LinkedHashSet<>();

		Map<File, List<Attrs>> files = runspec.select(getBase(), null, missing);

		for (Entry<File, List<Attrs>> e : files.entrySet()) {
			for (Attrs attrs : e.getValue()) {
				Run run = new Run(project.getWorkspace(), getBase(), e.getKey());
				attrs.stream()
					.forEachOrdered(run::setProperty);
				runs.add(run);
			}
		}

		return runs;
	}

	Map<File, Resource> doExports(Map<File, List<Attrs>> entries) {
		Map<File, Resource> result = new LinkedHashMap<>();

		for (Entry<File, List<Attrs>> e : entries.entrySet()) {

			for (Attrs attrs : e.getValue()) {

				File file = e.getKey();
				try {

					Run run = Run.createRun(getProject().getWorkspace(), file);

					//
					// History made it that we had an -export instruction and
					// somehow
					// later export functions were added using the exporters
					// that
					// were not exactly aligned. I think we also had some
					// separate
					// function
					// in bndtools.
					// The code is now reconciled but we need to support the old
					// mode of the -export that had some quirks in naming. If no
					// options are given for the type of exporter or the name
					// then we assume it must be backward compatible. Otherwise
					// the -export follows the export function with the
					// exporters.
					//

					boolean backwardCompatible = !attrs.containsKey(Constants.EXPORT_TYPE)
						&& !attrs.containsKey(Constants.EXPORT_NAME);

					String name = run.getName();
					if (backwardCompatible) {

						if (run.getProperty(BUNDLE_SYMBOLICNAME) == null)
							run.setProperty(BUNDLE_SYMBOLICNAME, getBsn() + ".run");

						attrs.put(Constants.EXPORT_NAME, name + Constants.DEFAULT_JAR_EXTENSION);
					}
					if (attrs.containsKey(Constants.EXPORT_BSN)) {
						run.setProperty(BUNDLE_SYMBOLICNAME, attrs.get(Constants.EXPORT_BSN));
					}
					if (attrs.containsKey(Constants.EXPORT_VERSION)) {
						run.setProperty(BUNDLE_VERSION, attrs.get(Constants.EXPORT_VERSION));
					}

					attrs.stream()
						.forEachOrdered(run::setProperty);

					Entry<String, Resource> export = run.export(null, attrs);
					getInfo(run);
					if (isOk()) {
						File outputFile;
						if (backwardCompatible) {
							outputFile = project.getOutputFile(name, run.getBundleVersion());
						} else {
							name = attrs.getOrDefault(Constants.EXPORT_NAME, export.getKey());
							outputFile = getFile(project.getTarget(), name);
						}
						Resource put = result.put(outputFile, export.getValue());
						if (put != null) {
							error("Duplicate file in -export  %s. Input=%s, Attrs=%s, previous resource %s",
								outputFile.getName(), file.getName(), attrs, put);
						}
					}

				} catch (Exception ee) {
					exception(ee, "Failed to export %s, %s", file, ee.getMessage());
				}
			}
		}

		return result;
	}

	/**
	 * Add some extra stuff to the builds() method like exporting.
	 */

	@Override
	public Jar[] builds() throws Exception {
		project.exportedPackages.clear();
		project.importedPackages.clear();
		project.containedPackages.clear();

		return super.builds();
	}

	/**
	 * Called when we start to build a builder. We reset our map of bsn ->
	 * version and set the default contents of the bundle.
	 */
	@Override
	protected void startBuild(Builder builder) throws Exception {
		super.startBuild(builder);
		project.versionMap.remove(builder.getBsn());

		/*
		 * During discussion on bndtools/bndtools#1270, @rotty3000 raised the
		 * issue that, in a workspace build, bnd will not include anything in a
		 * bundle by default. One must specify Private-Package, Export-Package,
		 * Include-Resource, -includepackage, or -includeresource to put any
		 * content in a bundle. And new users make mistakes and end up with
		 * empty bundles which will be unexpected. This is different than the
		 * non-workspace modes such as the bnd gradle plugin or the
		 * bnd-maven-plugin which always include default content (gradle: normal
		 * jar task content, maven: target/classes folder). So we change
		 * ProjectBuilder (not Builder which is used by non-workspace builds) to
		 * use the source output folder (e.g. bin folder) as the default
		 * contents if the bundle's bnd file does not specify any of the
		 * following instructions: -resourceonly, -includepackage,
		 * Private-Package, -privatepackage, Export-Package, Include-Resource,
		 * or -includeresource. If the bnd file specifies any of these
		 * instructions, then they will fully control the contents of the
		 * bundle.
		 */
		if (!project.isNoBundles() && (builder.getJar() == null)
			&& (builder.getProperty(Constants.RESOURCEONLY) == null)
			&& (builder.getProperty(Constants.INCLUDEPACKAGE) == null)
			&& (builder.getProperty(Constants.PRIVATE_PACKAGE) == null)
			&& (builder.getProperty(Constants.PRIVATEPACKAGE) == null)
			&& (builder.getProperty(Constants.EXPORT_PACKAGE) == null)
			&& (builder.getProperty(Constants.INCLUDE_RESOURCE) == null)
			&& (builder.getProperty(Constants.INCLUDERESOURCE) == null) && project.getOutput()
				.isDirectory()) {
			Jar outputDirJar = new Jar(project.getName(), project.getOutput());
			outputDirJar.setReproducible(is(REPRODUCIBLE));
			outputDirJar.setManifest(new Manifest());
			builder.setJar(outputDirJar);
		}
	}

	/**
	 * Called when we're done with a builder. In this case we retrieve package
	 * information from builder.
	 */
	@Override
	protected void doneBuild(Builder builder) throws Exception {
		project.exportedPackages.putAll(builder.getExports());
		project.importedPackages.putAll(builder.getImports());
		project.containedPackages.putAll(builder.getContained());

		xrefClasspath(project.unreferencedClasspathEntries, builder.getImports());
		xrefClasspath(project.unreferencedClasspathEntries, builder.getContained());

		//
		// For the workspace repo, we maintain a map
		// of bsn -> version for this project. So here
		// we update this map. In the startBuild method
		// we cleared the map
		//

		Version version = new Version(cleanupVersion(builder.getVersion()));
		project.versionMap.put(builder.getBsn(), version);
		super.doneBuild(builder);
	}

	private void xrefClasspath(Map<String, Container> unreferencedClasspathEntries, Packages packages) {
		for (Attrs attrs : packages.values()) {
			String from = attrs.get(Constants.FROM_DIRECTIVE);
			if (from != null) {
				unreferencedClasspathEntries.remove(from);
			}
		}
	}

	/**
	 * Find the source file for this type
	 *
	 * @param type
	 * @throws Exception
	 */
	@Override
	public String getSourceFileFor(TypeRef type) throws Exception {
		return super.getSourceFileFor(type, getSourcePath());
	}

	@Override
	public boolean isInteractive() {
		return getProject().isInteractive();
	}

}

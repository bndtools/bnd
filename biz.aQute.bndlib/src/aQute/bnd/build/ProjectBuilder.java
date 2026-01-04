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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.api.ArtifactInfo;
import aQute.bnd.build.api.BuildInfo;
import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.BundleInfo;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.BundleId;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.EmbeddedResource;
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
import aQute.libg.reporter.ReporterAdapter;

public class ProjectBuilder extends Builder {
	private static final Predicate<String>	pomPropertiesFilter	= new PathSet("META-INF/maven/*/*/pom.properties")
		.matches();
	private final static Logger				logger				= LoggerFactory.getLogger(ProjectBuilder.class);
	private final DiffPluginImpl			differ				= new DiffPluginImpl();
	Project									project;
	boolean									initialized;
	boolean									includeTestpath		= false;
	BuildInfoImpl							buildInfo;

	static class BuildInfoImpl extends ReporterAdapter implements BuildInfo {

		final List<ArtifactInfoImpl>	artifacts	= new ArrayList<>();
		final Project					project;

		BuildInfoImpl(Project project) throws Exception {
			this.project = project;
		}

		@Override
		public List<ArtifactInfo> getArtifactInfos() {
			return new ArrayList<>(artifacts);
		}

		@Override
		public Project getProject() {
			return project;
		}

		@Override
		public String toString() {
			return "BuildInfo[" + project + ": " + artifacts + "]";
		}

	}

	static class ArtifactInfoImpl extends ReporterAdapter implements ArtifactInfo {
		final Manifest							manifest;
		final Packages							exports;
		final Packages							imports;
		final Packages							contained;
		final BundleId							bundleId;

		File									file;
		List<Location>							errors;
		Supplier<org.osgi.resource.Resource>	indexer;

		public ArtifactInfoImpl(Builder builder) throws Exception {
			String bsn = builder.getBsn();
			String version = builder.getVersion();
			bundleId = new BundleId(bsn, version);
			manifest = builder.getJar()
				.getManifest();
			exports = builder.getExports()
				.dup();
			imports = builder.getImports()
				.dup();
			contained = builder.getContained()
				.dup();
			getInfo(builder);
		}

		@Override
		public BundleId getBundleId() {
			return bundleId;
		}

		@Override
		public Packages getExports() {
			return exports;
		}

		@Override
		public Packages getImports() {
			return imports;
		}

		@Override
		public Packages getContained() {
			return contained;
		}

		@Override
		public String toString() {
			return "Artifact[" + bundleId + "]";
		}

	}

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
					Jar jar = new Jar(output);
					jar.putResource(PROJECT_MARKER, new EmbeddedResource("project marker", 0L));
					addClasspath(jar);
				}

				if (includeTestpath) {
					for (Container file : project.getTestpath()) {
						addClasspath(dependencies, file);
					}
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

				// add ${repo} references (important for sub-bundles)
				if (dependencies != null) {

					for (Container c : RepoCollector.collectRepoReferences(this)) {

						File file = c.getFile();
						if ((c.getType() == TYPE.PROJECT) && !file.exists()) {
							continue;
						}
						Map<String, String> containerAttributes = c.getAttributes();
						if (!Boolean.parseBoolean(containerAttributes.getOrDefault("maven-optional", "false"))) {
							try (Jar jar = new Jar(file)) {
								fillDependencies(dependencies, jar, containerAttributes);
							}
							catch (ZipException e) {
								// not a jar file (can happen if a
								// ${repo}-reference a non-jar (.dylib, .so)
							}
						}
					}
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
		Map<String, String> containerAttributes = c.getAttributes();
		if ((dependencies != null)
			&& !Boolean.parseBoolean(containerAttributes.getOrDefault("maven-optional", "false"))) {
			fillDependencies(dependencies, jar, containerAttributes);
		}
	}

	private void fillDependencies(Parameters dependencies, Jar jar, Map<String, String> containerAttributes) {
		String depGroupId = containerAttributes.get("maven-groupId");
		String depArtifactId = containerAttributes.get("maven-artifactId");
		String depVersion = containerAttributes.get("maven-version");
		String scope = containerAttributes.getOrDefault("maven-scope", getProperty(MAVEN_SCOPE, "compile"));
		if ((depGroupId != null) && (depArtifactId != null) && (depVersion != null)) {
			// the repo provided maven attributes to the container
			Attrs attrs = new Attrs();
			attrs.put("groupId", depGroupId);
			attrs.put("artifactId", depArtifactId);
			attrs.put("version", depVersion);
			attrs.put("scope", scope);
			StringBuilder key = new StringBuilder().append(depGroupId)
				.append(':')
				.append(depArtifactId)
				.append(':')
				.append(depVersion);
			String depClassifier = containerAttributes.get("maven-classifier");
			if ((depClassifier != null) && !depClassifier.isEmpty()) {
				attrs.put("classifier", depClassifier);
				key.append(":jar:")
					.append(depClassifier);
			}
			dependencies.add(key.toString(), attrs);
		} else {
			// fall back to pom.properties in jar
			if (jar == null) {
				return;
			}

			jar.getResources(pomPropertiesFilter)
				.forEachOrdered(r -> {
					UTF8Properties pomProperties = new UTF8Properties();
					try (InputStream in = r.openInputStream()) {
						pomProperties.load(in);
					} catch (Exception e) {
						logger.debug("unable to read pom.properties resource {}", r, e);
						return;
					}
					String pomGroupId = pomProperties.getProperty("groupId");
					String pomArtifactId = pomProperties.getProperty("artifactId");
					String pomVersion = pomProperties.getProperty("version");
					if ((pomGroupId != null) && (pomArtifactId != null) && (pomVersion != null)) {
						Attrs attrs = new Attrs();
						attrs.put("groupId", pomGroupId);
						attrs.put("artifactId", pomArtifactId);
						attrs.put("version", pomVersion);
						attrs.put("scope", scope);
						String key = new StringBuilder().append(pomGroupId)
							.append(':')
							.append(pomArtifactId)
							.append(':')
							.append(pomVersion)
							.toString();
						dependencies.add(key, attrs);
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
				if (rr instanceof InfoRepository infoRepository) {
					ResourceDescriptor descriptor = infoRepository.getDescriptor(getBsn(), older);
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
		Version version = Version.parseVersion(getVersion());
		SortedSet<Version> versions = removeStagedAndFilter(repo.versions(bsn), repo, bsn);

		if (versions.isEmpty()) {
			// We have a repo
			// Baselining 0.x is uninteresting
			// x.0.0 is a new major version so maybe there is no baseline
			if ((version.getMajor() > 0) && ((version.getMinor() > 0) || (version.getMicro() > 0))) {
				warning(
					"There is no baseline for %s in the baseline repo %s. The build is for version %s, which is higher than %s which suggests that there should be a prior version.",
					getBsn(), repo, version.getWithoutQualifier(), new Version(version.getMajor()));
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

		InfoRepository ir = (repo instanceof InfoRepository infoRepository) ? infoRepository : null;

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

		String repoNames = getProperty(Constants.RELEASEREPO);

		List<RepositoryPlugin> releaseRepos = project.getReleaseRepos(repoNames);

		if (!releaseRepos.isEmpty()) {
			if (releaseRepos.size() > 1) {
				warning("Found multiple release repositories [%s], so we will use the first one", repoNames);
			}
			return releaseRepos.get(0);
		}

		error("No releaserepo(s) found for %s", repoNames);
		return null;
	}

	private RepositoryPlugin getBaselineRepo() {
		String repoName = getProperty(Constants.BASELINEREPO);
		if (repoName == null) {
			warning("Baselining is active, but no %s is set. Will fall back to release repositories",
				Constants.BASELINEREPO);
			return getReleaseRepo();
		}

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

		Map<File, List<Attrs>> files = runspec.select(getBase(), Function.identity(), missing);

		for (Entry<File, List<Attrs>> e : files.entrySet()) {
			for (Attrs attrs : e.getValue()) {
				Run run = new Run(project.getWorkspace(), getBase(), e.getKey());
				attrs.forEach(run::setProperty);
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
				try (Run run = Run.createRun(getProject().getWorkspace(), file)) {

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

					attrs.forEach(run::setProperty);

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
		buildInfo = new BuildInfoImpl(project);
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

		if (!project.isNoBundles() && builder.getJar() == null && project.getOutput()
			.isDirectory()) {

			if (!builder.isPropertySet(EXPAND_HEADERS)) {
				builder.setProperty(Constants.INCLUDEPACKAGE, ALL_FROM_PROJECT);
			}
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

		ArtifactInfoImpl artifactInfo = new ArtifactInfoImpl(builder);
		buildInfo.artifacts.add(artifactInfo);
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

	public ProjectBuilder includeTestpath() {
		this.includeTestpath = true;
		return this;
	}

	public BuildInfoImpl getBuildInfo() {
		return buildInfo;
	}
}

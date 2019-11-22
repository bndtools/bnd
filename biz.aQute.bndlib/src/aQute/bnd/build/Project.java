package aQute.bnd.build;

import static aQute.bnd.build.Container.toPaths;
import static java.util.stream.Collectors.toList;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Container.TYPE;
import aQute.bnd.exporter.executable.ExecutableJarExporter;
import aQute.bnd.exporter.runbundles.RunbundlesExporter;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.help.Syntax;
import aQute.bnd.help.instructions.ProjectInstructions;
import aQute.bnd.help.instructions.ProjectInstructions.StaleTest;
import aQute.bnd.http.HttpClient;
import aQute.bnd.maven.support.Pom;
import aQute.bnd.maven.support.ProjectPom;
import aQute.bnd.osgi.About;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Macro;
import aQute.bnd.osgi.Packages;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.osgi.eclipse.EclipseClasspath;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.bnd.service.CommandPlugin;
import aQute.bnd.service.DependencyContributor;
import aQute.bnd.service.Deploy;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.PutOptions;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.bnd.service.Scripter;
import aQute.bnd.service.Strategy;
import aQute.bnd.service.action.Action;
import aQute.bnd.service.action.NamedAction;
import aQute.bnd.service.export.Exporter;
import aQute.bnd.service.release.ReleaseBracketingPlugin;
import aQute.bnd.service.specifications.RunSpecification;
import aQute.bnd.stream.MapStream;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.collections.Iterables;
import aQute.lib.converter.Converter;
import aQute.lib.exceptions.ConsumerWithException;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.FileTree;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.command.Command;
import aQute.libg.generics.Create;
import aQute.libg.glob.Glob;
import aQute.libg.qtokens.QuotedTokenizer;
import aQute.libg.reporter.ReporterMessages;
import aQute.libg.sed.Sed;
import aQute.libg.tuple.Pair;

/**
 * This class is NOT threadsafe
 */

public class Project extends Processor {
	private final static Logger logger = LoggerFactory.getLogger(Project.class);

	static class RefreshData {
		Parameters installRepositories;
	}

	final static String				DEFAULT_ACTIONS					= "build; label='Build', test; label='Test', run; label='Run', clean; label='Clean', release; label='Release', refreshAll; label=Refresh, deploy;label=Deploy";
	public final static String		BNDFILE							= "bnd.bnd";
	final static Path				BNDPATH							= Paths.get(BNDFILE);
	public final static String		BNDCNF							= "cnf";
	public final static String		SHA_256							= "SHA-256";
	final Workspace					workspace;
	private final AtomicBoolean		preparedPaths					= new AtomicBoolean();
	private final Set<Project>		dependenciesFull				= new LinkedHashSet<>();
	private final Set<Project>		dependenciesBuild				= new LinkedHashSet<>();
	private final Set<Project>		dependenciesTest				= new LinkedHashSet<>();
	private final Set<Project>		dependents						= new LinkedHashSet<>();
	final Collection<Container>		classpath						= new LinkedHashSet<>();
	final Collection<Container>		buildpath						= new LinkedHashSet<>();
	final Collection<Container>		testpath						= new LinkedHashSet<>();
	final Collection<Container>		runpath							= new LinkedHashSet<>();
	final Collection<Container>		runbundles						= new LinkedHashSet<>();
	final Collection<Container>		runfw							= new LinkedHashSet<>();
	File							runstorage;
	final Map<File, Attrs>			sourcepath						= new LinkedHashMap<>();
	final Collection<File>			allsourcepath					= new LinkedHashSet<>();
	final Collection<Container>		bootclasspath					= new LinkedHashSet<>();
	final Map<String, Version>		versionMap						= new LinkedHashMap<>();
	File							output;
	File							target;
	private final AtomicInteger		revision						= new AtomicInteger();
	private File					files[];
	boolean							delayRunDependencies			= true;
	final ProjectMessages			msgs							= ReporterMessages.base(this,
		ProjectMessages.class);
	private Properties				ide;
	final Packages					exportedPackages				= new Packages();
	final Packages					importedPackages				= new Packages();
	final Packages					containedPackages				= new Packages();
	final PackageInfo				packageInfo						= new PackageInfo(this);
	private Makefile				makefile;
	private volatile RefreshData	data							= new RefreshData();
	public Map<String, Container>	unreferencedClasspathEntries	= new HashMap<>();
	public ProjectInstructions		instructions					= getInstructions(ProjectInstructions.class);

	public Project(Workspace workspace, File unused, File buildFile) {
		super(workspace);
		this.workspace = workspace;
		setFileMustExist(false);
		if (buildFile != null)
			setProperties(buildFile);

		// For backward compatibility reasons, we also read
		readBuildProperties();
	}

	public Project(Workspace workspace, File buildDir) {
		this(workspace, buildDir, new File(buildDir, BNDFILE));
	}

	private void readBuildProperties() {
		try {
			File f = getFile("build.properties");
			if (f.isFile()) {
				Properties p = loadProperties(f);
				for (String key : Iterables.iterable(p.propertyNames(), String.class::cast)) {
					String newkey = key;
					if (key.indexOf('$') >= 0) {
						newkey = getReplacer().process(key);
					}
					setProperty(newkey, p.getProperty(key));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Project getUnparented(File propertiesFile) throws Exception {
		propertiesFile = propertiesFile.getAbsoluteFile();
		Workspace workspace = new Workspace(propertiesFile.getParentFile());
		Project project = new Project(workspace, propertiesFile.getParentFile());
		project.setProperties(propertiesFile);
		project.setFileMustExist(true);
		return project;
	}

	public boolean isValid() {
		if (getBase() == null || !getBase().isDirectory())
			return false;

		return getPropertiesFile() == null || getPropertiesFile().isFile();
	}

	/**
	 * Return a new builder that is nicely setup for this project. Please close
	 * this builder after use.
	 *
	 * @param parent The project builder to use as parent, use this project if
	 *            null
	 * @throws Exception
	 */
	public ProjectBuilder getBuilder(ProjectBuilder parent) throws Exception {

		ProjectBuilder builder;

		if (parent == null)
			builder = new ProjectBuilder(this);
		else
			builder = new ProjectBuilder(parent);

		builder.setBase(getBase());
		builder.use(this);
		return builder;
	}

	public int getChanged() {
		return revision.get();
	}

	/*
	 * Indicate a change in the external world that affects our build. This will
	 * clear any cached results.
	 */
	public void setChanged() {
		preparedPaths.set(false);
		files = null;
		revision.getAndIncrement();
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	@Override
	public String toString() {
		return getName();
	}

	/**
	 * Set up all the paths
	 */

	public void prepare() throws Exception {
		if (!isValid()) {
			warning("Invalid project attempts to prepare: %s", this);
			return;
		}

		synchronized (preparedPaths) {
			if (preparedPaths.get()) {
				// ensure output folders exist
				getSrcOutput0();
				getTarget0();
				return;
			}
			if (!workspace.trail.add(this)) {
				throw new CircularDependencyException(workspace.trail.toString() + "," + this);
			}
			try {
				String basePath = IO.absolutePath(getBase());

				dependenciesFull.clear();
				dependenciesBuild.clear();
				dependenciesTest.clear();
				dependents.clear();

				buildpath.clear();
				testpath.clear();
				sourcepath.clear();
				allsourcepath.clear();
				bootclasspath.clear();

				// JIT
				runpath.clear();
				runbundles.clear();
				runfw.clear();

				// We use a builder to construct all the properties for
				// use.
				setProperty("basedir", basePath);

				// If a bnd.bnd file exists, we read it.
				// Otherwise, we just do the build properties.
				if (!getPropertiesFile().isFile() && new File(getBase(), ".classpath").isFile()) {
					// Get our Eclipse info, we might depend on other
					// projects
					// though ideally this should become empty and void
					doEclipseClasspath();
				}

				// Calculate our source directories

				Parameters srces = new Parameters(mergeProperties(Constants.DEFAULT_PROP_SRC_DIR), this);
				if (srces.isEmpty())
					srces.add(Constants.DEFAULT_PROP_SRC_DIR, new Attrs());

				for (Entry<String, Attrs> e : srces.entrySet()) {

					File dir = getFile(removeDuplicateMarker(e.getKey()));

					if (!IO.absolutePath(dir)
						.startsWith(basePath)) {
						error("The source directory lies outside the project %s directory: %s", this, dir)
							.header(Constants.DEFAULT_PROP_SRC_DIR)
							.context(e.getKey());
						continue;
					}

					if (!dir.exists()) {
						try {
							IO.mkdirs(dir);
						} catch (Exception ex) {
							exception(ex, "could not create src directory (in src property) %s", dir)
								.header(Constants.DEFAULT_PROP_SRC_DIR)
								.context(e.getKey());
							continue;
						}
						if (!dir.exists()) {
							error("could not create src directory (in src property) %s", dir)
								.header(Constants.DEFAULT_PROP_SRC_DIR)
								.context(e.getKey());
							continue;
						}
					}

					if (dir.isDirectory()) {

						sourcepath.put(dir, new Attrs(e.getValue()));
						allsourcepath.add(dir);
					} else
						error("the src path (src property) contains an entry that is not a directory %s", dir)
							.header(Constants.DEFAULT_PROP_SRC_DIR)
							.context(e.getKey());
				}

				// Set default bin directory
				output = getSrcOutput0();
				if (!output.isDirectory()) {
					msgs.NoOutputDirectory_(output);
				}

				// Where we store all our generated stuff.
				target = getTarget0();

				// Where the launched OSGi framework stores stuff
				String runStorageStr = getProperty(Constants.RUNSTORAGE);
				runstorage = runStorageStr != null ? getFile(runStorageStr) : null;

				// We might have some other projects we want build
				// before we do anything, but these projects are not in
				// our path. The -dependson allows you to build them before.
				// The values are possibly negated globbing patterns.

				Set<String> requiredProjectNames = new LinkedHashSet<>(
					getMergedParameters(Constants.DEPENDSON).keySet());

				// Allow DependencyConstributors to modify requiredProjectNames
				List<DependencyContributor> dcs = getPlugins(DependencyContributor.class);
				for (DependencyContributor dc : dcs)
					dc.addDependencies(this, requiredProjectNames);

				Instructions is = new Instructions(requiredProjectNames);
				Collection<Project> projects = getWorkspace().getAllProjects();
				projects.remove(this); // since -dependson could use a wildcard
				Set<Instruction> unused = new HashSet<>();
				Set<Project> buildDeps = new LinkedHashSet<>(is.select(projects, unused, false));

				for (Instruction u : unused)
					msgs.MissingDependson_(u.getInput());

				// We have two paths that consists of repo files, projects,
				// or some other stuff. The doPath routine adds them to the
				// path and extracts the projects so we can build them
				// before.

				doPath(buildpath, buildDeps, parseBuildpath(), bootclasspath, false, BUILDPATH);

				Set<Project> testDeps = new LinkedHashSet<>(buildDeps);
				doPath(testpath, testDeps, parseTestpath(), bootclasspath, false, TESTPATH);

				if (!delayRunDependencies) {
					doPath(runfw, testDeps, parseRunFw(), null, false, RUNFW);
					doPath(runpath, testDeps, parseRunpath(), null, false, RUNPATH);
					doPath(runbundles, testDeps, parseRunbundles(), null, true, RUNBUNDLES);
				}

				// We now know all dependent projects. But we also depend
				// on whatever those projects depend on. This creates an
				// ordered list without any duplicates. This of course assumes
				// that there is no circularity. However, this is checked
				// by the inPrepare flag, will throw an exception if we
				// are circular.

				Set<Project> visited = new HashSet<>();
				visited.add(this);
				for (Project project : testDeps) {
					project.traverse(dependenciesFull, this, visited);
				}

				dependenciesBuild.addAll(dependenciesFull);
				dependenciesBuild.retainAll(buildDeps);
				dependenciesTest.addAll(dependenciesFull);
				dependenciesTest.retainAll(testDeps);

				for (Project project : dependenciesFull) {
					allsourcepath.addAll(project.getSourcePath());
				}

				preparedPaths.set(true);
			} finally {
				workspace.trail.remove(this);
			}
		}
	}

	/*
	 *
	 */

	private File getSrcOutput0() throws IOException {
		File output = getSrcOutput().getAbsoluteFile();
		if (!output.exists()) {
			IO.mkdirs(output);
			getWorkspace().changedFile(output);
		}
		return output;
	}

	private File getTarget0() throws IOException {
		File target = getTargetDir();
		if (!target.exists()) {
			IO.mkdirs(target);
			getWorkspace().changedFile(target);
		}
		return target;
	}

	/**
	 * This method is deprecated because this can handle only one source dir.
	 * Use getSourcePath. For backward compatibility we will return the first
	 * entry on the source path.
	 *
	 * @return first entry on the {@link #getSourcePath()}
	 */
	@Deprecated
	public File getSrc() throws Exception {
		prepare();
		if (sourcepath.isEmpty())
			return getFile("src");

		return sourcepath.keySet()
			.iterator()
			.next();
	}

	public File getSrcOutput() {
		return getFile(getProperty(Constants.DEFAULT_PROP_BIN_DIR));
	}

	public File getTestSrc() {
		return getFile(getProperty(Constants.DEFAULT_PROP_TESTSRC_DIR));
	}

	public File getTestOutput() {
		return getFile(getProperty(Constants.DEFAULT_PROP_TESTBIN_DIR));
	}

	public File getTargetDir() {
		return getFile(getProperty(Constants.DEFAULT_PROP_TARGET_DIR));
	}

	private void traverse(Set<Project> dependencies, Project dependent, Set<Project> visited) throws Exception {
		if (visited.add(this)) {
			for (Project project : getTestDependencies()) {
				project.traverse(dependencies, this, visited);
			}
			dependencies.add(this);
		}

		dependents.add(dependent);
	}

	/**
	 * Iterate over the entries and place the projects on the projects list and
	 * all the files of the entries on the resultpath.
	 *
	 * @param resultpath The list that gets all the files
	 * @param projects The list that gets any projects that are entries
	 * @param entries The input list of classpath entries
	 */
	private void doPath(Collection<Container> resultpath, Collection<Project> projects, Collection<Container> entries,
		Collection<Container> bootclasspath, boolean noproject, String name) {
		for (Container cpe : entries) {
			if (cpe.getError() != null)
				error("%s", cpe.getError()).header(name)
					.context(cpe.getBundleSymbolicName());
			else {
				if (cpe.getType() == Container.TYPE.PROJECT) {
					projects.add(cpe.getProject());

					if (noproject //
						&& since(About._2_3) //
						&& VERSION_ATTR_PROJECT.equals(cpe.getAttributes()
							.get(VERSION_ATTRIBUTE))) {
						//
						// we're trying to put a project's output directory on
						// -runbundles list
						//
						error(
							"%s is specified with version=project on %s. This version uses the project's output directory, which is not allowed since it must be an actual JAR file for this list.",
							cpe.getBundleSymbolicName(), name).header(name)
								.context(cpe.getBundleSymbolicName());
					}
				}
				if (bootclasspath != null && (cpe.getBundleSymbolicName()
					.startsWith("ee.")
					|| cpe.getAttributes()
						.containsKey("boot")))
					bootclasspath.add(cpe);
				else
					resultpath.add(cpe);
			}
		}
	}

	/**
	 * Parse the list of bundles that are a prerequisite to this project.
	 * Bundles are listed in repo specific names. So we just let our repo
	 * plugins iterate over the list of bundles and we get the highest version
	 * from them.
	 */

	private List<Container> parseBuildpath() throws Exception {
		List<Container> bundles = getBundles(Strategy.LOWEST, mergeProperties(Constants.BUILDPATH),
			Constants.BUILDPATH);
		return bundles;
	}

	private List<Container> parseRunpath() throws Exception {
		return getBundles(Strategy.HIGHEST, mergeProperties(Constants.RUNPATH), Constants.RUNPATH);
	}

	private List<Container> parseRunbundles() throws Exception {

		return parseRunbundles(mergeProperties(Constants.RUNBUNDLES));
	}

	protected List<Container> parseRunbundles(String spec) throws Exception {

		return getBundles(Strategy.HIGHEST, spec, Constants.RUNBUNDLES);
	}
	private List<Container> parseRunFw() throws Exception {
		return getBundles(Strategy.HIGHEST, getProperty(Constants.RUNFW), Constants.RUNFW);
	}

	private List<Container> parseTestpath() throws Exception {
		return getBundles(Strategy.HIGHEST, mergeProperties(Constants.TESTPATH), Constants.TESTPATH);
	}

	/**
	 * Analyze the header and return a list of files that should be on the
	 * build, test or some other path. The list is assumed to be a list of bsns
	 * with a version specification. The special case of version=project
	 * indicates there is a project in the same workspace. The path to the
	 * output directory is calculated. The default directory ${bin} can be
	 * overridden with the output attribute.
	 *
	 * @param strategyx STRATEGY_LOWEST or STRATEGY_HIGHEST
	 * @param spec The header
	 */

	public List<Container> getBundles(Strategy strategyx, String spec, String source) throws Exception {
		Instructions decorator = new Instructions(mergeProperties(source + "+"));

		List<Container> result = new ArrayList<>();
		Parameters bundles = new Parameters(spec, this);
		decorator.decorate(bundles);

		try {
			for (Iterator<Entry<String, Attrs>> i = bundles.entrySet()
				.iterator(); i.hasNext();) {
				Entry<String, Attrs> entry = i.next();
				String bsn = removeDuplicateMarker(entry.getKey());
				Map<String, String> attrs = entry.getValue();

				Container found = null;

				String versionRange = attrs.get("version");
				boolean triedGetBundle = false;

				if (bsn.indexOf('*') >= 0) {
					return getBundlesWildcard(bsn, versionRange, strategyx, attrs);
				}

				if (versionRange != null) {
					if (versionRange.equals(VERSION_ATTR_LATEST) || versionRange.equals(VERSION_ATTR_SNAPSHOT)) {
						found = getBundle(bsn, versionRange, strategyx, attrs);
						triedGetBundle = true;
					}
				}
				if (found == null) {

					//
					// TODO This looks like a duplicate
					// of what is done in getBundle??
					//
					if (versionRange != null
						&& (versionRange.equals(VERSION_ATTR_PROJECT) || versionRange.equals(VERSION_ATTR_LATEST))) {

						//
						// Use the bin directory ...
						//
						Project project = getWorkspace().getProject(bsn);
						if (project != null && project.exists()) {
							File f = project.getOutput();
							found = new Container(project, bsn, versionRange, Container.TYPE.PROJECT, f, null, attrs,
								null);
						} else {
							msgs.NoSuchProject(bsn, spec)
								.context(bsn)
								.header(source);
							continue;
						}
					} else if (versionRange != null && versionRange.equals("file")) {
						File f = getFile(bsn);
						String error = null;
						if (!f.exists())
							error = "File does not exist: " + IO.absolutePath(f);
						if (f.getName()
							.endsWith(".lib")) {
							found = new Container(this, bsn, "file", Container.TYPE.LIBRARY, f, error, attrs, null);
						} else {
							found = new Container(this, bsn, "file", Container.TYPE.EXTERNAL, f, error, attrs, null);
						}
					} else if (!triedGetBundle) {
						found = getBundle(bsn, versionRange, strategyx, attrs);
					}
				}

				if (found != null) {
					List<Container> libs = found.getMembers();
					for (Container cc : libs) {
						if (result.contains(cc)) {
							if (isPedantic())
								warning("Multiple bundles with the same final URL: %s, dropped duplicate", cc);
						} else {
							if (cc.getError() != null) {
								error("Cannot find %s", cc).context(bsn)
									.header(source);
							}
							result.add(cc);
						}
					}
				} else {
					// Oops, not a bundle in sight :-(
					Container x = new Container(this, bsn, versionRange, Container.TYPE.ERROR, null,
						bsn + ";version=" + versionRange + " not found", attrs, null);
					result.add(x);
					error("Can not find URL for bsn %s", bsn).context(bsn)
						.header(source);
				}
			}
		} catch (CircularDependencyException e) {
			String message = e.getMessage();
			if (source != null)
				message = String.format("%s (from property: %s)", message, source);
			msgs.CircularDependencyContext_Message_(getName(), message);
		} catch (IOException e) {
			exception(e, "Unexpected exception in get bundles", spec);
		}
		return result;
	}

	/**
	 * Just calls a new method with a default parm.
	 *
	 * @throws Exception
	 */
	Collection<Container> getBundles(Strategy strategy, String spec) throws Exception {
		return getBundles(strategy, spec, null);
	}

	/**
	 * Get all bundles matching a wildcard expression.
	 *
	 * @param bsnPattern A bsn wildcard, e.g. "osgi*" or just "*".
	 * @param range A range to narrow the versions of bundles found, or null to
	 *            return any version.
	 * @param strategyx The version selection strategy, which may be 'HIGHEST'
	 *            or 'LOWEST' only -- 'EXACT' is not permitted.
	 * @param attrs Additional search attributes.
	 * @throws Exception
	 */
	public List<Container> getBundlesWildcard(String bsnPattern, String range, Strategy strategyx,
		Map<String, String> attrs) throws Exception {

		if (VERSION_ATTR_SNAPSHOT.equals(range) || VERSION_ATTR_PROJECT.equals(range))
			return Collections.singletonList(new Container(this, bsnPattern, range, TYPE.ERROR, null,
				"Cannot use snapshot or project version with wildcard matches", null, null));
		if (strategyx == Strategy.EXACT)
			return Collections.singletonList(new Container(this, bsnPattern, range, TYPE.ERROR, null,
				"Cannot use exact version strategy with wildcard matches", null, null));

		VersionRange versionRange;
		if (range == null || VERSION_ATTR_LATEST.equals(range))
			versionRange = new VersionRange("0");
		else
			versionRange = new VersionRange(range);

		RepoFilter repoFilter = parseRepoFilter(attrs);

		if (bsnPattern != null) {
			bsnPattern = bsnPattern.trim();
			if (bsnPattern.length() == 0 || bsnPattern.equals("*"))
				bsnPattern = null;
		}

		SortedMap<String, Pair<Version, RepositoryPlugin>> providerMap = new TreeMap<>();

		List<RepositoryPlugin> plugins = workspace.getRepositories();
		for (RepositoryPlugin plugin : plugins) {

			if (repoFilter != null && !repoFilter.match(plugin))
				continue;

			List<String> bsns = plugin.list(bsnPattern);
			if (bsns != null)
				for (String bsn : bsns) {
					SortedSet<Version> versions = plugin.versions(bsn);
					if (versions != null && !versions.isEmpty()) {
						Pair<Version, RepositoryPlugin> currentProvider = providerMap.get(bsn);

						Version candidate;
						switch (strategyx) {
							case HIGHEST :
								candidate = versions.last();
								if (currentProvider == null || candidate.compareTo(currentProvider.getFirst()) > 0) {
									providerMap.put(bsn, new Pair<>(candidate, plugin));
								}
								break;

							case LOWEST :
								candidate = versions.first();
								if (currentProvider == null || candidate.compareTo(currentProvider.getFirst()) < 0) {
									providerMap.put(bsn, new Pair<>(candidate, plugin));
								}
								break;
							default :
								// we shouldn't have reached this point!
								throw new IllegalStateException(
									"Cannot use exact version strategy with wildcard matches");
						}
					}
				}

		}

		List<Container> containers = new ArrayList<>(providerMap.size());

		for (Entry<String, Pair<Version, RepositoryPlugin>> entry : providerMap.entrySet()) {
			String bsn = entry.getKey();
			Version version = entry.getValue()
				.getFirst();
			RepositoryPlugin repo = entry.getValue()
				.getSecond();

			DownloadBlocker downloadBlocker = new DownloadBlocker(this);
			File bundle = repo.get(bsn, version, attrs, downloadBlocker);
			if (bundle != null && !bundle.getName()
				.endsWith(".lib")) {
				containers
					.add(new Container(this, bsn, range, Container.TYPE.REPO, bundle, null, attrs, downloadBlocker));
			}
		}

		return containers;
	}

	static void mergeNames(String names, Set<String> set) {
		StringTokenizer tokenizer = new StringTokenizer(names, ",");
		while (tokenizer.hasMoreTokens())
			set.add(tokenizer.nextToken()
				.trim());
	}

	static String flatten(Set<String> names) {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (String name : names) {
			if (!first)
				builder.append(',');
			builder.append(name);
			first = false;
		}
		return builder.toString();
	}

	static void addToPackageList(Container container, String newPackageNames) {
		Set<String> merged = new HashSet<>();

		String packageListStr = container.getAttributes()
			.get("packages");
		if (packageListStr != null)
			mergeNames(packageListStr, merged);
		if (newPackageNames != null)
			mergeNames(newPackageNames, merged);

		container.putAttribute("packages", flatten(merged));
	}

	/**
	 * The user selected pom in a path. This will place the pom as well as its
	 * dependencies on the list
	 *
	 * @param strategyx the strategy to use.
	 * @param result The list of result containers
	 * @throws Exception anything goes wrong
	 */
	public void doMavenPom(Strategy strategyx, List<Container> result, String action) throws Exception {
		File pomFile = getFile("pom.xml");
		if (!pomFile.isFile())
			msgs.MissingPom();
		else {
			ProjectPom pom = getWorkspace().getMaven()
				.createProjectModel(pomFile);
			if (action == null)
				action = "compile";
			Pom.Scope act = Pom.Scope.valueOf(action);
			Set<Pom> dependencies = pom.getDependencies(act);
			for (Pom sub : dependencies) {
				File artifact = sub.getArtifact();
				Container container = new Container(artifact, null);
				result.add(container);
			}
		}
	}

	/**
	 * Return the full transitive dependencies of this project.
	 *
	 * @return A set of the full transitive dependencies of this project.
	 * @throws Exception
	 */
	public Collection<Project> getDependson() throws Exception {
		prepare();
		return dependenciesFull;
	}

	/**
	 * Return the direct build dependencies of this project.
	 *
	 * @return A set of the direct build dependencies of this project.
	 * @throws Exception
	 */
	public Set<Project> getBuildDependencies() throws Exception {
		prepare();
		return dependenciesBuild;
	}

	/**
	 * Return the direct test dependencies of this project.
	 * <p>
	 * The result includes the direct build dependencies of this project as
	 * well, so the result is a super set of {@link #getBuildDependencies()}.
	 *
	 * @return A set of the test build dependencies of this project.
	 * @throws Exception
	 */
	public Set<Project> getTestDependencies() throws Exception {
		prepare();
		return dependenciesTest;
	}

	/**
	 * Return the full transitive dependents of this project.
	 * <p>
	 * The result includes projects which have build and test dependencies on
	 * this project.
	 * <p>
	 * Since the full transitive dependents of this project is updated during
	 * the computation of other project dependencies, until all projects are
	 * prepared, the dependents result may be partial.
	 *
	 * @return A set of the transitive set of projects which depend on this
	 *         project.
	 * @throws Exception
	 */
	public Set<Project> getDependents() throws Exception {
		prepare();
		return dependents;
	}

	public Collection<Container> getBuildpath() throws Exception {
		prepare();
		return buildpath;
	}

	public Collection<Container> getTestpath() throws Exception {
		prepare();
		return testpath;
	}

	/**
	 * Handle dependencies for paths that are calculated on demand.
	 *
	 * @param testpath2
	 * @param parseTestpath
	 */
	private void justInTime(Collection<Container> path, List<Container> entries, boolean noproject, String name) {
		if (delayRunDependencies && path.isEmpty())
			doPath(path, dependenciesFull, entries, null, noproject, name);
	}

	public Collection<Container> getRunpath() throws Exception {
		prepare();
		justInTime(runpath, parseRunpath(), false, RUNPATH);
		return runpath;
	}

	public Collection<Container> getRunbundles() throws Exception {
		prepare();
		justInTime(runbundles, parseRunbundles(), true, RUNBUNDLES);
		return runbundles;
	}

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

	public File getRunStorage() throws Exception {
		prepare();
		return runstorage;
	}

	public boolean getRunBuilds() {
		boolean result;
		String runBuildsStr = getProperty(Constants.RUNBUILDS);
		if (runBuildsStr == null)
			result = !getPropertiesFile().getName()
				.toLowerCase()
				.endsWith(Constants.DEFAULT_BNDRUN_EXTENSION);
		else
			result = Boolean.parseBoolean(runBuildsStr);
		return result;
	}

	public Collection<File> getSourcePath() throws Exception {
		prepare();
		return sourcepath.keySet();
	}

	public Collection<File> getAllsourcepath() throws Exception {
		prepare();
		return allsourcepath;
	}

	public Collection<Container> getBootclasspath() throws Exception {
		prepare();
		return bootclasspath;
	}

	public File getOutput() throws Exception {
		prepare();
		return output;
	}

	private void doEclipseClasspath() throws Exception {
		EclipseClasspath eclipse = new EclipseClasspath(this, getWorkspace().getBase(), getBase());
		eclipse.setRecurse(false);

		// We get the file directories but in this case we need
		// to tell ant that the project names
		for (File dependent : eclipse.getDependents()) {
			Project required = workspace.getProject(dependent.getName());
			dependenciesFull.add(required);
		}
		for (File f : eclipse.getClasspath()) {
			buildpath.add(new Container(f, null));
		}
		for (File f : eclipse.getBootclasspath()) {
			bootclasspath.add(new Container(f, null));
		}
		for (File f : eclipse.getSourcepath()) {
			sourcepath.put(f, new Attrs());
		}
		allsourcepath.addAll(eclipse.getAllSources());
		output = eclipse.getOutput();
	}

	public String _p_dependson(String args[]) throws Exception {
		return list(args, toFiles(getDependson()));
	}

	private Collection<?> toFiles(Collection<Project> projects) {
		List<File> files = new ArrayList<>();
		for (Project p : projects) {
			files.add(p.getBase());
		}
		return files;
	}

	public String _p_buildpath(String args[]) throws Exception {
		return list(args, getBuildpath());
	}

	public String _p_testpath(String args[]) throws Exception {
		return list(args, getRunpath());
	}

	public String _p_sourcepath(String args[]) throws Exception {
		return list(args, getSourcePath());
	}

	public String _p_allsourcepath(String args[]) throws Exception {
		return list(args, getAllsourcepath());
	}

	public String _p_bootclasspath(String args[]) throws Exception {
		return list(args, getBootclasspath());
	}

	public String _p_output(String args[]) throws Exception {
		if (args.length != 1)
			throw new IllegalArgumentException("${output} should not have arguments");
		return IO.absolutePath(getOutput());
	}

	private String list(String[] args, Collection<?> list) {
		if (args.length > 3)
			throw new IllegalArgumentException(
				"${" + args[0] + "[;<separator>]} can only take a separator as argument, has " + Arrays.toString(args));

		String separator = ",";

		if (args.length == 2) {
			separator = args[1];
		}

		return join(list, separator);
	}

	@Override
	protected Object[] getMacroDomains() {
		return new Object[] {
			workspace
		};
	}

	public File release(String jarName, InputStream jarStream) throws Exception {
		return release(null, jarName, jarStream);
	}

	public URI releaseURI(String jarName, InputStream jarStream) throws Exception {
		return releaseURI(null, jarName, jarStream);
	}

	/**
	 * Release
	 *
	 * @param name The repository name
	 * @param jarName
	 * @param jarStream
	 * @throws Exception
	 */
	public File release(String name, String jarName, InputStream jarStream) throws Exception {
		URI uri = releaseURI(name, jarName, jarStream);
		if (uri != null && uri.getScheme()
			.equals("file")) {
			return new File(uri);
		}
		return null;
	}

	public URI releaseURI(String name, String jarName, InputStream jarStream) throws Exception {
		List<RepositoryPlugin> releaseRepos = getReleaseRepos(name);
		if (releaseRepos.isEmpty()) {
			return null;
		}
		try (ProjectBuilder builder = getBuilder(null)) {
			builder.init();
			RepositoryPlugin releaseRepo = releaseRepos.get(0); // use only
																// first
			// release repo
			return releaseRepo(releaseRepo, builder, jarName, jarStream);
		}
	}

	private URI releaseRepo(RepositoryPlugin releaseRepo, Processor context, String jarName, InputStream jarStream)
		throws Exception {
		logger.debug("release to {}", releaseRepo.getName());
		try {
			PutOptions putOptions = new RepositoryPlugin.PutOptions();
			// TODO find sub bnd that is associated with this thing
			putOptions.context = context;
			PutResult r = releaseRepo.put(jarStream, putOptions);
			logger.debug("Released {} to {} in repository {}", jarName, r.artifact, releaseRepo);
			return r.artifact;
		} catch (Exception e) {
			msgs.Release_Into_Exception_(jarName, releaseRepo, e);
			return null;
		}
	}

	private List<RepositoryPlugin> getReleaseRepos(String names) {
		Parameters repoNames = parseReleaseRepos(names);
		List<RepositoryPlugin> plugins = getPlugins(RepositoryPlugin.class);
		List<RepositoryPlugin> result = new ArrayList<>();
		if (repoNames == null) { // -releaserepo unspecified
			for (RepositoryPlugin plugin : plugins) {
				if (plugin.canWrite()) {
					result.add(plugin);
					break;
				}
			}
			if (result.isEmpty()) {
				msgs.NoNameForReleaseRepository();
			}
			return result;
		}
		repoNames: for (String repoName : repoNames.keySet()) {
			for (RepositoryPlugin plugin : plugins) {
				if (plugin.canWrite() && repoName.equals(plugin.getName())) {
					result.add(plugin);
					continue repoNames;
				}
			}
			msgs.ReleaseRepository_NotFoundIn_(repoName, plugins);
		}
		return result;
	}

	private Parameters parseReleaseRepos(String names) {
		if (names == null) {
			names = mergeProperties(RELEASEREPO);
			if (names == null) {
				return null; // -releaserepo unspecified
			}
		}
		return new Parameters(names, this);
	}

	public void release(boolean test) throws Exception {
		release(null, test);
	}

	/**
	 * Release
	 *
	 * @param name The respository name
	 * @param test Run testcases
	 * @throws Exception
	 */
	public void release(String name, boolean test) throws Exception {
		List<RepositoryPlugin> releaseRepos = getReleaseRepos(name);
		if (releaseRepos.isEmpty()) {
			return;
		}
		logger.debug("release");
		File[] jars = getBuildFiles(false);
		if (jars == null) {
			jars = build(test);
			// If build fails jars will be null
			if (jars == null) {
				logger.debug("no jars built");
				return;
			}
		}
		logger.debug("releasing {} - {}", jars, releaseRepos);

		try (ProjectBuilder builder = getBuilder(null)) {
			builder.init();
			for (RepositoryPlugin releaseRepo : releaseRepos) {
				for (File jar : jars) {
					releaseRepo(releaseRepo, builder, jar.getName(), new BufferedInputStream(IO.stream(jar)));
				}
			}
		}
	}

	/**
	 * Get a bundle from one of the plugin repositories. If an exact version is
	 * required we just return the first repository found (in declaration order
	 * in the build.bnd file).
	 *
	 * @param bsn The bundle symbolic name
	 * @param range The version range
	 * @param strategy set to LOWEST or HIGHEST
	 * @return the file object that points to the bundle or null if not found
	 * @throws Exception when something goes wrong
	 */

	public Container getBundle(String bsn, String range, Strategy strategy, Map<String, String> attrs)
		throws Exception {

		if (range == null)
			range = "0";

		if (VERSION_ATTR_SNAPSHOT.equals(range) || VERSION_ATTR_PROJECT.equals(range)) {
			return getBundleFromProject(bsn, attrs);
		} else if (VERSION_ATTR_HASH.equals(range)) {
			return getBundleByHash(bsn, attrs);
		}

		Strategy useStrategy = strategy;

		if (VERSION_ATTR_LATEST.equals(range)) {
			Container c = getBundleFromProject(bsn, attrs);
			if (c != null)
				return c;

			useStrategy = Strategy.HIGHEST;
		}

		useStrategy = overrideStrategy(attrs, useStrategy);
		RepoFilter repoFilter = parseRepoFilter(attrs);

		List<RepositoryPlugin> plugins = workspace.getRepositories();

		if (useStrategy == Strategy.EXACT) {
			if (!Verifier.isVersion(range))
				return new Container(this, bsn, range, Container.TYPE.ERROR, null,
					bsn + ";version=" + range + " Invalid version", null, null);

			// For an exact range we just iterate over the repos
			// and return the first we find.
			Version version = new Version(range);
			for (RepositoryPlugin plugin : plugins) {
				DownloadBlocker blocker = new DownloadBlocker(this);
				File result = plugin.get(bsn, version, attrs, blocker);
				if (result != null)
					return toContainer(bsn, range, attrs, result, blocker);
			}
		} else {
			VersionRange versionRange = VERSION_ATTR_LATEST.equals(range) ? new VersionRange("0")
				: new VersionRange(range);

			// We have a range search. Gather all the versions in all the repos
			// and make a decision on that choice. If the same version is found
			// in
			// multiple repos we take the first

			SortedMap<Version, RepositoryPlugin> versions = new TreeMap<>();
			for (RepositoryPlugin plugin : plugins) {

				if (repoFilter != null && !repoFilter.match(plugin))
					continue;

				try {
					SortedSet<Version> vs = plugin.versions(bsn);
					if (vs != null) {
						for (Version v : vs) {
							if (!versions.containsKey(v) && versionRange.includes(v))
								versions.put(v, plugin);
						}
					}
				} catch (UnsupportedOperationException ose) {
					// We have a plugin that cannot list versions, try
					// if it has this specific version
					// The main reaosn for this code was the Maven Remote
					// Repository
					// To query, we must have a real version
					if (!versions.isEmpty() && Verifier.isVersion(range)) {
						Version version = new Version(range);
						DownloadBlocker blocker = new DownloadBlocker(this);
						File file = plugin.get(bsn, version, attrs, blocker);
						// and the entry must exist
						// if it does, return this as a result
						if (file != null)
							return toContainer(bsn, range, attrs, file, blocker);
					}
				}
			}

			//
			// We have to augment the list of returned versions
			// with info from the workspace. We use null as a marker
			// to indicate that it is a workspace project
			//

			SortedSet<Version> localVersions = getWorkspace().getWorkspaceRepository()
				.versions(bsn);
			for (Version v : localVersions) {
				if (!versions.containsKey(v) && versionRange.includes(v))
					versions.put(v, null);
			}

			// Verify if we found any, if so, we use the strategy to pick
			// the first or last

			if (!versions.isEmpty()) {
				Version provider = null;

				switch (useStrategy) {
					case HIGHEST :
						provider = versions.lastKey();
						break;

					case LOWEST :
						provider = versions.firstKey();
						break;
					case EXACT :
						// TODO need to handle exact better
						break;
				}
				if (provider != null) {
					RepositoryPlugin repo = versions.get(provider);
					if (repo == null) {
						// A null provider indicates that we have a local
						// project
						return getBundleFromProject(bsn, attrs);
					}

					String version = provider.toString();
					DownloadBlocker blocker = new DownloadBlocker(this);
					File result = repo.get(bsn, provider, attrs, blocker);
					if (result != null)
						return toContainer(bsn, version, attrs, result, blocker);
				} else {
					msgs.FoundVersions_ForStrategy_ButNoProvider(versions, useStrategy);
				}
			}
		}

		//
		// If we get this far we ran into an error somewhere
		//

		return new Container(this, bsn, range, Container.TYPE.ERROR, null,
			bsn + ";version=" + range + " Not found in " + plugins, null, null);

	}

	/**
	 * @param attrs
	 * @param useStrategy
	 */
	protected Strategy overrideStrategy(Map<String, String> attrs, Strategy useStrategy) {
		if (attrs != null) {
			String overrideStrategy = attrs.get("strategy");

			if (overrideStrategy != null) {
				if ("highest".equalsIgnoreCase(overrideStrategy))
					useStrategy = Strategy.HIGHEST;
				else if ("lowest".equalsIgnoreCase(overrideStrategy))
					useStrategy = Strategy.LOWEST;
				else if ("exact".equalsIgnoreCase(overrideStrategy))
					useStrategy = Strategy.EXACT;
			}
		}
		return useStrategy;
	}

	private static class RepoFilter {
		private Pattern[] patterns;

		RepoFilter(Pattern[] patterns) {
			this.patterns = patterns;
		}

		boolean match(RepositoryPlugin repo) {
			if (patterns == null)
				return true;
			for (Pattern pattern : patterns) {
				if (pattern.matcher(repo.getName())
					.matches())
					return true;
			}
			return false;
		}
	}

	protected RepoFilter parseRepoFilter(Map<String, String> attrs) {
		if (attrs == null)
			return null;
		String patternStr = attrs.get("repo");
		if (patternStr == null)
			return null;

		List<Pattern> patterns = new LinkedList<>();
		QuotedTokenizer tokenize = new QuotedTokenizer(patternStr, ",");

		String token = tokenize.nextToken();
		while (token != null) {
			patterns.add(Glob.toPattern(token));
			token = tokenize.nextToken();
		}
		return new RepoFilter(patterns.toArray(new Pattern[0]));
	}

	/**
	 * @param bsn
	 * @param range
	 * @param attrs
	 * @param result
	 */
	protected Container toContainer(String bsn, String range, Map<String, String> attrs, File result,
		DownloadBlocker db) {
		File f = result;
		if (f == null) {
			msgs.ConfusedNoContainerFile();
			f = new File("was null");
		}
		Container container;
		if (f.getName()
			.endsWith("lib"))
			container = new Container(this, bsn, range, Container.TYPE.LIBRARY, f, null, attrs, db);
		else
			container = new Container(this, bsn, range, Container.TYPE.REPO, f, null, attrs, db);

		return container;
	}

	/**
	 * Look for the bundle in the workspace. The premise is that the bsn must
	 * start with the project name.
	 *
	 * @param bsn The bsn
	 * @param attrs Any attributes
	 * @throws Exception
	 */
	private Container getBundleFromProject(String bsn, Map<String, String> attrs) throws Exception {
		String pname = bsn;
		while (true) {
			Project p = getWorkspace().getProject(pname);
			if (p != null && p.isValid()) {
				Container c = p.getDeliverable(bsn, attrs);
				return c;
			}

			int n = pname.lastIndexOf('.');
			if (n <= 0)
				return null;
			pname = pname.substring(0, n);
		}
	}

	private Container getBundleByHash(String bsn, Map<String, String> attrs) throws Exception {
		String hashStr = attrs.get("hash");
		String algo = SHA_256;

		String hash = hashStr;
		int colonIndex = hashStr.indexOf(':');
		if (colonIndex > -1) {
			algo = hashStr.substring(0, colonIndex);
			int afterColon = colonIndex + 1;
			hash = (colonIndex < hashStr.length()) ? hashStr.substring(afterColon) : "";
		}

		for (RepositoryPlugin plugin : workspace.getRepositories()) {
			// The plugin *may* understand version=hash directly
			DownloadBlocker blocker = new DownloadBlocker(this);
			File result = plugin.get(bsn, Version.LOWEST, Collections.unmodifiableMap(attrs), blocker);

			// If not, and if the repository implements the OSGi Repository
			// Service, use a capability search on the osgi.content namespace.
			if (result == null && plugin instanceof Repository) {
				Repository repo = (Repository) plugin;

				if (!SHA_256.equals(algo))
					// R5 repos only support SHA-256
					continue;

				Requirement contentReq = new CapReqBuilder(ContentNamespace.CONTENT_NAMESPACE)
					.filter(String.format("(%s=%s)", ContentNamespace.CONTENT_NAMESPACE, hash))
					.buildSyntheticRequirement();
				Set<Requirement> reqs = Collections.singleton(contentReq);

				Map<Requirement, Collection<Capability>> providers = repo.findProviders(reqs);
				Collection<Capability> caps = providers != null ? providers.get(contentReq) : null;
				if (caps != null && !caps.isEmpty()) {
					Capability cap = caps.iterator()
						.next();

					IdentityCapability idCap = ResourceUtils.getIdentityCapability(cap.getResource());
					Map<String, Object> idAttrs = idCap.getAttributes();
					String id = (String) idAttrs.get(IdentityNamespace.IDENTITY_NAMESPACE);
					Object version = idAttrs.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
					Version bndVersion = version != null ? Version.parseVersion(version.toString()) : Version.LOWEST;

					if (!bsn.equals(id)) {
						String error = String.format("Resource with requested hash does not match ID '%s' [hash: %s]",
							bsn, hashStr);
						return new Container(this, bsn, "hash", Container.TYPE.ERROR, null, error, null, null);
					}

					result = plugin.get(id, bndVersion, null, blocker);
				}
			}

			if (result != null)
				return toContainer(bsn, "hash", attrs, result, blocker);
		}

		// If we reach this far, none of the repos found the resource.
		return new Container(this, bsn, "hash", Container.TYPE.ERROR, null,
			"Could not find resource by content hash " + hashStr, null, null);
	}

	/**
	 * Deploy the file (which must be a bundle) into the repository.
	 *
	 * @param name The repository name
	 * @param file bundle
	 */
	public void deploy(String name, File file) throws Exception {
		List<RepositoryPlugin> plugins = getPlugins(RepositoryPlugin.class);

		RepositoryPlugin rp = null;
		for (RepositoryPlugin plugin : plugins) {
			if (!plugin.canWrite()) {
				continue;
			}
			if (name == null) {
				rp = plugin;
				break;
			} else if (name.equals(plugin.getName())) {
				rp = plugin;
				break;
			}
		}

		if (rp != null) {
			try {
				rp.put(new BufferedInputStream(IO.stream(file)), new RepositoryPlugin.PutOptions());
				return;
			} catch (Exception e) {
				msgs.DeployingFile_On_Exception_(file, rp.getName(), e);
			}
			return;
		}
		logger.debug("No repo found {}", file);
		throw new IllegalArgumentException("No repository found for " + file);
	}

	/**
	 * Deploy the file (which must be a bundle) into the repository.
	 *
	 * @param file bundle
	 */
	public void deploy(File file) throws Exception {
		String name = getProperty(Constants.DEPLOYREPO);
		deploy(name, file);
	}

	/**
	 * Deploy the current project to a repository
	 *
	 * @throws Exception
	 */
	public void deploy() throws Exception {
		Parameters deploy = new Parameters(getProperty(DEPLOY), this);
		if (deploy.isEmpty()) {
			warning("Deploying but %s is not set to any repo", DEPLOY);
			return;
		}
		File[] outputs = getBuildFiles();
		for (File output : outputs) {
			for (Deploy d : getPlugins(Deploy.class)) {
				logger.debug("Deploying {} to: {}", output.getName(), d);
				try {
					if (d.deploy(this, output.getName(), new BufferedInputStream(IO.stream(output))))
						logger.debug("deployed {} successfully to {}", output, d);
				} catch (Exception e) {
					msgs.Deploying(e);
				}
			}
		}
	}

	/**
	 * Macro access to the repository
	 * ${repo;<bsn>[;<versionrange>[;<lowest|highest|exact>]]}
	 */

	static String _repoHelp = "${repo;<bsn>[;<versionrange>[;('HIGHEST'|'LOWEST'|'EXACT')]]}";

	public String _repo(String args[]) throws Exception {
		if (args.length < 2) {
			msgs.RepoTooFewArguments(_repoHelp, args);
			return null;
		}

		String bsns = args[1];
		String version = null;
		Strategy strategy = Strategy.HIGHEST;

		if (args.length > 2) {
			version = args[2];
			if (args.length == 4) {
				if (args[3].equalsIgnoreCase("HIGHEST"))
					strategy = Strategy.HIGHEST;
				else if (args[3].equalsIgnoreCase("LOWEST"))
					strategy = Strategy.LOWEST;
				else if (args[3].equalsIgnoreCase("EXACT"))
					strategy = Strategy.EXACT;
				else
					msgs.InvalidStrategy(_repoHelp, args);
			}
		}

		Collection<String> parts = split(bsns);
		List<String> paths = new ArrayList<>();

		for (String bsn : parts) {
			Container container = getBundle(bsn, version, strategy, null);
			if (container.getError() != null) {
				error("${repo} macro refers to an artifact %s-%s (%s) that has an error: %s", bsn, version, strategy,
					container.getError());
			} else
				add(paths, container);
		}
		return join(paths);
	}

	private void add(List<String> paths, Container container) throws Exception {
		if (container.getType() == Container.TYPE.LIBRARY) {
			List<Container> members = container.getMembers();
			for (Container sub : members) {
				add(paths, sub);
			}
		} else {
			if (container.getError() == null)
				paths.add(IO.absolutePath(container.getFile()));
			else {
				paths.add("<<${repo} = " + container.getBundleSymbolicName() + "-" + container.getVersion() + " : "
					+ container.getError() + ">>");

				if (isPedantic()) {
					warning("Could not expand repo path request: %s ", container);
				}
			}

		}
	}

	public File getTarget() throws Exception {
		prepare();
		return target;
	}

	/**
	 * This is the external method that will pre-build any dependencies if it is
	 * out of date.
	 *
	 * @param underTest
	 * @throws Exception
	 */
	public File[] build(boolean underTest) throws Exception {
		if (isNoBundles())
			return null;

		if (getProperty("-nope") != null) {
			warning("Please replace -nope with %s", NOBUNDLES);
			return null;
		}

		logger.debug("building {}", this);

		File[] files = buildLocal(underTest);
		install(files);

		return files;
	}

	private void install(File[] files) throws Exception {
		if (files == null)
			return;
		try (ProjectBuilder builder = getBuilder(null)) {
			builder.init();
			Parameters p = getInstallRepositories();
			for (Map.Entry<String, Attrs> e : p.entrySet()) {
				RepositoryPlugin rp = getWorkspace().getRepository(e.getKey());
				if (rp != null) {
					for (File f : files) {
						install(rp, builder, f, e.getValue());
					}
				} else
					warning("No such repository to install into: %s", e.getKey());
			}
		}
	}

	public Parameters getInstallRepositories() {
		if (data.installRepositories == null) {
			data.installRepositories = new Parameters(mergeProperties(BUILDREPO), this);
		}
		return data.installRepositories;
	}

	private void install(RepositoryPlugin repo, Processor context, File f, Attrs value) throws Exception {
		try (Processor p = new Processor(context)) {
			p.getProperties()
				.putAll(value);
			PutOptions options = new PutOptions();
			options.context = p;
			try (InputStream in = IO.stream(f)) {
				repo.put(in, options);
			} catch (Exception e) {
				exception(e, "Cannot install %s into %s because %s", f, repo.getName(), e);
			}
		}
	}

	/**
	 * Return the files
	 */

	public File[] getFiles() {
		return files;
	}

	/**
	 * Check if this project needs building. This is defined as:
	 */
	public boolean isStale() throws Exception {
		Set<Project> visited = new HashSet<>();
		return isStale(visited);
	}

	boolean isStale(Set<Project> visited) throws Exception {
		// When we do not generate anything ...
		if (isNoBundles())
			return false;

		if (!visited.add(this)) {
			return false;
		}

		long buildTime = 0;

		File[] files = getBuildFiles(false);
		if (files == null)
			return true;

		for (File f : files) {
			if (f.lastModified() < lastModified())
				return true;

			if (buildTime < f.lastModified())
				buildTime = f.lastModified();
		}

		for (Project dependency : getDependson()) {
			if (dependency == this)
				continue;

			if (dependency.isNoBundles()) {
				continue;
			}

			if (dependency.isStale(visited)) {
				return true;
			}

			File[] deps = dependency.getBuildFiles(false);
			if (deps == null) {
				return true;
			}
			for (File f : deps) {
				if (buildTime < f.lastModified()) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * This method must only be called when it is sure that the project has been
	 * build before in the same session. It is a bit yucky, but ant creates
	 * different class spaces which makes it hard to detect we already build it.
	 * This method remembers the files in the appropriate instance vars.
	 */

	public File[] getBuildFiles() throws Exception {
		return getBuildFiles(true);
	}

	public File[] getBuildFiles(boolean buildIfAbsent) throws Exception {
		File[] current = files;
		if (current != null) {
			return current;
		}

		File bfs = new File(getTarget(), BUILDFILES);
		if (bfs.isFile()) {
			try (BufferedReader rdr = IO.reader(bfs)) {
				List<File> list = newList();
				for (String s = rdr.readLine(); s != null; s = rdr.readLine()) {
					s = s.trim();
					File ff = getFile(getTarget(), s);
					if (!ff.isFile()) {
						// Originally we warned the user
						// but lets just rebuild. That way
						// the error is not noticed but
						// it seems better to correct,
						// See #154
						rdr.close();
						IO.delete(bfs);
						return files = buildIfAbsent ? buildLocal(false) : null;
					}
					list.add(ff);
				}
				return files = list.toArray(new File[0]);
			}
		}
		return files = buildIfAbsent ? buildLocal(false) : null;
	}

	/**
	 * Build without doing any dependency checking. Make sure any dependent
	 * projects are built first.
	 *
	 * @param underTest
	 * @throws Exception
	 */
	public File[] buildLocal(boolean underTest) throws Exception {
		if (isNoBundles()) {
			return files = null;
		}

		preBuildChecks();

		versionMap.clear();
		getMakefile().make();

		File[] buildfiles = getBuildFiles(false);
		File bfs = new File(getTarget(), BUILDFILES);
		files = null;

		//
		// #761 tstamp can vary between invocations in one build
		// Macro can handle a @tstamp time so we freeze the time at
		// the start of the build. We do this carefully so someone higher
		// up the chain can actually freeze the time longer
		//
		boolean tstamp = false;
		if (getProperty(TSTAMP) == null) {
			setProperty(TSTAMP, Long.toString(System.currentTimeMillis()));
			tstamp = true;
		}

		try (ProjectBuilder builder = getBuilder(null)) {
			if (underTest)
				builder.setProperty(Constants.UNDERTEST, "true");
			Jar jars[] = builder.builds();

			getInfo(builder);

			if (isPedantic() && !unreferencedClasspathEntries.isEmpty()) {
				warning("Unreferenced class path entries %s", unreferencedClasspathEntries.keySet());
			}

			if (!isOk()) {
				return null;
			}

			//
			// Save the JARs
			//

			Set<File> buildFilesSet = Create.set();
			long lastModified = 0L;
			for (Jar jar : jars) {
				File file = saveBuild(jar);
				if (file == null) {
					error("Could not save %s", jar.getName());
				} else {
					buildFilesSet.add(file);
					if (lastModified < jar.lastModified()) {
						lastModified = jar.lastModified();
					}
				}
			}

			if (!isOk())
				return null;

			//
			// Handle the exports
			//

			Instructions runspec = new Instructions(getProperty(EXPORT));
			Set<Instruction> missing = new LinkedHashSet<>();

			Map<File, List<Attrs>> selectedRunFiles = runspec.select(getBase(), Function.identity(), missing);

			if (!missing.isEmpty()) {
				// this is a new error :-( so make it a warning
				warning("-export entries %s could not be found", missing);
			}
			if (selectedRunFiles.containsKey(getPropertiesFile())) {
				error("Cannot export the same file that contains the -export instruction %s", getPropertiesFile());
				return null;
			}

			Map<File, Resource> exports = builder.doExports(selectedRunFiles);
			getInfo(builder);

			if (!isOk())
				return null;

			for (Map.Entry<File, Resource> ee : exports.entrySet()) {
				File outputFile = ee.getKey();
				File actual = write(f -> {
					IO.copy(ee.getValue()
						.openInputStream(), f);
				}, outputFile);

				if (actual != null) {
					buildFilesSet.add(actual);
				} else {
					error("Could not save %s", ee.getKey());
				}
			}

			if (!isOk()) {
				return null;
			}

			boolean bfsWrite = !bfs.exists() || (lastModified > bfs.lastModified());
			if (buildfiles != null) {
				Set<File> removed = Create.set(buildfiles);
				if (!removed.equals(buildFilesSet)) {
					bfsWrite = true;
					removed.removeAll(buildFilesSet);
					for (File remove : removed) {
						IO.delete(remove);
						getWorkspace().changedFile(remove);
					}
				}
			}
			// Write out the filenames in the buildfiles file
			// so we can get them later even in another process
			if (bfsWrite) {
				try (PrintWriter fw = IO.writer(bfs)) {
					for (File f : buildFilesSet) {
						fw.write(IO.absolutePath(f));
						fw.write('\n');
					}
				}
				getWorkspace().changedFile(bfs);
			}
			bfs = null; // avoid delete in finally block

			return files = buildFilesSet.toArray(new File[0]);
		} finally {
			if (tstamp)
				unsetProperty(TSTAMP);
			if (bfs != null) {
				IO.delete(bfs); // something went wrong, so delete
				getWorkspace().changedFile(bfs);
			}
		}
	}

	/**
	 * Answer if this project does not have any output
	 */
	public boolean isNoBundles() {
		return isTrue(getProperty(NOBUNDLES));
	}

	public File saveBuild(Jar jar) throws Exception {
		try {
			File outputFile = getOutputFile(jar.getName(), jar.getVersion());

			reportNewer(outputFile.lastModified(), jar);
			File logicalFile = write(jar::write, outputFile);

			logger.debug("{} ({}) {}", jar.getName(), outputFile.getName(), jar.getResources()
				.size());
			//
			// For maven we've got the shitty situation that the
			// files in the generated directories have an ever changing
			// version number so it is hard to refer to them in test cases
			// and from for example bndtools if you want to refer to the
			// latest so the following code attempts to create a link to the
			// output file if this is using some other naming scheme,
			// creating a constant name. Would probably be more logical to
			// always output in the canonical name and then create a link to
			// the desired name but not sure how much that would break BJ's
			// maven handling that caused these versioned JARs
			//

			File canonical = new File(getTarget(), jar.getName() + ".jar");
			if (!canonical.equals(logicalFile)) {
				IO.delete(canonical);
				if (!IO.createSymbolicLink(canonical, outputFile)) {
					//
					// As alternative, we copy the file
					//
					IO.copy(outputFile, canonical);
				}
				getWorkspace().changedFile(canonical);
			}
			return logicalFile;
		} finally {
			jar.close();
		}
	}

	private File write(ConsumerWithException<File> jar, File outputFile)
		throws IOException, InterruptedException, Exception {
		File logicalFile = outputFile;

		File fp = outputFile.getParentFile();
		if (!fp.isDirectory()) {
			IO.mkdirs(fp);
		}

		// On windows we sometimes cannot delete a file because
		// someone holds a lock in our or another process. So if
		// we set the -overwritestrategy flag we use an avoiding
		// strategy.
		// We will always write to a temp file name. Basically the
		// calculated name + a variable suffix. We then create
		// a link with the constant name to this variable name.
		// This allows us to pick a different suffix when we cannot
		// delete the file. Yuck, but better than the alternative.

		String overwritestrategy = getProperty("-x-overwritestrategy", "classic");
		swtch: switch (overwritestrategy) {
			case "delay" :
				for (int i = 0; i < 10; i++) {
					try {
						IO.deleteWithException(outputFile);
						jar.accept(outputFile);
						break swtch;
					} catch (Exception e) {
						Thread.sleep(500);
					}
				}

				// Execute normal case to get classic behavior
				// FALL THROUGH

			case "classic" :
				IO.deleteWithException(outputFile);
				jar.accept(outputFile);
				break swtch;

			case "gc" :
				try {
					IO.deleteWithException(outputFile);
				} catch (Exception e) {
					System.gc();
					System.runFinalization();
					IO.deleteWithException(outputFile);
				}
				jar.accept(outputFile);
				break swtch;

			case "windows-only-disposable-names" :
				if (!IO.isWindows()) {
					IO.deleteWithException(outputFile);
					jar.accept(outputFile);
					break swtch;
				}
				// Fall through

			case "disposable-names" :
				int suffix = 0;
				while (true) {
					outputFile = new File(outputFile.getParentFile(), outputFile.getName() + "-" + suffix);
					IO.delete(outputFile);
					if (!outputFile.isFile()) {
						// Succeeded to delete the file
						jar.accept(outputFile);
						Files.createSymbolicLink(logicalFile.toPath(), outputFile.toPath());
						break;
					} else {
						warning("Could not delete build file {} ", overwritestrategy);
						logger.warn("Cannot delete file {} but that should be ok", outputFile);
					}
					suffix++;
				}
				break swtch;

			default :
				error(
					"Invalid value for -x-overwritestrategy: %s, expected classic, delay, gc, windows-only-disposable-names, disposable-names",
					overwritestrategy);
				IO.deleteWithException(outputFile);
				jar.accept(outputFile);
				break swtch;

		}

		getWorkspace().changedFile(outputFile);
		if (!outputFile.equals(logicalFile))
			getWorkspace().changedFile(logicalFile);
		return logicalFile;
	}

	/**
	 * Calculate the file for a JAR. The default name is bsn.jar, but this can
	 * be overridden with an
	 *
	 * @throws Exception
	 */
	public File getOutputFile(String bsn, String version) throws Exception {
		if (version == null)
			version = "0";
		try (Processor scoped = new Processor(this)) {
			scoped.setProperty("@bsn", bsn);
			scoped.setProperty("@version", version);
			String path = scoped.getProperty(OUTPUTMASK, bsn + ".jar");
			return IO.getFile(getTarget(), path);
		}
	}

	public File getOutputFile(String bsn) throws Exception {
		return getOutputFile(bsn, "0.0.0");
	}

	private void reportNewer(long lastModified, Jar jar) {
		if (isTrue(getProperty(Constants.REPORTNEWER))) {
			StringBuilder sb = new StringBuilder();
			String del = "Newer than " + new Date(lastModified);
			for (Map.Entry<String, Resource> entry : jar.getResources()
				.entrySet()) {
				if (entry.getValue()
					.lastModified() > lastModified) {
					sb.append(del);
					del = ", \n     ";
					sb.append(entry.getKey());
				}
			}
			if (sb.length() > 0)
				warning("%s", sb.toString());
		}
	}

	/**
	 * Refresh if we are based on stale data. This also implies our workspace.
	 */
	@Override
	public boolean refresh() {
		versionMap.clear();
		data = new RefreshData();
		boolean changed = false;
		if (isCnf()) {
			changed = workspace.refresh();
		}
		return super.refresh() || changed;
	}

	public boolean isCnf() {
		try {
			return getBase().getCanonicalPath()
				.equals(getWorkspace().getBuildDir()
					.getCanonicalPath());
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public void propertiesChanged() {
		super.propertiesChanged();
		setChanged();
		makefile = null;
		versionMap.clear();
		data = new RefreshData();
	}

	public String getName() {
		return getBase().getName();
	}

	public Map<String, Action> getActions() {
		Map<String, Action> all = newMap();
		fillActions(all);
		getWorkspace().fillActions(all);

		Map<String, Action> actions = MapStream.of(all)
			.mapKey(key -> getReplacer().process(key))
			.filterKey(Strings::nonNullOrTrimmedEmpty)
			.collect(MapStream.toMap((u, v) -> v, LinkedHashMap::new));
		return actions;
	}

	public void fillActions(Map<String, Action> all) {
		List<NamedAction> plugins = getPlugins(NamedAction.class);
		for (NamedAction a : plugins)
			all.put(a.getName(), a);

		Parameters actions = new Parameters(getProperty("-actions", DEFAULT_ACTIONS), this);
		for (Entry<String, Attrs> entry : actions.entrySet()) {
			String key = Processor.removeDuplicateMarker(entry.getKey());
			Action action;

			if (entry.getValue()
				.get("script") != null) {
				// TODO check for the type
				action = new ScriptAction(entry.getValue()
					.get("type"),
					entry.getValue()
						.get("script"));
			} else {
				action = new ReflectAction(key);
			}
			String label = entry.getValue()
				.get("label");
			all.put(label.toLowerCase(), action);
		}
	}

	public void release() throws Exception {
		release(false);
	}

	/**
	 * Export this project via the exporters. The return is the calculated name
	 * (can be overridden with the option `name`, which must be the basename
	 * since the extension is defined by the exporter)
	 *
	 * @param type an exporter type like `bnd.executablejar` or null as default
	 *            `bnd.executablejar.pack`, the original export function
	 * @param options parameters to the exporters
	 * @return and entry with the name and resource for the export
	 * @throws Exception
	 */
	public Map.Entry<String, Resource> export(String type, Map<String, String> options) throws Exception {

		if (options == null) {
			options = Collections.emptyMap();
		}

		if (type == null) {
			type = options.getOrDefault(Constants.EXPORT_TYPE, "bnd.executablejar.pack");
		}

		Parameters exportTypes = parseHeader(getProperty(Constants.EXPORTTYPE));
		Map<String, String> attrs = exportTypes.get(type);
		if (attrs != null) {
			attrs.putAll(options);
			options = attrs;
		}

		Exporter exporter = getExporter(type);
		if (exporter == null) {
			error("No exporter for %s", type);
			return null;
		}

		Entry<String, Resource> entry = exporter.export(type, this, options);
		if (entry == null) {
			error("Export failed %s for type %s", this, type);
			return null;
		}

		return entry;
	}

	private Exporter getExporter(String type) {
		List<Exporter> exporters = getPlugins(Exporter.class);
		for (Exporter e : exporters) {
			for (String exporterType : e.getTypes()) {
				if (type.equals(exporterType)) {
					return e;
				}
			}
		}
		return null;
	}

	/**
	 * The keep flag is really awkward since it overrides the -runkeep flag in
	 * the file. Use doExport instead
	 */
	@Deprecated
	public void export(String runFilePath, boolean keep, File output) throws Exception {
		Map<String, String> options = Collections.singletonMap("keep", Boolean.toString(keep));
		Entry<String, Resource> export;
		if (runFilePath == null || runFilePath.length() == 0 || ".".equals(runFilePath)) {
			clear();
			export = export(ExecutableJarExporter.EXECUTABLE_JAR, options);
		} else {
			File runFile = IO.getFile(getBase(), runFilePath);
			if (!runFile.isFile())
				throw new IOException(
					String.format("Run file %s does not exist (or is not a file).", IO.absolutePath(runFile)));
			try (Run run = new Run(getWorkspace(), getBase(), runFile)) {
				export = run.export(ExecutableJarExporter.EXECUTABLE_JAR, options);
				getInfo(run);
			}
		}
		if (export != null) {
			try (JarResource r = (JarResource) export.getValue()) {
				r.getJar()
					.write(output);
			}
		}
	}

	/**
	 * @since 2.4
	 */
	public void exportRunbundles(String runFilePath, File outputDir) throws Exception {
		Map<String, String> options = Collections.emptyMap();
		Entry<String, Resource> export;
		if (runFilePath == null || runFilePath.length() == 0 || ".".equals(runFilePath)) {
			clear();
			export = export(RunbundlesExporter.RUNBUNDLES, options);
		} else {
			File runFile = IO.getFile(getBase(), runFilePath);
			if (!runFile.isFile())
				throw new IOException(
					String.format("Run file %s does not exist (or is not a file).", IO.absolutePath(runFile)));
			try (Run run = new Run(getWorkspace(), getBase(), runFile)) {
				export = run.export(RunbundlesExporter.RUNBUNDLES, options);
				getInfo(run);
			}
		}
		if (export != null) {
			try (JarResource r = (JarResource) export.getValue()) {
				r.getJar()
					.writeFolder(outputDir);
			}
		}
	}

	/**
	 * Release.
	 *
	 * @param name The repository name
	 * @throws Exception
	 */
	public void release(String name) throws Exception {
		release(name, false);
	}

	public void clean() throws Exception {
		clean(getTargetDir(), "target");
		clean(getSrcOutput(), "source output");
		clean(getTestOutput(), "test output");
	}

	void clean(File dir, String type) throws IOException {
		if (!dir.exists())
			return;

		String basePath = getBase().getCanonicalPath();
		String dirPath = dir.getCanonicalPath();
		if (!dirPath.startsWith(basePath)) {
			logger.debug("path outside the project dir {}", type);
			return;
		}

		if (dirPath.length() == basePath.length()) {
			error("Trying to delete the project directory for %s", type);
			return;
		}

		IO.delete(dir);
		if (dir.exists()) {
			error("Trying to delete %s (%s), but failed", dir, type);
			return;
		}

		IO.mkdirs(dir);
	}

	public File[] build() throws Exception {
		return build(false);
	}

	private Makefile getMakefile() {
		if (makefile == null) {
			makefile = new Makefile(this);
		}
		return makefile;
	}

	public void run() throws Exception {
		try (ProjectLauncher pl = getProjectLauncher()) {
			pl.setTrace(isTrace() || isRunTrace());
			pl.launch();
		}
	}

	public boolean isRunTrace() {
		return isTrue(getProperty(RUNTRACE));
	}

	public void runLocal() throws Exception {
		try (ProjectLauncher pl = getProjectLauncher()) {
			pl.setTrace(isTrace() || isRunTrace());
			pl.start(null);
		}
	}

	public void test() throws Exception {
		test(null);
	}

	public void test(List<String> tests) throws Exception {
		String testcases = getProperties().getProperty(Constants.TESTCASES);
		if (testcases == null) {
			warning("No %s set", Constants.TESTCASES);
			return;
		}
		clear();

		test(null, tests);
	}

	public void test(File reportDir, List<String> tests) throws Exception {
		ProjectTester tester = getProjectTester();
		if (reportDir != null) {
			logger.debug("Setting reportDir {}", reportDir);
			IO.delete(reportDir);
			tester.setReportDir(reportDir);
		}
		if (tests != null) {
			logger.debug("Adding tests {}", tests);
			for (String test : tests) {
				tester.addTest(test);
			}
		}
		tester.prepare();

		if (!isOk()) {
			logger.error("Tests not run because project has errors");
			return;
		}
		int errors = tester.test();
		if (errors == 0) {
			logger.info("No Errors");
		} else {
			if (errors > 0) {
				logger.info("{} Error(s)", errors);
			} else {
				logger.info("Error {}", errors);
			}
		}
	}

	/**
	 * Run JUnit
	 *
	 * @throws Exception
	 */
	public void junit() throws Exception {
		@SuppressWarnings("resource")
		JUnitLauncher launcher = new JUnitLauncher(this);
		launcher.updateFromProject();
		launcher.launch();
	}

	/**
	 * This methods attempts to turn any jar into a valid jar. If this is a
	 * bundle with manifest, a manifest is added based on defaults. If it is a
	 * bundle, but not r4, we try to add the r4 headers.
	 *
	 * @throws Exception
	 */
	public Jar getValidJar(File f) throws Exception {
		Jar jar = new Jar(f);
		return getValidJar(jar, IO.absolutePath(f));
	}

	public Jar getValidJar(URL url) throws Exception {
		try (Resource resource = Resource.fromURL(url, getPlugin(HttpClient.class))) {
			Jar jar = Jar.fromResource(url.getFile()
				.replace('/', '.'), resource);
			return getValidJar(jar, url.toString());
		}
	}

	public Jar getValidJar(Jar jar, String id) throws Exception {
		Manifest manifest = jar.getManifest();
		if (manifest == null) {
			logger.debug("Wrapping with all defaults");
			Builder b = new Builder(this);
			this.addClose(b);
			b.addClasspath(jar);
			b.setProperty("Bnd-Message", "Wrapped from " + id + "because lacked manifest");
			b.setProperty(Constants.EXPORT_PACKAGE, "*");
			b.setProperty(Constants.IMPORT_PACKAGE, "*;resolution:=optional");
			jar = b.build();
		} else if (manifest.getMainAttributes()
			.getValue(Constants.BUNDLE_MANIFESTVERSION) == null) {
			logger.debug("Not a release 4 bundle, wrapping with manifest as source");
			Builder b = new Builder(this);
			this.addClose(b);
			b.addClasspath(jar);
			b.setProperty(Constants.PRIVATE_PACKAGE, "*");
			b.mergeManifest(manifest);
			String imprts = manifest.getMainAttributes()
				.getValue(Constants.IMPORT_PACKAGE);
			if (imprts == null)
				imprts = "";
			else
				imprts += ",";
			imprts += "*;resolution=optional";

			b.setProperty(Constants.IMPORT_PACKAGE, imprts);
			b.setProperty("Bnd-Message", "Wrapped from " + id + "because had incomplete manifest");
			jar = b.build();
		}
		return jar;
	}

	public String _project(@SuppressWarnings("unused") String args[]) {
		return IO.absolutePath(getBase());
	}

	/**
	 * Bump the version of this project. First check the main bnd file. If this
	 * does not contain a version, check the include files. If they still do not
	 * contain a version, then check ALL the sub builders. If not, add a version
	 * to the main bnd file.
	 *
	 * @param mask the mask for bumping, see {@link Macro#_version(String[])}
	 * @throws Exception
	 */
	public void bump(String mask) throws Exception {
		String pattern = "(" + Constants.BUNDLE_VERSION + "\\s*(:|=)\\s*)(([0-9]+(\\.[0-9]+(\\.[0-9]+)?)?))";
		String replace = "$1${version;" + mask + ";$3}";
		try {
			// First try our main bnd file
			if (replace(getPropertiesFile(), pattern, replace))
				return;

			logger.debug("no version in bnd.bnd");

			// Try the included filed in reverse order (last has highest
			// priority)
			for (Iterator<File> iter = new ArrayDeque<>(getIncluded()).descendingIterator(); iter.hasNext();) {
				File file = iter.next();
				if (replace(file, pattern, replace)) {
					logger.debug("replaced version in file {}", file);
					return;
				}
			}
			logger.debug("no version in included files");

			boolean found = false;

			// Replace in all sub builders.
			try (ProjectBuilder b = getBuilder(null)) {
				for (Builder sub : b.getSubBuilders()) {
					found |= replace(sub.getPropertiesFile(), pattern, replace);
				}
			}

			if (!found) {
				logger.debug("no version in sub builders, add it to bnd.bnd");
				String bndfile = IO.collect(getPropertiesFile());
				bndfile += "\n# Added by by bump\n" + Constants.BUNDLE_VERSION + ": 0.0.0\n";
				IO.store(bndfile, getPropertiesFile());
			}
		} finally {
			forceRefresh();
		}
	}

	boolean replace(File f, String pattern, String replacement) throws IOException {
		final Macro macro = getReplacer();
		Sed sed = new Sed(line -> macro.process(line), f);
		sed.replace(pattern, replacement);
		return sed.doIt() > 0;
	}

	public void bump() throws Exception {
		bump(getProperty(BUMPPOLICY, "=+0"));
	}

	public void action(String command) throws Exception {
		action(command, new Object[0]);
	}

	public void action(String command, Object... args) throws Exception {
		Map<String, Action> actions = getActions();

		Action a = actions.get(command);
		if (a == null)
			a = new ReflectAction(command);

		before(this, command);
		try {
			if (args.length == 0)
				a.execute(this, command);
			else
				a.execute(this, args);
		} catch (Exception t) {
			after(this, command, t);
			throw t;
		}
	}

	/**
	 * Run all before command plugins
	 */
	void before(@SuppressWarnings("unused") Project p, String a) {
		List<CommandPlugin> testPlugins = getPlugins(CommandPlugin.class);
		for (CommandPlugin testPlugin : testPlugins) {
			testPlugin.before(this, a);
		}
	}

	/**
	 * Run all after command plugins
	 */
	void after(@SuppressWarnings("unused") Project p, String a, Throwable t) {
		List<CommandPlugin> testPlugins = getPlugins(CommandPlugin.class);
		for (int i = testPlugins.size() - 1; i >= 0; i--) {
			testPlugins.get(i)
				.after(this, a, t);
		}
	}

	public void refreshAll() {
		workspace.refresh();
		refresh();
	}

	@SuppressWarnings("unchecked")
	public void script(@SuppressWarnings("unused") String type, String script) throws Exception {
		script(type, script, new Object[0]);
	}

	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	public void script(String type, String script, Object... args) throws Exception {
		// TODO check tyiping
		List<Scripter> scripters = getPlugins(Scripter.class);
		if (scripters.isEmpty()) {
			msgs.NoScripters_(script);
			return;
		}

		Properties p = new UTF8Properties(getProperties());

		for (int i = 0; i < args.length; i++)
			p.setProperty("" + i, Converter.cnv(String.class, args[i]));
		scripters.get(0)
			.eval((Map) p, new StringReader(script));
	}

	public String _repos(@SuppressWarnings("unused") String args[]) throws Exception {
		List<RepositoryPlugin> repos = getPlugins(RepositoryPlugin.class);
		List<String> names = new ArrayList<>();
		for (RepositoryPlugin rp : repos)
			names.add(rp.getName());
		return join(names, ", ");
	}

	public String _help(String args[]) throws Exception {
		if (args.length == 1)
			return "Specify the option or header you want information for";

		Syntax syntax = Syntax.HELP.get(args[1]);
		if (syntax == null)
			return "No help for " + args[1];

		String what = null;
		if (args.length > 2)
			what = args[2];

		if (what == null || what.equals("lead"))
			return syntax.getLead();
		if (what.equals("example"))
			return syntax.getExample();
		if (what.equals("pattern"))
			return syntax.getPattern();
		if (what.equals("values"))
			return syntax.getValues();

		return "Invalid type specified for help: lead, example, pattern, values";
	}

	/**
	 * Returns containers for the deliverables of this project. The deliverables
	 * is the project builder for this project if no -sub is specified.
	 * Otherwise it contains all the sub bnd files.
	 *
	 * @return A collection of containers
	 * @throws Exception
	 */
	public Collection<Container> getDeliverables() throws Exception {
		List<Container> result = new ArrayList<>();
		try (ProjectBuilder pb = getBuilder(null)) {
			for (Builder builder : pb.getSubBuilders()) {
				Container c = new Container(this, builder.getBsn(), builder.getVersion(), Container.TYPE.PROJECT,
					getOutputFile(builder.getBsn(), builder.getVersion()), null, null, null);
				result.add(c);
			}
			return result;
		}

	}

	/**
	 * Return a builder associated with the give bnd file or null. The bnd.bnd
	 * file can contain -sub option. This option allows specifying files in the
	 * same directory that should drive the generation of multiple deliverables.
	 * This method figures out if the bndFile is actually one of the bnd files
	 * of a deliverable.
	 *
	 * @param bndFile A file pointing to a bnd file.
	 * @return null or a builder for a sub file, the caller must close this
	 *         builder
	 * @throws Exception
	 */
	public Builder getSubBuilder(File bndFile) throws Exception {
		bndFile = bndFile.getAbsoluteFile();

		// Verify that we are inside the project.
		if (!bndFile.toPath()
			.startsWith(getBase().toPath()))
			return null;

		ProjectBuilder pb = getBuilder(null);
		boolean close = true;
		try {
			for (Builder b : pb.getSubBuilders()) {
				File propertiesFile = b.getPropertiesFile();
				if (propertiesFile != null) {
					if (propertiesFile.equals(bndFile)) {
						// Found it!
						// disconnect from its parent life cycle
						if (b == pb) {
							close = false;
						} else {
							pb.removeClose(b);
						}
						return b;
					}
				}
			}
			return null;
		} finally {
			if (close) {
				pb.close();
			}
		}
	}

	/**
	 * Return a build that maps to the sub file.
	 *
	 * @param string
	 * @throws Exception
	 */
	public ProjectBuilder getSubBuilder(String string) throws Exception {
		ProjectBuilder pb = getBuilder(null);
		boolean close = true;
		try {
			for (Builder b : pb.getSubBuilders()) {
				if (b.getBsn()
					.equals(string)
					|| b.getBsn()
						.endsWith("." + string)) {
					// disconnect from its parent life cycle
					if (b == pb) {
						close = false;
					} else {
						pb.removeClose(b);
					}
					return (ProjectBuilder) b;
				}
			}
			return null;
		} finally {
			if (close) {
				pb.close();
			}
		}
	}

	/**
	 * Answer the container associated with a given bsn.
	 *
	 * @throws Exception
	 */
	public Container getDeliverable(String bsn, Map<String, String> attrs) throws Exception {
		try (ProjectBuilder pb = getBuilder(null)) {
			for (Builder b : pb.getSubBuilders()) {
				if (b.getBsn()
					.equals(bsn))
					return new Container(this, getOutputFile(bsn, b.getVersion()), attrs);
			}
		}
		return null;
	}

	/**
	 * Get a list of the sub builders. A bnd.bnd file can contain the -sub
	 * option. This will generate multiple deliverables. This method returns the
	 * builders for each sub file. If no -sub option is present, the list will
	 * contain a builder for the bnd.bnd file.
	 *
	 * @return A list of builders.
	 * @throws Exception
	 * @deprecated As of 3.4. Replace with
	 *
	 *             <pre>
	 *             try (ProjectBuilder pb = getBuilder(null)) {
	 *             	for (Builder b : pb.getSubBuilders()) {
	 *             		...
	 *             	}
	 *             }
	 *             </pre>
	 */
	@Deprecated
	public Collection<? extends Builder> getSubBuilders() throws Exception {
		ProjectBuilder pb = getBuilder(null);
		boolean close = true;
		try {
			List<Builder> builders = pb.getSubBuilders();
			for (Builder b : builders) {
				if (b == pb) {
					close = false;
				} else {
					pb.removeClose(b);
				}
			}
			return builders;
		} finally {
			if (close) {
				pb.close();
			}
		}
	}

	/**
	 * Calculate the classpath. We include our own runtime.jar which includes
	 * the test framework and we include the first of the test frameworks
	 * specified.
	 *
	 * @throws Exception
	 */
	Collection<File> toFile(Collection<Container> containers) throws Exception {
		ArrayList<File> files = new ArrayList<>();
		for (Container container : containers) {
			container.contributeFiles(files, this);
		}
		return files;
	}

	public Collection<String> getRunVM() {
		Parameters hdr = getMergedParameters(RUNVM);
		return hdr.keyList();
	}

	public Collection<String> getRunProgramArgs() {
		Parameters hdr = getMergedParameters(RUNPROGRAMARGS);
		return hdr.keyList();
	}

	public Map<String, String> getRunProperties() {
		return OSGiHeader.parseProperties(mergeProperties(RUNPROPERTIES));
	}

	/**
	 * Get a launcher.
	 *
	 * @throws Exception
	 */
	public ProjectLauncher getProjectLauncher() throws Exception {
		ProjectLauncher launcher = getHandler(ProjectLauncher.class, getRunpath(), LAUNCHER_PLUGIN,
			"biz.aQute.launcher");

		launcher.updateFromProject(); // we need to do after constructor

		return launcher;
	}

	public ProjectTester getProjectTester() throws Exception {
		String defaultDefault = since(About._3_0) ? "biz.aQute.tester" : "biz.aQute.junit";

		return getHandler(ProjectTester.class, getTestpath(), TESTER_PLUGIN,
			getProperty(Constants.TESTER, defaultDefault));
	}

	private <T> T getHandler(Class<T> target, Collection<Container> containers, String header, String defaultHandler)
		throws Exception {
		Class<? extends T> handlerClass = target;

		// Make sure we find at least one handler, but hope to find an earlier
		// one
		List<Container> withDefault = Create.list();
		withDefault.addAll(containers);
		withDefault.addAll(getBundles(Strategy.HIGHEST, defaultHandler, null));
		logger.debug("candidates for handler {}: {}", target, withDefault);

		for (Container c : withDefault) {
			Manifest manifest = c.getManifest();

			if (manifest != null) {
				String launcher = manifest.getMainAttributes()
					.getValue(header);
				if (launcher != null) {
					Class<?> clz = getClass(launcher, c.getFile());
					if (clz != null) {
						if (!target.isAssignableFrom(clz)) {
							msgs.IncompatibleHandler_For_(launcher, defaultHandler);
						} else {
							logger.debug("found handler {} from {}", defaultHandler, c);
							handlerClass = clz.asSubclass(target);

							Constructor<? extends T> constructor;
							try {
								constructor = handlerClass.getConstructor(Project.class, Container.class);
							} catch (NoSuchMethodException e) {
								constructor = handlerClass.getConstructor(Project.class);
							}
							try {
								return (constructor.getParameterCount() == 1) ? constructor.newInstance(this)
									: constructor.newInstance(this, c);
							} catch (InvocationTargetException e) {
								throw Exceptions.duck(Exceptions.unrollCause(e, InvocationTargetException.class));
							}
						}
					}
				}
			}
		}

		throw new IllegalArgumentException("Default handler for " + header + " not found in " + defaultHandler);
	}

	/**
	 * Make this project delay the calculation of the run dependencies. The run
	 * dependencies calculation can be done in prepare or until the dependencies
	 * are actually needed.
	 */
	public void setDelayRunDependencies(boolean x) {
		delayRunDependencies = x;
	}

	/**
	 * bnd maintains a class path that is set by the environment, i.e. bnd is
	 * not in charge of it.
	 */

	public void addClasspath(File f) {
		if (!f.isFile() && !f.isDirectory()) {
			msgs.AddingNonExistentFileToClassPath_(f);
		}
		Container container = new Container(f, null);
		classpath.add(container);
	}

	public void clearClasspath() {
		classpath.clear();
		unreferencedClasspathEntries.clear();
	}

	public Collection<Container> getClasspath() {
		return classpath;
	}

	/**
	 * Pack the project (could be a bndrun file) and save it on disk. Report
	 * errors if they happen. Caller must close this JAR
	 *
	 * @param profile
	 * @return a jar with the executable code
	 * @throws Exception
	 */
	public Jar pack(String profile) throws Exception {
		return ExecutableJarExporter.pack(this, profile);
	}

	/**
	 * Do a baseline for this project
	 *
	 * @throws Exception
	 */

	public void baseline() throws Exception {
		try (ProjectBuilder pb = getBuilder(null)) {
			for (Builder b : pb.getSubBuilders()) {
				@SuppressWarnings("resource")
				Jar build = b.build();
				getInfo(b);
			}
			getInfo(pb);
		}
	}

	/**
	 * Method to verify that the paths are correct, ie no missing dependencies
	 *
	 * @param test for test cases, also adds -testpath
	 * @throws Exception
	 */
	public void verifyDependencies(boolean test) throws Exception {
		verifyDependencies(RUNBUNDLES, getRunbundles());
		verifyDependencies(RUNPATH, getRunpath());
		if (test)
			verifyDependencies(TESTPATH, getTestpath());
		verifyDependencies(BUILDPATH, getBuildpath());
	}

	private void verifyDependencies(String title, Collection<Container> path) throws Exception {
		List<String> msgs = new ArrayList<>();
		for (Container c : new ArrayList<>(path)) {
			for (Container cc : c.getMembers()) {
				if (cc.getError() != null)
					msgs.add(cc + " - " + cc.getError());
				else if (!cc.getFile()
					.isFile()
					&& !cc.getFile()
						.equals(cc.getProject()
							.getOutput())
					&& !cc.getFile()
						.equals(cc.getProject()
							.getTestOutput()))
					msgs.add(cc + " file does not exists: " + cc.getFile());
			}
		}
		if (msgs.isEmpty())
			return;

		error("%s: has errors: %s", title, Strings.join(msgs));
	}

	/**
	 * Report detailed info from this project
	 *
	 * @throws Exception
	 */

	@Override
	public void report(Map<String, Object> table) throws Exception {
		super.report(table);
		report(table, true);
	}

	protected void report(Map<String, Object> table, boolean isProject) throws Exception {
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

	// TODO test format parametsr

	public void compile(boolean test) throws Exception {

		Command javac = getCommonJavac(false);
		javac.add("-d", IO.absolutePath(getOutput()));

		StringBuilder buildpath = new StringBuilder();

		String buildpathDel = "";
		Collection<Container> bp = Container.flatten(getBuildpath());
		logger.debug("buildpath {}", getBuildpath());
		for (Container c : bp) {
			buildpath.append(buildpathDel)
				.append(IO.absolutePath(c.getFile()));
			buildpathDel = File.pathSeparator;
		}

		if (buildpath.length() != 0) {
			javac.add("-classpath", buildpath.toString());
		}

		List<File> sp = new ArrayList<>(getSourcePath());
		StringBuilder sourcepath = new StringBuilder();
		String sourcepathDel = "";

		for (File sourceDir : sp) {
			sourcepath.append(sourcepathDel)
				.append(IO.absolutePath(sourceDir));
			sourcepathDel = File.pathSeparator;
		}

		javac.add("-sourcepath", sourcepath.toString());

		Glob javaFiles = new Glob("*.java");
		List<File> files = javaFiles.getFiles(getSrc(), true, false);

		for (File file : files) {
			javac.add(IO.absolutePath(file));
		}

		if (files.isEmpty()) {
			logger.debug("Not compiled, no source files");
		} else
			compile(javac, "src");

		if (test) {
			javac = getCommonJavac(true);
			javac.add("-d", IO.absolutePath(getTestOutput()));

			Collection<Container> tp = Container.flatten(getTestpath());
			for (Container c : tp) {
				buildpath.append(buildpathDel)
					.append(IO.absolutePath(c.getFile()));
				buildpathDel = File.pathSeparator;
			}
			if (buildpath.length() != 0) {
				javac.add("-classpath", buildpath.toString());
			}

			sourcepath.append(sourcepathDel)
				.append(IO.absolutePath(getTestSrc()));
			javac.add("-sourcepath", sourcepath.toString());

			javaFiles.getFiles(getTestSrc(), files, true, false);
			for (File file : files) {
				javac.add(IO.absolutePath(file));
			}
			if (files.isEmpty()) {
				logger.debug("Not compiled for test, no test src files");
			} else
				compile(javac, "test");
		}
	}

	private void compile(Command javac, String what) throws Exception {
		logger.debug("compile {} {}", what, javac);

		StringBuilder stdout = new StringBuilder();
		StringBuilder stderr = new StringBuilder();

		int n = javac.execute(stdout, stderr);
		logger.debug("javac stdout: {}", stdout);
		logger.debug("javac stderr: {}", stderr);

		if (n != 0) {
			error("javac failed %s", stderr);
		}
	}

	private Command getCommonJavac(boolean test) throws Exception {
		Command javac = new Command();
		javac.add(getJavaExecutable("javac"));
		String target = getProperty("javac.target", "1.6");
		String profile = getProperty("javac.profile", "");
		String source = getProperty("javac.source", "1.6");
		String debug = getProperty("javac.debug");
		if ("on".equalsIgnoreCase(debug) || "true".equalsIgnoreCase(debug))
			debug = "vars,source,lines";

		Parameters options = new Parameters(getProperty("java.options"), this);

		boolean deprecation = isTrue(getProperty("java.deprecation"));

		javac.add("-encoding", "UTF-8");

		javac.add("-source", source);

		javac.add("-target", target);

		if (!profile.isEmpty())
			javac.add("-profile", profile);

		if (deprecation)
			javac.add("-deprecation");

		if (test || debug == null) {
			javac.add("-g:source,lines,vars");
		} else {
			javac.add("-g:" + debug);
		}

		javac.addAll(options.keyList());

		StringBuilder bootclasspath = new StringBuilder();
		String bootclasspathDel = "-Xbootclasspath/p:";

		Collection<Container> bcp = Container.flatten(getBootclasspath());
		for (Container c : bcp) {
			bootclasspath.append(bootclasspathDel)
				.append(IO.absolutePath(c.getFile()));
			bootclasspathDel = File.pathSeparator;
		}

		if (bootclasspath.length() != 0) {
			javac.add(bootclasspath.toString());
		}
		return javac;
	}

	public String _ide(String[] args) throws IOException {
		if (args.length < 2) {
			error("The ${ide;<>} macro needs an argument");
			return null;
		}
		if (ide == null) {
			ide = new UTF8Properties();
			File file = getFile(".settings/org.eclipse.jdt.core.prefs");
			if (!file.isFile()) {
				error("The ${ide;<>} macro requires a .settings/org.eclipse.jdt.core.prefs file in the project");
				return null;
			}
			try (InputStream in = IO.stream(file)) {
				ide.load(in);
			}
		}

		String deflt = args.length > 2 ? args[2] : null;
		if ("javac.target".equals(args[1])) {
			return ide.getProperty("org.eclipse.jdt.core.compiler.codegen.targetPlatform", deflt);
		}
		if ("javac.source".equals(args[1])) {
			return ide.getProperty("org.eclipse.jdt.core.compiler.source", deflt);
		}
		return null;
	}

	public Map<String, Version> getVersions() throws Exception {
		if (versionMap.isEmpty()) {
			try (ProjectBuilder pb = getBuilder(null)) {
				for (Builder builder : pb.getSubBuilders()) {
					String v = builder.getVersion();
					if (v == null)
						v = "0";
					else {
						v = Analyzer.cleanupVersion(v);
						if (!Verifier.isVersion(v))
							continue; // skip
					}

					Version version = new Version(v);
					versionMap.put(builder.getBsn(), version);
				}
			}
		}
		return new LinkedHashMap<>(versionMap);
	}

	public Collection<String> getBsns() throws Exception {
		return new ArrayList<>(getVersions().keySet());
	}

	public Version getVersion(String bsn) throws Exception {
		Version version = getVersions().get(bsn);
		if (version == null) {
			throw new IllegalArgumentException("Bsn " + bsn + " does not exist in project " + getName());
		}
		return version;
	}

	/**
	 * Get the exported packages form all builders calculated from the last
	 * build
	 */

	public Packages getExports() {
		return exportedPackages;
	}

	/**
	 * Get the imported packages from all builders calculated from the last
	 * build
	 */

	public Packages getImports() {
		return importedPackages;
	}

	/**
	 * Get the contained packages calculated from all builders from the last
	 * build
	 */

	public Packages getContained() {
		return containedPackages;
	}

	public void remove() throws Exception {
		getWorkspace().removeProject(this);
		IO.delete(getBase());
	}

	public boolean getRunKeep() {
		return is(Constants.RUNKEEP);
	}

	public void setPackageInfo(String packageName, Version newVersion) throws Exception {
		packageInfo.setPackageInfo(packageName, newVersion);
	}

	public Version getPackageInfo(String packageName) throws Exception {
		return packageInfo.getPackageInfo(packageName);
	}

	/**
	 * Actions to perform before a full workspace release. This is executed for
	 * projects that describe the distribution
	 */

	public void preRelease() {
		for (ReleaseBracketingPlugin rp : getWorkspace().getPlugins(ReleaseBracketingPlugin.class)) {
			rp.begin(this);
		}
	}

	/**
	 * Actions to perform after a full workspace release. This is executed for
	 * projects that describe the distribution
	 */

	public void postRelease() {
		for (ReleaseBracketingPlugin rp : getWorkspace().getPlugins(ReleaseBracketingPlugin.class)) {
			rp.end(this);
		}
	}

	/**
	 * Copy a repository to another repository
	 *
	 * @throws Exception
	 */

	public void copy(RepositoryPlugin source, String filter, RepositoryPlugin destination) throws Exception {
		copy(source, filter == null ? null : new Instructions(filter), destination);
	}

	public void copy(RepositoryPlugin source, Instructions filter, RepositoryPlugin destination) throws Exception {

		assert source != null;
		assert destination != null;

		logger.info("copy from repo {} to {} with filter {}", source, destination, filter);

		for (String bsn : source.list(null)) {
			for (Version version : source.versions(bsn)) {
				if (filter == null || filter.matches(bsn)) {
					logger.info("copy {}:{}", bsn, version);
					File file = source.get(bsn, version, null);
					if (file.getName()
						.endsWith(".jar")) {
						try (InputStream in = IO.stream(file)) {
							PutOptions po = new PutOptions();
							po.bsn = bsn;
							po.context = null;
							po.type = "bundle";
							po.version = version;
							PutResult put = destination.put(in, po);
						} catch (Exception e) {
							logger.error("Failed to copy {}-{}", e, bsn, version);
							error("Failed to copy %s:%s from %s to %s, error: %s", bsn, version, source, destination,
								e);
						}
					}
				}
			}
		}
	}

	@Override
	public boolean isInteractive() {
		return getWorkspace().isInteractive();
	}

	/**
	 * Return a basic type only specification of the run aspect of this project
	 */
	public RunSpecification getSpecification() {

		RunSpecification runspecification = new RunSpecification();
		try {
			ProjectLauncher l = getProjectLauncher();
			runspecification.bin = getOutput().getAbsolutePath();
			runspecification.bin_test = getTestOutput().getAbsolutePath();
			runspecification.target = getTarget().getAbsolutePath();
			runspecification.errors.addAll(getErrors());
			runspecification.extraSystemCapabilities = getRunSystemCapabilities().toBasic();
			runspecification.extraSystemPackages = getRunSystemPackages().toBasic();
			runspecification.properties = l.getRunProperties();
			runspecification.runbundles = toPaths(runspecification.errors, getRunbundles());
			runspecification.runfw = toPaths(runspecification.errors, getRunFw());
			runspecification.runpath = toPaths(runspecification.errors, getRunpath());

			for (String key : Iterables.iterable(getProperties().propertyNames(), String.class::cast)) {
				// skip non instructions to prevent macro expansions we do not
				// want
				if (key.startsWith("-"))
					runspecification.instructions.put(key, getProperty(key));
			}
		} catch (Exception e) {
			runspecification.errors.add(e.toString());
		}
		runspecification.errors.addAll(getErrors());
		return runspecification;
	}

	public Parameters getRunSystemPackages() {
		return new Parameters(mergeProperties(Constants.RUNSYSTEMPACKAGES));
	}

	public Parameters getRunSystemCapabilities() {
		return new Parameters(mergeProperties(Constants.RUNSYSTEMCAPABILITIES));
	}

	/**
	 * Check prebuild things.
	 */
	protected void preBuildChecks() {
		instructions.stalecheck()
			.forEach(this::staleCheck);
	}

	/*
	 * Check if a set of files is out of date with another set of files. If so,
	 * generate an error or warning, or execute a command
	 */

	private void staleCheck(String src, StaleTest st) {
		try {
			String useSrc = Strings.trim(Processor.removeDuplicateMarker(src));
			if (useSrc.isEmpty() || useSrc.equals(Constants.EMPTY_HEADER))
				return;

			if (st.newer() == null) {
				setLocation(Constants.STALECHECK, Pattern.quote(useSrc),
					warning("No `newer=...` files spec for src= '%s' found in %s", useSrc, Constants.STALECHECK));
				return;
			}

			FileTree tree = new FileTree();

			OptionalLong newest = tree.getFiles(getBase(), Strings.split(useSrc))
				.stream()
				.filter(File::isFile)
				.mapToLong(File::lastModified)
				.max();

			if (!newest.isPresent()) {
				setLocation(Constants.STALECHECK, Pattern.quote(useSrc),
					warning("No source files '%s' found for %s", useSrc, Constants.STALECHECK));
				return;
			}

			long time = newest.getAsLong();

			List<String> defaultIncludes = Strings.splitAsStream(st.newer())
				.map(p -> getFile(p).isDirectory() && !p.endsWith("/") ? p + "/" : p)
				.collect(toList());

			List<File> dependentFiles = tree.getFiles(getBase(), defaultIncludes)
				.stream()
				.filter(File::isFile)
				.filter(f -> f.lastModified() < time)
				.sorted()
				.collect(toList());

			boolean staleFiles = !dependentFiles.isEmpty();

			if (staleFiles) {
				String qsrc = Pattern.quote(useSrc);

				Optional<String> warning = st.warning();
				if (!warning.isPresent() && !st.error()
					.isPresent()
					&& !st.command()
						.isPresent()) {
					warning = Optional.ofNullable("detected stale files");
				}

				st.error()
					.ifPresent(msg -> setLocation(Constants.STALECHECK, qsrc,
						error("%s : %s > %s", msg, useSrc, dependentFiles)));
				warning.ifPresent(msg -> setLocation(Constants.STALECHECK, qsrc,
					warning("%s : %s > %s", msg, useSrc, dependentFiles)));

				st.command()
					.ifPresent(ConsumerWithException.asConsumer(cmd -> system(cmd, null)));
			}

		} catch (Exception e) {
			exception(e, "unexpected exception in %s", Constants.STALECHECK);
		}
	}

	public Container getBundle(org.osgi.resource.Resource r) throws Exception {
		IdentityCapability identity = ResourceUtils.getIdentityCapability(r);
		if (identity == null)
			return Container.error(this, r.toString());

		if (r.getCapabilities(ResourceUtils.WORKSPACE_NAMESPACE) != null) {
			Container bundle = getBundle(identity.osgi_identity(), "snapshot", Strategy.HIGHEST, null);
			if (bundle != null)
				return bundle;
		}

		Container bundle = getBundle(identity.osgi_identity(), identity.version()
			.toString(), Strategy.EXACT, null);
		if (bundle != null)
			return bundle;

		return Container.error(this, identity.osgi_identity() + "-" + identity.version());
	}

	public boolean isStandalone() {
		return getWorkspace().getLayout() == WorkspaceLayout.STANDALONE;
	}

	@Override
	public String getChecksum() {
		try {
			prepare();
			return super.getChecksum();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

}

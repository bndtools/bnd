package aQute.bnd.build;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.*;
import java.util.jar.*;

import aQute.bnd.header.*;
import aQute.bnd.help.*;
import aQute.bnd.maven.support.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.eclipse.*;
import aQute.bnd.service.*;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import aQute.bnd.service.action.*;
import aQute.bnd.version.*;
import aQute.lib.io.*;
import aQute.libg.generics.*;
import aQute.libg.reporter.*;
import aQute.libg.sed.*;

/**
 * This class is NOT threadsafe
 */

public class Project extends Processor {

	final static String			DEFAULT_ACTIONS			= "build; label='Build', test; label='Test', run; label='Run', clean; label='Clean', release; label='Release', refreshAll; label=Refresh, deploy;label=Deploy";
	public final static String	BNDFILE					= "bnd.bnd";
	public final static String	BNDCNF					= "cnf";
	final Workspace				workspace;
	boolean						preparedPaths;
	final Collection<Project>	dependson				= new LinkedHashSet<Project>();
	final Collection<Container>	classpath				= new LinkedHashSet<Container>();
	final Collection<Container>	buildpath				= new LinkedHashSet<Container>();
	final Collection<Container>	testpath				= new LinkedHashSet<Container>();
	final Collection<Container>	runpath					= new LinkedHashSet<Container>();
	final Collection<Container>	runbundles				= new LinkedHashSet<Container>();
	File						runstorage;
	final Collection<File>		sourcepath				= new LinkedHashSet<File>();
	final Collection<File>		allsourcepath			= new LinkedHashSet<File>();
	final Collection<Container>	bootclasspath			= new LinkedHashSet<Container>();
	final Lock					lock					= new ReentrantLock(true);
	volatile String				lockingReason;
	volatile Thread				lockingThread;
	File						output;
	File						target;
	boolean						inPrepare;
	int							revision;
	File						files[];
	static List<Project>		trail					= new ArrayList<Project>();
	boolean						delayRunDependencies	= false;
	final ProjectMessages		msgs					= ReporterMessages.base(this, ProjectMessages.class);

	public Project(Workspace workspace, @SuppressWarnings("unused") File projectDir, File buildFile) throws Exception {
		super(workspace);
		this.workspace = workspace;
		setFileMustExist(false);
		setProperties(buildFile);
		assert workspace != null;
		// For backward compatibility reasons, we also read
		readBuildProperties();
	}

	public Project(Workspace workspace, File buildDir) throws Exception {
		this(workspace, buildDir, new File(buildDir, BNDFILE));
	}

	private void readBuildProperties() throws Exception {
		try {
			File f = getFile("build.properties");
			if (f.isFile()) {
				Properties p = loadProperties(f);
				for (Enumeration< ? > e = p.propertyNames(); e.hasMoreElements();) {
					String key = (String) e.nextElement();
					String newkey = key;
					if (key.indexOf('$') >= 0) {
						newkey = getReplacer().process(key);
					}
					setProperty(newkey, p.getProperty(key));
				}
			}
		}
		catch (Exception e) {
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

	public synchronized boolean isValid() {
		return getBase().isDirectory() && getPropertiesFile().isFile();
	}

	/**
	 * Return a new builder that is nicely setup for this project. Please close
	 * this builder after use.
	 * 
	 * @param parent
	 *            The project builder to use as parent, use this project if null
	 * @return
	 * @throws Exception
	 */
	public synchronized ProjectBuilder getBuilder(ProjectBuilder parent) throws Exception {

		ProjectBuilder builder;

		if (parent == null)
			builder = new ProjectBuilder(this);
		else
			builder = new ProjectBuilder(parent);

		builder.setBase(getBase());
		builder.setPedantic(isPedantic());
		builder.setTrace(isTrace());
		return builder;
	}

	public synchronized int getChanged() {
		return revision;
	}

	/*
	 * Indicate a change in the external world that affects our build. This will
	 * clear any cached results.
	 */
	public synchronized void setChanged() {
		// if (refresh()) {
		preparedPaths = false;
		files = null;
		revision++;
		// }
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public String toString() {
		return getBase().getName();
	}

	/**
	 * Set up all the paths
	 */

	public synchronized void prepare() throws Exception {
		if (!isValid()) {
			warning("Invalid project attempts to prepare: %s", this);
			return;
		}

		if (inPrepare)
			throw new CircularDependencyException(trail.toString() + "," + this);

		trail.add(this);
		try {
			if (!preparedPaths) {
				inPrepare = true;
				try {
					dependson.clear();
					buildpath.clear();
					sourcepath.clear();
					allsourcepath.clear();
					bootclasspath.clear();
					testpath.clear();
					runpath.clear();
					runbundles.clear();

					// We use a builder to construct all the properties for
					// use.
					setProperty("basedir", getBase().getAbsolutePath());

					// If a bnd.bnd file exists, we read it.
					// Otherwise, we just do the build properties.
					if (!getPropertiesFile().isFile() && new File(getBase(), ".classpath").isFile()) {
						// Get our Eclipse info, we might depend on other
						// projects
						// though ideally this should become empty and void
						doEclipseClasspath();
					}

					// Calculate our source directory

					File src = getSrc();
					if (src.isDirectory()) {
						sourcepath.add(src);
						allsourcepath.add(src);
					} else
						sourcepath.add(getBase());

					// Set default bin directory
					output = getOutput0();
					if (!output.exists()) {
						output.mkdirs();
						getWorkspace().changedFile(output);
					}
					if (!output.isDirectory())
						msgs.NoOutputDirectory_(output);
					else {
						Container c = new Container(this, output);
						if (!buildpath.contains(c))
							buildpath.add(c);
					}

					// Where we store all our generated stuff.
					target = getTarget0();

					// Where the launched OSGi framework stores stuff
					String runStorageStr = getProperty(Constants.RUNSTORAGE);
					runstorage = runStorageStr != null ? getFile(runStorageStr) : null;

					// We might have some other projects we want build
					// before we do anything, but these projects are not in
					// our path. The -dependson allows you to build them before.

					List<Project> dependencies = new ArrayList<Project>();
					// dependencies.add( getWorkspace().getProject("cnf"));

					String dp = getProperty(Constants.DEPENDSON);
					Set<String> requiredProjectNames = new Parameters(dp).keySet();
					List<DependencyContributor> dcs = getPlugins(DependencyContributor.class);
					for (DependencyContributor dc : dcs)
						dc.addDependencies(this, requiredProjectNames);

					for (String p : requiredProjectNames) {
						Project required = getWorkspace().getProject(p);
						if (required == null)
							msgs.MissingDependson_(p);
						else {
							dependencies.add(required);
						}

					}

					// We have two paths that consists of repo files, projects,
					// or some other stuff. The doPath routine adds them to the
					// path and extracts the projects so we can build them
					// before.

					doPath(buildpath, dependencies, parseBuildpath(), bootclasspath);
					doPath(testpath, dependencies, parseTestpath(), bootclasspath);
					if (!delayRunDependencies) {
						doPath(runpath, dependencies, parseRunpath(), null);
						doPath(runbundles, dependencies, parseRunbundles(), null);
					}

					// We now know all dependent projects. But we also depend
					// on whatever those projects depend on. This creates an
					// ordered list without any duplicates. This of course
					// assumes
					// that there is no circularity. However, this is checked
					// by the inPrepare flag, will throw an exception if we
					// are circular.

					Set<Project> done = new HashSet<Project>();
					done.add(this);
					allsourcepath.addAll(sourcepath);

					for (Project project : dependencies)
						project.traverse(dependson, done);

					for (Project project : dependson) {
						allsourcepath.addAll(project.getSourcePath());
					}
					if (isOk())
						preparedPaths = true;
				}
				finally {
					inPrepare = false;
				}
			}
		}
		finally {
			trail.remove(this);
		}
	}

	/**
	 * @return
	 */
	private File getOutput0() {
		return getFile(getProperty("bin", "bin")).getAbsoluteFile();
	}

	/**
	 * 
	 */
	private File getTarget0() {
		File target = getFile(getProperty("target", "generated"));
		if (!target.exists()) {
			target.mkdirs();
			getWorkspace().changedFile(target);
		}
		return target;
	}

	public File getSrc() {
		return new File(getBase(), getProperty("src", "src"));
	}

	private void traverse(Collection<Project> dependencies, Set<Project> visited) throws Exception {
		if (visited.contains(this))
			return;

		visited.add(this);

		for (Project project : getDependson())
			project.traverse(dependencies, visited);

		dependencies.add(this);
	}

	/**
	 * Iterate over the entries and place the projects on the projects list and
	 * all the files of the entries on the resultpath.
	 * 
	 * @param resultpath
	 *            The list that gets all the files
	 * @param projects
	 *            The list that gets any projects that are entries
	 * @param entries
	 *            The input list of classpath entries
	 */
	private void doPath(Collection<Container> resultpath, Collection<Project> projects, Collection<Container> entries,
			Collection<Container> bootclasspath) {
		for (Container cpe : entries) {
			if (cpe.getError() != null)
				error(cpe.getError());
			else {
				if (cpe.getType() == Container.TYPE.PROJECT) {
					projects.add(cpe.getProject());
				}
				if (bootclasspath != null
						&& (cpe.getBundleSymbolicName().startsWith("ee.") || cpe.getAttributes().containsKey("boot")))
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
	 * 
	 * @return
	 */

	private List<Container> parseBuildpath() throws Exception {
		List<Container> bundles = getBundles(Strategy.LOWEST, getProperty(Constants.BUILDPATH), Constants.BUILDPATH);
		appendPackages(Strategy.LOWEST, getProperty(Constants.BUILDPACKAGES), bundles, ResolverMode.build);
		return bundles;
	}

	private List<Container> parseRunpath() throws Exception {
		return getBundles(Strategy.HIGHEST, getProperty(Constants.RUNPATH), Constants.RUNPATH);
	}

	private List<Container> parseRunbundles() throws Exception {
		return getBundles(Strategy.HIGHEST, getProperty(Constants.RUNBUNDLES), Constants.RUNBUNDLES);
	}

	private List<Container> parseTestpath() throws Exception {
		return getBundles(Strategy.HIGHEST, getProperty(Constants.TESTPATH), Constants.TESTPATH);
	}

	/**
	 * Analyze the header and return a list of files that should be on the
	 * build, test or some other path. The list is assumed to be a list of bsns
	 * with a version specification. The special case of version=project
	 * indicates there is a project in the same workspace. The path to the
	 * output directory is calculated. The default directory ${bin} can be
	 * overridden with the output attribute.
	 * 
	 * @param strategy
	 *            STRATEGY_LOWEST or STRATEGY_HIGHEST
	 * @param spec
	 *            The header
	 * @return
	 */

	public List<Container> getBundles(Strategy strategyx, String spec, String source) throws Exception {
		List<Container> result = new ArrayList<Container>();
		Parameters bundles = new Parameters(spec);

		try {
			for (Iterator<Entry<String,Attrs>> i = bundles.entrySet().iterator(); i.hasNext();) {
				Entry<String,Attrs> entry = i.next();
				String bsn = removeDuplicateMarker(entry.getKey());
				Map<String,String> attrs = entry.getValue();

				Container found = null;

				String versionRange = attrs.get("version");

				if (versionRange != null) {
					if (versionRange.equals("latest") || versionRange.equals("snapshot")) {
						found = getBundle(bsn, versionRange, strategyx, attrs);
					}
				}
				if (found == null) {
					if (versionRange != null && (versionRange.equals("project") || versionRange.equals("latest"))) {
						Project project = getWorkspace().getProject(bsn);
						if (project != null && project.exists()) {
							File f = project.getOutput();
							found = new Container(project, bsn, versionRange, Container.TYPE.PROJECT, f, null, attrs);
						} else {
							msgs.NoSuchProject(bsn, spec);
							continue;
						}
					} else if (versionRange != null && versionRange.equals("file")) {
						File f = getFile(bsn);
						String error = null;
						if (!f.exists())
							error = "File does not exist: " + f.getAbsolutePath();
						if (f.getName().endsWith(".lib")) {
							found = new Container(this, bsn, "file", Container.TYPE.LIBRARY, f, error, attrs);
						} else {
							found = new Container(this, bsn, "file", Container.TYPE.EXTERNAL, f, error, attrs);
						}
					} else {
						found = getBundle(bsn, versionRange, strategyx, attrs);
					}
				}

				if (found != null) {
					List<Container> libs = found.getMembers();
					for (Container cc : libs) {
						if (result.contains(cc))
							warning("Multiple bundles with the same final URL: %s, dropped duplicate", cc);
						else
							result.add(cc);
					}
				} else {
					// Oops, not a bundle in sight :-(
					Container x = new Container(this, bsn, versionRange, Container.TYPE.ERROR, null, bsn + ";version="
							+ versionRange + " not found", attrs);
					result.add(x);
					warning("Can not find URL for bsn " + bsn);
				}
			}
		}
		catch (CircularDependencyException e) {
			String message = e.getMessage();
			if (source != null)
				message = String.format("%s (from property: %s)", message, source);
			msgs.CircularDependencyContext_Message_(getName(), message);
		}
		catch (Exception e) {
			msgs.Unexpected_Error_(spec, e);
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
	 * Calculates the containers required to fulfil the {@code -buildpackages}
	 * instruction, and appends them to the existing list of containers.
	 * 
	 * @param strategyx
	 *            The package-version disambiguation strategy.
	 * @param spec
	 *            The value of the @{code -buildpackages} instruction.
	 * @throws Exception
	 */
	public void appendPackages(Strategy strategyx, String spec, List<Container> resolvedBundles, ResolverMode mode)
			throws Exception {
		Map<File,Container> pkgResolvedBundles = new HashMap<File,Container>();

		List<Entry<String,Attrs>> queue = new LinkedList<Map.Entry<String,Attrs>>();
		queue.addAll(new Parameters(spec).entrySet());

		while (!queue.isEmpty()) {
			Entry<String,Attrs> entry = queue.remove(0);

			String pkgName = entry.getKey();
			Map<String,String> attrs = entry.getValue();

			Container found = null;

			String versionRange = attrs.get(Constants.VERSION_ATTRIBUTE);
			if ("latest".equals(versionRange) || "snapshot".equals(versionRange))
				found = getPackage(pkgName, versionRange, strategyx, attrs, mode);

			if (found == null)
				found = getPackage(pkgName, versionRange, strategyx, attrs, mode);

			if (found != null) {
				if (resolvedBundles.contains(found)) {
					// Don't add his bundle because it was already included
					// using -buildpath
				} else {
					List<Container> libs = found.getMembers();
					for (Container cc : libs) {
						Container existing = pkgResolvedBundles.get(cc.file);
						if (existing != null)
							addToPackageList(existing, attrs.get("packages"));
						else {
							addToPackageList(cc, attrs.get("packages"));
							pkgResolvedBundles.put(cc.file, cc);
						}

						String importUses = cc.getAttributes().get("import-uses");
						if (importUses != null)
							queue.addAll(0, new Parameters(importUses).entrySet());
					}
				}
			} else {
				// Unable to resolve
				Container x = new Container(this, "X", versionRange, Container.TYPE.ERROR, null, "package " + pkgName
						+ ";version=" + versionRange + " not found", attrs);
				resolvedBundles.add(x);
				warning("Can not find URL for package " + pkgName);
			}
		}

		for (Container container : pkgResolvedBundles.values()) {
			resolvedBundles.add(container);
		}
	}

	static void mergeNames(String names, Set<String> set) {
		StringTokenizer tokenizer = new StringTokenizer(names, ",");
		while (tokenizer.hasMoreTokens())
			set.add(tokenizer.nextToken().trim());
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
		Set<String> merged = new HashSet<String>();

		String packageListStr = container.attributes.get("packages");
		if (packageListStr != null)
			mergeNames(packageListStr, merged);
		if (newPackageNames != null)
			mergeNames(newPackageNames, merged);

		container.putAttribute("packages", flatten(merged));
	}

	/**
	 * Find a container to fulfil a package requirement
	 * 
	 * @param packageName
	 *            The package required
	 * @param range
	 *            The package version range required
	 * @param strategyx
	 *            The package-version disambiguation strategy
	 * @param attrs
	 *            Other attributes specified by the search.
	 * @return
	 * @throws Exception
	 */
	public Container getPackage(String packageName, String range, Strategy strategyx, Map<String,String> attrs,
			ResolverMode mode) throws Exception {
		if ("snapshot".equals(range))
			return new Container(this, "", range, Container.TYPE.ERROR, null,
					"snapshot not supported for package lookups", null);

		if (attrs == null)
			attrs = new HashMap<String,String>(2);
		attrs.put("package", packageName);
		attrs.put("mode", mode.name());

		Strategy useStrategy = findStrategy(attrs, strategyx, range);

		List<RepositoryPlugin> plugins = getPlugins(RepositoryPlugin.class);
		for (RepositoryPlugin plugin : plugins) {
			try {
				File result = plugin.get(null, range, useStrategy, attrs);
				if (result != null) {
					if (result.getName().endsWith("lib"))
						return new Container(this, result.getName(), range, Container.TYPE.LIBRARY, result, null, attrs);
					return new Container(this, result.getName(), range, Container.TYPE.REPO, result, null, attrs);
				}
			}
			catch (Exception e) {
				// Ignore... lots of repos will fail here
			}
		}

		return new Container(this, "X", range, Container.TYPE.ERROR, null, "package " + packageName + ";version="
				+ range + " Not found in " + plugins, null);
	}

	private Strategy findStrategy(Map<String,String> attrs, Strategy defaultStrategy, String versionRange) {
		Strategy useStrategy = defaultStrategy;
		String overrideStrategy = attrs.get("strategy");
		if (overrideStrategy != null) {
			if ("highest".equalsIgnoreCase(overrideStrategy))
				useStrategy = Strategy.HIGHEST;
			else if ("lowest".equalsIgnoreCase(overrideStrategy))
				useStrategy = Strategy.LOWEST;
			else if ("exact".equalsIgnoreCase(overrideStrategy))
				useStrategy = Strategy.EXACT;
		}
		if ("latest".equals(versionRange))
			useStrategy = Strategy.HIGHEST;
		return useStrategy;
	}

	/**
	 * The user selected pom in a path. This will place the pom as well as its
	 * dependencies on the list
	 * 
	 * @param strategyx
	 *            the strategy to use.
	 * @param result
	 *            The list of result containers
	 * @param attrs
	 *            The attributes
	 * @throws Exception
	 *             anything goes wrong
	 */
	public void doMavenPom(Strategy strategyx, List<Container> result, String action) throws Exception {
		File pomFile = getFile("pom.xml");
		if (!pomFile.isFile())
			msgs.MissingPom();
		else {
			ProjectPom pom = getWorkspace().getMaven().createProjectModel(pomFile);
			if (action == null)
				action = "compile";
			Pom.Scope act = Pom.Scope.valueOf(action);
			Set<Pom> dependencies = pom.getDependencies(act);
			for (Pom sub : dependencies) {
				File artifact = sub.getArtifact();
				Container container = new Container(artifact);
				result.add(container);
			}
		}
	}

	public Collection<Project> getDependson() throws Exception {
		prepare();
		return dependson;
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
	private void justInTime(Collection<Container> path, List<Container> entries) {
		if (delayRunDependencies && path.isEmpty())
			doPath(path, dependson, entries, null);
	}

	public Collection<Container> getRunpath() throws Exception {
		prepare();
		justInTime(runpath, parseRunpath());
		return runpath;
	}

	public Collection<Container> getRunbundles() throws Exception {
		prepare();
		justInTime(runbundles, parseRunbundles());
		return runbundles;
	}

	public File getRunStorage() throws Exception {
		prepare();
		return runstorage;
	}

	public boolean getRunBuilds() {
		boolean result;
		String runBuildsStr = getProperty(Constants.RUNBUILDS);
		if (runBuildsStr == null)
			result = !getPropertiesFile().getName().toLowerCase().endsWith(Constants.DEFAULT_BNDRUN_EXTENSION);
		else
			result = Boolean.parseBoolean(runBuildsStr);
		return result;
	}

	public Collection<File> getSourcePath() throws Exception {
		prepare();
		return sourcepath;
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
			dependson.add(required);
		}
		for (File f : eclipse.getClasspath()) {
			buildpath.add(new Container(f));
		}
		for (File f : eclipse.getBootclasspath()) {
			bootclasspath.add(new Container(f));
		}
		sourcepath.addAll(eclipse.getSourcepath());
		allsourcepath.addAll(eclipse.getAllSources());
		output = eclipse.getOutput();
	}

	public String _p_dependson(String args[]) throws Exception {
		return list(args, toFiles(getDependson()));
	}

	private Collection< ? > toFiles(Collection<Project> projects) {
		List<File> files = new ArrayList<File>();
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
		return getOutput().getAbsolutePath();
	}

	private String list(String[] args, Collection< ? > list) {
		if (args.length > 3)
			throw new IllegalArgumentException("${" + args[0]
					+ "[;<separator>]} can only take a separator as argument, has " + Arrays.toString(args));

		String separator = ",";

		if (args.length == 2) {
			separator = args[1];
		}

		return join(list, separator);
	}

	protected Object[] getMacroDomains() {
		return new Object[] {
			workspace
		};
	}

	public File release(Jar jar) throws Exception {
		String name = getProperty(Constants.RELEASEREPO);
		return release(name, jar);
	}

	/**
	 * Release
	 * 
	 * @param name
	 *            The repository name
	 * @param jar
	 * @return
	 * @throws Exception
	 */
	public File release(String name, Jar jar) throws Exception {
		trace("release %s", name);
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
				File file = rp.put(jar);
				trace("Released %s to file %s in repository %s", jar.getName(), file, rp);
			}
			catch (Exception e) {
				msgs.Release_Into_Exception_(jar, rp, e);
			}
			finally {
				jar.close();
			}
		} else if (name == null)
			msgs.NoNameForReleaseRepository();
		else
			msgs.ReleaseRepository_NotFoundIn_(name, plugins);

		return null;

	}

	public void release(boolean test) throws Exception {
		String name = getProperty(Constants.RELEASEREPO);
		release(name, test);
	}

	/**
	 * Release
	 * 
	 * @param name
	 *            The respository name
	 * @param test
	 *            Run testcases
	 * @throws Exception
	 */
	public void release(String name, boolean test) throws Exception {
		trace("release");
		File[] jars = build(test);
		// If build fails jars will be null
		if (jars == null) {
			trace("no jars being build");
			return;
		}
		trace("build ", Arrays.toString(jars));
		for (File jar : jars) {
			Jar j = new Jar(jar);
			try {
				release(name, j);
			}
			finally {
				j.close();
			}
		}

	}

	/**
	 * Get a bundle from one of the plugin repositories. If an exact version is
	 * required we just return the first repository found (in declaration order
	 * in the build.bnd file).
	 * 
	 * @param bsn
	 *            The bundle symbolic name
	 * @param range
	 *            The version range
	 * @param lowest
	 *            set to LOWEST or HIGHEST
	 * @return the file object that points to the bundle or null if not found
	 * @throws Exception
	 *             when something goes wrong
	 */

	public Container getBundle(String bsn, String range, Strategy strategy, Map<String,String> attrs) throws Exception {

		if (range == null)
			range = "0";

		if ("snapshot".equals(range)) {
			return getBundleFromProject(bsn, attrs);
		}

		Strategy useStrategy = strategy;

		if ("latest".equals(range)) {
			Container c = getBundleFromProject(bsn, attrs);
			if (c != null)
				return c;

			useStrategy = Strategy.HIGHEST;
		}

		useStrategy = overrideStrategy(attrs, useStrategy);

		List<RepositoryPlugin> plugins = workspace.getRepositories();

		if (useStrategy == Strategy.EXACT) {

			// For an exact range we just iterate over the repos
			// and return the first we find.

			for (RepositoryPlugin plugin : plugins) {
				File result = plugin.get(bsn, range, Strategy.EXACT, attrs);
				if (result != null)
					return toContainer(bsn, range, attrs, result);
			}
		} else {
			VersionRange versionRange = "latest".equals(range) ? new VersionRange("0") : new VersionRange(range);

			// We have a range search. Gather all the versions in all the repos
			// and make a decision on that choice. If the same version is found
			// in
			// multiple repos we take the first

			SortedMap<Version,RepositoryPlugin> versions = new TreeMap<Version,RepositoryPlugin>();
			for (RepositoryPlugin plugin : plugins) {
				try {
					List<Version> vs = plugin.versions(bsn);
					if (vs != null) {
						for (Version v : vs) {
							if (!versions.containsKey(v) && versionRange.includes(v))
								versions.put(v, plugin);
						}
					}
				}
				catch (UnsupportedOperationException ose) {
					// We have a plugin that cannot list versions, try
					// if it has this specific version
					// The main reaosn for this code was the Maven Remote
					// Repository
					// To query, we must have a real version
					if (!versions.isEmpty() && Verifier.isVersion(range)) {
						File file = plugin.get(bsn, range, useStrategy, attrs);
						// and the entry must exist
						// if it does, return this as a result
						if (file != null)
							return toContainer(bsn, range, attrs, file);
					}
				}
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
					String version = provider.toString();
					File result = repo.get(bsn, version, Strategy.EXACT, attrs);
					if (result != null)
						return toContainer(bsn, version, attrs, result);
				} else
					msgs.FoundVersions_ForStrategy_ButNoProvider(versions, useStrategy);
			}
		}

		//
		// If we get this far we ran into an error somewhere

		return new Container(this, bsn, range, Container.TYPE.ERROR, null, bsn + ";version=" + range + " Not found in "
				+ plugins, null);

	}

	/**
	 * @param attrs
	 * @param useStrategy
	 * @return
	 */
	protected Strategy overrideStrategy(Map<String,String> attrs, Strategy useStrategy) {
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

	/**
	 * @param bsn
	 * @param range
	 * @param attrs
	 * @param result
	 * @return
	 */
	protected Container toContainer(String bsn, String range, Map<String,String> attrs, File result) {
		File f = result;
		if (f == null) {
			msgs.ConfusedNoContainerFile();
			f = new File("was null");
		}
		if (f.getName().endsWith("lib"))
			return new Container(this, bsn, range, Container.TYPE.LIBRARY, f, null, attrs);
		return new Container(this, bsn, range, Container.TYPE.REPO, f, null, attrs);
	}

	/**
	 * Look for the bundle in the workspace. The premise is that the bsn must
	 * start with the project name.
	 * 
	 * @param bsn
	 *            The bsn
	 * @param attrs
	 *            Any attributes
	 * @return
	 * @throws Exception
	 */
	private Container getBundleFromProject(String bsn, Map<String,String> attrs) throws Exception {
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

	/**
	 * Deploy the file (which must be a bundle) into the repository.
	 * 
	 * @param name
	 *            The repository name
	 * @param file
	 *            bundle
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
			Jar jar = new Jar(file);
			try {
				rp.put(jar);
				return;
			}
			catch (Exception e) {
				msgs.DeployingFile_On_Exception_(file, rp.getName(), e);
			}
			finally {
				jar.close();
			}
			return;
		}
		trace("No repo found " + file);
		throw new IllegalArgumentException("No repository found for " + file);
	}

	/**
	 * Deploy the file (which must be a bundle) into the repository.
	 * 
	 * @param file
	 *            bundle
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
		Parameters deploy = new Parameters(getProperty(DEPLOY));
		if (deploy.isEmpty()) {
			warning("Deploying but %s is not set to any repo", DEPLOY);
			return;
		}
		File[] outputs = getBuildFiles();
		for (File output : outputs) {
			Jar jar = new Jar(output);
			try {
				for (Deploy d : getPlugins(Deploy.class)) {
					trace("Deploying %s to: %s", jar, d);
					try {
						if (d.deploy(this, jar))
							trace("deployed %s successfully to %s", output, d);
					}
					catch (Exception e) {
						msgs.Deploying(e);
					}
				}
			}
			finally {
				jar.close();
			}
		}
	}

	/**
	 * Macro access to the repository ${repo;<bsn>[;<version>[;<low|high>]]}
	 */

	static String	_repoHelp	= "${repo ';'<bsn> [ ; <version> [; ('HIGHEST'|'LOWEST')]}";

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
		List<String> paths = new ArrayList<String>();

		for (String bsn : parts) {
			Container container = getBundle(bsn, version, strategy, null);
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
				paths.add(container.getFile().getAbsolutePath());
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
	 * @return
	 * @throws Exception
	 */
	public File[] build(boolean underTest) throws Exception {
		if (isNoBundles())
			return null;

		if (getProperty("-nope") != null) {
			warning("Please replace -nope with %s", NOBUNDLES);
			return null;
		}

		if (isStale()) {
			trace("building " + this);
			files = buildLocal(underTest);
		}

		return files;
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
		if ( workspace.isOffline()) {
			trace("working %s offline, so always stale", this);
			return true;
		}
		
		Set<Project> visited = new HashSet<Project>();
		return isStale(visited);
	}

	boolean isStale(Set<Project> visited) throws Exception {
		// When we do not generate anything ...
		if (isNoBundles())
			return false;

		if (visited.contains(this)) {
			msgs.CircularDependencyContext_Message_(this.getName(), visited.toString());
			return false;
		}

		visited.add(this);

		long buildTime = 0;

		files = getBuildFiles(false);
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

			if (dependency.isStale())
				return true;

			if (dependency.isNoBundles())
				continue;

			File[] deps = dependency.getBuildFiles();
			for (File f : deps) {
				if (f.lastModified() >= buildTime)
					return true;
			}
		}
		
		return false;
	}

	/**
	 * This method must only be called when it is sure that the project has been
	 * build before in the same session. It is a bit yucky, but ant creates
	 * different class spaces which makes it hard to detect we already build it.
	 * This method remembers the files in the appropriate instance vars.
	 * 
	 * @return
	 */

	public File[] getBuildFiles() throws Exception {
		return getBuildFiles(true);
	}

	public File[] getBuildFiles(boolean buildIfAbsent) throws Exception {
		if (files != null)
			return files;

		File f = new File(getTarget(), BUILDFILES);
		if (f.isFile()) {
			BufferedReader rdr = IO.reader(f);
			try {
				List<File> files = newList();
				for (String s = rdr.readLine(); s != null; s = rdr.readLine()) {
					s = s.trim();
					File ff = new File(s);
					if (!ff.isFile()) {
						// Originally we warned the user
						// but lets just rebuild. That way
						// the error is not noticed but
						// it seems better to correct,
						// See #154
						rdr.close();
						f.delete();
						break;
					}
					files.add(ff);
				}
				return this.files = files.toArray(new File[files.size()]);
			}
			finally {
				rdr.close();
			}
		}
		if (buildIfAbsent)
			return files = buildLocal(false);
		return files = null;
	}

	/**
	 * Build without doing any dependency checking. Make sure any dependent
	 * projects are built first.
	 * 
	 * @param underTest
	 * @return
	 * @throws Exception
	 */
	public File[] buildLocal(boolean underTest) throws Exception {
		if (isNoBundles())
			return null;

		File bfs = new File(getTarget(), BUILDFILES);
		bfs.delete();

		files = null;
		ProjectBuilder builder = getBuilder(null);
		try {
			if (underTest)
				builder.setProperty(Constants.UNDERTEST, "true");
			Jar jars[] = builder.builds();
			File[] files = new File[jars.length];

			getInfo(builder);

			if (isOk()) {
				this.files = files;

				for (int i = 0; i < jars.length; i++) {
					Jar jar = jars[i];
					files[i] = saveBuild(jar);
				}

				// Write out the filenames in the buildfiles file
				// so we can get them later evenin another process
				Writer fw = IO.writer(bfs);
				try {
					for (File f : files) {
						fw.append(f.getAbsolutePath());
						fw.append("\n");
					}
				}
				finally {
					fw.close();
				}
				getWorkspace().changedFile(bfs);
				return files;
			}
			return null;
		}
		finally {
			builder.close();
		}
	}

	/**
	 * Answer if this project does not have any output
	 * 
	 * @return
	 */
	public boolean isNoBundles() {
		return getProperty(NOBUNDLES) != null;
	}

	public File saveBuild(Jar jar) throws Exception {
		try {
			String bsn = jar.getName();
			File f = getOutputFile(bsn);
			String msg = "";
			if (!f.exists() || f.lastModified() < jar.lastModified()) {
				reportNewer(f.lastModified(), jar);
				f.delete();
				if (!f.getParentFile().isDirectory())
					f.getParentFile().mkdirs();
				jar.write(f);

				getWorkspace().changedFile(f);
			} else {
				msg = "(not modified since " + new Date(f.lastModified()) + ")";
			}
			trace(jar.getName() + " (" + f.getName() + ") " + jar.getResources().size() + " " + msg);
			return f;
		}
		finally {
			jar.close();
		}
	}

	public File getOutputFile(String bsn) throws Exception {
		return new File(getTarget(), bsn + ".jar");
	}

	private void reportNewer(long lastModified, Jar jar) {
		if (isTrue(getProperty(Constants.REPORTNEWER))) {
			StringBuilder sb = new StringBuilder();
			String del = "Newer than " + new Date(lastModified);
			for (Map.Entry<String,Resource> entry : jar.getResources().entrySet()) {
				if (entry.getValue().lastModified() > lastModified) {
					sb.append(del);
					del = ", \n     ";
					sb.append(entry.getKey());
				}
			}
			if (sb.length() > 0)
				warning(sb.toString());
		}
	}

	/**
	 * Refresh if we are based on stale data. This also implies our workspace.
	 */
	public boolean refresh() {
		boolean changed = false;
		if (isCnf()) {
			changed = workspace.refresh();
		}
		return super.refresh() || changed;
	}

	public boolean isCnf() {
		return getBase().getName().equals(Workspace.CNFDIR);
	}

	public void propertiesChanged() {
		super.propertiesChanged();
		preparedPaths = false;
		files = null;

	}

	public String getName() {
		return getBase().getName();
	}

	public Map<String,Action> getActions() {
		Map<String,Action> all = newMap();
		Map<String,Action> actions = newMap();
		fillActions(all);
		getWorkspace().fillActions(all);

		for (Map.Entry<String,Action> action : all.entrySet()) {
			String key = getReplacer().process(action.getKey());
			if (key != null && key.trim().length() != 0)
				actions.put(key, action.getValue());
		}
		return actions;
	}

	public void fillActions(Map<String,Action> all) {
		List<NamedAction> plugins = getPlugins(NamedAction.class);
		for (NamedAction a : plugins)
			all.put(a.getName(), a);

		Parameters actions = new Parameters(getProperty("-actions", DEFAULT_ACTIONS));
		for (Entry<String,Attrs> entry : actions.entrySet()) {
			String key = Processor.removeDuplicateMarker(entry.getKey());
			Action action;

			if (entry.getValue().get("script") != null) {
				// TODO check for the type
				action = new ScriptAction(entry.getValue().get("type"), entry.getValue().get("script"));
			} else {
				action = new ReflectAction(key);
			}
			String label = entry.getValue().get("label");
			all.put(label.toLowerCase(), action);
		}
	}

	public void release() throws Exception {
		release(false);
	}

	/**
	 * Release.
	 * 
	 * @param name
	 *            The repository name
	 * @throws Exception
	 */
	public void release(String name) throws Exception {
		release(name, false);
	}

	public void clean() throws Exception {
		File target = getTarget0();
		if (target.isDirectory() && target.getParentFile() != null) {
			IO.delete(target);
			target.mkdirs();
		}
		File output = getOutput0();
		if (getOutput().isDirectory())
			IO.delete(output);
		output.mkdirs();
	}

	public File[] build() throws Exception {
		return build(false);
	}

	public void run() throws Exception {
		ProjectLauncher pl = getProjectLauncher();
		pl.setTrace(isTrace());
		pl.launch();
	}

	public void test() throws Exception {
		clear();
		ProjectTester tester = getProjectTester();
		tester.setContinuous(isTrue(getProperty(Constants.TESTCONTINUOUS)));
		tester.prepare();

		if (!isOk()) {
			return;
		}
		int errors = tester.test();
		if (errors == 0) {
			System.err.println("No Errors");
		} else {
			if (errors > 0) {
				System.err.println(errors + " Error(s)");

			} else
				System.err.println("Error " + errors);
		}
	}

	/**
	 * This methods attempts to turn any jar into a valid jar. If this is a
	 * bundle with manifest, a manifest is added based on defaults. If it is a
	 * bundle, but not r4, we try to add the r4 headers.
	 * 
	 * @param descriptor
	 * @param in
	 * @return
	 * @throws Exception
	 */
	public Jar getValidJar(File f) throws Exception {
		Jar jar = new Jar(f);
		return getValidJar(jar, f.getAbsolutePath());
	}

	public Jar getValidJar(URL url) throws Exception {
		InputStream in = url.openStream();
		try {
			Jar jar = new Jar(url.getFile().replace('/', '.'), in, System.currentTimeMillis());
			return getValidJar(jar, url.toString());
		}
		finally {
			in.close();
		}
	}

	public Jar getValidJar(Jar jar, String id) throws Exception {
		Manifest manifest = jar.getManifest();
		if (manifest == null) {
			trace("Wrapping with all defaults");
			Builder b = new Builder(this);
			b.addClasspath(jar);
			b.setProperty("Bnd-Message", "Wrapped from " + id + "because lacked manifest");
			b.setProperty(Constants.EXPORT_PACKAGE, "*");
			b.setProperty(Constants.IMPORT_PACKAGE, "*;resolution:=optional");
			jar = b.build();
		} else if (manifest.getMainAttributes().getValue(Constants.BUNDLE_MANIFESTVERSION) == null) {
			trace("Not a release 4 bundle, wrapping with manifest as source");
			Builder b = new Builder(this);
			b.addClasspath(jar);
			b.setProperty(Constants.PRIVATE_PACKAGE, "*");
			b.mergeManifest(manifest);
			String imprts = manifest.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
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
		return getBase().getAbsolutePath();
	}

	/**
	 * Bump the version of this project. First check the main bnd file. If this
	 * does not contain a version, check the include files. If they still do not
	 * contain a version, then check ALL the sub builders. If not, add a version
	 * to the main bnd file.
	 * 
	 * @param mask
	 *            the mask for bumping, see {@link Macro#_version(String[])}
	 * @throws Exception
	 */
	public void bump(String mask) throws Exception {
		String pattern = "(Bundle-Version\\s*(:|=)\\s*)(([0-9]+(\\.[0-9]+(\\.[0-9]+)?)?))";
		String replace = "$1${version;" + mask + ";$3}";
		try {
			// First try our main bnd file
			if (replace(getPropertiesFile(), pattern, replace))
				return;

			trace("no version in bnd.bnd");

			// Try the included filed in reverse order (last has highest
			// priority)
			List<File> included = getIncluded();
			if (included != null) {
				List<File> copy = new ArrayList<File>(included);
				Collections.reverse(copy);

				for (File file : copy) {
					if (replace(file, pattern, replace)) {
						trace("replaced version in file %s", file);
						return;
					}
				}
			}
			trace("no version in included files");

			boolean found = false;

			// Replace in all sub builders.
			for (Builder sub : getSubBuilders()) {
				found |= replace(sub.getPropertiesFile(), pattern, replace);
			}

			if (!found) {
				trace("no version in sub builders, add it to bnd.bnd");
				String bndfile = IO.collect(getPropertiesFile());
				bndfile += "\n# Added by by bump\nBundle-Version: 0.0.0\n";
				IO.store(bndfile, getPropertiesFile());
			}
		}
		finally {
			forceRefresh();
		}
	}

	boolean replace(File f, String pattern, String replacement) throws IOException {
		final Macro macro = getReplacer();
		Sed sed = new Sed( new Replacer() {
			public String process(String line) {
				return macro.process(line);
			}
		}, f);
		sed.replace(pattern, replacement);
		return sed.doIt() > 0;
	}

	public void bump() throws Exception {
		bump(getProperty(BUMPPOLICY, "=+0"));
	}

	public void action(String command) throws Throwable {
		Map<String,Action> actions = getActions();

		Action a = actions.get(command);
		if (a == null)
			a = new ReflectAction(command);

		before(this, command);
		try {
			a.execute(this, command);
		}
		catch (Throwable t) {
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
			testPlugins.get(i).after(this, a, t);
		}
	}

	public String _findfile(String args[]) {
		File f = getFile(args[1]);
		List<String> files = new ArrayList<String>();
		tree(files, f, "", new Instruction(args[2]));
		return join(files);
	}

	void tree(List<String> list, File current, String path, Instruction instr) {
		if (path.length() > 0)
			path = path + "/";

		String subs[] = current.list();
		if (subs != null) {
			for (String sub : subs) {
				File f = new File(current, sub);
				if (f.isFile()) {
					if (instr.matches(sub) && !instr.isNegated())
						list.add(path + sub);
				} else
					tree(list, f, path + sub, instr);
			}
		}
	}

	public void refreshAll() {
		workspace.refresh();
		refresh();
	}

	@SuppressWarnings("unchecked")
	public void script(@SuppressWarnings("unused") String type, String script) throws Exception {
		// TODO check tyiping
		List<Scripter> scripters = getPlugins(Scripter.class);
		if (scripters.isEmpty()) {
			msgs.NoScripters_(script);
			return;
		}
		@SuppressWarnings("rawtypes")
		Map x = getProperties();
		scripters.get(0).eval(x, new StringReader(script));
	}

	public String _repos(@SuppressWarnings("unused") String args[]) throws Exception {
		List<RepositoryPlugin> repos = getPlugins(RepositoryPlugin.class);
		List<String> names = new ArrayList<String>();
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
		List<Container> result = new ArrayList<Container>();
		Collection< ? extends Builder> builders = getSubBuilders();

		for (Builder builder : builders) {
			Container c = new Container(this, builder.getBsn(), builder.getVersion(), Container.TYPE.PROJECT,
					getOutputFile(builder.getBsn()), null, null);
			result.add(c);
		}
		return result;

	}

	/**
	 * Return the builder associated with the give bnd file or null. The bnd.bnd
	 * file can contain -sub option. This option allows specifying files in the
	 * same directory that should drive the generation of multiple deliverables.
	 * This method figures out if the bndFile is actually one of the bnd files
	 * of a deliverable.
	 * 
	 * @param bndFile
	 *            A file pointing to a bnd file.
	 * @return null or the builder for a sub file.
	 * @throws Exception
	 */
	public Builder getSubBuilder(File bndFile) throws Exception {
		bndFile = bndFile.getCanonicalFile();

		// Verify that we are inside the project.
		File base = getBase().getCanonicalFile();
		if (!bndFile.getAbsolutePath().startsWith(base.getAbsolutePath()))
			return null;

		Collection< ? extends Builder> builders = getSubBuilders();
		for (Builder sub : builders) {
			File propertiesFile = sub.getPropertiesFile();
			if (propertiesFile != null) {
				if (propertiesFile.getCanonicalFile().equals(bndFile)) {
					// Found it!
					return sub;
				}
			}
		}
		return null;
	}

	/**
	 * Answer the container associated with a given bsn.
	 * 
	 * @param bndFile
	 *            A file pointing to a bnd file.
	 * @return null or the builder for a sub file.
	 * @throws Exception
	 */
	public Container getDeliverable(String bsn, @SuppressWarnings("unused") Map<String,String> attrs) throws Exception {
		Collection< ? extends Builder> builders = getSubBuilders();
		for (Builder sub : builders) {
			if (sub.getBsn().equals(bsn))
				return new Container(this, getOutputFile(bsn));
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
	 */
	public Collection< ? extends Builder> getSubBuilders() throws Exception {
		return getBuilder(null).getSubBuilders();
	}

	/**
	 * Calculate the classpath. We include our own runtime.jar which includes
	 * the test framework and we include the first of the test frameworks
	 * specified.
	 * 
	 * @throws Exception
	 */
	Collection<File> toFile(Collection<Container> containers) throws Exception {
		ArrayList<File> files = new ArrayList<File>();
		for (Container container : containers) {
			container.contributeFiles(files, this);
		}
		return files;
	}

	public Collection<String> getRunVM() {
		Parameters hdr = getParameters(RUNVM);
		return hdr.keySet();
	}

	public Map<String,String> getRunProperties() {
		return OSGiHeader.parseProperties(getProperty(RUNPROPERTIES));
	}

	/**
	 * Get a launcher.
	 * 
	 * @return
	 * @throws Exception
	 */
	public ProjectLauncher getProjectLauncher() throws Exception {
		return getHandler(ProjectLauncher.class, getRunpath(), LAUNCHER_PLUGIN, "biz.aQute.launcher");
	}

	public ProjectTester getProjectTester() throws Exception {
		return getHandler(ProjectTester.class, getTestpath(), TESTER_PLUGIN, "biz.aQute.junit");
	}

	private <T> T getHandler(Class<T> target, Collection<Container> containers, String header, String defaultHandler)
			throws Exception {
		Class< ? extends T> handlerClass = target;

		// Make sure we find at least one handler, but hope to find an earlier
		// one
		List<Container> withDefault = Create.list();
		withDefault.addAll(containers);
		withDefault.addAll(getBundles(Strategy.HIGHEST, defaultHandler, null));
		trace("candidates for tester %s", withDefault);

		for (Container c : withDefault) {
			Manifest manifest = c.getManifest();

			if (manifest != null) {
				String launcher = manifest.getMainAttributes().getValue(header);
				if (launcher != null) {
					Class< ? > clz = getClass(launcher, c.getFile());
					if (clz != null) {
						if (!target.isAssignableFrom(clz)) {
							msgs.IncompatibleHandler_For_(launcher, defaultHandler);
						} else {
							handlerClass = clz.asSubclass(target);
							Constructor< ? extends T> constructor = handlerClass.getConstructor(Project.class);
							return constructor.newInstance(this);
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
	 * Sets the package version on an exported package
	 * 
	 * @param packageName
	 *            The package name
	 * @param version
	 *            The new package version
	 */
	public void setPackageInfo(String packageName, Version version) {
		try {
			updatePackageInfoFile(packageName, version);
		}
		catch (Exception e) {
			msgs.SettingPackageInfoException_(e);
		}
	}

	void updatePackageInfoFile(String packageName, Version newVersion) throws Exception {

		File file = getPackageInfoFile(packageName);

		// If package/classes are copied into the bundle through Private-Package
		// etc, there will be no source
		if (!file.getParentFile().exists()) {
			return;
		}

		Version oldVersion = getPackageInfo(packageName);

		if (newVersion.compareTo(oldVersion) == 0) {
			return;
		}
		PrintWriter pw = IO.writer(file);
		pw.println("version " + newVersion);
		pw.flush();
		pw.close();

		String path = packageName.replace('.', '/') + "/packageinfo";
		File binary = IO.getFile(getOutput(), path);
		binary.getParentFile().mkdirs();
		IO.copy(file, binary);

		refresh();
	}

	File getPackageInfoFile(String packageName) {
		String path = packageName.replace('.', '/') + "/packageinfo";
		return IO.getFile(getSrc(), path);

	}

	public Version getPackageInfo(String packageName) throws IOException {
		File packageInfoFile = getPackageInfoFile(packageName);
		if (!packageInfoFile.exists()) {
			return Version.emptyVersion;
		}
		BufferedReader reader = null;
		try {
			reader = IO.reader(packageInfoFile);
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("version ")) {
					return Version.parseVersion(line.substring(8));
				}
			}
		}
		finally {
			if (reader != null) {
				IO.close(reader);
			}
		}
		return Version.emptyVersion;
	}

	/**
	 * bnd maintains a class path that is set by the environment, i.e. bnd is
	 * not in charge of it.
	 */

	public void addClasspath(File f) {
		if (!f.isFile() && !f.isDirectory()) {
			msgs.AddingNonExistentFileToClassPath_(f);
		}
		Container container = new Container(f);
		classpath.add(container);
	}

	public void clearClasspath() {
		classpath.clear();
	}

	public Collection<Container> getClasspath() {
		return classpath;
	}
}

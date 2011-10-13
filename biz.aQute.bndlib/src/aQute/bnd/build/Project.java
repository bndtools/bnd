package aQute.bnd.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.Manifest;

import aQute.bnd.help.Syntax;
import aQute.bnd.maven.support.Pom;
import aQute.bnd.maven.support.ProjectPom;
import aQute.bnd.service.CommandPlugin;
import aQute.bnd.service.DependencyContributor;
import aQute.bnd.service.Deploy;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import aQute.bnd.service.Scripter;
import aQute.bnd.service.action.Action;
import aQute.bnd.service.action.NamedAction;
import aQute.lib.io.IO;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Constants;
import aQute.lib.osgi.Instruction;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.Processor;
import aQute.lib.osgi.Resource;
import aQute.lib.osgi.eclipse.EclipseClasspath;
import aQute.libg.generics.Create;
import aQute.libg.header.OSGiHeader;
import aQute.libg.sed.Sed;

/**
 * This class is NOT threadsafe
 * 
 * @author aqute
 * 
 */

public class Project extends Processor {

	final static String			DEFAULT_ACTIONS			= "build; label='Build', test; label='Test', run; label='Run', clean; label='Clean', release; label='Release', refreshAll; label=Refresh, deploy;label=Deploy";
	public final static String	BNDFILE					= "bnd.bnd";
	public final static String	BNDCNF					= "cnf";
	final Workspace				workspace;
	boolean						preparedPaths;
	final Collection<Project>	dependson				= new LinkedHashSet<Project>();
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
	private long				buildtime;
	static List<Project>		trail					= new ArrayList<Project>();
	boolean						delayRunDependencies	= false;

	public Project(Workspace workspace, File projectDir, File buildFile) throws Exception {
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
				for (Enumeration<?> e = p.propertyNames(); e.hasMoreElements();) {
					String key = (String) e.nextElement();
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
					output = getFile(getProperty("bin", "bin")).getAbsoluteFile();
					if (!output.exists()) {
						output.mkdirs();
						getWorkspace().changedFile(output);
					}
					if (!output.isDirectory())
						error("Can not find output directory: " + output);
					else if (!buildpath.contains(output))
						buildpath.add(new Container(this, output));

					// Where we store all our generated stuff.
					target = getFile(getProperty("target", "generated"));
					if (!target.exists()) {
						target.mkdirs();
						getWorkspace().changedFile(target);
					}
					
					// Where the launched OSGi framework stores stuff
					String runStorageStr = getProperty(Constants.RUNSTORAGE);
					runstorage = runStorageStr != null ? getFile(runStorageStr) : null;

					// We might have some other projects we want build
					// before we do anything, but these projects are not in
					// our path. The -dependson allows you to build them before.

					List<Project> dependencies = new ArrayList<Project>();
					// dependencies.add( getWorkspace().getProject("cnf"));

					String dp = getProperty(Constants.DEPENDSON);
					Set<String> requiredProjectNames = parseHeader(dp).keySet();
					List<DependencyContributor> dcs = getPlugins(DependencyContributor.class);
					for (DependencyContributor dc : dcs)
						dc.addDependencies(this, requiredProjectNames);

					for (String p : requiredProjectNames) {
						Project required = getWorkspace().getProject(p);
						if (required == null)
							error("No such project " + required + " on " + Constants.DEPENDSON);
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
				} finally {
					inPrepare = false;
				}
			}
		} finally {
			trail.remove(this);
		}
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
	private void doPath(Collection<Container> resultpath, Collection<Project> projects,
			Collection<Container> entries, Collection<Container> bootclasspath) {
		for (Container cpe : entries) {
			if (cpe.getError() != null)
				error(cpe.getError());
			else {
				if (cpe.getType() == Container.TYPE.PROJECT) {
					projects.add(cpe.getProject());
				}
				if (bootclasspath != null && cpe.getBundleSymbolicName().startsWith("ee.")
						|| cpe.getAttributes().containsKey("boot"))
					bootclasspath.add(cpe);
				else
					resultpath.add(cpe);
			}
		}
	}

	/**
	 * Parse the list of bundles that are a prerequisite to this project.
	 * 
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
		Map<String, Map<String, String>> bundles = parseHeader(spec);

		try {
			for (Iterator<Map.Entry<String, Map<String, String>>> i = bundles.entrySet().iterator(); i
					.hasNext();) {
				Map.Entry<String, Map<String, String>> entry = i.next();
				String bsn = entry.getKey();
				Map<String, String> attrs = entry.getValue();

				Container found = null;

				// Check if we have to use the maven pom ...
				if (bsn.equals("pom")) {
					doMavenPom(strategyx, result, attrs.get("scope"));
					continue;
				}

				String versionRange = attrs.get("version");

				if (versionRange != null) {
					if (versionRange.equals("latest") || versionRange.equals("snapshot")) {
						found = getBundle(bsn, versionRange, strategyx, attrs);
					}
				}
				if (found == null) {
					if (versionRange != null
							&& (versionRange.equals("project") || versionRange.equals("latest"))) {
						Project project = getWorkspace().getProject(bsn);
						if (project != null && project.exists()) {
							File f = project.getOutput();
							found = new Container(project, bsn, "project", Container.TYPE.PROJECT,
									f, null, attrs);
						} else {
							error("Reference to project that does not exist in workspace\n"
									+ "  Project       %s\n" + "  Specification %s", bsn, spec);
							continue;
						}
					} else if (versionRange != null && versionRange.equals("file")) {
						File f = getFile(bsn);
						String error = null;
						if (!f.exists())
							error = "File does not exist: " + f.getAbsolutePath();
						if (f.getName().endsWith(".lib")) {
							found = new Container(this, bsn, "file", Container.TYPE.LIBRARY, f,
									error, attrs);
						} else {
							found = new Container(this, bsn, "file", Container.TYPE.EXTERNAL, f,
									error, attrs);
						}
					} else {
						found = getBundle(bsn, versionRange, strategyx, attrs);
					}
				}

				if (found != null) {
					List<Container> libs = found.getMembers();
					for (Container cc : libs) {
						if (result.contains(cc))
							warning("Multiple bundles with the same final URL: " + cc);

						result.add(cc);
					}
				} else {
					// Oops, not a bundle in sight :-(
					Container x = new Container(this, bsn, versionRange, Container.TYPE.ERROR,
							null, bsn + ";version=" + versionRange + " not found", attrs);
					result.add(x);
					warning("Can not find URL for bsn " + bsn);
				}
			}
		} catch (CircularDependencyException e) {
			String message = e.getMessage();
			if (source != null)
				message = String.format("%s (from property: %s)", message, source);
			error("Circular dependency detected from project %s: %s", e, getName(), message);
		} catch (Exception e) {
			error("Unexpected error while trying to get the bundles from " + spec, e);
			e.printStackTrace();
		}
		return result;
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
	public void appendPackages(Strategy strategyx, String spec, List<Container> resolvedBundles, ResolverMode mode) throws Exception {
		Map<File, Container> pkgResolvedBundles = new HashMap<File, Container>();
		
		List<Entry<String, Map<String, String>>> queue = new LinkedList<Map.Entry<String,Map<String,String>>>();
		queue.addAll(parseHeader(spec).entrySet());
		
		while (!queue.isEmpty()) {
			Entry<String, Map<String, String>> entry = queue.remove(0);
			
			String pkgName = entry.getKey();
			Map<String, String> attrs = entry.getValue();
			
			Container found = null;
			
			String versionRange = attrs.get(Constants.VERSION_ATTRIBUTE);
			if ("latest".equals(versionRange) || "snapshot".equals(versionRange))
				found = getPackage(pkgName, versionRange, strategyx, attrs, mode);

			if (found == null)
				found = getPackage(pkgName, versionRange, strategyx, attrs, mode);

			if (found != null) {
				if (resolvedBundles.contains(found)) {
					// Don't add his bundle because it was already included using -buildpath
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
							queue.addAll(0, parseHeader(importUses).entrySet());
					}
				}
			} else {
				// Unable to resolve
				Container x = new Container(this, "X", versionRange, Container.TYPE.ERROR, null, "package " + pkgName + ";version=" + versionRange + " not found", attrs);
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
			if (!first) builder.append(',');
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
	public Container getPackage(String packageName, String range, Strategy strategyx, Map<String, String> attrs, ResolverMode mode) throws Exception {
		if ("snapshot".equals(range))
			return new Container(this, "", range, Container.TYPE.ERROR, null, "snapshot not supported for package lookups", null);
		
		if (attrs == null)
			attrs = new HashMap<String, String>(2);
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
					else
						return new Container(this, result.getName(), range, Container.TYPE.REPO, result, null, attrs);
				}
			} catch (Exception e) {
				// Ignore... lots of repos will fail here
			}
		}
		
		return new Container(this, "X", range, Container.TYPE.ERROR, null, "package " + packageName + ";version=" + range + " Not found in " + plugins, null);
	}

	private Strategy findStrategy(Map<String, String> attrs, Strategy defaultStrategy, String versionRange) {
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
	public void doMavenPom(Strategy strategyx, List<Container> result, String action)
			throws Exception {
		File pomFile = getFile("pom.xml");
		if (!pomFile.isFile())
			error("Specified to use pom.xml but the project directory does not contain a pom.xml file");
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

	private Collection<?> toFiles(Collection<Project> projects) {
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

	private String list(String[] args, Collection<?> list) {
		if (args.length > 3)
			throw new IllegalArgumentException("${" + args[0]
					+ "[;<separator>]} can only take a separator as argument, has "
					+ Arrays.toString(args));

		String separator = ",";

		if (args.length == 2) {
			separator = args[1];
		}

		return join(list, separator);
	}

	protected Object[] getMacroDomains() {
		return new Object[] { workspace };
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
				return rp.put(jar);
			} catch (Exception e) {
				error("Deploying " + jar.getName() + " on " + rp.getName(), e);
			} finally {
				jar.close();
			}
		}
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
		File[] jars = build(test);
		// If build fails jars will be null
		if (jars == null) {
			return;
		}
		for (File jar : jars) {
			Jar j = new Jar(jar);
			release(name, j);
			j.close();
		}

	}

	/**
	 * Get a bundle from one of the plugin repositories.
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
	public Container getBundle(String bsn, String range, Strategy strategyx,
			Map<String, String> attrs) throws Exception {

		if ("snapshot".equals(range)) {
			return getBundleFromProject(bsn, attrs);
		}

		if ("latest".equals(range)) {
			Container c = getBundleFromProject(bsn, attrs);
			if (c != null)
				return c;
		}

		List<RepositoryPlugin> plugins = getPlugins(RepositoryPlugin.class);
		;

		Strategy useStrategy = strategyx;
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

		// If someone really wants the latest, lets give it to them.
		// regardless of they asked for a lowest strategy
		if (range != null && range.equals("latest"))
			useStrategy = Strategy.HIGHEST;

		// if ( bsn.indexOf('+')>0) {
		// return getMavenContainer(bsn,range,useStrategy,attrs);
		// }

		// Maybe we want an exact match this time.
		// In that case we limit the range to be exactly
		// the version specified. We ignore it when a range
		// is used instead of a version

		for (RepositoryPlugin plugin : plugins) {
			File result = plugin.get(bsn, range, useStrategy, attrs);
			if (result != null) {
				if (result.getName().endsWith("lib"))
					return new Container(this, bsn, range, Container.TYPE.LIBRARY, result, null,
							attrs);
				else
					return new Container(this, bsn, range, Container.TYPE.REPO, result, null, attrs);
			}
		}

		return new Container(this, bsn, range, Container.TYPE.ERROR, null, bsn + ";version="
				+ range + " Not found in " + plugins, null);
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
		;
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
			} catch (Exception e) {
				error("Deploying " + file + " on " + rp.getName(), e);
			} finally {
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
		Map<String, Map<String, String>> deploy = parseHeader(getProperty(DEPLOY));
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
					} catch (Exception e) {
						error("Error while deploying %s, %s", this, e);
						e.printStackTrace();
					}
				}
			} finally {
				jar.close();
			}
		}
	}

	/**
	 * Macro access to the repository
	 * 
	 * ${repo;<bsn>[;<version>[;<low|high>]]}
	 */

	public String _repo(String args[]) throws Exception {
		if (args.length < 2)
			throw new IllegalArgumentException(
					"Too few arguments for repo, syntax=: ${repo ';'<bsn> [ ; <version> [; ('HIGHEST'|'LOWEST')]}");

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
					error("${repo;<bsn>;<version>;<'highest'|'lowest'|'exact'>} macro requires a strategy of 'highest' or 'lowest', and is "
							+ args[3]);
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
				paths.add("<<${repo} = " + container.getBundleSymbolicName() + "-"
						+ container.getVersion() + " : " + container.getError() + ">>");

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
		if (getProperty(NOBUNDLES) != null)
			return null;

		if (getProperty("-nope") != null) {
			warning("Please replace -nope with %s", NOBUNDLES);
			return null;
		}

		// Check if we have a local modification not yet build
		boolean outofdate = false;

		// Check for each dependency if it is locally modified or
		// its build time > our build time.
		for (Project dependency : getDependson()) {
			if (dependency != this) {
				if (outofdate || dependency.getBuildTime() <= dependency.lastModified()) {
					dependency.buildLocal(false);
					outofdate = true;
				}
			}
		}

		if (files == null || outofdate || getBuildTime() <= lastModified()) {
			trace("Building " + this);
			files = buildLocal(underTest);
		}

		return files;
	}

	/**
	 * This method must only be called when it is sure that the project has been
	 * build before in the same session.
	 * 
	 * It is a bit yucky, but ant creates different class spaces which makes it
	 * hard to detect we already build it.
	 * 
	 * @return
	 */

	public File[] getBuildFiles() throws Exception {
		return getBuildFiles(true);
	}

	public File[] getBuildFiles(boolean buildIfAbsent) throws Exception {
		File f = new File(getTarget(), BUILDFILES);
		if (f.isFile()) {
			FileReader fin = new FileReader(f);
			BufferedReader rdr = new BufferedReader(fin);
			try {
				List<File> files = newList();
				for (String s = rdr.readLine(); s != null; s = rdr.readLine()) {
					s = s.trim();
					File ff = new File(s);
					if (!ff.isFile()) {
						error("buildfile lists file but the file does not exist %s", ff);
					} else
						files.add(ff);
				}
				return files.toArray(new File[files.size()]);
			} finally {
				fin.close();
			}
		}
		if (buildIfAbsent)
			return buildLocal(false);
		else
			return null;
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
		if (getProperty(NOBUNDLES) != null)
			return null;

		long buildtime = System.currentTimeMillis();
		File bfs = new File(getTarget(), BUILDFILES);
		bfs.delete();

		files = null;
		ProjectBuilder builder = getBuilder(null);
		if (underTest)
			builder.setProperty(Constants.UNDERTEST, "true");
		Jar jars[] = builder.builds();
		File[] files = new File[jars.length];

		for (int i = 0; i < jars.length; i++) {
			Jar jar = jars[i];
			files[i] = saveBuild(jar);
		}
		getInfo(builder);
		builder.close();
		if (isOk()) {
			this.files = files;

			// Write out the filenames in the buildfiles file
			// so we can get them later evenin another process
			FileWriter fw = new FileWriter(bfs);
			try {
				for (File f : files) {
					fw.append(f.getAbsolutePath());
					fw.append("\n");
				}
			} finally {
				fw.close();
			}
			getWorkspace().changedFile(bfs);
			this.buildtime = buildtime;
			return files;
		} else
			return null;
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
		} finally {
			jar.close();
		}
	}

	private File getOutputFile(String bsn) throws Exception {
		return new File(getTarget(), bsn + ".jar");
	}

	private void reportNewer(long lastModified, Jar jar) {
		if (isTrue(getProperty(Constants.REPORTNEWER))) {
			StringBuilder sb = new StringBuilder();
			String del = "Newer than " + new Date(lastModified);
			for (Map.Entry<String, Resource> entry : jar.getResources().entrySet()) {
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

	public Map<String, Action> getActions() {
		Map<String, Action> all = newMap();
		Map<String, Action> actions = newMap();
		fillActions(all);
		getWorkspace().fillActions(all);

		for (Map.Entry<String, Action> action : all.entrySet()) {
			String key = getReplacer().process(action.getKey());
			if (key != null && key.trim().length() != 0)
				actions.put(key, action.getValue());
		}
		return actions;
	}

	public void fillActions(Map<String, Action> all) {
		List<NamedAction> plugins = getPlugins(NamedAction.class);
		for (NamedAction a : plugins)
			all.put(a.getName(), a);

		Map<String, Map<String, String>> actions = parseHeader(getProperty("-actions",
				DEFAULT_ACTIONS));
		for (Map.Entry<String, Map<String, String>> entry : actions.entrySet()) {
			String key = Processor.removeDuplicateMarker(entry.getKey());
			Action action;

			if (entry.getValue().get("script") != null) {
				// TODO check for the type
				action = new ScriptAction(entry.getValue().get("type"), entry.getValue().get(
						"script"));
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
		File target = getTarget();
		if (target.isDirectory() && target.getParentFile() != null) {
			IO.delete(target);
		}
		if (getOutput().isDirectory())
			IO.delete(getOutput());
		getOutput().mkdirs();
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
		tester.setContinuous(isTrue( getProperty(Constants.TESTCONTINUOUS)));
		tester.prepare();

		if (!isOk()) {
			return;
		}
		int errors = tester.test();
		if (errors == 0) {
			System.out.println("No Errors");
		} else {
			if (errors > 0) {
				System.out.println(errors + " Error(s)");

			} else
				System.out.println("Error " + errors);
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
		} finally {
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

	public String _project(String args[]) {
		return getBase().getAbsolutePath();
	}

	public void bump(String mask) throws IOException {
		Sed sed = new Sed(getReplacer(), getPropertiesFile());
		sed.replace("(Bundle-Version\\s*(:|=)\\s*)(([0-9]+(\\.[0-9]+(\\.[0-9]+)?)?))",
				"$1${version;" + mask + ";$3}");
		sed.doIt();
		refresh();
	}

	public void bump() throws IOException {
		bump(getProperty(BUMPPOLICY, "=+0"));
	}

	public void action(String command) throws Throwable {
		Map<String, Action> actions = getActions();

		Action a = actions.get(command);
		if (a == null)
			a = new ReflectAction(command);

		before(this, command);
		try {
			a.execute(this, command);
		} catch (Throwable t) {
			after(this, command, t);
			throw t;
		}
	}

	/**
	 * Run all before command plugins
	 * 
	 */
	void before(Project p, String a) {
		List<CommandPlugin> testPlugins = getPlugins(CommandPlugin.class);
		for (CommandPlugin testPlugin : testPlugins) {
			testPlugin.before(this, a);
		}
	}

	/**
	 * Run all after command plugins
	 */
	void after(Project p, String a, Throwable t) {
		List<CommandPlugin> testPlugins = getPlugins(CommandPlugin.class);
		for (int i = testPlugins.size() - 1; i >= 0; i--) {
			testPlugins.get(i).after(this, a, t);
		}
	}

	public String _findfile(String args[]) {
		File f = getFile(args[1]);
		List<String> files = new ArrayList<String>();
		tree(files, f, "", Instruction.getPattern(args[2]));
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

	@SuppressWarnings("unchecked") public void script(String type, String script) throws Exception {
		// TODO check tyiping
		List<Scripter> scripters = getPlugins(Scripter.class);
		if (scripters.isEmpty()) {
			error("Can not execute script because there are no scripters registered: %s", script);
			return;
		}
		@SuppressWarnings("rawtypes")
		Map x = (Map) getProperties();
		scripters.get(0).eval((Map<String, Object>) x, new StringReader(script));
	}

	public String _repos(String args[]) throws Exception {
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
		if (what == null || what.equals("example"))
			return syntax.getExample();
		if (what == null || what.equals("pattern"))
			return syntax.getPattern();
		if (what == null || what.equals("values"))
			return syntax.getValues();

		return "Invalid type specified for help: lead, example, pattern, values";
	}

	/**
	 * Returns containers for the deliverables of this project. The deliverables
	 * is the project builder for this project if no -sub is specified.
	 * Otherwise it contains all the sub bnd files.
	 * 
	 * @return A collection of containers
	 * 
	 * @throws Exception
	 */
	public Collection<Container> getDeliverables() throws Exception {
		List<Container> result = new ArrayList<Container>();
		Collection<? extends Builder> builders = getSubBuilders();

		for (Builder builder : builders) {
			Container c = new Container(this, builder.getBsn(), builder.getVersion(),
					Container.TYPE.PROJECT, getOutputFile(builder.getBsn()), null, null);
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

		Collection<? extends Builder> builders = getSubBuilders();
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
	public Container getDeliverable(String bsn, Map<String, String> attrs) throws Exception {
		Collection<? extends Builder> builders = getSubBuilders();
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
	public Collection<? extends Builder> getSubBuilders() throws Exception {
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
		Map<String, Map<String, String>> hdr = parseHeader(getProperty(RUNVM));
		return hdr.keySet();
	}

	public Map<String, String> getRunProperties() {
		return OSGiHeader.parseProperties(getProperty(RUNPROPERTIES));
	}

	/**
	 * Get a launcher.
	 * 
	 * @return
	 * @throws Exception
	 */
	public ProjectLauncher getProjectLauncher() throws Exception {
		return getHandler(ProjectLauncher.class, getRunpath(), LAUNCHER_PLUGIN,
				"biz.aQute.launcher");
	}

	public ProjectTester getProjectTester() throws Exception {
		return getHandler(ProjectTester.class, getTestpath(), TESTER_PLUGIN, "biz.aQute.junit");
	}

	private <T> T getHandler(Class<T> target, Collection<Container> containers, String header,
			String defaultHandler) throws Exception {
		Class<? extends T> handlerClass = target;

		// Make sure we find at least one handler, but hope to find an earlier
		// one
		List<Container> withDefault = Create.list();
		withDefault.addAll(containers);
		withDefault.addAll(getBundles(Strategy.HIGHEST, defaultHandler, null));

		for (Container c : withDefault) {
			Manifest manifest = c.getManifest();

			if (manifest != null) {
				String launcher = manifest.getMainAttributes().getValue(header);
				if (launcher != null) {
					Class<?> clz = getClass(launcher, c.getFile());
					if (clz != null) {
						if (!target.isAssignableFrom(clz)) {
							error("Found a %s class in %s but it is not compatible with: %s", clz,
									c, target);
						} else {
							handlerClass = clz.asSubclass(target);
							Constructor<? extends T> constructor = handlerClass
									.getConstructor(Project.class);
							return constructor.newInstance(this);
						}
					}
				}
			}
		}
		throw new IllegalArgumentException("Default handler for " + header + " not found in "
				+ defaultHandler);
	}

	public synchronized boolean lock(String reason) throws InterruptedException {
		if (!lock.tryLock(5, TimeUnit.SECONDS)) {
			error("Could not acquire lock for %s, was locked by %s for %s", reason, lockingThread,
					lockingReason);
			System.out.printf("Could not acquire lock for %s, was locked by %s for %s\n", reason,
					lockingThread, lockingReason);
			System.out.flush();
			return false;
		}
		this.lockingReason = reason;
		this.lockingThread = Thread.currentThread();
		return true;
	}

	public void unlock() {
		lockingReason = null;
		lock.unlock();
	}

	public long getBuildTime() throws Exception {
		if (buildtime == 0) {

			files = getBuildFiles();
			if (files != null && files.length >= 1)
				buildtime = files[0].lastModified();
		}
		return buildtime;
	}

	/**
	 * Make this project delay the calculation of the run dependencies.
	 * 
	 * The run dependencies calculation can be done in prepare or until the
	 * dependencies are actually needed.
	 */
	public void setDelayRunDependencies(boolean x) {
		delayRunDependencies = x;
	}

}

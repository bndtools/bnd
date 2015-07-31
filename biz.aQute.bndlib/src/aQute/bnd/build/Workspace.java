package aQute.bnd.build;

import java.io.*;
import java.lang.ref.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.jar.*;
import java.util.regex.*;

import javax.naming.*;

import aQute.bnd.annotation.plugin.*;
import aQute.bnd.exporter.subsystem.*;
import aQute.bnd.header.*;
import aQute.bnd.maven.support.*;
import aQute.bnd.osgi.*;
import aQute.bnd.resource.repository.*;
import aQute.bnd.service.*;
import aQute.bnd.service.action.*;
import aQute.bnd.service.extension.*;
import aQute.bnd.service.lifecycle.*;
import aQute.bnd.service.repository.*;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.url.*;
import aQute.bnd.version.*;
import aQute.lib.deployer.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.settings.*;
import aQute.lib.strings.*;
import aQute.lib.utf8properties.*;
import aQute.lib.zip.*;
import aQute.service.reporter.*;

public class Workspace extends Processor {
	public static final String					EXT				= "ext";

	static final int							BUFFER_SIZE		= IOConstants.PAGE_SIZE * 16;

	public static final String					BUILDFILE		= "build.bnd";
	public static final String					CNFDIR			= "cnf";
	public static final String					BNDDIR			= "bnd";
	public static final String					CACHEDIR		= "cache";

	static Map<File,WeakReference<Workspace>>	cache			= newHashMap();
	static Processor							defaults		= null;
	final Map<String,Project>					models			= newHashMap();
	final Map<String,Action>					commands		= newMap();
	final File									buildDir;
	final Maven									maven			= new Maven(Processor.getExecutor());
	private boolean								offline			= true;
	Settings									settings		= new Settings();
	WorkspaceRepository							workspaceRepo	= new WorkspaceRepository(this);
	static String								overallDriver	= "unset";
	static Parameters							overallGestalt	= new Parameters();
	/**
	 * Signal a BndListener plugin. We ran an infinite bug loop :-(
	 */
	final ThreadLocal<Reporter>					signalBusy		= new ThreadLocal<Reporter>();
	ResourceRepositoryImpl						resourceRepositoryImpl;

	private Parameters							gestalt;

	private String								driver;

	/**
	 * This static method finds the workspace and creates a project (or returns
	 * an existing project)
	 * 
	 * @param projectDir
	 * @return
	 */
	public static Project getProject(File projectDir) throws Exception {
		projectDir = projectDir.getAbsoluteFile();
		assert projectDir.isDirectory();

		Workspace ws = getWorkspace(projectDir.getParentFile());
		return ws.getProject(projectDir.getName());
	}

	static synchronized public Processor getDefaults() {
		if (defaults != null)
			return defaults;

		UTF8Properties props = new UTF8Properties();
		InputStream propStream = Workspace.class.getResourceAsStream("defaults.bnd");
		if (propStream != null) {
			try {
				props.load(propStream);
			}
			catch (IOException e) {
				throw new IllegalArgumentException("Unable to load bnd defaults.", e);
			}
			finally {
				IO.close(propStream);
			}
		} else
			System.err.println("Cannot load defaults");
		defaults = new Processor(props, false);

		return defaults;
	}

	public static Workspace getWorkspace(File parent) throws Exception {
		return getWorkspace(parent, CNFDIR);
	}

	public static Workspace getWorkspaceWithoutException(File parent) throws Exception {
		try {
			return getWorkspace(parent);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * /* Return the nearest workspace
	 */
	public static Workspace findWorkspace(File base) throws Exception {
		File rover = base;
		while (rover != null) {
			File file = IO.getFile(rover, "cnf/build.bnd");
			if (file.isFile())
				return getWorkspace(rover);

			rover = rover.getParentFile();
		}
		return null;
	}

	public static Workspace getWorkspace(File parent, String bndDir) throws Exception {
		File workspaceDir = parent.getAbsoluteFile();

		// the cnf directory can actually be a
		// file that redirects
		while (workspaceDir.isDirectory()) {
			File test = new File(workspaceDir, CNFDIR);

			if (!test.exists())
				test = new File(workspaceDir, bndDir);

			if (test.isDirectory())
				break;

			if (test.isFile()) {
				String redirect = IO.collect(test).trim();
				test = getFile(test.getParentFile(), redirect).getAbsoluteFile();
				workspaceDir = test;
			}
			if (!test.exists())
				throw new IllegalArgumentException("No Workspace found from: " + parent);
		}

		synchronized (cache) {
			WeakReference<Workspace> wsr = cache.get(workspaceDir);
			Workspace ws;
			if (wsr == null || (ws = wsr.get()) == null) {
				ws = new Workspace(workspaceDir, bndDir);
				cache.put(workspaceDir, new WeakReference<Workspace>(ws));
			}
			return ws;
		}
	}

	public Workspace(File dir) throws Exception {
		this(dir, CNFDIR);
	}

	public Workspace(File dir, String bndDir) throws Exception {
		super(getDefaults());
		dir = dir.getAbsoluteFile();
		if (!dir.exists() && !dir.mkdirs()) {
			throw new IOException("Could not create directory " + dir);
		}
		assert dir.isDirectory();

		File buildDir = new File(dir, bndDir).getAbsoluteFile();
		if (!buildDir.isDirectory())
			buildDir = new File(dir, CNFDIR).getAbsoluteFile();

		this.buildDir = buildDir;

		File buildFile = new File(buildDir, BUILDFILE).getAbsoluteFile();
		if (!buildFile.isFile())
			warning("No Build File in " + dir);

		setProperties(buildFile, dir);
		propertiesChanged();

		//
		// There is a nasty bug/feature in Java that gives errors on our
		// SSL use of github. The flag jsse.enableSNIExtension should be set
		// to false. So here we provide a way to set system properties
		// as early as possible
		//

		Attrs sysProps = OSGiHeader.parseProperties(mergeProperties(SYSTEMPROPERTIES));
		for (Entry<String,String> e : sysProps.entrySet()) {
			System.setProperty(e.getKey(), e.getValue());
		}

	}

	public Project getProject(String bsn) throws Exception {
		synchronized (models) {
			Project project = models.get(bsn);
			if (project != null)
				return project;

			File projectDir = getFile(bsn);
			project = new Project(this, projectDir);
			if (!project.isValid())
				return null;

			models.put(bsn, project);
			return project;
		}
	}

	void removeProject(Project p) throws Exception {
		if (p.isCnf())
			return;
		
		synchronized (models) {
			models.remove(p.getName());
		}
		for ( LifeCyclePlugin lp : getPlugins(LifeCyclePlugin.class)) {
			lp.delete(p);
		}
	}

	public boolean isPresent(String name) {
		return models.containsKey(name);
	}

	public Collection<Project> getCurrentProjects() {
		return models.values();
	}

	@Override
	public boolean refresh() {
		if (super.refresh()) {
			for (Project project : getCurrentProjects()) {
				project.propertiesChanged();
			}
			return true;
		}
		return false;
	}

	@Override
	public void propertiesChanged() {
		File extDir = new File(this.buildDir, EXT);
		File[] extensions = extDir.listFiles();
		if (extensions != null) {
			for (File extension : extensions) {
				String extensionName = extension.getName();
				if (extensionName.endsWith(".bnd")) {
					extensionName = extensionName.substring(0, extensionName.length() - ".bnd".length());
					try {
						doIncludeFile(extension, false, getProperties(), "ext." + extensionName);
					}
					catch (Exception e) {
						error("PropertiesChanged: " + e.getMessage());
					}
				}
			}
		}
		super.propertiesChanged();
	}

	public String _workspace(@SuppressWarnings("unused") String args[]) {
		return getBase().getAbsolutePath();
	}

	public void addCommand(String menu, Action action) {
		commands.put(menu, action);
	}

	public void removeCommand(String menu) {
		commands.remove(menu);
	}

	public void fillActions(Map<String,Action> all) {
		all.putAll(commands);
	}

	public Collection<Project> getAllProjects() throws Exception {
		List<Project> projects = new ArrayList<Project>();
		for (File file : getBase().listFiles()) {
			if (new File(file, Project.BNDFILE).isFile()) {
				Project p = getProject(file.getAbsoluteFile().getName());
				if (p != null) {
					projects.add(p);
				}
			}
		}
		return projects;
	}

	/**
	 * Inform any listeners that we changed a file (created/deleted/changed).
	 * 
	 * @param f
	 *            The changed file
	 */
	public void changedFile(File f) {
		List<BndListener> listeners = getPlugins(BndListener.class);
		for (BndListener l : listeners)
			try {
				offline = false;
				l.changed(f);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
	}

	public void bracket(boolean begin) {
		List<BndListener> listeners = getPlugins(BndListener.class);
		for (BndListener l : listeners)
			try {
				if (begin)
					l.begin();
				else
					l.end();
			}
			catch (Exception e) {
				// who cares?
			}
	}

	public void signal(Reporter reporter) {
		if (signalBusy.get() != null)
			return;

		signalBusy.set(reporter);
		try {
			List<BndListener> listeners = getPlugins(BndListener.class);
			for (BndListener l : listeners)
				try {
					l.signal(this);
				}
				catch (Exception e) {
					// who cares?
				}
		}
		catch (Exception e) {
			// Ignore
		}
		finally {
			signalBusy.set(null);
		}
	}

	@Override
	public void signal() {
		signal(this);
	}

	void copy(InputStream in, OutputStream out) throws Exception {
		byte data[] = new byte[BUFFER_SIZE];
		int size = in.read(data);
		while (size > 0) {
			out.write(data, 0, size);
			size = in.read(data);
		}
	}

	class CachedFileRepo extends FileRepo {
		final Lock	lock	= new ReentrantLock();
		boolean		inited;

		CachedFileRepo() {
			super("cache", getFile(buildDir, CACHEDIR), false);
		}

		@Override
		public String toString() {
			return "bnd-cache";
		}

		@Override
		protected boolean init() throws Exception {
			if (lock.tryLock(50, TimeUnit.SECONDS) == false)
				throw new TimeLimitExceededException("Cached File Repo is locked and can't acquire it");
			try {
				if (super.init()) {
					inited = true;
					if (!root.exists() && !root.mkdirs()) {
						throw new IOException("Could not create cache directory " + root);
					}
					if (!root.isDirectory())
						throw new IllegalArgumentException("Cache directory " + root + " not a directory");

					InputStream in = getClass().getResourceAsStream(EMBEDDED_REPO);
					if (in != null)
						unzip(in, root);
					else {
						if (root.isDirectory() && root.list().length >= 2) {
							trace("Assuming I am in a bnd test ...  the embedded repo is missig but it exists on the file system");
							return true;
						}

						error("Couldn't find embedded-repo.jar in bundle ");
					}
					return true;
				} else
					return false;
			}
			finally {
				lock.unlock();
			}
		}

		void unzip(InputStream in, File dir) throws Exception {
			try {
				JarInputStream jin = new JarInputStream(in);
				JarEntry jentry = jin.getNextJarEntry();
				while (jentry != null) {
					if (!jentry.isDirectory()) {
						File dest = Processor.getFile(dir, jentry.getName());
						long modifiedTime = ZipUtil.getModifiedTime(jentry);
						if (!dest.isFile() || dest.lastModified() < modifiedTime || modifiedTime <= 0) {
							File dp = dest.getParentFile();
							if (!dp.exists() && !dp.mkdirs()) {
								throw new IOException("Could not create directory " + dp);
							}
							FileOutputStream out = new FileOutputStream(dest);
							try {
								copy(jin, out);
							}
							finally {
								out.close();
							}
						}
					}
					jentry = jin.getNextJarEntry();
				}
			}
			finally {
				in.close();
			}
		}
	}

	public void syncCache() throws Exception {
		CachedFileRepo cf = new CachedFileRepo();
		cf.init();
		cf.close();
	}

	public List<RepositoryPlugin> getRepositories() {
		return getPlugins(RepositoryPlugin.class);
	}

	public Collection<Project> getBuildOrder() throws Exception {
		List<Project> result = new ArrayList<Project>();
		for (Project project : getAllProjects()) {
			Collection<Project> dependsOn = project.getDependson();
			getBuildOrder(dependsOn, result);
			if (!result.contains(project)) {
				result.add(project);
			}
		}
		return result;
	}

	private void getBuildOrder(Collection<Project> dependsOn, List<Project> result) throws Exception {
		for (Project project : dependsOn) {
			Collection<Project> subProjects = project.getDependson();
			for (Project subProject : subProjects) {
				if (!result.contains(subProject)) {
					result.add(subProject);
				}
			}
			if (!result.contains(project)) {
				result.add(project);
			}
		}
	}

	public static Workspace getWorkspace(String path) throws Exception {
		File file = IO.getFile(new File(""), path);
		return getWorkspace(file);
	}

	public Maven getMaven() {
		return maven;
	}

	@Override
	protected void setTypeSpecificPlugins(Set<Object> list) {
		try {
			super.setTypeSpecificPlugins(list);
			list.add(this);
			list.add(maven);
			list.add(settings);

			if (!isTrue(getProperty(NOBUILDINCACHE))) {
				CachedFileRepo cache = new CachedFileRepo();
				cache.init(); // init early so the contents can be plugins
				list.add(cache);
			}

			resourceRepositoryImpl = new ResourceRepositoryImpl();
			resourceRepositoryImpl.setCache(IO.getFile(getProperty(CACHEDIR, "~/.bnd/caches/shas")));
			resourceRepositoryImpl.setExecutor(getExecutor());
			resourceRepositoryImpl.setIndexFile(getFile(buildDir, "repo.json"));
			resourceRepositoryImpl.setURLConnector(new MultiURLConnectionHandler(this));
			customize(resourceRepositoryImpl, null);
			list.add(resourceRepositoryImpl);

			//
			// Exporters
			//
			list.add(new SubsystemExporter());

		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Add any extensions listed
	 * 
	 * @param list
	 * @param rri
	 */
	@Override
	protected void addExtensions(Set<Object> list) {
		//
		// <bsn>; version=<range>
		//
		Parameters extensions = getMergedParameters(EXTENSION);
		Map<DownloadBlocker,Attrs> blockers = new HashMap<DownloadBlocker,Attrs>();

		for (Entry<String,Attrs> i : extensions.entrySet()) {
			String bsn = removeDuplicateMarker(i.getKey());
			String stringRange = i.getValue().get(VERSION_ATTRIBUTE);

			trace("Adding extension %s-%s", bsn, stringRange);

			if (stringRange == null)
				stringRange = Version.LOWEST.toString();
			else if (!VersionRange.isVersionRange(stringRange)) {
				error("Invalid version range %s on extension %s", stringRange, bsn);
				continue;
			}
			try {
				SortedSet<ResourceDescriptor> matches = resourceRepositoryImpl.find(null, bsn, new VersionRange(
						stringRange));
				if (matches.isEmpty()) {
					error("Extension %s;version=%s not found in base repo", bsn, stringRange);
					continue;
				}

				DownloadBlocker blocker = new DownloadBlocker(this);
				blockers.put(blocker, i.getValue());
				resourceRepositoryImpl.getResource(matches.last().id, blocker);
			}
			catch (Exception e) {
				error("Failed to load extension %s-%s, %s", bsn, stringRange, e);
			}
		}

		trace("Found extensions %s", blockers);

		for (Entry<DownloadBlocker,Attrs> blocker : blockers.entrySet()) {
			try {
				String reason = blocker.getKey().getReason();
				if (reason != null) {
					error("Extension load failed: %s", reason);
					continue;
				}

				@SuppressWarnings("resource")
				URLClassLoader cl = new URLClassLoader(new URL[] {
					blocker.getKey().getFile().toURI().toURL()
				}, getClass().getClassLoader());
				Enumeration<URL> manifests = cl.getResources("META-INF/MANIFEST.MF");
				while (manifests.hasMoreElements()) {
					Manifest m = new Manifest(manifests.nextElement().openStream());
					Parameters activators = new Parameters(m.getMainAttributes().getValue("Extension-Activator"));
					for (Entry<String,Attrs> e : activators.entrySet()) {
						try {
							Class< ? > c = cl.loadClass(e.getKey());
							ExtensionActivator extensionActivator = (ExtensionActivator) c.newInstance();
							customize(extensionActivator, blocker.getValue());
							List< ? > plugins = extensionActivator.activate(this, blocker.getValue());
							list.add(extensionActivator);

							if (plugins != null)
								for (Object plugin : plugins) {
									list.add(plugin);
								}
						}
						catch (ClassNotFoundException cnfe) {
							error("Loading extension %s, extension activator missing: %s (ignored)", blocker,
									e.getKey());
						}
					}
				}
			}
			catch (Exception e) {
				error("failed to install extension %s due to %s", blocker, e);
			}
		}
	}

	/**
	 * Return if we're in offline mode. Offline mode is defined as an
	 * environment where nobody tells us the resources are out of date (refresh
	 * or changed). This is currently defined as having bndlisteners.
	 * 
	 * @return
	 */
	public boolean isOffline() {
		return offline;
	}

	public Workspace setOffline(boolean on) {
		this.offline = on;
		return this;
	}

	/**
	 * Provide access to the global settings of this machine.
	 * 
	 * @throws Exception
	 * @throws UnknownHostException
	 */

	public String _global(String[] args) throws Exception {
		Macro.verifyCommand(args, "${global;<name>[;<default>]}, get a global setting from ~/.bnd/settings.json", null,
				2, 3);

		String key = args[1];
		if (key.equals("key.public"))
			return Hex.toHexString(settings.getPublicKey());
		if (key.equals("key.private"))
			return Hex.toHexString(settings.getPrivateKey());

		String s = settings.get(key);
		if (s != null)
			return s;

		if (args.length == 3)
			return args[2];

		return null;
	}

	public String _user(String[] args) throws Exception {
		return _global(args);
	}

	/**
	 * Return the repository signature digests. These digests are a unique id
	 * for the contents of the repository
	 */

	public Object _repodigests(String[] args) throws Exception {
		Macro.verifyCommand(args, "${repodigests;[;<repo names>]...}, get the repository digests", null, 1, 10000);
		List<RepositoryPlugin> repos = getRepositories();
		if (args.length > 1) {
			repos: for (Iterator<RepositoryPlugin> it = repos.iterator(); it.hasNext();) {
				String name = it.next().getName();
				for (int i = 1; i < args.length; i++) {
					if (name.equals(args[i])) {
						continue repos;
					}
				}
				it.remove();
			}
		}
		List<String> digests = new ArrayList<String>();
		for (RepositoryPlugin repo : repos) {
			try {
				if (repo instanceof RepositoryDigest) {
					byte[] digest = ((RepositoryDigest) repo).getDigest();
					digests.add(Hex.toHexString(digest));
				} else {
					if (args.length != 1)
						error("Specified repo %s for ${repodigests} was named but it is not found", repo.getName());
				}
			}
			catch (Exception e) {
				if (args.length != 1)
					error("Specified repo %s for digests is not found", repo.getName());
				// else Ignore
			}
		}
		return join(digests, ",");
	}

	public static Run getRun(File file) throws Exception {
		if (!file.isFile()) {
			return null;
		}

		File projectDir = file.getParentFile();
		File workspaceDir = projectDir.getParentFile();
		if (!workspaceDir.isDirectory()) {
			return null;
		}

		Workspace ws = getWorkspaceWithoutException(workspaceDir);
		if (ws == null) {
			return null;
		}

		return new Run(ws, projectDir, file);
	}

	/**
	 * Report details of this workspace
	 */

	public void report(Map<String,Object> table) throws Exception {
		super.report(table);
		table.put("Workspace", toString());
		table.put("Plugins", getPlugins(Object.class));
		table.put("Repos", getRepositories());
		table.put("Projects in build order", getBuildOrder());
	}

	public File getCache(String name) {
		return getFile(buildDir, CACHEDIR + "/" + name);
	}

	/**
	 * Return the workspace repo
	 */

	public WorkspaceRepository getWorkspaceRepository() {
		return workspaceRepo;
	}

	public void checkStructure() {
		if (!buildDir.isDirectory())
			error("No directory for cnf %s", buildDir);
		else {
			File build = IO.getFile(buildDir, BUILDFILE);
			if (build.isFile()) {
				error("No %s file in %s", BUILDFILE, buildDir);
			}
		}
	}

	public File getBuildDir() {
		return buildDir;
	}

	public boolean isValid() {
		return IO.getFile(buildDir, BUILDFILE).isFile();
	}

	public RepositoryPlugin getRepository(String repo) {
		for (RepositoryPlugin r : getRepositories()) {
			if (repo.equals(r.getName())) {
				return r;
			}
		}
		return null;
	}

	public void close() {
		cache.remove(getPropertiesFile().getParentFile().getParentFile());
	}

	/**
	 * Get the bnddriver, can be null if not set. The overallDriver is the
	 * environment that runs this bnd.
	 */
	public String getDriver() {
		if (driver == null) {
			driver = getProperty(Constants.BNDDRIVER, null);
			if (driver != null)
				driver = driver.trim();
		}

		if (driver != null)
			return driver;

		return overallDriver;
	}

	/**
	 * Set the driver of this environment
	 */
	public static void setDriver(String driver) {
		overallDriver = driver;
	}

	/**
	 * Macro to return the driver. Without any arguments, we return the name of
	 * the driver. If there are arguments, we check each of the arguments
	 * against the name of the driver. If it matches, we return the driver name.
	 * If none of the args match the driver name we return an empty string
	 * (which is false).
	 */

	public String _driver(String args[]) {
		if (args.length == 1) {
			return getDriver();
		}
		String driver = getDriver();
		if (driver == null)
			driver = getProperty(Constants.BNDDRIVER);

		if (driver != null) {
			for (int i = 1; i < args.length; i++) {
				if (args[i].equalsIgnoreCase(driver))
					return driver;
			}
		}
		return "";
	}

	/**
	 * Add a gestalt to all workspaces. The gestalt is a set of parts describing
	 * the environment. Each part has a name and optionally attributes. This
	 * method adds a gestalt to the VM. Per workspace it is possible to augment
	 * this.
	 */

	public static void addGestalt(String part, Attrs attrs) {
		Attrs already = overallGestalt.get(part);
		if (attrs == null)
			attrs = new Attrs();

		if (already != null) {
			already.putAll(attrs);
		} else
			already = attrs;

		overallGestalt.put(part, already);
	}

	/**
	 * Get the attrs for a gestalt part
	 */
	public Attrs getGestalt(String part) {
		return getGestalt().get(part);
	}

	/**
	 * Get the attrs for a gestalt part
	 */
	public Parameters getGestalt() {
		if (gestalt == null) {
			gestalt = getMergedParameters(Constants.GESTALT);
			gestalt.mergeWith(overallGestalt, false);
		}
		return gestalt;
	}

	/**
	 * The macro to access the gestalt
	 * <p>
	 * {@code $ gestalt;part[;key[;value]]}
	 */

	public String _gestalt(String args[]) {
		if (args.length >= 2) {
			Attrs attrs = getGestalt(args[1]);
			if (attrs == null)
				return "";

			if (args.length == 2)
				return args[1];

			String s = attrs.get(args[2]);
			if (args.length == 3) {
				if (s == null)
					s = "";
				return s;
			}

			if (args.length == 4) {
				if (args[3].equals(s))
					return s;
				else
					return "";
			}
		}
		throw new IllegalArgumentException("${gestalt;<part>[;key[;<value>]]} has too many arguments");
	}

	@Override
	public String toString() {
		return "Workspace [" + getBase().getName() + "]";
	}

	/**
	 * Create a project in this workspace
	 */

	public Project createProject(String name) throws Exception {
		
		if (!Verifier.SYMBOLICNAME.matcher(name).matches()) {
			error("A project name is a Bundle Symbolic Name, this must therefore consist of only letters, digits and dots");
			return null;
		}

		File pdir = getFile(name);
		pdir.mkdirs();

		IO.store("#\n#   " + name.toUpperCase().replace('.', ' ') + "\n#\n", getFile(pdir, Project.BNDFILE));
		Project p = new Project(this, pdir);

		p.getTarget().mkdirs();
		p.getOutput().mkdirs();
		p.getTestOutput().mkdirs();
		for (File dir : p.getSourcePath()) {
			dir.mkdirs();
		}
		p.getTestSrc().mkdirs();

		for (LifeCyclePlugin l : getPlugins(LifeCyclePlugin.class))
			l.created(p);

		if (!p.isValid()) {
			error("project %s is not valid", p);
		}

		return p;
	}

	/**
	 * Create a new Workspace
	 * 
	 * @param opts
	 * @param wsdir
	 * @throws Exception
	 */
	public static Workspace createWorkspace(File wsdir) throws Exception {
		if (wsdir.exists())
			return null;

		wsdir.mkdirs();
		File cnf = IO.getFile(wsdir, CNFDIR);
		cnf.mkdir();
		IO.store("", new File(cnf, BUILDFILE));
		IO.store("-nobundles: true\n", new File(cnf, Project.BNDFILE));
		File ext = new File(cnf, EXT);
		ext.mkdir();
		Workspace ws = getWorkspace(wsdir);

		return ws;
	}

	/**
	 * Add a plugin
	 * 
	 * @param plugin
	 * @throws Exception
	 */

	public boolean addPlugin(Class< ? > plugin, String alias, Map<String,String> parameters, boolean force)
			throws Exception {
		BndPlugin ann = plugin.getAnnotation(BndPlugin.class);

		if (alias == null) {
			if (ann != null)
				alias = ann.name();
			else {
				alias = Strings.getLastSegment(plugin.getName()).toLowerCase();
				if (alias.endsWith("plugin")) {
					alias = alias.substring(0, alias.length() - "plugin".length());
				}
			}
		}

		if ( !Verifier.isBsn(alias)) {
			error("Not a valid plugin name %s", alias);
		}
			
		File ext = getFile(Workspace.CNFDIR + "/" + Workspace.EXT);
		ext.mkdirs();

		File f = new File(ext, alias + ".bnd");

		if (!force) {
			if (f.exists()) {
				error("Plugin %s already exists", alias);
				return false;
			}
		} else {
			IO.delete(f);
		}

		Object l = plugin.newInstance();

		Formatter setup = new Formatter();
		try {
			setup.format("#\n" //
					+ "# Plugin %s setup\n" //
					+ "#\n", alias);
			setup.format("-plugin.%s = %s", alias, plugin.getName());

			for (Map.Entry<String,String> e : parameters.entrySet()) {
				setup.format("; \\\n \t%s = '%s'", e.getKey(), escaped(e.getValue()));
			}
			setup.format("\n\n");
			
			String out = setup.toString();
			if ( l instanceof LifeCyclePlugin ) {
				out = ((LifeCyclePlugin) l).augmentSetup(out, alias, parameters);
				((LifeCyclePlugin) l).init(this);
			}

			trace("setup %s", out);
			IO.store(out, f);
		}
		finally {
			setup.close();
		}

		refresh();
		
		for ( LifeCyclePlugin lp : getPlugins(LifeCyclePlugin.class)) {
			lp.addedPlugin(this, plugin.getName(), alias, parameters);
		}
		return true;
	}

	static Pattern ESCAPE_P = Pattern.compile("(\"|')(.*)\1");
	private Object escaped(String value) {
		Matcher matcher = ESCAPE_P.matcher(value);
		if ( matcher.matches())
			value = matcher.group(2);
		
		return value.replaceAll("'", "\\'");
	}

	public boolean removePlugin(String alias) {
		File ext = getFile(Workspace.CNFDIR + "/" + Workspace.EXT);
		File f = new File(ext, alias + ".bnd");
		if ( !f.exists()) {
			error("No such plugin %s", alias);
			return false;
		}
		
		IO.delete(f);
		
		refresh();	
		return true;
	}


}

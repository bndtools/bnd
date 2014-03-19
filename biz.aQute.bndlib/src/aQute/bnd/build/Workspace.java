package aQute.bnd.build;

import java.io.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.jar.*;

import javax.naming.*;

import aQute.bnd.header.*;
import aQute.bnd.maven.support.*;
import aQute.bnd.osgi.*;
import aQute.bnd.resource.repository.*;
import aQute.bnd.service.*;
import aQute.bnd.service.action.*;
import aQute.bnd.service.extension.*;
import aQute.bnd.service.phases.*;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.url.*;
import aQute.bnd.version.*;
import aQute.lib.deployer.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.settings.*;
import aQute.service.reporter.*;

public class Workspace extends Processor {
	public static final String					BUILDFILE	= "build.bnd";
	public static final String					CNFDIR		= "cnf";
	public static final String					BNDDIR		= "bnd";
	public static final String					CACHEDIR	= "cache";

	static Map<File,WeakReference<Workspace>>	cache		= newHashMap();
	static Processor							defaults	= null;
	final Map<String,Project>					models		= newHashMap();
	final Map<String,Action>					commands	= newMap();
	final File									buildDir;
	final Maven									maven		= new Maven(Processor.getExecutor());
	private boolean								offline		= true;
	Settings									settings	= new Settings();

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

		Properties props = new Properties();
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
		defaults = new Processor(props);

		return defaults;
	}

	public static Workspace getWorkspace(File parent) throws Exception {
		return getWorkspace(parent, BNDDIR);
	}

	public static Workspace getWorkspaceWithoutException(File parent) throws Exception {
		try {
			return getWorkspace(parent);
		}
		catch (IllegalArgumentException e) {
			return null;
		}
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
		this(dir, BNDDIR);
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
		super.propertiesChanged();
		File extDir = new File(this.buildDir, "ext");
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
	}

	public String _workspace(@SuppressWarnings("unused")
	String args[]) {
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
			if (new File(file, Project.BNDFILE).isFile())
				projects.add(getProject(file.getAbsoluteFile().getName()));
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

	/**
	 * Signal a BndListener plugin. We ran an infinite bug loop :-(
	 */
	final ThreadLocal<Reporter>		signalBusy	= new ThreadLocal<Reporter>();
	private ResourceRepositoryImpl	resourceRepositoryImpl;

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
		byte data[] = new byte[10000];
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
						if ( root.isDirectory() && root.list().length >= 2) {
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
						if (!dest.isFile() || dest.lastModified() < jentry.getTime() || jentry.getTime() <= 0) {
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
			list.add(maven);
			list.add(settings);
			if (!isTrue(getProperty(NOBUILDINCACHE))) {
				list.add(new CachedFileRepo());
			}

			resourceRepositoryImpl = new ResourceRepositoryImpl();
			resourceRepositoryImpl.setCache(IO.getFile(getProperty(CACHEDIR, "~/.bnd/caches/shas")));
			resourceRepositoryImpl.setExecutor(getExecutor());
			resourceRepositoryImpl.setIndexFile(getFile(CNFDIR + "/repo.json"));

			resourceRepositoryImpl.setURLConnector(new MultiURLConnectionHandler(this));
			customize(resourceRepositoryImpl, null);
			list.add(resourceRepositoryImpl);
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
		Parameters extensions = new Parameters(getProperty(EXTENSION));
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
				ResourceDescriptor highest = resourceRepositoryImpl.findBestMatch(bsn, new VersionRange(stringRange));
				if (highest == null) {
					error("Extension %s;version=%s not found in base repo", bsn, stringRange);
					continue;
				}

				DownloadBlocker blocker = new DownloadBlocker(this);
				blockers.put(blocker, i.getValue());
				resourceRepositoryImpl.getResource(highest.id, blocker);
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

				URLClassLoader cl = new URLClassLoader(new URL[] {
					blocker.getKey().getFile().toURI().toURL()
				});
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
						it.remove();
						continue repos;
					}
				}
				it.remove();
			}
		}
		List<String> digests = new ArrayList<String>();
		for (RepositoryPlugin repo : repos) {
			try {
				// TODO use RepositoryDigest interface when it is widely
				// implemented
				Method m = repo.getClass().getMethod("getDigest");
				byte[] digest = (byte[]) m.invoke(repo);
				digests.add(Hex.toHexString(digest));
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

	/**
	 * Phases implement a simple build system that can be extended with plugins
	 * and potentially scripts. We have a number of standard phases that can be
	 * called which are then chained together.
	 * 
	 * @param reporter
	 * @param options
	 * @return
	 * @throws Exception 
	 */
	public Phases getPhases(final Processor reporter, EnumSet<PhasesPlugin.Options> options) throws Exception {
		if (options == null)
			options = EnumSet.noneOf(PhasesPlugin.Options.class);
		else
			if ( options.contains(PhasesPlugin.Options.TRACE)) {
				for (Project p : getAllProjects()) {
					p.setTrace(true);
				}
			}
		
		List<PhasesPlugin> pps = getPlugins(PhasesPlugin.class);
		Collections.sort(pps, Collections.reverseOrder(PhasesPlugin.COMPARATOR));
		final List<Phases> phases = new ArrayList<Phases>();

		final Phases master = new Phases() {

			public void begin(Workspace p) {
				throw new UnsupportedOperationException();
			}

			public void end(Workspace p) {
				throw new UnsupportedOperationException();
			}

			public void before(Project p, String phase) {
				throw new UnsupportedOperationException();
			}

			public void after(Project p, String phase, Exception e) {
				throw new UnsupportedOperationException();
			}

			public void compile(Project p, boolean test) throws Exception {
				reporter.progress("compile " + p);
				Phases first = phases.get(0);
				reporter.trace("before compile(test=%s) %s", test, p);
				first.before(p, "compile");
				try {
					first.compile(p, test);
					first.after(p, "compile", null);
					reporter.trace("after compile(test=%s) %s", test, p);
				}
				catch (Exception e) {
					reporter.trace("after compile(test=%s) %s with error %s", test, p, e);
				}
			}

			public void build(Project p, boolean test) throws Exception {
				reporter.progress("build " + p);
				Phases first = phases.get(0);
				reporter.trace("before build(test=%s) %s", test, p);
				first.before(p, "build");
				try {
					first.build(p, test);
					first.after(p, "build", null);
					reporter.trace("after build(test=%s) %s", test, p);
				}
				catch (Exception e) {
					reporter.trace("after build(test=%s) %s with error %s", test, p, e);
				}
			}

			public void test(Project p) throws Exception {
				reporter.progress("test " + p);
				Phases first = phases.get(0);
				reporter.trace("before test %s", p);
				first.before(p, "test");
				try {
					first.test(p);
					first.after(p, "test", null);
					reporter.trace("after test %s", p);
				}
				catch (Exception e) {
					reporter.trace("after test %s with error %s", p, e);
				}
			}

			public void junit(Project p) throws Exception {
				reporter.progress("junit " + p);
				Phases first = phases.get(0);
				reporter.trace("before junit %s", p);
				first.before(p, "junit");
				try {
					first.junit(p);
					first.after(p, "junit", null);
					reporter.trace("after junit %s", p);
				}
				catch (Exception e) {
					reporter.trace("after junit %s with error %s", p, e);
				}
			}

			public void release(Project p) throws Exception {
				reporter.progress("release " + p);
				Phases first = phases.get(0);
				reporter.trace("before release %s", p);
				first.before(p, "release");
				try {
					first.release(p);
					first.after(p, "release", null);
					reporter.trace("after release %s", p);
				}
				catch (Exception e) {
					reporter.trace("after release %s with error %s", p, e);
				}
			}

			public void valid(Project p) throws Exception {
				reporter.progress("valid " + p);
				Phases first = phases.get(0);
				reporter.trace("before valid %s", p);
				first.before(p, "valid");
				try {
					first.valid(p);
					first.after(p, "valid", null);
					reporter.trace("after valid %s", p);
				}
				catch (Exception e) {
					reporter.trace("after valid %s with error %s", p, e);
				}
			}

			public void action(Project p, String action) throws Exception {
				reporter.progress("action " + p);

				Phases first = phases.get(0);
				reporter.trace("before action %s", p);
				first.before(p, "action");
				try {
					first.action(p, action);
					first.after(p, "action", null);
					reporter.trace("after action %s", p);
				}
				catch (Exception e) {
					reporter.trace("after action %s with error %s", p, e);
				}
			}

			public void pack(Run r) throws Exception {
				reporter.progress("pack " + r);

				Phases first = phases.get(0);
				reporter.trace("before pack %s", r);
				first.before(r, "pack");
				try {
					first.pack(r);
					first.after(r, "pack", null);
					reporter.trace("after pack %s", r);
				}
				catch (Exception e) {
					reporter.trace("after pack %s with error %s", r, e);
				}				
			}
		};

		Phases prev = new DefaultPhases(reporter, master); // last one
		phases.add(prev);

		// We do this in reverse order
		for (PhasesPlugin pp : pps) {
			Phases tmp = pp.getPhases(reporter, master, prev, options);
			phases.add(0, tmp);
			prev = tmp;
		}
		for (Project p : getAllProjects()) {
			reporter.getInfo(p);
		}
		reporter.getInfo(this);
		return master;
	}
}

package aQute.bnd.build;

import static aQute.lib.exceptions.BiFunctionWithException.asBiFunction;
import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.invoke.MethodType.methodType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodType;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.exporter.executable.ExecutableJarExporter;
import aQute.bnd.exporter.runbundles.RunbundlesExporter;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.http.HttpClient;
import aQute.bnd.maven.support.Maven;
import aQute.bnd.osgi.About;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Macro;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.remoteworkspace.server.RemoteWorkspaceServer;
import aQute.bnd.resource.repository.ResourceRepositoryImpl;
import aQute.bnd.service.BndListener;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.action.Action;
import aQute.bnd.service.extension.ExtensionActivator;
import aQute.bnd.service.lifecycle.LifeCyclePlugin;
import aQute.bnd.service.repository.Prepare;
import aQute.bnd.service.repository.RepositoryDigest;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.stream.MapStream;
import aQute.bnd.url.MultiURLConnectionHandler;
import aQute.bnd.util.home.Home;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.collections.Iterables;
import aQute.lib.deployer.FileRepo;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.io.IOConstants;
import aQute.lib.io.NonClosingInputStream;
import aQute.lib.settings.Settings;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.glob.Glob;
import aQute.libg.uri.URIUtil;
import aQute.service.reporter.Reporter;

public class Workspace extends Processor {
	private final static Logger	logger							= LoggerFactory.getLogger(Workspace.class);
	public static final File	BND_DEFAULT_WS					= IO.getFile(Home.getUserHomeBnd() + "/default-ws");
	public static final String	BND_CACHE_REPONAME				= "bnd-cache";
	public static final String	EXT								= "ext";
	public static final String	BUILDFILE						= "build.bnd";
	public static final String	CNFDIR							= "cnf";
	public static final String	BNDDIR							= "bnd";
	public static final String	CACHEDIR						= "cache/" + About.CURRENT;
	public static final String	STANDALONE_REPO_CLASS			= "aQute.bnd.repository.osgi.OSGiRepository";

	static final int			BUFFER_SIZE						= IOConstants.PAGE_SIZE * 16;
	private static final String	PLUGIN_STANDALONE		= Constants.PLUGIN + ".standalone_";

	static class WorkspaceData {
		List<RepositoryPlugin>	repositories;
		RemoteWorkspaceServer	remoteServer;
	}

	private final static Map<File, WeakReference<Workspace>>	cache				= newHashMap();
	static Processor											defaults			= null;
	final Map<String, Action>									commands			= newMap();
	final Maven													maven;
	private final AtomicBoolean									offline				= new AtomicBoolean();
	Settings													settings			= new Settings(
		Home.getUserHomeBnd() + "/settings.json");
	WorkspaceRepository											workspaceRepo		= new WorkspaceRepository(this);
	static String												overallDriver		= "unset";
	static Parameters											overallGestalt		= new Parameters();
	/**
	 * Signal a BndListener plugin. We ran an infinite bug loop :-(
	 */
	final ThreadLocal<Reporter>									signalBusy			= new ThreadLocal<>();
	ResourceRepositoryImpl										resourceRepositoryImpl;
	private Parameters											gestalt;
	private String												driver;
	private final WorkspaceLayout								layout;
	final Set<Project>											trail				= Collections
		.newSetFromMap(new ConcurrentHashMap<Project, Boolean>());
	private WorkspaceData										data				= new WorkspaceData();
	private File												buildDir;
	private final ProjectTracker								projects;
	private final ReadWriteLock									lock				= new ReentrantReadWriteLock(true);

	public static boolean										remoteWorkspaces	= false;

	/**
	 * This static method finds the workspace and creates a project (or returns
	 * an existing project)
	 *
	 * @param projectDir
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
		try (InputStream propStream = Workspace.class.getResourceAsStream("defaults.bnd")) {
			if (propStream != null) {
				props.load(propStream);
			} else {
				System.err.println("Cannot load defaults");
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("Unable to load bnd defaults.", e);
		}
		defaults = new Processor(props, false);

		return defaults;
	}

	public static Workspace createDefaultWorkspace() throws Exception {
		File build = IO.getFile(BND_DEFAULT_WS, CNFDIR + "/" + BUILDFILE);
		if (!build.exists()) {
			build.getParentFile()
				.mkdirs();
			IO.store("", build);
		}
		Workspace ws = new Workspace(BND_DEFAULT_WS, CNFDIR);
		return ws;
	}

	public static Workspace getWorkspace(File workspaceDir) throws Exception {
		return getWorkspace(workspaceDir, CNFDIR);
	}

	public static Workspace getWorkspaceWithoutException(File workspaceDir) throws Exception {
		try {
			return getWorkspace(workspaceDir);
		} catch (IllegalArgumentException e) {
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

	public static Workspace getWorkspace(File workspaceDir, String bndDir) throws Exception {
		workspaceDir = workspaceDir.getAbsoluteFile();

		// the cnf directory can actually be a
		// file that redirects
		while (workspaceDir.isDirectory()) {
			File test = new File(workspaceDir, CNFDIR);

			if (!test.exists())
				test = new File(workspaceDir, bndDir);

			if (test.isDirectory())
				break;

			if (test.isFile()) {
				String redirect = IO.collect(test)
					.trim();
				test = getFile(test.getParentFile(), redirect).getAbsoluteFile();
				workspaceDir = test;
			}
			if (!test.exists())
				throw new IllegalArgumentException("No Workspace found from: " + workspaceDir);
		}

		synchronized (cache) {
			WeakReference<Workspace> wsr = cache.get(workspaceDir);
			Workspace ws;
			if (wsr == null || (ws = wsr.get()) == null) {
				ws = new Workspace(workspaceDir, bndDir);
				cache.put(workspaceDir, new WeakReference<>(ws));
			}
			return ws;
		}
	}

	public Workspace(File workspaceDir) throws Exception {
		this(workspaceDir, CNFDIR);
	}

	public Workspace(File workspaceDir, String bndDir) throws Exception {
		super(getDefaults());
		this.maven = new Maven(Processor.getExecutor(), this);
		this.layout = WorkspaceLayout.BND;
		workspaceDir = workspaceDir.getAbsoluteFile();
		setBase(workspaceDir); // setBase before call to setFileSystem
		addBasicPlugin(new LoggingProgressPlugin());
		setFileSystem(workspaceDir, bndDir);
		projects = new ProjectTracker(this);
	}

	private Workspace(WorkspaceLayout layout) throws Exception {
		super(getDefaults());
		this.maven = new Maven(Processor.getExecutor(), this);
		this.layout = layout;
		setBuildDir(IO.getFile(BND_DEFAULT_WS, CNFDIR));
		projects = new ProjectTracker(this);
	}

	public void setFileSystem(File workspaceDir, String bndDir) throws Exception {
		workspaceDir = workspaceDir.getAbsoluteFile();
		IO.mkdirs(workspaceDir);
		assert workspaceDir.isDirectory();

		synchronized (cache) {
			WeakReference<Workspace> wsr = cache.get(getBase());
			if ((wsr != null) && (wsr.get() == this)) {
				cache.remove(getBase());
				cache.put(workspaceDir, wsr);
			}
		}

		File buildDir = new File(workspaceDir, bndDir).getAbsoluteFile();
		if (!buildDir.isDirectory())
			buildDir = new File(workspaceDir, CNFDIR).getAbsoluteFile();

		setBuildDir(buildDir);

		File buildFile = new File(buildDir, BUILDFILE).getAbsoluteFile();
		if (!buildFile.isFile()) {
			warning("No Build File in %s, creating it", workspaceDir);
			IO.store("", buildFile);
		}
		setProperties(buildFile, workspaceDir);
		propertiesChanged();

		//
		// There is a nasty bug/feature in Java that gives errors on our
		// SSL use of github. The flag jsse.enableSNIExtension should be set
		// to false. So here we provide a way to set system properties
		// as early as possible
		//

		Attrs sysProps = OSGiHeader.parseProperties(mergeProperties(SYSTEMPROPERTIES));
		sysProps.stream()
			.forEachOrdered(System::setProperty);
	}

	public Project getProjectFromFile(File projectDir) {
		projectDir = projectDir.getAbsoluteFile();
		assert projectDir.isDirectory();

		if (getBase().equals(projectDir.getParentFile())) {
			return getProject(projectDir.getName());
		}
		return null;
	}

	public Project getProject(String bsn) {
		return projects.getProject(bsn)
			.orElse(null);
	}

	public boolean isPresent(String name) {
		return projects.getProject(name)
			.isPresent();
	}

	public Collection<Project> getCurrentProjects() {
		return projects.getAllProjects();
	}

	@Override
	public boolean refresh() {
		data = new WorkspaceData();

		gestalt = null;
		if (super.refresh()) {

			for (Project project : getCurrentProjects()) {
				project.propertiesChanged();
			}
			return true;
		}
		return false;
	}

	/**
	 * Signal that the driver has detected a dynamic change in the workspace
	 * directory, for example a project was added or removed in the IDE. Since
	 * this does not affect the inherited properties we can only change the list
	 * of projects.
	 */
	public void refreshProjects() {
		projects.refresh();
	}

	@Override
	public void propertiesChanged() {

		if (data.remoteServer != null)
			IO.close(data.remoteServer);

		data = new WorkspaceData();
		File extDir = new File(getBuildDir(), EXT);
		File[] extensions = extDir.listFiles();
		if (extensions != null) {
			for (File extension : extensions) {
				String extensionName = extension.getName();
				if (extensionName.endsWith(".bnd")) {
					extensionName = extensionName.substring(0, extensionName.length() - ".bnd".length());
					try {
						doIncludeFile(extension, false, getProperties(), "ext." + extensionName);
					} catch (Exception e) {
						exception(e, "PropertiesChanged: %s", e);
					}
				}
			}
		}

		if (remoteWorkspaces || Processor.isTrue(getProperty(Constants.REMOTEWORKSPACE))) {
			try {
				data.remoteServer = new RemoteWorkspaceServer(this);
			} catch (IOException e) {
				exception(e, "Could not create remote workspace %s", getBase());
			}
		}
		super.propertiesChanged();
	}

	public String _workspace(@SuppressWarnings("unused") String args[]) {
		return IO.absolutePath(getBase());
	}

	public void addCommand(String menu, Action action) {
		commands.put(menu, action);
	}

	public void removeCommand(String menu) {
		commands.remove(menu);
	}

	public void fillActions(Map<String, Action> all) {
		all.putAll(commands);
	}

	public Collection<Project> getAllProjects() {
		return projects.getAllProjects();
	}

	/**
	 * Inform any listeners that we changed a file (created/deleted/changed).
	 *
	 * @param f The changed file
	 */
	public void changedFile(File f) {
		List<BndListener> listeners = getPlugins(BndListener.class);
		for (BndListener l : listeners)
			try {
				l.changed(f);
			} catch (Exception e) {
				logger.debug("Exception in a BndListener changedFile method call", e);
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
			} catch (Exception e) {
				if (begin)
					logger.debug("Exception in a BndListener begin method call", e);
				else
					logger.debug("Exception in a BndListener end method call", e);
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
				} catch (Exception e) {
					logger.debug("Exception in a BndListener signal method call", e);
				}
		} catch (Exception e) {
			// Ignore
		} finally {
			signalBusy.set(null);
		}
	}

	@Override
	public void signal() {
		signal(this);
	}

	class CachedFileRepo extends FileRepo {
		final Lock	lock	= new ReentrantLock();
		boolean		inited;

		CachedFileRepo() {
			super(BND_CACHE_REPONAME, getCache(BND_CACHE_REPONAME), false);
		}

		@Override
		protected boolean init() throws Exception {
			if (lock.tryLock(50, TimeUnit.SECONDS)) {
				try {
					if (super.init()) {
						inited = true;
						IO.mkdirs(root);
						if (!root.isDirectory())
							throw new IllegalArgumentException("Cache directory " + root + " not a directory");

						URL url = getClass().getResource(EMBEDDED_REPO);
						if (url != null) {
							try (Resource resource = Resource.fromURL(url);
								InputStream in = resource.openInputStream()) {
								if (in != null) {
									unzip(in, root.toPath());
									return true;
								}
							}
						}
						error("Could not find " + EMBEDDED_REPO + " as a resource on the classpath");
					}
					return false;
				} finally {
					lock.unlock();
				}
			} else {
				throw new TimeoutException("Cannot acquire Cached File Repo lock");
			}
		}

		private void unzip(InputStream in, Path dir) throws Exception {
			try (JarInputStream jin = new JarInputStream(in)) {
				FileTime modifiedTime = FileTime.fromMillis(System.currentTimeMillis());
				Manifest manifest = jin.getManifest();
				if (manifest != null) {
					String timestamp = manifest.getMainAttributes()
						.getValue("Timestamp");
					if (timestamp != null) {
						// truncate to seconds since file system can discard
						// milliseconds
						long seconds = TimeUnit.MILLISECONDS.toSeconds(Long.parseLong(timestamp));
						modifiedTime = FileTime.from(seconds, TimeUnit.SECONDS);
					}
				}
				for (JarEntry jentry = jin.getNextJarEntry(); jentry != null; jentry = jin.getNextJarEntry()) {
					if (jentry.isDirectory()) {
						continue;
					}
					String jentryName = jentry.getName();
					if (jentryName.startsWith("META-INF/")) {
						continue;
					}
					Path dest = IO.getBasedPath(dir, jentryName);
					if (!Files.isRegularFile(dest) || Files.getLastModifiedTime(dest)
						.compareTo(modifiedTime) < 0) {
						IO.mkdirs(dest.getParent());
						IO.copy(new NonClosingInputStream(jin), dest);
						Files.setLastModifiedTime(dest, modifiedTime);
					}
				}
			}
		}
	}

	public void syncCache() throws Exception {
		CachedFileRepo cf = new CachedFileRepo();
		cf.init();
		cf.close();
	}

	public List<RepositoryPlugin> getRepositories() throws Exception {
		if (data.repositories == null) {
			data.repositories = getPlugins(RepositoryPlugin.class);
			for (RepositoryPlugin repo : data.repositories) {
				if (repo instanceof Prepare) {
					((Prepare) repo).prepare();
				}
			}
		}
		return data.repositories;
	}

	public Collection<Project> getBuildOrder() throws Exception {
		Set<Project> result = new LinkedHashSet<>();
		for (Project project : projects.getAllProjects()) {
			Collection<Project> dependsOn = project.getDependson();
			getBuildOrder(dependsOn, result);
			result.add(project);
		}
		return result;
	}

	private void getBuildOrder(Collection<Project> dependsOn, Set<Project> result) throws Exception {
		for (Project project : dependsOn) {
			Collection<Project> subProjects = project.getDependson();
			for (Project subProject : subProjects) {
				result.add(subProject);
			}
			result.add(project);
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
				CachedFileRepo repo = new CachedFileRepo();
				list.add(repo);
			}

			resourceRepositoryImpl = new ResourceRepositoryImpl();
			resourceRepositoryImpl.setCache(IO.getFile(getProperty(CACHEDIR, Home.getUserHomeBnd() + "/caches/shas")));
			resourceRepositoryImpl.setExecutor(getExecutor());
			resourceRepositoryImpl.setIndexFile(getFile(getBuildDir(), "repo.json"));
			resourceRepositoryImpl.setURLConnector(new MultiURLConnectionHandler(this));
			customize(resourceRepositoryImpl, null);
			list.add(resourceRepositoryImpl);

			//
			// Exporters
			//

			list.add(new ExecutableJarExporter());
			list.add(new RunbundlesExporter());

			HttpClient client = new HttpClient();
			try {
				client.setOffline(getOffline());
				client.setRegistry(this);
				client.readSettings(this);

			} catch (Exception e) {
				exception(e, "Failed to load the communication settings");
			}
			list.add(client);

		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Add any extensions listed
	 *
	 * @param list
	 */
	@Override
	protected void addExtensions(Set<Object> list) {
		//
		// <bsn>; version=<range>
		//
		Parameters extensions = getMergedParameters(EXTENSION);
		Map<DownloadBlocker, Attrs> blockers = new HashMap<>();

		for (Entry<String, Attrs> i : extensions.entrySet()) {
			String bsn = removeDuplicateMarker(i.getKey());
			String stringRange = i.getValue()
				.get(VERSION_ATTRIBUTE);

			logger.debug("Adding extension {}-{}", bsn, stringRange);

			if (stringRange == null)
				stringRange = Version.LOWEST.toString();
			else if (!VersionRange.isVersionRange(stringRange)) {
				error("Invalid version range %s on extension %s", stringRange, bsn);
				continue;
			}
			try {
				SortedSet<ResourceDescriptor> matches = resourceRepositoryImpl.find(null, bsn,
					new VersionRange(stringRange));
				if (matches.isEmpty()) {
					error("Extension %s;version=%s not found in base repo", bsn, stringRange);
					continue;
				}

				DownloadBlocker blocker = new DownloadBlocker(this);
				blockers.put(blocker, i.getValue());
				resourceRepositoryImpl.getResource(matches.last().id, blocker);
			} catch (Exception e) {
				error("Failed to load extension %s-%s, %s", bsn, stringRange, e);
			}
		}

		logger.debug("Found extensions {}", blockers);

		for (Entry<DownloadBlocker, Attrs> blocker : blockers.entrySet()) {
			try {
				String reason = blocker.getKey()
					.getReason();
				if (reason != null) {
					error("Extension load failed: %s", reason);
					continue;
				}

				@SuppressWarnings("resource")
				URLClassLoader cl = new URLClassLoader(new URL[] {
					blocker.getKey()
						.getFile()
						.toURI()
						.toURL()
				}, getClass().getClassLoader());
				for (URL manifest : Iterables.iterable(cl.getResources("META-INF/MANIFEST.MF"))) {
					try (InputStream is = manifest.openStream()) {
						Manifest m = new Manifest(is);
						Parameters activators = new Parameters(m.getMainAttributes()
							.getValue("Extension-Activator"), this);
						for (Entry<String, Attrs> e : activators.entrySet()) {
							try {
								Class<?> c = cl.loadClass(e.getKey());
								ExtensionActivator extensionActivator = (ExtensionActivator) newInstance(c);
								customize(extensionActivator, blocker.getValue());
								List<?> plugins = extensionActivator.activate(this, blocker.getValue());
								list.add(extensionActivator);

								if (plugins != null)
									for (Object plugin : plugins) {
										list.add(plugin);
									}
							} catch (ClassNotFoundException cnfe) {
								error("Loading extension %s, extension activator missing: %s (ignored)", blocker,
									e.getKey());
							}
						}
					}
				}
			} catch (Exception e) {
				error("failed to install extension %s due to %s", blocker, e);
			}
		}
	}

	public boolean isOffline() {
		return offline.get();
	}

	public AtomicBoolean getOffline() {
		return offline;
	}

	public Workspace setOffline(boolean on) {
		offline.set(on);
		return this;
	}

	static final String _globalHelp = "${global;<name>[;<default>]}, get a global setting from ~/.bnd/settings.json";

	/**
	 * Provide access to the global settings of this machine.
	 *
	 * @throws Exception
	 */

	public String _global(String[] args) throws Exception {
		Macro.verifyCommand(args, _globalHelp, null, 2, 3);

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

	static final String _repodigestsHelp = "${repodigests;[;<repo names>]...}, get the repository digests";

	/**
	 * Return the repository signature digests. These digests are a unique id
	 * for the contents of the repository
	 */

	public Object _repodigests(String[] args) throws Exception {
		Macro.verifyCommand(args, _repodigestsHelp, null, 1, 10000);
		List<RepositoryPlugin> repos = getRepositories();
		if (args.length > 1) {
			repos: for (Iterator<RepositoryPlugin> it = repos.iterator(); it.hasNext();) {
				String name = it.next()
					.getName();
				for (int i = 1; i < args.length; i++) {
					if (name.equals(args[i])) {
						continue repos;
					}
				}
				it.remove();
			}
		}
		List<String> digests = new ArrayList<>();
		for (RepositoryPlugin repo : repos) {
			try {
				if (repo instanceof RepositoryDigest) {
					byte[] digest = ((RepositoryDigest) repo).getDigest();
					digests.add(Hex.toHexString(digest));
				} else {
					if (args.length != 1)
						error("Specified repo %s for ${repodigests} was named but it is not found", repo.getName());
				}
			} catch (Exception e) {
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

	@Override
	public void report(Map<String, Object> table) throws Exception {
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
		if (!getBuildDir().isDirectory())
			error("No directory for cnf %s", getBuildDir());
		else {
			File build = IO.getFile(getBuildDir(), BUILDFILE);
			if (build.isFile()) {
				error("No %s file in %s", BUILDFILE, getBuildDir());
			}
		}
	}

	public File getBuildDir() {
		return buildDir;
	}

	public void setBuildDir(File buildDir) {
		this.buildDir = buildDir;
	}

	public boolean isValid() {
		return IO.getFile(getBuildDir(), BUILDFILE)
			.isFile();
	}

	public RepositoryPlugin getRepository(String repo) throws Exception {
		for (RepositoryPlugin r : getRepositories()) {
			if (repo.equals(r.getName())) {
				return r;
			}
		}
		return null;
	}

	@Override
	public void close() {
		if (data.remoteServer != null) {
			IO.close(data.remoteServer);
		}
		synchronized (cache) {
			WeakReference<Workspace> wsr = cache.get(getBase());
			if ((wsr != null) && (wsr.get() == this)) {
				cache.remove(getBase());
			}
		}

		projects.close();

		try {
			super.close();
		} catch (IOException e) {
			/* For backwards compatibility, we ignore the exception */
		}
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
	 * Get the layout style of the workspace.
	 */
	public WorkspaceLayout getLayout() {
		return layout;
	}

	static final String _gestaltHelp = "${gestalt;<part>[;key[;<value>]]} has too many arguments";

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
		throw new IllegalArgumentException(_gestaltHelp);
	}

	@Override
	public String toString() {
		return "Workspace [" + getBase().getName() + "]";
	}

	/**
	 * Add a plugin
	 *
	 * @param plugin
	 * @throws Exception
	 */

	public boolean addPlugin(Class<?> plugin, String alias, Map<String, String> parameters, boolean force)
		throws Exception {
		BndPlugin ann = plugin.getAnnotation(BndPlugin.class);

		if (alias == null) {
			if (ann != null)
				alias = ann.name();
			else {
				alias = Strings.getLastSegment(plugin.getName())
					.toLowerCase();
				if (alias.endsWith("plugin")) {
					alias = alias.substring(0, alias.length() - "plugin".length());
				}
			}
		}

		if (!Verifier.isBsn(alias)) {
			error("Not a valid plugin name %s", alias);
		}

		File ext = getFile(Workspace.CNFDIR + "/" + Workspace.EXT);
		IO.mkdirs(ext);

		File f = new File(ext, alias + ".bnd");

		if (!force) {
			if (f.exists()) {
				error("Plugin %s already exists", alias);
				return false;
			}
		} else {
			IO.delete(f);
		}

		Object l = newInstance(plugin);

		try (Formatter setup = new Formatter()) {
			setup.format("#\n" //
				+ "# Plugin %s setup\n" //
				+ "#\n", alias);
			setup.format(Constants.PLUGIN + ".%s = %s", alias, plugin.getName());

			MapStream.of(parameters)
				.mapValue(this::escaped)
				.forEachOrdered((k, v) -> setup.format("; \\\n \t%s = '%s'", k, v));
			setup.format("\n\n");

			String out = setup.toString();
			if (l instanceof LifeCyclePlugin) {
				out = ((LifeCyclePlugin) l).augmentSetup(out, alias, parameters);
				((LifeCyclePlugin) l).init(this);
			}

			logger.debug("setup {}", out);
			IO.store(out, f);
		}

		refresh();

		for (LifeCyclePlugin lp : getPlugins(LifeCyclePlugin.class)) {
			lp.addedPlugin(this, plugin.getName(), alias, parameters);
		}
		return true;
	}

	private static final MethodType defaultConstructor = methodType(void.class);

	private static <T> T newInstance(Class<T> rawClass) throws Exception {
		try {
			return (T) publicLookup().findConstructor(rawClass, defaultConstructor)
				.invoke();
		} catch (Error | Exception e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private final static Pattern ESCAPE_P = Pattern.compile("([\"'])(.*)\\1");

	private Object escaped(String value) {
		Matcher matcher = ESCAPE_P.matcher(value);
		if (matcher.matches())
			value = matcher.group(2);

		return value.replaceAll("'", "\\'");
	}

	public boolean removePlugin(String alias) {
		File ext = getFile(Workspace.CNFDIR + "/" + Workspace.EXT);
		File f = new File(ext, alias + ".bnd");
		if (!f.exists()) {
			error("No such plugin %s", alias);
			return false;
		}

		IO.delete(f);

		refresh();
		return true;
	}

	/**
	 * Create a workspace that does not inherit from a cnf directory etc.
	 *
	 * @param run
	 */
	public static Workspace createStandaloneWorkspace(Processor run, URI base) throws Exception {
		Workspace ws = new Workspace(WorkspaceLayout.STANDALONE);

		AtomicBoolean copyAll = new AtomicBoolean(false);
		AtomicInteger counter = new AtomicInteger();
		Parameters standalone = new Parameters(run.getProperty(STANDALONE), ws);
		standalone.stream()
			.filterKey(locationStr -> {
				if ("true".equalsIgnoreCase(locationStr)) {
					copyAll.set(true);
					return false;
				}
				return true;
			})
			.map(asBiFunction((locationStr, attrs) -> {
				String index = String.format("%02d", counter.incrementAndGet());
				String name = attrs.get("name");
				if (name == null) {
					name = "repo".concat(index);
				}
				URI resolvedLocation = URIUtil.resolve(base, locationStr);
				try (Formatter f = new Formatter(Locale.US)) {
					f.format(STANDALONE_REPO_CLASS + "; name='%s'; locations='%s'", name, resolvedLocation);
					attrs.stream()
						.filterKey(k -> !k.equals("name"))
						.forEachOrdered((k, v) -> f.format("; %s='%s'", k, v));
					return MapStream.entry(PLUGIN_STANDALONE.concat(index), f.toString());
				}
			}))
			.forEachOrdered(ws::setProperty);

		MapStream<String, Object> runProperties = MapStream.of(run.getProperties())
			.mapKey(String.class::cast);
		if (!copyAll.get()) {
			runProperties = runProperties.filterKey(k -> k.equals(Constants.PLUGIN) || k.startsWith(Constants.PLUGIN + "."));
		}
		Properties wsProperties = ws.getProperties();
		runProperties.filterKey(k -> !k.startsWith(PLUGIN_STANDALONE))
			.forEachOrdered(wsProperties::put);

		return ws;
	}

	public boolean isDefaultWorkspace() {
		return BND_DEFAULT_WS.equals(getBase());
	}

	@Override
	public boolean isInteractive() {
		return getGestalt(Constants.GESTALT_INTERACTIVE) != null;
	}

	public static void resetStatic() {
		overallDriver = "unset";
		overallGestalt = new Parameters();
	}

	/**
	 * Create a project in this workspace
	 */

	public Project createProject(String name) throws Exception {

		if (!Verifier.SYMBOLICNAME.matcher(name)
			.matches()) {
			error(
				"A project name is a Bundle Symbolic Name, this must therefore consist of only letters, digits and dots");
			return null;
		}

		File pdir = getFile(name);
		IO.mkdirs(pdir);

		IO.store("#\n#   " + name.toUpperCase()
			.replace('.', ' ') + "\n#\n", getFile(pdir, Project.BNDFILE));

		projects.refresh();
		Project p = getProject(name);

		IO.mkdirs(p.getTarget());
		IO.mkdirs(p.getOutput());
		IO.mkdirs(p.getTestOutput());
		for (File dir : p.getSourcePath()) {
			IO.mkdirs(dir);
		}
		IO.mkdirs(p.getTestSrc());

		for (LifeCyclePlugin l : getPlugins(LifeCyclePlugin.class)) {
			l.created(p);
		}

		if (!p.isValid()) {
			error("project %s is not valid", p);
		}

		return p;
	}

	void removeProject(Project p) throws Exception {
		if (p.isCnf())
			return;

		for (LifeCyclePlugin lp : getPlugins(LifeCyclePlugin.class)) {
			lp.delete(p);
		}
		projects.refresh();
	}

	/**
	 * Create a new Workspace
	 *
	 * @param wsdir
	 * @throws Exception
	 */
	public static Workspace createWorkspace(File wsdir) throws Exception {
		if (wsdir.exists())
			return null;

		IO.mkdirs(wsdir);
		File cnf = IO.getFile(wsdir, CNFDIR);
		IO.mkdirs(cnf);
		IO.store("", new File(cnf, BUILDFILE));
		IO.store("-nobundles: true\n", new File(cnf, Project.BNDFILE));
		File ext = new File(cnf, EXT);
		IO.mkdirs(ext);
		Workspace ws = getWorkspace(wsdir);

		return ws;
	}

	/**
	 * Lock the workspace and its corresponding projects for reading. The r
	 * parameter when called can freely use any read function in the workspace.
	 *
	 * @param r the lambda to run
	 * @param timeoutInMs the timeout in milliseconds
	 * @return the value of the lambda
	 */
	public <T> T readLocked(Callable<T> r, long timeoutInMs) throws Exception {
		return locked(r, timeoutInMs, lock.readLock());
	}

	public <T> T readLocked(Callable<T> r) throws Exception {
		return readLocked(r, 120000);
	}

	/**
	 * Lock the workspace and its corresponding projects for all functions. The
	 * r parameter when called can freely use any function in the workspace.
	 *
	 * @param r the lambda to run
	 * @param timeoutInMs the timeout in milliseconds
	 * @return the value of the lambda
	 */
	public <T> T writeLocked(Callable<T> r, long timeoutInMs) throws Exception {
		return locked(r, timeoutInMs, lock.writeLock());
	}

	public <T> T writeLocked(Callable<T> r) throws Exception {
		return writeLocked(r, 120000);
	}

	<T> T locked(Callable<T> r, long timeoutInMs, Lock readLock) throws Exception {
		boolean locked = readLock.tryLock(timeoutInMs, TimeUnit.MILLISECONDS);
		if (!locked)
			throw new TimeoutException();

		try {
			return r.call();
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Provide a macro that lists all currently loaded project names that match
	 * a macro. This macro checks for cycles since I am not sure if calling
	 * getAllProjects is safe for some macros in all cases. I.e. the primary use
	 * case wants to use it in -dependson
	 *
	 * <pre>
	 *      ${projectswhere;key;glob}
	 * </pre>
	 */

	static final String							_projectswhereHelp		= "${projectswhere[;<key>;<glob>]} - Make sure this cannot be called recursively at startup";
	private static final ThreadLocal<Boolean>	projectswhereCycleCheck	= new ThreadLocal<>();

	public String _projectswhere(String args[]) {
		if (projectswhereCycleCheck.get() != null) {
			throw new IllegalStateException("The ${projectswhere} macro is called recursively");
		}
		projectswhereCycleCheck.set(Boolean.TRUE);
		try {

			Macro.verifyCommand(args, _projectswhereHelp, null, 1, 3);

			Collection<Project> allProjects = getAllProjects();
			if (args.length == 1) {
				return Strings.join(allProjects);
			}

			String key = args[1];
			boolean negate;
			Glob g;
			if (args.length > 2) {
				if (args[2].startsWith("!")) {
					g = new Glob(args[2].substring(1));
					negate = true;
				} else {
					g = new Glob(args[2]);
					negate = false;
				}
			} else {
				g = null;
				negate = false;
			}

			return allProjects.stream()
				.filter(p -> {
					String value = p.getProperty(key);
					if (value == null)
						return negate;

					if (g == null)
						return !negate;

					return negate ^ g.matcher(value)
						.matches();
				})
				.map(Project::getName)
				.collect(Strings.joining());
		} finally {
			projectswhereCycleCheck.set(null);
		}
	}
}

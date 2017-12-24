package aQute.bnd.build;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.naming.TimeLimitExceededException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.connection.settings.ConnectionSettings;
import aQute.bnd.exporter.subsystem.SubsystemExporter;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.http.HttpClient;
import aQute.bnd.maven.support.Maven;
import aQute.bnd.osgi.About;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Macro;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.resource.repository.ResourceRepositoryImpl;
import aQute.bnd.service.BndListener;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.action.Action;
import aQute.bnd.service.extension.ExtensionActivator;
import aQute.bnd.service.lifecycle.LifeCyclePlugin;
import aQute.bnd.service.repository.Prepare;
import aQute.bnd.service.repository.RepositoryDigest;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.url.MultiURLConnectionHandler;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.deployer.FileRepo;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.io.IOConstants;
import aQute.lib.settings.Settings;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.lib.zip.ZipUtil;
import aQute.libg.uri.URIUtil;
import aQute.service.reporter.Reporter;

public class Workspace extends Processor {
	private final static Logger	logger							= LoggerFactory.getLogger(Workspace.class);
	public static final File	BND_DEFAULT_WS					= IO.getFile("~/.bnd/default-ws");
	public static final String	BND_CACHE_REPONAME				= "bnd-cache";
	public static final String	EXT								= "ext";
	public static final String	BUILDFILE						= "build.bnd";
	public static final String	CNFDIR							= "cnf";
	public static final String	BNDDIR							= "bnd";
	public static final String	CACHEDIR						= "cache/" + About.CURRENT;
	public static final String	STANDALONE_REPO_CLASS			= "aQute.bnd.repository.osgi.OSGiRepository";

	static final int			BUFFER_SIZE						= IOConstants.PAGE_SIZE * 16;
	private static final String	PLUGIN_STANDALONE				= "-plugin.standalone_";
	private final Pattern		EMBEDDED_REPO_TESTING_PATTERN	= Pattern
			.compile(".*biz\\.aQute\\.bnd\\.embedded-repo(-.*)?\\.jar");

	static class WorkspaceData {
		List<RepositoryPlugin> repositories;
	}

	private final static Map<File,WeakReference<Workspace>>	cache					= newHashMap();
	static Processor							defaults				= null;
	private final Map<String, Project>							models					= new HashMap<>();
	private final Set<String>									modelsUnderConstruction	= new HashSet<>();
	final Map<String,Action>					commands				= newMap();
	final Maven									maven					= new Maven(Processor.getExecutor());
	private final AtomicBoolean								offline					= new AtomicBoolean();
	Settings									settings				= new Settings();
	WorkspaceRepository							workspaceRepo			= new WorkspaceRepository(this);
	static String								overallDriver			= "unset";
	static Parameters							overallGestalt			= new Parameters();
	/**
	 * Signal a BndListener plugin. We ran an infinite bug loop :-(
	 */
	final ThreadLocal<Reporter>					signalBusy				= new ThreadLocal<Reporter>();
	ResourceRepositoryImpl						resourceRepositoryImpl;
	private Parameters							gestalt;
	private String								driver;
	private final WorkspaceLayout				layout;
	final Set<Project>							trail					= Collections
			.newSetFromMap(new ConcurrentHashMap<Project,Boolean>());
	private WorkspaceData						data					= new WorkspaceData();
	private File								buildDir;

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
				String redirect = IO.collect(test).trim();
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
				cache.put(workspaceDir, new WeakReference<Workspace>(ws));
			}
			return ws;
		}
	}

	public Workspace(File workspaceDir) throws Exception {
		this(workspaceDir, CNFDIR);
	}

	public Workspace(File workspaceDir, String bndDir) throws Exception {
		super(getDefaults());
		workspaceDir = workspaceDir.getAbsoluteFile();
		setBase(workspaceDir); // setBase before call to setFileSystem
		this.layout = WorkspaceLayout.BND;
		addBasicPlugin(new LoggingProgressPlugin());
		setFileSystem(workspaceDir, bndDir);
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
		if (!buildFile.isFile())
			warning("No Build File in %s", workspaceDir);

		setProperties(buildFile, workspaceDir);
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

	private Workspace(WorkspaceLayout layout) throws Exception {
		super(getDefaults());
		this.layout = layout;
		setBuildDir(IO.getFile(BND_DEFAULT_WS, CNFDIR));
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
		synchronized (models) {
			Project project = models.get(bsn);
			if (project != null) {
				return project;
			}
			if (!modelsUnderConstruction.add(bsn)) {
				return null;
			}
			try {
				File projectDir = getFile(bsn);
				File bnd = getFile(projectDir, Project.BNDFILE);
				if (!bnd.isFile()) {
					return null;
				}
				project = new Project(this, projectDir);
				if (!project.isValid()) {
					return null;
				}
				models.put(bsn, project);
				return project;
			} finally {
				modelsUnderConstruction.remove(bsn);
			}
		}
	}

	void removeProject(Project p) throws Exception {
		if (p.isCnf())
			return;

		synchronized (models) {
			models.remove(p.getName());
		}
		for (LifeCyclePlugin lp : getPlugins(LifeCyclePlugin.class)) {
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
		data = new WorkspaceData();
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
		try (Stream<Path> paths = Files.list(getBase().toPath())) {
			List<Project> projects = paths
				.filter(p -> Files.isDirectory(p) && Files.isRegularFile(p.resolve(Project.BNDPATH)))
				.map(p -> getProject(p.getFileName()
					.toString()))
				.filter(Objects::nonNull)
				.collect(toList());
			return projects;
		}
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
			if (lock.tryLock(50, TimeUnit.SECONDS) == false)
				throw new TimeLimitExceededException("Cached File Repo is locked and can't acquire it");
			try {
				if (super.init()) {
					inited = true;
					IO.mkdirs(root);
					if (!root.isDirectory())
						throw new IllegalArgumentException("Cache directory " + root + " not a directory");

					try (InputStream in = getClass().getResourceAsStream(EMBEDDED_REPO)) {
						if (in != null) {
							unzip(in, root);
							return true;
						}
					}
					// We may be in unit test, look for
					// biz.aQute.bnd.embedded-repo.jar on the
					// classpath
					StringTokenizer classPathTokenizer = new StringTokenizer(
							System.getProperty("java.class.path", ""), File.pathSeparator);
					while (classPathTokenizer.hasMoreTokens()) {
						String classPathEntry = classPathTokenizer.nextToken().trim();
						if (EMBEDDED_REPO_TESTING_PATTERN.matcher(classPathEntry).matches()) {
							try (InputStream in = IO.stream(Paths.get(classPathEntry))) {
								unzip(in, root);
								return true;
							}
						}
					}
					error("Couldn't find biz.aQute.bnd.embedded-repo on the classpath");
					return false;
				} else
					return false;
			} finally {
				lock.unlock();
			}
		}

		private void unzip(InputStream in, File dir) throws Exception {
			try (JarInputStream jin = new JarInputStream(in)) {
				byte[] data = new byte[BUFFER_SIZE];
				for (JarEntry jentry = jin.getNextJarEntry(); jentry != null; jentry = jin.getNextJarEntry()) {
					if (jentry.isDirectory()) {
						continue;
					}
					String jentryName = jentry.getName();
					if (jentryName.startsWith("META-INF/")) {
						continue;
					}
					File dest = getFile(dir, jentryName);
					long modifiedTime = ZipUtil.getModifiedTime(jentry);
					if (!dest.isFile() || dest.lastModified() < modifiedTime || modifiedTime <= 0) {
						File dp = dest.getParentFile();
						IO.mkdirs(dp);
						try (OutputStream out = IO.outputStream(dest)) {
							for (int size = jin.read(data); size > 0; size = jin.read(data)) {
								out.write(data, 0, size);
							}
						}
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
		Set<Project> result = new LinkedHashSet<Project>();
		for (Project project : getAllProjects()) {
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
			resourceRepositoryImpl.setCache(IO.getFile(getProperty(CACHEDIR, "~/.bnd/caches/shas")));
			resourceRepositoryImpl.setExecutor(getExecutor());
			resourceRepositoryImpl.setIndexFile(getFile(getBuildDir(), "repo.json"));
			resourceRepositoryImpl.setURLConnector(new MultiURLConnectionHandler(this));
			customize(resourceRepositoryImpl, null);
			list.add(resourceRepositoryImpl);

			//
			// Exporters
			//

			list.add(new SubsystemExporter());

			try {
				HttpClient client = new HttpClient(getExecutor(), getScheduledExecutor());
				client.setOffline(getOffline());
				client.setRegistry(this);
				try (ConnectionSettings cs = new ConnectionSettings(this, client)) {
					cs.readSettings();
				}

				list.add(client);
			} catch (Exception e) {
				exception(e, "Failed to load the communication settings");
			}

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
		Map<DownloadBlocker,Attrs> blockers = new HashMap<DownloadBlocker,Attrs>();

		for (Entry<String,Attrs> i : extensions.entrySet()) {
			String bsn = removeDuplicateMarker(i.getKey());
			String stringRange = i.getValue().get(VERSION_ATTRIBUTE);

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
					try(InputStream is = manifests.nextElement().openStream()) {
						Manifest m = new Manifest(is);
						Parameters activators = new Parameters(m.getMainAttributes().getValue("Extension-Activator"), this);
						for (Entry<String, Attrs> e : activators.entrySet()) {
							try {
								Class<?> c = cl.loadClass(e.getKey());
								ExtensionActivator extensionActivator = (ExtensionActivator) c.getConstructor()
										.newInstance();
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

	/**
	 * Provide access to the global settings of this machine.
	 * 
	 * @throws Exception
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
		return IO.getFile(getBuildDir(), BUILDFILE).isFile();
	}

	public RepositoryPlugin getRepository(String repo) throws Exception {
		for (RepositoryPlugin r : getRepositories()) {
			if (repo.equals(r.getName())) {
				return r;
			}
		}
		return null;
	}

	public void close() {
		synchronized (cache) {
			WeakReference<Workspace> wsr = cache.get(getBase());
			if ((wsr != null) && (wsr.get() == this)) {
				cache.remove(getBase());
			}
		}

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
		IO.mkdirs(pdir);

		IO.store("#\n#   " + name.toUpperCase().replace('.', ' ') + "\n#\n", getFile(pdir, Project.BNDFILE));
		Project p = new Project(this, pdir);

		IO.mkdirs(p.getTarget());
		IO.mkdirs(p.getOutput());
		IO.mkdirs(p.getTestOutput());
		for (File dir : p.getSourcePath()) {
			IO.mkdirs(dir);
		}
		IO.mkdirs(p.getTestSrc());

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

		Object l = plugin.getConstructor().newInstance();

		try (Formatter setup = new Formatter()) {
			setup.format("#\n" //
					+ "# Plugin %s setup\n" //
					+ "#\n", alias);
			setup.format("-plugin.%s = %s", alias, plugin.getName());

			for (Map.Entry<String,String> e : parameters.entrySet()) {
				setup.format("; \\\n \t%s = '%s'", e.getKey(), escaped(e.getValue()));
			}
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

	static Pattern ESCAPE_P = Pattern.compile("(\"|')(.*)\1");

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

		Parameters standalone = new Parameters(run.getProperty(STANDALONE), ws);
		StringBuilder sb = new StringBuilder();
		try (Formatter f = new Formatter(sb, Locale.US)) {
			int counter = 1;
			for (Map.Entry<String,Attrs> e : standalone.entrySet()) {
				String locationStr = e.getKey();
				if ("true".equalsIgnoreCase(locationStr))
					break;

				URI resolvedLocation = URIUtil.resolve(base, locationStr);

				String key = f.format("%s%02d", PLUGIN_STANDALONE, counter).toString();
				sb.setLength(0);
				Attrs attrs = e.getValue();
				String name = attrs.get("name");
				if (name == null) {
					name = String.format("repo%02d", counter);
				}
				f.format("%s; name='%s'; locations='%s'", STANDALONE_REPO_CLASS, name, resolvedLocation);
				for (Map.Entry<String,String> attribEntry : attrs.entrySet()) {
					if (!"name".equals(attribEntry.getKey()))
						f.format("; %s='%s'", attribEntry.getKey(), attribEntry.getValue());
				}
				String value = f.toString();
				sb.setLength(0);
				ws.setProperty(key, value);
				counter++;
			}
		}

		return ws;
	}

	public boolean isDefaultWorkspace() {
		return BND_DEFAULT_WS.equals(getBase());
	}
}

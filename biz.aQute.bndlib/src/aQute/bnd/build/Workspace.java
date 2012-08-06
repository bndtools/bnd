package aQute.bnd.build;

import java.io.*;
import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.jar.*;

import javax.naming.*;

import aQute.bnd.maven.support.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.bnd.service.action.*;
import aQute.lib.deployer.*;
import aQute.lib.io.*;
import aQute.service.reporter.*;

public class Workspace extends Processor {
	public static final String					BUILDFILE	= "build.bnd";
	public static final String					CNFDIR		= "cnf";
	public static final String					BNDDIR		= "bnd";
	public static final String					CACHEDIR	= "cache";

	static Map<File,WeakReference<Workspace>>	cache		= newHashMap();
	final Map<String,Project>					models		= newHashMap();
	final Map<String,Action>					commands	= newMap();
	final File									buildDir;
	final Maven									maven		= new Maven(Processor.getExecutor());
	private boolean								offline		= true;

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

	public static Workspace getWorkspace(File parent) throws Exception {
		File workspaceDir = parent.getAbsoluteFile();

		// the cnf directory can actually be a
		// file that redirects
		while (workspaceDir.isDirectory()) {
			File test = new File(workspaceDir, CNFDIR);

			if (!test.exists())
				test = new File(workspaceDir, BNDDIR);

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
				ws = new Workspace(workspaceDir);
				cache.put(workspaceDir, new WeakReference<Workspace>(ws));
			}
			return ws;
		}
	}

	public Workspace(File dir) throws Exception {
		dir = dir.getAbsoluteFile();
		if (!dir.exists() && !dir.mkdirs()) {
			throw new IOException("Could not create directory " + dir);
		}
		assert dir.isDirectory();

		File buildDir = new File(dir, BNDDIR).getAbsoluteFile();
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
			if (new File(file, Project.BNDFILE).isFile())
				projects.add(getProject(file));
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
	final ThreadLocal<Reporter>	signalBusy	= new ThreadLocal<Reporter>();

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
		protected void init() throws Exception {
			if (lock.tryLock(50, TimeUnit.SECONDS) == false)
				throw new TimeLimitExceededException("Cached File Repo is locked and can't acquire it");
			try {
				if (!inited) {
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
						error("Couldn't find embedded-repo.jar in bundle ");
					}
				}
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
						if (!dest.isFile() || dest.lastModified() < jentry.getTime() || jentry.getTime() == 0) {
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
		super.setTypeSpecificPlugins(list);
		list.add(maven);
		list.add(new CachedFileRepo());
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
}

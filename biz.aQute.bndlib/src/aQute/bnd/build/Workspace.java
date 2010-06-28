package aQute.bnd.build;

import java.io.*;
import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.jar.*;

import javax.naming.*;

import aQute.bnd.service.*;
import aQute.bnd.service.action.*;
import aQute.lib.deployer.*;
import aQute.lib.osgi.*;

public class Workspace extends Processor {
	public static final String					BUILDFILE	= "build.bnd";
	public static final String					CNFDIR		= "cnf";
	public static final String					BNDDIR		= "bnd";
	public static final String					CACHEDIR	= "cache";

	static Map<File, WeakReference<Workspace>>	cache		= newHashMap();
	final Map<String, Project>					models		= newHashMap();
	final Map<String, Action>					commands	= newMap();
	final CachedFileRepo						cachedRepo;
	final File buildDir;
	
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
		File test = new File(workspaceDir, CNFDIR);
		
		// Check if we can find a bnd dir higher up so
		// we can have nested workspaces.
		if ( !test.isDirectory())
			test = new File(workspaceDir, BNDDIR);

		if ( !test.isDirectory())
			throw new IllegalArgumentException("No Workspace found from: " + parent);
		
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
		dir.mkdirs();
		assert dir.isDirectory();

		File buildDir = new File(dir, BNDDIR).getAbsoluteFile();
		if (!buildDir.isDirectory())
			buildDir = new File(dir, CNFDIR).getAbsoluteFile();

		this.buildDir = buildDir;
		
		File buildFile = new File(buildDir, BUILDFILE).getAbsoluteFile();
		if (!buildFile.isFile())
			warning("No Build File in " + dir);
		setProperties(buildFile, dir);
		cachedRepo = new CachedFileRepo();
		addBasicPlugin(cachedRepo);
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

	public boolean refresh() {
		if (super.refresh()) {
			for (Project project : getCurrentProjects()) {
				project.propertiesChanged();
			}
			return true;
		}
		return false;
	}

	public String _workspace(String args[]) {
		return getBase().getAbsolutePath();
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
				l.changed(f);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	private void copy(InputStream in, OutputStream out) throws Exception {
		byte data[] = new byte[10000];
		int size = in.read(data);
		while (size > 0) {
			out.write(data, 0, size);
			size = in.read(data);
		}
	}

	class CachedFileRepo extends FileRepo {
		final Lock lock = new ReentrantLock();
		boolean	inited;

		CachedFileRepo() {
			super("cache", getFile(buildDir, CACHEDIR), false);
		}

		protected void init() throws Exception {
			if ( lock.tryLock(50, TimeUnit.SECONDS) == false)
				throw new TimeLimitExceededException("Cached File Repo is locked and can't acquire it");			
			try {
				if (!inited) {
					inited = true;
					root.mkdirs();
					if (!root.isDirectory())
						throw new IllegalArgumentException("Cannot create cache dir " + root);

					InputStream in = getClass().getResourceAsStream(EMBEDDED_REPO);
					if (in != null)
						unzip(in, root);
				}
			} finally {
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
						if (!dest.isFile() || dest.lastModified() < jentry.getTime() || jentry.getTime()==0) {
							dest.getParentFile().mkdirs();
							FileOutputStream out = new FileOutputStream(dest);
							try {
								copy(jin, out);
							} finally {
								out.close();
							}
						}
					}
					jentry = jin.getNextJarEntry();
				}
			} finally {
				in.close();
			}
		}
	}

	public List<RepositoryPlugin> getRepositories() {
		return getPlugins(RepositoryPlugin.class);
	}
}

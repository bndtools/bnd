package aQute.bnd.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.Strategy;

public class Container {
	public enum TYPE {
		REPO, PROJECT, EXTERNAL, LIBRARY, ERROR
	}

	private final File			file;
	private final String		path;
	final TYPE					type;
	final String				bsn;
	final String				version;
	final String				error;
	final Project				project;
	final DownloadBlocker		db;
	volatile Map<String,String>	attributes;
	private long				manifestTime;
	private Manifest			manifest;

	/** provides access to expanded 'Bundle-ClassPath' entries */
	private BundleClasspathCache	cache;

	Container(Project project, String bsn, String version, TYPE type, File source, String error,
			Map<String,String> attributes, DownloadBlocker db) {
		this.bsn = bsn;
		this.version = version;
		this.type = type;
		this.file = source != null ? source : new File("/" + bsn + ":" + version + ":" + type);
		this.path = file.getAbsolutePath();

		this.project = project;
		this.error = error;
		if (attributes == null || attributes.isEmpty())
			this.attributes = Collections.emptyMap();
		else
			this.attributes = attributes;
		this.db = db;
	}

	public Container(Project project, File file, Map<String,String> attributes) {
		this(project, file.getName(), "project", TYPE.PROJECT, file, null, attributes, null);
	}

	public Container(Project project, File file) {
		this(project, file, null);
	}

	public Container(File file, DownloadBlocker db) {
		this(null, file.getName(), "project", TYPE.EXTERNAL, file, null, null, db);
	}

	public File getFile() {
		if (db != null && db.getReason() != null) {
			return new File(db.getReason() + ": " + file);
		}
		return file;
	}

	/**
	 * Iterate over the containers and get the files they represent @param
	 * files @return @throws Exception
	 */
	public boolean contributeFiles(List<File> files, Processor reporter) throws Exception {
		switch (type) {
			case EXTERNAL :
			case REPO :
				files.add(getFile());
				return true;

			case PROJECT :
				File[] fs = project.build();
				reporter.getInfo(project);
				if (fs == null)
					return false;

				for (File f : fs)
					files.add(f);
				return true;

			case LIBRARY :
				List<Container> containers = getMembers();
				for (Container container : containers) {
					if (!container.contributeFiles(files, reporter))
						return false;
				}
				return true;

			case ERROR :
				reporter.error(error);
				return false;
		}
		return false;
	}

	public String getBundleSymbolicName() {
		return bsn;
	}

	public String getVersion() {
		return version;
	}

	public TYPE getType() {
		return type;
	}

	public String getError() {
		return error;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Container)
			return path.equals(((Container) other).path);
		return false;
	}

	@Override
	public int hashCode() {
		return path.hashCode();
	}

	public Project getProject() {
		return project;
	}

	/**
	 * Must show the file name or the error formatted as a file name @return
	 */
	@Override
	public String toString() {
		if (getError() != null)
			return "/error/" + getError();
		return getFile().getAbsolutePath().replace(File.separatorChar, '/');
	}

	public Map<String,String> getAttributes() {
		return attributes;
	}

	public void putAttribute(String name, String value) {
		if (attributes == Collections.<String, String> emptyMap())
			attributes = new HashMap<String,String>(1);
		attributes.put(name, value);
	}

	/**
	 * Return the this if this is anything else but a library. If it is a
	 * library, return the members. This could work recursively, e.g., libraries
	 * can point to libraries. @return @throws Exception
	 */
	public List<Container> getMembers() throws Exception {
		List<Container> result = project.newList();

		// Are ww a library? If no, we are the result
		if (getType() == TYPE.LIBRARY) {
			// We are a library, parse the file. This is
			// basically a specification clause per line.
			// I.e. you can do bsn; version, bsn2; version. But also
			// spread it out over lines.
			InputStream in = null;
			BufferedReader rd = null;
			String line;
			try {
				in = new FileInputStream(getFile());
				rd = new BufferedReader(new InputStreamReader(in, Constants.DEFAULT_CHARSET));
				while ((line = rd.readLine()) != null) {
					line = line.trim();
					if (!line.startsWith("#") && line.length() > 0) {
						List<Container> list = project.getBundles(Strategy.HIGHEST, line, null);
						result.addAll(list);
					}
				}
			}
			finally {
				if (rd != null) {
					rd.close();
				}
				if (in != null) {
					in.close();
				}
			}
		}
		// If this container is a bundle using the 'Bundle-ClassPath' header,
		// then we need to add the associated entries to the build path
		else if ((getType() == TYPE.EXTERNAL || getType() == TYPE.REPO) && hasBundleClasspath()) {
			result.add(this);
			result.addAll(getBundleClasspathEntries());
		}
		else {
			result.add(this);
		}

		return result;
	}

	private List<Container> getBundleClasspathEntries() throws Exception {
		List<Container> entries = new ArrayList<>();

		BundleClasspathCache bcpCache = getCache();

		// handles expanding entries specified using 'Bundle-ClassPath'
		Parameters bcp = getBundleClasspath();
		Jar jar = new Jar(file);
		try {
			for (String path : bcp.keySet()) {
				Resource resource = jar.getResource(path);

				if (resource != null) {
					File bcpFile = bcpCache.getEntry(this, resource, path);
					Container embedded = new Container(this.project, path, getVersion(), getType(), bcpFile, null,
							getAttributes(), this.db);
					entries.add(embedded);
				}
			}
		}
		finally {
			jar.close();
		}

		return entries;
	}

	private Parameters getBundleClasspath() throws Exception {
		Domain manifest = Domain.domain(file);
		return manifest.getParameters(Constants.BUNDLE_CLASSPATH);
	}

	private boolean hasBundleClasspath() throws Exception {
		return !getBundleClasspath().isEmpty();
	}

	private BundleClasspathCache getCache() {
		if (cache == null) {
			cache = new BundleClasspathCache();
			File cacheDir = project.getWorkspace().getCache("bundle_classpath_cache");
			cache.init(cacheDir);
		}
		return cache;
	}

	/**
	 * Flatten a container in the output list. (e.g. expand any
	 * libraries). @param container the container to flatten @param list the
	 * result list
	 */
	public static void flatten(Container container, List<Container> list) throws Exception {
		if (container.getType() == TYPE.LIBRARY) {
			flatten(container.getMembers(), list);
		} else
			list.add(container);
	}

	/**
	 * Take a container list and flatten it (e.g. expand any libraries). @param
	 * containers The containers to flatten, can be null @return a list of
	 * containers guaranteed to contain no libraries
	 */
	public static List<Container> flatten(Collection<Container> containers) throws Exception {
		List<Container> list = new ArrayList<Container>();
		flatten(containers, list);
		return list;
	}

	/**
	 * Take a container list and flatten it (e.g. expand any libraries). @param
	 * containers The containers to flatten, can be null @return a list of
	 * containers guaranteed to contain no libraries
	 */

	public static void flatten(Collection<Container> containers, List<Container> list) throws Exception {
		if (containers == null)
			return;

		for (Container container : containers) {
			flatten(container, list);
		}

	}

	/**
	 * Answer the manifest for this container (if possible). Manifest is cached
	 * until the file is renewed.
	 */

	public Manifest getManifest() throws Exception {
		if (getError() != null || getFile() == null)
			return null;

		if (manifestTime < getFile().lastModified()) {
			InputStream in = new FileInputStream(getFile());
			try {
				JarInputStream jin = new JarInputStream(in);
				manifest = jin.getManifest();
				jin.close();
				manifestTime = getFile().lastModified();
			}
			finally {
				in.close();
			}
		}
		return manifest;
	}
}

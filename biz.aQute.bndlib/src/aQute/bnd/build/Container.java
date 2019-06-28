package aQute.bnd.build;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import aQute.bnd.build.DownloadBlocker.Stage;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.Strategy;
import aQute.lib.io.IO;

public class Container {
	public enum TYPE {
		REPO,
		PROJECT,
		EXTERNAL,
		LIBRARY,
		ERROR
	}

	private volatile File					file;
	private final String					path;
	private final TYPE						type;
	private final String					bsn;
	private final String					version;
	private volatile String					error;
	private final Project					project;
	private volatile DownloadBlocker		db;
	private volatile Map<String, String>	attributes;
	private long							manifestTime;
	private Manifest						manifest;
	private volatile File[]					bundleClasspathExpansion;
	public String							warning	= "";

	Container(Project project, String bsn, String version, TYPE type, File source, String error,
		Map<String, String> attributes, DownloadBlocker db) {
		this.bsn = bsn;
		this.version = version;
		this.type = type;
		this.file = source != null ? source : new File("/" + bsn + ":" + version + ":" + type);
		this.path = IO.absolutePath(file);

		this.project = project;
		this.error = error;

		if (attributes == null || attributes.isEmpty()) {
			attributes = Collections.emptyMap();
		} else if (attributes.containsKey("expand-bcp")) {
			this.bundleClasspathExpansion = new File[0];
		}
		this.attributes = attributes;
		this.db = db;

	}

	public Container(Project project, File file, Map<String, String> attributes) {
		this(project, file.getName(), "project", TYPE.PROJECT, file, null, attributes, null);
	}

	public Container(Project project, File file) {
		this(project, file, null);
	}

	public Container(File file, DownloadBlocker db) {
		this(null, file.getName(), "project", TYPE.EXTERNAL, file, null, null, db);
	}

	public Container(File file, DownloadBlocker db, Attrs attributes) {
		this(null, file.getName(), "project", TYPE.EXTERNAL, file, null, attributes, db);
	}

	private Container(Project project, String message) {
		this.project = project;
		this.bsn = "<unknown>";
		this.version = "<unknown>";
		this.warning = message;
		this.type = TYPE.ERROR;
		this.path = "unknown";
	}

	public File getFile() {
		DownloadBlocker blocker = db;
		if (blocker != null) {
			File f = blocker.getFile();
			if (blocker.getStage() == Stage.FAILURE) {
				String r = blocker.getReason();
				if (error == null) {
					error = r;
				}
				return new File(r + ": " + f);
			}
			this.file = f;
			this.db = null;
		}
		return file;
	}

	/**
	 * Iterate over the containers and get the files they represent. If a file
	 * is already in the list, it is not added again.
	 *
	 * @param files
	 * @throws Exception
	 */
	public boolean contributeFiles(List<File> files, Processor reporter) throws Exception {
		switch (type) {
			case EXTERNAL :
			case REPO :
				for (File f : getBundleClasspathFiles()) {
					if (!files.contains(f)) {
						files.add(f);
					}
				}
				return true;

			case PROJECT :
				File[] fs = project.build();
				reporter.getInfo(project);
				if (fs == null)
					return false;

				for (File f : fs) {
					if (!files.contains(f)) {
						files.add(f);
					}
				}
				return true;

			case LIBRARY :
				List<Container> containers = getMembers();
				for (Container container : containers) {
					if (!container.contributeFiles(files, reporter))
						return false;
				}
				return true;

			case ERROR :
				reporter.error("%s", getError());
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
	 * Must show the file name or the error formatted as a file name
	 */
	@Override
	public String toString() {
		if (getError() != null)
			return "/error/" + getError();
		return IO.absolutePath(getFile());
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public synchronized void putAttribute(String name, String value) {
		if (attributes == Collections.<String, String> emptyMap())
			attributes = new HashMap<>(1);
		attributes.put(name, value);
	}

	/**
	 * Return the this if this is anything else but a library. If it is a
	 * library, return the members. This could work recursively, e.g., libraries
	 * can point to libraries.
	 *
	 * @throws Exception
	 */
	public List<Container> getMembers() throws Exception {
		List<Container> result = project.newList();

		// Are ww a library? If no, we are the result
		if (getType() == TYPE.LIBRARY) {
			// We are a library, parse the file. This is
			// basically a specification clause per line.
			// I.e. you can do bsn; version, bsn2; version. But also
			// spread it out over lines.
			try (BufferedReader rd = IO.reader(getFile(), Constants.DEFAULT_CHARSET)) {
				String line;
				while ((line = rd.readLine()) != null) {
					line = line.trim();
					if (!line.startsWith("#") && line.length() > 0) {
						List<Container> list = project.getBundles(Strategy.HIGHEST, line, null);
						result.addAll(list);
					}
				}
			}
		} else
			result.add(this);

		return result;
	}

	/**
	 * Flatten a container in the output list. (e.g. expand any libraries).
	 *
	 * @param container the container to flatten
	 * @param list the result list
	 */
	public static void flatten(Container container, List<Container> list) throws Exception {
		if (container.getType() == TYPE.LIBRARY) {
			flatten(container.getMembers(), list);
		} else
			list.add(container);
	}

	/**
	 * Take a container list and flatten it (e.g. expand any libraries).
	 *
	 * @param containers The containers to flatten, can be null
	 * @return a list of containers guaranteed to contain no libraries
	 */
	public static List<Container> flatten(Collection<Container> containers) throws Exception {
		List<Container> list = new ArrayList<>();
		flatten(containers, list);
		return list;
	}

	/**
	 * Take a container list and flatten it (e.g. expand any libraries).
	 *
	 * @param containers The containers to flatten, can be null
	 * @param list of containers guaranteed to contain no libraries
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
			try (JarInputStream jin = new JarInputStream(IO.stream(getFile()))) {
				manifest = jin.getManifest();
			}
			manifestTime = getFile().lastModified();
		}
		return manifest;
	}

	/**
	 * @throws Exception
	 */

	private File[] getBundleClasspathFiles() throws Exception {
		File[] bce = bundleClasspathExpansion;
		if (bce == null) {
			return bundleClasspathExpansion = new File[] {
				getFile()
			};
		}
		if (bce.length != 0) {
			return bce;
		}

		File file = getFile();
		Manifest m = getManifest();
		String bundleClassPath;
		if (m == null || (bundleClassPath = m.getMainAttributes()
			.getValue(Constants.BUNDLE_CLASSPATH)) == null) {
			return bundleClasspathExpansion = new File[] {
				file
			};
		}

		File bundleClasspathDirectory = IO.getFile(file.getParentFile(), "." + file.getName() + "-bcp");
		Parameters header = new Parameters(bundleClassPath, project);
		List<File> files = new ArrayList<>(header.size());
		IO.mkdirs(bundleClasspathDirectory);

		int n = 0;
		Jar jar = null;
		try {
			for (Map.Entry<String, Attrs> entry : header.entrySet()) {
				if (".".equals(entry.getKey())) {
					files.add(file);
				} else {
					File member = new File(bundleClasspathDirectory, n + "-" + toName(entry.getKey()));
					if (!isCurrent(file, member)) {

						if (jar == null) {
							jar = new Jar(file);
						}

						Resource resource = jar.getResource(entry.getKey());
						if (resource == null) {
							warning += "Invalid bcp entry: " + entry.getKey() + "\n";
						} else {
							IO.copy(resource.openInputStream(), member);
							member.setLastModified(file.lastModified());
						}

					}
					files.add(member);
				}
				n++;
			}
		} finally {
			if (jar != null)
				jar.close();
		}

		return bundleClasspathExpansion = files.toArray(bce);
	}

	private boolean isCurrent(File file, File member) {
		return member.isFile() && member.lastModified() == file.lastModified();
	}

	private String toName(String key) {
		int n = key.lastIndexOf('/');
		return key.substring(n + 1);
	}

	public String getWarning() {
		return warning;
	}

	/**
	 * Convert a set of containers to a list of paths. Only containers that have
	 * no error will be converted. Any errors will be collected in the errors
	 * parameter. If the errors parameter is null, an exception is thrown for
	 * the first erroneous container.
	 *
	 * @param errors a list of errors or null
	 * @param containers the containers to convert.
	 */
	static public List<String> toPaths(List<String> errors, Collection<Container> containers) {
		return containers.stream()
			.filter(container -> {
				if (container.getError() == null)
					return true;

				if (errors != null) {
					errors.add(container.getError());
				} else
					throw new IllegalArgumentException("Container " + container + " has error " + container.getError());

				return false;
			})
			.map(container -> container.getFile())
			.map(File::getAbsolutePath)
			.collect(Collectors.toList());
	}

	public static Container error(Project project, String message) {

		return new Container(project, message);
	}

}

package aQute.bnd.build;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import aQute.bnd.service.RepositoryPlugin.Strategy;
import aQute.lib.osgi.*;

public class Container {
	public enum TYPE {
		REPO, PROJECT, EXTERNAL, LIBRARY, ERROR
	}

	final File					file;
	final TYPE					type;
	final String				bsn;
	final String				version;
	final String				error;
	final Project				project;
	volatile Map<String,String>	attributes;
	private long				manifestTime;
	private Manifest			manifest;

	Container(Project project, String bsn, String version, TYPE type, File source, String error,
			Map<String,String> attributes) {
		this.bsn = bsn;
		this.version = version;
		this.type = type;
		this.file = source != null ? source : new File("/" + bsn + ":" + version + ":" + type);
		this.project = project;
		this.error = error;
		if (attributes == null || attributes.isEmpty())
			this.attributes = Collections.emptyMap();
		else
			this.attributes = attributes;
	}

	public Container(Project project, File file) {
		this(project, file.getName(), "project", TYPE.PROJECT, file, null, null);
	}

	public Container(File file) {
		this(null, file.getName(), "project", TYPE.EXTERNAL, file, null, null);
	}

	public File getFile() {
		return file;
	}

	/**
	 * Iterate over the containers and get the files they represent
	 * 
	 * @param files
	 * @return
	 * @throws Exception
	 */
	public boolean contributeFiles(List<File> files, Processor reporter) throws Exception {
		switch (type) {
			case EXTERNAL :
			case REPO :
				files.add(file);
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

	public boolean equals(Object other) {
		if (other instanceof Container)
			return file.equals(((Container) other).file);
		return false;
	}

	public int hashCode() {
		return file.hashCode();
	}

	public Project getProject() {
		return project;
	}

	/**
	 * Must show the file name or the error formatted as a file name
	 * 
	 * @return
	 */
	public String toString() {
		if (getError() != null)
			return "/error/" + getError();
		return getFile().getAbsolutePath();
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
	 * can point to libraries.
	 * 
	 * @return
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
			InputStream in = null;
			BufferedReader rd = null;
			String line;
			try {
				in = new FileInputStream(file);
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
		} else
			result.add(this);

		return result;
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

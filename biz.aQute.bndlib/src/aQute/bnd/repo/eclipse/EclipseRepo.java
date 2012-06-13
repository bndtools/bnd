package aQute.bnd.repo.eclipse;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.*;

import aQute.bnd.service.*;
import aQute.lib.io.*;
import aQute.lib.osgi.*;
import aQute.libg.generics.*;
import aQute.libg.header.*;
import aQute.libg.reporter.*;
import aQute.libg.version.*;

public class EclipseRepo implements Plugin, RepositoryPlugin {
	File						root;
	Reporter					reporter;
	String						name;
	Parameters					index;

	public final static String	LOCATION	= "location";
	public final static String	NAME		= "name";

	public void setProperties(Map<String,String> map) {
		String location = map.get(LOCATION);
		if (location == null)
			throw new IllegalArgumentException("Location muse be set on a EclipseRepo plugin");

		root = new File(location);
		if (!root.isDirectory())
			throw new IllegalArgumentException("Repository is not a valid directory " + root);

		if (!new File(root, "plugins").isDirectory())
			throw new IllegalArgumentException("Repository is not a valid directory (no plugins directory)" + root);

		name = map.get(NAME);

		try {
			index = buildIndex();
		}
		catch (Exception e) {
			throw new RuntimeException("Could not build index for eclipse repo: " + root);
		}
	}

	Parameters buildIndex() throws Exception {
		File index = new File(root, "bnd.index").getAbsoluteFile();
		File[] plugins = new File(root, "plugins").listFiles();

		for (File f : plugins) {
			f = f.getAbsoluteFile();
			if (f.isFile()) {
				if (f.lastModified() > index.lastModified()) {

					Parameters map = buildIndex(plugins);
					write(index, map);
					return map;
				}
			}
		}

		String s = read(index);
		return Processor.parseHeader(s, null);
	}

	private String read(File index) throws Exception {
		if (index.isFile()) {
			BufferedReader fr = IO.reader(index);
			StringBuilder sb = new StringBuilder();

			try {
				String s = fr.readLine();
				while (s != null) {
					sb.append(s);
					s = fr.readLine();
				}
			}
			finally {
				fr.close();
			}
		}
		return null;
	}

	private void write(File index, Map<String, ? extends Map<String,String>> map) throws Exception {
		String s = Processor.printClauses(map);
		index.getParentFile().mkdirs();
		PrintWriter fw = IO.writer(index);
		try {
			fw.write(s);
		}
		finally {
			fw.close();
		}
	}

	private Parameters buildIndex(File[] plugins) {
		Parameters map = new Parameters();
		for (File plugin : plugins) {
			try {
				Jar jar = new Jar(plugin);
				Manifest manifest = jar.getManifest();
				String bsn = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
				String version = manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION);

				if (bsn != null) {
					if (version == null)
						version = "0";

					Map<String,String> instance = map.get(bsn);
					if (instance == null) {
						instance = Create.map();
					}
					instance.put(version, plugin.getAbsolutePath());
				}
			}
			catch (Exception e) {
				// Ignore exceptions in the plugins dir.
			}
		}
		return map;
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	public boolean canWrite() {
		return false;
	}

	public File[] get(String bsn, String range) throws Exception {
		VersionRange r = new VersionRange(range);
		Map<String,String> instances = index.get(bsn);
		if (instances == null)
			return null;

		List<File> result = Create.list();

		for (Entry<String,String> entry : instances.entrySet()) {
			if (r.includes(new Version(entry.getKey()))) {
				File f = new File(entry.getValue());
				if (f.isFile()) {
					result.add(f);
				}
			}
		}
		return result.toArray(new File[result.size()]);
	}

	public String getName() {
		return name;
	}

	public List<String> list(String regex) {
		Instruction pattern = null;
		if (regex != null)
			pattern = new Instruction(regex);

		List<String> result = new ArrayList<String>();
		for (String f : index.keySet()) {
			if (pattern == null || pattern.matches(f))
				result.add(f);
		}
		return result;
	}

	public File put(Jar jar) throws Exception {
		return null;
	}

	public List<Version> versions(String bsn) {
		Map<String,String> instances = index.get(bsn);
		if (instances == null)
			return null;

		List<Version> versions = Create.list();
		for (String v : instances.keySet())
			versions.add(new Version(v));
		return versions;
	}

	public File get(String bsn, String range, Strategy strategy, Map<String,String> properties) throws Exception {
		File[] files = get(bsn, range);
		if (files.length >= 0) {
			switch (strategy) {
				case LOWEST :
					return files[0];
				case HIGHEST :
					return files[files.length - 1];
			}
		}
		return null;
	}

	public String getLocation() {
		return root.toString();
	}
}

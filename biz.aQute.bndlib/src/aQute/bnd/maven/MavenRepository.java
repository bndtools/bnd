package aQute.bnd.maven;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import aQute.bnd.service.*;
import aQute.lib.osgi.*;
import aQute.libg.reporter.*;
import aQute.libg.version.*;

public class MavenRepository implements RepositoryPlugin, Plugin, BsnToMavenPath {

	public final static String	NAME	= "name";

	File						root;
	Reporter					reporter;
	String						name;

	public String toString() {
		return "maven:" + root;
	}

	public boolean canWrite() {
		return false;
	}

	private File[] get(String bsn, String version) throws Exception {
		VersionRange range = new VersionRange("0");
		if (version != null)
			range = new VersionRange(version);

		List<BsnToMavenPath> plugins = ((Processor) reporter).getPlugins(BsnToMavenPath.class);
		if (plugins.isEmpty())
			plugins.add(this);

		for (BsnToMavenPath cvr : plugins) {
			String[] paths = cvr.getGroupAndArtifact(bsn);
			if (paths != null) {
				File[] files = find(paths[0], paths[1], range);
				if (files != null)
					return files;
			}
		}
		reporter.trace("Cannot find in maven: %s-%s", bsn, version);
		return null;
	}

	File[] find(String groupId, String artifactId, VersionRange range) {
		String path = groupId.replace(".", "/");
		File vsdir = Processor.getFile(root, path);
		if (!vsdir.isDirectory())
			return null;

		vsdir = Processor.getFile(vsdir, artifactId);

		List<File> result = new ArrayList<File>();
		if (vsdir.isDirectory()) {
			String versions[] = vsdir.list();
			for (String v : versions) {
				String vv = Analyzer.cleanupVersion(v);
				if (Verifier.isVersion(vv)) {
					Version vvv = new Version(vv);
					if (range.includes(vvv)) {
						File file = Processor.getFile(vsdir, v + "/" + artifactId + "-" + v + ".jar");
						if (file.isFile())
							result.add(file);
						else
							reporter.warning("Expected maven entry was not a valid file %s ", file);
					}
				} else {
					reporter.warning(
							"Expected a version directory in maven: dir=%s raw-version=%s cleaned-up-version=%s",
							vsdir, vv, v);
				}
			}
		} else
			return null;

		return result.toArray(new File[result.size()]);
	}

	public List<String> list(String regex) {
		List<String> bsns = new ArrayList<String>();
		Pattern match = Pattern.compile(".*");
		if (regex != null)
			match = Pattern.compile(regex);
		find(bsns, match, root, "");
		return bsns;
	}

	void find(List<String> bsns, Pattern pattern, File base, String name) {
		if (base.isDirectory()) {
			String list[] = base.list();
			boolean found = false;
			for (String entry : list) {
				char c = entry.charAt(0);
				if (c >= '0' && c <= '9') {
					if (pattern.matcher(name).matches())
						found = true;
				} else {
					String nextName = entry;
					if (name.length() != 0)
						nextName = name + "." + entry;

					File next = Processor.getFile(base, entry);
					find(bsns, pattern, next, nextName);
				}
			}
			if (found)
				bsns.add(name);
		}
	}

	public File put(Jar jar) throws Exception {
		throw new IllegalStateException("Maven does not support the put command");
	}

	public List<Version> versions(String bsn) throws Exception {

		File files[] = get(bsn, null);
		List<Version> versions = new ArrayList<Version>();
		for (File f : files) {
			String version = f.getParentFile().getName();
			version = Builder.cleanupVersion(version);
			Version v = new Version(version);
			versions.add(v);
		}
		return versions;
	}

	public void setProperties(Map<String,String> map) {
		File home = new File("");
		String root = map.get("root");
		if (root == null) {
			home = new File(System.getProperty("user.home"));
			this.root = Processor.getFile(home, ".m2/repository").getAbsoluteFile();
		} else
			this.root = Processor.getFile(home, root).getAbsoluteFile();

		if (!this.root.isDirectory()) {
			reporter.error("Maven repository did not get a proper URL to the repository %s", root);
		}
		name = map.get(NAME);

	}

	public void setReporter(Reporter processor) {
		this.reporter = processor;
	}

	public String[] getGroupAndArtifact(String bsn) {
		String groupId;
		String artifactId;
		int n = bsn.indexOf('.');

		while (n > 0) {
			artifactId = bsn.substring(n + 1);
			groupId = bsn.substring(0, n);

			File gdir = new File(root, groupId.replace('.', File.separatorChar)).getAbsoluteFile();
			File adir = new File(gdir, artifactId).getAbsoluteFile();
			if (adir.isDirectory())
				return new String[] {
						groupId, artifactId
				};

			n = bsn.indexOf('.', n + 1);
		}
		return null;
	}

	public String getName() {
		if (name == null) {
			return toString();
		}
		return name;
	}

	public File get(String bsn, String range, Strategy strategy, Map<String,String> properties) throws Exception {
		File[] files = get(bsn, range);
		if (files.length >= 0) {
			switch (strategy) {
				case LOWEST :
					return files[0];
				case HIGHEST :
					return files[files.length - 1];
				case EXACT :
					// TODO exact
					break;
			}
		}
		return null;
	}

	public void setRoot(File f) {
		root = f;
	}

	public String getLocation() {
		return root.toString();
	}
}

package aQute.bnd.maven;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.Strategy;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.collections.SortedList;
import aQute.lib.io.IO;
import aQute.service.reporter.Reporter;

@Deprecated
public class MavenRepository implements RepositoryPlugin, Plugin, BsnToMavenPath {
	private final static Logger	logger				= LoggerFactory.getLogger(MavenRepository.class);

	public final static String	NAME				= "name";
	static final String			MAVEN_REPO_LOCAL	= System.getProperty("maven.repo.local", "~/.m2/repository");

	File						root;
	Reporter					reporter;
	String						name;

	@Override
	public String toString() {
		return "maven:" + root;
	}

	@Override
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
		logger.debug("Cannot find in maven: {}-{}", bsn, version);
		return null;
	}

	File[] find(String groupId, String artifactId, VersionRange range) {
		String path = groupId.replace(".", "/");
		File vsdir = Processor.getFile(root, path);
		if (!vsdir.isDirectory())
			return null;

		vsdir = Processor.getFile(vsdir, artifactId);

		List<File> result = new ArrayList<>();
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
						"Expected a version directory in maven: dir=%s raw-version=%s cleaned-up-version=%s", vsdir, vv,
						v);
				}
			}
		} else
			return null;

		return result.toArray(new File[0]);
	}

	@Override
	public List<String> list(String regex) {
		List<String> bsns = new ArrayList<>();
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
					if (pattern.matcher(name)
						.matches())
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

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		throw new UnsupportedOperationException("Maven does not support the put command");
	}

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {

		File files[] = get(bsn, null);
		List<Version> versions = new ArrayList<>();
		for (File f : files) {
			String version = f.getParentFile()
				.getName();
			version = Analyzer.cleanupVersion(version);
			Version v = new Version(version);
			versions.add(v);
		}
		if (versions.isEmpty())
			return SortedList.empty();

		return new SortedList<>(versions);
	}

	@Override
	public void setProperties(Map<String, String> map) {
		String root = map.get("root");
		if (root == null) {
			this.root = IO.getFile(MAVEN_REPO_LOCAL);
		} else {
			File home = new File("");
			this.root = Processor.getFile(home, root)
				.getAbsoluteFile();
		}

		if (!this.root.isDirectory()) {
			reporter.error("Maven repository did not get a proper URL to the repository %s", root);
		}
		name = map.get(NAME);

	}

	@Override
	public void setReporter(Reporter processor) {
		this.reporter = processor;
	}

	@Override
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

	@Override
	public String getName() {
		if (name == null) {
			return toString();
		}
		return name;
	}

	public File get(String bsn, String range, Strategy strategy, Map<String, String> properties) throws Exception {
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

	@Override
	public String getLocation() {
		return root.toString();
	}

	@Override
	public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
		throws Exception {
		File file = get(bsn, version.toString(), Strategy.EXACT, properties);
		if (file == null)
			return null;

		for (DownloadListener l : listeners) {
			try {
				l.success(file);
			} catch (Exception e) {
				reporter.exception(e, "Download listener for %s", file);
			}
		}
		return file;
	}
}

package aQute.lib.deployer;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;

import aQute.bnd.service.*;
import aQute.lib.osgi.*;
import aQute.libg.reporter.*;
import aQute.libg.version.*;

public class FileRepo implements Plugin, RepositoryPlugin, Refreshable {
	public static String LOCATION = "location";
	public static String READONLY = "readonly";
	public static String NAME = "name";

	File[] EMPTY_FILES = new File[0];
	protected File root;
	boolean canWrite = true;
	Pattern REPO_FILE = Pattern
			.compile("([-a-zA-z0-9_\\.]+)-([0-9\\.]+|latest)\\.(jar|lib)");
	Reporter reporter;
	boolean dirty;
	String name;

	public FileRepo() {}
	
	public FileRepo(String name, File location, boolean canWrite) {
		this.name = name;
		this.root = location;
		this.canWrite = canWrite;
	}
	
	protected void init() throws Exception {
		// for extensions
	}
	
	public void setProperties(Map<String, String> map) {
		String location = (String) map.get(LOCATION);
		if (location == null)
			throw new IllegalArgumentException(
					"Location muse be set on a FileRepo plugin");

		root = new File(location);
		if (!root.isDirectory())
			throw new IllegalArgumentException(
					"Repository is not a valid directory " + root);

		String readonly = (String) map.get(READONLY);
		if (readonly != null && Boolean.valueOf(readonly).booleanValue())
			canWrite = false;

		name = (String) map.get(NAME);
	}

	/**
	 * Get a list of URLs to bundles that are constrained by the bsn and
	 * versionRange.
	 */
	public File[] get(String bsn, String versionRange)
			throws Exception {
		init();
		
		// If the version is set to project, we assume it is not
		// for us. A project repo will then get it.
		if (versionRange != null && versionRange.equals("project"))
			return null;

		//
		// Check if the entry exists
		//
		File f = new File(root, bsn);
		if (!f.isDirectory())
			return null;

		//
		// The version range we are looking for can
		// be null (for all) or a version range.
		//
		VersionRange range;
		if (versionRange == null || versionRange.equals("latest")) {
			range = new VersionRange("0");
		} else
			range = new VersionRange(versionRange);

		//
		// Iterator over all the versions for this BSN.
		// Create a sorted map over the version as key
		// and the file as URL as value. Only versions
		// that match the desired range are included in
		// this list.
		//
		File instances[] = f.listFiles();
		SortedMap<Version, File> versions = new TreeMap<Version, File>();
		for (int i = 0; i < instances.length; i++) {
			Matcher m = REPO_FILE.matcher(instances[i].getName());
			if (m.matches() && m.group(1).equals(bsn)) {
				String versionString = m.group(2);
				Version version;
				if (versionString.equals("latest"))
					version = new Version(Integer.MAX_VALUE);
				else
					version = new Version(versionString);

				if (range.includes(version)
						|| versionString.equals(versionRange))
					versions.put(version, instances[i]);
			}
		}

		File[] files = (File[]) versions.values().toArray(EMPTY_FILES);
		if ("latest".equals(versionRange) && files.length > 0) {
			return new File[] { files[files.length - 1] };
		}
		return files;
	}

	public boolean canWrite() {
		return canWrite;
	}

	public File put(Jar jar) throws Exception {
		init();
		dirty = true;

		Manifest manifest = jar.getManifest();
		if (manifest == null)
			throw new IllegalArgumentException("No manifest in JAR: " + jar);

		String bsn = manifest.getMainAttributes().getValue(
				Analyzer.BUNDLE_SYMBOLICNAME);
		if (bsn == null)
			throw new IllegalArgumentException("No Bundle SymbolicName set");

		Map<String, Map<String, String>> b = Processor.parseHeader(bsn, null);
		if (b.size() != 1)
			throw new IllegalArgumentException("Multiple bsn's specified " + b);

		for (String key : b.keySet()) {
			bsn = key;
			if (!Verifier.SYMBOLICNAME.matcher(bsn).matches())
				throw new IllegalArgumentException(
						"Bundle SymbolicName has wrong format: " + bsn);
		}

		String versionString = manifest.getMainAttributes().getValue(
				Analyzer.BUNDLE_VERSION);
		Version version;
		if (versionString == null)
			version = new Version();
		else
			version = new Version(versionString);

		File dir = new File(root, bsn);
		dir.mkdirs();
		String fName = bsn + "-" + version.getMajor() + "."
				+ version.getMinor() + "." + version.getMicro() + ".jar";
		File file = new File(dir, fName);

		reporter.trace("Updating " + file.getAbsolutePath());
		if (!file.exists() || file.lastModified() < jar.lastModified()) {
			jar.write(file);
			reporter.progress("Updated " + file.getAbsolutePath());
		} else {
			reporter.progress("Did not update " + jar
					+ " because repo has a newer version");
			reporter.trace("NOT Updating " + fName + " (repo is newer)");
		}

		file = new File(dir, bsn + "-latest.jar");
		if (file.exists() && file.lastModified() < jar.lastModified()) {
			jar.write(file);
		}
		return file;
	}

	public void setLocation(String string) {
		root = new File(string);
		if (!root.isDirectory())
			throw new IllegalArgumentException("Invalid repository directory");
	}

	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	public List<String> list(String regex) throws Exception {
		init();
		Instruction pattern = null;
		if (regex != null)
			pattern = Instruction.getPattern(regex);

		String list[] = root.list();
		List<String> result = new ArrayList<String>();
		if (root != null) {
			for (String f : list) {
				if (pattern == null || pattern.matches(f))
					result.add(f);
			}
		} else 
			if ( reporter != null)
				reporter.error("FileRepo root directory (%s) does not exist", root);

		return result;
	}

	public List<Version> versions(String bsn) throws Exception {
		init();
		File dir = new File(root, bsn);
		if (dir.isDirectory()) {
			String versions[] = dir.list();
			List<Version> list = new ArrayList<Version>();
			for (String v : versions) {
				Matcher m = REPO_FILE.matcher(v);
				if (m.matches()) {
					String version = m.group(2);
					if (version.equals("latest"))
						version = "99";
					list.add(new Version(version));
				}
			}
			return list;
		}
		return null;
	}

	public String toString() {
		return String
				.format("%-40s r/w=%s", root.getAbsolutePath(), canWrite());
	}

	public File getRoot() {
		return root;
	}

	public boolean refresh() {
		if (dirty) {
			dirty = false;
			return true;
		} else
			return false;
	}

	public String getName() {
		if (name == null) {
			return toString();
		}
		return name;
	}
}

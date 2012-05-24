package aQute.libg.filerepo;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import aQute.libg.version.*;

public class FileRepo {
	File	root;
	Pattern	REPO_FILE	= Pattern.compile("([-a-zA-z0-9_\\.]+)-([0-9\\.]+|latest)\\.(jar|lib)");

	public FileRepo(File root) {
		this.root = root;
	}

	/**
	 * Get a list of URLs to bundles that are constrained by the bsn and
	 * versionRange.
	 */
	public File[] get(String bsn, final VersionRange versionRange) throws Exception {

		//
		// Check if the entry exists
		//
		File f = new File(root, bsn);
		if (!f.isDirectory())
			return null;

		//
		// Iterator over all the versions for this BSN.
		// Create a sorted map over the version as key
		// and the file as URL as value. Only versions
		// that match the desired range are included in
		// this list.
		//
		return f.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				Matcher m = REPO_FILE.matcher(name);
				if (!m.matches())
					return false;
				if ( versionRange == null)
					return true;
				
				Version v = new Version(m.group(2));
				return versionRange.includes(v);
			}
		});
	}

	public List<String> list(String regex) throws Exception {
		if (regex == null)
			regex = ".*";
		final Pattern pattern = Pattern.compile(regex);

		String list[] = root.list(new FilenameFilter() {

			public boolean accept(File dir, String name) {
				Matcher matcher = pattern.matcher(name);
				return matcher.matches();
			}

		});
		return Arrays.asList(list);
	}

	public List<Version> versions(String bsn) throws Exception {
		File dir = new File(root, bsn);
		final List<Version> versions = new ArrayList<Version>();
		dir.list(new FilenameFilter() {

			public boolean accept(File dir, String name) {
				Matcher m = REPO_FILE.matcher(name);
				if (m.matches()) {
					versions.add(new Version(m.group(2)));
					return true;
				}
				return false;
			}

		});
		return versions;
	}

	public File get(String bsn, VersionRange range, int strategy) throws Exception {
		File[] files = get(bsn, range);
		if (files == null || files.length == 0)
			return null;

		if (files.length == 1)
			return files[0];

		if (strategy < 0) {
			return files[0];
		}
		return files[files.length - 1];
	}

	public File put(String bsn, Version version) {
		File dir = new File(root,bsn);
		dir.mkdirs();
		File file = new File(dir, bsn + "-" + version.getMajor() + "." + version.getMinor() + "." + version.getMicro() + ".jar");
		return file;
	}
	
}

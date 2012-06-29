package aQute.lib.deployer;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;

import aQute.lib.osgi.*;
import aQute.libg.header.*;
import aQute.libg.version.*;
import aQute.service.reporter.*;

public class FileInstallRepo extends FileRepo {

	String		group;
	boolean		dirty;
	Reporter	reporter;
	Pattern		REPO_FILE	= Pattern.compile("([-a-zA-z0-9_\\.]+)-([0-9\\.]+)\\.(jar|lib)");

	public void setProperties(Map<String,String> map) {
		super.setProperties(map);
		group = map.get("group");
	}

	public void setReporter(Reporter reporter) {
		super.setReporter(reporter);
		this.reporter = reporter;
	}

	public File put(Jar jar) throws Exception {
		dirty = true;
		Manifest manifest = jar.getManifest();
		if (manifest == null)
			throw new IllegalArgumentException("No manifest in JAR: " + jar);

		String bsn = manifest.getMainAttributes().getValue(Analyzer.BUNDLE_SYMBOLICNAME);
		if (bsn == null)
			throw new IllegalArgumentException("No Bundle SymbolicName set");

		Parameters b = Processor.parseHeader(bsn, null);
		if (b.size() != 1)
			throw new IllegalArgumentException("Multiple bsn's specified " + b);

		for (String key : b.keySet()) {
			bsn = key;
			if (!Verifier.SYMBOLICNAME.matcher(bsn).matches())
				throw new IllegalArgumentException("Bundle SymbolicName has wrong format: " + bsn);
		}

		String versionString = manifest.getMainAttributes().getValue(Analyzer.BUNDLE_VERSION);
		Version version;
		if (versionString == null)
			version = new Version();
		else
			version = new Version(versionString);

		File dir;
		if (group == null) {
			dir = getRoot();
		} else {
			dir = new File(getRoot(), group);
			dir.mkdirs();
		}
		String fName = bsn + "-" + version.getMajor() + "." + version.getMinor() + "." + version.getMicro() + ".jar";
		File file = new File(dir, fName);

		jar.write(file);
		fireBundleAdded(jar, file);

		file = new File(dir, bsn + "-latest.jar");
		if (file.isFile() && file.lastModified() < jar.lastModified()) {
			jar.write(file);
		}
		return file;
	}

	public boolean refresh() {
		if (dirty) {
			dirty = false;
			return true;
		}
		return false;
	}

	@Override
	public List<String> list(String regex) {
		Instruction pattern = null;
		if (regex != null)
			pattern = new Instruction(regex);

		String list[] = getRoot().list();
		List<String> result = new ArrayList<String>();
		for (String f : list) {
			Matcher m = REPO_FILE.matcher(f);
			if (!m.matches()) {
				continue;
			}
			String s = m.group(1);
			if (pattern == null || pattern.matches(s))
				result.add(s);
		}
		return result;
	}
}

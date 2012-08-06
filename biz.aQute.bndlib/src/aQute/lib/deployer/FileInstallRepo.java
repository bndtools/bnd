package aQute.lib.deployer;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;

import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.version.*;
import aQute.lib.io.*;
import aQute.service.reporter.*;

public class FileInstallRepo extends FileRepo {

	String		group;
	boolean		dirty;
	Reporter	reporter;
	Pattern		REPO_FILE	= Pattern.compile("([-a-zA-z0-9_\\.]+)-([0-9\\.]+)\\.(jar|lib)");

	@Override
	public void setProperties(Map<String,String> map) {
		super.setProperties(map);
		group = map.get("group");
	}

	@Override
	public void setReporter(Reporter reporter) {
		super.setReporter(reporter);
		this.reporter = reporter;
	}

	@Override
	protected PutResult putArtifact(File tmpFile, PutOptions options) throws Exception {
		assert (tmpFile != null);
		assert (options != null);

		Jar jar = null;
		try {
			init();
			dirty = true;

			jar = new Jar(tmpFile);

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

			if (reporter != null)
				reporter.trace("bsn=%s version=%s", bsn, version);

			File dir;
			if (group == null) {
				dir = getRoot();
			} else {
				dir = new File(getRoot(), group);
				if (!dir.exists() && !dir.mkdirs()) {
					throw new IOException("Could not create directory " + dir);
				}
			}
			String fName = bsn + "-" + version.getWithoutQualifier() + ".jar";
			File file = new File(dir, fName);

			PutResult result = new PutResult();

			if (reporter != null)
				reporter.trace("updating %s ", file.getAbsolutePath());

			if (file.exists()) {
				IO.delete(file);
			}
			IO.rename(tmpFile, file);
			result.artifact = file.toURI();

			if (reporter != null)
				reporter.progress(-1, "updated " + file.getAbsolutePath());

			fireBundleAdded(jar, file);

			File latest = new File(dir, bsn + "-latest.jar");
			boolean latestExists = latest.exists() && latest.isFile();
			boolean latestIsOlder = latestExists && (latest.lastModified() < jar.lastModified());
			if ((options.createLatest && !latestExists) || latestIsOlder) {
				if (latestExists) {
					IO.delete(latest);
				}
				IO.copy(file, latest);
				result.latest = latest.toURI();
			}

			return result;
		}
		finally {
			if (jar != null) {
				jar.close();
			}
		}
	}

	@Override
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

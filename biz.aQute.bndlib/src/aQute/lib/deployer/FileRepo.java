package aQute.lib.deployer;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;

import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.service.*;
import aQute.bnd.version.*;
import aQute.lib.io.*;
import aQute.service.reporter.*;

public class FileRepo implements Plugin, RepositoryPlugin, Refreshable, RegistryPlugin {
	public final static String	LOCATION	= "location";
	public final static String	READONLY	= "readonly";
	public final static String	NAME		= "name";

	File[]						EMPTY_FILES	= new File[0];
	protected File				root;
	Registry					registry;
	boolean						canWrite	= true;
	Pattern						REPO_FILE	= Pattern.compile("([-a-zA-z0-9_\\.]+)-([0-9\\.]+|latest)\\.(jar|lib)");
	Reporter					reporter;
	boolean						dirty;
	String						name;

	public FileRepo() {}

	public FileRepo(String name, File location, boolean canWrite) {
		this.name = name;
		this.root = location;
		this.canWrite = canWrite;
	}

	protected void init() throws Exception {
		// for extensions
	}

	public void setProperties(Map<String,String> map) {
		String location = map.get(LOCATION);
		if (location == null)
			throw new IllegalArgumentException("Location must be set on a FileRepo plugin");

		root = new File(location);

		String readonly = map.get(READONLY);
		if (readonly != null && Boolean.valueOf(readonly).booleanValue())
			canWrite = false;

		name = map.get(NAME);
	}

	/**
	 * Get a list of URLs to bundles that are constrained by the bsn and
	 * versionRange.
	 */
	private File[] get(String bsn, String versionRange) throws Exception {
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
		SortedMap<Version,File> versions = new TreeMap<Version,File>();
		for (int i = 0; i < instances.length; i++) {
			Matcher m = REPO_FILE.matcher(instances[i].getName());
			if (m.matches() && m.group(1).equals(bsn)) {
				String versionString = m.group(2);
				Version version;
				if (versionString.equals("latest"))
					version = new Version(Integer.MAX_VALUE);
				else
					version = new Version(versionString);

				if (range.includes(version) || versionString.equals(versionRange))
					versions.put(version, instances[i]);
			}
		}

		File[] files = versions.values().toArray(EMPTY_FILES);
		if ("latest".equals(versionRange) && files.length > 0) {
			return new File[] {
				files[files.length - 1]
			};
		}
		return files;
	}

	public boolean canWrite() {
		return canWrite;
	}

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

			File dir = new File(root, bsn);
			if (!dir.exists() && !dir.mkdirs()) {
				throw new IOException("Could not create directory " + dir);
			}
			String fName = bsn + "-" + version.getWithoutQualifier() + ".jar";
			File file = new File(dir, fName);

			boolean renamed = false;
			PutResult result = new PutResult();

			if (reporter != null)
				reporter.trace("updating %s ", file.getAbsolutePath());
			if (!file.exists() || file.lastModified() < jar.lastModified()) {
				if (file.exists()) {
					IO.delete(file);
				}
				IO.rename(tmpFile, file);
				renamed = true;
				result.artifact = file.toURI();

				if (reporter != null)
					reporter.progress(-1, "updated " + file.getAbsolutePath());

				fireBundleAdded(jar, file);
			} else {
				if (reporter != null) {
					reporter.progress(-1, "Did not update " + jar + " because repo has a newer version");
					reporter.trace("NOT Updating " + fName + " (repo is newer)");
				}
			}

			File latest = new File(dir, bsn + "-latest.jar");
			boolean latestExists = latest.exists() && latest.isFile();
			boolean latestIsOlder = latestExists && (latest.lastModified() < jar.lastModified());
			if ((options.createLatest && !latestExists) || latestIsOlder) {
				if (latestExists) {
					IO.delete(latest);
				}
				if (!renamed) {
					IO.rename(tmpFile, latest);
				} else {
					IO.copy(file, latest);
				}
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

	/* a straight copy of this method lives in LocalIndexedRepo */
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		/* both parameters are required */
		if ((stream == null) || (options == null)) {
			throw new IllegalArgumentException("No stream and/or options specified");
		}

		/* determine if the put is allowed */
		if (!canWrite) {
			throw new IOException("Repository is read-only");
		}

		/* the root directory of the repository has to be a directory */
		if (!root.isDirectory()) {
			throw new IOException("Repository directory " + root + " is not a directory");
		}

		/* determine if the artifact needs to be verified */
		boolean verifyFetch = (options.digest != null);
		boolean verifyPut = !options.allowArtifactChange;

		/* determine which digests are needed */
		boolean needFetchDigest = verifyFetch || verifyPut;
		boolean needPutDigest = verifyPut || options.generateDigest;

		/*
		 * setup a new stream that encapsulates the stream and calculates (when
		 * needed) the digest
		 */
		DigestInputStream dis = new DigestInputStream(stream, MessageDigest.getInstance("SHA-1"));
		dis.on(needFetchDigest);

		File tmpFile = null;
		try {
			/*
			 * copy the artifact from the (new/digest) stream into a temporary
			 * file in the root directory of the repository
			 */
			tmpFile = IO.createTempFile(root, "put", ".bnd");
			IO.copy(dis, tmpFile);

			/* get the digest if available */
			byte[] disDigest = needFetchDigest ? dis.getMessageDigest().digest() : null;

			/* verify the digest when requested */
			if (verifyFetch && !MessageDigest.isEqual(options.digest, disDigest)) {
				throw new IOException("Retrieved artifact digest doesn't match specified digest");
			}

			/* put the artifact into the repository (from the temporary file) */
			PutResult r = putArtifact(tmpFile, options);

			/* calculate the digest when requested */
			if (needPutDigest && (r.artifact != null)) {
				MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
				IO.copy(new File(r.artifact), sha1);
				r.digest = sha1.digest();
			}

			/* verify the artifact when requested */
			if (verifyPut && (r.digest != null) && !MessageDigest.isEqual(disDigest, r.digest)) {
				File f = new File(r.artifact);
				if (f.exists()) {
					IO.delete(f);
				}
				throw new IOException("Stored artifact digest doesn't match specified digest");
			}

			return r;
		}
		finally {
			if (tmpFile != null && tmpFile.exists()) {
				IO.delete(tmpFile);
			}
		}
	}

	protected void fireBundleAdded(Jar jar, File file) {
		if (registry == null)
			return;
		List<RepositoryListenerPlugin> listeners = registry.getPlugins(RepositoryListenerPlugin.class);
		for (RepositoryListenerPlugin listener : listeners) {
			try {
				listener.bundleAdded(this, jar, file);
			}
			catch (Exception e) {
				if (reporter != null)
					reporter.warning("Repository listener threw an unexpected exception: %s", e);
			}
		}
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
			pattern = new Instruction(regex);

		List<String> result = new ArrayList<String>();
		if (root == null) {
			if (reporter != null)
				reporter.error("FileRepo root directory is not set.");
		} else {
			File[] list = root.listFiles();
			if (list != null) {
				for (File f : list) {
					if (!f.isDirectory())
						continue; // ignore non-directories
					String fileName = f.getName();
					if (fileName.charAt(0) == '.')
						continue; // ignore hidden files
					if (pattern == null || pattern.matches(fileName))
						result.add(fileName);
				}
			} else if (reporter != null)
				reporter.error("FileRepo root directory (%s) does not exist", root);
		}

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
						version = Integer.MAX_VALUE + "";
					list.add(new Version(version));
				}
			}
			return list;
		}
		return null;
	}

	@Override
	public String toString() {
		return String.format("%-40s r/w=%s", root.getAbsolutePath(), canWrite());
	}

	public File getRoot() {
		return root;
	}

	public boolean refresh() {
		if (dirty) {
			dirty = false;
			return true;
		}
		return false;
	}

	public String getName() {
		if (name == null) {
			return toString();
		}
		return name;
	}

	public Jar get(String bsn, Version v) throws Exception {
		init();
		File bsns = new File(root, bsn);
		File version = new File(bsns, bsn + "-" + v.getMajor() + "." + v.getMinor() + "." + v.getMicro() + ".jar");
		if (version.exists())
			return new Jar(version);
		return null;
	}

	public File get(String bsn, String version, Strategy strategy, Map<String,String> properties) throws Exception {
		if (version == null)
			version = "0.0.0";

		if (strategy == Strategy.EXACT) {
			VersionRange vr = new VersionRange(version);
			if (vr.isRange())
				return null;

			if (vr.getHigh().getMajor() == Integer.MAX_VALUE)
				version = "latest";

			File file = IO.getFile(root, bsn + "/" + bsn + "-" + version + ".jar");
			if (file.isFile())
				return file;
			file = IO.getFile(root, bsn + "/" + bsn + "-" + version + ".lib");
			if (file.isFile())
				return file;
			return null;

		}
		File[] files = get(bsn, version);
		if (files == null || files.length == 0)
			return null;

		if (files.length >= 0) {
			switch (strategy) {
				case LOWEST :
					return files[0];
				case HIGHEST :
					return files[files.length - 1];
				case EXACT :
					// TODO
					break;
			}
		}
		return null;
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	public String getLocation() {
		return root.toString();
	}

}

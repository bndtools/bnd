package aQute.lib.deployer;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;

import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.service.*;
import aQute.bnd.version.*;
import aQute.lib.io.*;
import aQute.libg.command.*;
import aQute.libg.cryptography.*;
import aQute.service.reporter.*;

/**
 * A FileRepo is the primary and example implementation of a repository based on
 * a file system. It maintains its files in a bsn/bsn-version.jar style from a
 * given location. It implements all the functions of the
 * {@link RepositoryPlugin}, {@link Refreshable}, {@link Actionable}, and
 * {@link Closeable}. The FileRepo can be extended or used as is. When used as
 * is, it is possible to add shell commands to the life cycle of the FileRepo.
 * This life cycle is as follows:
 * <ul>
 * <li>{@link #CMD_INIT} - Is only executed when the location did not exist</li>
 * <li>{@link #CMD_OPEN} - Called (after init if necessary) to open it once</li>
 * <li>{@link #CMD_REFRESH} - Called when refreshed.</li>
 * <li>{@link #CMD_BEFORE_PUT} - Before the file system is changed</li>
 * <li>{@link #CMD_AFTER_PUT} - After the file system has changed, and the put
 * was a success</li>
 * <li>{@link #CMD_ABORT_PUT} - When the put is aborted.</li>
 * <li>{@link #CMD_CLOSE} - To close the repository.</li>
 * </ul>
 * Additionally, it is possible to set the {@link #CMD_SHELL} and the
 * {@link #CMD_PATH}. Notice that you can use the ${global} macro to read global
 * (that is, machine local) settings from the ~/.bnd/settings.json file (can be
 * managed with bnd).
 */
public class FileRepo implements Plugin, RepositoryPlugin, Refreshable, RegistryPlugin, Actionable, Closeable {

	/**
	 * If set, will trace to stdout. Works only if no reporter is set.
	 */
	public final static String	TRACE			= "trace";

	/**
	 * Property name for the location of the repo, must be a valid path name
	 * using forward slashes (see {@link IO#getFile(String)}.
	 */
	public final static String	LOCATION		= "location";

	/**
	 * Property name for the readonly state of the repository. If no, will
	 * read/write, otherwise it must be a boolean value read by
	 * {@link Boolean#parseBoolean(String)}. Read only repositories will not
	 * accept writes.
	 */
	public final static String	READONLY		= "readonly";

	/**
	 * Set the name of this repository (optional)
	 */
	public final static String	NAME			= "name";

	/**
	 * Path property for commands. A comma separated path for directories to be
	 * searched for command. May contain $ @} which will be replaced by the
	 * system path. If this property is not set, the system path is assumed.
	 */
	public static final String	CMD_PATH		= "cmd.path";

	/**
	 * The name ( and path) of the shell to execute the commands. By default
	 * this is sh and searched in the path.
	 */
	public static final String	CMD_SHELL		= "cmd.shell";

	/**
	 * Property for commands. The command only runs when the location does not
	 * exist. The $ @} is replaced with the location and the current work
	 * directory is also the location.
	 */
	public static final String	CMD_INIT		= "cmd.init";

	/**
	 * Property for commands. Command is run before the repo is first used. The
	 * $ @} is replaced with the location and the current work directory is also
	 * the location.
	 */
	public static final String	CMD_OPEN		= "cmd.open";

	/**
	 * Property for commands. The command runs after a put operation.
	 */
	public static final String	CMD_AFTER_PUT	= "cmd.after.put";

	/**
	 * Property for commands. The command runs when the repository is refreshed.
	 * The $ @} is replaced with the location and the current work directory is
	 * also the location.
	 */
	public static final String	CMD_REFRESH		= "cmd.refresh";

	/**
	 * Property for commands. The command runs after the file is put. The $ @}
	 * is replaced with the file name, the working directory is the location.
	 */
	public static final String	CMD_BEFORE_PUT	= "cmd.before.put";

	/**
	 * Property for commands. The command runs when a put is aborted after file
	 * changes were made. The $ @} is not set, the current working directory is
	 * the location.
	 */
	public static final String	CMD_ABORT_PUT	= "cmd.abort.put";

	/**
	 * Property for commands. The command runs after the file is put. The $ @}
	 * is not set, the current working directory is the location.
	 */
	public static final String	CMD_CLOSE		= "cmd.cose";

	/**
	 * Called before a beforeGet
	 */
	public static final String	CMD_BEFORE_GET	= "cmd.before.get";

	/**
	 * Options used when the options are null
	 */
	static final PutOptions		DEFAULTOPTIONS	= new PutOptions();

	String						shell;
	String						path;
	String						init;
	String						open;
	String						refresh;
	String						beforePut;
	String						afterPut;
	String						abortPut;
	String						beforeGet;
	String						close;

	File[]						EMPTY_FILES		= new File[0];
	protected File				root;
	Registry					registry;
	boolean						canWrite		= true;
	Pattern						REPO_FILE		= Pattern.compile("([-a-zA-z0-9_\\.]+)-([0-9\\.]+|latest)\\.(jar|lib)");
	Reporter					reporter;
	boolean						dirty;
	String						name;
	boolean						inited;
	boolean						needsInit;
	boolean						trace;

	public FileRepo() {}

	public FileRepo(String name, File location, boolean canWrite) {
		this.name = name;
		this.root = location;
		this.canWrite = canWrite;
	}

	/**
	 * Initialize the repository Subclasses should first call this method and
	 * then if it returns true, do their own initialization
	 * 
	 * @return true if initialized, false if already had been initialized.
	 * @throws Exception
	 */
	protected boolean init() throws Exception {
		if (inited)
			return false;

		inited = true;

		if (!root.isDirectory()) {
			root.mkdirs();
			if (!root.isDirectory())
				throw new IllegalArgumentException("Location cannot be turned into a directory " + root);

			exec(init, root.getAbsoluteFile());
		}

		exec(open, root.getAbsoluteFile());
		return true;
	}

	/**
	 * @see aQute.bnd.service.Plugin#setProperties(java.util.Map)
	 */
	public void setProperties(Map<String,String> map) {
		String location = map.get(LOCATION);
		if (location == null)
			throw new IllegalArgumentException("Location must be set on a FileRepo plugin");

		root = IO.getFile(IO.home, location);
		String readonly = map.get(READONLY);
		if (readonly != null && Boolean.valueOf(readonly).booleanValue())
			canWrite = false;

		name = map.get(NAME);
		path = map.get(CMD_PATH);
		shell = map.get(CMD_SHELL);
		init = map.get(CMD_INIT);
		open = map.get(CMD_OPEN);
		refresh = map.get(CMD_REFRESH);
		beforePut = map.get(CMD_BEFORE_PUT);
		abortPut = map.get(CMD_ABORT_PUT);
		afterPut = map.get(CMD_AFTER_PUT);
		beforeGet = map.get(CMD_BEFORE_GET);
		close = map.get(CMD_CLOSE);

		trace = map.get(TRACE) != null && Boolean.parseBoolean(map.get(TRACE));
	}

	/**
	 * Get a list of URLs to bundles that are constrained by the bsn and
	 * versionRange.
	 * 
	 * TODO can be removed
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

	/**
	 * Answer if this repository can write.
	 */
	public boolean canWrite() {
		return canWrite;
	}

	/**
	 * Local helper method that tries to insert a file in the repository
	 * 
	 * @param tmpFile
	 * @param options
	 * @return
	 * @throws Exception
	 */
	protected PutResult putArtifact(File tmpFile, PutOptions options) throws Exception {
		assert (tmpFile != null);
		assert (options != null);

		Jar jar = null;
		try {
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
				reporter.trace("updating %s ", file.getAbsoluteFile());

			IO.delete(file);
			IO.rename(tmpFile, file);

			renamed = true;
			result.artifact = file.toURI();

			if (reporter != null)
				reporter.progress(-1, "updated " + file.getAbsolutePath());

			fireBundleAdded(jar, file);
			exec(afterPut, file.getAbsoluteFile());

			return result;
		}
		finally {
			if (jar != null) {
				jar.close();
			}
		}
	}

	/**
	 *  a straight copy of this method lives in LocalIndexedRepo 
	 *  
	 */
	public PutResult put(InputStream stream, PutOptions options) throws Exception {

		if (options == null)
			options = DEFAULTOPTIONS;

		/* both parameters are required */
		if ((stream == null)) {
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

		init();

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

		exec(beforePut, null);

		File tmpFile = null;
		try {

			// TODO we need to lock?
			/*
			 * copy the artifact from the (new/digest) stream into a temporary
			 * file in the root directory of the repository
			 */
			tmpFile = IO.createTempFile(root, "put", ".jar");
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
		catch (Exception e) {
			exec(abortPut, null);
			throw e;
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
		root = IO.getFile(string);
		if (root.isDirectory())
			;
		needsInit = true;
		root.mkdirs();
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

	public boolean refresh() throws Exception {
		init();
		exec(refresh, null);
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

	public File get(String bsn, Version version, Map<String,String> properties) {
		File file = IO.getFile(root, bsn + "/" + bsn + "-" + version.getWithoutQualifier() + ".jar");
		if (file.isFile())
			return file;

		file = IO.getFile(root, bsn + "/" + bsn + "-" + version.getWithoutQualifier() + ".lib");
		if (file.isFile())
			return file;

		return null;
	}

	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	public String getLocation() {
		return root.toString();
	}

	public Map<String,Runnable> actions(Object... target) throws Exception {
		if (target == null || target.length == 0)
			return null; // no default actions

		try {
			String bsn = (String) target[0];
			Version version = (Version) target[1];

			final File f = get(bsn, version, null);
			if (f == null)
				return null;

			Map<String,Runnable> actions = new HashMap<String,Runnable>();
			actions.put("Delete", new Runnable() {
				public void run() {
					IO.delete(f);
				};
			});
			return actions;
		}
		catch (Exception e) {
			return null;
		}
	}

	public String tooltip(Object... target) throws Exception {
		if (target == null || target.length == 0)
			return String.format("File repository %s on location %s", getName(), root);

		try {
			String bsn = (String) target[0];
			Version version = (Version) target[1];
			File f = get(bsn, version, null);
			return String.format("%s, %s bytes, %s", f.getAbsolutePath(), f.length(), SHA1.digest(f).asHex());

		}
		catch (Exception e) {
			return null;
		}
	}

	public void close() throws IOException {
		exec(close, root.getAbsoluteFile());
	}

	/**
	 * Execute a command. Used in different stages so that the repository can be
	 * synced.
	 * 
	 * @param line
	 * @param target
	 */
	void exec(String line, File target) {
		if (line == null)
			return;

		try {
			if (target != null)
				line = line.replaceAll("\\$\\{@\\}", target.getAbsolutePath());

			System.out.println("Cmd: " + line);
			Command cmd = new Command("sh");
			cmd.inherit();
			String oldpath = cmd.var("PATH");

			if (path != null) {
				path = path.replaceAll("\\s*,\\s*", File.pathSeparator);
				path = path.replaceAll("\\$\\{@\\}", oldpath);
				cmd.var("PATH", path);
			}

			cmd.setCwd(getRoot());
			StringBuilder stdout = new StringBuilder();
			StringBuilder stderr = new StringBuilder();
			int result = cmd.execute(line, stdout, stderr);
			if (result != 0) {
				if (reporter != null)
					reporter.error("Command %s failed with %s %s %s", line, result, stdout, stderr);
				else
					throw new Exception("Command " + line + " failed " + result + " " + stdout + " " + stderr);

			}
		}
		catch (Exception e) {
			if (reporter != null)
				reporter.exception(e, e.getMessage());
			else
				throw new RuntimeException(e);
		}
	}

}

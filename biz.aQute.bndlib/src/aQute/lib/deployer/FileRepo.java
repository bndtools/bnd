package aQute.lib.deployer;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.regex.*;

import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.service.*;
import aQute.bnd.version.*;
import aQute.lib.io.*;
import aQute.libg.command.*;
import aQute.libg.cryptography.*;
import aQute.libg.reporter.*;
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

		if (reporter == null) {
			ReporterAdapter reporter = trace ? new ReporterAdapter(System.out) : new ReporterAdapter();
			reporter.setTrace(trace);
			reporter.setExceptions(trace);
			this.reporter = reporter;
		}

		if (!root.isDirectory()) {
			root.mkdirs();
			if (!root.isDirectory())
				throw new IllegalArgumentException("Location cannot be turned into a directory " + root);

			exec(init, root.getAbsolutePath());
		}
		open();
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
	 * Answer if this repository can write.
	 */
	public boolean canWrite() {
		return canWrite;
	}

	/**
	 * Local helper method that tries to insert a file in the repository. This
	 * method can be overridden but MUST not change the content of the tmpFile.
	 * This method should also create a latest version of the artifact for
	 * reference by tools like ant etc. </p> It is allowed to rename the file,
	 * the tmp file must be beneath the root directory to prevent rename
	 * problems.
	 * 
	 * @param tmpFile
	 *            source file
	 * @return a File that contains the content of the tmpFile
	 * @throws Exception
	 */
	protected File putArtifact(File tmpFile) throws Exception {
		assert (tmpFile != null);

		Jar jar = new Jar(tmpFile);
		try {
			dirty = true;

			String bsn = jar.getBsn();
			if (bsn == null)
				throw new IllegalArgumentException("No bsn set in jar: " + tmpFile);

			String versionString = jar.getVersion();
			if (versionString == null)
				versionString = "0";
			else if (!Verifier.isVersion(versionString))
				throw new IllegalArgumentException("Incorrect version in : " + tmpFile + " " + versionString);
			Version version = new Version(versionString);

			reporter.trace("bsn=%s version=%s", bsn, version);

			File dir = new File(root, bsn);
			dir.mkdirs();
			if (!dir.isDirectory())
				throw new IOException("Could not create directory " + dir);

			String fName = bsn + "-" + version.getWithoutQualifier() + ".jar";
			File file = new File(dir, fName);


			reporter.trace("updating %s ", file.getAbsolutePath());

			IO.rename(tmpFile, file);

			fireBundleAdded(jar, file);

			// TODO like to beforeGet rid of the latest option. This is only
			// used to have a constant name for the outside users (like ant)
			// we should be able to handle this differently?
			File latest = new File(dir, bsn + "-latest.jar");
			IO.copy(file, latest);

			reporter.trace("updated %s", file.getAbsolutePath());

			return file;
		}
		finally {
			jar.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see aQute.bnd.service.RepositoryPlugin#put(java.io.InputStream,
	 * aQute.bnd.service.RepositoryPlugin.PutOptions)
	 */
	public PutResult put(InputStream stream, PutOptions options) throws Exception {

		/* determine if the put is allowed */
		if (!canWrite) {
			throw new IOException("Repository is read-only");
		}

		assert stream != null;

		if (options == null)
			options = DEFAULTOPTIONS;

		init();

		/* determine if the artifact needs to be verified */
		boolean verifyPut = !options.allowArtifactChange;

		/* determine which digests are needed */
		boolean needPutDigest = verifyPut || options.generateDigest;

		/*
		 * copy the artifact from the (new/digest) stream into a temporary file
		 * in the root directory of the repository
		 */
		File tmpFile = IO.createTempFile(root, "put", ".jar");
		try {
			DigestInputStream dis = new DigestInputStream(stream, MessageDigest.getInstance("SHA-1"));
			
			try {
				
				IO.copy(dis, tmpFile);
				
				byte[] digest = dis.getMessageDigest().digest();
				
				if (options.digest != null && !Arrays.equals(digest, options.digest))
					throw new IOException("Retrieved artifact digest doesn't match specified digest");

				/*
				 * put the artifact into the repository (from the temporary
				 * file)
				 */
				beforePut(tmpFile);
				File file = putArtifact(tmpFile);
				file.setReadOnly();
				afterPut(file);

				PutResult result = new PutResult();

				/* calculate the digest when requested */
				if (needPutDigest && (result.artifact != null)) {
					MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
					IO.copy(new File(result.artifact), sha1);
					result.digest = sha1.digest();
				}
				
				/* verify the artifact when requested */
				if (verifyPut && (result.digest != null) && !MessageDigest.isEqual(digest, result.digest)) {
					File f = new File(result.artifact);
					if (f.exists()) {
						IO.delete(f);
					}
					throw new IOException("Stored artifact digest doesn't match specified digest");
				}

				result.artifact = file.toURI();

				return result;
			}
			finally {
				dis.close();
			}
		}
		catch (Exception e) {
			abortPut(tmpFile);
			throw e;
		}
		finally {
			IO.delete(tmpFile);
		}
	}

	public void setLocation(String string) {
		root = IO.getFile(string);
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

	/*
	 * (non-Javadoc)
	 * @see aQute.bnd.service.RepositoryPlugin#get(java.lang.String,
	 * aQute.bnd.version.Version, java.util.Map)
	 */
	public File get(String bsn, Version version, Map<String,String> properties) throws Exception {
		init();
		beforeGet(bsn, version);
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
		if (inited)
			exec(close, root.getAbsolutePath());
	}

	protected void open() {
		exec(open, root.getAbsolutePath());
	}

	protected void beforePut(File tmp) {
		exec(beforePut, root.getAbsolutePath(), tmp.getAbsolutePath());
	}

	protected void afterPut(File file) {
		exec(afterPut, root.getAbsolutePath(), file.getAbsolutePath());
	}

	protected void abortPut(File tmpFile) {
		exec(abortPut, root.getAbsolutePath(), tmpFile.getAbsolutePath());
	}

	protected void beforeGet(String bsn, Version version) {
		exec(beforeGet, root.getAbsolutePath(), bsn, version);
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

	/**
	 * Execute a command. Used in different stages so that the repository can be
	 * synced with external tools.
	 * 
	 * @param line
	 * @param target
	 */
	void exec(String line, Object... args) {
		if (line == null)
			return;

		try {
			if (args != null)
				for (int i = 0; i < args.length; i++) {
					if (i == 0)
						line = line.replaceAll("\\$\\{@\\}", args[i].toString());
					line = line.replaceAll("\\$\\{" + i + "\\}", args[i].toString());
				}

			if (shell == null) {
				shell = System.getProperty("os.name").toLowerCase().indexOf("win") > 0 ? "cmd.exe" : "sh";
			}
			Command cmd = new Command(shell);

			if (path != null) {
				cmd.inherit();
				String oldpath = cmd.var("PATH");
				path = path.replaceAll("\\s*,\\s*", File.pathSeparator);
				path = path.replaceAll("\\$\\{@\\}", oldpath);
				cmd.var("PATH", path);
			}

			cmd.setCwd(getRoot());
			StringBuilder stdout = new StringBuilder();
			StringBuilder stderr = new StringBuilder();
			int result = cmd.execute(line, stdout, stderr);
			if (result != 0) {
				reporter.error("Command %s failed with %s %s %s", line, result, stdout, stderr);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			reporter.exception(e, e.getMessage());
		}
	}

}

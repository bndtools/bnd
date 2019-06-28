package aQute.lib.deployer;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Refreshable;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.RepositoryListenerPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.Version;
import aQute.lib.collections.SortedList;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import aQute.lib.persistentmap.PersistentMap;
import aQute.libg.command.Command;
import aQute.libg.cryptography.SHA1;
import aQute.libg.cryptography.SHA256;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;

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
 * <li>{@link #CMD_BEFORE_GET} - Before the file is gotten</li>
 * <li>{@link #CMD_AFTER_ACTION} - Before the file is gotten</li>
 * <li>{@link #CMD_CLOSE} - When the repo is closed and no more actions will
 * take place</li> was a success</li>
 * <li>{@link #CMD_ABORT_PUT} - When the put is aborted.</li>
 * <li>{@link #CMD_CLOSE} - To close the repository.</li>
 * </ul>
 * Additionally, it is possible to set the {@link #CMD_SHELL} and the
 * {@link #CMD_PATH}. Notice that you can use the ${global} macro to read global
 * (that is, machine local) settings from the ~/.bnd/settings.json file (can be
 * managed with bnd).
 */

@aQute.bnd.annotation.plugin.BndPlugin(name = "filerepo", parameters = FileRepo.Config.class)
public class FileRepo implements Plugin, RepositoryPlugin, Refreshable, RegistryPlugin, Actionable, Closeable {
	private final static Logger logger = LoggerFactory.getLogger(FileRepo.class);

	interface Config {
		String name();

		String location();

		boolean readonly();

		boolean trace();

		boolean index();

		String cmd_path();

		String cmd_shell();

		String cmd_init();

		String cmd_open();

		String cmd_after_put();

		String cmd_before_put();

		String cmd_abort_put();

		String cmd_before_get();

		String cmd_after_action();

		String cmd_refresh();

		String cmd_close();
	}

	/**
	 * If set, will trace to stdout. Works only if no reporter is set.
	 */
	public final static String				TRACE				= "trace";

	/**
	 * Property name for the location of the repo, must be a valid path name
	 * using forward slashes (see {@link IO#getFile(String)}.
	 */
	public final static String				LOCATION			= "location";

	/**
	 * Property name for the readonly state of the repository. If no, will
	 * read/write, otherwise it must be a boolean value read by
	 * {@link Boolean#parseBoolean(String)}. Read only repositories will not
	 * accept writes. Defaults to false.
	 */
	public final static String				READONLY			= "readonly";

	/**
	 * Property name for the latest option of the repository. If true, will copy
	 * the put jar to a 'latest' file (option must be a boolean value read by
	 * {@link Boolean#parseBoolean(String)}). Defaults to true.
	 */
	public final static String				LATEST_OPTION		= "latest";

	/**
	 * Set the name of this repository (optional)
	 */
	public final static String				NAME				= "name";

	/**
	 * Should this file repo have an index? Either true or false (absent)
	 */
	public final static String				INDEX				= "index";

	/**
	 * Path property for commands. A comma separated path for directories to be
	 * searched for command. May contain $ @} which will be replaced by the
	 * system path. If this property is not set, the system path is assumed.
	 */
	public static final String				CMD_PATH			= "cmd.path";

	/**
	 * The name ( and path) of the shell to execute the commands. By default
	 * this is sh and searched in the path.
	 */
	public static final String				CMD_SHELL			= "cmd.shell";

	/**
	 * Property for commands. The command only runs when the location does not
	 * exist.
	 * </p>
	 */
	public static final String				CMD_INIT			= "cmd.init";

	/**
	 * Property for commands. Command is run before the repo is first used.
	 * </p>
	 */
	public static final String				CMD_OPEN			= "cmd.open";

	/**
	 * Property for commands. The command runs after a put operation.
	 * </p>
	 */
	public static final String				CMD_AFTER_PUT		= "cmd.after.put";

	/**
	 * Property for commands. The command runs when the repository is refreshed.
	 * </p>
	 */
	public static final String				CMD_REFRESH			= "cmd.refresh";

	/**
	 * Property for commands. The command runs after the file is put.
	 * </p>
	 */
	public static final String				CMD_BEFORE_PUT		= "cmd.before.put";

	/**
	 * Property for commands. The command runs when a put is aborted after file
	 * changes were made.
	 * </p>
	 */
	public static final String				CMD_ABORT_PUT		= "cmd.abort.put";

	/**
	 * Property for commands. The command runs after the file is put.
	 * </p>
	 */
	public static final String				CMD_CLOSE			= "cmd.close";

	/**
	 * Property for commands. Will be run after an action has been executed.
	 * </p>
	 */
	public static final String				CMD_AFTER_ACTION	= "cmd.after.action";

	/**
	 * Called before a before get.
	 */
	public static final String				CMD_BEFORE_GET		= "cmd.before.get";

	/**
	 * Options used when the options are null
	 */
	static final PutOptions					DEFAULTOPTIONS		= new PutOptions();

	public static final int					MAX_MAJOR			= 999999999;

	private static final String				LATEST_POSTFIX		= "-" + Constants.VERSION_ATTR_LATEST + ".jar";
	public static final Version				LATEST_VERSION		= new Version(MAX_MAJOR, 0, 0);
	private static final SortedSet<Version>	LATEST_SET			= new TreeSet<>(Collections.singleton(LATEST_VERSION));

	final static JSONCodec					codec				= new JSONCodec();
	String									shell;
	String									path;
	String									init;
	String									open;
	String									refresh;
	String									beforePut;
	String									afterPut;
	String									abortPut;
	String									beforeGet;
	String									close;
	String									action;

	File[]									EMPTY_FILES			= new File[0];
	protected File							root;
	Registry								registry;
	boolean									createLatest		= true;
	boolean									canWrite			= true;
	private final static Pattern			REPO_FILE			= Pattern
		.compile("(?:([-.\\w]+)-)(" + Version.VERSION_STRING + "|" + Constants.VERSION_ATTR_LATEST + ")\\.(jar|lib)");
	Reporter								reporter;
	boolean									dirty				= true;
	String									name;
	boolean									inited;
	boolean									trace;
	PersistentMap<ResourceDescriptor>		index;

	private boolean							hasIndex;

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
		logger.debug("init");
		if (!root.isDirectory()) {
			IO.mkdirs(root);
			if (!root.isDirectory())
				throw new IllegalArgumentException("Location cannot be turned into a directory " + root);

			exec(init, IO.absolutePath(root));
		}

		if (hasIndex)
			index = new PersistentMap<>(new File(root, ".index"), ResourceDescriptor.class);

		open();
		return true;
	}

	/**
	 * @see aQute.bnd.service.Plugin#setProperties(java.util.Map)
	 */
	@Override
	public void setProperties(Map<String, String> map) {
		String location = map.get(LOCATION);
		if (location == null)
			throw new IllegalArgumentException("Location must be set on a FileRepo plugin");

		root = IO.getFile(IO.home, location);

		String readonly = map.get(READONLY);
		if (readonly != null)
			canWrite = !Boolean.valueOf(readonly)
				.booleanValue();

		String createLatest = map.get(LATEST_OPTION);
		if (createLatest != null)
			this.createLatest = Boolean.valueOf(createLatest)
				.booleanValue();

		hasIndex = Processor.isTrue(map.get(INDEX));
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
		action = map.get(CMD_AFTER_ACTION);

		trace = map.get(TRACE) != null && Boolean.parseBoolean(map.get(TRACE));
	}

	/**
	 * Answer if this repository can write.
	 */
	@Override
	public boolean canWrite() {
		return canWrite;
	}

	/**
	 * Local helper method that tries to insert a file in the repository. This
	 * method can be overridden but MUST not change the content of the tmpFile.
	 * This method should also create a latest version of the artifact for
	 * reference by tools like ant etc.
	 * </p>
	 * It is allowed to rename the file, the tmp file must be beneath the root
	 * directory to prevent rename problems.
	 *
	 * @param tmpFile source file
	 * @param digest
	 * @return a File that contains the content of the tmpFile
	 * @throws Exception
	 */
	protected File putArtifact(File tmpFile, byte[] digest) throws Exception {
		return putArtifact(tmpFile, null, digest);
	}

	protected File putArtifact(File tmpFile, PutOptions options, byte[] digest) throws Exception {
		assert (tmpFile != null);

		try (Jar tmpJar = new Jar(tmpFile)) {
			String bsn = null;
			if (options != null && options.bsn != null) {
				bsn = options.bsn;
			} else {
				bsn = tmpJar.getBsn();
			}

			if (bsn == null)
				throw new IllegalArgumentException("No bsn set in jar: " + tmpFile);

			Version version = null;
			if (options != null && options.version != null) {
				version = options.version;
			} else {
				try {
					version = new Version(tmpJar.getVersion());
				} catch (Exception e) {
					throw new IllegalArgumentException("Incorrect version in : " + tmpFile + " " + tmpJar.getVersion());
				}
			}

			if (version == null) {
				/*
				 * should not happen because bsn != null, which mean that the
				 * jar is valid and it has a manifest. just to be safe though
				 */
				version = Version.LOWEST;
			}

			logger.debug("bsn={} version={}", bsn, version);

			File dir = new File(root, bsn);
			IO.mkdirs(dir);
			if (!dir.isDirectory())
				throw new IOException("Could not create directory " + dir);

			String fName = bsn + "-" + version.toStringWithoutQualifier() + ".jar";
			File file = new File(dir, fName);

			logger.debug("updating {}", file.getAbsolutePath());

			if (hasIndex)
				index.put(bsn + "-" + version.toStringWithoutQualifier(),
					buildDescriptor(tmpFile, tmpJar, digest, bsn, version));

			// An open jar on file will fail rename on windows
			tmpJar.close();

			dirty = true;
			if (file.isFile() && !file.canWrite()) {
				// older versions of this class made file readonly
				file.setWritable(true);
			}
			IO.rename(tmpFile, file);

			fireBundleAdded(file);
			afterPut(file, bsn, version, Hex.toHexString(digest));

			if (createLatest) {
				File latest = new File(dir, bsn + LATEST_POSTFIX);
				IO.copy(file, latest);
			}

			logger.debug("updated {}", file.getAbsolutePath());

			return file;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see aQute.bnd.service.RepositoryPlugin#put(java.io.InputStream,
	 * aQute.bnd.service.RepositoryPlugin.PutOptions)
	 */
	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		/* determine if the put is allowed */
		if (!canWrite) {
			throw new IOException("Repository is read-only");
		}

		assert stream != null;

		if (options == null)
			options = DEFAULTOPTIONS;

		init();

		/*
		 * copy the artifact from the (new/digest) stream into a temporary file
		 * in the root directory of the repository
		 */
		File tmpFile = IO.createTempFile(root, "put", ".jar");
		try (DigestInputStream dis = new DigestInputStream(stream, MessageDigest.getInstance("SHA-1"))) {
			IO.copy(dis, tmpFile);

			byte[] digest = dis.getMessageDigest()
				.digest();

			if (options.digest != null && !Arrays.equals(digest, options.digest))
				throw new IOException("Retrieved artifact digest doesn't match specified digest");

			/*
			 * put the artifact into the repository (from the temporary file)
			 */
			beforePut(tmpFile);
			File file = putArtifact(tmpFile, options, digest);

			PutResult result = new PutResult();
			result.digest = digest;
			result.artifact = file.toURI();

			return result;
		} catch (Exception e) {
			abortPut(tmpFile);
			throw e;
		} finally {
			IO.delete(tmpFile);
		}
	}

	public void setLocation(String string) {
		root = IO.getFile(string);
	}

	@Override
	public void setReporter(Reporter reporter) {
		this.reporter = reporter;
	}

	@Override
	public List<String> list(String regex) throws Exception {
		init();
		Instruction pattern = null;
		if (regex != null)
			pattern = new Instruction(regex);

		List<String> result = new ArrayList<>();
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

	@Override
	public SortedSet<Version> versions(String bsn) throws Exception {
		init();
		File dir = new File(root, bsn);
		boolean latest = false;
		if (dir.isDirectory()) {
			String versions[] = dir.list();
			List<Version> list = new ArrayList<>();
			for (String v : versions) {
				Matcher m = REPO_FILE.matcher(v);
				if (m.matches()) {
					String version = m.group(2);
					if (!version.equals(Constants.VERSION_ATTR_LATEST))
						list.add(new Version(version));
					else
						latest = true;
				}
			}
			if (list.isEmpty() && latest)
				return LATEST_SET;
			else
				return new SortedList<>(list);
		}
		return SortedList.empty();
	}

	@Override
	public String toString() {
		return String.format("%s [%-40s r/w=%s]", getName(), IO.absolutePath(getRoot()), canWrite());
	}

	@Override
	public File getRoot() {
		return root;
	}

	@Override
	public boolean refresh() throws Exception {
		init();
		exec(refresh, root);
		rebuildIndex();
		return true;
	}

	@Override
	public String getName() {
		if (name == null) {
			return getLocation();
		}
		return name;
	}

	/*
	 * (non-Javadoc)
	 * @see aQute.bnd.service.RepositoryPlugin#get(java.lang.String,
	 * aQute.bnd.version.Version, java.util.Map)
	 */
	@Override
	public File get(String bsn, Version version, Map<String, String> properties, DownloadListener... listeners)
		throws Exception {
		init();
		beforeGet(bsn, version);
		File file = getLocal(bsn, version, properties);
		if (file.exists()) {
			for (DownloadListener l : listeners) {
				try {
					l.success(file);
				} catch (Exception e) {
					reporter.exception(e, "Download listener for %s", file);
				}
			}
			return file;
		}
		return null;
	}

	@Override
	public void setRegistry(Registry registry) {
		this.registry = registry;
	}

	@Override
	public String getLocation() {
		return root.toString();
	}

	@Override
	public Map<String, Runnable> actions(Object... target) throws Exception {
		if (target == null || target.length == 0) {
			Map<String, Runnable> actions = new LinkedHashMap<>();
			actions.put("Rebuild Resource Index", () -> {
				try {
					refresh();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			return actions; // no default actions
		}

		try {
			String bsn = (String) target[0];
			Version version = (Version) target[1];

			final File f = get(bsn, version, null);
			if (f == null)
				return null;

			Map<String, Runnable> actions = new HashMap<>();
			actions.put("Delete " + bsn + "-" + status(bsn, version), () -> {
				IO.delete(f);
				if (f.getParentFile()
					.list().length == 0)
					IO.delete(f.getParentFile());
				afterAction(f, "delete");
			});
			return actions;
		} catch (Exception e) {
			return null;
		}
	}

	protected void afterAction(File f, String key) {
		exec(action, root, f, key);
	}

	/*
	 * (non-Javadoc)
	 * @see aQute.bnd.service.Actionable#tooltip(java.lang.Object[])
	 */
	@Override
	@SuppressWarnings("unchecked")
	public String tooltip(Object... target) throws Exception {
		if (target == null || target.length == 0)
			return String.format("%s\n%s", getName(), root);

		try {
			String bsn = (String) target[0];
			Version version = (Version) target[1];
			Map<String, String> map = null;
			if (target.length > 2)
				map = (Map<String, String>) target[2];

			File f = getLocal(bsn, version, map);

			String s = "";
			ResourceDescriptor descriptor = getDescriptor(bsn, version);
			if (descriptor != null && descriptor.description != null) {
				s = descriptor.description + "\n";
			}

			s += String.format("Path: %s\nSize: %s\nSHA1: %s", IO.absolutePath(f), readable(f.length(), 0),
				SHA1.digest(f)
					.asHex());
			if (f.getName()
				.endsWith(".lib") && f.isFile()) {
				s += "\n" + IO.collect(f);
			}
			return s;

		} catch (Exception e) {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see aQute.bnd.service.Actionable#title(java.lang.Object[])
	 */
	@Override
	public String title(Object... target) throws Exception {
		if (target == null || target.length == 0)
			return getName();

		if (target.length == 1 && target[0] instanceof String)
			return (String) target[0];

		if (target.length == 2 && target[0] instanceof String && target[1] instanceof Version) {
			return status((String) target[0], (Version) target[1]);
		}

		return null;
	}

	protected File getLocal(String bsn, Version version, Map<String, String> properties) {
		File dir = new File(root, bsn);

		if (LATEST_VERSION.equals(version)) {
			File fjar = new File(dir, bsn + LATEST_POSTFIX);
			if (fjar.isFile())
				return fjar.getAbsoluteFile();
		}

		File fjar = new File(dir, bsn + "-" + version.toStringWithoutQualifier() + ".jar");
		if (fjar.isFile())
			return fjar.getAbsoluteFile();

		File sfjar = new File(dir, version.toStringWithoutQualifier() + ".jar");
		if (sfjar.isFile())
			return sfjar.getAbsoluteFile();

		File flib = new File(dir, bsn + "-" + version.toStringWithoutQualifier() + ".lib");
		if (flib.isFile())
			return flib.getAbsoluteFile();

		File sflib = new File(dir, version.toStringWithoutQualifier() + ".lib");
		if (sflib.isFile())
			return sflib.getAbsoluteFile();

		return fjar.getAbsoluteFile();
	}

	protected String status(String bsn, Version version) {
		File file = getLocal(bsn, version, null);

		String vs;
		if (LATEST_VERSION.equals(version)) {
			vs = Constants.VERSION_ATTR_LATEST;
		} else {
			vs = version.toString();
		}
		StringBuilder sb = new StringBuilder(vs);
		String del = " [";

		if (file.getName()
			.endsWith(".lib")) {
			sb.append(del)
				.append("L");
			del = "";
		} else if (!file.getName()
			.endsWith(".jar")) {
			sb.append(del)
				.append("?");
			del = "";
		}
		if (!file.isFile()) {
			sb.append(del)
				.append("X");
			del = "";
		}
		if (file.length() == 0) {
			sb.append(del)
				.append("0");
			del = "";
		}
		if (del.equals(""))
			sb.append("]");
		return sb.toString();
	}

	private static String[] names = {
		"bytes", "Kb", "Mb", "Gb"
	};

	private Object readable(long length, int n) {
		if (length < 0)
			return "<invalid>";

		if (length < 1024 || n >= names.length)
			return length + names[n];

		return readable(length / 1024, n + 1);
	}

	@Override
	public void close() throws IOException {
		if (inited) {
			exec(close, IO.absolutePath(getRoot()));
			if (hasIndex)
				index.close();
		}
	}

	protected void open() {
		exec(open, IO.absolutePath(getRoot()));
	}

	protected void beforePut(File tmp) {
		exec(beforePut, IO.absolutePath(getRoot()), IO.absolutePath(tmp));
	}

	protected void afterPut(File file, String bsn, Version version, String sha) {
		exec(afterPut, IO.absolutePath(getRoot()), IO.absolutePath(file), sha);
	}

	protected void abortPut(File tmpFile) {
		exec(abortPut, IO.absolutePath(getRoot()), IO.absolutePath(tmpFile));
	}

	protected void beforeGet(String bsn, Version version) {
		exec(beforeGet, IO.absolutePath(getRoot()), bsn, version);
	}

	protected void fireBundleAdded(File file) throws Exception {
		if (registry == null)
			return;
		List<RepositoryListenerPlugin> listeners = registry.getPlugins(RepositoryListenerPlugin.class);
		if (listeners.isEmpty())
			return;

		try (Jar jar = new Jar(file)) {
			for (RepositoryListenerPlugin listener : listeners) {
				try {
					listener.bundleAdded(this, jar, file);
				} catch (Exception e) {
					if (reporter != null)
						reporter.warning("Repository listener threw an unexpected exception: %s", e);
				}
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
		if (line == null) {

			logger.debug("Line is empty, args={}", args == null ? new Object[0] : args);

			return;
		}

		logger.debug("exec {}", line);

		try {
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					if (i == 0) {
						// replaceAll backslash magic ensures windows paths
						// remain intact
						line = line.replaceAll("\\$\\{@\\}", args[0].toString()
							.replaceAll("\\\\", "\\\\\\\\"));
					}
					// replaceAll backslash magic ensures windows paths remain
					// intact
					line = line.replaceAll("\\$" + i, args[i].toString()
						.replaceAll("\\\\", "\\\\\\\\"));
				}
			}
			// purge remaining placeholders
			line = line.replaceAll("\\s*\\$[0-9]\\s*", "");

			int result = 0;
			StringBuilder stdout = new StringBuilder();
			StringBuilder stderr = new StringBuilder();
			if (System.getProperty("os.name")
				.toLowerCase()
				.contains("win")) {

				// FIXME ignoring possible shell setting stdin approach used
				// below does not work in windows
				Command cmd = new Command("cmd.exe /C " + line);
				cmd.setCwd(getRoot());
				result = cmd.execute(stdout, stderr);

			} else {
				if (shell == null) {
					shell = "sh";
				}
				Command cmd = new Command(shell);
				cmd.setCwd(getRoot());

				if (path != null) {
					cmd.inherit();
					String oldpath = cmd.var("PATH");
					path = path.replaceAll("\\s*,\\s*", File.pathSeparator);
					path = path.replaceAll("\\$\\{@\\}", oldpath);
					cmd.var("PATH", path);
				}
				result = cmd.execute(line, stdout, stderr);
			}
			if (result != 0) {
				reporter.error("Command %s failed with %s %s %s", line, result, stdout, stderr);
			}
		} catch (Exception e) {
			e.printStackTrace();
			reporter.exception(e, "%s", e);
		}
	}

	/*
	 * 8 Set the root directory directly
	 */
	public void setDir(File repoDir) {
		this.root = repoDir;
	}

	/**
	 * Delete an entry from the repository and cleanup the directory
	 *
	 * @param bsn
	 * @param version
	 * @throws Exception
	 */
	public void delete(String bsn, Version version) throws Exception {
		init();
		assert bsn != null;

		SortedSet<Version> versions;
		if (version == null)
			versions = versions(bsn);
		else
			versions = new SortedList<>(version);

		for (Version v : versions) {
			File f = getLocal(bsn, version, null);
			if (!f.isFile())
				reporter.error("No artifact found for %s:%s", bsn, version);
			else
				IO.delete(f);
		}
		if (versions(bsn).isEmpty())
			IO.delete(new File(root, bsn));

		index.remove(bsn + "-" + version);
	}

	public ResourceDescriptor getDescriptor(String bsn, Version version) throws Exception {
		init();
		if (hasIndex) {
			ResourceDescriptor resourceDescriptor = index.get(bsn + "-" + version);
			if (resourceDescriptor == null)
				System.out.println("Keys " + index.keySet());
			return resourceDescriptor;
		}
		return null;
	}

	public SortedSet<ResourceDescriptor> getResources() throws Exception {
		init();
		if (hasIndex) {
			TreeSet<ResourceDescriptor> resources = new TreeSet<>((a, b) -> {
				if (a == b)
					return 0;

				int r = a.bsn.compareTo(b.bsn);
				if (r != 0)
					return r;

				if (a.version != b.version) {
					if (a.version == null)
						return 1;
					if (b.version == null)
						return -1;

					r = a.version.compareTo(b.version);
					if (r != 0)
						return r;
				}
				if (a.id.length > b.id.length)
					return 1;
				if (a.id.length < b.id.length)
					return -1;

				for (int i = 0; i < a.id.length; i++) {
					if (a.id[i] > b.id[i])
						return 1;
					if (a.id[i] < b.id[i])
						return 1;
				}
				return 0;
			});
			for (ResourceDescriptor rd : index.values()) {
				resources.add(rd);
			}
			return resources;
		}
		return null;
	}

	public ResourceDescriptor getResource(byte[] sha) throws Exception {
		init();
		if (hasIndex) {
			for (ResourceDescriptor rd : index.values()) {
				if (Arrays.equals(rd.id, sha))
					return rd;
			}
		}
		return null;
	}

	void rebuildIndex() throws Exception {
		init();
		if (!hasIndex || !dirty)
			return;

		index.clear();
		for (String bsn : list(null)) {
			for (Version version : versions(bsn)) {
				File f = get(bsn, version, null);
				index.put(bsn + "-" + version, buildDescriptor(f, null, null, bsn, version));
			}
		}
		dirty = false;
	}

	private ResourceDescriptor buildDescriptor(File f, Jar jar, byte[] digest, String bsn, Version version)
		throws NoSuchAlgorithmException, Exception {
		init();
		Jar tmpjar = jar;
		if (jar == null)
			tmpjar = new Jar(f);
		try {
			Manifest m = tmpjar.getManifest();
			ResourceDescriptor rd = new ResourceDescriptor();
			rd.bsn = bsn;
			rd.version = version;
			rd.description = m.getMainAttributes()
				.getValue(Constants.BUNDLE_DESCRIPTION);
			rd.id = digest;
			if (rd.id == null)
				rd.id = SHA1.digest(f)
					.digest();
			rd.sha256 = SHA256.digest(f)
				.digest();
			rd.url = f.toURI();
			return rd;
		} finally {
			if (tmpjar != null)
				tmpjar.close();
		}
	}

	public void setIndex(boolean b) {
		hasIndex = b;
	}
}

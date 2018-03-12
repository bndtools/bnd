package aQute.bnd.osgi;

import static aQute.libg.slf4j.GradleLogging.LIFECYCLE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryDonePlugin;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.url.URLConnectionHandler;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.collections.Iterables;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.io.IOConstants;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.cryptography.SHA1;
import aQute.libg.generics.Create;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;

public class Processor extends Domain implements Reporter, Registry, Constants, Closeable {
	private static final Logger	logger	= LoggerFactory.getLogger(Processor.class);
	public static Reporter log;;

	static {
		ReporterAdapter reporterAdapter = new ReporterAdapter(System.out);
		reporterAdapter.setTrace(true);
		reporterAdapter.setExceptions(true);
		reporterAdapter.setPedantic(true);
		log = reporterAdapter;
	}

	static final int								BUFFER_SIZE			= IOConstants.PAGE_SIZE * 1;

	static Pattern									PACKAGES_IGNORED	= Pattern
			.compile("(java\\.lang\\.reflect|sun\\.reflect).*");

	static ThreadLocal<Processor>					current				= new ThreadLocal<>();
	private final static ScheduledExecutorService	sheduledExecutor;
	private final static ExecutorService			executor;
	static {
		ThreadFactory threadFactory = Executors.defaultThreadFactory();
		executor = new ThreadPoolExecutor(0, 64, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactory,
				new RejectedExecutionHandler() {
					/*
					 * We are stealing another's thread because we have hit max
					 * pool size, so we cannot let the runnable's exception
					 * propagate back up this thread.
					 */
					@Override
					public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
						if (executor.isShutdown()) {
							return;
						}
						try {
							r.run();
						} catch (Throwable t) {
							try {
								Thread thread = Thread.currentThread();
								thread.getUncaughtExceptionHandler().uncaughtException(thread, t);
							} catch (Throwable for_real) {
								// we will ignore this
							}
						}
					}
				});
		sheduledExecutor = new ScheduledThreadPoolExecutor(4, threadFactory);
	}
	static Random					random			= new Random();
	// TODO handle include files out of date
	public final static String		LIST_SPLITTER	= "\\s*,\\s*";
	private final static Pattern				LIST_SPLITTER_PATTERN	= Pattern.compile(LIST_SPLITTER);
	final List<String>				errors			= new ArrayList<>();
	final List<String>				warnings		= new ArrayList<>();
	final Set<Object>				basicPlugins	= new HashSet<>();
	private final Set<Closeable>	toBeClosed		= new HashSet<>();
	private Set<Object>				plugins;

	boolean							pedantic;
	boolean							trace;
	boolean							exceptions;
	boolean							fileMustExist	= true;

	private File					base			= new File("").getAbsoluteFile();
	private URI						baseURI			= base.toURI();

	Properties						properties;
	String							profile;
	private Macro					replacer;
	private long					lastModified;
	private File					propertiesFile;
	private boolean					fixup			= true;
	long							modified;
	Processor						parent;
	private final CopyOnWriteArrayList<File>	included		= new CopyOnWriteArrayList<>();

	CL								pluginLoader;
	Collection<String>				filter;
	HashSet<String>					missingCommand;
	Boolean							strict;
	boolean							fixupMessages;

	public static class FileLine {
		public static final FileLine	DUMMY	= new FileLine(null, 0, 0);
		public File						file;
		public int						line;
		public int						length;
		public int						start;
		public int						end;

		public FileLine() {

		}

		public FileLine(File file, int line, int length) {
			this.file = file;
			this.line = line;
			this.length = length;

		}

		public void set(SetLocation sl) {
			sl.file(file.getAbsolutePath());
			sl.line(line);
			sl.length(length);
		}
	}

	public Processor() {
		properties = new UTF8Properties();
	}

	public Processor(Properties parent) {
		properties = new UTF8Properties(parent);
	}

	public Processor(Processor processor) {
		this(processor.getProperties0());
		this.parent = processor;
	}

	public Processor(Properties props, boolean copy) {
		if (copy)
			properties = new UTF8Properties(props);
		else
			properties = props;
	}

	public void setParent(Processor processor) {
		this.parent = processor;
		Properties updated = new UTF8Properties(processor.getProperties0());
		updated.putAll(getProperties0());
		properties = updated;
	}

	public Processor getParent() {
		return parent;
	}

	public Processor getTop() {
		if (parent == null)
			return this;
		return parent.getTop();
	}

	public void getInfo(Reporter processor, String prefix) {
		if (prefix == null)
			prefix = getBase() + " :";
		if (isFailOk())
			addAll(warnings, processor.getErrors(), prefix, processor);
		else
			addAll(errors, processor.getErrors(), prefix, processor);
		addAll(warnings, processor.getWarnings(), prefix, processor);

		processor.getErrors().clear();
		processor.getWarnings().clear();

	}

	public void getInfo(Reporter processor) {
		getInfo(processor, "");
	}

	private void addAll(List<String> to, List<String> from, String prefix, Reporter reporter) {
		try {
			for (String message : from) {
				String newMessage = prefix.isEmpty() ? message : prefix + message;
				to.add(newMessage);

				Location location = reporter.getLocation(message);
				if (location != null) {
					SetLocation newer = location(newMessage);
					for (Field f : newer.getClass().getFields()) {
						if (!"message".equals(f.getName())) {
							f.set(newer, f.get(location));
						}
					}
				}
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * A processor can mark itself current for a thread.
	 */
	private Processor current() {
		Processor p = current.get();
		if (p == null)
			return this;
		return p;
	}

	public SetLocation warning(String string, Object... args) {
		fixupMessages = false;
		Processor p = current();
		String s = formatArrays(string, args);
		if (!p.warnings.contains(s))
			p.warnings.add(s);
		p.signal();
		return location(s);
	}

	public SetLocation error(String string, Object... args) {
		fixupMessages = false;
		Processor p = current();
		try {
			if (p.isFailOk())
				return p.warning(string, args);
			String s = formatArrays(string, args);
			if (!p.errors.contains(s))
				p.errors.add(s);
			return location(s);
		} finally {
			p.signal();
		}
	}

	/**
	 * @deprecated Use SLF4J
	 *             Logger.info(aQute.libg.slf4j.GradleLogging.LIFECYCLE)
	 *             instead.
	 */
	@Deprecated
	public void progress(float progress, String format, Object... args) {
		Logger l = getLogger();
		if (l.isInfoEnabled(LIFECYCLE)) {
			String message = formatArrays(format, args);
			if (progress > 0)
				l.info(LIFECYCLE, "[{}] {}", (int) progress, message);
			else
				l.info(LIFECYCLE, "{}", message);
		}
	}

	public void progress(String format, Object... args) {
		progress(-1f, format, args);
	}

	public SetLocation error(String format, Throwable t, Object... args) {
		return exception(t, format, args);
	}

	@Override
	public SetLocation exception(Throwable t, String format, Object... args) {
		Processor p = current();
		if (p.trace) {
			p.getLogger().info("Reported exception", t);
		} else {
			p.getLogger().debug("Reported exception", t);
		}
		if (p.exceptions) {
			printExceptionSummary(t, System.err);
		}
		// unwrap InvocationTargetException
		while ((t instanceof InvocationTargetException) && (t.getCause() != null)) {
			t = t.getCause();
		}
		String s = formatArrays("Exception: %s", Exceptions.toString(t));
		if (p.isFailOk()) {
			p.warnings.add(s);
		} else {
			p.errors.add(s);
		}
		return error(format, args);
	}

	public int printExceptionSummary(Throwable e, PrintStream out) {
		if (e == null) {
			return 0;
		}
		int count = 10;
		int n = printExceptionSummary(e.getCause(), out);

		if (n == 0) {
			out.println("Root cause: " + e.getMessage() + "   :" + e.getClass().getName());
			count = Integer.MAX_VALUE;
		} else {
			out.println("Rethrown from: " + e.toString());
		}
		out.println();
		printStackTrace(e, count, out);
		System.err.println();
		return n + 1;
	}

	public void printStackTrace(Throwable e, int count, PrintStream out) {
		e.printStackTrace(out);
	}

	public void signal() {}

	public List<String> getWarnings() {
		fixupMessages();
		return warnings;
	}

	public List<String> getErrors() {
		fixupMessages();
		return errors;
	}

	/**
	 * Standard OSGi header parser.
	 *
	 * @param value
	 */
	static public Parameters parseHeader(String value, Processor logger) {
		return new Parameters(value, logger);
	}

	public Parameters parseHeader(String value) {
		return new Parameters(value, this);
	}

	public void addClose(Closeable jar) {
		assert jar != null;
		toBeClosed.add(jar);
	}

	public void removeClose(Closeable jar) {
		assert jar != null;
		toBeClosed.remove(jar);
	}

	public boolean isPedantic() {
		return current().pedantic;
	}

	public void setPedantic(boolean pedantic) {
		this.pedantic = pedantic;
	}

	public void use(Processor reporter) {
		setPedantic(reporter.isPedantic());
		setTrace(reporter.isTrace());
		setExceptions(reporter.isExceptions());
		setFailOk(reporter.isFailOk());
	}

	public static File getFile(File base, String file) {
		return IO.getFile(base, file);
	}

	public File getFile(String file) {
		return getFile(base, file);
	}

	/**
	 * Return a list of plugins that implement the given class.
	 *
	 * @param clazz Each returned plugin implements this class/interface
	 * @return A list of plugins
	 */
	public <T> List<T> getPlugins(Class<T> clazz) {
		List<T> l = new ArrayList<>();
		Set<Object> all = getPlugins();
		for (Object plugin : all) {
			if (clazz.isInstance(plugin))
				l.add(clazz.cast(plugin));
		}
		return l;
	}

	/**
	 * Returns the first plugin it can find of the given type.
	 *
	 * @param <T>
	 * @param clazz
	 */
	public <T> T getPlugin(Class<T> clazz) {
		Set<Object> all = getPlugins();
		for (Object plugin : all) {
			if (clazz.isInstance(plugin))
				return clazz.cast(plugin);
		}
		return null;
	}

	/**
	 * Return a list of plugins. Plugins are defined with the -plugin command.
	 * They are class names, optionally associated with attributes. Plugins can
	 * implement the Plugin interface to see these attributes. Any object can be
	 * a plugin.
	 */
	public Set<Object> getPlugins() {
		Set<Object> p;
		synchronized (this) {
			p = plugins;
			if (p != null)
				return p;

			plugins = p = new CopyOnWriteArraySet<>();
			missingCommand = new HashSet<>();
		}
		// We only use plugins now when they are defined on our level
		// and not if it is in our parent. We inherit from our parent
		// through the previous block.

		String spe = getProperty(PLUGIN);
		if (NONE.equals(spe))
			return p;

		// The owner of the plugin is always in there.
		p.add(this);
		setTypeSpecificPlugins(p);

		if (parent != null)
			p.addAll(parent.getPlugins());

		//
		// Look only local
		//

		spe = mergeLocalProperties(PLUGIN);
		String pluginPath = mergeProperties(PLUGINPATH);
		loadPlugins(p, spe, pluginPath);

		addExtensions(p);

		for (RegistryDonePlugin rdp : getPlugins(RegistryDonePlugin.class)) {
			try {
				rdp.done();
			} catch (Exception e) {
				error("Calling done on %s, gives an exception %s", rdp, e);
			}
		}
		return p;
	}

	/**
	 * Is called when all plugins are loaded
	 *
	 * @param p
	 */
	protected void addExtensions(Set<Object> p) {

	}

	/**
	 * Magic to load the plugins. This is quite tricky actually since we allow
	 * plugins to be downloaded (this is mainly intended for repositories since
	 * in general plugins should use extensions, however to bootstrap the
	 * extensions we need more). Since downloads might need plugins for
	 * passwords and protocols we need to first load the paths specified on the
	 * plugin clause, then check if there are any local plugins (starting with
	 * aQute.bnd and be able to load from our own class loader).
	 * <p>
	 * After that, we load the plugin paths, these can use the built in
	 * connectors.
	 * <p>
	 * Last but not least, we load the remaining plugins.
	 *
	 * @param instances
	 * @param pluginString
	 */
	protected void loadPlugins(Set<Object> instances, String pluginString, String pluginPathString) {
		Parameters plugins = new Parameters(pluginString, this);
		CL loader = getLoader();

		// First add the plugin-specific paths from their path: directives
		for (Entry<String,Attrs> entry : plugins.entrySet()) {
			String key = removeDuplicateMarker(entry.getKey());
			String path = entry.getValue().get(PATH_DIRECTIVE);
			if (path != null) {
				String parts[] = path.split("\\s*,\\s*");
				try {
					for (String p : parts) {
						File f = getFile(p).getAbsoluteFile();
						loader.add(f.toURI().toURL());
					}
				} catch (Exception e) {
					error("Problem adding path %s to loader for plugin %s. Exception: (%s)", path, key, e);
				}
			}
		}

		//
		// Try to load any plugins that are local
		// these must start with aQute.bnd.* and
		// and be possible to load. The main intention
		// of this code is to load the URL connectors so that
		// any access to remote plugins can use the connector
		// model.
		//

		Set<String> loaded = new HashSet<>();
		for (Entry<String,Attrs> entry : plugins.entrySet()) {
			String className = removeDuplicateMarker(entry.getKey());
			Attrs attrs = entry.getValue();

			logger.debug("Trying pre-plugin {}", className);

			Object plugin = loadPlugin(getClass().getClassLoader(), attrs, className, true);
			if (plugin != null) {
				// with the marker!!
				loaded.add(entry.getKey());
				instances.add(plugin);
			}
		}

		//
		// Make sure we load each plugin only once
		// by removing the entries that were successfully loaded
		//
		plugins.keySet().removeAll(loaded);

		loadPluginPath(instances, pluginPathString, loader);

		//
		// Load the remaining plugins
		//

		for (Entry<String,Attrs> entry : plugins.entrySet()) {
			String className = removeDuplicateMarker(entry.getKey());
			Attrs attrs = entry.getValue();

			logger.debug("Loading secondary plugin {}", className);

			// We can defer the error if the plugin specifies
			// a command name. In that case, we'll verify that
			// a bnd file does not contain any references to a
			// plugin
			// command. The reason this feature was added was
			// to compile plugin classes with the same build.
			String commands = attrs.get(COMMAND_DIRECTIVE);

			Object plugin = loadPlugin(loader, attrs, className, commands != null);
			if (plugin != null)
				instances.add(plugin);
			else {
				if (commands == null)
					error("Cannot load the plugin %s", className);
				else {
					Collection<String> cs = split(commands);
					missingCommand.addAll(cs);
				}
			}
		}
	}

	/**
	 * Add the @link {@link Constants#PLUGINPATH} entries (which are file names)
	 * to the class loader. If this file does not exist, and there is a
	 * {@link Constants#PLUGINPATH_URL_ATTR} attribute then we download it first
	 * from that url. You can then also specify a
	 * {@link Constants#PLUGINPATH_SHA1_ATTR} attribute to verify the file.
	 *
	 * @see PLUGINPATH
	 * @param pluginPath the clauses for the plugin path
	 * @param loader The class loader to extend
	 */
	private void loadPluginPath(Set<Object> instances, String pluginPath, CL loader) {
		Parameters pluginpath = new Parameters(pluginPath, this);

		nextClause: for (Entry<String,Attrs> entry : pluginpath.entrySet()) {

			File f = getFile(entry.getKey()).getAbsoluteFile();
			if (!f.isFile()) {

				//
				// File does not exist! Check if we need to download
				//

				String url = entry.getValue().get(PLUGINPATH_URL_ATTR);
				if (url != null) {
					try {

						logger.debug("downloading {} to {}", url, f.getAbsoluteFile());
						URL u = new URL(url);
						URLConnection connection = u.openConnection();

						//
						// Allow the URLCOnnectionHandlers to interact with the
						// connection so they can sign it or decorate it with
						// a password etc.
						//
						for (Object plugin : instances) {
							if (plugin instanceof URLConnectionHandler) {
								URLConnectionHandler handler = (URLConnectionHandler) plugin;
								if (handler.matches(u))
									handler.handle(connection);
							}
						}

						//
						// Copy the url to the file
						//
						IO.mkdirs(f.getParentFile());
						IO.copy(connection.getInputStream(), f);

						//
						// If there is a sha specified, we verify the download
						// of the
						// the file.
						//
						String digest = entry.getValue().get(PLUGINPATH_SHA1_ATTR);
						if (digest != null) {
							if (Hex.isHex(digest.trim())) {
								byte[] sha1 = Hex.toByteArray(digest);
								byte[] filesha1 = SHA1.digest(f).digest();
								if (!Arrays.equals(sha1, filesha1)) {
									error("Plugin path: %s, specified url %s and a sha1 but the file does not match the sha",
											entry.getKey(), url);
								}
							} else {
								error("Plugin path: %s, specified url %s and a sha1 '%s' but this is not a hexadecimal",
										entry.getKey(), url, digest);
							}
						}
					} catch (Exception e) {
						error("Failed to download plugin %s from %s, error %s", entry.getKey(), url, e);
						continue nextClause;
					}
				} else {
					error("No such file %s from %s and no 'url' attribute on the path so it can be downloaded",
							entry.getKey(), this);
					continue nextClause;
				}
			}
			logger.debug("Adding {} to loader for plugins", f);
			try {
				loader.add(f.toURI().toURL());
			} catch (MalformedURLException e) {
				// Cannot happen since every file has a correct url
			}
		}
	}

	/**
	 * Load a plugin and customize it. If the plugin cannot be loaded then we
	 * return null.
	 *
	 * @param loader Name of the loader
	 * @param attrs
	 * @param className
	 */
	private Object loadPlugin(ClassLoader loader, Attrs attrs, String className, boolean ignoreError) {
		try {
			Class< ? > c = loader.loadClass(className);
			Object plugin = c.getConstructor().newInstance();
			customize(plugin, attrs);
			if (plugin instanceof Closeable) {
				addClose((Closeable) plugin);
			}
			return plugin;
		} catch (NoClassDefFoundError e) {
			if (!ignoreError)
				exception(e, "Failed to load plugin %s;%s, error: %s ", className, attrs, e);
		} catch (ClassNotFoundException e) {
			if (!ignoreError)
				exception(e, "Failed to load plugin %s;%s, error: %s ", className, attrs, e);
		} catch (Exception e) {
			exception(e, "Unexpected error loading plugin %s-%s: %s", className, attrs, e);
		}
		return null;
	}

	protected void setTypeSpecificPlugins(Set<Object> list) {
		list.add(getExecutor());
		list.add(random);
		list.addAll(basicPlugins);
	}

	/**
	 * Set the initial parameters of a plugin
	 *
	 * @param plugin
	 * @param map
	 */
	protected <T> T customize(T plugin, Attrs map) {
		if (plugin instanceof Plugin) {
			((Plugin) plugin).setReporter(this);
			try {
				if (map == null)
					map = Attrs.EMPTY_ATTRS;
				((Plugin) plugin).setProperties(map);
			} catch (Exception e) {
				error("While setting properties %s on plugin %s, %s", map, plugin, e);
			}
		}
		if (plugin instanceof RegistryPlugin) {
			((RegistryPlugin) plugin).setRegistry(this);
		}
		return plugin;
	}

	/**
	 * Indicates that this run should ignore errors and succeed anyway
	 *
	 * @return true if this processor should return errors
	 */
	@Override
	public boolean isFailOk() {
		String v = getProperty(Analyzer.FAIL_OK, null);
		return v != null && v.equalsIgnoreCase("true");
	}

	public File getBase() {
		return base;
	}

	public URI getBaseURI() {
		return baseURI;
	}

	public void setBase(File base) {
		this.base = base;
		baseURI = (base == null) ? null : base.toURI();
	}

	public void clear() {
		errors.clear();
		warnings.clear();
		locations.clear();
		fixupMessages = false;
	}

	public Logger getLogger() {
		return logger;
	}

	/**
	 * @deprecated Use SLF4J Logger.debug instead.
	 */
	@Deprecated
	public void trace(String msg, Object... parms) {
		Processor p = current();
		Logger l = p.getLogger();
		if (p.trace) {
			if (l.isInfoEnabled()) {
				l.info("{}", formatArrays(msg, parms));
			}
		} else {
			if (l.isDebugEnabled()) {
				l.debug("{}", formatArrays(msg, parms));
			}
		}
	}

	public <T> List<T> newList() {
		return new ArrayList<>();
	}

	public <T> Set<T> newSet() {
		return new TreeSet<>();
	}

	public static <K, V> Map<K,V> newMap() {
		return new LinkedHashMap<>();
	}

	public static <K, V> Map<K,V> newHashMap() {
		return new LinkedHashMap<>();
	}

	public <T> List<T> newList(Collection<T> t) {
		return new ArrayList<>(t);
	}

	public <T> Set<T> newSet(Collection<T> t) {
		return new TreeSet<>(t);
	}

	public <K, V> Map<K,V> newMap(Map<K,V> t) {
		return new LinkedHashMap<>(t);
	}

	public void close() throws IOException {
		for (Closeable c : toBeClosed) {
			IO.close(c);
		}
		if (pluginLoader != null)
			pluginLoader.closex();

		toBeClosed.clear();
	}

	public String _basedir(@SuppressWarnings("unused") String args[]) {
		if (base == null)
			throw new IllegalArgumentException("No base dir set");

		return base.getAbsolutePath();
	}

	public String _propertiesname(String[] args) {
		if (args.length > 1) {
			error("propertiesname does not take arguments");
			return null;
		}

		File pf = getPropertiesFile();
		if (pf == null)
			return "";

		return pf.getName();
	}

	public String _propertiesdir(String[] args) {
		if (args.length > 1) {
			error("propertiesdir does not take arguments");
			return null;
		}
		File pf = getPropertiesFile();
		if (pf == null)
			return "";

		return pf.getParentFile().getAbsolutePath();
	}

	static String _uri = "${uri;<uri>[;<baseuri>]}, Resolve the uri against the baseuri. baseuri defaults to the processor base.";

	public String _uri(String args[]) throws Exception {
		Macro.verifyCommand(args, _uri, null, 2, 3);

		URI uri = new URI(args[1]);
		if (!uri.isAbsolute() || uri.getScheme().equals("file")) {
			URI base;
			if (args.length > 2) {
				base = new URI(args[2]);
			} else {
				base = getBaseURI();
				if (base == null) {
					throw new IllegalArgumentException("No base dir set");
				}
			}
			uri = base.resolve(uri.getSchemeSpecificPart());
		}
		return uri.toString();
	}

	static String _fileuri = "${fileuri;<path>}, Return a file uri for the specified path. Relative paths are resolved against the processor base.";

	public String _fileuri(String args[]) throws Exception {
		Macro.verifyCommand(args, _fileuri, null, 2, 2);

		File f = IO.getFile(getBase(), args[1]).getCanonicalFile();
		return f.toURI().toString();
	}

	/**
	 * Property handling ...
	 */

	public Properties getProperties() {
		if (fixup) {
			fixup = false;
			begin();
		}
		fixupMessages = false;
		return getProperties0();
	}

	private Properties getProperties0() {
		return properties;
	}

	public String getProperty(String key) {
		return getProperty(key, null);
	}

	public void mergeProperties(File file, boolean override) {
		if (file.isFile()) {
			try {
				Properties properties = loadProperties(file);
				mergeProperties(properties, override);
			} catch (Exception e) {
				error("Error loading properties file: %s", file);
			}
		} else {
			if (!file.exists())
				error("Properties file does not exist: %s", file);
			else
				error("Properties file must a file, not a directory: %s", file);
		}
	}

	public void mergeProperties(Properties properties, boolean override) {
		for (Enumeration< ? > e = properties.propertyNames(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			String value = properties.getProperty(key);
			if (override || !getProperties().containsKey(key))
				setProperty(key, value);
		}
	}

	public void setProperties(Properties properties) {
		doIncludes(getBase(), properties);
		getProperties0().putAll(properties);
		mergeProperties(Constants.INIT); // execute macros in -init
		getProperties0().remove(Constants.INIT);
	}

	public void setProperties(File base, Properties properties) {
		doIncludes(base, properties);
		getProperties0().putAll(properties);
	}

	public void addProperties(File file) throws Exception {
		addIncluded(file);
		Properties p = loadProperties(file);
		setProperties(p);
	}

	public void addProperties(Map< ? , ? > properties) {
		for (Entry< ? , ? > entry : properties.entrySet()) {
			setProperty(entry.getKey().toString(), entry.getValue() + "");
		}
	}

	public void addIncluded(File file) {
		addIncludedIfAbsent(file);
	}

	private boolean addIncludedIfAbsent(File file) {
		return included.addIfAbsent(file);
	}

	private boolean removeIncluded(File file) {
		return included.remove(file);
	}

	/**
	 * Inspect the properties and if you find -includes parse the line included
	 * manifest files or properties files. The files are relative from the given
	 * base, this is normally the base for the analyzer.
	 *
	 * @param ubase
	 * @param p
	 * @param done
	 * @throws IOException
	 * @throws IOException
	 */

	private void doIncludes(File ubase, Properties p) {
		String includes = p.getProperty(INCLUDE);
		if (includes != null) {
			includes = getReplacer().process(includes);
			p.remove(INCLUDE);
			Collection<String> clauses = new Parameters(includes, this).keySet();

			for (String value : clauses) {
				boolean fileMustExist = true;
				boolean overwrite = true;
				while (true) {
					if (value.startsWith("-")) {
						fileMustExist = false;
						value = value.substring(1).trim();
					} else if (value.startsWith("~")) {
						// Overwrite properties!
						overwrite = false;
						value = value.substring(1).trim();
					} else
						break;
				}
				try {
					File file = getFile(ubase, value).getAbsoluteFile();
					if (!file.isFile()) {
						try {
							URL url = new URL(value);
							int n = value.lastIndexOf('.');
							String ext = ".jar";
							if (n >= 0)
								ext = value.substring(n);

							Path tmp = Files.createTempFile("url", ext);
							try (Resource resource = Resource.fromURL(url)) {
								try (OutputStream out = IO.outputStream(tmp)) {
									resource.write(out);
								}
								Files.setLastModifiedTime(tmp, FileTime.fromMillis(resource.lastModified()));
								doIncludeFile(tmp.toFile(), overwrite, p);
							} finally {
								removeIncluded(tmp.toFile());
								IO.delete(tmp);
							}
						} catch (MalformedURLException mue) {
							if (fileMustExist)
								error("Included file %s %s", file,
										(file.isDirectory() ? "is directory" : "does not exist"));
						} catch (Exception e) {
							if (fileMustExist)
								exception(e, "Error in processing included URL: %s", value);
						}
					} else
						doIncludeFile(file, overwrite, p);
				} catch (Exception e) {
					if (fileMustExist)
						exception(e, "Error in processing included file: %s", value);
				}
			}
		}
	}

	/**
	 * @param file
	 * @param overwrite
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void doIncludeFile(File file, boolean overwrite, Properties target) throws Exception {
		doIncludeFile(file, overwrite, target, null);
	}

	/**
	 * @param file
	 * @param overwrite
	 * @param extensionName
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void doIncludeFile(File file, boolean overwrite, Properties target, String extensionName) throws Exception {
		if (!addIncludedIfAbsent(file)) {
			error("Cyclic or multiple include of %s", file);
		}
		updateModified(file.lastModified(), file.toString());
		Properties sub;
		if (file.getName().toLowerCase().endsWith(".mf")) {
			try (InputStream in = IO.stream(file)) {
				sub = getManifestAsProperties(in);
			}
		} else
			sub = loadProperties(file);

		doIncludes(file.getParentFile(), sub);
		// make sure we do not override properties
		for (Map.Entry< ? , ? > entry : sub.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();

			if (overwrite || !target.containsKey(key)) {
				target.setProperty(key, value);
			} else if (extensionName != null) {
				String extensionKey = extensionName + "." + key;
				if (!target.containsKey(extensionKey))
					target.setProperty(extensionKey, value);
			}
		}
	}

	public void unsetProperty(String string) {
		getProperties().remove(string);

	}

	public boolean refresh() {
		synchronized (this) {
			plugins = null; // We always refresh our plugins
		}

		if (propertiesFile == null)
			return false;

		boolean changed = updateModified(propertiesFile.lastModified(), "properties file");
		for (File file : getIncluded()) {
			if (changed)
				break;

			changed |= !file.exists() || updateModified(file.lastModified(), "include file: " + file);
		}

		profile = getProperty(PROFILE); // Used in property access

		if (changed) {
			forceRefresh();
			return true;
		}
		return false;
	}

	/**
	 * If strict is true, then extra verification is done.
	 */
	boolean isStrict() {
		if (strict == null)
			strict = isTrue(getProperty(STRICT)); // Used in property access
		return strict;
	}

	/**
	 *
	 */
	public void forceRefresh() {
		included.clear();
		Processor p = getParent();
		properties = (p != null) ? new UTF8Properties(p.getProperties0()) : new UTF8Properties();

		setProperties(propertiesFile, base);
		propertiesChanged();
	}

	public void propertiesChanged() {}

	/**
	 * Set the properties by file. Setting the properties this way will also set
	 * the base for this analyzer. After reading the properties, this will call
	 * setProperties(Properties) which will handle the includes.
	 *
	 * @param propertiesFile
	 */
	public void setProperties(File propertiesFile) {
		propertiesFile = propertiesFile.getAbsoluteFile();
		setProperties(propertiesFile, propertiesFile.getParentFile());
	}

	public void setProperties(File propertiesFile, File base) {
		this.propertiesFile = propertiesFile.getAbsoluteFile();
		setBase(base);
		try {
			if (propertiesFile.isFile()) {
				// System.err.println("Loading properties " + propertiesFile);
				long modified = propertiesFile.lastModified();
				if (modified > System.currentTimeMillis() + 100) {
					System.err.println("Huh? This is in the future " + propertiesFile);
					this.modified = System.currentTimeMillis();
				} else
					this.modified = modified;

				included.clear();
				Properties p = loadProperties(propertiesFile);
				setProperties(p);
			} else {
				if (fileMustExist) {
					error("No such properties file: %s", propertiesFile);
				}
			}
		} catch (IOException e) {
			error("Could not load properties %s", propertiesFile);
		}
	}

	protected void begin() {
		if (isTrue(getProperty(PEDANTIC)))
			setPedantic(true);
	}

	public static boolean isTrue(String value) {

		if (value == null)
			return false;

		value = value.trim();
		if (value.isEmpty())
			return false;

		if (value.startsWith("!"))
			if (value.equals("!"))
				return false;
			else
				return !isTrue(value.substring(1));

		if ("false".equalsIgnoreCase(value))
			return false;

		if ("off".equalsIgnoreCase(value))
			return false;

		if ("not".equalsIgnoreCase(value))
			return false;

		return true;
	}

	/**
	 * Get a property without preprocessing it with a proper default
	 *
	 * @param key
	 * @param deflt
	 */

	public String getUnprocessedProperty(String key, String deflt) {
		if (filter != null && filter.contains(key)) {
			return (String) getProperties().getOrDefault(key, deflt);
		}
		return getProperties().getProperty(key, deflt);
	}

	/**
	 * Get a property with preprocessing it with a proper default
	 *
	 * @param key
	 * @param deflt
	 */
	public String getProperty(String key, String deflt) {
		return getProperty(key, deflt, ",");
	}

	public String getProperty(String key, String deflt, String separator) {
		return getProperty(key, deflt, separator, true);
	}

	@SuppressWarnings("resource")
	private String getProperty(String key, String deflt, String separator, boolean inherit) {

		Instruction ins = new Instruction(key);
		if (!ins.isLiteral()) {
			return getWildcardProperty(deflt, separator, inherit, ins);
		}

		return getLiteralProperty(key, deflt, this, inherit);
	}

	private String getWildcardProperty(String deflt, String separator, boolean inherit, Instruction ins) {
		// Handle a wildcard key, make sure they're sorted
		// for consistency
		String result = stream(inherit).filter(ins::matches)
			.sorted()
			.map(k -> getLiteralProperty(k, null, this, inherit))
			.filter(v -> (v != null) && !v.isEmpty())
			.collect(joining(separator));
		return result.isEmpty() ? deflt : result;
	}

	private String getLiteralProperty(String key, String deflt, Processor source, boolean inherit) {
		String value = null;
		// Use the key as is first, if found ok

		if (filter != null && filter.contains(key)) {
			Object raw = getProperties().get(key);
			if (raw != null) {
				if (raw instanceof String) {
					value = (String) raw;
				} else {
					warning("Key '%s' has a non-String value: %s:%s", key, raw == null ? "" : raw.getClass().getName(),
							raw);
				}
			}
		} else {
			for (Processor proc = source; proc != null; proc = proc.getParent()) {
				Object raw = proc.getProperties()
					.get(key);
				if (raw != null) {
					if (raw instanceof String) {
						value = (String) raw;
					} else {
						warning("Key '%s' has a non-String value: %s:%s", key,
							raw == null ? ""
								: raw.getClass()
									.getName(),
							raw);
					}
					source = proc;
					break;
				}

				if (!inherit)
					break;
			}
			//
			// Check if we can find a replacement through the
			// replacer, which takes profiles into account
			if (value == null) {
				value = getReplacer().getMacro(key, null);
			}
		}

		if (value != null)
			return getReplacer().process(value, source);
		else if (deflt != null)
			return getReplacer().process(deflt, this);
		else
			return null;
	}

	/**
	 * Helper to load a properties file from disk.
	 *
	 * @param file
	 * @throws IOException
	 */
	public Properties loadProperties(File file) throws IOException {
		updateModified(file.lastModified(), "Properties file: " + file);
		UTF8Properties p = loadProperties0(file);
		return p;
	}

	/**
	 * Load Properties from disk. The default encoding is ISO-8859-1 but
	 * nowadays all files are encoded with UTF-8. So we try to load it first as
	 * UTF-8 and if this fails we fail back to ISO-8859-1
	 *
	 * @param in The stream to load from
	 * @param name The name of the file for doc reasons
	 * @return a Properties
	 * @throws IOException
	 */
	UTF8Properties loadProperties0(File file) throws IOException {
		String name = file.toURI().getPath();
		int n = name.lastIndexOf('/');
		if (n > 0)
			name = name.substring(0, n);
		if (name.length() == 0)
			name = ".";

		try {
			UTF8Properties p = new UTF8Properties();
			p.load(file, this);
			return p.replaceAll("\\$\\{\\.\\}", Matcher.quoteReplacement(name));
		} catch (Exception e) {
			error("Error during loading properties file: %s, error: %s", name, e);
			return new UTF8Properties();
		}
	}

	/**
	 * Replace a string in all the values of the map. This can be used to
	 * preassign variables that change. I.e. the base directory ${.} for a
	 * loaded properties
	 */
	public static Properties replaceAll(Properties p, String pattern, String replacement) {
		UTF8Properties result = new UTF8Properties();
		Pattern regex = Pattern.compile(pattern);
		for (Map.Entry<Object,Object> entry : p.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			value = regex.matcher(value).replaceAll(replacement);
			result.put(key, value);
		}
		return result;
	}

	/**
	 * Print a standard Map based OSGi header.
	 *
	 * @param exports map { name => Map { attribute|directive => value } }
	 * @return the clauses
	 * @throws IOException
	 */
	public static String printClauses(Map< ? , ? extends Map< ? , ? >> exports) throws IOException {
		return printClauses(exports, false);
	}

	public static String printClauses(Map< ? , ? extends Map< ? , ? >> exports,
			@SuppressWarnings("unused") boolean checkMultipleVersions) throws IOException {
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (Entry< ? , ? extends Map< ? , ? >> entry : exports.entrySet()) {
			String name = entry.getKey().toString();
			Map< ? , ? > clause = entry.getValue();

			// We allow names to be duplicated in the input
			// by ending them with '~'. This is necessary to use
			// the package names as keys. However, we remove these
			// suffixes in the output so that you can set multiple
			// exports with different attributes.
			String outname = removeDuplicateMarker(name);
			sb.append(del);
			sb.append(outname);
			printClause(clause, sb);
			del = ",";
		}
		return sb.toString();
	}

	public static void printClause(Map< ? , ? > map, StringBuilder sb) throws IOException {
		if (map instanceof Attrs) {
			Attrs attrs = (Attrs) map;
			for (Entry<String, String> entry : attrs.entrySet()) {
				String key = entry.getKey();
				// Skip directives we do not recognize
				if (skipPrint(key))
					continue;

				sb.append(";");
				attrs.append(sb, entry);
			}
		} else {
			for (Entry<?, ?> entry : map.entrySet()) {
				String key = entry.getKey()
					.toString();
				// Skip directives we do not recognize
				if (skipPrint(key))
					continue;

				sb.append(";");
				sb.append(key);
				sb.append("=");
				String value = ((String) entry.getValue()).trim();
				quote(sb, value);
			}
		}
	}

	private static boolean skipPrint(String key) {
		switch (key) {
			case INTERNAL_SOURCE_DIRECTIVE :
			case INTERNAL_EXPORTED_DIRECTIVE :
			case NO_IMPORT_DIRECTIVE :
			case PROVIDE_DIRECTIVE :
			case SPLIT_PACKAGE_DIRECTIVE :
			case FROM_DIRECTIVE :
				return true;
			default :
				return false;
		}
	}

	/**
	 * @param sb
	 * @param value
	 * @throws IOException
	 */
	public static boolean quote(Appendable sb, String value) throws IOException {
		return OSGiHeader.quote(sb, value);
	}

	public Macro getReplacer() {
		if (replacer == null)
			return replacer = new Macro(this, getMacroDomains());
		return replacer;
	}

	/**
	 * This should be overridden by subclasses to add extra macro command
	 * domains on the search list.
	 */
	protected Object[] getMacroDomains() {
		return new Object[] {};
	}

	/**
	 * Return the properties but expand all macros. This always returns a new
	 * Properties object that can be used in any way.
	 */
	public Properties getFlattenedProperties() {
		return getReplacer().getFlattenedProperties();

	}

	/**
	 * Return the properties but expand all macros. This always returns a new
	 * Properties object that can be used in any way.
	 */
	public Properties getFlattenedProperties(boolean ignoreInstructions) {
		return getReplacer().getFlattenedProperties(ignoreInstructions);
	}

	/**
	 * Return all inherited property keys. The keys are sorted for consistent
	 * ordering.
	 */
	public Set<String> getPropertyKeys(boolean inherit) {
		Set<String> result;
		if (parent == null || !inherit) {
			result = new TreeSet<>();
		} else {
			result = parent.getPropertyKeys(inherit);
		}
		for (Object o : getProperties0().keySet()) {
			result.add(o.toString());
		}
		return result;
	}

	public boolean updateModified(long time, @SuppressWarnings("unused") String reason) {
		if (time > lastModified) {
			lastModified = time;
			return true;
		}
		return false;
	}

	public long lastModified() {
		return lastModified;
	}

	/**
	 * Add or override a new property.
	 *
	 * @param key
	 * @param value
	 */
	public void setProperty(String key, String value) {
		checkheader: for (int i = 0; i < headers.length; i++) {
			if (headers[i].equalsIgnoreCase(key)) {
				key = headers[i];
				break checkheader;
			}
		}
		getProperties().put(key, value);
	}

	/**
	 * Read a manifest but return a properties object.
	 *
	 * @param in
	 * @throws IOException
	 */
	public static Properties getManifestAsProperties(InputStream in) throws IOException {
		Properties p = new UTF8Properties();
		Manifest manifest = new Manifest(in);
		for (Iterator<Object> it = manifest.getMainAttributes().keySet().iterator(); it.hasNext();) {
			Attributes.Name key = (Attributes.Name) it.next();
			String value = manifest.getMainAttributes().getValue(key);
			p.put(key.toString(), value);
		}
		return p;
	}

	public File getPropertiesFile() {
		return propertiesFile;
	}

	public void setFileMustExist(boolean mustexist) {
		fileMustExist = mustexist;
	}

	static public String read(InputStream in) throws Exception {
		return IO.collect(in, UTF_8);
	}

	/**
	 * Join a list.
	 */
	public static String join(Collection<?> list) {
		return join(list, ",");
	}

	public static String join(Collection<?> list, String delimeter) {
		if (list == null || list.isEmpty())
			return "";
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (Object item : list) {
			sb.append(del);
			sb.append(item);
			del = delimeter;
		}
		return sb.toString();
	}

	public static String join(Collection<?>... lists) {
		return join(",", lists);
	}

	public static String join(String delimeter, Collection<?>... lists) {
		if (lists == null || lists.length == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (Collection<?> list : lists) {
			for (Object item : list) {
				sb.append(del);
				sb.append(item);
				del = delimeter;
			}
		}
		return sb.toString();
	}

	public static String join(Object[] list, String delimeter) {
		if (list == null || list.length == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (Object item : list) {
			sb.append(del);
			sb.append(item);
			del = delimeter;
		}
		return sb.toString();
	}

	public static <T> String join(T[] list) {
		return join(list, ",");
	}

	public static void split(String s, Collection<String> set) {
		if (s == null || (s = s.trim()).isEmpty())
			return;
		for (String element : LIST_SPLITTER_PATTERN.split(s, 0)) {
			if (!element.isEmpty())
				set.add(element);
		}
	}

	public static Collection<String> split(String s) {
		if (s == null || (s = s.trim()).isEmpty())
			return Collections.emptyList();
		return Arrays.asList(LIST_SPLITTER_PATTERN.split(s, 0));
	}

	public static Collection<String> split(String s, String splitter) {
		if (s == null || (s = s.trim()).isEmpty())
			return Collections.emptyList();
		return Arrays.asList(s.split(splitter, 0));
	}

	public static String merge(String... strings) {
		ArrayList<String> result = new ArrayList<>();
		for (String s : strings) {
			if (s != null)
				split(s, result);
		}
		return join(result);
	}

	public boolean isExceptions() {
		return exceptions;
	}

	public void setExceptions(boolean exceptions) {
		this.exceptions = exceptions;
	}

	/**
	 * Make the file short if it is inside our base directory, otherwise long.
	 *
	 * @param f
	 */
	public String normalize(String f) {
		if (f.startsWith(base.getAbsolutePath() + "/"))
			return f.substring(base.getAbsolutePath().length() + 1);
		return f;
	}

	public String normalize(File f) {
		return normalize(f.getAbsolutePath());
	}

	public static String removeDuplicateMarker(String key) {
		int i = key.length() - 1;
		while (i >= 0 && key.charAt(i) == DUPLICATE_MARKER)
			--i;

		return key.substring(0, i + 1);
	}

	public static boolean isDuplicate(String name) {
		return name.length() > 0 && name.charAt(name.length() - 1) == DUPLICATE_MARKER;
	}

	public void setTrace(boolean x) {
		trace = x;
	}

	public static class CL extends URLClassLoader {

		CL(Processor p) {
			super(new URL[0], p.getClass().getClassLoader());
		}

		void closex() {
			Class<URLClassLoader> clazz = URLClassLoader.class;

			try {
				//
				// Java 7 is a good boy, it has a close method
				//
				clazz.getMethod("close").invoke(this);
				return;
			} catch (Exception e) {
				// ignore
			}

			//
			// On Java 6, we're screwed and have to much around
			// This is best effort, likely fails on non-SUN vms
			// :-(
			//

			try {
				Field ucpField = clazz.getDeclaredField("ucp");
				ucpField.setAccessible(true);
				Object cp = ucpField.get(this);
				Field loadersField = cp.getClass().getDeclaredField("loaders");
				loadersField.setAccessible(true);
				Collection< ? > loaders = (Collection< ? >) loadersField.get(cp);
				for (Object loader : loaders) {
					try {
						Field loaderField = loader.getClass().getDeclaredField("jar");
						loaderField.setAccessible(true);
						JarFile jarFile = (JarFile) loaderField.get(loader);
						jarFile.close();
					} catch (Throwable t) {
						// if we got this far, this is probably not a JAR loader
						// so skip it
					}
				}
			} catch (Throwable t) {
				// probably not a SUN VM
			}
		}

		void add(URL url) {
			URL urls[] = getURLs();
			for (URL u : urls) {
				if (u.equals(url))
					return;
			}
			super.addURL(url);
		}

		@Override
		public Class< ? > loadClass(String name) throws ClassNotFoundException {
			try {
				Class< ? > c = super.loadClass(name);
				return c;
			} catch (Throwable t) {
				StringBuilder sb = new StringBuilder();
				sb.append(name);
				sb.append(" not found, parent: ");
				sb.append(getParent());
				sb.append(" urls:");
				sb.append(Arrays.toString(getURLs()));
				sb.append(" exception:");
				sb.append(Exceptions.toString(t));
				throw new ClassNotFoundException(sb.toString(), t);
			}
		}
	}

	protected CL getLoader() {
		if (pluginLoader == null) {
			pluginLoader = new CL(this);
		}
		return pluginLoader;
	}

	/*
	 * Check if this is a valid project.
	 */
	public boolean exists() {
		return base != null && base.isDirectory() && propertiesFile != null && propertiesFile.isFile();
	}

	public boolean isOk() {
		return isFailOk() || (getErrors().size() == 0);
	}

	/**
	 * Move errors and warnings to their proper place by scanning the fixup
	 * messages property.
	 */
	private void fixupMessages() {
		if (fixupMessages)
			return;
		fixupMessages = true;
		Parameters fixup = getMergedParameters(Constants.FIXUPMESSAGES);
		if (fixup.isEmpty())
			return;

		Instructions instrs = new Instructions(fixup);

		doFixup(instrs, errors, warnings, FIXUPMESSAGES_IS_ERROR);
		doFixup(instrs, warnings, errors, FIXUPMESSAGES_IS_WARNING);
	}

	private void doFixup(Instructions instrs, List<String> messages, List<String> other, String type) {
		for (int i = 0; i < messages.size(); i++) {
			String message = messages.get(i);
			Instruction matcher = instrs.finder(message);
			if (matcher == null || matcher.isNegated())
				continue;

			Attrs attrs = instrs.get(matcher);

			//
			// Default the pattern applies to the errors and warnings
			// but we can restrict it: e.g. restrict:=error
			//

			String restrict = attrs.get(FIXUPMESSAGES_RESTRICT_DIRECTIVE);
			if (restrict != null && !restrict.equals(type))
				continue;

			//
			// We can optionally replace the message with another text. E.g.
			// replace:"hello world". This can use macro expansion, the ${@}
			// macro is set to the old message.
			//
			String replace = attrs.get(FIXUPMESSAGES_REPLACE_DIRECTIVE);
			if (replace != null) {
				logger.debug("replacing {} with {}", message, replace);
				setProperty("@", message);
				message = getReplacer().process(replace);
				messages.set(i, message);
				unsetProperty("@");
			}

			//
			//
			String is = attrs.get(FIXUPMESSAGES_IS_DIRECTIVE);

			if (attrs.isEmpty() || FIXUPMESSAGES_IS_IGNORE.equals(is)) {
				messages.remove(i--);
			} else {
				if (is != null && !type.equals(is)) {
					messages.remove(i--);
					other.add(message);
				}
			}
		}
	}

	public boolean check(String... pattern) throws IOException {
		Set<String> missed = Create.set();

		if (pattern != null) {
			for (String p : pattern) {
				boolean match = false;
				Pattern pat = Pattern.compile(p);
				for (Iterator<String> i = errors.iterator(); i.hasNext();) {
					if (pat.matcher(i.next()).find()) {
						i.remove();
						match = true;
					}
				}
				for (Iterator<String> i = warnings.iterator(); i.hasNext();) {
					if (pat.matcher(i.next()).find()) {
						i.remove();
						match = true;
					}
				}
				if (!match)
					missed.add(p);

			}
		}
		if (missed.isEmpty() && isPerfect())
			return true;

		if (!missed.isEmpty())
			System.err.println("Missed the following patterns in the warnings or errors: " + missed);

		report(System.err);
		return false;
	}

	protected void report(Appendable out) throws IOException {
		if (errors.size() > 0) {
			out.append(String.format("-----------------%nErrors%n"));
			for (int i = 0; i < errors.size(); i++) {
				out.append(String.format("%03d: %s%n", i, errors.get(i)));
			}
		}
		if (warnings.size() > 0) {
			out.append(String.format("-----------------%nWarnings%n"));
			for (int i = 0; i < warnings.size(); i++) {
				out.append(String.format("%03d: %s%n", i, warnings.get(i)));
			}
		}
	}

	public boolean isPerfect() {
		return getErrors().size() == 0 && getWarnings().size() == 0;
	}

	public void setForceLocal(Collection<String> local) {
		filter = local;
	}

	/**
	 * Answer if the name is a missing plugin's command name. If a bnd file
	 * contains the command name of a plugin, and that plugin is not available,
	 * then an error is reported during manifest calculation. This allows the
	 * plugin to fail to load when it is not needed. We first get the plugins to
	 * ensure it is properly initialized.
	 *
	 * @param name
	 */
	public boolean isMissingPlugin(String name) {
		getPlugins();
		return missingCommand != null && missingCommand.contains(name);
	}

	/**
	 * Append two strings to for a path in a ZIP or JAR file. It is guaranteed
	 * to return a string that does not start, nor ends with a '/', while it is
	 * properly separated with slashes. Double slashes are properly removed.
	 *
	 * <pre>
	 * &quot;/&quot; + &quot;abc/def/&quot; becomes &quot;abc/def&quot;
	 * &#064;param prefix &#064;param suffix &#064;return
	 */
	public static String appendPath(String... parts) {
		StringBuilder sb = new StringBuilder();
		boolean lastSlash = true;
		for (String part : parts) {
			for (int i = 0; i < part.length(); i++) {
				char c = part.charAt(i);
				if (c == '/') {
					if (!lastSlash)
						sb.append('/');
					lastSlash = true;
				} else {
					sb.append(c);
					lastSlash = false;
				}
			}

			if (!lastSlash && sb.length() > 0) {
				sb.append('/');
				lastSlash = true;
			}
		}
		if (lastSlash && sb.length() > 0)
			sb.deleteCharAt(sb.length() - 1);

		return sb.toString();
	}

	/**
	 * Parse the a=b strings and return a map of them.
	 *
	 * @param attrs
	 * @param clazz
	 */
	public static Attrs doAttrbutes(Object[] attrs, Clazz clazz, Macro macro) {
		Attrs map = new Attrs();

		if (attrs == null || attrs.length == 0)
			return map;

		for (Object a : attrs) {
			String attr = (String) a;
			int n = attr.indexOf('=');
			if (n > 0) {
				map.put(attr.substring(0, n), macro.process(attr.substring(n + 1)));
			} else
				throw new IllegalArgumentException(formatArrays(
						"Invalid attribute on package-info.java in %s , %s. Must be <key>=<name> ", clazz, attr));
		}
		return map;
	}

	/**
	 * This method is the same as String.format but it makes sure that any
	 * arrays are transformed to strings.
	 *
	 * @param string
	 * @param parms
	 */
	public static String formatArrays(String string, Object... parms) {
		return Strings.format(string, parms);
	}

	/**
	 * Check if the object is an array and turn it into a string if it is,
	 * otherwise unchanged.
	 *
	 * @param object the object to make printable
	 * @return a string if it was an array or the original object
	 */
	public static Object makePrintable(Object object) {
		if (object == null)
			return null;

		if (object.getClass().isArray()) {
			return Arrays.toString(makePrintableArray(object));
		}
		return object;
	}

	private static Object[] makePrintableArray(Object array) {
		final int length = Array.getLength(array);
		Object[] output = new Object[length];
		for (int i = 0; i < length; i++) {
			output[i] = makePrintable(Array.get(array, i));
		}
		return output;
	}

	public static String append(String... strings) {
		List<String> result = Create.list();
		for (String s : strings) {
			result.addAll(split(s));
		}
		return join(result);
	}

	public synchronized Class< ? > getClass(String type, File jar) throws Exception {
		CL cl = getLoader();
		cl.add(jar.toURI().toURL());
		return cl.loadClass(type);
	}

	public boolean isTrace() {
		return current().trace;
	}

	public static long getDuration(String tm, long dflt) {
		if (tm == null)
			return dflt;

		tm = tm.toUpperCase();
		TimeUnit unit = TimeUnit.MILLISECONDS;
		Matcher m = Pattern.compile("\\s*(\\d+)\\s*(NANOSECONDS|MICROSECONDS|MILLISECONDS|SECONDS|MINUTES|HOURS|DAYS)?")
				.matcher(tm);
		if (m.matches()) {
			long duration = Long.parseLong(tm);
			String u = m.group(2);
			if (u != null)
				unit = TimeUnit.valueOf(u);
			duration = TimeUnit.MILLISECONDS.convert(duration, unit);
			return duration;
		}
		return dflt;
	}

	/**
	 * Generate a random string, which is guaranteed to be a valid Java
	 * identifier (first character is an ASCII letter, subsequent characters are
	 * ASCII letters or numbers). Takes an optional parameter for the length of
	 * string to generate; default is 8 characters.
	 */
	public String _random(String[] args) {
		int numchars = 8;
		if (args.length > 1) {
			try {
				numchars = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid character count parameter in ${random} macro.");
			}
		}

		synchronized (Processor.class) {
			if (random == null)
				random = new Random();
		}

		char[] letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
		char[] alphanums = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

		char[] array = new char[numchars];
		for (int i = 0; i < numchars; i++) {
			char c;
			if (i == 0)
				c = letters[random.nextInt(letters.length)];
			else
				c = alphanums[random.nextInt(alphanums.length)];
			array[i] = c;
		}

		return new String(array);
	}

	/**
	 * <p>
	 * Generates a Capability string, in the format specified by the OSGi
	 * Provide-Capability header, representing the current native platform
	 * according to OSGi RFC 188. For example on Windows7 running on an x86_64
	 * processor it should generate the following:
	 * </p>
	 *
	 * <pre>
	 * osgi.native;osgi.native.osname:List&lt;String&gt;="Windows7,Windows
	 * 7,Win32";osgi.native.osversion:Version=6.1.0;osgi.native.processor:List&
	 * lt;String&gt;="x86-64,amd64,em64t,x86_64"
	 * </pre>
	 *
	 * @param args The array of properties. For example: the macro invocation of
	 *            "${native_capability;osversion=3.2.4;osname=Linux}" results in
	 *            an args array of
	 *            [native_capability,&nbsp;osversion=3.2.4,&nbsp;osname=Linux]
	 */

	public String _native_capability(String... args) throws Exception {
		return OSInformation.getNativeCapabilityClause(this, args);
	}

	/**
	 * Set the current command thread. This must be balanced with the
	 * {@link #endHandleErrors(Processor)} method. The method returns the
	 * previous command owner or null. The command owner will receive all
	 * warnings and error reports.
	 */

	protected Processor beginHandleErrors(String message) {
		logger.debug("begin {}", message);
		Processor previous = current.get();
		current.set(this);
		return previous;
	}

	/**
	 * End a command. Will restore the previous command owner.
	 *
	 * @param previous
	 */
	protected void endHandleErrors(Processor previous) {
		logger.debug("end");
		current.set(previous);
	}

	public static Executor getExecutor() {
		return executor;
	}

	public static ScheduledExecutorService getScheduledExecutor() {
		return sheduledExecutor;
	}

	/**
	 * These plugins are added to the total list of plugins. The separation is
	 * necessary because the list of plugins is refreshed now and then so we
	 * need to be able to add them at any moment in time.
	 *
	 * @param plugin
	 */
	public synchronized void addBasicPlugin(Object plugin) {
		basicPlugins.add(plugin);
		Set<Object> p = plugins;
		if (p != null)
			p.add(plugin);
	}

	public synchronized void removeBasicPlugin(Object plugin) {
		basicPlugins.remove(plugin);
		Set<Object> p = plugins;
		if (p != null)
			p.remove(plugin);
	}

	public List<File> getIncluded() {
		return included;
	}

	/**
	 * Overrides for the Domain class
	 */
	@Override
	public String get(String key) {
		return getProperty(key);
	}

	@Override
	public String get(String key, String deflt) {
		return getProperty(key, deflt);
	}

	@Override
	public void set(String key, String value) {
		getProperties().setProperty(key, value);
	}

	Stream<String> stream() {
		return stream(true);
	}

	private Stream<String> stream(boolean inherit) {
		return StreamSupport.stream(iterable(inherit).spliterator(), false);
	}

	@Override
	public Iterator<String> iterator() {
		return iterable(true).iterator();
	}

	@Override
	public Spliterator<String> spliterator() {
		return iterable(true).spliterator();
	}

	private Iterable<String> iterable(boolean inherit) {
		Set<Object> first = getProperties0().keySet();
		Iterable<? extends Object> second;
		if (parent == null || !inherit) {
			second = Collections.emptyList();
		} else {
			second = parent.iterable(inherit);
		}

		Iterable<String> iterable = Iterables.distinct(first, second,
			o -> (o instanceof String) ? (String) o : null);
		return iterable;
	}

	public Set<String> keySet() {
		return getPropertyKeys(true);
	}

	/**
	 * Printout of the status of this processor for toString()
	 */

	@Override
	public String toString() {
		try {
			StringBuilder sb = new StringBuilder();
			report(sb);
			return sb.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Utiltity to replace an extension
	 *
	 * @param s
	 * @param extension
	 * @param newExtension
	 */
	public String replaceExtension(String s, String extension, String newExtension) {
		if (s.endsWith(extension))
			s = s.substring(0, s.length() - extension.length());

		return s + newExtension;
	}

	/**
	 * Create a location object and add it to the locations
	 */
	List<Location> locations = new ArrayList<>();

	static class SetLocationImpl extends Location implements SetLocation {
		public SetLocationImpl(String s) {
			this.message = s;
		}

		public SetLocation file(String file) {
			this.file = file;
			return this;
		}

		public SetLocation header(String header) {
			this.header = header;
			return this;
		}

		public SetLocation context(String context) {
			this.context = context;
			return this;
		}

		public SetLocation method(String methodName) {
			this.methodName = methodName;
			return this;
		}

		public SetLocation line(int n) {
			this.line = n;
			return this;
		}

		public SetLocation reference(String reference) {
			this.reference = reference;
			return this;
		}

		public SetLocation details(Object details) {
			this.details = details;
			return this;
		}

		public Location location() {
			return this;
		}

		public SetLocation length(int length) {
			this.length = length;
			return this;
		}

	}

	private SetLocation location(String s) {
		SetLocationImpl loc = new SetLocationImpl(s);
		locations.add(loc);
		return loc;
	}

	public Location getLocation(String msg) {
		for (Location l : locations)
			if ((l.message != null) && l.message.equals(msg))
				return l;

		return null;
	}

	/**
	 * Get a header relative to this processor, tking its parents and includes
	 * into account.
	 *
	 * @param header
	 * @throws IOException
	 */
	public FileLine getHeader(String header) throws Exception {
		return getHeader(
				Pattern.compile("^[ \t]*" + Pattern.quote(header), Pattern.MULTILINE + Pattern.CASE_INSENSITIVE));
	}

	public static Pattern toFullHeaderPattern(String header) {
		StringBuilder sb = new StringBuilder();
		sb.append("^[ \t]*(").append(header).append(")(\\.[^\\s:=]*)?[ \t]*[ \t:=][ \t]*");
		sb.append("[^\\\\\n\r]*(\\\\\n[^\\\\\n\r]*)*");
		try {
			return Pattern.compile(sb.toString(), Pattern.MULTILINE + Pattern.CASE_INSENSITIVE);
		} catch (Exception e) {
			return Pattern.compile("^[ \t]*" + Pattern.quote(header), Pattern.MULTILINE + Pattern.CASE_INSENSITIVE);
		}
	}

	public FileLine getHeader(Pattern header) throws Exception {
		return getHeader(header, null);
	}

	public FileLine getHeader(String header, String clause) throws Exception {
		return getHeader(toFullHeaderPattern(header), clause == null ? null : Pattern.compile(Pattern.quote(clause)));
	}

	public FileLine getHeader(Pattern header, Pattern clause) throws Exception {
		FileLine fl = getHeader0(header, clause);
		if (fl != null)
			return fl;

		@SuppressWarnings("resource")
		Processor rover = this;
		while (rover.getPropertiesFile() == null)
			if (rover.parent == null) {
				return new FileLine(new File("ANONYMOUS"), 0, 0);
			} else
				rover = rover.parent;

		return new FileLine(rover.getPropertiesFile(), 0, 0);
	}

	private FileLine getHeader0(Pattern header, Pattern clause) throws Exception {
		FileLine fl;

		File f = getPropertiesFile();
		if (f != null) {
			// Find in "our" local file
			fl = findHeader(f, header, clause);
			if (fl != null)
				return fl;

			// Get the includes (actually should parse the header
			// to see if they override or only provide defaults?

			for (Iterator<File> iter = new ArrayDeque<>(getIncluded()).descendingIterator(); iter.hasNext();) {
				File file = iter.next();
				fl = findHeader(file, header);
				if (fl != null) {
					return fl;
				}
			}
		}
		// Ok, not on this level ...
		if (getParent() != null) {
			fl = getParent().getHeader(header, clause);
			if (fl != null)
				return fl;
		}

		// Ok, report the error on the sub file
		// Sometimes we do not have a file ...
		if (f == null && parent != null)
			f = parent.getPropertiesFile();

		if (f == null)
			return null;

		return new FileLine(f, 0, 0);
	}

	public static FileLine findHeader(File f, String header) throws IOException {
		return findHeader(f,
				Pattern.compile("^[ \t]*" + Pattern.quote(header), Pattern.MULTILINE + Pattern.CASE_INSENSITIVE));
	}

	public static FileLine findHeader(File f, Pattern header) throws IOException {
		return findHeader(f, header, null);
	}

	public static FileLine findHeader(File f, Pattern header, Pattern clause) throws IOException {
		if (f.isFile()) {
			String s = IO.collect(f);
			Matcher matcher = header.matcher(s);
			while (matcher.find()) {

				FileLine fl = new FileLine();
				fl.file = f;
				fl.start = matcher.start();
				fl.end = matcher.end();
				fl.length = fl.end - fl.start;
				fl.line = getLine(s, fl.start);

				if (clause != null) {

					Matcher mclause = clause.matcher(s);
					mclause.region(fl.start, fl.end);

					if (mclause.find()) {
						fl.start = mclause.start();
						fl.end = mclause.end();
					} else
						//
						// If no clause matches, maybe
						// we have merged headers
						//
						continue;
				}

				return fl;
			}
		}
		return null;
	}

	public static int getLine(String s, int index) {
		int n = 0;
		while (--index > 0) {
			char c = s.charAt(index);
			if (c == '\n') {
				n++;
			}
		}
		return n;
	}

	/**
	 * This method is about compatibility. New behavior can be conditionally
	 * introduced by calling this method and passing what version this behavior
	 * was introduced. This allows users of bnd to set the -upto instructions to
	 * the version that they want to be compatible with. If this instruction is
	 * not set, we assume the latest version.
	 */

	Version upto = null;

	public boolean since(Version introduced) {
		if (upto == null) {
			String uptov = getProperty(UPTO);
			if (uptov == null) {
				upto = Version.HIGHEST;
				return true;
			}
			if (!Version.VERSION.matcher(uptov).matches()) {
				error("The %s given version is not a version: %s", UPTO, uptov);
				upto = Version.HIGHEST;
				return true;
			}

			upto = new Version(uptov);
		}
		return upto.compareTo(introduced) >= 0;
	}

	/**
	 * Report the details of this processor. Should in general be overridden
	 *
	 * @param table
	 * @throws Exception
	 */

	public void report(Map<String,Object> table) throws Exception {
		table.put("Included Files", getIncluded());
		table.put("Base", getBase());
		table.put("Properties", getProperties0().entrySet());
	}

	/**
	 * Simplified way to check booleans
	 */

	public boolean is(String propertyName) {
		return isTrue(getProperty(propertyName));
	}

	/**
	 * Return merged properties. The parameters provide a list of property names
	 * which are concatenated in the output, separated by a comma. Not only are
	 * those property names looked for, also all property names that have that
	 * constant as a prefix, a '.', and then whatever (.*). The result is either
	 * null if nothing was found or a list of properties
	 */

	public String mergeProperties(String key) {
		return mergeProperties(key, ",");
	}

	public String mergeLocalProperties(String key) {
		if (since(About._3_3)) {
			return getProperty(makeWildcard(key), null, ",", false);
		} else
			return mergeProperties(key);
	}

	public String mergeProperties(String key, String separator) {
		if (since(About._2_4))
			return getProperty(makeWildcard(key), null, separator, true);
		else
			return getProperty(key);

	}

	private String makeWildcard(String key) {
		return key + ".*";
	}

	/**
	 * Get a Parameters from merged properties
	 */

	public Parameters getMergedParameters(String key) {
		return new Parameters(mergeProperties(key), this);
	}

	/**
	 * Add an element to an array, creating a new one if necessary
	 */

	public <T> T[] concat(Class<T> type, T[] prefix, T suffix) {
		@SuppressWarnings("unchecked")
		T[] result = (T[]) Array.newInstance(type, (prefix != null ? prefix.length : 0) + 1);
		if (result.length > 1) {
			System.arraycopy(prefix, 0, result, 0, result.length - 1);
		}
		result[result.length - 1] = suffix;
		return result;
	}

	/**
	 * Try to get a Jar from a file name/path or a url, or in last resort from
	 * the classpath name part of their files.
	 *
	 * @param name URL or filename relative to the base
	 * @param from Message identifying the caller for errors
	 * @return null or a Jar with the contents for the name
	 */
	public Jar getJarFromName(String name, String from) {
		File file = new File(name);
		if (!file.isAbsolute())
			file = new File(getBase(), name);

		if (file.exists())
			try {
				Jar jar = new Jar(file);
				addClose(jar);
				return jar;
			} catch (Exception e) {
				error("Exception in parsing jar file for %s: %s %s", from, name, e);
			}
		// It is not a file ...
		try {
			// Lets try a URL
			URL url = new URL(name);
			URLConnection connection = url.openConnection();
			try (InputStream in = connection.getInputStream()) {
				long lastModified = connection.getLastModified();
				if (lastModified == 0L)
					// We assume the worst :-(
					lastModified = System.currentTimeMillis();
				Jar jar = new Jar(fileName(url.getPath()), in, lastModified);
				addClose(jar);
				return jar;
			}
		} catch (IOException ee) {
			// ignore
		}
		return null;
	}

	private String fileName(String path) {
		int n = path.lastIndexOf('/');
		if (n > 0)
			return path.substring(n + 1);
		return path;
	}

	/**
	 * Return the name of the properties file
	 */

	public String _thisfile(String[] args) {
		if (propertiesFile == null) {
			error("${thisfile} executed on a processor without a properties file");
			return null;
		}

		return propertiesFile.getAbsolutePath().replaceAll("\\\\", "/");
	}

	/**
	 * Copy the settings of another processor
	 */
	public void getSettings(Processor p) {
		this.trace = p.isTrace();
		this.pedantic = p.isPedantic();
		this.exceptions = p.isExceptions();
	}

	/**
	 * Return a range expression for a filter from a version. By default this is
	 * based on consumer compatibility. You can specify a third argument (true)
	 * to get provider compatibility.
	 *
	 * <pre>
	 *  ${frange;1.2.3} ->
	 * (&(version>=1.2.3)(!(version>=2.0.0)) ${frange;1.2.3, true} ->
	 * (&(version>=1.2.3)(!(version>=1.3.0)) ${frange;[1.2.3,2.3.4)} ->
	 * (&(version>=1.2.3)(!(version>=2.3.4))
	 * </pre>
	 */
	public String _frange(String[] args) {
		if (args.length < 2 || args.length > 3) {
			error("Invalid filter range, 2 or 3 args ${frange;<version>[;true|false]}");
			return null;
		}

		String v = args[1];
		boolean isProvider = args.length == 3 && isTrue(args[2]);
		VersionRange vr;

		if (Verifier.isVersion(v)) {
			Version l = new Version(v);
			Version h = isProvider ? new Version(l.getMajor(), l.getMinor() + 1, 0)
					: new Version(l.getMajor() + 1, 0, 0);
			vr = new VersionRange(true, l, h, false);
		} else if (Verifier.isVersionRange(v)) {
			vr = new VersionRange(v);
		} else {
			error("The _frange parameter %s is neither a version nor a version range", v);
			return null;
		}

		return vr.toFilter();
	}

	public String _findfile(String args[]) {
		File f = getFile(args[1]);
		List<String> files = new ArrayList<>();
		tree(files, f, "", new Instruction(args[2]));
		return join(files);
	}

	void tree(List<String> list, File current, String path, Instruction instr) {
		if (path.length() > 0)
			path = path + "/";

		String subs[] = current.list();
		if (subs != null) {
			for (String sub : subs) {
				File f = new File(current, sub);
				if (f.isFile()) {
					if (instr.matches(sub) && !instr.isNegated())
						list.add(path + sub);
				} else
					tree(list, f, path + sub, instr);
			}
		}
	}

}

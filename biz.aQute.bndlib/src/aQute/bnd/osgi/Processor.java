package aQute.bnd.osgi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.osgi.util.promise.PromiseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.help.Syntax;
import aQute.bnd.help.SyntaxAnnotation;
import aQute.bnd.http.HttpClient;
import aQute.bnd.memoize.CloseableMemoize;
import aQute.bnd.memoize.Memoize;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.Registry;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.stream.MapStream;
import aQute.bnd.unmodifiable.Lists;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.collections.Iterables;
import aQute.lib.io.IO;
import aQute.lib.io.IOConstants;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.command.Command;
import aQute.libg.cryptography.Digester;
import aQute.libg.cryptography.SHA1;
import aQute.libg.generics.Create;
import aQute.libg.qtokens.QuotedTokenizer;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.reporter.Reporter;

public class Processor extends Domain implements Reporter, Registry, Constants, Closeable {
	private static final Logger	logger	= LoggerFactory.getLogger(Processor.class);
	public static Reporter		log;

	static {
		ReporterAdapter reporterAdapter = new ReporterAdapter(System.out);
		reporterAdapter.setTrace(true);
		reporterAdapter.setExceptions(true);
		reporterAdapter.setPedantic(true);
		log = reporterAdapter;
	}

	static final int									BUFFER_SIZE	= IOConstants.PAGE_SIZE * 1;

	final static ThreadLocal<Processor>					current		= new ThreadLocal<>();

	private static final Memoize<ExecutorGroup>	executors			= Memoize.supplier(ExecutorGroup::new);

	private static final Memoize<Random>		random				= Memoize.supplier(Random::new);
	public final static String					LIST_SPLITTER	= "\\s*,\\s*";
	final List<String>							errors			= new ArrayList<>();
	final List<String>							warnings		= new ArrayList<>();
	private final Set<Object>					basicPlugins		= Collections
		.newSetFromMap(new ConcurrentHashMap<>());
	private final Set<AutoCloseable>			toBeClosed			= Collections
		.newSetFromMap(new ConcurrentHashMap<>());

	private volatile CloseableMemoize<CL>		pluginLoader		= newPluginLoader();
	private volatile Memoize<PluginsContainer>	pluginsContainer	= newPluginsContainer();

	boolean										pedantic;
	boolean										trace;
	boolean										exceptions;
	boolean										fileMustExist	= true;

	private File								base			= new File("").getAbsoluteFile();
	private URI									baseURI			= base.toURI();

	Properties									properties;
	String										profile;
	private Macro								replacer;
	private long								lastModified;
	private File								propertiesFile;
	private boolean								fixup			= true;
	private Processor							parent;
	private final CopyOnWriteArrayList<File>	included		= new CopyOnWriteArrayList<>();

	Collection<String>							filter;
	Boolean										strict;
	boolean										fixupMessages;

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
			sl.file(IO.absolutePath(file));
			sl.line(line);
			sl.length(length);
		}
	}

	public Processor() {
		this(new UTF8Properties(), false);
	}

	public Processor(Properties props) {
		this(props, true);
	}

	public Processor(Processor parent) {
		this(parent, parent.getProperties0(), true);
	}

	public Processor(Properties props, boolean wrap) {
		this.properties = wrap ? new UTF8Properties(props) : props;
	}

	public Processor(Processor parent, Properties props, boolean wrap) {
		this(props, wrap);
		this.parent = parent;
		if (parent != null) {
			updateModified(parent.lastModified(), "parent");
		}
	}

	public void setParent(Processor parent) {
		this.parent = parent;
		Properties updated = (parent != null) ? new UTF8Properties(parent.getProperties0()) : new UTF8Properties();
		updated.putAll(getProperties0());
		properties = updated;
		propertiesChanged();
	}

	public Processor getParent() {
		return parent;
	}

	public Processor getTop() {
		if (getParent() == null)
			return this;
		return getParent().getTop();
	}

	public void getInfo(Reporter processor, String prefix) {
		if (prefix == null)
			prefix = getBase() + " :";
		if (isFailOk())
			addAll(warnings, processor.getErrors(), prefix, processor);
		else
			addAll(errors, processor.getErrors(), prefix, processor);
		addAll(warnings, processor.getWarnings(), prefix, processor);

		processor.getErrors()
			.clear();
		processor.getWarnings()
			.clear();

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
					for (Field f : newer.getClass()
						.getFields()) {
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

	@Override
	public SetLocation warning(String string, Object... args) {
		fixupMessages = false;
		Processor p = current();
		String s = formatArrays(string, args);
		if (!p.warnings.contains(s))
			p.warnings.add(s);
		p.signal();
		return p.location(s);
	}

	@Override
	public SetLocation error(String string, Object... args) {
		fixupMessages = false;
		Processor p = current();
		try {

			//
			// Make Throwables into a string that shows their causes
			//

			for (int i = 0; i < args.length; i++) {
				if (args[i] instanceof Throwable t) {
					args[i] = Exceptions.causes(t);
				}
			}
			if (p.isFailOk())
				return p.warning(string, args);
			String s = formatArrays(string, args);
			if (!p.errors.contains(s))
				p.errors.add(s);
			return p.location(s);
		} finally {
			p.signal();
		}
	}

	/**
	 * @deprecated Use SLF4J Logger.info() instead.
	 */
	@Override
	@Deprecated
	public void progress(float progress, String format, Object... args) {
		Logger l = getLogger();
		if (l.isInfoEnabled()) {
			String message = formatArrays(format, args);
			if (progress > 0)
				l.info("[{}] {}", (int) progress, message);
			else
				l.info("{}", message);
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
			p.getLogger()
				.info("Reported exception {}", Exceptions.causes(t), t);
		} else {
			p.getLogger()
				.debug("Reported exception {}", Exceptions.causes(t), t);
		}
		if (p.exceptions) {
			printExceptionSummary(t, System.err);
		}
		// unroll InvocationTargetException
		t = Exceptions.unrollCause(t, InvocationTargetException.class);
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
			out.println("Root cause: " + e.getMessage() + "   :" + e.getClass()
				.getName());
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

	@Override
	public List<String> getWarnings() {
		fixupMessages();
		return warnings;
	}

	@Override
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

	public void addClose(AutoCloseable closeable) {
		assert closeable != null;
		toBeClosed.add(closeable);
	}


	public void removeClose(AutoCloseable closeable) {
		assert closeable != null;
		toBeClosed.remove(closeable);
	}

	@Override
	public boolean isPedantic() {
		Processor p = current();
		return p.pedantic;
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
	@Override
	public <T> List<T> getPlugins(Class<T> clazz) {
		List<T> list = getPlugins().getPlugins(clazz);
		return list;
	}

	/**
	 * Returns the first plugin it can find of the given type.
	 *
	 * @param <T>
	 * @param clazz
	 */
	@Override
	public <T> T getPlugin(Class<T> clazz) {
		T plugin = getPlugins().getPlugin(clazz);
		return plugin;
	}

	/**
	 * Return the PluginsContainer. Plugins are defined with the -plugin
	 * command. They are class names, optionally associated with attributes.
	 * Plugins can implement the Plugin interface to see these attributes. Any
	 * object can be a plugin.
	 */
	public PluginsContainer getPlugins() {
		PluginsContainer pc = pluginsContainer.get();
		return pc;
	}

	/**
	 * Return a memoizer for the PluginsContainer.
	 */
	private Memoize<PluginsContainer> newPluginsContainer() {
		Memoize<PluginsContainer> supplier = Memoize.supplier(() -> {
			PluginsContainer pc = new PluginsContainer();
			pc.init(this);
			return pc;
		});
		// do postInit outside of above memoizer to allow reentrant
		// access to the inited PluginsContainer
		AtomicBoolean postInit = new AtomicBoolean(true);
		return Memoize.predicateSupplier(supplier, pc -> {
			// only first caller gets to do postInit
			if (postInit.getAndSet(false)) {
				pc.postInit(this);
			}
			return true; // always memoize the PluginsContainer
		});
	}

	/**
	 * Is called after the PluginsContainer is initialized.
	 *
	 * @param pluginsContainer
	 */
	protected void addExtensions(PluginsContainer pluginsContainer) {

	}

	protected void setTypeSpecificPlugins(PluginsContainer pluginsContainer) {
		pluginsContainer.add(getExecutor());
		pluginsContainer.add(getPromiseFactory());
		pluginsContainer.add(random);
		pluginsContainer.addAll(basicPlugins);
	}

	/**
	 * Set the initial parameters of a plugin
	 *
	 * @param plugin
	 * @param map
	 */
	protected <T> T customize(T plugin, Attrs map, PluginsContainer pluginsContainer) {
		if (plugin instanceof Plugin pluginPlugin) {
			try {
				pluginPlugin.setReporter(this);
			} catch (Exception e) {
				exception(e, "While setting reporter on plugin %s", pluginPlugin);
			}
			try {
				if (map == null) {
					map = Attrs.EMPTY_ATTRS;
				}
				pluginPlugin.setProperties(map);
			} catch (Exception e) {
				exception(e, "While setting properties %s on plugin %s", map, pluginPlugin);
			}
		}
		if (plugin instanceof RegistryPlugin registryPlugin) {
			try {
				registryPlugin.setRegistry(pluginsContainer);
			} catch (Exception e) {
				exception(e, "While setting registry on plugin %s", registryPlugin);
			}
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
		String v = getProperty(Constants.FAIL_OK, null);
		return v != null && v.equalsIgnoreCase("true");
	}

	public File getBase() {
		return base;
	}

	public URI getBaseURI() {
		return baseURI;
	}

	public void setBase(File base) {
		if (base == null) {
			this.base = null;
			baseURI = null;
		} else {
			this.base = base.getAbsoluteFile();
			baseURI = base.toURI();
		}
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
	 * Used to provide verbose commands
	 */
	@Override
	public void trace(String msg, Object... parms) {

		Processor p = current();
		if (p.trace) {
			String s = formatArrays(msg, parms);
			System.out.println(s);
		}
	}

	public <T> List<T> newList() {
		return new ArrayList<>();
	}

	public <T> Set<T> newSet() {
		return new TreeSet<>();
	}

	public static <K, V> Map<K, V> newMap() {
		return new LinkedHashMap<>();
	}

	public static <K, V> Map<K, V> newHashMap() {
		return new LinkedHashMap<>();
	}

	public <T> List<T> newList(Collection<T> t) {
		return new ArrayList<>(t);
	}

	public <T> Set<T> newSet(Collection<T> t) {
		return new TreeSet<>(t);
	}

	public <K, V> Map<K, V> newMap(Map<K, V> t) {
		return new LinkedHashMap<>(t);
	}

	@Override
	public void close() throws IOException {
		toBeClosed.forEach(IO::close);

		clearPlugins();

		toBeClosed.clear();
	}

	private void clearPlugins() {
		CloseableMemoize<CL> outgoingPluginLoader = pluginLoader;
		Memoize<PluginsContainer> outgoingPluginsContainer = pluginsContainer;
		pluginLoader = newPluginLoader();
		pluginsContainer = newPluginsContainer();
		outgoingPluginsContainer.ifPresent(PluginsContainer::close);
		IO.close(outgoingPluginLoader);
	}

	public String _basedir(@SuppressWarnings("unused") String args[]) {
		if (base == null)
			throw new IllegalArgumentException("No base dir set");

		return IO.absolutePath(base);
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

		return IO.absolutePath(pf.getParentFile());
	}

	static final String _uriHelp = "${uri;<uri>[;<baseuri>]}, Resolve the uri against the baseuri. baseuri defaults to the processor base.";

	public String _uri(String args[]) throws Exception {
		Macro.verifyCommand(args, _uriHelp, null, 2, 3);

		URI uri = new URI(args[1]);
		if (!uri.isAbsolute() || uri.getScheme()
			.equals("file")) {
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

	public String _fileuri(String args[]) throws Exception {
		return getReplacer()._fileuri(args);
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

	public String getUnexpandedProperty(String key) {
		if (filter != null && filter.contains(key)) {
			Object raw = getProperties().get(key);
			return (raw instanceof String string) ? string : null;
		}
		return getProperties().getProperty(key);
	}

	public void mergeProperties(File file, boolean overwrite) {
		if (file.isFile()) {
			try {
				Properties properties = loadProperties(file);
				mergeProperties(properties, overwrite);
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

	public void mergeProperties(Properties properties, boolean overwrite) {
		for (String key : Iterables.iterable(properties.propertyNames(), String.class::cast)) {
			String value = properties.getProperty(key);
			if (overwrite || !getProperties().containsKey(key))
				setProperty(key, value);
		}
	}

	public void setProperties(Properties properties) {
		setProperties(getBase(), properties);
	}

	public void setProperties(File base, Properties properties) {
		doIncludes(base, properties);
		getProperties0().putAll(properties);
		mergeProperties(Constants.INIT); // execute macros in -init
		getProperties0().remove(Constants.INIT);
		propertiesChanged();
	}

	public void addProperties(File file) throws Exception {
		addIncluded(file);
		Properties p = loadProperties(file);
		setProperties(p);
	}

	public void addProperties(Map<?, ?> properties) {
		properties.forEach((k, v) -> setProperty(k.toString(), String.valueOf(v)));
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
						value = value.substring(1)
							.trim();
					} else if (value.startsWith("~")) {
						// Don't overwrite properties!
						overwrite = false;
						value = value.substring(1)
							.trim();
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
							try (Resource resource = Resource.fromURL(url, getPlugin(HttpClient.class))) {
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
			return;
		}
		updateModified(file.lastModified(), file.toString());
		Properties sub;
		if (Strings.endsWithIgnoreCase(file.getName(), ".mf")) {
			try (InputStream in = IO.stream(file)) {
				sub = getManifestAsProperties(in);
			}
		} else
			sub = loadProperties(file);

		doIncludes(file.getParentFile(), sub);
		// take care regarding overwriting properties
		for (Map.Entry<?, ?> entry : sub.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();

			if (overwrite || !target.containsKey(key)) {
				target.setProperty(key, value);
			} else if (extensionName != null) {
				String extensionKey = key + "." + extensionName;
				if (!target.containsKey(extensionKey))
					target.setProperty(extensionKey, value);
			}
		}
	}

	public void unsetProperty(String string) {
		getProperties().remove(string);

	}

	public boolean refresh() {
		clearPlugins(); // We always refresh our plugins

		if (propertiesFile == null)
			return false;

		boolean changed = updateModified(propertiesFile.lastModified(), "properties file");
		for (File file : getIncluded()) {
			changed |= !file.exists() || updateModified(file.lastModified(), "include file: " + file);
		}

		profile = null; // Used in property access

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
	}

	public void propertiesChanged() {
		Processor p = getParent();
		if (p != null) {
			updateModified(p.lastModified(), "propertiesChanged");
		}

		clearPlugins(); // force plugins to reload since properties have changed
	}

	/**
	 * Set the properties by file. Setting the properties this way will also set
	 * the base for this analyzer. After reading the properties, this will call
	 * setProperties(Properties) which will handle the includes.
	 *
	 * @param propertiesFile
	 */
	public void setProperties(File propertiesFile) {
		if (propertiesFile == null)
			return;
		propertiesFile = propertiesFile.getAbsoluteFile();
		setProperties(propertiesFile, propertiesFile.getParentFile());
	}

	public void setProperties(File propertiesFile, File base) {
		this.propertiesFile = propertiesFile.getAbsoluteFile();
		setBase(base);
		try {
			if (propertiesFile.isFile()) {
				// System.err.println("Loading properties " + propertiesFile);
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
			Object raw = getProperties().get(key);
			return (raw instanceof String string) ? string : deflt;
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
		if (ins.isLiteral()) {
			return getLiteralProperty(ins.getLiteral(), deflt, this, inherit);
		}

		return getWildcardProperty(deflt, separator, inherit, ins);
	}

	private String getWildcardProperty(String deflt, String separator, boolean inherit, Instruction ins) {
		// Handle a wildcard key, make sure they're sorted
		// for consistency
		String result = stream(inherit).filter(ins::matches)
			.sorted()
			.map(k -> getLiteralProperty(k, null, this, inherit))
			.filter(v -> (v != null) && !v.isEmpty())
			.collect(Strings.joining(separator, "", "", deflt));
		return result;
	}

	private String getLiteralProperty(String key, String deflt, Processor source, boolean inherit) {
		String value = null;
		// Use the key as is first, if found ok

		for (Processor proc = source; proc != null; proc = proc.getParent()) {
			Object raw = proc.getProperties()
				.get(key);
			if (raw != null) {
				if (raw instanceof String string) {
					value = string;
				} else if (isPedantic()) {
					warning("Key '%s' has a non-String value: %s:%s", key, raw.getClass()
						.getName(), raw);
				}
				source = proc;
				break;
			}

			if (!inherit) {
				break;
			}
			Collection<String> keyFilter = proc.filter;
			if ((keyFilter != null) && (keyFilter.contains(key))) {
				break;
			}
		}
		//
		// Check if we can find a replacement through the
		// replacer, which takes profiles into account
		if (value == null) {
			value = getReplacer().getMacro(key, null);
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
	 * @throws IOException
	 */
	UTF8Properties loadProperties0(File file) throws IOException {
		try {
			UTF8Properties p = new UTF8Properties();
			p.load(file, this, Constants.OSGI_SYNTAX_HEADERS);
			return p.replaceHere(file.getParentFile());
		} catch (Exception e) {
			error("Error during loading properties file: %s, error: %s", file, e);
			return new UTF8Properties();
		}
	}

	/**
	 * Replace a string in all the values of the map. This can be used to
	 * preassign variables that change. I.e. the base directory ${.} for a
	 * loaded properties
	 */
	public static Properties replaceAll(Properties p, String pattern, String replacement) {
		Pattern regex = Pattern.compile(pattern);
		UTF8Properties result = MapStream.of(p)
			.mapValue(value -> regex.matcher((String) value)
				.replaceAll(replacement))
			.collect(MapStream.toMap((u, v) -> v, UTF8Properties::new));
		return result;
	}

	/**
	 * Print a standard Map based OSGi header.
	 *
	 * @param exports map { name => Map { attribute|directive => value } }
	 * @return the clauses
	 * @throws IOException
	 */
	public static String printClauses(Map<?, ? extends Map<?, ?>> exports) throws IOException {
		return printClauses(exports, false);
	}

	public static String printClauses(Map<?, ? extends Map<?, ?>> exports,
		@SuppressWarnings("unused") boolean checkMultipleVersions) throws IOException {
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (Entry<?, ? extends Map<?, ?>> entry : exports.entrySet()) {
			String name = entry.getKey()
				.toString();
			Map<?, ?> clause = entry.getValue();

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

	public static void printClause(Map<?, ?> map, StringBuilder sb) throws IOException {
		if (map instanceof Attrs attrs) {
			for (Entry<String, String> entry : attrs.entrySet()) {
				String key = entry.getKey();
				// Skip directives we do not recognize
				if (!AttributeClasses.MANIFEST.test(key))
					continue;

				sb.append(";");
				attrs.append(sb, entry);
			}
		} else {
			for (Entry<?, ?> entry : map.entrySet()) {
				String key = entry.getKey()
					.toString();
				// Skip directives we do not recognize
				if (!AttributeClasses.MANIFEST.test(key))
					continue;

				sb.append(";");
				sb.append(key);
				sb.append("=");
				String value = ((String) entry.getValue()).trim();
				quote(sb, value);
			}
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
		if (getParent() == null || !inherit) {
			result = new TreeSet<>();
		} else {
			result = getParent().getPropertyKeys(inherit);
			if (filter != null) {
				result.removeAll(filter);
			}
		}
		for (Object o : getProperties0().keySet()) {
			result.add(o.toString());
		}
		return result;
	}

	public boolean updateModified(long time, String reason) {
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
	 * Add or overwrite a new property.
	 *
	 * @param key
	 * @param value
	 */
	public void setProperty(String key, String value) {
		getProperties().put(normalizeKey(key), value);
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
		for (Object object : manifest.getMainAttributes()
			.keySet()) {
			Attributes.Name key = (Attributes.Name) object;
			String value = manifest.getMainAttributes()
				.getValue(key);
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

	public static void split(String s, Collection<String> collection) {
		Strings.splitAsStream(s)
			.forEachOrdered(collection::add);
	}

	public static Collection<String> split(String s) {
		return Strings.split(s);
	}

	public static Collection<String> split(String s, String splitter) {
		return Strings.split(splitter, s);
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
	 * @param file
	 */
	public String normalize(String file) {
		file = IO.normalizePath(file);
		String path = IO.absolutePath(base);
		int len = path.length();
		if (file.startsWith(path) && file.charAt(len) == '/') {
			return file.substring(len + 1);
		}
		return file;
	}

	public String normalize(File file) {
		return normalize(file.getAbsolutePath());
	}

	public static String removeDuplicateMarker(String key) {
		int i = key.length() - 1;
		while ((i >= 0) && (key.charAt(i) == DUPLICATE_MARKER)) {
			--i;
		}
		return key.substring(0, i + 1);
	}

	public static boolean isDuplicate(String key) {
		return key.indexOf(DUPLICATE_MARKER, key.length() - 1) >= 0;
	}

	public void setTrace(boolean x) {
		trace = x;
	}

	public static class CL extends ActivelyClosingClassLoader {
		static {
			ClassLoader.registerAsParallelCapable();
		}

		public CL(Processor p) {
			super(p, p.getClass()
				.getClassLoader());
		}

		@Override
		@Deprecated
		public URL[] getURLs() {
			return new URL[0];
		}

		@Override
		public void add(File file) {
			super.add(file);
		}

	}

	protected CL getLoader() {
		return pluginLoader.get();
	}


	private CloseableMemoize<CL> newPluginLoader() {
		return CloseableMemoize.closeableSupplier(() -> {
			CL pluginLoader = new CL(this);
			if (IO.isWindows() && isInteractive()) {
				pluginLoader.autopurge(TimeUnit.SECONDS.toNanos(5L));
			}
			return pluginLoader;
		});
	}
	/*
	 * Check if this is a valid project.
	 */
	public boolean exists() {
		return base != null && base.isDirectory() && propertiesFile != null && propertiesFile.isFile();
	}

	@Override
	public boolean isOk() {
		return isFailOk() || getErrors().isEmpty();
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

		Instructions instrs = new Instructions();
		fixup.forEach((k, v) -> instrs.put(Instruction.legacy(k), v));

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
					if (pat.matcher(i.next())
						.find()) {
						i.remove();
						match = true;
					}
				}
				for (Iterator<String> i = warnings.iterator(); i.hasNext();) {
					if (pat.matcher(i.next())
						.find()) {
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
		return getErrors().isEmpty() && getWarnings().isEmpty();
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
		return getPlugins().isMissingPlugin(name);
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
		StringBuilder sb = new StringBuilder(parts.length * 16);
		boolean lastSlash = true;
		for (String part : parts) {
			final int partlen = part.length();
			if (partlen == 0) {
				continue;
			}
			if (!lastSlash) {
				sb.append('/');
				lastSlash = true;
			}
			for (int i = 0; i < partlen; i++) {
				char c = part.charAt(i);
				if (lastSlash) {
					if (c != '/') {
						sb.append(c);
						lastSlash = false;
					}
				} else {
					sb.append(c);
					if (c == '/') {
						lastSlash = true;
					}
				}
			}
		}
		if (lastSlash) {
			int sblen = sb.length();
			if (sblen > 0) {
				sb.setLength(sblen - 1);
			}
		}

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

		if (object.getClass()
			.isArray()) {
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

	public synchronized Class<?> getClass(String type, File jar) throws Exception {
		CL cl = getLoader();
		cl.add(jar);
		return cl.loadClass(type);
	}

	public boolean isTrace() {
		Processor p = current();
		return p.trace;
	}

	private static final Pattern DURATION_P = Pattern
		.compile("\\s*(\\d+)\\s*(NANOSECONDS|MICROSECONDS|MILLISECONDS|SECONDS|MINUTES|HOURS|DAYS)?");

	public static long getDuration(String tm, long dflt) {
		if (tm == null)
			return dflt;

		Matcher m = DURATION_P.matcher(tm.toUpperCase(Locale.ROOT));
		if (m.matches()) {
			long duration = Long.parseLong(m.group(1));
			String u = m.group(2);
			TimeUnit unit = (u != null) ? TimeUnit.valueOf(u) : TimeUnit.MILLISECONDS;
			return unit.toMillis(duration);
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

		Random random = Processor.random.get();

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
		return executors.get()
			.getExecutor();
	}

	public static ScheduledExecutorService getScheduledExecutor() {
		return executors.get()
			.getScheduledExecutor();
	}

	public static PromiseFactory getPromiseFactory() {
		return executors.get()
			.getPromiseFactory();
	}

	/**
	 * These plugins are added to the total list of plugins. The separation is
	 * necessary because the list of plugins is refreshed now and then so we
	 * need to be able to add them at any moment in time.
	 *
	 * @param plugin
	 */
	public void addBasicPlugin(Object plugin) {
		basicPlugins.add(plugin);
		pluginsContainer.ifPresent(pc -> pc.add(plugin));
	}

	public void removeBasicPlugin(Object plugin) {
		basicPlugins.remove(plugin);
		pluginsContainer.ifPresent(pc -> pc.remove(plugin));
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
		setProperty(key, value);
	}

	Stream<String> stream() {
		return stream(true);
	}

	private Stream<String> stream(boolean inherit) {
		return StreamSupport.stream(iterable(inherit, Objects::nonNull).spliterator(), false);
	}

	@Override
	public Iterator<String> iterator() {
		return iterable(true, Objects::nonNull).iterator();
	}

	@Override
	public Spliterator<String> spliterator() {
		return iterable(true, Objects::nonNull).spliterator();
	}

	private Iterable<String> iterable(boolean inherit, Predicate<String> keyFilter) {
		Set<Object> first = getProperties0().keySet();
		Iterable<? extends Object> second;
		if (getParent() == null || !inherit) {
			second = Collections.emptyList();
		} else {
			second = getParent().iterable(inherit,
				(filter == null) ? keyFilter : keyFilter.and(key -> !filter.contains(key)));
		}

		Iterable<String> iterable = Iterables.distinct(first, second, o -> (o instanceof String string) ? string : null,
			keyFilter);
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

		@Override
		public SetLocation file(String file) {
			this.file = (file != null) ? IO.normalizePath(file) : null;
			return this;
		}

		@Override
		public SetLocation header(String header) {
			this.header = header;
			return this;
		}

		@Override
		public SetLocation context(String context) {
			this.context = context;
			return this;
		}

		@Override
		public SetLocation method(String methodName) {
			this.methodName = methodName;
			return this;
		}

		@Override
		public SetLocation line(int n) {
			this.line = n;
			return this;
		}

		@Override
		public SetLocation reference(String reference) {
			this.reference = reference;
			return this;
		}

		@Override
		public SetLocation details(Object details) {
			this.details = details;
			return this;
		}

		@Override
		public Location location() {
			return this;
		}

		@Override
		public SetLocation length(int length) {
			this.length = length;
			return this;
		}

	}

	public SetLocation setLocation(String header, String clause, SetLocation setLocation) {
		try {
			FileLine info = getHeader(header, clause);
			if (info != null) {
				info.set(setLocation);
			} else {
				setLocation.header(header)
					.context(clause);
			}
		} catch (Exception e) {
			exception(e, "unexpected exception in setLocation");
		}
		return setLocation;
	}

	private SetLocation location(String s) {
		SetLocationImpl loc = new SetLocationImpl(s);
		locations.add(loc);
		return loc;
	}

	@Override
	public Location getLocation(String msg) {
		assert msg != null : "Must provide message";
		for (Location l : locations)
			if ((l.message != null) && msg.equals(l.message))
				return l;

		return null;
	}

	/**
	 * Get a header relative to this processor, taking its parents and includes
	 * into account.
	 *
	 * @param header
	 * @throws IOException
	 */
	public FileLine getHeader(String header) throws Exception {
		return getHeader(
			Pattern.compile("^[ \t]*".concat(Pattern.quote(header)), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE));
	}

	public static Pattern toFullHeaderPattern(String header) {
		StringBuilder sb = new StringBuilder();
		sb.append("^[ \t]*(")
			.append(header)
			.append(")(\\.[^\\s:=]*)?[ \t]*[ \t:=][ \t]*");
		sb.append("[^\\\\\n\r]*(\\\\\n[^\\\\\n\r]*)*");
		try {
			return Pattern.compile(sb.toString(), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		} catch (Exception e) {
			return Pattern.compile("^[ \t]*".concat(Pattern.quote(header)),
				Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		}
	}

	public FileLine getHeader(Pattern header) throws Exception {
		return getHeader(header, null);
	}

	public FileLine getHeader(String header, String clause) throws Exception {
		return getHeader(toFullHeaderPattern(header), clause == null ? null : Pattern.compile(clause, Pattern.LITERAL));
	}

	public FileLine getHeader(Pattern header, Pattern clause) throws Exception {
		FileLine fl = getHeader0(header, clause);
		if (fl != null)
			return fl;

		@SuppressWarnings("resource")
		Processor rover = this;
		while (rover.getPropertiesFile() == null)
			if (rover.getParent() == null) {
				return new FileLine(new File("ANONYMOUS"), 0, 0);
			} else
				rover = rover.getParent();

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
			// to see if they overwrite or only provide defaults?

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
		if (f == null && getParent() != null)
			f = getParent().getPropertiesFile();

		if (f == null)
			return null;

		return new FileLine(f, 0, 0);
	}

	public static FileLine findHeader(File f, String header) throws IOException {
		return findHeader(f,
			Pattern.compile("^[ \t]*".concat(Pattern.quote(header)), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE));
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
			if (!Version.VERSION.matcher(uptov)
				.matches()) {
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

	public void report(Map<String, Object> table) throws Exception {
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
			try (Resource resource = Resource.fromURL(url, getPlugin(HttpClient.class))) {
				Jar jar = Jar.fromResource(fileName(url.getPath()), resource);
				if (jar.lastModified() <= 0L) {
					// We assume the worst :-(
					jar.updateModified(System.currentTimeMillis(), "use current time");
				}
				addClose(jar);
				return jar;
			}
		} catch (IOException ee) {
			// ignore
		} catch (Exception ee) {
			throw Exceptions.duck(ee);
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

		return IO.absolutePath(propertiesFile);
	}

	/**
	 * Copy the settings of another processor
	 */
	public void getSettings(Processor p) {
		this.trace = p.isTrace();
		this.pedantic = p.isPedantic();
		this.exceptions = p.isExceptions();
	}

	static final String _frangeHelp = "${frange;<version>[;true|false]}";

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
			error("Invalid filter range, 2 or 3 args" + _frangeHelp);
			return null;
		}

		String v = args[1];
		boolean isProvider = args.length == 3 && isTrue(args[2]);
		VersionRange vr;

		if (Verifier.isVersion(v)) {
			Version l = new Version(v);
			Version h = isProvider ? l.bumpMinor() : l.bumpMajor();
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
			path = path.concat("/");

		for (String sub : IO.list(current)) {
			File f = new File(current, sub);
			if (f.isFile()) {
				if (instr.matches(sub) ^ instr.isNegated())
					list.add(path + sub);
			} else
				tree(list, f, path + sub, instr);
		}
	}

	/**
	 * Return an instance of an interface where each method is mapped to an
	 * instruction available from this Processor. See {@link SyntaxAnnotation}
	 * for how to annotate this interface.
	 */
	public <T> T getInstructions(Class<T> type) {
		return Syntax.getInstructions(this, type);
	}

	/**
	 * Return if this is an interactive environment like Eclipse or runs in
	 * batch mode. If interactive, things can get refreshed.
	 */
	public boolean isInteractive() {
		if (getParent() != null) {
			return getParent().isInteractive();
		}
		return false;
	}

	@Override
	public Parameters getParameters(String key, boolean allowDuplicates) {
		return new Parameters(get(key), this, allowDuplicates);
	}

	public String system(boolean allowFail, String command, String input) throws IOException, InterruptedException {
		List<String> args;
		if (IO.isWindows()) {
			args = Lists.of("cmd", "/c", Command.windowsQuote(command));
		} else {
			args = new QuotedTokenizer(command, " \t", false, true).stream()
				.filter(token -> !token.isEmpty())
				.collect(toList());
		}

		Process process = new ProcessBuilder(args).directory(getBase())
			.start();

		try (OutputStream stdin = process.getOutputStream()) {
			if (input != null) {
				IO.store(input, stdin, UTF_8);
			}
		}

		String out = IO.collect(process.getInputStream(), UTF_8);
		String err = IO.collect(process.getErrorStream(), UTF_8);

		int exitValue = process.waitFor();
		if (exitValue == 0) {
			return out.trim();
		}

		if (allowFail) {
			warning("System command %s failed with exit code %d (allowed)", command, exitValue);
		} else {
			error("System command %s failed with exit code %d: %s%n---%n%s", command, exitValue, out, err);
		}
		return null;
	}

	public String system(String command, String input) throws IOException, InterruptedException {
		boolean allowFail = false;
		command = command.trim();
		if (command.startsWith("-")) {
			command = command.substring(1);
			allowFail = true;
		}
		return system(allowFail, command, input);
	}

	public String getJavaExecutable(String java) {
		String path = getProperty(requireNonNull(java));
		if ((path == null) || path.equals(java)) {
			return IO.getJavaExecutablePath(java);
		}
		return path;
	}

	/**
	 * Return a parameters that contains the merged properties of the given key
	 * and that is decorated by the merged properties of the key + '+',
	 * optionally including literals, and decorated by the merged properties of
	 * the key + '++', always including literals.
	 *
	 * @param key The key of the property
	 */

	public Parameters decorated(String key, boolean literalsIncluded) {
		Parameters parameters = getMergedParameters(key);
		Instructions decorator = new Instructions(mergeProperties(key + "+"));
		decorator.decorate(parameters, literalsIncluded);
		decorator = new Instructions(mergeProperties(key + "++"));
		decorator.decorate(parameters, true);
		return parameters;
	}

	public Parameters decorated(String key) {
		return decorated(key, false);
	}

	public synchronized String getProfile() {
		if (profile == null) {
			profile = "cycle";
			profile = getProperty(Constants.PROFILE);
		}
		return profile;
	}

	/**
	 * A checksum based on the values of the properties
	 *
	 * @return A checksum based on the values of the properties
	 */
	public String getChecksum() {
		try (Processor p = new Processor(this)) {
			p.setProperty(Constants.TSTAMP, "0");

			Properties flattenedProperties = p.getFlattenedProperties();
			Digester<SHA1> digester = SHA1.getDigester();

			@SuppressWarnings({
				"unchecked", "rawtypes"
			})
			Set<String> keySet = new TreeSet<>((Set) flattenedProperties.keySet());
			keySet.forEach(k -> {
				try {
					byte[] bytes = k.getBytes(StandardCharsets.UTF_8);
					digester.write(bytes);
					String s = flattenedProperties.getProperty(k);
					if (s == null)
						return;

					bytes = s.getBytes(StandardCharsets.UTF_8);
					digester.write(bytes);
				} catch (Exception e) {
					throw Exceptions.duck(e);
				}
			});
			String checksum = digester.digest()
				.asHex();
			return checksum;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Return a list of all files that provide the properties for this
	 * Processor. This includes its own properties file, all included files, and
	 * the same for its ancestor Processor.
	 * <p>
	 * The order of the list is parent?.getSelfAndAncestors(), includes,
	 * properties file
	 *
	 * @return a list of files that this processor depends on
	 */
	public List<File> getSelfAndAncestors() {
		List<File> l = new ArrayList<>();
		return getSelfAndAncestors(l);
	}

	private List<File> getSelfAndAncestors(List<File> l) {
		if (getParent() != null)
			getParent().getSelfAndAncestors(l);
		l.addAll(getIncluded());

		File f = getPropertiesFile();
		if (f != null)
			l.add(f);
		return l;
	}

	/**
	 * Set the properties file but do **not** load the properties.
	 *
	 * @param source the properties file
	 */
	public void setPropertiesFile(File source) {
		this.propertiesFile = source;
	}

	/**
	 * Answer true if any of the property keys is set as a property
	 *
	 * @param keys list of property keys
	 * @return true if any of the property values is set
	 */
	public boolean isPropertySet(Set<String> keys) {
		for (String key : keys) {
			if (getProperty(key) != null)
				return true;
		}
		return false;
	}

	/**
	 * Search a property based on different names and return a default if not
	 * found
	 */

	public String getProperty(Domain domain, String deflt, String... keys) {
		for (String k : keys) {
			String value = domain.get(k);
			if (value != null)
				return value;
		}
		return deflt;
	}

	/**
	 * Search an int property on different names, return a default if not found
	 * and report invalid format
	 */
	public int getInt(Domain domain, String reason, int deflt, String... keys) {
		for (String k : keys) {
			String value = domain.get(k);
			if (value != null) {
				try {
					return Integer.parseInt(value);
				} catch (NumberFormatException e) {
					error("Expected integer. %s %s=%s in %s", reason, k, value, domain);
				}
			}
		}
		return deflt;
	}

}

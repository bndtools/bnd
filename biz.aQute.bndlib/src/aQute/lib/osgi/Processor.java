package aQute.lib.osgi;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.jar.*;
import java.util.regex.*;

import aQute.bnd.service.*;
import aQute.lib.collections.*;
import aQute.lib.io.*;
import aQute.libg.generics.*;
import aQute.libg.header.*;
import aQute.service.reporter.*;

public class Processor extends Domain implements Reporter, Registry, Constants, Closeable {

	static ThreadLocal<Processor>	current			= new ThreadLocal<Processor>();
	static ExecutorService			executor		= Executors.newCachedThreadPool();
	static Random					random			= new Random();

	// TODO handle include files out of date
	// TODO make splitter skip eagerly whitespace so trim is not necessary
	public final static String		LIST_SPLITTER	= "\\s*,\\s*";
	final List<String>				errors			= new ArrayList<String>();
	final List<String>				warnings		= new ArrayList<String>();
	final Set<Object>				basicPlugins	= new HashSet<Object>();
	private final Set<Closeable>	toBeClosed		= new HashSet<Closeable>();
	Set<Object>						plugins;

	boolean							pedantic;
	boolean							trace;
	boolean							exceptions;
	boolean							fileMustExist	= true;

	private File					base			= new File("").getAbsoluteFile();

	Properties						properties;
	private Macro					replacer;
	private long					lastModified;
	private File					propertiesFile;
	private boolean					fixup			= true;
	long							modified;
	Processor						parent;
	List<File>						included;

	CL								pluginLoader;
	Collection<String>				filter;
	HashSet<String>					missingCommand;

	public Processor() {
		properties = new Properties();
	}

	public Processor(Properties parent) {
		properties = new Properties(parent);
	}

	public Processor(Processor child) {
		this(child.properties);
		this.parent = child;
	}

	public void setParent(Processor processor) {
		this.parent = processor;
		Properties ext = new Properties(processor.properties);
		ext.putAll(this.properties);
		this.properties = ext;
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
		if (isFailOk())
			addAll(warnings, processor.getErrors(), prefix);
		else
			addAll(errors, processor.getErrors(), prefix);
		addAll(warnings, processor.getWarnings(), prefix);

		processor.getErrors().clear();
		processor.getWarnings().clear();
	}

	public void getInfo(Reporter processor) {
		getInfo(processor, "");
	}

	private <T> void addAll(List<String> to, List< ? extends T> from, String prefix) {
		for (T x : from) {
			to.add(prefix + x);
		}
	}

	/**
	 * A processor can mark itself current for a thread.
	 * 
	 * @return
	 */
	private Processor current() {
		Processor p = current.get();
		if (p == null)
			return this;
		return p;
	}

	public SetLocation warning(String string, Object... args) {
		Processor p = current();
		String s = formatArrays(string, args);
		if (!p.warnings.contains(s))
			p.warnings.add(s);
		p.signal();
		return location(s);
	}

	public SetLocation error(String string, Object... args) {
		Processor p = current();
		try {
			if (p.isFailOk())
				return p.warning(string, args);
			else {
				String s = formatArrays(string, args == null ? new Object[0] : args);
				if (!p.errors.contains(s))
					p.errors.add(s);
				return location(s);
			}
		}
		finally {
			p.signal();
		}
	}

	public void progress(float progress, String format, Object... args) {
		format = String.format("[%2d] %s", (int)progress, format);
		trace(format, args);
	}

	public void progress(String format, Object... args) {
		progress(-1f, format, args);
	}

	public SetLocation exception(Throwable t, String format, Object... args) {
		return error(format, t, args);
	}

	public SetLocation error(String string, Throwable t, Object... args) {
		Processor p = current();
		try {
			if (p.exceptions)
				t.printStackTrace();
			if (p.isFailOk()) {
				return p.warning(string + ": " + t, args);
			}
			else {
				p.errors.add("Exception: " + t.getMessage());
				String s = formatArrays(string, args == null ? new Object[0] : args);
				if (!p.errors.contains(s))
					p.errors.add(s);
				return location(s);
			}
		}
		finally {
			p.signal();
		}
	}

	public void signal() {}

	public List<String> getWarnings() {
		return warnings;
	}

	public List<String> getErrors() {
		return errors;
	}

	/**
	 * Standard OSGi header parser.
	 * 
	 * @param value
	 * @return
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
		setBase(reporter.getBase());
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
	 * @param clazz
	 *            Each returned plugin implements this class/interface
	 * @return A list of plugins
	 */
	public <T> List<T> getPlugins(Class<T> clazz) {
		List<T> l = new ArrayList<T>();
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
	 * @return
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
	 * 
	 * @return
	 */
	protected synchronized Set<Object> getPlugins() {
		if (this.plugins != null)
			return this.plugins;

		missingCommand = new HashSet<String>();
		Set<Object> list = new LinkedHashSet<Object>();

		// The owner of the plugin is always in there.
		list.add(this);
		setTypeSpecificPlugins(list);

		if (parent != null)
			list.addAll(parent.getPlugins());

		// We only use plugins now when they are defined on our level
		// and not if it is in our parent. We inherit from our parent
		// through the previous block.

		if (properties.containsKey(PLUGIN)) {
			String spe = getProperty(PLUGIN);
			if (spe.equals(NONE))
				return new LinkedHashSet<Object>();

			String pluginPath = getProperty(PLUGINPATH);
			loadPlugins(list, spe, pluginPath);
		}

		return this.plugins = list;
	}

	/**
	 * @param list
	 * @param spe
	 */
	protected void loadPlugins(Set<Object> list, String spe, String pluginPath) {
		Parameters plugins = new Parameters(spe);
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
				}
				catch (Exception e) {
					error("Problem adding path %s to loader for plugin %s. Exception: (%s)", path, key, e);
				}
			}
		}

		// Next add -pluginpath entries
		if (pluginPath != null && pluginPath.length() > 0) {
			StringTokenizer tokenizer = new StringTokenizer(pluginPath, ",");
			while (tokenizer.hasMoreTokens()) {
				String path = tokenizer.nextToken().trim();
				try {
					File f = getFile(path).getAbsoluteFile();
					loader.add(f.toURI().toURL());
				}
				catch (Exception e) {
					error("Problem adding path %s from global plugin path. Exception: %s", path, e);
				}
			}
		}

		// Load the plugins
		for (Entry<String,Attrs> entry : plugins.entrySet()) {
			String key = entry.getKey();

			try {
				trace("Using plugin %s", key);

				// Plugins could use the same class with different
				// parameters so we could have duplicate names Remove
				// the ! added by the parser to make each name unique.
				key = removeDuplicateMarker(key);

				try {
					Class< ? > c = loader.loadClass(key);
					Object plugin = c.newInstance();
					customize(plugin, entry.getValue());
					list.add(plugin);
				}
				catch (Throwable t) {
					// We can defer the error if the plugin specifies
					// a command name. In that case, we'll verify that
					// a bnd file does not contain any references to a
					// plugin
					// command. The reason this feature was added was
					// to compile plugin classes with the same build.
					String commands = entry.getValue().get(COMMAND_DIRECTIVE);
					if (commands == null)
						error("Problem loading the plugin: %s exception: (%s)", key, t);
					else {
						Collection<String> cs = split(commands);
						missingCommand.addAll(cs);
					}
				}
			}
			catch (Throwable e) {
				error("Problem loading the plugin: %s exception: (%s)", key, e);
			}
		}
	}

	protected void setTypeSpecificPlugins(Set<Object> list) {
		list.add(executor);
		list.add(random);
		list.addAll(basicPlugins);
	}

	/**
	 * @param plugin
	 * @param entry
	 */
	protected <T> T customize(T plugin, Attrs map) {
		if (plugin instanceof Plugin) {
			if (map != null)
				((Plugin) plugin).setProperties(map);

			((Plugin) plugin).setReporter(this);
		}
		if (plugin instanceof RegistryPlugin) {
			((RegistryPlugin) plugin).setRegistry(this);
		}
		return plugin;
	}

	public boolean isFailOk() {
		String v = getProperty(Analyzer.FAIL_OK, null);
		return v != null && v.equalsIgnoreCase("true");
	}

	public File getBase() {
		return base;
	}

	public void setBase(File base) {
		this.base = base;
	}

	public void clear() {
		errors.clear();
		warnings.clear();
	}

	public void trace(String msg, Object... parms) {
		Processor p = current();
		if (p.trace) {
			System.err.printf("# " + msg + "%n", parms);
		}
	}

	public <T> List<T> newList() {
		return new ArrayList<T>();
	}

	public <T> Set<T> newSet() {
		return new TreeSet<T>();
	}

	public static <K, V> Map<K,V> newMap() {
		return new LinkedHashMap<K,V>();
	}

	public static <K, V> Map<K,V> newHashMap() {
		return new LinkedHashMap<K,V>();
	}

	public <T> List<T> newList(Collection<T> t) {
		return new ArrayList<T>(t);
	}

	public <T> Set<T> newSet(Collection<T> t) {
		return new TreeSet<T>(t);
	}

	public <K, V> Map<K,V> newMap(Map<K,V> t) {
		return new LinkedHashMap<K,V>(t);
	}

	public void close() {
		for (Closeable c : toBeClosed) {
			try {
				c.close();
			}
			catch (IOException e) {
				// Who cares?
			}
		}
		toBeClosed.clear();
	}

	public String _basedir(@SuppressWarnings("unused") String args[]) {
		if (base == null)
			throw new IllegalArgumentException("No base dir set");

		return base.getAbsolutePath();
	}

	/**
	 * Property handling ...
	 * 
	 * @return
	 */

	public Properties getProperties() {
		if (fixup) {
			fixup = false;
			begin();
		}

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
			}
			catch (Exception e) {
				error("Error loading properties file: " + file);
			}
		} else {
			if (!file.exists())
				error("Properties file does not exist: " + file);
			else
				error("Properties file must a file, not a directory: " + file);
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
		this.properties.putAll(properties);
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

	public synchronized void addIncluded(File file) {
		if (included == null)
			included = new ArrayList<File>();
		included.add(file);
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
			Collection<String> clauses = new Parameters(includes).keySet();

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
					if (!file.isFile() && fileMustExist) {
						error("Included file " + file + (file.exists() ? " does not exist" : " is directory"));
					} else
						doIncludeFile(file, overwrite, p);
				}
				catch (Exception e) {
					if (fileMustExist)
						error("Error in processing included file: " + value, e);
				}
			}
		}
	}

	/**
	 * @param file
	 * @param parent
	 * @param done
	 * @param overwrite
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void doIncludeFile(File file, boolean overwrite, Properties target) throws Exception {
		doIncludeFile(file, overwrite, target, null);
	}

	/**
	 * @param file
	 * @param parent
	 * @param done
	 * @param overwrite
	 * @param extensionName
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void doIncludeFile(File file, boolean overwrite, Properties target, String extensionName) throws Exception {
		if (included != null && included.contains(file)) {
			error("Cyclic or multiple include of " + file);
		} else {
			addIncluded(file);
			updateModified(file.lastModified(), file.toString());
			InputStream in = new FileInputStream(file);
			try {
				Properties sub;
				if (file.getName().toLowerCase().endsWith(".mf")) {
					sub = getManifestAsProperties(in);
				} else
					sub = loadProperties(in, file.getAbsolutePath());

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
			finally {
				IO.close(in);
			}
		}
	}

	public void unsetProperty(String string) {
		getProperties().remove(string);

	}

	public boolean refresh() {
		plugins = null; // We always refresh our plugins

		if (propertiesFile == null)
			return false;

		boolean changed = updateModified(propertiesFile.lastModified(), "properties file");
		if (included != null) {
			for (File file : included) {
				if (changed)
					break;

				changed |= !file.exists() || updateModified(file.lastModified(), "include file: " + file);
			}
		}

		if (changed) {
			forceRefresh();
			return true;
		}
		return false;
	}

	/**
	 * 
	 */
	public void forceRefresh() {
		included = null;
		properties.clear();
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
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void setProperties(File propertiesFile) throws IOException {
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

				included = null;
				Properties p = loadProperties(propertiesFile);
				setProperties(p);
			} else {
				if (fileMustExist) {
					error("No such properties file: " + propertiesFile);
				}
			}
		}
		catch (IOException e) {
			error("Could not load properties " + propertiesFile);
		}
	}

	protected void begin() {
		if (isTrue(getProperty(PEDANTIC)))
			setPedantic(true);
	}

	public static boolean isTrue(String value) {
		if (value == null)
			return false;

		return !"false".equalsIgnoreCase(value);
	}

	/**
	 * Get a property without preprocessing it with a proper default
	 * 
	 * @param headerName
	 * @param deflt
	 * @return
	 */

	public String getUnprocessedProperty(String key, String deflt) {
		return getProperties().getProperty(key, deflt);
	}

	/**
	 * Get a property with preprocessing it with a proper default
	 * 
	 * @param headerName
	 * @param deflt
	 * @return
	 */
	public String getProperty(String key, String deflt) {
		String value = null;

		Instruction ins = new Instruction(key);
		if (!ins.isLiteral()) {
			// Handle a wildcard key, make sure they're sorted
			// for consistency
			SortedList<String> sortedList = SortedList.fromIterator(iterator());
			StringBuilder sb = new StringBuilder();
			String del = "";
			for (String k : sortedList) {
				if (ins.matches(k)) {
					String v = getProperty(k, null);
					if (v != null) {
						sb.append(del);
						del = ",";
						sb.append(v);
					}
				}
			}
			if (sb.length() == 0)
				return deflt;

			return sb.toString();
		}

		Processor source = this;

		if (filter != null && filter.contains(key)) {
			value = (String) getProperties().get(key);
		} else {
			while (source != null) {
				value = (String) source.getProperties().get(key);
				if (value != null)
					break;

				source = source.getParent();
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
	 * @return
	 * @throws IOException
	 */
	public Properties loadProperties(File file) throws IOException {
		updateModified(file.lastModified(), "Properties file: " + file);
		InputStream in = new FileInputStream(file);
		try {
			Properties p = loadProperties(in, file.getAbsolutePath());
			return p;
		}
		finally {
			in.close();
		}
	}

	Properties loadProperties(InputStream in, String name) throws IOException {
		int n = name.lastIndexOf('/');
		if (n > 0)
			name = name.substring(0, n);
		if (name.length() == 0)
			name = ".";

		try {
			Properties p = new Properties();
			p.load(in);
			return replaceAll(p, "\\$\\{\\.\\}", name);
		}
		catch (Exception e) {
			error("Error during loading properties file: " + name + ", error:" + e);
			return new Properties();
		}
	}

	/**
	 * Replace a string in all the values of the map. This can be used to
	 * preassign variables that change. I.e. the base directory ${.} for a
	 * loaded properties
	 */

	public static Properties replaceAll(Properties p, String pattern, String replacement) {
		Properties result = new Properties();
		for (Iterator<Map.Entry<Object,Object>> i = p.entrySet().iterator(); i.hasNext();) {
			Map.Entry<Object,Object> entry = i.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			value = value.replaceAll(pattern, replacement);
			result.put(key, value);
		}
		return result;
	}

	/**
	 * Print a standard Map based OSGi header.
	 * 
	 * @param exports
	 *            map { name => Map { attribute|directive => value } }
	 * @return the clauses
	 * @throws IOException
	 */
	public static String printClauses(Map< ? , ? extends Map< ? , ? >> exports) throws IOException {
		return printClauses(exports, false);
	}

	public static String printClauses(Map< ? , ? extends Map< ? , ? >> exports, @SuppressWarnings("unused") boolean checkMultipleVersions)
			throws IOException {
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

		for (Entry< ? , ? > entry : map.entrySet()) {
			Object key = entry.getKey();
			// Skip directives we do not recognize
			if (key.equals(NO_IMPORT_DIRECTIVE) || key.equals(PROVIDE_DIRECTIVE) || key.equals(SPLIT_PACKAGE_DIRECTIVE)
					|| key.equals(FROM_DIRECTIVE))
				continue;

			String value = ((String) entry.getValue()).trim();
			sb.append(";");
			sb.append(key);
			sb.append("=");

			quote(sb, value);
		}
	}

	/**
	 * @param sb
	 * @param value
	 * @return
	 * @throws IOException
	 */
	public static boolean quote(Appendable sb, String value) throws IOException {
		boolean clean = (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"')
				|| Verifier.TOKEN.matcher(value).matches();
		if (!clean)
			sb.append("\"");
		sb.append(value);
		if (!clean)
			sb.append("\"");
		return clean;
	}

	public Macro getReplacer() {
		if (replacer == null)
			return replacer = new Macro(this, getMacroDomains());
		return replacer;
	}

	/**
	 * This should be overridden by subclasses to add extra macro command
	 * domains on the search list.
	 * 
	 * @return
	 */
	protected Object[] getMacroDomains() {
		return new Object[] {};
	}

	/**
	 * Return the properties but expand all macros. This always returns a new
	 * Properties object that can be used in any way.
	 * 
	 * @return
	 */
	public Properties getFlattenedProperties() {
		return getReplacer().getFlattenedProperties();

	}

	/**
	 * Return all inherited property keys
	 * 
	 * @return
	 */
	public Set<String> getPropertyKeys(boolean inherit) {
		Set<String> result;
		if (parent == null || !inherit) {
			result = Create.set();
		} else
			result = parent.getPropertyKeys(inherit);
		for (Object o : properties.keySet())
			result.add(o.toString());

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
			if (headers[i].equalsIgnoreCase(value)) {
				value = headers[i];
				break checkheader;
			}
		}
		getProperties().put(key, value);
	}

	/**
	 * Read a manifest but return a properties object.
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static Properties getManifestAsProperties(InputStream in) throws IOException {
		Properties p = new Properties();
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
		InputStreamReader ir = new InputStreamReader(in, "UTF8");
		StringBuilder sb = new StringBuilder();

		try {
			char chars[] = new char[1000];
			int size = ir.read(chars);
			while (size > 0) {
				sb.append(chars, 0, size);
				size = ir.read(chars);
			}
		}
		finally {
			ir.close();
		}
		return sb.toString();
	}

	/**
	 * Join a list.
	 * 
	 * @param args
	 * @return
	 */
	public static String join(Collection< ? > list, String delimeter) {
		return join(delimeter, list);
	}

	public static String join(String delimeter, Collection< ? >... list) {
		StringBuilder sb = new StringBuilder();
		String del = "";
		if (list != null) {
			for (Collection< ? > l : list) {
				for (Object item : l) {
					sb.append(del);
					sb.append(item);
					del = delimeter;
				}
			}
		}
		return sb.toString();
	}

	public static String join(Object[] list, String delimeter) {
		if (list == null)
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

	public static String join(Collection< ? >... list) {
		return join(",", list);
	}

	public static <T> String join(T list[]) {
		return join(list, ",");
	}

	public static void split(String s, Collection<String> set) {

		String elements[] = s.trim().split(LIST_SPLITTER);
		for (String element : elements) {
			if (element.length() > 0)
				set.add(element);
		}
	}

	public static Collection<String> split(String s) {
		return split(s, LIST_SPLITTER);
	}

	public static Collection<String> split(String s, String splitter) {
		if (s != null)
			s = s.trim();
		if (s == null || s.trim().length() == 0)
			return Collections.emptyList();

		return Arrays.asList(s.split(splitter));
	}

	public static String merge(String... strings) {
		ArrayList<String> result = new ArrayList<String>();
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
	 * @return
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

	static class CL extends URLClassLoader {

		CL() {
			super(new URL[0], Processor.class.getClassLoader());
		}

		void add(URL url) {
			URL urls[] = getURLs();
			for (URL u : urls) {
				if (u.equals(url))
					return;
			}
			super.addURL(url);
		}

		public Class< ? > loadClass(String name) throws NoClassDefFoundError {
			try {
				Class< ? > c = super.loadClass(name);
				return c;
			}
			catch (Throwable t) {
				StringBuilder sb = new StringBuilder();
				sb.append(name);
				sb.append(" not found, parent:  ");
				sb.append(getParent());
				sb.append(" urls:");
				sb.append(Arrays.toString(getURLs()));
				sb.append(" exception:");
				sb.append(t);
				throw new NoClassDefFoundError(sb.toString());
			}
		}
	}

	private CL getLoader() {
		if (pluginLoader == null) {
			pluginLoader = new CL();
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
			out.append("-----------------\nErrors\n");
			for (int i = 0; i < errors.size(); i++) {
				out.append(String.format("%03d: %s\n", i, errors.get(i)));
			}
		}
		if (warnings.size() > 0) {
			out.append(String.format("-----------------\nWarnings\n"));
			for (int i = 0; i < warnings.size(); i++) {
				out.append(String.format("%03d: %s\n", i, warnings.get(i)));
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
	 * @return
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
	 *  &quot;/&quot; + &quot;abc/def/&quot; becomes &quot;abc/def&quot;
	 *  
	 * &#064;param prefix
	 * &#064;param suffix
	 * &#064;return
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
	 * @return
	 */
	public static Attrs doAttrbutes(Object[] attrs, Clazz clazz, Macro macro) {
		Attrs map = new Attrs();

		if (attrs == null || attrs.length == 0)
			return map;

		for (Object a : attrs) {
			String attr = (String) a;
			int n = attr.indexOf("=");
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
	 * @return
	 */
	public static String formatArrays(String string, Object... parms) {
		Object[] parms2 = parms;
		Object[] output = new Object[parms.length];
		for (int i = 0; i < parms.length; i++) {
			output[i] = makePrintable(parms[i]);
		}
		return String.format(string, parms2);
	}

	/**
	 * Check if the object is an array and turn it into a string if it is,
	 * otherwise unchanged.
	 * 
	 * @param object
	 *            the object to make printable
	 * @return a string if it was an array or the original object
	 */
	public static Object makePrintable(Object object) {
		if (object == null)
			return object;

		if (object.getClass().isArray()) {
			Object[] array = (Object[]) object;
			Object[] output = new Object[array.length];
			for (int i = 0; i < array.length; i++) {
				output[i] = makePrintable(array[i]);
			}
			return Arrays.toString(output);
		}
		return object;
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
		Matcher m = Pattern
				.compile("\\s*(\\d+)\\s*(NANOSECONDS|MICROSECONDS|MILLISECONDS|SECONDS|MINUTES|HOURS|DAYS)?").matcher(
						tm);
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
			}
			catch (NumberFormatException e) {
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
	 * Set the current command thread. This must be balanced with the
	 * {@link #end(Processor)} method. The method returns the previous command
	 * owner or null. The command owner will receive all warnings and error
	 * reports.
	 */

	protected Processor beginHandleErrors(String message) {
		trace("begin %s", message);
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
		trace("end");
		current.set(previous);
	}

	public static Executor getExecutor() {
		return executor;
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
		if (plugins != null)
			plugins.add(plugin);
	}

	public synchronized void removeBasicPlugin(Object plugin) {
		basicPlugins.remove(plugin);
		if (plugins != null)
			plugins.remove(plugin);
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

	@Override
	public Iterator<String> iterator() {
		Set<String> keys = keySet();
		final Iterator<String> it = keys.iterator();

		return new Iterator<String>() {
			String	current;

			public boolean hasNext() {
				return it.hasNext();
			}

			public String next() {
				return current = it.next().toString();
			}

			public void remove() {
				getProperties().remove(current);
			}
		};
	}

	public Set<String> keySet() {
		Set<String> set;
		if (parent == null)
			set = Create.set();
		else
			set = parent.keySet();

		for (Object o : properties.keySet())
			set.add(o.toString());

		return set;
	}

	/**
	 * Printout of the status of this processor for toString()
	 */

	public String toString() {
		try {
			StringBuilder sb = new StringBuilder();
			report(sb);
			return sb.toString();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Utiltity to replace an extension
	 * 
	 * @param s
	 * @param extension
	 * @param newExtension
	 * @return
	 */
	public String replaceExtension(String s, String extension, String newExtension) {
		if (s.endsWith(extension))
			s = s.substring(0, s.length() - extension.length());

		return s + newExtension;
	}

	/**
	 * Create a location object and add it to the locations
	 * 
	 * @param s
	 * @return
	 */
	List<Location>	locations	= new ArrayList<Location>();

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

}

package aQute.lib.osgi;

/**
 * This class can calculate the required headers for a (potential) JAR file. It
 * analyzes a directory or JAR for the packages that are contained and that are
 * referred to by the bytecodes. The user can the use regular expressions to
 * define the attributes and directives. The matching is not fully regex for
 * convenience. A * and ? get a . prefixed and dots are escaped.
 * 
 * <pre>
 *                                                             			*;auto=true				any		
 *                                                             			org.acme.*;auto=true    org.acme.xyz
 *                                                             			org.[abc]*;auto=true    org.acme.xyz
 * </pre>
 * 
 * Additional, the package instruction can start with a '=' or a '!'. The '!'
 * indicates negation. Any matching package is removed. The '=' is literal, the
 * expression will be copied verbatim and no matching will take place.
 * 
 * Any headers in the given properties are used in the output properties.
 */
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.*;
import java.util.jar.Attributes.Name;
import java.util.regex.*;

import aQute.bnd.annotation.*;
import aQute.bnd.service.*;
import aQute.lib.base64.*;
import aQute.lib.collections.*;
import aQute.lib.filter.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.osgi.Clazz.QUERY;
import aQute.libg.cryptography.*;
import aQute.libg.generics.*;
import aQute.libg.header.*;
import aQute.libg.tarjan.*;
import aQute.libg.version.Version;

public class Analyzer extends Processor {

	static String							version;
	static Pattern							versionPattern			= Pattern
																			.compile("(\\d+\\.\\d+)\\.\\d+.*");
	final Map<String, Map<String, String>>	contained				= newHashMap();							// package
	final Map<String, Map<String, String>>	referred				= newHashMap();							// refers
	// package
	final Map<String, Set<String>>			uses					= newHashMap();							// package
	Map<String, Clazz>						classspace;
	Map<String, Clazz>						importedClassesCache	= newMap();
	Map<String, Map<String, String>>		exports;
	Map<String, Map<String, String>>		imports;
	Map<String, Map<String, String>>		bundleClasspath;													// Bundle
	final Map<String, Map<String, String>>	ignored					= newHashMap();							// Ignored
	// packages
	Jar										dot;
	Map<String, Map<String, String>>		classpathExports;

	String									activator;

	final List<Jar>							classpath				= newList();

	static Properties						bndInfo;

	boolean									analyzed;
	String									bsn;
	String									versionPolicyUses;
	String									versionPolicyImplemented;
	boolean									diagnostics				= false;
	SortedSet<Clazz.JAVA>					formats					= new TreeSet<Clazz.JAVA>();
	private boolean							inited;

	public Analyzer(Processor parent) {
		super(parent);
	}

	public Analyzer() {
	}

	/**
	 * Specifically for Maven
	 * 
	 * @param properties
	 *            the properties
	 */

	public static Properties getManifest(File dirOrJar) throws Exception {
		Analyzer analyzer = new Analyzer();
		try {
			analyzer.setJar(dirOrJar);
			Properties properties = new Properties();
			properties.put(IMPORT_PACKAGE, "*");
			properties.put(EXPORT_PACKAGE, "*");
			analyzer.setProperties(properties);
			Manifest m = analyzer.calcManifest();
			Properties result = new Properties();
			for (Iterator<Object> i = m.getMainAttributes().keySet().iterator(); i.hasNext();) {
				Attributes.Name name = (Attributes.Name) i.next();
				result.put(name.toString(), m.getMainAttributes().getValue(name));
			}
			return result;
		} finally {
			analyzer.close();
		}
	}

	/**
	 * Calculates the data structures for generating a manifest.
	 * 
	 * @throws IOException
	 */
	public void analyze() throws Exception {
		if (!analyzed) {
			analyzed = true;
			activator = getProperty(BUNDLE_ACTIVATOR);
			bundleClasspath = parseHeader(getProperty(BUNDLE_CLASSPATH));

			analyzeClasspath();

			classspace = analyzeBundleClasspath(dot, bundleClasspath, contained, referred, uses);

			for (AnalyzerPlugin plugin : getPlugins(AnalyzerPlugin.class)) {
				if (plugin instanceof AnalyzerPlugin) {
					AnalyzerPlugin analyzer = (AnalyzerPlugin) plugin;
					try {
						boolean reanalyze = analyzer.analyzeJar(this);
						if (reanalyze)
							classspace = analyzeBundleClasspath(dot, bundleClasspath, contained,
									referred, uses);
					} catch (Exception e) {
						error("Plugin Analyzer " + analyzer + " throws exception " + e);
						e.printStackTrace();
					}
				}
			}

			if (activator != null) {
				// Add the package of the activator to the set
				// of referred classes. This must be done before we remove
				// contained set.
				int n = activator.lastIndexOf('.');
				if (n > 0) {
					referred.put(activator.substring(0, n), new LinkedHashMap<String, String>());
				}
			}

			referred.keySet().removeAll(contained.keySet());
			if (referred.containsKey(".")) {
				error("The default package '.' is not permitted by the Import-Package syntax. \n"
						+ " This can be caused by compile errors in Eclipse because Eclipse creates \n"
						+ "valid class files regardless of compile errors.\n"
						+ "The following package(s) import from the default package "
						+ getUsedBy("."));
			}

			Map<String, Map<String, String>> exportInstructions = parseHeader(getProperty(EXPORT_PACKAGE));
			Map<String, Map<String, String>> additionalExportInstructions = parseHeader(getProperty(EXPORT_CONTENTS));
			exportInstructions.putAll(additionalExportInstructions);
			Map<String, Map<String, String>> importInstructions = parseHeader(getImportPackages());
			Map<String, Map<String, String>> dynamicImports = parseHeader(getProperty(DYNAMICIMPORT_PACKAGE));

			if (dynamicImports != null) {
				// Remove any dynamic imports from the referred set.
				referred.keySet().removeAll(dynamicImports.keySet());
			}

			Map<String, Map<String, String>> superfluous = newHashMap();
			// Tricky!
			for (Iterator<String> i = exportInstructions.keySet().iterator(); i.hasNext();) {
				String instr = i.next();
				if (!instr.startsWith("!"))
					superfluous.put(instr, exportInstructions.get(instr));
			}

			exports = merge("export-package", exportInstructions, contained, superfluous.keySet(),
					null);

			// disallow export of default package
			exports.remove(".");

			for (Iterator<Map.Entry<String, Map<String, String>>> i = superfluous.entrySet()
					.iterator(); i.hasNext();) {
				// It is possible to mention metadata directories in the export
				// explicitly, they are then exported and removed from the
				// warnings. Note that normally metadata directories are not
				// exported.
				Map.Entry<String, Map<String, String>> entry = i.next();
				String pack = entry.getKey();
				if (isDuplicate(pack))
					i.remove();
				else if (isMetaData(pack)) {
					exports.put(pack, entry.getValue());
					i.remove();
				}
			}

			if (!superfluous.isEmpty()) {
				warning("Superfluous export-package instructions: " + superfluous.keySet());
			}

			// Add all exports that do not have an -noimport: directive
			// to the imports.
			Map<String, Map<String, String>> referredAndExported = newMap(referred);
			referredAndExported.putAll(doExportsToImports(exports));

			// match the imports to the referred and exported packages,
			// merge the info for matching packages
			Set<String> extra = new TreeSet<String>(importInstructions.keySet());
			imports = merge("import-package", importInstructions, referredAndExported, extra,
					ignored);

			// Instructions that have not been used could be superfluous
			// or if they do not contain wildcards, should be added
			// as extra imports, the user knows best.
			for (Iterator<String> i = extra.iterator(); i.hasNext();) {
				String p = i.next();
				if (p.startsWith("!") || p.indexOf('*') >= 0 || p.indexOf('?') >= 0
						|| p.indexOf('[') >= 0) {
					if (!isResourceOnly() && !(p.equals("*")))
						warning("Did not find matching referal for " + p);
				} else {
					Map<String, String> map = importInstructions.get(p);
					imports.put(p, map);
				}
			}

			// See what information we can find to augment the
			// exports. I.e. look on the classpath
			augmentExports();

			// See what information we can find to augment the
			// imports. I.e. look on the classpath
			augmentImports();

			// Add the uses clause to the exports
			doUses(exports, uses, imports);
		}
	}

	/**
	 * Copy the input collection into an output set but skip names that have
	 * been marked as duplicates or are optional.
	 * 
	 * @param superfluous
	 * @return
	 */
	Set<Instruction> removeMarkedDuplicates(Collection<Instruction> superfluous) {
		Set<Instruction> result = new HashSet<Instruction>();
		for (Iterator<Instruction> i = superfluous.iterator(); i.hasNext();) {
			Instruction instr = (Instruction) i.next();
			if (!isDuplicate(instr.getPattern()) && !instr.isOptional())
				result.add(instr);
		}
		return result;
	}

	/**
	 * Analyzer has an empty default but the builder has a * as default.
	 * 
	 * @return
	 */
	protected String getImportPackages() {
		return getProperty(IMPORT_PACKAGE, false);
	}

	/**
	 * 
	 * @return
	 */
	boolean isResourceOnly() {
		return isTrue(getProperty(RESOURCEONLY));
	}

	/**
	 * Answer the list of packages that use the given package.
	 */
	Set<String> getUsedBy(String pack) {
		Set<String> set = newSet();
		for (Iterator<Map.Entry<String, Set<String>>> i = uses.entrySet().iterator(); i.hasNext();) {
			Map.Entry<String, Set<String>> entry = i.next();
			Set<String> used = entry.getValue();
			if (used.contains(pack))
				set.add(entry.getKey());
		}
		return set;
	}

	/**
	 * One of the main workhorses of this class. This will analyze the current
	 * setp and calculate a new manifest according to this setup. This method
	 * will also set the manifest on the main jar dot
	 * 
	 * @return
	 * @throws IOException
	 */
	public Manifest calcManifest() throws Exception {
		analyze();
		Manifest manifest = new Manifest();
		Attributes main = manifest.getMainAttributes();

		main.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		main.putValue(BUNDLE_MANIFESTVERSION, "2");

		boolean noExtraHeaders = "true".equalsIgnoreCase(getProperty(NOEXTRAHEADERS));

		if (!noExtraHeaders) {
			main.putValue(CREATED_BY,
					System.getProperty("java.version") + " (" + System.getProperty("java.vendor")
							+ ")");
			main.putValue(TOOL, "Bnd-" + getBndVersion());
			main.putValue(BND_LASTMODIFIED, "" + System.currentTimeMillis());
		}

		String exportHeader = printClauses(exports, true);

		if (exportHeader.length() > 0)
			main.putValue(EXPORT_PACKAGE, exportHeader);
		else
			main.remove(EXPORT_PACKAGE);

		Map<String, Map<String, String>> temp = removeKeys(imports, "java.");
		if (!temp.isEmpty()) {
			main.putValue(IMPORT_PACKAGE, printClauses(temp));
		} else {
			main.remove(IMPORT_PACKAGE);
		}

		temp = newMap(contained);
		temp.keySet().removeAll(exports.keySet());

		if (!temp.isEmpty())
			main.putValue(PRIVATE_PACKAGE, printClauses(temp));
		else
			main.remove(PRIVATE_PACKAGE);

		if (!ignored.isEmpty()) {
			main.putValue(IGNORE_PACKAGE, printClauses(ignored));
		} else {
			main.remove(IGNORE_PACKAGE);
		}

		if (bundleClasspath != null && !bundleClasspath.isEmpty())
			main.putValue(BUNDLE_CLASSPATH, printClauses(bundleClasspath));
		else
			main.remove(BUNDLE_CLASSPATH);

		doNamesection(dot, manifest);

		for (Enumeration<?> h = getProperties().propertyNames(); h.hasMoreElements();) {
			String header = (String) h.nextElement();
			if (header.trim().length() == 0) {
				warning("Empty property set with value: " + getProperties().getProperty(header));
				continue;
			}

			if (isMissingPlugin(header.trim())) {
				error("Missing plugin for command %s", header);
			}
			if (!Character.isUpperCase(header.charAt(0))) {
				if (header.charAt(0) == '@')
					doNameSection(manifest, header);
				continue;
			}

			if (header.equals(BUNDLE_CLASSPATH) || header.equals(EXPORT_PACKAGE)
					|| header.equals(IMPORT_PACKAGE))
				continue;

			if (header.equalsIgnoreCase("Name")) {
				error("Your bnd file contains a header called 'Name'. This interferes with the manifest name section.");
				continue;
			}

			if (Verifier.HEADER_PATTERN.matcher(header).matches()) {
				String value = getProperty(header);
				if (value != null && main.getValue(header) == null) {
					if (value.trim().length() == 0)
						main.remove(header);
					else if (value.trim().equals(EMPTY_HEADER))
						main.putValue(header, "");
					else
						main.putValue(header, value);
				}
			} else {
				// TODO should we report?
			}
		}

		//
		// Calculate the bundle symbolic name if it is
		// not set.
		// 1. set
		// 2. name of properties file (must be != bnd.bnd)
		// 3. name of directory, which is usualy project name
		//
		String bsn = getBsn();
		if (main.getValue(BUNDLE_SYMBOLICNAME) == null) {
			main.putValue(BUNDLE_SYMBOLICNAME, bsn);
		}

		//
		// Use the same name for the bundle name as BSN when
		// the bundle name is not set
		//
		if (main.getValue(BUNDLE_NAME) == null) {
			main.putValue(BUNDLE_NAME, bsn);
		}

		if (main.getValue(BUNDLE_VERSION) == null)
			main.putValue(BUNDLE_VERSION, "0");

		// Copy old values into new manifest, when they
		// exist in the old one, but not in the new one
		merge(manifest, dot.getManifest());

		// Remove all the headers mentioned in -removeheaders
		Map<String, Map<String, String>> removes = parseHeader(getProperty(REMOVEHEADERS));
		Set<Instruction> matchers = Instruction.replaceWithInstruction(removes).keySet();

		Collection<Object> toBeRemoved = Instruction.select(matchers, main.keySet());
		Iterator<Object> i = main.keySet().iterator();
		while (i.hasNext())
			if (toBeRemoved.contains(i.next()))
				i.remove();

		dot.setManifest(manifest);
		return manifest;
	}

	/**
	 * Parse the namesection as instructions and then match them against the
	 * current set of resources
	 * 
	 * For example:
	 * 
	 * <pre>
	 * 	-namesection: *;baz=true, abc/def/bar/X.class=3
	 * </pre>
	 * 
	 * The raw value of {@link Constants#NAMESECTION} is used but the values of
	 * the attributes are replaced where @ is set to the resource name. This
	 * allows macro to operate on the resource
	 * 
	 */

	private void doNamesection(Jar dot, Manifest manifest) {

		Map<String, Map<String, String>> namesection = parseHeader(getProperties().getProperty(
				NAMESECTION));
		Set<Entry<Instruction, Map<String, String>>> instructions = Instruction
				.replaceWithInstruction(namesection).entrySet();
		Set<Map.Entry<String, Resource>> resources = new HashSet<Map.Entry<String, Resource>>(dot
				.getResources().entrySet());

		//
		// For each instruction, iterator over the resources and filter
		// them. If a resource matches, it must be removed even if the
		// instruction is negative. If positive, add a name section
		// to the manifest for the given resource name. Then add all
		// attributes from the instruction to that name section.
		//
		for (Map.Entry<Instruction, Map<String, String>> instr : instructions) {
			boolean matched = false;

			// For each instruction

			for (Iterator<Map.Entry<String, Resource>> it = resources.iterator(); it.hasNext();) {

				// For each resource

				Map.Entry<String, Resource> next = it.next();
				if (instr.getKey().matches(next.getKey())) {

					// Instruction matches the resource

					matched = true;
					if (!instr.getKey().isNegated()) {

						// Positive match, add the attributes

						Attributes attrs = manifest.getAttributes(next.getKey());
						if (attrs == null) {
							attrs = new Attributes();
							manifest.getEntries().put(next.getKey(), attrs);
						}

						//
						// Add all the properties from the instruction to the
						// name section
						//

						for (Map.Entry<String, String> property : instr.getValue().entrySet()) {
							setProperty("@", next.getKey());
							try {
								String processed = getReplacer().process(property.getValue());
								attrs.putValue(property.getKey(), processed);
							} finally {
								unsetProperty("@");
							}
						}
					}
					it.remove();
				}
			}

			if (!matched)
				warning("The instruction %s in %s did not match any resources", instr.getKey(),
						NAMESECTION);
		}

	}

	/**
	 * This method is called when the header starts with a @, signifying a name
	 * section header. The name part is defined by replacing all the @ signs to
	 * a /, removing the first and the last, and using the last part as header
	 * name:
	 * 
	 * <pre>
	 * &#064;org@osgi@service@event@Implementation-Title
	 * </pre>
	 * 
	 * This will be the header Implementation-Title in the
	 * org/osgi/service/event named section.
	 * 
	 * @param manifest
	 * @param header
	 */
	private void doNameSection(Manifest manifest, String header) {
		String path = header.replace('@', '/');
		int n = path.lastIndexOf('/');
		// Must succeed because we start with @
		String name = path.substring(n + 1);
		// Skip first /
		path = path.substring(1, n);
		if (name.length() != 0 && path.length() != 0) {
			Attributes attrs = manifest.getAttributes(path);
			if (attrs == null) {
				attrs = new Attributes();
				manifest.getEntries().put(path, attrs);
			}
			attrs.putValue(name, getProperty(header));
		} else {
			warning("Invalid header (starts with @ but does not seem to be for the Name section): %s",
					header);
		}
	}

	/**
	 * Clear the key part of a header. I.e. remove everything from the first ';'
	 * 
	 * @param value
	 * @return
	 */
	public String getBsn() {
		String value = getProperty(BUNDLE_SYMBOLICNAME);
		if (value == null) {
			if (getPropertiesFile() != null)
				value = getPropertiesFile().getName();

			String projectName = getBase().getName();
			if (value == null || value.equals("bnd.bnd")) {
				value = projectName;
			} else if (value.endsWith(".bnd")) {
				value = value.substring(0, value.length() - 4);
				if (!value.startsWith(getBase().getName()))
					value = projectName + "." + value;
			}
		}

		if (value == null)
			return "untitled";

		int n = value.indexOf(';');
		if (n > 0)
			value = value.substring(0, n);
		return value.trim();
	}

	public String _bsn(String args[]) {
		return getBsn();
	}

	/**
	 * Calculate an export header solely based on the contents of a JAR file
	 * 
	 * @param bundle
	 *            The jar file to analyze
	 * @return
	 */
	public String calculateExportsFromContents(Jar bundle) {
		String ddel = "";
		StringBuffer sb = new StringBuffer();
		Map<String, Map<String, Resource>> map = bundle.getDirectories();
		for (Iterator<String> i = map.keySet().iterator(); i.hasNext();) {
			String directory = (String) i.next();
			if (directory.equals("META-INF") || directory.startsWith("META-INF/"))
				continue;
			if (directory.equals("OSGI-OPT") || directory.startsWith("OSGI-OPT/"))
				continue;
			if (directory.equals("/"))
				continue;

			if (directory.endsWith("/"))
				directory = directory.substring(0, directory.length() - 1);

			directory = directory.replace('/', '.');
			sb.append(ddel);
			sb.append(directory);
			ddel = ",";
		}
		return sb.toString();
	}

	public Map<String, Map<String, String>> getBundleClasspath() {
		return bundleClasspath;
	}

	public Map<String, Map<String, String>> getContained() {
		return contained;
	}

	public Map<String, Map<String, String>> getExports() {
		return exports;
	}

	public Map<String, Map<String, String>> getImports() {
		return imports;
	}

	public Jar getJar() {
		return dot;
	}

	public Map<String, Map<String, String>> getReferred() {
		return referred;
	}

	/**
	 * Return the set of unreachable code depending on exports and the bundle
	 * activator.
	 * 
	 * @return
	 */
	public Set<String> getUnreachable() {
		Set<String> unreachable = new HashSet<String>(uses.keySet()); // all
		for (Iterator<String> r = exports.keySet().iterator(); r.hasNext();) {
			String packageName = r.next();
			removeTransitive(packageName, unreachable);
		}
		if (activator != null) {
			String pack = activator.substring(0, activator.lastIndexOf('.'));
			removeTransitive(pack, unreachable);
		}
		return unreachable;
	}

	public Map<String, Set<String>> getUses() {
		return uses;
	}

	/**
	 * Get the version for this bnd
	 * 
	 * @return version or unknown.
	 */
	public String getBndVersion() {
		return getBndInfo("version", "1.42.1");
	}

	public long getBndLastModified() {
		String time = getBndInfo("modified", "0");
		try {
			return Long.parseLong(time);
		} catch (Exception e) {
		}
		return 0;
	}

	public String getBndInfo(String key, String defaultValue) {
		if (bndInfo == null) {
			bndInfo = new Properties();
			try {
				InputStream in = Analyzer.class.getResourceAsStream("bnd.info");
				if (in != null) {
					bndInfo.load(in);
					in.close();
				}
			} catch (IOException ioe) {
				warning("Could not read bnd.info in " + Analyzer.class.getPackage() + ioe);
			}
		}
		return bndInfo.getProperty(key, defaultValue);
	}

	/**
	 * Merge the existing manifest with the instructions.
	 * 
	 * @param manifest
	 *            The manifest to merge with
	 * @throws IOException
	 */
	public void mergeManifest(Manifest manifest) throws IOException {
		if (manifest != null) {
			Attributes attributes = manifest.getMainAttributes();
			for (Iterator<Object> i = attributes.keySet().iterator(); i.hasNext();) {
				Name name = (Name) i.next();
				String key = name.toString();
				// Dont want instructions
				if (key.startsWith("-"))
					continue;

				if (getProperty(key) == null)
					setProperty(key, (String) attributes.get(name));
			}
		}
	}

	public void setBase(File file) {
		super.setBase(file);
		getProperties().put("project.dir", getBase().getAbsolutePath());
	}

	/**
	 * Set the classpath for this analyzer by file.
	 * 
	 * @param classpath
	 * @throws IOException
	 */
	public void setClasspath(File[] classpath) throws IOException {
		List<Jar> list = new ArrayList<Jar>();
		for (int i = 0; i < classpath.length; i++) {
			if (classpath[i].exists()) {
				Jar current = new Jar(classpath[i]);
				list.add(current);
			} else {
				error("Missing file on classpath: %s", classpath[i]);
			}
		}
		for (Iterator<Jar> i = list.iterator(); i.hasNext();) {
			addClasspath(i.next());
		}
	}

	public void setClasspath(Jar[] classpath) {
		for (int i = 0; i < classpath.length; i++) {
			addClasspath(classpath[i]);
		}
	}

	public void setClasspath(String[] classpath) {
		for (int i = 0; i < classpath.length; i++) {
			Jar jar = getJarFromName(classpath[i], " setting classpath");
			if (jar != null)
				addClasspath(jar);
		}
	}

	/**
	 * Set the JAR file we are going to work in. This will read the JAR in
	 * memory.
	 * 
	 * @param jar
	 * @return
	 * @throws IOException
	 */
	public Jar setJar(File jar) throws IOException {
		Jar jarx = new Jar(jar);
		addClose(jarx);
		return setJar(jarx);
	}

	/**
	 * Set the JAR directly we are going to work on.
	 * 
	 * @param jar
	 * @return
	 */
	public Jar setJar(Jar jar) {
		this.dot = jar;
		return jar;
	}

	protected void begin() {
		if (inited == false) {
			inited = true;
			super.begin();

			updateModified(getBndLastModified(), "bnd last modified");
			verifyManifestHeadersCase(getProperties());

		}
	}

	/**
	 * Check if the given class or interface name is contained in the jar.
	 * 
	 * @param interfaceName
	 * @return
	 */

	public boolean checkClass(String interfaceName) {
		String path = Clazz.fqnToPath(interfaceName);
		if (classspace.containsKey(path))
			return true;

		if (interfaceName.startsWith("java."))
			return true;

		if (imports != null && !imports.isEmpty()) {
			String pack = interfaceName;
			int n = pack.lastIndexOf('.');
			if (n > 0)
				pack = pack.substring(0, n);
			else
				pack = ".";

			if (imports.containsKey(pack))
				return true;
		}
		int n = interfaceName.lastIndexOf('.');
		if (n > 0 && n + 1 < interfaceName.length()
				&& Character.isUpperCase(interfaceName.charAt(n + 1))) {
			return checkClass(interfaceName.substring(0, n) + '$' + interfaceName.substring(n + 1));
		}
		return false;
	}

	/**
	 * Try to get a Jar from a file name/path or a url, or in last resort from
	 * the classpath name part of their files.
	 * 
	 * @param name
	 *            URL or filename relative to the base
	 * @param from
	 *            Message identifying the caller for errors
	 * @return null or a Jar with the contents for the name
	 */
	Jar getJarFromName(String name, String from) {
		File file = new File(name);
		if (!file.isAbsolute())
			file = new File(getBase(), name);

		if (file.exists())
			try {
				Jar jar = new Jar(file);
				addClose(jar);
				return jar;
			} catch (Exception e) {
				error("Exception in parsing jar file for " + from + ": " + name + " " + e);
			}
		// It is not a file ...
		try {
			// Lets try a URL
			URL url = new URL(name);
			Jar jar = new Jar(fileName(url.getPath()));
			addClose(jar);
			URLConnection connection = url.openConnection();
			InputStream in = connection.getInputStream();
			long lastModified = connection.getLastModified();
			if (lastModified == 0)
				// We assume the worst :-(
				lastModified = System.currentTimeMillis();
			EmbeddedResource.build(jar, in, lastModified);
			in.close();
			return jar;
		} catch (IOException ee) {
			// Check if we have files on the classpath
			// that have the right name, allows us to specify those
			// names instead of the full path.
			for (Iterator<Jar> cp = getClasspath().iterator(); cp.hasNext();) {
				Jar entry = cp.next();
				if (entry.source != null && entry.source.getName().equals(name)) {
					return entry;
				}
			}
			// error("Can not find jar file for " + from + ": " + name);
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
	 * 
	 * @param manifests
	 * @throws Exception
	 */
	void merge(Manifest result, Manifest old) throws IOException {
		if (old != null) {
			for (Iterator<Map.Entry<Object, Object>> e = old.getMainAttributes().entrySet()
					.iterator(); e.hasNext();) {
				Map.Entry<Object, Object> entry = e.next();
				Attributes.Name name = (Attributes.Name) entry.getKey();
				String value = (String) entry.getValue();
				if (name.toString().equalsIgnoreCase("Created-By"))
					name = new Attributes.Name("Originally-Created-By");
				if (!result.getMainAttributes().containsKey(name))
					result.getMainAttributes().put(name, value);
			}

			// do not overwrite existing entries
			Map<String, Attributes> oldEntries = old.getEntries();
			Map<String, Attributes> newEntries = result.getEntries();
			for (Iterator<Map.Entry<String, Attributes>> e = oldEntries.entrySet().iterator(); e
					.hasNext();) {
				Map.Entry<String, Attributes> entry = e.next();
				if (!newEntries.containsKey(entry.getKey())) {
					newEntries.put(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	String stem(String name) {
		int n = name.lastIndexOf('.');
		if (n > 0)
			return name.substring(0, n);
		else
			return name;
	}

	/**
	 * Bnd is case sensitive for the instructions so we better check people are
	 * not using an invalid case. We do allow this to set headers that should
	 * not be processed by us but should be used by the framework.
	 * 
	 * @param properties
	 *            Properties to verify.
	 */

	void verifyManifestHeadersCase(Properties properties) {
		for (Iterator<Object> i = properties.keySet().iterator(); i.hasNext();) {
			String header = (String) i.next();
			for (int j = 0; j < headers.length; j++) {
				if (!headers[j].equals(header) && headers[j].equalsIgnoreCase(header)) {
					warning("Using a standard OSGi header with the wrong case (bnd is case sensitive!), using: "
							+ header + " and expecting: " + headers[j]);
					break;
				}
			}
		}
	}

	/**
	 * We will add all exports to the imports unless there is a -noimport
	 * directive specified on an export. This directive is skipped for the
	 * manifest.
	 * 
	 * We also remove any version parameter so that augmentImports can do the
	 * version policy.
	 * 
	 * The following method is really tricky and evolved over time. Coming from
	 * the original background of OSGi, it was a weird idea for me to have a
	 * public package that should not be substitutable. I was so much convinced
	 * that this was the right rule that I rücksichtlos imported them all. Alas,
	 * the real world was more subtle than that. It turns out that it is not a
	 * good idea to always import. First, there must be a need to import, i.e.
	 * there must be a contained package that refers to the exported package for
	 * it to make use importing that package. Second, if an exported package
	 * refers to an internal package than it should not be imported.
	 * 
	 * Additionally, it is necessary to treat the exports in groups. If an
	 * exported package refers to another exported packages than it must be in
	 * the same group. A framework can only substitute exports for imports for
	 * the whole of such a group. WHY????? Not clear anymore ...
	 * 
	 */
	/**
	 * I could no longer argue why the groups are needed :-( See what happens
	 * ... The getGroups calculated the groups and then removed the imports from
	 * there. Now we only remove imports that have internal references. Using
	 * internal code for an exported package means that a bundle cannot import
	 * that package from elsewhere because its assumptions might be violated if
	 * it receives a substitution. //
	 */
	Map<String, Map<String, String>> doExportsToImports(Map<String, Map<String, String>> exports) {

		// private packages = contained - exported.
		Set<String> privatePackages = new HashSet<String>(contained.keySet());
		privatePackages.removeAll(exports.keySet());

		// private references = ∀ p : private packages | uses(p)
		Set<String> privateReferences = newSet();
		for (String p : privatePackages) {
			Set<String> uses = this.uses.get(p);
			if (uses != null)
				privateReferences.addAll(uses);
		}

		// Assume we are going to export all exported packages
		Set<String> toBeImported = new HashSet<String>(exports.keySet());

		// Remove packages that are not referenced privately
		toBeImported.retainAll(privateReferences);

		// Not necessary to import anything that is already
		// imported in the Import-Package statement.
		if (imports != null)
			toBeImported.removeAll(imports.keySet());

		// Remove exported packages that are referring to
		// private packages.
		// Each exported package has a uses clause. We just use
		// the used packages for each exported package to find out
		// if it refers to an internal package.
		//

		for (Iterator<String> i = toBeImported.iterator(); i.hasNext();) {
			Set<String> usedByExportedPackage = this.uses.get(i.next());
			for (String privatePackage : privatePackages) {
				if (usedByExportedPackage.contains(privatePackage)) {
					i.remove();
					break;
				}
			}
		}

		// Clean up attributes and generate result map
		Map<String, Map<String, String>> result = newMap();
		for (Iterator<String> i = toBeImported.iterator(); i.hasNext();) {
			String ep = i.next();
			Map<String, String> parameters = exports.get(ep);

			String noimport = parameters.get(NO_IMPORT_DIRECTIVE);
			if (noimport != null && noimport.equalsIgnoreCase("true"))
				continue;

			// // we can't substitute when there is no version
			// String version = parameters.get(VERSION_ATTRIBUTE);
			// if (version == null) {
			// if (isPedantic())
			// warning(
			// "Cannot automatically import exported package %s because it has no version defined",
			// ep);
			// continue;
			// }

			parameters = newMap(parameters);
			parameters.remove(VERSION_ATTRIBUTE);
			result.put(ep, parameters);
		}
		return result;
	}

	private <T> boolean intersects(Collection<T> aa, Collection<T> bb) {
		if (aa.equals(bb))
			return true;

		if (aa.size() > bb.size())
			return intersects(bb, aa);

		for (T t : aa)
			if (bb.contains(t))
				return true;
		return false;
	}

	public boolean referred(String packageName) {
		// return true;
		for (Map.Entry<String, Set<String>> contained : uses.entrySet()) {
			if (!contained.getKey().equals(packageName)) {
				if (contained.getValue().contains(packageName))
					return true;
			}
		}
		return false;
	}

	/**
	 * Create the imports/exports by parsing
	 * 
	 * @throws IOException
	 */
	void analyzeClasspath() throws Exception {
		classpathExports = newHashMap();
		for (Iterator<Jar> c = getClasspath().iterator(); c.hasNext();) {
			Jar current = c.next();
			checkManifest(current);
			for (Iterator<String> j = current.getDirectories().keySet().iterator(); j.hasNext();) {
				String dir = j.next();
				Resource resource = current.getResource(dir + "/packageinfo");
				if (resource != null) {
					InputStream in = resource.openInputStream();
					try {
						String version = parsePackageInfo(in);
						setPackageInfo(dir, VERSION_ATTRIBUTE, version);
					} finally {
						in.close();
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param jar
	 */
	void checkManifest(Jar jar) {
		try {
			Manifest m = jar.getManifest();
			if (m != null) {
				String exportHeader = m.getMainAttributes().getValue(EXPORT_PACKAGE);
				if (exportHeader != null) {
					Map<String, Map<String, String>> exported = parseHeader(exportHeader);
					if (exported != null) {
						for (Map.Entry<String, Map<String, String>> entry : exported.entrySet()) {
							if (!classpathExports.containsKey(entry.getKey())) {
								classpathExports.put(entry.getKey(), entry.getValue());
							}
						}
					}
				}
			}
		} catch (Exception e) {
			warning("Erroneous Manifest for " + jar + " " + e);
		}
	}

	/**
	 * Find some more information about imports in manifest and other places.
	 */
	void augmentImports() {

		for (String packageName : imports.keySet()) {
			setProperty(CURRENT_PACKAGE, packageName);
			try {
				Map<String, String> importAttributes = imports.get(packageName);
				Map<String, String> exporterAttributes = classpathExports.get(packageName);
				if (exporterAttributes == null)
					exporterAttributes = exports.get(packageName);

				if (exporterAttributes != null) {
					augmentVersion(importAttributes, exporterAttributes);
					augmentMandatory(importAttributes, exporterAttributes);
					if (exporterAttributes.containsKey(IMPORT_DIRECTIVE))
						importAttributes.put(IMPORT_DIRECTIVE,
								exporterAttributes.get(IMPORT_DIRECTIVE));
				}

				fixupAttributes(importAttributes);
				removeAttributes(importAttributes);

			} finally {
				unsetProperty(CURRENT_PACKAGE);
			}
		}
	}

	/**
	 * Provide any macro substitutions and versions for exported packages.
	 */

	void augmentExports() {
		for (String packageName : exports.keySet()) {
			setProperty(CURRENT_PACKAGE, packageName);
			try {
				Map<String, String> attributes = exports.get(packageName);
				Map<String, String> exporterAttributes = classpathExports.get(packageName);
				if (exporterAttributes == null)
					continue;

				for (Map.Entry<String, String> entry : exporterAttributes.entrySet()) {
					String key = entry.getKey();
					if (key.equalsIgnoreCase(SPECIFICATION_VERSION))
						key = VERSION_ATTRIBUTE;
					if (!key.endsWith(":") && !attributes.containsKey(key)) {
						attributes.put(key, entry.getValue());
					}
				}

				fixupAttributes(attributes);
				removeAttributes(attributes);

			} finally {
				unsetProperty(CURRENT_PACKAGE);
			}
		}
	}

	/**
	 * Fixup Attributes
	 * 
	 * Execute any macros on an export and
	 */

	void fixupAttributes(Map<String, String> attributes) {
		// Convert any attribute values that have macros.
		for (String key : attributes.keySet()) {
			String value = attributes.get(key);
			if (value.indexOf('$') >= 0) {
				value = getReplacer().process(value);
				attributes.put(key, value);
			}
		}

	}

	/*
	 * Remove the attributes mentioned in the REMOVE_ATTRIBUTE_DIRECTIVE.
	 */

	void removeAttributes(Map<String, String> attributes) {
		// You can add a remove-attribute: directive with a regular
		// expression for attributes that need to be removed. We also
		// remove all attributes that have a value of !. This allows
		// you to use macros with ${if} to remove values.
		String remove = attributes.remove(REMOVE_ATTRIBUTE_DIRECTIVE);
		Instruction removeInstr = null;

		if (remove != null)
			removeInstr = Instruction.getPattern(remove);

		for (Iterator<Map.Entry<String, String>> i = attributes.entrySet().iterator(); i.hasNext();) {
			Map.Entry<String, String> entry = i.next();
			if (entry.getValue().equals("!"))
				i.remove();
			else if (removeInstr != null && removeInstr.matches((String) entry.getKey()))
				i.remove();
			else {
				// Not removed ...
			}
		}
	}

	/**
	 * If we use an import with mandatory attributes we better all use them
	 * 
	 * @param currentAttributes
	 * @param exporter
	 */
	private void augmentMandatory(Map<String, String> currentAttributes,
			Map<String, String> exporter) {
		String mandatory = (String) exporter.get("mandatory:");
		if (mandatory != null) {
			String[] attrs = mandatory.split("\\s*,\\s*");
			for (int i = 0; i < attrs.length; i++) {
				if (!currentAttributes.containsKey(attrs[i]))
					currentAttributes.put(attrs[i], exporter.get(attrs[i]));
			}
		}
	}

	/**
	 * Check if we can augment the version from the exporter.
	 * 
	 * We allow the version in the import to specify a @ which is replaced with
	 * the exporter's version.
	 * 
	 * @param currentAttributes
	 * @param exporter
	 */
	private void augmentVersion(Map<String, String> currentAttributes, Map<String, String> exporter) {

		String exportVersion = (String) exporter.get(VERSION_ATTRIBUTE);
		if (exportVersion == null)
			return;

		exportVersion = cleanupVersion(exportVersion);
		String importRange = currentAttributes.get(VERSION_ATTRIBUTE);
		boolean impl = isTrue(currentAttributes.get(PROVIDE_DIRECTIVE));
		try {
			setProperty("@", exportVersion);

			if (importRange != null) {
				importRange = cleanupVersion(importRange);
				importRange = getReplacer().process(importRange);
			} else
				importRange = getVersionPolicy(impl);

		} finally {
			unsetProperty("@");
		}
		// See if we can borrow the version
		// we must replace the ${@} with the version we
		// found this can be useful if you want a range to start
		// with the found version.
		currentAttributes.put(VERSION_ATTRIBUTE, importRange);
	}

	/**
	 * Calculate a version from a version policy.
	 * 
	 * @param version
	 *            The actual exported version
	 * @param impl
	 *            true for implementations and false for clients
	 */

	String calculateVersionRange(String version, boolean impl) {
		setProperty("@", version);
		try {
			return getVersionPolicy(impl);
		} finally {
			unsetProperty("@");
		}
	}

	/**
	 * Add the uses clauses. This method iterates over the exports and cal
	 * 
	 * @param exports
	 * @param uses
	 * @throws MojoExecutionException
	 */
	void doUses(Map<String, Map<String, String>> exports, Map<String, Set<String>> uses,
			Map<String, Map<String, String>> imports) {
		if ("true".equalsIgnoreCase(getProperty(NOUSES)))
			return;

		for (Iterator<String> i = exports.keySet().iterator(); i.hasNext();) {
			String packageName = i.next();
			setProperty(CURRENT_PACKAGE, packageName);
			try {
				doUses(packageName, exports, uses, imports);
			} finally {
				unsetProperty(CURRENT_PACKAGE);
			}

		}
	}

	/**
	 * @param packageName
	 * @param exports
	 * @param uses
	 * @param imports
	 */
	protected void doUses(String packageName, Map<String, Map<String, String>> exports,
			Map<String, Set<String>> uses, Map<String, Map<String, String>> imports) {
		Map<String, String> clause = exports.get(packageName);

		// Check if someone already set the uses: directive
		String override = clause.get(USES_DIRECTIVE);
		if (override == null)
			override = USES_USES;

		// Get the used packages
		Set<String> usedPackages = uses.get(packageName);

		if (usedPackages != null) {

			// Only do a uses on exported or imported packages
			// and uses should also not contain our own package
			// name
			Set<String> sharedPackages = new HashSet<String>();
			sharedPackages.addAll(imports.keySet());
			sharedPackages.addAll(exports.keySet());
			usedPackages.retainAll(sharedPackages);
			usedPackages.remove(packageName);

			StringBuffer sb = new StringBuffer();
			String del = "";
			for (Iterator<String> u = usedPackages.iterator(); u.hasNext();) {
				String usedPackage = u.next();
				if (!usedPackage.startsWith("java.")) {
					sb.append(del);
					sb.append(usedPackage);
					del = ",";
				}
			}
			if (override.indexOf('$') >= 0) {
				setProperty(CURRENT_USES, sb.toString());
				override = getReplacer().process(override);
				unsetProperty(CURRENT_USES);
			} else
				// This is for backward compatibility 0.0.287
				// can be deprecated over time
				override = override.replaceAll(USES_USES, sb.toString()).trim();

			if (override.endsWith(","))
				override = override.substring(0, override.length() - 1);
			if (override.startsWith(","))
				override = override.substring(1);
			if (override.length() > 0) {
				clause.put(USES_DIRECTIVE, override);
			}
		}
	}

	/**
	 * Transitively remove all elemens from unreachable through the uses link.
	 * 
	 * @param name
	 * @param unreachable
	 */
	void removeTransitive(String name, Set<String> unreachable) {
		if (!unreachable.contains(name))
			return;

		unreachable.remove(name);

		Set<String> ref = uses.get(name);
		if (ref != null) {
			for (Iterator<String> r = ref.iterator(); r.hasNext();) {
				String element = (String) r.next();
				removeTransitive(element, unreachable);
			}
		}
	}

	/**
	 * Helper method to set the package info
	 * 
	 * @param dir
	 * @param key
	 * @param value
	 */
	void setPackageInfo(String dir, String key, String value) {
		if (value != null) {
			String pack = dir.replace('/', '.');
			Map<String, String> map = classpathExports.get(pack);
			if (map == null) {
				map = new HashMap<String, String>();
				classpathExports.put(pack, map);
			}
			if (!map.containsKey(VERSION_ATTRIBUTE))
				map.put(key, value);
			else if (!map.get(VERSION_ATTRIBUTE).equals(value)) {
				// System.out.println("duplicate version info for " + dir + " "
				// + value + " and " + map.get(VERSION_ATTRIBUTE));
			}
		}
	}

	public void close() {
		if (diagnostics) {
			PrintStream out = System.out;
			out.printf("Current directory            : %s\n", new File("").getAbsolutePath());
			out.println("Classpath used");
			for (Jar jar : getClasspath()) {
				out.printf("File                                : %s\n", jar.getSource());
				out.printf("File abs path                       : %s\n", jar.getSource()
						.getAbsolutePath());
				out.printf("Name                                : %s\n", jar.getName());
				Map<String, Map<String, Resource>> dirs = jar.getDirectories();
				for (Map.Entry<String, Map<String, Resource>> entry : dirs.entrySet()) {
					Map<String, Resource> dir = entry.getValue();
					String name = entry.getKey().replace('/', '.');
					if (dir != null) {
						out.printf("                                      %-30s %d\n", name,
								dir.size());
					} else {
						out.printf("                                      %-30s <<empty>>\n", name);
					}
				}
			}
		}

		super.close();
		if (dot != null)
			dot.close();

		if (classpath != null)
			for (Iterator<Jar> j = classpath.iterator(); j.hasNext();) {
				Jar jar = j.next();
				jar.close();
			}
	}

	/**
	 * Findpath looks through the contents of the JAR and finds paths that end
	 * with the given regular expression
	 * 
	 * ${findpath (; reg-expr (; replacement)? )? }
	 * 
	 * @param args
	 * @return
	 */
	public String _findpath(String args[]) {
		return findPath("findpath", args, true);
	}

	public String _findname(String args[]) {
		return findPath("findname", args, false);
	}

	String findPath(String name, String[] args, boolean fullPathName) {
		if (args.length > 3) {
			warning("Invalid nr of arguments to " + name + " " + Arrays.asList(args)
					+ ", syntax: ${" + name + " (; reg-expr (; replacement)? )? }");
			return null;
		}

		String regexp = ".*";
		String replace = null;

		switch (args.length) {
		case 3:
			replace = args[2];
		case 2:
			regexp = args[1];
		}
		StringBuffer sb = new StringBuffer();
		String del = "";

		Pattern expr = Pattern.compile(regexp);
		for (Iterator<String> e = dot.getResources().keySet().iterator(); e.hasNext();) {
			String path = e.next();
			if (!fullPathName) {
				int n = path.lastIndexOf('/');
				if (n >= 0) {
					path = path.substring(n + 1);
				}
			}

			Matcher m = expr.matcher(path);
			if (m.matches()) {
				if (replace != null)
					path = m.replaceAll(replace);

				sb.append(del);
				sb.append(path);
				del = ", ";
			}
		}
		return sb.toString();
	}

	public void putAll(Map<String, String> additional, boolean force) {
		for (Iterator<Map.Entry<String, String>> i = additional.entrySet().iterator(); i.hasNext();) {
			Map.Entry<String, String> entry = i.next();
			if (force || getProperties().get(entry.getKey()) == null)
				setProperty((String) entry.getKey(), (String) entry.getValue());
		}
	}

	boolean	firstUse	= true;

	public List<Jar> getClasspath() {
		if (firstUse) {
			firstUse = false;
			String cp = getProperty(CLASSPATH);
			if (cp != null)
				for (String s : split(cp)) {
					Jar jar = getJarFromName(s, "getting classpath");
					if (jar != null)
						addClasspath(jar);
					else
						warning("Cannot find entry on -classpath: %s", s);
				}
		}
		return classpath;
	}

	public void addClasspath(Jar jar) {
		if (isPedantic() && jar.getResources().isEmpty())
			warning("There is an empty jar or directory on the classpath: " + jar.getName());

		classpath.add(jar);
	}

	public void addClasspath(File cp) throws IOException {
		if (!cp.exists())
			warning("File on classpath that does not exist: " + cp);
		Jar jar = new Jar(cp);
		addClose(jar);
		classpath.add(jar);
	}

	public void clear() {
		classpath.clear();
	}

	public Jar getTarget() {
		return dot;
	}

	protected Map<String, Clazz> analyzeBundleClasspath(Jar dot,
			Map<String, Map<String, String>> bundleClasspath,
			Map<String, Map<String, String>> contained, Map<String, Map<String, String>> referred,
			Map<String, Set<String>> uses) throws Exception {
		Map<String, Clazz> classSpace = new HashMap<String, Clazz>();
		Set<String> hide = Create.set();
		boolean containsDirectory = false;

		for (String path : bundleClasspath.keySet()) {
			if (dot.getDirectories().containsKey(path)) {
				containsDirectory = true;
				break;
			}
		}

		if (bundleClasspath.isEmpty()) {
			analyzeJar(dot, "", classSpace, contained, referred, uses, hide, true);
		} else {
			for (String path : bundleClasspath.keySet()) {
				Map<String, String> info = bundleClasspath.get(path);

				if (path.equals(".")) {
					analyzeJar(dot, "", classSpace, contained, referred, uses, hide,
							!containsDirectory);
					continue;
				}
				//
				// There are 3 cases:
				// - embedded JAR file
				// - directory
				// - error
				//

				Resource resource = dot.getResource(path);
				if (resource != null) {
					try {
						Jar jar = new Jar(path);
						addClose(jar);
						EmbeddedResource.build(jar, resource);
						analyzeJar(jar, "", classSpace, contained, referred, uses, hide, true);
					} catch (Exception e) {
						warning("Invalid bundle classpath entry: " + path + " " + e);
					}
				} else {
					if (dot.getDirectories().containsKey(path)) {
						// if directories are used, we should not have dot as we
						// would have the classes in these directories on the
						// class
						// path twice.
						if (bundleClasspath.containsKey("."))
							warning("Bundle-ClassPath uses a directory '%s' as well as '.', this implies the directory is seen \n"
									+ "twice by the class loader. bnd assumes that the classes are only "
									+ "loaded from '%s'. It is better to unroll the directory to create a flat bundle.",
									path, path);
						analyzeJar(dot, Processor.appendPath(path) + "/", classSpace, contained,
								referred, uses, hide, true);
					} else {
						if (!"optional".equals(info.get(RESOLUTION_DIRECTIVE)))
							warning("No sub JAR or directory " + path);
					}
				}
			}

			for (Clazz c : classSpace.values()) {
				formats.add(c.getFormat());
			}
		}
		return classSpace;
	}

	/**
	 * We traverse through all the classes that we can find and calculate the
	 * contained and referred set and uses. This method ignores the Bundle
	 * classpath.
	 * 
	 * @param jar
	 * @param contained
	 * @param referred
	 * @param uses
	 * @throws IOException
	 */
	private void analyzeJar(Jar jar, String prefix, Map<String, Clazz> classSpace,
			Map<String, Map<String, String>> contained, Map<String, Map<String, String>> referred,
			Map<String, Set<String>> uses, Set<String> hide, boolean reportWrongPath)
			throws Exception {

		next: for (String path : jar.getResources().keySet()) {
			if (path.startsWith(prefix) /* && !hide.contains(path) */) {
				hide.add(path);
				String relativePath = path.substring(prefix.length());

				// // TODO this check (and the whole hide) is likely redundant
				// // it only protects against repeated checks for non-class
				// // bundle resources, but should not affect results otherwise.
				// if (!hide.add(relativePath)) {
				// continue;
				// }

				// Check if we'd already had this one.
				// Notice that we're flattening the space because
				// this is what class loaders do.
				if (classSpace.containsKey(relativePath))
					continue;

				String pack = getPackage(relativePath);

				if (pack != null && !contained.containsKey(pack)) {
					// For each package we encounter for the first
					// time
					if (!isMetaData(relativePath)) {

						Map<String, String> info = newMap();
						contained.put(pack, info);

						Resource pinfo = jar.getResource(prefix + pack.replace('.', '/')
								+ "/packageinfo");
						if (pinfo != null) {
							InputStream in = pinfo.openInputStream();
							String version;
							try {
								version = parsePackageInfo(in);
							} finally {
								in.close();
							}
							if (version != null)
								info.put(VERSION_ATTRIBUTE, version);
						}
					}
				}

				// Check class resources, we need to analyze them
				if (path.endsWith(".class")) {
					Resource resource = jar.getResource(path);
					Clazz clazz;

					try {
						InputStream in = resource.openInputStream();
						clazz = new Clazz(relativePath, resource);
						try {
							// Check if we have a package-info
							if (relativePath.endsWith("/package-info.class")) {
								// package-info can contain an Export annotation
								Map<String, String> info = contained.get(pack);
								parsePackageInfoClass(clazz, info);
							} else {
								// Otherwise we just parse it simply
								clazz.parseClassFile();
							}
						} finally {
							in.close();
						}
					} catch (Throwable e) {
						error("Invalid class file: " + relativePath, e);
						e.printStackTrace();
						continue next;
					}

					String calculatedPath = clazz.getClassName() + ".class";
					if (!calculatedPath.equals(relativePath)) {
						if (!isNoBundle() && reportWrongPath) {
							error("Class in different directory than declared. Path from class name is "
									+ calculatedPath
									+ " but the path in the jar is "
									+ relativePath + " from '" + jar + "'");
						}
					}

					classSpace.put(relativePath, clazz);

					// Look at the referred packages
					// and copy them to our baseline
					for (String p : clazz.getReferred()) {
						Map<String, String> attrs = referred.get(p);
						if (attrs == null) {
							attrs = newMap();
							referred.put(p, attrs);
						}
					}

					// Add all the used packages
					// to this package
					Set<String> t = uses.get(pack);
					if (t == null)
						uses.put(pack, t = new LinkedHashSet<String>());
					t.addAll(clazz.getReferred());
					t.remove(pack);
				}
			}
		}
	}

	static Pattern	OBJECT_REFERENCE	= Pattern.compile("L([^/]+/)*([^;]+);");

	private void parsePackageInfoClass(final Clazz clazz, final Map<String, String> info)
			throws Exception {
		clazz.parseClassFileWithCollector(new ClassDataCollector() {
			@Override public void annotation(Annotation a) {
				if (a.name.equals(Clazz.toDescriptor(aQute.bnd.annotation.Version.class))) {

					// Check version
					String version = a.get("value");
					if (!info.containsKey(Constants.VERSION_ATTRIBUTE)) {
						if (version != null) {
							version = getReplacer().process(version);
							if (Verifier.VERSION.matcher(version).matches())
								info.put(VERSION_ATTRIBUTE, version);
							else
								error("Export annotatio in %s has invalid version info: %s", clazz,
										version);
						}
					} else {
						// Verify this matches with packageinfo
						String presentVersion = info.get(VERSION_ATTRIBUTE);
						try {
							Version av = new Version(presentVersion);
							Version bv = new Version(version);
							if (!av.equals(bv)) {
								error("Version from annotation for %s differs with packageinfo or Manifest",
										Clazz.getPackage(clazz.className));
							}
						} catch (Exception e) {
							// Ignore
						}
					}
				} else if (a.name.equals(Clazz.toDescriptor(Export.class))) {

					// Check mandatory attributes
					Map<String, String> attrs = doAttrbutes((Object[]) a.get(Export.MANDATORY),
							clazz, getReplacer());
					if (!attrs.isEmpty()) {
						info.putAll(attrs);
						info.put(MANDATORY_DIRECTIVE, Processor.join(attrs.keySet()));
					}

					// Check optional attributes
					attrs = doAttrbutes((Object[]) a.get(Export.OPTIONAL), clazz, getReplacer());
					if (!attrs.isEmpty()) {
						info.putAll(attrs);
					}

					// Check Included classes
					Object[] included = a.get(Export.INCLUDE);
					if (included != null && included.length > 0) {
						StringBuilder sb = new StringBuilder();
						String del = "";
						for (Object i : included) {
							Matcher m = OBJECT_REFERENCE.matcher((String) i);
							if (m.matches()) {
								sb.append(del);
								sb.append(m.group(2));
								del = ",";
							}
						}
						info.put(INCLUDE_DIRECTIVE, sb.toString());
					}

					// Check Excluded classes
					Object[] excluded = a.get(Export.EXCLUDE);
					if (excluded != null && excluded.length > 0) {
						StringBuilder sb = new StringBuilder();
						String del = "";
						for (Object i : excluded) {
							Matcher m = OBJECT_REFERENCE.matcher((String) i);
							if (m.matches()) {
								sb.append(del);
								sb.append(m.group(2));
								del = ",";
							}
						}
						info.put(EXCLUDE_DIRECTIVE, sb.toString());
					}

					// Check Uses
					Object[] uses = a.get(Export.USES);
					if (uses != null && uses.length > 0) {
						String old = info.get(USES_DIRECTIVE);
						if (old == null)
							old = "";
						StringBuilder sb = new StringBuilder(old);
						String del = sb.length() == 0 ? "" : ",";

						for (Object use : uses) {
							sb.append(del);
							sb.append(use);
							del = ",";
						}
						info.put(USES_DIRECTIVE, sb.toString());
					}
				}
			}

		});
	}

	/**
	 * Clean up version parameters. Other builders use more fuzzy definitions of
	 * the version syntax. This method cleans up such a version to match an OSGi
	 * version.
	 * 
	 * @param VERSION_STRING
	 * @return
	 */
	static Pattern	fuzzyVersion		= Pattern
												.compile(
														"(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9](.*))?",
														Pattern.DOTALL);
	static Pattern	fuzzyVersionRange	= Pattern
												.compile(
														"(\\(|\\[)\\s*([-\\da-zA-Z.]+)\\s*,\\s*([-\\da-zA-Z.]+)\\s*(\\]|\\))",
														Pattern.DOTALL);
	static Pattern	fuzzyModifier		= Pattern.compile("(\\d+[.-])*(.*)", Pattern.DOTALL);

	static Pattern	nummeric			= Pattern.compile("\\d*");

	static public String cleanupVersion(String version) {
		Matcher m = Verifier.VERSIONRANGE.matcher(version);

		if (m.matches()) {
			return version;
		}

		m = fuzzyVersionRange.matcher(version);
		if (m.matches()) {
			String prefix = m.group(1);
			String first = m.group(2);
			String last = m.group(3);
			String suffix = m.group(4);
			return prefix + cleanupVersion(first) + "," + cleanupVersion(last) + suffix;
		} else {
			m = fuzzyVersion.matcher(version);
			if (m.matches()) {
				StringBuffer result = new StringBuffer();
				String major = removeLeadingZeroes(m.group(1));
				String minor = removeLeadingZeroes(m.group(3));
				String micro = removeLeadingZeroes(m.group(5));
				String qualifier = m.group(7);

				if (major != null) {
					result.append(major);
					if (minor != null) {
						result.append(".");
						result.append(minor);
						if (micro != null) {
							result.append(".");
							result.append(micro);
							if (qualifier != null) {
								result.append(".");
								cleanupModifier(result, qualifier);
							}
						} else if (qualifier != null) {
							result.append(".0.");
							cleanupModifier(result, qualifier);
						}
					} else if (qualifier != null) {
						result.append(".0.0.");
						cleanupModifier(result, qualifier);
					}
					return result.toString();
				}
			}
		}
		return version;
	}

	private static String removeLeadingZeroes(String group) {
		int n = 0;
		while (group != null && n < group.length() - 1 && group.charAt(n) == '0')
			n++;
		if (n == 0)
			return group;

		return group.substring(n);
	}

	static void cleanupModifier(StringBuffer result, String modifier) {
		Matcher m = fuzzyModifier.matcher(modifier);
		if (m.matches())
			modifier = m.group(2);

		for (int i = 0; i < modifier.length(); i++) {
			char c = modifier.charAt(i);
			if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
					|| c == '_' || c == '-')
				result.append(c);
		}
	}

	/**
	 * Decide if the package is a metadata package.
	 * 
	 * @param pack
	 * @return
	 */
	boolean isMetaData(String pack) {
		for (int i = 0; i < METAPACKAGES.length; i++) {
			if (pack.startsWith(METAPACKAGES[i]))
				return true;
		}
		return false;
	}

	public String getPackage(String clazz) {
		int n = clazz.lastIndexOf('/');
		if (n < 0)
			return ".";
		return clazz.substring(0, n).replace('/', '.');
	}

	//
	// We accept more than correct OSGi versions because in a later
	// phase we actually cleanup maven versions. But it is a bit yucky
	//
	static String parsePackageInfo(InputStream jar) throws IOException {
		try {
			Properties p = new Properties();
			p.load(jar);
			jar.close();
			if (p.containsKey("version")) {
				return p.getProperty("version");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	final static String	DEFAULT_PROVIDER_POLICY	= "${range;[==,=+)}";
	final static String	DEFAULT_CONSUMER_POLICY	= "${range;[==,+)}";

	@SuppressWarnings("deprecation") public String getVersionPolicy(boolean implemented) {
		if (implemented) {
			String s = getProperty(PROVIDER_POLICY);
			if (s != null)
				return s;

			s = getProperty(VERSIONPOLICY_IMPL);
			if (s != null)
				return s;

			return getProperty(VERSIONPOLICY, DEFAULT_PROVIDER_POLICY);
		} else {
			String s = getProperty(CONSUMER_POLICY);
			if (s != null)
				return s;

			s = getProperty(VERSIONPOLICY_USES);
			if (s != null)
				return s;

			return getProperty(VERSIONPOLICY, DEFAULT_CONSUMER_POLICY);
		}
		// String vp = implemented ? getProperty(VERSIONPOLICY_IMPL) :
		// getProperty(VERSIONPOLICY_USES);
		//
		// if (vp != null)
		// return vp;
		//
		// if (implemented)
		// return getProperty(VERSIONPOLICY_IMPL, "{$range;[==,=+}");
		// else
		// return getProperty(VERSIONPOLICY, "${range;[==,+)}");
	}

	/**
	 * The extends macro traverses all classes and returns a list of class names
	 * that extend a base class.
	 */

	static String	_classesHelp	= "${classes;'implementing'|'extending'|'importing'|'named'|'version'|'any';<pattern>}, Return a list of class fully qualified class names that extend/implement/import any of the contained classes matching the pattern\n";

	public String _classes(String... args) throws Exception {
		// Macro.verifyCommand(args, _classesHelp, new
		// Pattern[]{null,Pattern.compile("(implementing|implements|extending|extends|importing|imports|any)"),
		// null}, 3,3);

		Collection<Clazz> matched = getClasses(args);
		if (matched.isEmpty())
			return "";

		return join(matched);
	}

	public Collection<Clazz> getClasses(String... args) throws Exception {

		Set<Clazz> matched = new HashSet<Clazz>(classspace.values());
		for (int i = 1; i < args.length; i++) {
			if (args.length < i + 1)
				throw new IllegalArgumentException(
						"${classes} macro must have odd number of arguments. " + _classesHelp);

			String typeName = args[i];
			if (typeName.equalsIgnoreCase("extending"))
				typeName = "extends";
			else if (typeName.equalsIgnoreCase("importing"))
				typeName = "imports";
			else if (typeName.equalsIgnoreCase("implementing"))
				typeName = "implements";

			Clazz.QUERY type = Clazz.QUERY.valueOf(typeName.toUpperCase());

			if (type == null)
				throw new IllegalArgumentException("${classes} has invalid type: " + typeName
						+ ". " + _classesHelp);

			Instruction instr = null;
			if (Clazz.HAS_ARGUMENT.contains(type)) {
				StringBuilder sb = new StringBuilder();
				String s = args[++i];
				if (type == QUERY.ANNOTATION) {
					// Annotations use the descriptor format ...
					// But at least they're always an object
					sb.append("L");
					for (int ci = 0; ci < s.length(); ci++) {
						char c = s.charAt(ci);
						if (c == '.')
							sb.append("/");
						else
							sb.append(c);
					}
					sb.append(';');
				} else {
					// The argument is declared as a dotted name but the classes
					// use a slashed named. So convert the name before we make
					// it a instruction. We also have to take into account
					// that some classes are nested and use $ for separator
					for (int ci = 0; ci < s.length(); ci++) {
						char c = s.charAt(ci);
						if (c == '.')
							sb.append("(/|\\$)");
						else
							sb.append(c);
					}
				}
				instr = Instruction.getPattern(sb.toString());
			}
			for (Iterator<Clazz> c = matched.iterator(); c.hasNext();) {
				Clazz clazz = c.next();
				if (!clazz.is(type, instr, this)) {
					c.remove();
				}
			}
		}
		return matched;
	}

	/**
	 * Get the exporter of a package ...
	 */

	public String _exporters(String args[]) throws Exception {
		Macro.verifyCommand(
				args,
				"${exporters;<packagename>}, returns the list of jars that export the given package",
				null, 2, 2);
		StringBuilder sb = new StringBuilder();
		String del = "";
		String pack = args[1].replace('.', '/');
		for (Jar jar : classpath) {
			if (jar.getDirectories().containsKey(pack)) {
				sb.append(del);
				sb.append(jar.getName());
			}
		}
		return sb.toString();
	}

	public Map<String, Clazz> getClassspace() {
		return classspace;
	}

	/**
	 * Locate a resource on the class path.
	 * 
	 * @param path
	 *            Path of the reosurce
	 * @return A resource or <code>null</code>
	 */
	public Resource findResource(String path) {
		for (Jar entry : getClasspath()) {
			Resource r = entry.getResource(path);
			if (r != null)
				return r;
		}
		return null;
	}

	/**
	 * Find a clazz on the class path. This class has been parsed.
	 * 
	 * @param path
	 * @return
	 */
	public Clazz findClass(String path) throws Exception {
		Clazz c = classspace.get(path);
		if (c != null)
			return c;

		c = importedClassesCache.get(path);
		if (c != null)
			return c;

		Resource r = findResource(path);
		if (r != null) {
			c = new Clazz(path, r);
			c.parseClassFile();
			importedClassesCache.put(path, c);
		}
		return c;
	}

	/**
	 * Answer the bundle version.
	 * 
	 * @return
	 */
	public String getVersion() {
		String version = getProperty(BUNDLE_VERSION);
		if (version == null)
			version = "0.0.0";
		return version;
	}

	public boolean isNoBundle() {
		return isTrue(getProperty(RESOURCEONLY)) || isTrue(getProperty(NOMANIFEST));
	}

	public void referTo(String impl) {
		String pack = Clazz.getPackage(impl);
		if (!referred.containsKey(pack))
			referred.put(pack, new LinkedHashMap<String, String>());
	}

	/**
	 * Calculate the groups inside the bundle. A group consists of packages that
	 * have a reference to each other.
	 */

	public MultiMap<Set<String>, String> getGroups() {
		MultiMap<String, String> map = new MultiMap<String, String>();
		Set<String> keys = uses.keySet();

		for (Map.Entry<String, Set<String>> entry : uses.entrySet()) {
			Set<String> newSet = new HashSet<String>(entry.getValue());
			newSet.retainAll(keys);
			map.put(entry.getKey(), newSet);
		}

		// Calculate strongly connected packages
		Set<Set<String>> scc = Tarjan.tarjan(map);

		MultiMap<Set<String>, String> grouped = new MultiMap<Set<String>, String>();
		for (Set<String> group : scc) {
			for (String p : group) {
				grouped.addAll(group, uses.get(p));
			}
		}
		return grouped;
	}

	/**
	 * Ensure that we are running on the correct bnd.
	 */
	void doRequireBnd() {
		Map<String, String> require = OSGiHeader.parseProperties(getProperty(REQUIRE_BND));
		if (require == null || require.isEmpty())
			return;

		Hashtable<String, String> map = new Hashtable<String, String>();
		map.put(Constants.VERSION_FILTER, getBndVersion());

		for (String filter : require.keySet()) {
			try {
				Filter f = new Filter(filter);
				if (f.match(map))
					continue;
				error("%s fails %s", REQUIRE_BND, require.get(filter));
			} catch (Exception t) {
				error("%s with value %s throws exception", t, REQUIRE_BND, require);
			}
		}
	}

	/**
	 * md5 macro
	 */

	static String	_md5Help	= "${md5;path}";

	public String _md5(String args[]) throws Exception {
		Macro.verifyCommand(args, _md5Help,
				new Pattern[] { null, null, Pattern.compile("base64|hex") }, 2, 3);

		Digester<MD5> digester = MD5.getDigester();
		Resource r = dot.getResource(args[1]);
		if (r == null)
			throw new FileNotFoundException("From " + digester + ", not found " + args[1]);

		IO.copy(r.openInputStream(), digester);
		boolean hex = args.length > 2 && args[2].equals("hex");
		if (hex)
			return Hex.toHexString(digester.digest().digest());
		else
			return Base64.encodeBase64(digester.digest().digest());
	}

	/**
	 * SHA1 macro
	 */

	static String	_sha1Help	= "${sha1;path}";

	public String _sha1(String args[]) throws Exception {
		Macro.verifyCommand(args, _sha1Help,
				new Pattern[] { null, null, Pattern.compile("base64|hex") }, 2, 3);
		Digester<SHA1> digester = SHA1.getDigester();
		Resource r = dot.getResource(args[1]);
		if (r == null)
			throw new FileNotFoundException("From sha1, not found " + args[1]);

		IO.copy(r.openInputStream(), digester);
		return Base64.encodeBase64(digester.digest().digest());
	}
}

package aQute.bnd.osgi;

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
import static aQute.libg.generics.Create.*;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.*;
import java.util.jar.Attributes.Name;
import java.util.regex.*;

import aQute.bnd.annotation.*;
import aQute.bnd.header.*;
import aQute.bnd.osgi.Descriptors.Descriptor;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.service.*;
import aQute.bnd.version.*;
import aQute.bnd.version.Version;
import aQute.lib.base64.*;
import aQute.lib.collections.*;
import aQute.lib.filter.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.libg.cryptography.*;
import aQute.libg.generics.*;
import aQute.libg.reporter.*;

public class Analyzer extends Processor {
	private final SortedSet<Clazz.JAVA>				ees						= new TreeSet<Clazz.JAVA>();
	static Properties								bndInfo;

	// Bundle parameters
	private Jar										dot;
	private final Packages							contained				= new Packages();
	private final Packages							referred				= new Packages();
	private Packages								exports;
	private Packages								imports;
	private TypeRef									activator;

	// Global parameters
	private final MultiMap<PackageRef,PackageRef>	uses					= new MultiMap<PackageRef,PackageRef>(
																					PackageRef.class, PackageRef.class,
																					true);
	private final MultiMap<PackageRef,PackageRef>	apiUses					= new MultiMap<PackageRef,PackageRef>(
																					PackageRef.class, PackageRef.class,
																					true);
	private final Packages							classpathExports		= new Packages();
	private final Descriptors						descriptors				= new Descriptors();
	private final List<Jar>							classpath				= list();
	private final Map<TypeRef,Clazz>				classspace				= map();
	private final Map<TypeRef,Clazz>				importedClassesCache	= map();
	private boolean									analyzed				= false;
	private boolean									diagnostics				= false;
	private boolean									inited					= false;
	final protected AnalyzerMessages				msgs					= ReporterMessages.base(this,
																					AnalyzerMessages.class);

	public Analyzer(Processor parent) {
		super(parent);
	}

	public Analyzer() {}

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
		}
		finally {
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
			uses.clear();
			apiUses.clear();
			classspace.clear();
			classpathExports.clear();

			// Parse all the class in the
			// the jar according to the OSGi bcp
			analyzeBundleClasspath();

			//
			// calculate class versions in use
			//
			for (Clazz c : classspace.values()) {
				ees.add(c.getFormat());
			}

			//
			// Get exported packages from the
			// entries on the classpath
			//

			for (Jar current : getClasspath()) {
				getExternalExports(current, classpathExports);
				for (String dir : current.getDirectories().keySet()) {
					PackageRef packageRef = getPackageRef(dir);
					Resource resource = current.getResource(dir + "/packageinfo");
					getExportVersionsFromPackageInfo(packageRef, resource, classpathExports);
				}
			}

			// Handle the bundle activator

			String s = getProperty(BUNDLE_ACTIVATOR);
			if (s != null) {
				activator = getTypeRefFromFQN(s);
				referTo(activator);
				trace("activator %s %s", s, activator);
			}

			// Execute any plugins
			// TODO handle better reanalyze
			doPlugins();

			Jar extra = getExtra();
			while (extra != null) {
				dot.addAll(extra);
				analyzeJar(extra, "", true);
				extra = getExtra();
			}

			referred.keySet().removeAll(contained.keySet());

			//
			// EXPORTS
			//
			{
				Set<Instruction> unused = Create.set();

				Instructions filter = new Instructions(getExportPackage());
				filter.append(getExportContents());

				exports = filter(filter, contained, unused);

				if (!unused.isEmpty()) {
					warning("Unused Export-Package instructions: %s ", unused);
				}

				// See what information we can find to augment the
				// exports. I.e. look on the classpath
				augmentExports(exports);
			}

			//
			// IMPORTS
			// Imports MUST come after exports because we use information from
			// the exports
			//
			{
				// Add all exports that do not have an -noimport: directive
				// to the imports.
				Packages referredAndExported = new Packages(referred);
				referredAndExported.putAll(doExportsToImports(exports));

				removeDynamicImports(referredAndExported);

				// Remove any Java references ... where are the closures???
				for (Iterator<PackageRef> i = referredAndExported.keySet().iterator(); i.hasNext();) {
					if (i.next().isJava())
						i.remove();
				}

				Set<Instruction> unused = Create.set();
				String h = getProperty(IMPORT_PACKAGE);
				if (h == null) // If not set use a default
					h = "*";

				if (isPedantic() && h.trim().length() == 0)
					warning("Empty Import-Package header");

				Instructions filter = new Instructions(h);
				imports = filter(filter, referredAndExported, unused);
				if (!unused.isEmpty()) {
					// We ignore the end wildcard catch
					if (!(unused.size() == 1 && unused.iterator().next().toString().equals("*")))
						warning("Unused Import-Package instructions: %s ", unused);
				}

				// See what information we can find to augment the
				// imports. I.e. look in the exports
				augmentImports(imports, exports);
			}

			//
			// USES
			//
			// Add the uses clause to the exports

			boolean api = isTrue(getProperty(EXPERIMENTS)) || true; // brave,
																	// lets see

			doUses(exports, api ? apiUses : uses, imports);

			//
			// Verify that no exported package has a reference to a private
			// package
			// This can cause a lot of harm.
			// TODO restrict the check to public API only, but even then
			// exported packages
			// should preferably not refer to private packages.
			//
			Set<PackageRef> privatePackages = getPrivates();

			// References to java are not imported so they would show up as
			// private
			// packages, lets kill them as well.

			for (Iterator<PackageRef> p = privatePackages.iterator(); p.hasNext();)
				if (p.next().isJava())
					p.remove();

			for (PackageRef exported : exports.keySet()) {
				List<PackageRef> used = uses.get(exported);
				if (used != null) {
					Set<PackageRef> privateReferences = new HashSet<PackageRef>(apiUses.get(exported));
					privateReferences.retainAll(privatePackages);
					if (!privateReferences.isEmpty())
						msgs.Export_Has_PrivateReferences_(exported, privateReferences.size(), privateReferences);
				}
			}

			//
			// Checks
			//
			if (referred.containsKey(Descriptors.DEFAULT_PACKAGE)) {
				error("The default package '.' is not permitted by the Import-Package syntax. \n"
						+ " This can be caused by compile errors in Eclipse because Eclipse creates \n"
						+ "valid class files regardless of compile errors.\n"
						+ "The following package(s) import from the default package "
						+ uses.transpose().get(Descriptors.DEFAULT_PACKAGE));
			}

		}
	}

	/**
	 * Discussed with BJ and decided to kill the .
	 * 
	 * @param referredAndExported
	 */
	void removeDynamicImports(Packages referredAndExported) {

		// // Remove any matching a dynamic import package instruction
		// Instructions dynamicImports = new
		// Instructions(getDynamicImportPackage());
		// Collection<PackageRef> dynamic = dynamicImports.select(
		// referredAndExported.keySet(), false);
		// referredAndExported.keySet().removeAll(dynamic);
	}

	protected Jar getExtra() throws Exception {
		return null;
	}

	/**
	 * 
	 */
	void doPlugins() {
		for (AnalyzerPlugin plugin : getPlugins(AnalyzerPlugin.class)) {
			try {
				Processor previous = beginHandleErrors(plugin.toString());
				boolean reanalyze = plugin.analyzeJar(this);
				endHandleErrors(previous);
				if (reanalyze) {
					classspace.clear();
					analyzeBundleClasspath();
				}
			}
			catch (Exception e) {
				error("Analyzer Plugin %s failed %s", plugin, e);
			}
		}
	}

	/**
	 * @return
	 */
	boolean isResourceOnly() {
		return isTrue(getProperty(RESOURCEONLY));
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
		try {
			analyze();
			Manifest manifest = new Manifest();
			Attributes main = manifest.getMainAttributes();

			main.put(Attributes.Name.MANIFEST_VERSION, "1.0");
			main.putValue(BUNDLE_MANIFESTVERSION, "2");

			boolean noExtraHeaders = "true".equalsIgnoreCase(getProperty(NOEXTRAHEADERS));

			if (!noExtraHeaders) {
				main.putValue(CREATED_BY, System.getProperty("java.version") + " (" + System.getProperty("java.vendor")
						+ ")");
				main.putValue(TOOL, "Bnd-" + getBndVersion());
				main.putValue(BND_LASTMODIFIED, "" + System.currentTimeMillis());
			}

			String exportHeader = printClauses(exports, true);

			if (exportHeader.length() > 0)
				main.putValue(EXPORT_PACKAGE, exportHeader);
			else
				main.remove(EXPORT_PACKAGE);

			// Remove all the Java packages from the imports
			if (!imports.isEmpty()) {
				main.putValue(IMPORT_PACKAGE, printClauses(imports));
			} else {
				main.remove(IMPORT_PACKAGE);
			}

			Packages temp = new Packages(contained);
			temp.keySet().removeAll(exports.keySet());

			if (!temp.isEmpty())
				main.putValue(PRIVATE_PACKAGE, printClauses(temp));
			else
				main.remove(PRIVATE_PACKAGE);

			Parameters bcp = getBundleClasspath();
			if (bcp.isEmpty() || (bcp.containsKey(".") && bcp.size() == 1))
				main.remove(BUNDLE_CLASSPATH);
			else
				main.putValue(BUNDLE_CLASSPATH, printClauses(bcp));

			doNamesection(dot, manifest);

			for (Enumeration< ? > h = getProperties().propertyNames(); h.hasMoreElements();) {
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

				if (header.equals(BUNDLE_CLASSPATH) || header.equals(EXPORT_PACKAGE) || header.equals(IMPORT_PACKAGE))
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

			// Copy old values into new manifest, when they
			// exist in the old one, but not in the new one
			merge(manifest, dot.getManifest());

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

			// Remove all the headers mentioned in -removeheaders
			Instructions instructions = new Instructions(getProperty(REMOVEHEADERS));
			Collection<Object> result = instructions.select(main.keySet(), false);
			main.keySet().removeAll(result);

			// We should not set the manifest here, this is in general done
			// by the caller.
			// dot.setManifest(manifest);
			return manifest;
		}
		catch (Exception e) {
			// This should not really happen. The code should never throw
			// exceptions in normal situations. So if it happens we need more
			// information. So to help diagnostics. We do a full property dump
			throw new IllegalStateException("Calc manifest failed, state=\n" + getFlattenedProperties(), e);
		}
	}

	/**
	 * Parse the namesection as instructions and then match them against the
	 * current set of resources For example:
	 * 
	 * <pre>
	 * 	-namesection: *;baz=true, abc/def/bar/X.class=3
	 * </pre>
	 * 
	 * The raw value of {@link Constants#NAMESECTION} is used but the values of
	 * the attributes are replaced where @ is set to the resource name. This
	 * allows macro to operate on the resource
	 */

	private void doNamesection(Jar dot, Manifest manifest) {

		Parameters namesection = parseHeader(getProperties().getProperty(NAMESECTION));
		Instructions instructions = new Instructions(namesection);
		Set<String> resources = new HashSet<String>(dot.getResources().keySet());

		//
		// For each instruction, iterator over the resources and filter
		// them. If a resource matches, it must be removed even if the
		// instruction is negative. If positive, add a name section
		// to the manifest for the given resource name. Then add all
		// attributes from the instruction to that name section.
		//
		for (Map.Entry<Instruction,Attrs> instr : instructions.entrySet()) {
			boolean matched = false;

			// For each instruction

			for (Iterator<String> i = resources.iterator(); i.hasNext();) {
				String path = i.next();
				// For each resource

				if (instr.getKey().matches(path)) {

					// Instruction matches the resource

					matched = true;
					if (!instr.getKey().isNegated()) {

						// Positive match, add the attributes

						Attributes attrs = manifest.getAttributes(path);
						if (attrs == null) {
							attrs = new Attributes();
							manifest.getEntries().put(path, attrs);
						}

						//
						// Add all the properties from the instruction to the
						// name section
						//

						for (Map.Entry<String,String> property : instr.getValue().entrySet()) {
							setProperty("@", path);
							try {
								String processed = getReplacer().process(property.getValue());
								attrs.putValue(property.getKey(), processed);
							}
							finally {
								unsetProperty("@");
							}
						}
					}
					i.remove();
				}
			}

			if (!matched && resources.size() > 0)
				warning("The instruction %s in %s did not match any resources", instr.getKey(), NAMESECTION);
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
			warning("Invalid header (starts with @ but does not seem to be for the Name section): %s", header);
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

	public String _bsn(@SuppressWarnings("unused")
	String args[]) {
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
		StringBuilder sb = new StringBuilder();
		Map<String,Map<String,Resource>> map = bundle.getDirectories();
		for (Iterator<String> i = map.keySet().iterator(); i.hasNext();) {
			String directory = i.next();
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

	public Packages getContained() {
		return contained;
	}

	public Packages getExports() {
		return exports;
	}

	public Packages getImports() {
		return imports;
	}

	public Set<PackageRef> getPrivates() {
		HashSet<PackageRef> privates = new HashSet<PackageRef>(contained.keySet());
		privates.removeAll(exports.keySet());
		privates.removeAll(imports.keySet());
		return privates;
	}

	public Jar getJar() {
		return dot;
	}

	public Packages getReferred() {
		return referred;
	}

	/**
	 * Return the set of unreachable code depending on exports and the bundle
	 * activator.
	 * 
	 * @return
	 */
	public Set<PackageRef> getUnreachable() {
		Set<PackageRef> unreachable = new HashSet<PackageRef>(uses.keySet()); // all
		for (Iterator<PackageRef> r = exports.keySet().iterator(); r.hasNext();) {
			PackageRef packageRef = r.next();
			removeTransitive(packageRef, unreachable);
		}
		if (activator != null) {
			removeTransitive(activator.getPackageRef(), unreachable);
		}
		return unreachable;
	}

	public Map<PackageRef,List<PackageRef>> getUses() {
		return uses;
	}

	public Map<PackageRef,List<PackageRef>> getAPIUses() {
		return apiUses;
	}

	public Packages getClasspathExports() {
		return classpathExports;
	}

	/**
	 * Get the version for this bnd
	 * 
	 * @return version or unknown.
	 */
	public String getBndVersion() {
		return getBndInfo("version", "<unknown>");
	}

	static SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd hh:mm:ss z yyyy");
	public long getBndLastModified() {
		String time = getBndInfo("lastmodified", "0");
		if ( time.matches("\\d+"))
			return Long.parseLong(time);
		
		try {
			Date parse = df.parse(time);
			if ( parse != null)
				return parse.getTime();
		} catch( ParseException e) {
			// Ignore
		}
		return 0;
	}

	public String getBndInfo(String key, String defaultValue) {
		if (bndInfo == null) {
			try {
				Properties bndInfoLocal = new Properties();
				URL url = Analyzer.class.getResource("bnd.info");
				if (url != null) {
					InputStream in = url.openStream();
					try {
						bndInfoLocal.load(in);
					}
					finally {
						in.close();
					}
				}
				bndInfo = bndInfoLocal;
			}
			catch (Exception e) {
				e.printStackTrace();
				return defaultValue;
			}
		}
		String value = bndInfo.getProperty(key);
		if (value == null)
			return defaultValue;
		return value;
	}

	/**
	 * Merge the existing manifest with the instructions but do not override
	 * existing properties.
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
					setProperty(key, attributes.getValue(name));
			}
		}
	}

	@Override
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
		if (dot != null)
			removeClose(dot);

		this.dot = jar;
		if (dot != null)
			addClose(dot);

		return jar;
	}

	@Override
	protected void begin() {
		if (inited == false) {
			inited = true;
			super.begin();

			updateModified(getBndLastModified(), "bnd last modified");
			verifyManifestHeadersCase(getProperties());

		}
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
			}
			catch (Exception e) {
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
		}
		catch (IOException ee) {
			// Check if we have files on the classpath
			// that have the right name, allows us to specify those
			// names instead of the full path.
			for (Iterator<Jar> cp = getClasspath().iterator(); cp.hasNext();) {
				Jar entry = cp.next();
				if (entry.getSource() != null && entry.getSource().getName().equals(name)) {
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
	 * @param manifests
	 * @throws Exception
	 */
	private void merge(Manifest result, Manifest old) {
		if (old != null) {
			for (Iterator<Map.Entry<Object,Object>> e = old.getMainAttributes().entrySet().iterator(); e.hasNext();) {
				Map.Entry<Object,Object> entry = e.next();
				Attributes.Name name = (Attributes.Name) entry.getKey();
				String value = (String) entry.getValue();
				if (name.toString().equalsIgnoreCase("Created-By"))
					name = new Attributes.Name("Originally-Created-By");
				if (!result.getMainAttributes().containsKey(name))
					result.getMainAttributes().put(name, value);
			}

			// do not overwrite existing entries
			Map<String,Attributes> oldEntries = old.getEntries();
			Map<String,Attributes> newEntries = result.getEntries();
			for (Iterator<Map.Entry<String,Attributes>> e = oldEntries.entrySet().iterator(); e.hasNext();) {
				Map.Entry<String,Attributes> entry = e.next();
				if (!newEntries.containsKey(entry.getKey())) {
					newEntries.put(entry.getKey(), entry.getValue());
				}
			}
		}
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
	 * manifest. We also remove any version parameter so that augmentImports can
	 * do the version policy. The following method is really tricky and evolved
	 * over time. Coming from the original background of OSGi, it was a weird
	 * idea for me to have a public package that should not be substitutable. I
	 * was so much convinced that this was the right rule that I rücksichtlos
	 * imported them all. Alas, the real world was more subtle than that. It
	 * turns out that it is not a good idea to always import. First, there must
	 * be a need to import, i.e. there must be a contained package that refers
	 * to the exported package for it to make use importing that package.
	 * Second, if an exported package refers to an internal package than it
	 * should not be imported. Additionally, it is necessary to treat the
	 * exports in groups. If an exported package refers to another exported
	 * packages than it must be in the same group. A framework can only
	 * substitute exports for imports for the whole of such a group. WHY?????
	 * Not clear anymore ...
	 */
	Packages doExportsToImports(Packages exports) {

		// private packages = contained - exported.
		Set<PackageRef> privatePackages = new HashSet<PackageRef>(contained.keySet());
		privatePackages.removeAll(exports.keySet());

		// private references = ∀ p : private packages | uses(p)
		Set<PackageRef> privateReferences = newSet();
		for (PackageRef p : privatePackages) {
			Collection<PackageRef> uses = this.uses.get(p);
			if (uses != null)
				privateReferences.addAll(uses);
		}

		// Assume we are going to export all exported packages
		Set<PackageRef> toBeImported = new HashSet<PackageRef>(exports.keySet());

		// Remove packages that are not referenced privately
		toBeImported.retainAll(privateReferences);

		// Not necessary to import anything that is already
		// imported in the Import-Package statement.
		// TODO toBeImported.removeAll(imports.keySet());

		// Remove exported packages that are referring to
		// private packages.
		// Each exported package has a uses clause. We just use
		// the used packages for each exported package to find out
		// if it refers to an internal package.
		//

		for (Iterator<PackageRef> i = toBeImported.iterator(); i.hasNext();) {
			PackageRef next = i.next();
			Collection<PackageRef> usedByExportedPackage = this.uses.get(next);

			// We had an NPE on usedByExportedPackage in GF.
			// I guess this can happen with hard coded
			// imports that do not match reality ...
			if (usedByExportedPackage == null || usedByExportedPackage.isEmpty()) {
				continue;
			}

			for (PackageRef privatePackage : privatePackages) {
				if (usedByExportedPackage.contains(privatePackage)) {
					i.remove();
					break;
				}
			}
		}

		// Clean up attributes and generate result map
		Packages result = new Packages();
		for (Iterator<PackageRef> i = toBeImported.iterator(); i.hasNext();) {
			PackageRef ep = i.next();
			Attrs parameters = exports.get(ep);

			String noimport = parameters == null ? null : parameters.get(NO_IMPORT_DIRECTIVE);
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

			parameters = new Attrs();
			parameters.remove(VERSION_ATTRIBUTE);
			result.put(ep, parameters);
		}
		return result;
	}

	public boolean referred(PackageRef packageName) {
		// return true;
		for (Map.Entry<PackageRef,List<PackageRef>> contained : uses.entrySet()) {
			if (!contained.getKey().equals(packageName)) {
				if (contained.getValue().contains(packageName))
					return true;
			}
		}
		return false;
	}

	/**
	 * @param jar
	 */
	private void getExternalExports(Jar jar, Packages classpathExports) {
		try {
			Manifest m = jar.getManifest();
			if (m != null) {
				Domain domain = Domain.domain(m);
				Parameters exported = domain.getExportPackage();
				for (Entry<String,Attrs> e : exported.entrySet()) {
					PackageRef ref = getPackageRef(e.getKey());
					if (!classpathExports.containsKey(ref)) {
						// TODO e.getValue().put(SOURCE_DIRECTIVE,
						// jar.getBsn()+"-"+jar.getVersion());

						classpathExports.put(ref, e.getValue());
					}
				}
			}
		}
		catch (Exception e) {
			warning("Erroneous Manifest for " + jar + " " + e);
		}
	}

	/**
	 * Find some more information about imports in manifest and other places. It
	 * is assumed that the augmentsExports has already copied external attrs
	 * from the classpathExports.
	 * 
	 * @throws Exception
	 */
	void augmentImports(Packages imports, Packages exports) throws Exception {
		List<PackageRef> noimports = Create.list();
		Set<PackageRef> provided = findProvidedPackages();

		for (PackageRef packageRef : imports.keySet()) {
			String packageName = packageRef.getFQN();

			setProperty(CURRENT_PACKAGE, packageName);
			try {
				Attrs importAttributes = imports.get(packageRef);
				Attrs exportAttributes = exports.get(packageRef, classpathExports.get(packageRef, new Attrs()));

				String exportVersion = exportAttributes.getVersion();
				String importRange = importAttributes.getVersion();

				if (exportVersion == null) {
					// TODO Should check if the source is from a bundle.

				} else {

					//
					// Version Policy - Import version substitution. We
					// calculate the export version and then allow the
					// import version attribute to use it in a substitution
					// by using a ${@} macro. The export version can
					// be defined externally or locally
					//

					boolean provider = isTrue(importAttributes.get(PROVIDE_DIRECTIVE))
							|| isTrue(exportAttributes.get(PROVIDE_DIRECTIVE)) || provided.contains(packageRef);

					exportVersion = cleanupVersion(exportVersion);

					try {
						setProperty("@", exportVersion);

						if (importRange != null) {
							importRange = cleanupVersion(importRange);
							importRange = getReplacer().process(importRange);
						} else
							importRange = getVersionPolicy(provider);

					}
					finally {
						unsetProperty("@");
					}
					importAttributes.put(VERSION_ATTRIBUTE, importRange);
				}

				//
				// Check if exporter has mandatory attributes
				//
				String mandatory = exportAttributes.get(MANDATORY_DIRECTIVE);
				if (mandatory != null) {
					String[] attrs = mandatory.split("\\s*,\\s*");
					for (int i = 0; i < attrs.length; i++) {
						if (!importAttributes.containsKey(attrs[i]))
							importAttributes.put(attrs[i], exportAttributes.get(attrs[i]));
					}
				}

				if (exportAttributes.containsKey(IMPORT_DIRECTIVE))
					importAttributes.put(IMPORT_DIRECTIVE, exportAttributes.get(IMPORT_DIRECTIVE));

				fixupAttributes(importAttributes);
				removeAttributes(importAttributes);

				String result = importAttributes.get(Constants.VERSION_ATTRIBUTE);
				if (result == null)
					noimports.add(packageRef);
			}
			finally {
				unsetProperty(CURRENT_PACKAGE);
			}
		}

		if (isPedantic() && noimports.size() != 0) {
			warning("Imports that lack version ranges: %s", noimports);
		}
	}

	/**
	 * Find the packages we depend on, where we implement an interface that is a
	 * Provider Type. These packages, when we import them, must use the provider
	 * policy.
	 * 
	 * @throws Exception
	 */
	Set<PackageRef> findProvidedPackages() throws Exception {
		Set<PackageRef> providers = Create.set();
		Set<TypeRef> cached = Create.set();

		for (Clazz c : classspace.values()) {
			TypeRef[] interfaces = c.getInterfaces();
			if (interfaces != null)
				for (TypeRef t : interfaces)
					if (cached.contains(t) || isProvider(t)) {
						cached.add(t);
						providers.add(t.getPackageRef());
					}
		}
		return providers;
	}

	private boolean isProvider(TypeRef t) throws Exception {
		Clazz c = findClass(t);
		if (c == null)
			return false;

		if (c.annotations == null)
			return false;

		TypeRef pt = getTypeRefFromFQN(ProviderType.class.getName());
		boolean result = c.annotations.contains(pt);
		return result;
	}

	/**
	 * Provide any macro substitutions and versions for exported packages.
	 */

	void augmentExports(Packages exports) {
		for (PackageRef packageRef : exports.keySet()) {
			String packageName = packageRef.getFQN();
			setProperty(CURRENT_PACKAGE, packageName);
			try {
				Attrs attributes = exports.get(packageRef);
				Attrs exporterAttributes = classpathExports.get(packageRef);
				if (exporterAttributes == null)
					continue;

				for (Map.Entry<String,String> entry : exporterAttributes.entrySet()) {
					String key = entry.getKey();
					if (key.equalsIgnoreCase(SPECIFICATION_VERSION))
						key = VERSION_ATTRIBUTE;

					// dont overwrite and no directives
					if (!key.endsWith(":") && !attributes.containsKey(key)) {
						attributes.put(key, entry.getValue());
					}
				}

				fixupAttributes(attributes);
				removeAttributes(attributes);

			}
			finally {
				unsetProperty(CURRENT_PACKAGE);
			}
		}
	}

	/**
	 * Fixup Attributes Execute any macros on an export and
	 */

	void fixupAttributes(Attrs attributes) {
		// Convert any attribute values that have macros.
		for (String key : attributes.keySet()) {
			String value = attributes.get(key);
			if (value.indexOf('$') >= 0) {
				value = getReplacer().process(value);
				attributes.put(key, value);
			}
		}

	}

	/**
	 * Remove the attributes mentioned in the REMOVE_ATTRIBUTE_DIRECTIVE. You
	 * can add a remove-attribute: directive with a regular expression for
	 * attributes that need to be removed. We also remove all attributes that
	 * have a value of !. This allows you to use macros with ${if} to remove
	 * values.
	 */

	void removeAttributes(Attrs attributes) {
		String remove = attributes.remove(REMOVE_ATTRIBUTE_DIRECTIVE);

		if (remove != null) {
			Instructions removeInstr = new Instructions(remove);
			attributes.keySet().removeAll(removeInstr.select(attributes.keySet(), false));
		}

		// Remove any ! valued attributes
		for (Iterator<Entry<String,String>> i = attributes.entrySet().iterator(); i.hasNext();) {
			String v = i.next().getValue();
			if (v.equals("!"))
				i.remove();
		}
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
		}
		finally {
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
	void doUses(Packages exports, Map<PackageRef,List<PackageRef>> uses, Packages imports) {
		if ("true".equalsIgnoreCase(getProperty(NOUSES)))
			return;

		for (Iterator<PackageRef> i = exports.keySet().iterator(); i.hasNext();) {
			PackageRef packageRef = i.next();
			String packageName = packageRef.getFQN();
			setProperty(CURRENT_PACKAGE, packageName);
			try {
				doUses(packageRef, exports, uses, imports);
			}
			finally {
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
	protected void doUses(PackageRef packageRef, Packages exports, Map<PackageRef,List<PackageRef>> uses,
			Packages imports) {
		Attrs clause = exports.get(packageRef);

		// Check if someone already set the uses: directive
		String override = clause.get(USES_DIRECTIVE);
		if (override == null)
			override = USES_USES;

		// Get the used packages
		Collection<PackageRef> usedPackages = uses.get(packageRef);

		if (usedPackages != null) {

			// Only do a uses on exported or imported packages
			// and uses should also not contain our own package
			// name
			Set<PackageRef> sharedPackages = new HashSet<PackageRef>();
			sharedPackages.addAll(imports.keySet());
			sharedPackages.addAll(exports.keySet());
			sharedPackages.retainAll(usedPackages);
			sharedPackages.remove(packageRef);

			StringBuilder sb = new StringBuilder();
			String del = "";
			for (Iterator<PackageRef> u = sharedPackages.iterator(); u.hasNext();) {
				PackageRef usedPackage = u.next();
				if (!usedPackage.isJava()) {
					sb.append(del);
					sb.append(usedPackage.getFQN());
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
				override = override.replaceAll(USES_USES, Matcher.quoteReplacement(sb.toString())).trim();

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
	void removeTransitive(PackageRef name, Set<PackageRef> unreachable) {
		if (!unreachable.contains(name))
			return;

		unreachable.remove(name);

		List<PackageRef> ref = uses.get(name);
		if (ref != null) {
			for (Iterator<PackageRef> r = ref.iterator(); r.hasNext();) {
				PackageRef element = r.next();
				removeTransitive(element, unreachable);
			}
		}
	}

	/**
	 * Helper method to set the package info resource
	 * 
	 * @param dir
	 * @param key
	 * @param value
	 * @throws Exception
	 */
	void getExportVersionsFromPackageInfo(PackageRef packageRef, Resource r, Packages classpathExports)
			throws Exception {
		if (r == null)
			return;

		Properties p = new Properties();
		try {
			InputStream in = r.openInputStream();
			try {
				p.load(in);
			}
			finally {
				in.close();
			}
			Attrs map = classpathExports.get(packageRef);
			if (map == null) {
				classpathExports.put(packageRef, map = new Attrs());
			}
			for (Enumeration<String> t = (Enumeration<String>) p.propertyNames(); t.hasMoreElements();) {
				String key = t.nextElement();
				String value = map.get(key);
				if (value == null) {
					value = p.getProperty(key);

					// Messy, to allow directives we need to
					// allow the value to start with a ':' since we cannot
					// encode this in a property name

					if (value.startsWith(":")) {
						key = key + ":";
						value = value.substring(1);
					}
					map.put(key, value);
				}
			}
		}
		catch (Exception e) {
			msgs.NoSuchFile_(r);
		}
	}

	@Override
	public void close() {
		if (diagnostics) {
			PrintStream out = System.err;
			out.printf("Current directory            : %s%n", new File("").getAbsolutePath());
			out.println("Classpath used");
			for (Jar jar : getClasspath()) {
				out.printf("File                                : %s%n", jar.getSource());
				out.printf("File abs path                       : %s%n", jar.getSource().getAbsolutePath());
				out.printf("Name                                : %s%n", jar.getName());
				Map<String,Map<String,Resource>> dirs = jar.getDirectories();
				for (Map.Entry<String,Map<String,Resource>> entry : dirs.entrySet()) {
					Map<String,Resource> dir = entry.getValue();
					String name = entry.getKey().replace('/', '.');
					if (dir != null) {
						out.printf("                                      %-30s %d%n", name, dir.size());
					} else {
						out.printf("                                      %-30s <<empty>>%n", name);
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
	 * with the given regular expression ${findpath (; reg-expr (; replacement)?
	 * )? }
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
			warning("Invalid nr of arguments to " + name + " " + Arrays.asList(args) + ", syntax: ${" + name
					+ " (; reg-expr (; replacement)? )? }");
			return null;
		}

		String regexp = ".*";
		String replace = null;

		switch (args.length) {
			case 3 :
				replace = args[2];
				//$FALL-THROUGH$
			case 2 :
				regexp = args[1];
		}
		StringBuilder sb = new StringBuilder();
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

	public void putAll(Map<String,String> additional, boolean force) {
		for (Iterator<Map.Entry<String,String>> i = additional.entrySet().iterator(); i.hasNext();) {
			Map.Entry<String,String> entry = i.next();
			if (force || getProperties().get(entry.getKey()) == null)
				setProperty(entry.getKey(), entry.getValue());
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

	public void addClasspath(Collection< ? > jars) throws IOException {
		for (Object jar : jars) {
			if (jar instanceof Jar)
				addClasspath((Jar) jar);
			else if (jar instanceof File)
				addClasspath((File) jar);
			else if (jar instanceof String)
				addClasspath(getFile((String) jar));
			else
				error("Cannot convert to JAR to add to classpath %s. Not a File, Jar, or String", jar);
		}
	}

	public void addClasspath(File cp) throws IOException {
		if (!cp.exists())
			warning("File on classpath that does not exist: " + cp);
		Jar jar = new Jar(cp);
		addClose(jar);
		classpath.add(jar);
	}

	@Override
	public void clear() {
		classpath.clear();
	}

	public Jar getTarget() {
		return dot;
	}

	private void analyzeBundleClasspath() throws Exception {
		Parameters bcp = getBundleClasspath();

		if (bcp.isEmpty()) {
			analyzeJar(dot, "", true);
		} else {
			boolean okToIncludeDirs = true;

			for (String path : bcp.keySet()) {
				if (dot.getDirectories().containsKey(path)) {
					okToIncludeDirs = false;
					break;
				}
			}

			for (String path : bcp.keySet()) {
				Attrs info = bcp.get(path);

				if (path.equals(".")) {
					analyzeJar(dot, "", okToIncludeDirs);
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
						analyzeJar(jar, "", true);
					}
					catch (Exception e) {
						warning("Invalid bundle classpath entry: " + path + " " + e);
					}
				} else {
					if (dot.getDirectories().containsKey(path)) {
						// if directories are used, we should not have dot as we
						// would have the classes in these directories on the
						// class path twice.
						if (bcp.containsKey("."))
							warning("Bundle-ClassPath uses a directory '%s' as well as '.'. This means bnd does not know if a directory is a package.",
									path, path);
						analyzeJar(dot, Processor.appendPath(path) + "/", true);
					} else {
						if (!"optional".equals(info.get(RESOLUTION_DIRECTIVE)))
							warning("No sub JAR or directory " + path);
					}
				}
			}

		}
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
	private boolean analyzeJar(Jar jar, String prefix, boolean okToIncludeDirs) throws Exception {
		Map<String,Clazz> mismatched = new HashMap<String,Clazz>();

		next: for (String path : jar.getResources().keySet()) {
			if (path.startsWith(prefix)) {

				String relativePath = path.substring(prefix.length());

				if (okToIncludeDirs) {
					int n = relativePath.lastIndexOf('/');
					if (n < 0)
						n = relativePath.length();
					String relativeDir = relativePath.substring(0, n);

					PackageRef packageRef = getPackageRef(relativeDir);
					if (!packageRef.isMetaData() && !contained.containsKey(packageRef)) {
						contained.put(packageRef);

						// For each package we encounter for the first
						// time. Unfortunately we can only do this once
						// we found a class since the bcp has a tendency
						// to overlap
						if (!packageRef.isMetaData()) {
							Resource pinfo = jar.getResource(prefix + packageRef.getPath() + "/packageinfo");
							getExportVersionsFromPackageInfo(packageRef, pinfo, classpathExports);
						}
					}
				}

				// Check class resources, we need to analyze them
				if (path.endsWith(".class")) {
					Resource resource = jar.getResource(path);
					Clazz clazz;
					Attrs info = null;

					try {
						InputStream in = resource.openInputStream();
						clazz = new Clazz(this, path, resource);
						try {
							// Check if we have a package-info
							if (relativePath.endsWith("/package-info.class")) {
								// package-info can contain an Export annotation
								info = new Attrs();
								parsePackageInfoClass(clazz, info);
							} else {
								// Otherwise we just parse it simply
								clazz.parseClassFile();
							}
						}
						finally {
							in.close();
						}
					}
					catch (Throwable e) {
						error("Invalid class file %s (%s)", e, relativePath, e);
						e.printStackTrace();
						continue next;
					}

					String calculatedPath = clazz.getClassName().getPath();
					if (!calculatedPath.equals(relativePath)) {
						// If there is a mismatch we
						// warning
						if (okToIncludeDirs) // assume already reported
							mismatched.put(clazz.getAbsolutePath(), clazz);
					} else {
						classspace.put(clazz.getClassName(), clazz);
						PackageRef packageRef = clazz.getClassName().getPackageRef();

						if (!contained.containsKey(packageRef)) {
							contained.put(packageRef);
							if (!packageRef.isMetaData()) {
								Resource pinfo = jar.getResource(prefix + packageRef.getPath() + "/packageinfo");
								getExportVersionsFromPackageInfo(packageRef, pinfo, classpathExports);
							}
						}
						if (info != null)
							contained.merge(packageRef, false, info);

						// Look at the referred packages
						// and copy them to our baseline
						Set<PackageRef> refs = Create.set();
						for (PackageRef p : clazz.getReferred()) {
							referred.put(p);
							refs.add(p);
						}
						refs.remove(packageRef);
						uses.addAll(packageRef, refs);

						// Collect the API
						apiUses.addAll(packageRef, clazz.getAPIUses());
					}
				}
			}
		}

		if (mismatched.size() > 0) {
			error("Classes found in the wrong directory: %s", mismatched);
			return false;
		}
		return true;
	}

	static Pattern	OBJECT_REFERENCE	= Pattern.compile("L([^/]+/)*([^;]+);");

	private void parsePackageInfoClass(final Clazz clazz, final Attrs info) throws Exception {
		clazz.parseClassFileWithCollector(new ClassDataCollector() {
			@Override
			public void annotation(Annotation a) {
				String name = a.name.getFQN();
				if (aQute.bnd.annotation.Version.class.getName().equals(name)) {

					// Check version
					String version = a.get("value");
					if (!info.containsKey(Constants.VERSION_ATTRIBUTE)) {
						if (version != null) {
							version = getReplacer().process(version);
							if (Verifier.VERSION.matcher(version).matches())
								info.put(VERSION_ATTRIBUTE, version);
							else
								error("Export annotation in %s has invalid version info: %s", clazz, version);
						}
					} else {
						// Verify this matches with packageinfo
						String presentVersion = info.get(VERSION_ATTRIBUTE);
						try {
							Version av = new Version(presentVersion);
							Version bv = new Version(version);
							if (!av.equals(bv)) {
								error("Version from annotation for %s differs with packageinfo or Manifest", clazz
										.getClassName().getFQN());
							}
						}
						catch (Exception e) {
							// Ignore
						}
					}
				} else if (name.equals(Export.class.getName())) {

					// Check mandatory attributes
					Attrs attrs = doAttrbutes((Object[]) a.get(Export.MANDATORY), clazz, getReplacer());
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
	static Pattern	fuzzyVersion		= Pattern.compile("(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9](.*))?",
												Pattern.DOTALL);
	static Pattern	fuzzyVersionRange	= Pattern.compile(
												"(\\(|\\[)\\s*([-\\da-zA-Z.]+)\\s*,\\s*([-\\da-zA-Z.]+)\\s*(\\]|\\))",
												Pattern.DOTALL);
	static Pattern	fuzzyModifier		= Pattern.compile("(\\d+[.-])*(.*)", Pattern.DOTALL);

	static Pattern	nummeric			= Pattern.compile("\\d*");

	static public String cleanupVersion(String version) {
		Matcher m = Verifier.VERSIONRANGE.matcher(version);

		if (m.matches()) {
			try {
				VersionRange vr = new VersionRange(version);
				return version;
			} catch( Exception e) {
				// ignore
			}
		}

		m = fuzzyVersionRange.matcher(version);
		if (m.matches()) {
			String prefix = m.group(1);
			String first = m.group(2);
			String last = m.group(3);
			String suffix = m.group(4);
			return prefix + cleanupVersion(first) + "," + cleanupVersion(last) + suffix;
		}

		m = fuzzyVersion.matcher(version);
		if (m.matches()) {
			StringBuilder result = new StringBuilder();
			String major = removeLeadingZeroes(m.group(1));
			String minor = removeLeadingZeroes(m.group(3));
			String micro = removeLeadingZeroes(m.group(5));
			String qualifier = m.group(7);

			if (qualifier == null) {
				if (!isInteger(minor)) {
					qualifier = minor;
					minor = "0";
				} else if (!isInteger(micro)) {
					qualifier = micro;
					micro = "0";
				}
			}
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
		return version;
	}

	/**
	 * TRhe cleanup version got confused when people used numeric dates like
	 * 201209091230120 as qualifiers. These are too large for Integers. This
	 * method checks if the all digit string fits in an integer.
	 * <pre>
	 * maxint = 2,147,483,647 = 10 digits
	 * </pre>	 
	 * @param integer
	 * @return if this fits in an integer
	 */
	private static boolean isInteger(String minor) {
		return minor.length() < 10 || (minor.length() == 10 && minor.compareTo("2147483647") < 0);
	}

	private static String removeLeadingZeroes(String group) {
		if (group == null)
			return "0";

		int n = 0;
		while (n < group.length() - 1 && group.charAt(n) == '0')
			n++;
		if (n == 0)
			return group;

		return group.substring(n);
	}

	static void cleanupModifier(StringBuilder result, String modifier) {
		Matcher m = fuzzyModifier.matcher(modifier);
		if (m.matches())
			modifier = m.group(2);

		for (int i = 0; i < modifier.length(); i++) {
			char c = modifier.charAt(i);
			if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '-')
				result.append(c);
		}
	}

	final static String	DEFAULT_PROVIDER_POLICY	= "${range;[==,=+)}";
	final static String	DEFAULT_CONSUMER_POLICY	= "${range;[==,+)}";

	public String getVersionPolicy(boolean implemented) {
		if (implemented) {
			return getProperty(PROVIDER_POLICY, DEFAULT_PROVIDER_POLICY);
		}

		return getProperty(CONSUMER_POLICY, DEFAULT_CONSUMER_POLICY);
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
				throw new IllegalArgumentException("${classes} macro must have odd number of arguments. "
						+ _classesHelp);

			String typeName = args[i];
			if (typeName.equalsIgnoreCase("extending"))
				typeName = "extends";
			else if (typeName.equalsIgnoreCase("importing"))
				typeName = "imports";
			else if (typeName.equalsIgnoreCase("annotation"))
				typeName = "annotated";
			else if (typeName.equalsIgnoreCase("implementing"))
				typeName = "implements";

			Clazz.QUERY type = Clazz.QUERY.valueOf(typeName.toUpperCase());

			if (type == null)
				throw new IllegalArgumentException("${classes} has invalid type: " + typeName + ". " + _classesHelp);

			Instruction instr = null;
			if (Clazz.HAS_ARGUMENT.contains(type)) {
				String s = args[++i];
				instr = new Instruction(s);
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
		Macro.verifyCommand(args, "${exporters;<packagename>}, returns the list of jars that export the given package",
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

	public Map<TypeRef,Clazz> getClassspace() {
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
	public Clazz findClass(TypeRef typeRef) throws Exception {
		Clazz c = classspace.get(typeRef);
		if (c != null)
			return c;

		c = importedClassesCache.get(typeRef);
		if (c != null)
			return c;

		Resource r = findResource(typeRef.getPath());
		if (r == null) {
			getClass().getClassLoader();
			URL url = ClassLoader.getSystemResource(typeRef.getPath());
			if (url != null)
				r = new URLResource(url);
		}
		if (r != null) {
			c = new Clazz(this, typeRef.getPath(), r);
			c.parseClassFile();
			importedClassesCache.put(typeRef, c);
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

	public void referTo(TypeRef ref) {
		PackageRef pack = ref.getPackageRef();
		if (!referred.containsKey(pack))
			referred.put(pack, new Attrs());
	}

	public void referToByBinaryName(String binaryClassName) {
		TypeRef ref = descriptors.getTypeRef(binaryClassName);
		referTo(ref);
	}

	/**
	 * Ensure that we are running on the correct bnd.
	 */
	void doRequireBnd() {
		Attrs require = OSGiHeader.parseProperties(getProperty(REQUIRE_BND));
		if (require == null || require.isEmpty())
			return;

		Hashtable<String,String> map = new Hashtable<String,String>();
		map.put(Constants.VERSION_FILTER, getBndVersion());

		for (String filter : require.keySet()) {
			try {
				Filter f = new Filter(filter);
				if (f.match(map))
					continue;
				error("%s fails %s", REQUIRE_BND, require.get(filter));
			}
			catch (Exception t) {
				error("%s with value %s throws exception", t, REQUIRE_BND, require);
			}
		}
	}

	/**
	 * md5 macro
	 */

	static String	_md5Help	= "${md5;path}";

	public String _md5(String args[]) throws Exception {
		Macro.verifyCommand(args, _md5Help, new Pattern[] {
				null, null, Pattern.compile("base64|hex")
		}, 2, 3);

		Digester<MD5> digester = MD5.getDigester();
		Resource r = dot.getResource(args[1]);
		if (r == null)
			throw new FileNotFoundException("From " + digester + ", not found " + args[1]);

		IO.copy(r.openInputStream(), digester);
		boolean hex = args.length > 2 && args[2].equals("hex");
		if (hex)
			return Hex.toHexString(digester.digest().digest());

		return Base64.encodeBase64(digester.digest().digest());
	}

	/**
	 * SHA1 macro
	 */

	static String	_sha1Help	= "${sha1;path}";

	public String _sha1(String args[]) throws Exception {
		Macro.verifyCommand(args, _sha1Help, new Pattern[] {
				null, null, Pattern.compile("base64|hex")
		}, 2, 3);
		Digester<SHA1> digester = SHA1.getDigester();
		Resource r = dot.getResource(args[1]);
		if (r == null)
			throw new FileNotFoundException("From sha1, not found " + args[1]);

		IO.copy(r.openInputStream(), digester);
		return Base64.encodeBase64(digester.digest().digest());
	}

	public Descriptor getDescriptor(String descriptor) {
		return descriptors.getDescriptor(descriptor);
	}

	public TypeRef getTypeRef(String binaryClassName) {
		return descriptors.getTypeRef(binaryClassName);
	}

	public PackageRef getPackageRef(String binaryName) {
		return descriptors.getPackageRef(binaryName);
	}

	public TypeRef getTypeRefFromFQN(String fqn) {
		return descriptors.getTypeRefFromFQN(fqn);
	}

	public TypeRef getTypeRefFromPath(String path) {
		return descriptors.getTypeRefFromPath(path);
	}

	public boolean isImported(PackageRef packageRef) {
		return imports.containsKey(packageRef);
	}

	/**
	 * Merge the attributes of two maps, where the first map can contain
	 * wildcarded names. The idea is that the first map contains instructions
	 * (for example *) with a set of attributes. These patterns are matched
	 * against the found packages in actual. If they match, the result is set
	 * with the merged set of attributes. It is expected that the instructions
	 * are ordered so that the instructor can define which pattern matches
	 * first. Attributes in the instructions override any attributes from the
	 * actual.<br/>
	 * A pattern is a modified regexp so it looks like globbing. The * becomes a
	 * .* just like the ? becomes a .?. '.' are replaced with \\. Additionally,
	 * if the pattern starts with an exclamation mark, it will remove that
	 * matches for that pattern (- the !) from the working set. So the following
	 * patterns should work:
	 * <ul>
	 * <li>com.foo.bar</li>
	 * <li>com.foo.*</li>
	 * <li>com.foo.???</li>
	 * <li>com.*.[^b][^a][^r]</li>
	 * <li>!com.foo.* (throws away any match for com.foo.*)</li>
	 * </ul>
	 * Enough rope to hang the average developer I would say.
	 * 
	 * @param instructions
	 *            the instructions with patterns.
	 * @param source
	 *            the actual found packages, contains no duplicates
	 * @return Only the packages that were filtered by the given instructions
	 */

	Packages filter(Instructions instructions, Packages source, Set<Instruction> nomatch) {
		Packages result = new Packages();
		List<PackageRef> refs = new ArrayList<PackageRef>(source.keySet());
		Collections.sort(refs);

		List<Instruction> filters = new ArrayList<Instruction>(instructions.keySet());
		if (nomatch == null)
			nomatch = Create.set();

		for (Instruction instruction : filters) {
			boolean match = false;

			for (Iterator<PackageRef> i = refs.iterator(); i.hasNext();) {
				PackageRef packageRef = i.next();

				if (packageRef.isMetaData()) {
					i.remove(); // no use checking it again
					continue;
				}

				String packageName = packageRef.getFQN();

				if (instruction.matches(packageName)) {
					match = true;
					if (!instruction.isNegated()) {
						result.merge(packageRef, instruction.isDuplicate(), source.get(packageRef),
								instructions.get(instruction));
					}
					i.remove(); // Can never match again for another pattern
				}
			}
			if (!match && !instruction.isAny())
				nomatch.add(instruction);
		}

		/*
		 * Tricky. If we have umatched instructions they might indicate that we
		 * want to have multiple decorators for the same package. So we check
		 * the unmatched against the result list. If then then match and have
		 * actually interesting properties then we merge them
		 */

		for (Iterator<Instruction> i = nomatch.iterator(); i.hasNext();) {
			Instruction instruction = i.next();

			// We assume the user knows what he is
			// doing and inserted a literal. So
			// we ignore any not matched literals
			// #252, we should not be negated to make it a constant
			if (instruction.isLiteral() && !instruction.isNegated()) {
				result.merge(getPackageRef(instruction.getLiteral()), true, instructions.get(instruction));
				i.remove();
				continue;
			}

			// Not matching a negated instruction looks
			// like an error ... Though so, but
			// in the second phase of Export-Package
			// the !package will never match anymore.
			if (instruction.isNegated()) {
				i.remove();
				continue;
			}

			// An optional instruction should not generate
			// an error
			if (instruction.isOptional()) {
				i.remove();
				continue;
			}

			// boolean matched = false;
			// Set<PackageRef> prefs = new HashSet<PackageRef>(result.keySet());
			// for (PackageRef ref : prefs) {
			// if (instruction.matches(ref.getFQN())) {
			// result.merge(ref, true, source.get(ref),
			// instructions.get(instruction));
			// matched = true;
			// }
			// }
			// if (matched)
			// i.remove();
		}
		return result;
	}

	public void setDiagnostics(boolean b) {
		diagnostics = b;
	}

	public Clazz.JAVA getLowestEE() {
		if (ees.isEmpty())
			return Clazz.JAVA.JDK1_4;

		return ees.first();
	}

	public String _ee(@SuppressWarnings("unused")
	String args[]) {
		return getLowestEE().getEE();
	}

	/**
	 * Calculate the output file for the given target. The strategy is:
	 * 
	 * <pre>
	 * parameter given if not null and not directory
	 * if directory, this will be the output directory
	 * based on bsn-version.jar
	 * name of the source file if exists
	 * Untitled-[n]
	 * </pre>
	 * 
	 * @param output
	 *            may be null, otherwise a file path relative to base
	 */
	public File getOutputFile(String output) {

		if (output == null)
			output = get(Constants.OUTPUT);

		File outputDir;

		if (output != null) {
			File outputFile = getFile(output);
			if (outputFile.isDirectory())
				outputDir = outputFile;
			else
				return outputFile;
		} else
			outputDir = getBase();

		Entry<String,Attrs> name = getBundleSymbolicName();
		if (name != null) {
			String bsn = name.getKey();
			String version = getBundleVersion();
			Version v = Version.parseVersion(version);
			String outputName = bsn + "-" + v.getWithoutQualifier() + Constants.DEFAULT_JAR_EXTENSION;
			return new File(outputDir, outputName);
		}

		File source = getJar().getSource();
		if (source != null) {
			String outputName = source.getName();
			return new File(outputDir, outputName);
		}

		error("Cannot establish an output name from %s, nor bsn, nor source file name, using Untitled", output);
		int n = 0;
		File f = getFile(outputDir, "Untitled");
		while (f.isFile()) {
			f = getFile(outputDir, "Untitled-" + n++);
		}
		return f;
	}

	/**
	 * Utility function to carefully save the file. Will create a backup if the
	 * source file has the same path as the output. It will also only save if
	 * the file was modified or the force flag is true
	 * 
	 * @param output
	 *            the output file, if null {@link #getOutputFile(String)} is
	 *            used.
	 * @param force
	 *            if it needs to be overwritten
	 * @throws Exception
	 */

	public boolean save(File output, boolean force) throws Exception {
		if (output == null)
			output = getOutputFile(null);

		Jar jar = getJar();
		File source = jar.getSource();

		trace("check for modified build=%s file=%s, diff=%s", jar.lastModified(), output.lastModified(),
				jar.lastModified() - output.lastModified());

		if (!output.exists() || output.lastModified() <= jar.lastModified() || force) {
			File op = output.getParentFile();
			if (!op.exists() && !op.mkdirs()) {
				throw new IOException("Could not create directory " + op);
			}
			if (source != null && output.getCanonicalPath().equals(source.getCanonicalPath())) {
				File bak = new File(source.getParentFile(), source.getName() + ".bak");
				if (!source.renameTo(bak)) {
					error("Could not create backup file %s", bak);
				} else
					source.delete();
			}
			try {
				trace("Saving jar to %s", output);
				getJar().write(output);
			}
			catch (Exception e) {
				output.delete();
				error("Cannot write JAR file to %s due to %s", e, output, e.getMessage());
			}
			return true;
		}
		trace("Not modified %s", output);
		return false;

	}

	/**
	 * Set default import and export instructions if none are set
	 */
	public void setDefaults(String bsn, Version version) {
		if (getExportPackage() == null)
			setExportPackage("*");
		if (getImportPackage() == null)
			setExportPackage("*");
		if (bsn != null && getBundleSymbolicName() == null)
			setBundleSymbolicName(bsn);
		if (version != null && getBundleVersion() == null)
			setBundleVersion(version);
	}

	/**
	 * Remove the own references and optional java references from the uses lib
	 * 
	 * @param apiUses
	 * @param removeJava
	 * @return
	 */
	public Map<PackageRef,List<PackageRef>> cleanupUses(Map<PackageRef,List<PackageRef>> apiUses, boolean removeJava) {
		MultiMap<PackageRef,PackageRef> map = new MultiMap<PackageRef,PackageRef>(apiUses);
		for (Entry<PackageRef,List<PackageRef>> e : map.entrySet()) {
			e.getValue().remove(e.getKey());
			if (!removeJava)
				continue;

			for (Iterator<PackageRef> i = e.getValue().iterator(); i.hasNext();) {
				if (i.next().isJava())
					i.remove();
			}
		}
		return map;
	}

	/**
	 * Return the classes for a given source package.
	 * 
	 * @param source
	 *            the source package
	 * @return a set of classes for the requested package.
	 */
	public Set<Clazz> getClassspace(PackageRef source) {
		Set<Clazz> result = new HashSet<Clazz>();
		for (Clazz c : getClassspace().values()) {
			if (c.getClassName().getPackageRef() == source)
				result.add(c);
		}
		return result;
	}

	/**
	 * Create a cross reference from package source, to packages in dest
	 * 
	 * @param source
	 * @param dest
	 * @param sourceModifiers
	 * @return
	 * @throws Exception
	 */
	public Map<Clazz.Def,List<TypeRef>> getXRef(final PackageRef source, final Collection<PackageRef> dest,
			final int sourceModifiers) throws Exception {
		final MultiMap<Clazz.Def,TypeRef> xref = new MultiMap<Clazz.Def,TypeRef>(Clazz.Def.class, TypeRef.class, true);

		for (final Clazz clazz : getClassspace().values()) {
			if ((clazz.accessx & sourceModifiers) == 0)
				continue;

			if (source != null && source != clazz.getClassName().getPackageRef())
				continue;

			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				Clazz.Def	member;

				@Override
				public void extendsClass(TypeRef zuper) throws Exception {
					if (dest.contains(zuper.getPackageRef()))
						xref.add(clazz.getExtends(zuper), zuper);
				}

				@Override
				public void implementsInterfaces(TypeRef[] interfaces) throws Exception {
					for (TypeRef i : interfaces) {
						if (dest.contains(i.getPackageRef()))
							xref.add(clazz.getImplements(i), i);
					}
				}

				@Override
				public void referTo(TypeRef to, int modifiers) {
					if (to.isJava())
						return;

					if (!dest.contains(to.getPackageRef()))
						return;

					if (member != null && ((modifiers & sourceModifiers) != 0)) {
						xref.add(member, to);
					}

				}

				@Override
				public void method(Clazz.MethodDef defined) {
					member = defined;
				}

				@Override
				public void field(Clazz.FieldDef defined) {
					member = defined;
				}

			});

		}
		return xref;
	}

}

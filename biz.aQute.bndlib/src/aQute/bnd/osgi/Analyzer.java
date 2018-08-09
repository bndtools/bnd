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
import static aQute.libg.generics.Create.list;
import static aQute.libg.generics.Create.map;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.osgi.framework.Constants;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.Export;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Clazz.JAVA;
import aQute.bnd.osgi.Descriptors.Descriptor;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.bnd.service.classparser.ClassParser;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.lib.base64.Base64;
import aQute.lib.collections.Iterables;
import aQute.lib.collections.MultiMap;
import aQute.lib.collections.SortedList;
import aQute.lib.filter.Filter;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.cryptography.Digester;
import aQute.libg.cryptography.MD5;
import aQute.libg.cryptography.SHA1;
import aQute.libg.generics.Create;
import aQute.libg.glob.Glob;
import aQute.libg.reporter.ReporterMessages;
import aQute.libg.tuple.Pair;

public class Analyzer extends Processor {
	private final static Logger						logger					= LoggerFactory.getLogger(Analyzer.class);
	private final SortedSet<Clazz.JAVA>				ees						= new TreeSet<>();
	static Properties								bndInfo;

	// Bundle parameters
	private Jar										dot;
	private final Packages							contained				= new Packages();
	private final Packages							referred				= new Packages();
	private Packages								exports;
	private Packages								imports;
	private TypeRef									activator;

	// Global parameters
	private final MultiMap<PackageRef, PackageRef>	uses					= new MultiMap<>(PackageRef.class,
		PackageRef.class, true);
	private final MultiMap<PackageRef, PackageRef>	apiUses					= new MultiMap<>(PackageRef.class,
		PackageRef.class, true);
	private final Contracts							contracts				= new Contracts(this);
	private final Packages							classpathExports		= new Packages();
	private final Descriptors						descriptors				= new Descriptors();
	private final List<Jar>							classpath				= list();
	private final Map<TypeRef, Clazz>				classspace				= map();
	private final Map<TypeRef, Clazz>				importedClassesCache	= map();
	private boolean									analyzed				= false;
	private boolean									diagnostics				= false;
	private boolean									inited					= false;
	final protected AnalyzerMessages				msgs					= ReporterMessages.base(this,
		AnalyzerMessages.class);
	private AnnotationHeaders						annotationHeaders;
	private Set<PackageRef>							packagesVisited			= new HashSet<>();
	private Set<Check>								checks;

	public enum Check {
		ALL,
		IMPORTS,
		EXPORTS;
	}

	public Analyzer(Jar jar) throws Exception {
		super();
		this.dot = Objects.requireNonNull(jar);
		Manifest manifest = dot.getManifest();
		if (manifest != null)
			copyFrom(Domain.domain(manifest));
	}

	public Analyzer(Processor parent) {
		super(parent);
	}

	public Analyzer() {}

	/**
	 * Specifically for Maven
	 */

	public static Properties getManifest(File dirOrJar) throws Exception {
		try (Analyzer analyzer = new Analyzer()) {
			analyzer.setJar(dirOrJar);
			Properties properties = new UTF8Properties();
			properties.put(Constants.IMPORT_PACKAGE, "*");
			properties.put(Constants.EXPORT_PACKAGE, "*");
			analyzer.setProperties(properties);
			Manifest m = analyzer.calcManifest();
			Properties result = new UTF8Properties();
			for (Iterator<Object> i = m.getMainAttributes()
				.keySet()
				.iterator(); i.hasNext();) {
				Attributes.Name name = (Attributes.Name) i.next();
				result.put(name.toString(), m.getMainAttributes()
					.getValue(name));
			}
			return result;
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
			contracts.clear();
			packagesVisited.clear();

			// Parse all the class in the
			// the jar according to the OSGi bcp
			analyzeBundleClasspath();

			//
			// Get exported packages from the
			// entries on the classpath and
			// collect any contracts
			//

			for (Jar current : getClasspath()) {

				getManifestInfoFromClasspath(current, classpathExports, contracts);

				Manifest m = current.getManifest();
				if (m == null)
					for (String dir : current.getDirectories()
						.keySet()) {
						learnPackage(current, "", getPackageRef(dir), classpathExports);
					}
			}

			// Handle the bundle activator

			String s = getProperty(Constants.BUNDLE_ACTIVATOR);
			if (s != null && !s.isEmpty()) {
				activator = getTypeRefFromFQN(s);
				referTo(activator);
				logger.debug("activator {} {}", s, activator);
			}

			// Conditional packages

			doConditionalPackages();

			// Execute any plugins
			// TODO handle better reanalyze
			doPlugins();

			//
			// calculate class versions in use
			//
			for (Clazz c : classspace.values()) {
				ees.add(c.getFormat());
			}

			if (since(About._2_3)) {
				try (ClassDataCollectors cds = new ClassDataCollectors(this)) {
					List<ClassParser> parsers = getPlugins(ClassParser.class);
					for (ClassParser cp : parsers) {
						cds.add(cp.getClassDataCollector(this));
					}

					//
					// built ins
					//

					cds.add(annotationHeaders = new AnnotationHeaders(this));

					for (Clazz c : classspace.values()) {
						cds.parse(c);
					}
				}
			}

			referred.keySet()
				.removeAll(contained.keySet());

			//
			// EXPORTS
			//
			{
				Set<Instruction> unused = Create.set();

				Instructions filter = new Instructions(getExportPackage());
				filter.appendIfAbsent(getExportContents());

				//
				// get the packages exported by the annotation. However,
				// we remove any explicitly explicitly named private package so
				// that
				// you can always override an annotation
				//

				Parameters exportedByAnnotation = getExportedByAnnotation();
				exportedByAnnotation.keySet()
					.removeAll(getPrivatePackage().keySet());

				filter.appendIfAbsent(exportedByAnnotation);

				exports = filter(filter, contained, unused);

				if (!unused.isEmpty()) {
					warning("Unused " + Constants.EXPORT_PACKAGE + " instructions: %s ", unused)
						.header(Constants.EXPORT_PACKAGE)
						.context(unused.iterator()
							.next()
							.getInput());
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
				referredAndExported.keySet()
					.removeIf(PackageRef::isJava);

				Set<Instruction> unused = Create.set();
				String h = getProperty(Constants.IMPORT_PACKAGE);
				if (h == null) // If not set use a default
					h = "*";

				if (isPedantic() && h.trim()
					.length() == 0)
					warning("Empty " + Constants.IMPORT_PACKAGE + " header");

				Instructions filter = new Instructions(h);
				imports = filter(filter, referredAndExported, unused);
				if (!unused.isEmpty()) {
					// We ignore the end wildcard catch
					if (!(unused.size() == 1 && unused.iterator()
						.next()
						.toString()
						.equals("*")))
						warning("Unused " + Constants.IMPORT_PACKAGE + " instructions: %s ", unused)
							.header(Constants.IMPORT_PACKAGE)
							.context(unused.iterator()
								.next()
								.getInput());
				}

				// See what information we can find to augment the
				// imports. I.e. look in the exports
				augmentImports(imports, exports);
			}

			//
			// USES
			//
			// Add the uses clause to the exports

			boolean api = true; // brave,
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
				if (p.next()
					.isJava())
					p.remove();

			for (PackageRef exported : exports.keySet()) {
				List<PackageRef> used = uses.get(exported);
				if (used != null) {
					Set<PackageRef> privateReferences = new TreeSet<>(apiUses.get(exported));
					privateReferences.retainAll(privatePackages);
					if (!privateReferences.isEmpty())
						msgs.Export_Has_PrivateReferences_(exported, privateReferences.size(), privateReferences);
				}
			}

			//
			// Checks
			//
			if (referred.containsKey(Descriptors.DEFAULT_PACKAGE)) {
				error(
					"The default package '.' is not permitted by the " + Constants.IMPORT_PACKAGE + " syntax.%n"
						+ " This can be caused by compile errors in Eclipse because Eclipse creates%n"
						+ "valid class files regardless of compile errors.%n"
						+ "The following package(s) import from the default package %s",
					uses.transpose()
						.get(Descriptors.DEFAULT_PACKAGE));
			}

			// Check for use of the deprecated bnd @Export annotation

			TypeRef bndAnnotation = descriptors.getTypeRefFromFQN(aQute.bnd.annotation.Export.class.getName());
			contained.keySet()
				.stream()
				.map(this::getPackageInfoClazz)
				.filter(Objects::nonNull)
				.distinct()
				.filter(clz -> clz.annotations()
					.contains(bndAnnotation))
				.map(Clazz::getClassName)
				.map(TypeRef::getPackageRef)
				.map(PackageRef::getFQN)
				.forEach(fqn -> warning(
					"The annotation aQute.bnd.annotation.Export applied to package %s is deprecated and will be removed in a future release. The org.osgi.annotation.bundle.Export should be used instead",
					fqn));
		}
	}

	private Parameters getExportedByAnnotation() {
		TypeRef exportAnnotation = descriptors.getTypeRefFromFQN("org.osgi.annotation.bundle.Export");
		return contained.keySet()
			.stream()
			.map(this::getPackageInfoClazz)
			.filter(Objects::nonNull)
			.distinct()
			.filter(clz -> clz.annotations()
				.contains(exportAnnotation))
			.map(Clazz::getClassName)
			.map(TypeRef::getPackageRef)
			.map(PackageRef::getFQN)
			.collect(Parameters.toParameters());
	}

	private Clazz getPackageInfoClazz(PackageRef pr) {
		String bin = pr.getBinary() + "/package-info";
		TypeRef tr = descriptors.getTypeRef(bin);
		try {
			return findClass(tr);
		} catch (Exception e) {
			return null;
		}
	}

	private void doConditionalPackages() throws Exception {
		//
		// We need to find out the contained packages
		// again ... so we need to clear any visited
		// packages otherwise new packages are not
		// added to contained
		//
		packagesVisited.clear();

		for (Jar extra = getExtra(); extra != null; extra = getExtra()) {
			dot.addAll(extra);
			analyzeJar(extra, "", true);
		}
	}

	/*
	 * Learn the package details from the Jar. This can either be the manifest
	 * (in that case the attrs are already set on the package), a
	 * package-info.java file, or a packageinfo file (in the given priority).
	 * This method will only learn a package once, so this method an be called
	 * for every class. It will also ignore any metadata or java packages.
	 */
	private void learnPackage(Jar jar, String prefix, PackageRef packageRef, Packages map) throws Exception {
		if (packageRef.isMetaData() || packageRef.isJava() || packageRef.isPrimitivePackage())
			return;

		//
		// We should ignore empty directories/packages. Empty packages should
		// not take the package slot. See #708
		//

		Map<String, Resource> dir = jar.getDirectories()
			.get(prefix + packageRef.getBinary());
		if (dir == null || dir.size() == 0)
			return;

		//
		// Make sure we only do this once for each package
		// in cp order (which is the calling order)
		//

		if (packagesVisited.contains(packageRef))
			return;
		packagesVisited.add(packageRef);

		//
		// The manifest has priority over the packageinfo or package-info.java
		//

		Attrs attrs = map.get(packageRef);
		if (attrs != null && attrs.size() > 1)
			return;

		//
		// The previous bnd version forgot to parse
		// the java-info file on the external classpath
		// so in backward compatibility mode we should skip
		// it.
		//
		if (map != classpathExports || since(About._2_3)) {
			Resource resource = jar.getResource(prefix + packageRef.getBinary() + "/package-info.class");
			if (resource != null) {
				Attrs info = parsePackageInfoClass(resource);
				if (info != null && info.containsKey(VERSION_ATTRIBUTE)) {
					info.put(FROM_DIRECTIVE, resource.toString());
					info.put(INTERNAL_SOURCE_DIRECTIVE, getName(jar));
					map.put(packageRef, info);
					return;
				}
			}
		}

		String path = prefix + packageRef.getBinary() + "/packageinfo";
		Resource resource = jar.getResource(path);
		if (resource != null) {
			Attrs info = parsePackageinfo(packageRef, resource);
			if (info != null) {
				info.put(FROM_DIRECTIVE, resource.toString());
				info.put(INTERNAL_SOURCE_DIRECTIVE, getName(jar));
				fixupOldStyleVersions(info);
				map.put(packageRef, info);
				return;
			}
		}

		//
		// We need to set an attribute because this is how the set
		// of available packages is remembered
		//
		map.put(packageRef)
			.put(INTERNAL_SOURCE_DIRECTIVE, getName(jar));

		// trace("%s from %s has no package info (either manifest, packageinfo
		// or package-info.class",
		// packageRef, jar);
	}

	protected String getName(Jar jar) throws Exception {
		String name = jar.getBsn();
		if (name == null) {
			name = jar.getName();
			if (name.equals("dot") && jar.getSource() != null)
				name = jar.getSource()
					.getName();
		}
		String version = jar.getVersion();
		if (version == null)
			version = "0.0.0";

		return name + "-" + version;
	}

	/*
	 * Helper method to set the package info resource
	 */
	static Pattern OLD_PACKAGEINFO_SYNTAX_P = Pattern
		.compile("class\\s+(.+)\\s+version\\s+(" + Verifier.VERSION_S + ")");

	Attrs parsePackageinfo(PackageRef packageRef, Resource r) throws Exception {

		Properties p = new UTF8Properties();
		try {
			try (InputStream in = r.openInputStream()) {
				p.load(in);
			}

			Attrs attrs = new Attrs();

			for (String key : Iterables.iterable(p.propertyNames(), String.class::cast)) {
				String propvalue = p.getProperty(key);

				if (key.equalsIgnoreCase("include")) {

					// Ouch, could be really old syntax
					// We ignore the include part upto we
					// ignored it long before.

					Matcher m = OLD_PACKAGEINFO_SYNTAX_P.matcher(propvalue);
					if (m.matches()) {
						key = Constants.VERSION_ATTRIBUTE;
						propvalue = m.group(2);
						if (isPedantic())
							warning("found old syntax in package info in package %s, from resource %s", packageRef, r);
					}
				}

				String value = propvalue;

				// Messy, to allow directives we need to
				// allow the value to start with a ':' upto we cannot
				// encode this in a property name

				if (value.startsWith(":")) {
					key = key + ":";
					value = value.substring(1);
				}
				attrs.put(key, value);
			}
			return attrs;
		} catch (Exception e) {
			msgs.NoSuchFile_(r);
		}
		return null;
	}

	/*
	 * Parse the package-info.java class
	 */
	static Pattern OBJECT_REFERENCE = Pattern.compile("([^\\.]+\\.)*([^\\.]+)");

	private Attrs parsePackageInfoClass(Resource r) throws Exception {
		final Attrs info = new Attrs();
		final Clazz clazz = new Clazz(this, "", r);

		clazz.parseClassFileWithCollector(new ClassDataCollector() {
			@Override
			public void annotation(Annotation a) {
				String name = a.getName()
					.getFQN();
				switch (name) {
					case "org.osgi.annotation.versioning.Version" :
						// Check version
						String version = a.get("value");
						if (!info.containsKey(Constants.VERSION_ATTRIBUTE)) {
							if (version != null) {
								version = getReplacer().process(version);
								if (Verifier.VERSION.matcher(version)
									.matches())
									info.put(VERSION_ATTRIBUTE, version);
								else
									error("Version annotation in %s has invalid version info: %s", clazz, version);
							}
						} else {
							// Verify this matches with packageinfo
							String presentVersion = info.get(VERSION_ATTRIBUTE);
							try {
								Version av = new Version(presentVersion).getWithoutQualifier();
								Version bv = new Version(version).getWithoutQualifier();
								if (!av.equals(bv)) {
									error("Version from annotation for %s differs with packageinfo or Manifest",
										clazz.getClassName()
											.getFQN());
								}
							} catch (Exception e) {
								// Ignore
							}
						}
						break;
					case "org.osgi.annotation.versioning.ProviderType" :
						if (!info.containsKey(PROVIDE_DIRECTIVE)) {
							// let Export.substitution override ProviderType
							info.put(PROVIDE_DIRECTIVE, "true");
						}
						break;
					case "org.osgi.annotation.bundle.Export" :
						Object[] usesClauses = a.get("uses");
						if (usesClauses != null) {
							StringJoiner sj = new StringJoiner(",");
							String old = info.get(USES_DIRECTIVE);
							if (old != null)
								sj.add(old);
							for (Object usesClause : usesClauses) {
								sj.add(usesClause.toString());
							}
							info.put(USES_DIRECTIVE, sj.toString());
						}

						Object substitution = a.get("substitution");
						if (substitution != null) {
							switch (substitution.toString()) {
								case "CONSUMER" :
									info.put(PROVIDE_DIRECTIVE, "false");
									break;
								case "PROVIDER" :
									info.put(PROVIDE_DIRECTIVE, "true");
									break;
								case "NOIMPORT" :
									info.put(NO_IMPORT_DIRECTIVE, "true");
									break;
								case "CALCULATED" :
									// nothing to do; this is the normal case
									break;
								default :
									error("Export annotation in %s has invalid substitution value: %s", clazz,
										substitution);
									break;
							}
						}

						Object[] attributes = a.get("attribute");
						if (attributes != null) {
							for (Object attribute : attributes) {
								info.mergeWith(OSGiHeader.parseProperties(attribute.toString(), Analyzer.this), false);
							}
						}
						break;
					case "aQute.bnd.annotation.Export" :
						// Check mandatory attributes
						Attrs attrs = doAttrbutes(a.get(Export.MANDATORY), clazz, getReplacer());
						if (!attrs.isEmpty()) {
							info.putAll(attrs);
							info.put(MANDATORY_DIRECTIVE, Processor.join(attrs.keySet()));
						}

						// Check optional attributes
						attrs = doAttrbutes(a.get(Export.OPTIONAL), clazz, getReplacer());
						if (!attrs.isEmpty()) {
							info.putAll(attrs);
						}

						// Check Included classes
						Object[] included = a.get(Export.INCLUDE);
						if (included != null && included.length > 0) {
							StringBuilder sb = new StringBuilder();
							String del = "";
							for (Object i : included) {
								Matcher m = OBJECT_REFERENCE.matcher(((TypeRef) i).getFQN());
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
								Matcher m = OBJECT_REFERENCE.matcher(((TypeRef) i).getFQN());
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
						break;
				}
			}
		});
		return info;
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
				boolean reanalyze;
				Processor previous = beginHandleErrors(plugin.toString());
				try {
					reanalyze = plugin.analyzeJar(this);
				} finally {
					endHandleErrors(previous);
				}
				if (reanalyze) {
					classspace.clear();
					analyzeBundleClasspath();
				}
			} catch (Exception e) {
				exception(e, "Analyzer Plugin %s failed %s", plugin, e);
			}
		}
	}

	/**
	 * @return {@code true} if the {@code -resourceonly} instruction is set,
	 *         {@code false} otherwise
	 */
	boolean isResourceOnly() {
		return isTrue(getProperty(RESOURCEONLY));
	}

	/**
	 * One of the main workhorses of this class. This will analyze the current
	 * setup and calculate a new manifest according to this setup.
	 * 
	 * @throws IOException
	 */
	public Manifest calcManifest() throws Exception {
		try {
			analyze();
			Manifest manifest = new Manifest();
			Attributes main = manifest.getMainAttributes();

			main.put(Attributes.Name.MANIFEST_VERSION, "1.0");
			main.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");

			boolean noExtraHeaders = "true".equalsIgnoreCase(getProperty(NOEXTRAHEADERS));

			if (!noExtraHeaders) {
				main.putValue(CREATED_BY,
					System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
				main.putValue(TOOL, "Bnd-" + getBndVersion());
				if (!dot.isReproducible()) {
					main.putValue(BND_LASTMODIFIED, Long.toString(System.currentTimeMillis()));
				}
			}

			String exportHeader = printClauses(exports, true);

			if (exportHeader.length() > 0)
				main.putValue(Constants.EXPORT_PACKAGE, exportHeader);
			else
				main.remove(new Name(Constants.EXPORT_PACKAGE));

			// Divide imports with resolution:=dynamic to DynamicImport-Package
			// and add them to the existing DynamicImport-Package instruction
			Pair<Packages, Parameters> regularAndDynamicImports = divideRegularAndDynamicImports();
			Packages regularImports = regularAndDynamicImports.getFirst();

			if (!regularImports.isEmpty()) {
				main.putValue(Constants.IMPORT_PACKAGE, printClauses(regularImports));
			} else {
				main.remove(new Name(Constants.IMPORT_PACKAGE));
			}

			Parameters dynamicImports = regularAndDynamicImports.getSecond();
			if (!dynamicImports.isEmpty()) {
				main.putValue(Constants.DYNAMICIMPORT_PACKAGE, printClauses(dynamicImports));
			} else {
				main.remove(new Name(Constants.DYNAMICIMPORT_PACKAGE));
			}

			Packages temp = new Packages(contained);
			temp.keySet()
				.removeAll(exports.keySet());

			//
			// This actually can contain file names if the look
			// like packages. So remove anything that maps
			// to a resource in the JAR
			//

			for (Iterator<PackageRef> i = temp.keySet()
				.iterator(); i.hasNext();) {
				String binary = i.next()
					.getBinary();
				Resource r = dot.getResource(binary);
				if (r != null)
					i.remove();
			}

			if (!temp.isEmpty())
				main.putValue(PRIVATE_PACKAGE, printClauses(temp));
			else
				main.remove(new Name(PRIVATE_PACKAGE));

			Parameters bcp = getBundleClasspath();
			if (bcp.isEmpty() || (bcp.containsKey(".") && bcp.size() == 1))
				main.remove(new Name(Constants.BUNDLE_CLASSPATH));
			else
				main.putValue(Constants.BUNDLE_CLASSPATH, printClauses(bcp));

			// ----- Require/Capabilities section

			Parameters requirements = new Parameters(annotationHeaders.getHeader(Constants.REQUIRE_CAPABILITY), this);
			Parameters capabilities = new Parameters(annotationHeaders.getHeader(Constants.PROVIDE_CAPABILITY), this);

			//
			// Do any contracts contracts
			//
			contracts.addToRequirements(requirements);

			//
			// We want to add the minimum EE as a requirement
			// based on the class version
			//

			if (!isTrue(getProperty(NOEE)) //
				&& !ees.isEmpty() // no use otherwise
				&& since(About._2_3) // we want people to not have to
										// automatically add it
				&& !requirements.containsKey(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE) // and
																											// it
																											// should
																											// not
																											// be
																											// there
																											// already
			) {

				JAVA highest = ees.last();
				Attrs attrs = new Attrs();

				String filter = doEEProfiles(highest);

				attrs.put(Constants.FILTER_DIRECTIVE, filter);

				//
				// Java 1.8 introduced profiles.
				// If -eeprofile= auto | (<profile>="...")+ is set then
				// we add a

				requirements.add(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, attrs);
			}

			if (!requirements.isEmpty())
				main.putValue(Constants.REQUIRE_CAPABILITY, requirements.toString());

			if (!capabilities.isEmpty())
				main.putValue(Constants.PROVIDE_CAPABILITY, capabilities.toString());

			// -----

			doNamesection(dot, manifest);
			for (String header : Iterables.iterable(getProperties().propertyNames(), String.class::cast)) {
				if (header.trim()
					.length() == 0) {
					warning("Empty property set with value: %s", getProperties().getProperty(header));
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

				if (header.equals(Constants.BUNDLE_CLASSPATH) || header.equals(Constants.EXPORT_PACKAGE)
					|| header.equals(Constants.IMPORT_PACKAGE) || header.equals(Constants.DYNAMICIMPORT_PACKAGE)
					|| header.equals(Constants.REQUIRE_CAPABILITY) || header.equals(Constants.PROVIDE_CAPABILITY))
					continue;

				if (header.equalsIgnoreCase("Name")) {
					error(
						"Your bnd file contains a header called 'Name'. This interferes with the manifest name section.");
					continue;
				}

				if (Verifier.HEADER_PATTERN.matcher(header)
					.matches()) {
					doHeader(main, header);
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
			if (main.getValue(Constants.BUNDLE_SYMBOLICNAME) == null) {
				main.putValue(Constants.BUNDLE_SYMBOLICNAME, bsn);
			}

			//
			// Use the same name for the bundle name as BSN when
			// the bundle name is not set
			//
			if (main.getValue(Constants.BUNDLE_NAME) == null) {
				main.putValue(Constants.BUNDLE_NAME, bsn);
			}

			if (main.getValue(Constants.BUNDLE_VERSION) == null)
				main.putValue(Constants.BUNDLE_VERSION, "0");

			// Remove all the headers mentioned in -removeheaders
			Instructions instructions = new Instructions(mergeProperties(REMOVEHEADERS));
			Collection<Object> result = instructions.select(main.keySet(), false);
			main.keySet()
				.removeAll(result);

			// We should not set the manifest here, this is in general done
			// by the caller.
			// dot.setManifest(manifest);
			return manifest;
		} catch (Exception e) {
			// This should not really happen. The code should never throw
			// exceptions in normal situations. So if it happens we need more
			// information. So to help diagnostics. We do a full property dump
			throw new IllegalStateException("Calc manifest failed, state=\n" + getFlattenedProperties(), e);
		}
	}

	/**
	 * Added for 1.8 profiles. A 1.8 profile is a set of packages so the VM can
	 * be delivered in smaller versions. This method will look at the
	 * {@link Constants#EEPROFILE} option. If it is set, it can be "auto" or it
	 * can contain a list of profiles specified as name="a,b,c" values. If we
	 * find a package outside the profiles, no profile is set. Otherwise the
	 * highest found profile is added. This only works for java packages.
	 */
	private String doEEProfiles(JAVA highest) throws IOException {
		String ee = getProperty(EEPROFILE);
		if (ee == null)
			return highest.getFilter();

		ee = ee.trim();

		Map<String, Set<String>> profiles;

		if (ee.equals(EEPROFILE_AUTO_ATTRIBUTE)) {
			profiles = highest.getProfiles();
			if (profiles == null)
				return highest.getFilter();
		} else {
			Attrs t = OSGiHeader.parseProperties(ee);
			profiles = new HashMap<>();

			for (Map.Entry<String, String> e : t.entrySet()) {
				String profile = e.getKey();
				String l = e.getValue();
				SortedList<String> sl = new SortedList<>(l.split("\\s*,\\s*"));
				profiles.put(profile, sl);
			}
		}
		SortedSet<String> found = new TreeSet<>();
		nextPackage: for (PackageRef p : referred.keySet()) {
			if (p.isJava()) {
				String fqn = p.getFQN();
				for (Entry<String, Set<String>> entry : profiles.entrySet()) {
					if (entry.getValue()
						.contains(fqn)) {

						found.add(entry.getKey());

						//
						// Check if we found all the possible profiles
						// that means we're finished
						//

						if (found.size() == profiles.size())
							break nextPackage;

						//
						// Profiles should be exclusive
						// so we can break if we found one
						//
						continue nextPackage;
					}
				}

				//
				// Ouch, outside any profile
				//
				return highest.getFilter();
			}
		}

		String filter = highest.getFilter();
		if (!found.isEmpty())
			filter = filter.replaceAll("JavaSE", "JavaSE/" + found.last());
		// TODO a more elegant way to build the filter, we now assume JavaSE
		return filter;

	}

	private void doHeader(Attributes main, String header) {
		String value = annotationHeaders.getHeader(header);
		if (value == null)
			return;
		Name name = new Name(header);
		if (main.getValue(name) != null)
			return;
		String trimmed = value.trim();
		if (trimmed.isEmpty())
			main.remove(name);
		else if (EMPTY_HEADER.equals(trimmed))
			main.put(name, "");
		else
			main.put(name, value);
	}

	/**
	 * Parse the namesection as instructions and then match them against the
	 * current set of resources For example:
	 * 
	 * <pre>
	 *  -namesection: *;baz=true,
	 * abc/def/bar/X.class=3
	 * </pre>
	 * 
	 * The raw value of {@link Constants#NAMESECTION} is used but the values of
	 * the attributes are replaced where @ is set to the resource name. This
	 * allows macro to operate on the resource
	 */

	private void doNamesection(Jar dot, Manifest manifest) {

		Parameters namesection = parseHeader(getProperties().getProperty(NAMESECTION));
		Instructions instructions = new Instructions(namesection);
		Set<String> resources = new HashSet<>(dot.getResources()
			.keySet());

		//
		// For each instruction, iterator over the resources and filter
		// them. If a resource matches, it must be removed even if the
		// instruction is negative. If positive, add a name section
		// to the manifest for the given resource name. Then add all
		// attributes from the instruction to that name section.
		//
		for (Map.Entry<Instruction, Attrs> instr : instructions.entrySet()) {
			boolean matched = false;

			// For each instruction

			for (Iterator<String> i = resources.iterator(); i.hasNext();) {
				String path = i.next();
				// For each resource

				if (instr.getKey()
					.matches(path)) {

					// Instruction matches the resource

					matched = true;
					if (!instr.getKey()
						.isNegated()) {

						// Positive match, add the attributes

						Attributes attrs = manifest.getAttributes(path);
						if (attrs == null) {
							attrs = new Attributes();
							manifest.getEntries()
								.put(path, attrs);
						}

						//
						// Add all the properties from the instruction to the
						// name section
						//

						for (Map.Entry<String, String> property : instr.getValue()
							.entrySet()) {
							setProperty("@", path);
							try {
								String processed = getReplacer().process(property.getValue());
								attrs.putValue(property.getKey(), processed);
							} finally {
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
	 *  &#064;org@osgi@service@event@Implementation-Title
	 * </pre>
	 * 
	 * This will be the header Implementation-Title in the
	 * org/osgi/service/event named section.
	 * 
	 * @param manifest
	 * @param header
	 */
	void doNameSection(Manifest manifest, String header) {
		String path = header.replace('@', '/');
		int n = path.lastIndexOf('/');
		// Must succeed because we start with @
		String name = path.substring(n + 1);
		// Skip first /
		path = path.substring(1, n == 0 ? 1 : n);
		if (name.length() != 0 && path.length() != 0) {
			Attributes attrs = manifest.getAttributes(path);
			if (attrs == null) {
				attrs = new Attributes();
				manifest.getEntries()
					.put(path, attrs);
			}
			attrs.putValue(name, getProperty(header));
		} else {
			warning("Invalid header (starts with @ but does not seem to be for the Name section): %s", header);
		}
	}

	/**
	 * Clear the key part of a header. I.e. remove everything from the first ';'
	 */
	public String getBsn() {
		String value = getProperty(Constants.BUNDLE_SYMBOLICNAME);
		if (value == null) {
			if (getPropertiesFile() != null)
				value = getPropertiesFile().getName();

			String projectName = getBase().getName();
			if (value == null || value.equals("bnd.bnd")) {
				value = projectName;
			} else if (value.endsWith(".bnd")) {
				value = value.substring(0, value.length() - 4);

				//
				// This basically unknown feature allowed you to
				// define a sub-bundle that specified a name that used the
				// project name as a prefix, the project prefix would then
				// be skipped. This caused several problems in practice
				// and we actually did not take this into account in other
				// places.
				//

				// if (!value.startsWith(getBase().getName()))
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

	public String _bsn(@SuppressWarnings("unused") String args[]) {
		return getBsn();
	}

	/**
	 * Calculate an export header solely based on the contents of a JAR file
	 * 
	 * @param bundle The jar file to analyze
	 */
	public String calculateExportsFromContents(Jar bundle) {
		String ddel = "";
		StringBuilder sb = new StringBuilder();
		Map<String, Map<String, Resource>> map = bundle.getDirectories();
		for (Iterator<String> i = map.keySet()
			.iterator(); i.hasNext();) {
			String directory = i.next();
			if (directory.equals("META-INF") || directory.startsWith("META-INF/"))
				continue;
			if (directory.equals("OSGI-OPT") || directory.startsWith("OSGI-OPT/"))
				continue;
			if (directory.equals("/"))
				continue;
			Map<String, Resource> resources = map.get(directory);
			if (resources == null || resources.isEmpty())
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
		HashSet<PackageRef> privates = new HashSet<>(contained.keySet());
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
	 */
	public Set<PackageRef> getUnreachable() {
		Set<PackageRef> unreachable = new HashSet<>(uses.keySet()); // all
		for (Iterator<PackageRef> r = exports.keySet()
			.iterator(); r.hasNext();) {
			PackageRef packageRef = r.next();
			removeTransitive(packageRef, unreachable);
		}
		if (activator != null) {
			removeTransitive(activator.getPackageRef(), unreachable);
		}
		return unreachable;
	}

	public Map<PackageRef, List<PackageRef>> getUses() {
		return uses;
	}

	public Map<PackageRef, List<PackageRef>> getAPIUses() {
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

	static SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);

	static {
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public long getBndLastModified() {
		String time = getBndInfo("lastmodified", "0");
		if (time.matches("\\d+"))
			return Long.parseLong(time);

		try {
			synchronized (df) {
				Date parse = df.parse(time);
				if (parse != null)
					return parse.getTime();
			}
		} catch (ParseException e) {
			// Ignore
		}
		return 0;
	}

	public String getBndInfo(String key, String defaultValue) {
		if (bndInfo == null) {
			try {
				Properties bndInfoLocal = new UTF8Properties();
				URL url = Analyzer.class.getResource("bnd.info");
				if (url != null) {
					try (InputStream in = url.openStream()) {
						bndInfoLocal.load(in);
					}
				}

				String v = bndInfoLocal.getProperty("version");
				if (!Version.isVersion(v)) {
					bndInfoLocal.put("version", About.CURRENT.toString());
				}
				bndInfo = bndInfoLocal;
			} catch (Exception e) {
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
	 * @param manifest The manifest to merge with
	 * @throws IOException
	 */
	public void mergeManifest(Manifest manifest) throws IOException {
		if (manifest != null) {
			Attributes attributes = manifest.getMainAttributes();
			for (Iterator<Object> i = attributes.keySet()
				.iterator(); i.hasNext();) {
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
		getProperties().put("project.dir", IO.absolutePath(getBase()));
	}

	/**
	 * Set the classpath for this analyzer by file.
	 * 
	 * @param classpath
	 * @throws IOException
	 */
	public void setClasspath(Collection<?> classpath) throws IOException {
		for (Object cpe : classpath) {
			if (cpe instanceof Jar) {
				addClasspath((Jar) cpe);
			} else if (cpe instanceof File) {
				File f = (File) cpe;
				if (!f.exists()) {
					error("Missing file on classpath: %s", IO.absolutePath(f));
					continue;
				}
				addClasspath(f);
			} else if (cpe instanceof String) {
				Jar j = getJarFromName((String) cpe, " setting classpath");
				if (j == null) {
					continue;
				}
				addClasspath(j);
			} else {
				error("Cannot convert to JAR to add to classpath %s. Not a File, Jar, or String", cpe);
			}
		}
	}

	public void setClasspath(File[] classpath) throws IOException {
		List<Jar> list = new ArrayList<>();
		for (int i = 0; i < classpath.length; i++) {
			if (classpath[i].exists()) {
				Jar current = new Jar(classpath[i]);
				list.add(current);
			} else {
				error("Missing file on classpath: %s", IO.absolutePath(classpath[i]));
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
	 * @param file
	 * @throws IOException
	 */
	public Jar setJar(File file) throws IOException {
		Jar jar = new Jar(file);
		setJar(jar);
		return jar;
	}

	/**
	 * Set the JAR directly we are going to work on.
	 * 
	 * @param jar
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
	 * @param name URL or filename relative to the base
	 * @param from Message identifying the caller for errors
	 * @return null or a Jar with the contents for the name
	 */
	@Override
	public Jar getJarFromName(String name, String from) {
		Jar j = super.getJarFromName(name, from);
		Glob g = new Glob(name);
		if (j == null) {
			for (Iterator<Jar> cp = getClasspath().iterator(); cp.hasNext();) {
				Jar entry = cp.next();
				if (entry.getSource() == null)
					continue;

				if (g.matcher(entry.getSource()
					.getName())
					.matches()) {
					return entry;
				}
			}
		}
		return j;
	}

	public List<Jar> getJarsFromName(String name, String from) {
		Jar j = super.getJarFromName(name, from);
		if (j != null)
			return Collections.singletonList(j);

		Glob g = new Glob(name);
		List<Jar> result = new ArrayList<>();
		for (Iterator<Jar> cp = getClasspath().iterator(); cp.hasNext();) {
			Jar entry = cp.next();
			if (entry.getSource() == null)
				continue;

			if (g.matcher(entry.getSource()
				.getName())
				.matches()) {
				result.add(entry);
			}
		}
		return result;
	}

	/**
	 * @param manifests
	 * @throws Exception
	 */
	private void merge(Manifest result, Manifest old) {
		if (old != null) {
			for (Iterator<Map.Entry<Object, Object>> e = old.getMainAttributes()
				.entrySet()
				.iterator(); e.hasNext();) {
				Map.Entry<Object, Object> entry = e.next();
				Attributes.Name name = (Attributes.Name) entry.getKey();
				String value = (String) entry.getValue();
				if (name.toString()
					.equalsIgnoreCase(aQute.bnd.osgi.Constants.CREATED_BY))
					name = new Attributes.Name("Originally-Created-By");
				if (!result.getMainAttributes()
					.containsKey(name))
					result.getMainAttributes()
						.put(name, value);
			}

			// do not overwrite existing entries
			Map<String, Attributes> oldEntries = old.getEntries();
			Map<String, Attributes> newEntries = result.getEntries();
			for (Iterator<Map.Entry<String, Attributes>> e = oldEntries.entrySet()
				.iterator(); e.hasNext();) {
				Map.Entry<String, Attributes> entry = e.next();
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
	 * @param properties Properties to verify.
	 */

	void verifyManifestHeadersCase(Properties properties) {
		for (Iterator<Object> i = properties.keySet()
			.iterator(); i.hasNext();) {
			String header = (String) i.next();
			for (int j = 0; j < headers.length; j++) {
				if (!headers[j].equals(header) && headers[j].equalsIgnoreCase(header)) {
					warning(
						"Using a standard OSGi header with the wrong case (bnd is case sensitive!), using: %s and expecting: %s",
						header, headers[j]);
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
		Set<PackageRef> privatePackages = new HashSet<>(contained.keySet());
		privatePackages.removeAll(exports.keySet());

		// private references = ∀ p : contained packages | uses(p)
		Set<PackageRef> containedReferences = newSet();
		for (PackageRef p : contained.keySet()) {
			Collection<PackageRef> uses = this.uses.get(p);
			if (uses != null)
				containedReferences.addAll(uses);
		}

		// Assume we are going to import all exported packages
		Set<PackageRef> toBeImported = new HashSet<>(exports.keySet());

		// Remove packages that are not referenced locally
		toBeImported.retainAll(containedReferences);

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
			// "Cannot automatically import exported package %s because it has
			// no version defined",
			// ep);
			// continue;
			// }

			parameters = new Attrs();
			result.put(ep, parameters);
		}
		return result;
	}

	public boolean referred(PackageRef packageName) {
		// return true;
		for (Map.Entry<PackageRef, List<PackageRef>> contained : uses.entrySet()) {
			if (!contained.getKey()
				.equals(packageName)) {
				if (contained.getValue()
					.contains(packageName))
					return true;
			}
		}
		return false;
	}

	/**
	 * @param jar
	 * @param contracts
	 * @param contracted
	 */
	private void getManifestInfoFromClasspath(Jar jar, Packages classpathExports, Contracts contracts) {
		try {
			Manifest m = jar.getManifest();
			if (m != null) {
				Domain domain = Domain.domain(m);
				Parameters exported = domain.getExportPackage();
				for (Entry<String, Attrs> e : exported.entrySet()) {
					PackageRef ref = getPackageRef(e.getKey());
					if (!classpathExports.containsKey(ref)) {
						e.getValue()
							.put(aQute.bnd.osgi.Constants.INTERNAL_EXPORTED_DIRECTIVE,
								jar.getBsn() + "-" + jar.getVersion());
						Attrs attrs = e.getValue();

						//
						// Fixup any old style specification versions
						//

						fixupOldStyleVersions(attrs);

						classpathExports.put(ref, attrs);
					}
				}

				//
				// Collect any declared contracts
				//
				Parameters pcs = domain.getProvideCapability();
				contracts.collectContracts(jar.getName(), pcs);
			}
		} catch (Exception e) {
			warning("Erroneous Manifest for %s %s", jar, e);
		}
	}

	private void fixupOldStyleVersions(Attrs attrs) {
		if (attrs.containsKey(SPECIFICATION_VERSION) && attrs.getVersion() == null) {
			attrs.put("version", attrs.get(SPECIFICATION_VERSION));
			attrs.remove(SPECIFICATION_VERSION);
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
				Attrs defaultAttrs = new Attrs();
				Attrs importAttributes = imports.get(packageRef);
				Attrs exportAttributes = exports.get(packageRef, classpathExports.get(packageRef, defaultAttrs));

				String exportVersion = exportAttributes.getVersion();
				String importRange = importAttributes.getVersion();

				if (check(Check.IMPORTS)) {
					if (exportAttributes == defaultAttrs) {
						warning(
							"Import package %s not found in any bundle on the -buildpath. List explicitly in Import-Package: p,* to get rid of this warning if false",
							packageRef);
						continue;
					}
					if (!exportAttributes.containsKey(INTERNAL_EXPORTED_DIRECTIVE)
						&& !exports.containsKey(packageRef)) {
						warning("'%s' is a private package import from %s", packageRef,
							exportAttributes.get(INTERNAL_SOURCE_DIRECTIVE));
						continue;
					}
				}

				//
				// Check if we have a contract. If we have a contract
				// then we should remove the version
				//
				if (contracts.isContracted(packageRef)) {
					// yes, contract based, so remove
					// the version and don't do the
					// version policy stuff
					importAttributes.remove(VERSION_ATTRIBUTE);
					continue;
				}

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

					boolean provider;
					if (importAttributes.containsKey(PROVIDE_DIRECTIVE)) {
						provider = isTrue(importAttributes.get(PROVIDE_DIRECTIVE));
					} else if (exportAttributes.containsKey(PROVIDE_DIRECTIVE)) {
						provider = isTrue(exportAttributes.get(PROVIDE_DIRECTIVE));
					} else {
						provider = provided.contains(packageRef);
					}
					exportVersion = cleanupVersion(exportVersion);

					importRange = applyVersionPolicy(exportVersion, importRange, provider);
					if (!importRange.trim()
						.isEmpty()) {
						importAttributes.put(VERSION_ATTRIBUTE, importRange);
					}
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

				fixupAttributes(packageRef, importAttributes);
				removeAttributes(importAttributes);

				String result = importAttributes.get(Constants.VERSION_ATTRIBUTE);
				if (result == null || !Verifier.isVersionRange(result))
					noimports.add(packageRef);
			} finally {
				unsetProperty(CURRENT_PACKAGE);
			}
		}

		if (isPedantic() && noimports.size() != 0) {
			warning("Imports that lack version ranges: %s", noimports);
		}
	}

	Pair<Packages, Parameters> divideRegularAndDynamicImports() {
		Packages regularImports = new Packages(imports);
		Parameters dynamicImports = getDynamicImportPackage();

		Iterator<Entry<PackageRef, Attrs>> regularImportsIterator = regularImports.entrySet()
			.iterator();
		while (regularImportsIterator.hasNext()) {
			Entry<PackageRef, Attrs> packageEntry = regularImportsIterator.next();
			PackageRef packageRef = packageEntry.getKey();
			Attrs attrs = packageEntry.getValue();
			String resolution = attrs.get(Constants.RESOLUTION_DIRECTIVE);
			if (aQute.bnd.osgi.Constants.RESOLUTION_DYNAMIC.equals(resolution)) {
				attrs.remove(Constants.RESOLUTION_DIRECTIVE);
				dynamicImports.put(packageRef.fqn, attrs);
				regularImportsIterator.remove();
			}
		}
		return new Pair<>(regularImports, dynamicImports);
	}

	String applyVersionPolicy(String exportVersion, String importRange, boolean provider) {
		try {
			setProperty("@", exportVersion);

			if (importRange != null) {
				importRange = cleanupVersion(importRange);
				importRange = getReplacer().process(importRange);
			} else
				importRange = getVersionPolicy(provider);

		} finally {
			unsetProperty("@");
		}
		return importRange;
	}

	/**
	 * Find the packages we depend on, where we implement an interface that is a
	 * Provider Type. These packages, when we import them, must use the provider
	 * policy.
	 * 
	 * @throws Exception
	 */
	Set<PackageRef> findProvidedPackages() throws Exception {
		Set<PackageRef> providers = classspace.values()
			.stream()
			.flatMap(c -> {
				// filter out interfaces in the same package as the class
				// implementing the
				// interface.
				PackageRef pkg = c.getClassName()
					.getPackageRef();
				return c.interfaces()
					.stream()
					.filter(i -> !Objects.equals(pkg, i.getPackageRef()));
			})
			.distinct()
			.filter(this::isProvider)
			.map(TypeRef::getPackageRef)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		return providers;
	}

	private boolean isProvider(TypeRef t) {
		Clazz c;
		try {
			c = findClass(t);
		} catch (Exception e) {
			return false;
		}
		if (c == null)
			return false;

		TypeRef providerType = getTypeRef("org/osgi/annotation/versioning/ProviderType");
		return c.annotations()
			.contains(providerType);
	}

	/**
	 * Provide any macro substitutions and versions for exported packages.
	 * 
	 * @throws IOException
	 */

	void augmentExports(Packages exports) throws IOException {
		for (PackageRef packageRef : exports.keySet()) {
			String packageName = packageRef.getFQN();
			setProperty(CURRENT_PACKAGE, packageName);
			Attrs attributes = exports.get(packageRef);
			try {
				Attrs exporterAttributes = classpathExports.get(packageRef);
				if (exporterAttributes == null) {
					if (check(Check.EXPORTS)) {
						Map<String, Resource> map = dot.getDirectories()
							.get(packageRef.getBinary());
						if ((map == null || map.isEmpty())) {
							error("Exporting an empty package '%s'", packageRef.getFQN());
						}
					}
				} else {
					for (Map.Entry<String, String> entry : exporterAttributes.entrySet()) {
						String key = entry.getKey();

						// dont overwrite and no directives
						if (!key.endsWith(":")) {
							if (!attributes.containsKey(key))
								attributes.put(key, entry.getValue());
							else {
								if (since(About._2_4)) {
									// we have the attribute from the classpath
									// and we have set it.
									if (key.equals(Constants.VERSION_ATTRIBUTE)) {
										try {
											Version fromExport = new Version(
												cleanupVersion(exporterAttributes.getVersion()));
											Version fromSet = new Version(cleanupVersion(attributes.getVersion()));
											if (!fromExport.equals(fromSet)) {
												SetLocation location = warning(
													"Version for package %s is set to different values in the source (%s) and in the manifest (%s). The version in the manifest is not "
														+ "picked up by an other sibling bundles in this project or projects that directly depend on this project",
													packageName, attributes.get(key), exporterAttributes.get(key));
												if (getPropertiesFile() != null)
													location.file(getPropertiesFile().getAbsolutePath());
												location.header(Constants.EXPORT_PACKAGE);
												location.context(packageName);
											}
										} catch (Exception e) {
											// Ignored here, is picked up in
											// other places
										}
									}
								}
							}
						}
					}
				}
			} finally {
				unsetProperty(CURRENT_PACKAGE);
			}
			fixupAttributes(packageRef, attributes);
			removeAttributes(attributes);
		}
	}

	/**
	 * Fixup Attributes Execute any macros on an export and
	 * 
	 * @throws IOException
	 */

	void fixupAttributes(PackageRef packageRef, Attrs attributes) throws IOException {
		// Convert any attribute values that have macros.
		for (String key : attributes.keySet()) {
			String value = attributes.get(key);
			if (value.indexOf('$') >= 0) {
				value = getReplacer().process(value);
				attributes.put(key, value);
			}
			if (!key.endsWith(":")) {
				String from = attributes.get(FROM_DIRECTIVE);
				verifyAttribute(from, "package info for " + packageRef, key, value);
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
			attributes.keySet()
				.removeAll(removeInstr.select(attributes.keySet(), false));
		}

		// Remove any ! valued attributes
		for (Iterator<Entry<String, String>> i = attributes.entrySet()
			.iterator(); i.hasNext();) {
			String v = i.next()
				.getValue();
			if (v.equals("!"))
				i.remove();
		}
	}

	/**
	 * Calculate a version from a version policy.
	 * 
	 * @param version The actual exported version
	 * @param impl true for implementations and false for clients
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
	void doUses(Packages exports, Map<PackageRef, List<PackageRef>> uses, Packages imports) {
		if (isTrue(getProperty(NOUSES)))
			return;

		for (Iterator<PackageRef> i = exports.keySet()
			.iterator(); i.hasNext();) {
			PackageRef packageRef = i.next();
			String packageName = packageRef.getFQN();
			setProperty(CURRENT_PACKAGE, packageName);
			try {
				doUses(packageRef, exports, uses, imports);
			} finally {
				unsetProperty(CURRENT_PACKAGE);
			}

		}
	}

	/**
	 * @param packageRef
	 * @param exports
	 * @param uses
	 * @param imports
	 */
	protected void doUses(PackageRef packageRef, Packages exports, Map<PackageRef, List<PackageRef>> uses,
		Packages imports) {
		Attrs clause = exports.get(packageRef);

		// Check if someone already set the uses: directive
		String override = clause.get(USES_DIRECTIVE, USES_USES);

		// Get the used packages
		Collection<PackageRef> usedPackages = uses.get(packageRef);

		if (usedPackages != null) {

			// Only do a uses on exported or imported packages
			// and uses should also not contain our own package
			// name
			Set<PackageRef> sharedPackages = new TreeSet<>();
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
				override = override.replaceAll(USES_USES, Matcher.quoteReplacement(sb.toString()))
					.trim();

			if (override.endsWith(","))
				override = override.substring(0, override.length() - 1);
			if (override.startsWith(","))
				override = override.substring(1);
			if (override.isEmpty()) {
				clause.remove(USES_DIRECTIVE);
			} else {
				clause.put(USES_DIRECTIVE, override);
			}
		} else {
			if (override.equals(USES_USES)) {
				clause.remove(USES_DIRECTIVE);
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
	 * Verify an attribute
	 * 
	 * @param f
	 * @param where
	 * @param key
	 * @param propvalue
	 * @throws IOException
	 */
	private void verifyAttribute(String path, String where, String key, String value) throws IOException {
		SetLocation location;
		if (!Verifier.isExtended(key)) {
			location = error("%s attribute [%s='%s'], key must be an EXTENDED (CORE1.3.2 %s). From %s", where, key,
				value, Verifier.EXTENDED_S, path);
		} else if (value == null || value.trim()
			.length() == 0) {
			location = error(
				"%s attribute [%s='%s'], value is empty which is not allowed in ARGUMENT_S (CORE1.3.2 %s). From %s",
				where, key, value, Verifier.ARGUMENT_S, path);
		} else
			return;
		if (path != null) {
			File f = new File(path);
			if (f.isFile()) {
				FileLine fl = findHeader(f, key);
				if (fl != null)
					fl.set(location);
			}
		}
	}

	@Override
	public void close() throws IOException {
		if (diagnostics) {
			PrintStream out = System.err;
			out.printf("Current directory            : %s%n", new File("").getAbsolutePath());
			out.println("Classpath used");
			for (Jar jar : getClasspath()) {
				out.printf("File                                : %s%n", jar.getSource());
				out.printf("File abs path                       : %s%n", jar.getSource()
					.getAbsolutePath());
				out.printf("Name                                : %s%n", jar.getName());
				Map<String, Map<String, Resource>> dirs = jar.getDirectories();
				for (Map.Entry<String, Map<String, Resource>> entry : dirs.entrySet()) {
					Map<String, Resource> dir = entry.getValue();
					String name = entry.getKey()
						.replace('/', '.');
					if (dir != null) {
						out.printf("                                      %-30s %d%n", name, dir.size());
					} else {
						out.printf("                                      %-30s <<empty>>%n", name);
					}
				}
			}
		}

		super.close();

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
	 */
	public String _findpath(String args[]) {
		return findPath("findpath", args, true);
	}

	public String _findname(String args[]) {
		return findPath("findname", args, false);
	}

	String findPath(String name, String[] args, boolean fullPathName) {
		if (args.length > 3) {
			warning("Invalid nr of arguments to %s %s, syntax: ${%s (; reg-expr (; replacement)? )? }", name,
				Arrays.asList(args), name);
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
		for (Iterator<String> e = dot.getResources()
			.keySet()
			.iterator(); e.hasNext();) {
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
		for (Iterator<Map.Entry<String, String>> i = additional.entrySet()
			.iterator(); i.hasNext();) {
			Map.Entry<String, String> entry = i.next();
			if (force || getProperties().get(entry.getKey()) == null)
				setProperty(entry.getKey(), entry.getValue());
		}
	}

	boolean firstUse = true;

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
		if (isPedantic() && jar.getResources()
			.isEmpty())
			warning("There is an empty jar or directory on the classpath: %s", jar.getName());

		addClose(jar);
		classpath.add(jar);
		updateModified(jar.lastModified(), jar.toString());
	}

	public void addClasspath(Collection<?> jars) throws IOException {
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
			warning("File on classpath that does not exist: %s", cp);
		Jar jar = new Jar(cp);
		addClasspath(jar);
	}

	@Override
	public void clear() {
		classpath.clear();
	}

	@Override
	public void forceRefresh() {
		super.forceRefresh();
		checks = null;
	}

	public Jar getTarget() {
		return getJar();
	}

	private void analyzeBundleClasspath() throws Exception {
		Parameters bcp = getBundleClasspath();

		if (bcp.isEmpty()) {
			analyzeJar(dot, "", true);
		} else {
			boolean okToIncludeDirs = true;

			for (String path : bcp.keySet()) {
				if (dot.getDirectories()
					.containsKey(path)) {
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
						Jar jar = Jar.fromResource(path, resource);
						addClose(jar);
						analyzeJar(jar, "", true);
					} catch (Exception e) {
						warning("Invalid bundle classpath entry: %s: %s", path, e);
					}
				} else {
					if (dot.getDirectories()
						.containsKey(path)) {
						// if directories are used, we should not have dot as we
						// would have the classes in these directories on the
						// class path twice.
						if (bcp.containsKey("."))
							warning(Constants.BUNDLE_CLASSPATH
								+ " uses a directory '%s' as well as '.'. This means bnd does not know if a directory is a package.",
								path);
						analyzeJar(dot, Processor.appendPath(path) + "/", true);
					} else {
						if (!"optional".equals(info.get(RESOLUTION_DIRECTIVE)))
							warning("No sub JAR or directory %s", path);
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
		Map<String, Clazz> mismatched = new HashMap<>();

		next: for (String path : jar.getResources()
			.keySet()) {
			if (path.startsWith(prefix)) {

				String relativePath = path.substring(prefix.length());

				if (okToIncludeDirs) {
					int n = relativePath.lastIndexOf('/');
					if (n < 0)
						n = relativePath.length();
					String relativeDir = relativePath.substring(0, n);

					PackageRef packageRef = getPackageRef(relativeDir);
					learnPackage(jar, prefix, packageRef, contained);
				}

				// Check class resources, we need to analyze them
				if (path.endsWith(".class")) {
					Resource resource = jar.getResource(path);
					Clazz clazz;

					try {
						clazz = new Clazz(this, path, resource);
						clazz.parseClassFile();
					} catch (Throwable e) {
						exception(e, "Invalid class file %s (%s)", relativePath, e);
						continue next;
					}

					String calculatedPath = clazz.getClassName()
						.getPath();
					if (!calculatedPath.equals(relativePath)) {
						// If there is a mismatch we
						// warning
						if (okToIncludeDirs) // assume already reported
							mismatched.put(clazz.getAbsolutePath(), clazz);
					} else {
						classspace.put(clazz.getClassName(), clazz);
						PackageRef packageRef = clazz.getClassName()
							.getPackageRef();
						learnPackage(jar, prefix, packageRef, contained);

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

	/**
	 * Clean up version parameters. Other builders use more fuzzy definitions of
	 * the version syntax. This method cleans up such a version to match an OSGi
	 * version.
	 */
	static Pattern	fuzzyVersion		= Pattern.compile("(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9](.*))?",
		Pattern.DOTALL);
	static Pattern	fuzzyVersionRange	= Pattern
		.compile("(\\(|\\[)\\s*([-\\da-zA-Z.]+)\\s*,\\s*([-\\da-zA-Z.]+)\\s*(\\]|\\))", Pattern.DOTALL);
	static Pattern	fuzzyModifier		= Pattern.compile("(\\d+[.-])*(.*)", Pattern.DOTALL);

	static Pattern	nummeric			= Pattern.compile("\\d*");

	static public String cleanupVersion(String version) {

		if (version == null)
			return "0";

		Matcher m = Verifier.VERSIONRANGE.matcher(version);

		if (m.matches()) {
			try {
				VersionRange vr = new VersionRange(version);
				return version;
			} catch (Exception e) {
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
	 * 
	 * <pre>
	 *  maxint =
	 * 2,147,483,647 = 10 digits
	 * </pre>
	 * 
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

	static final String _classesHelp = "${classes[;<query>;<pattern>]*}, Return a list of fully qualified class names of the contained classes matching the queries.\n"
		+ "A query must be one of " + join(Clazz.QUERY.values());

	public String _classes(String... args) throws Exception {
		Collection<Clazz> matched = getClasses(args);
		if (matched.isEmpty())
			return "";

		return join(matched);
	}

	public Collection<Clazz> getClasses(String... args) throws Exception {

		Set<Clazz> matched = new HashSet<>(classspace.values());
		for (int i = 1; i < args.length; i++) {
			String typeName = args[i].toUpperCase();
			Clazz.QUERY type;
			switch (typeName) {
				case "EXTENDING" :
					type = Clazz.QUERY.EXTENDS;
					break;
				case "IMPORTING" :
					type = Clazz.QUERY.IMPORTS;
					break;
				case "ANNOTATION" :
					type = Clazz.QUERY.ANNOTATED;
					break;
				case "IMPLEMENTING" :
					type = Clazz.QUERY.IMPLEMENTS;
					break;
				default :
					type = Clazz.QUERY.valueOf(typeName);
					break;
			}

			Instruction instr = null;
			if (Clazz.HAS_ARGUMENT.contains(type)) {
				if (++i == args.length) {
					throw new IllegalArgumentException(
						"${classes} query " + type + " must have a pattern argument. " + _classesHelp);
				}
				String s = args[i];
				instr = new Instruction(s);
			}
			for (Iterator<Clazz> c = matched.iterator(); c.hasNext();) {
				Clazz clazz = c.next();
				if (!clazz.is(type, instr, this)) {
					c.remove();
				}
			}
		}
		return new SortedList<>(matched, Clazz.NAME_COMPARATOR);
	}

	static final String _packagesHelp = "${packages[;<query>;<pattern>]}, Return a list of packages contained in the bundle matching the query.\n"
		+ "A query must be one of " + join(Packages.QUERY.values());

	public String _packages(String... args) throws Exception {
		Collection<PackageRef> matched = getPackages(contained, args);
		return matched.isEmpty() ? "" : join(matched);
	}

	public Collection<PackageRef> getPackages(Packages scope, String... args) throws Exception {
		List<PackageRef> pkgs = new LinkedList<>();

		Packages.QUERY queryType;
		Instruction instr;
		if (args.length == 1) {
			queryType = null;
			instr = null;
		} else if (args.length >= 2) {
			queryType = Packages.QUERY.valueOf(args[1].toUpperCase());
			if (args.length > 2)
				instr = new Instruction(args[2]);
			else
				instr = null;
		} else {
			throw new IllegalArgumentException("${packages} macro: invalid argument count");
		}

		for (Entry<PackageRef, Attrs> entry : scope.entrySet()) {
			PackageRef pkg = entry.getKey();

			TypeRef pkgInfoTypeRef = getTypeRefFromFQN(pkg.getFQN() + ".package-info");
			Clazz pkgInfo = classspace.get(pkgInfoTypeRef);

			boolean accept = false;
			if (queryType != null) {
				switch (queryType) {
					case ANY :
						accept = true;
						break;

					case NAMED :
						if (instr == null)
							throw new IllegalArgumentException("Not enough arguments in ${packages} macro");
						accept = instr.matches(pkg.getFQN()) ^ instr.isNegated();
						break;

					case ANNOTATED :
						if (instr == null)
							throw new IllegalArgumentException("Not enough arguments in ${packages} macro");
						accept = pkgInfo != null && pkgInfo.is(Clazz.QUERY.ANNOTATED, instr, this);
						break;
					case VERSIONED :
						accept = entry.getValue()
							.getVersion() != null;
						break;
					case CONDITIONAL :
						accept = entry.getValue()
							.get(INTERNAL_SOURCE_DIRECTIVE)
							.startsWith(CONDITIONALPACKAGE);
						break;
					default :
						break;
				}
			} else {
				accept = true;
			}

			if (accept)
				pkgs.add(pkg);
		}
		return pkgs;
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
			if (jar.getDirectories()
				.containsKey(pack)) {
				sb.append(del);
				sb.append(jar.getName());
			}
		}
		return sb.toString();
	}

	public Map<TypeRef, Clazz> getClassspace() {
		return classspace;
	}

	/**
	 * Return an attribute of a package
	 */

	public String _packageattribute(String[] args) {
		Macro.verifyCommand(args,
			"${packageattribute;<packagename>[;<attributename>]}, Return an attribute of a package, default the version. Only available after analysis",
			null, 2, 3);

		String packageName = args[1];
		String attrName = "version";
		if (args.length > 2)
			attrName = args[2];

		Attrs attrs = contained.getByFQN(packageName);
		if (attrs == null)
			return "version".equals(attrName) ? "0" : "";

		String value = attrs.get(attrName);
		if (value == null)
			return "version".equals(attrName) ? "0" : "";
		else
			return value;
	}

	/**
	 * Locate a resource on the class path.
	 * 
	 * @param path Path of the reosurce
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
				r = Resource.fromURL(url);
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
	 */
	public String getVersion() {
		String version = getProperty(Constants.BUNDLE_VERSION);
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
	protected void doRequireBnd() {
		Attrs require = OSGiHeader.parseProperties(getProperty(REQUIRE_BND));
		if (require == null || require.isEmpty())
			return;

		Hashtable<String, String> map = new Hashtable<>();
		map.put(aQute.bnd.osgi.Constants.VERSION_FILTER, getBndVersion());

		for (String filter : require.keySet()) {
			try {
				Filter f = new Filter(filter);
				if (f.match(map))
					continue;
				error("%s fails for filter %s values=%s", REQUIRE_BND, require.get(filter), map);
			} catch (Exception t) {
				exception(t, "%s with value %s throws exception", REQUIRE_BND, require);
			}
		}
	}

	/**
	 * md5 macro
	 */

	static String _md5Help = "${md5;path}";

	public String _md5(String args[]) throws Exception {
		Macro.verifyCommand(args, _md5Help, new Pattern[] {
			null, null, Pattern.compile("base64|hex")
		}, 2, 3);

		try (Digester<MD5> digester = MD5.getDigester()) {
			Resource r = dot.getResource(args[1]);
			if (r == null)
				throw new FileNotFoundException("From " + digester + ", not found " + args[1]);

			IO.copy(r.openInputStream(), digester);
			boolean hex = args.length > 2 && args[2].equals("hex");
			if (hex)
				return Hex.toHexString(digester.digest()
					.digest());

			return Base64.encodeBase64(digester.digest()
				.digest());
		}
	}

	/**
	 * SHA1 macro
	 */

	static String _sha1Help = "${sha1;path}";

	public String _sha1(String args[]) throws Exception {
		Macro.verifyCommand(args, _sha1Help, new Pattern[] {
			null, null, Pattern.compile("base64|hex")
		}, 2, 3);
		try (Digester<SHA1> digester = SHA1.getDigester()) {
			Resource r = dot.getResource(args[1]);
			if (r == null)
				throw new FileNotFoundException("From sha1, not found " + args[1]);

			IO.copy(r.openInputStream(), digester);
			return Base64.encodeBase64(digester.digest()
				.digest());
		}
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
	 * @param instructions the instructions with patterns.
	 * @param source the actual found packages, contains no duplicates
	 * @return Only the packages that were filtered by the given instructions
	 */

	Packages filter(Instructions instructions, Packages source, Set<Instruction> nomatch) {
		Packages result = new Packages();
		List<PackageRef> refs = new ArrayList<>(source.keySet());
		Collections.sort(refs);

		List<Instruction> filters = new ArrayList<>(instructions.keySet());
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

	public Clazz.JAVA getHighestEE() {
		if (ees.isEmpty())
			return Clazz.JAVA.JDK1_4;

		return ees.last();
	}

	public String _ee(String args[]) {
		return getHighestEE().getEE();
	}

	/**
	 * Calculate the output file for the given target. The strategy is:
	 * 
	 * <pre>
	 * parameter given if not null and not directory if directory, this will be
	 * the output directory based on bsn-version.jar name of the source file if
	 * exists Untitled-[n]
	 * </pre>
	 * 
	 * @param output may be null, otherwise a file path relative to base
	 */
	public File getOutputFile(String output) {

		if (output == null)
			output = get(aQute.bnd.osgi.Constants.OUTPUT);

		File outputDir;

		if (output != null) {
			File outputFile = getFile(output);
			if (outputFile.isDirectory())
				outputDir = outputFile;
			else
				return outputFile;
		} else
			outputDir = getBase();

		Entry<String, Attrs> name = getBundleSymbolicName();
		if (name != null) {
			String bsn = name.getKey();
			String version = getBundleVersion();
			Version v = Version.parseVersion(version);
			String outputName = bsn + "-" + v.toStringWithoutQualifier()
				+ aQute.bnd.osgi.Constants.DEFAULT_JAR_EXTENSION;
			return new File(outputDir, outputName);
		}

		File source = getJar().getSource();
		if (source != null) {
			String outputName = source.getName();
			return new File(outputDir, outputName);
		}

		if (getPropertiesFile() != null) {
			String nm = getPropertiesFile().getName();
			if (nm.endsWith(aQute.bnd.osgi.Constants.DEFAULT_BND_EXTENSION)) {
				nm = nm.substring(0, nm.length() - aQute.bnd.osgi.Constants.DEFAULT_BND_EXTENSION.length())
					+ aQute.bnd.osgi.Constants.DEFAULT_JAR_EXTENSION;
				logger.debug("name is {}", nm);
				return new File(outputDir, nm);
			}
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
	 * @param output the output file, if null {@link #getOutputFile(String)} is
	 *            used.
	 * @param force if it needs to be overwritten
	 * @throws Exception
	 */

	public boolean save(File output, boolean force) throws Exception {
		if (output == null)
			output = getOutputFile(null);

		Jar jar = getJar();
		File source = jar.getSource();

		logger.debug("check for modified build={} file={}, diff={}", jar.lastModified(), output.lastModified(),
			jar.lastModified() - output.lastModified());

		if (!output.exists() || output.lastModified() <= jar.lastModified() || force) {
			File op = output.getParentFile();
			IO.mkdirs(op);
			if (source != null && output.getCanonicalPath()
				.equals(source.getCanonicalPath())) {
				File bak = new File(source.getParentFile(), source.getName() + ".bak");
				try {
					IO.rename(source, bak);
				} catch (IOException e) {
					exception(e, "Could not create backup file %s", bak);
				}
			}
			try {
				logger.debug("Saving jar to {}", output);
				getJar().write(output);
			} catch (Exception e) {
				IO.delete(output);
				exception(e, "Cannot write JAR file to %s due to %s", output, e);
			}
			return true;
		}
		logger.debug("Not modified {}", output);
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
	 */
	public Map<PackageRef, List<PackageRef>> cleanupUses(Map<PackageRef, List<PackageRef>> apiUses,
		boolean removeJava) {
		MultiMap<PackageRef, PackageRef> map = new MultiMap<>(apiUses);
		for (Entry<PackageRef, List<PackageRef>> e : map.entrySet()) {
			e.getValue()
				.remove(e.getKey());
			if (!removeJava)
				continue;

			e.getValue()
				.removeIf(PackageRef::isJava);
		}
		return map;
	}

	/**
	 * Return the classes for a given source package.
	 * 
	 * @param source the source package
	 * @return a set of classes for the requested package.
	 */
	public Set<Clazz> getClassspace(PackageRef source) {
		Set<Clazz> result = new HashSet<>();
		for (Clazz c : getClassspace().values()) {
			if (c.getClassName()
				.getPackageRef() == source)
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
	 * @throws Exception
	 */
	public Map<Clazz.Def, List<TypeRef>> getXRef(final PackageRef source, final Collection<PackageRef> dest,
		final int sourceModifiers) throws Exception {
		final MultiMap<Clazz.Def, TypeRef> xref = new MultiMap<>(Clazz.Def.class, TypeRef.class, true);

		for (final Clazz clazz : getClassspace().values()) {
			if ((clazz.accessx & sourceModifiers) == 0)
				continue;

			if (source != null && source != clazz.getClassName()
				.getPackageRef())
				continue;

			clazz.parseClassFileWithCollector(new ClassDataCollector() {
				Clazz.Def member;

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

	public String _exports(String[] args) {
		return join(filter(getExports().keySet(), args));
	}

	public String _imports(String[] args) {
		return join(filter(getImports().keySet(), args));
	}

	private <T> Collection<T> filter(Collection<T> list, String[] args) {
		if (args == null || args.length <= 1)
			return list;
		if (args.length > 2)
			warning("Too many arguments for ${%s} macro", args[0]);
		Instructions instrs = new Instructions(args[1]);
		return instrs.select(list, false);
	}

	/**
	 * Report the details of this analyzer
	 */

	@Override
	public void report(Map<String, Object> table) throws Exception {
		super.report(table);
		analyze();
		table.put("Contained", getContained().entrySet());
		table.put("Imported", getImports().entrySet());
		table.put("Exported", getExports().entrySet());
		table.put("Referred", getReferred().entrySet());
		table.put("Bundle Symbolic Name", getBsn());
		table.put("Execution Environments", ees);
	}

	/**
	 * Return the EEs
	 */

	public SortedSet<Clazz.JAVA> getEEs() {
		return ees;
	}

	/**
	 * @param name
	 */
	public String validResourcePath(String name, String reportIfWrong) {
		boolean changed = false;
		StringBuilder sb = new StringBuilder(name);
		for (int i = 0; i < sb.length(); i++) {
			char c = sb.charAt(i);
			if (c == '-' || c == '.' || c == '_' || c == '$' || Character.isLetterOrDigit(c))
				continue;
			sb.replace(i, i + 1, "-");
			changed = true;
		}
		if (changed) {
			if (reportIfWrong != null)
				warning("%s: %s", reportIfWrong, name);
			return sb.toString();
		}
		return name;
	}

	/**
	 * Check if we have an a check option
	 */

	public boolean check(Check key) {
		if (checks == null) {
			Parameters p = new Parameters(getProperty("-check"), this);
			checks = EnumSet.noneOf(Check.class);
			for (String k : p.keyList()) {
				try {
					if (k.equalsIgnoreCase("all")) {
						checks = EnumSet.allOf(Check.class);
						break;
					}

					Check c = Check.valueOf(k.toUpperCase()
						.replace('-', '_'));
					checks.add(c);

				} catch (Exception e) {
					error("Invalid -check constant, allowed values are %s", Arrays.toString(Check.values()));
				}
			}
		}
		return checks.contains(key) || checks.contains(Check.ALL);
	}

	/**
	 * Find the source file for this type
	 * 
	 * @param type
	 * @throws Exception
	 */
	public String getSourceFileFor(TypeRef type) throws Exception {
		Set<File> sp = Collections.singleton(getFile(getProperty(DEFAULT_PROP_SRC_DIR, "src")));
		return getSourceFileFor(type, sp);
	}

	public String getSourceFileFor(TypeRef type, Collection<File> sourcePath) throws Exception {
		Clazz clazz = findClass(type);
		if (clazz == null) {
			Attrs attrs = classpathExports.get(type.getPackageRef());
			String from = attrs.get(aQute.bnd.osgi.Constants.FROM_DIRECTIVE);
			if (from != null) {
				return from;
			}
			return null;
		}

		String path = type.getPackageRef()
			.getBinary() + "/" + clazz.sourceFile;

		for (File srcDir : sourcePath) {
			if (!srcDir.isFile())
				continue;

			File file = IO.getFile(srcDir, path);
			if (file.isFile()) {
				return IO.absolutePath(file);
			}

		}
		return "";
	}

	/**
	 * Set location information for a type.
	 */

	public void setTypeLocation(SetLocation location, TypeRef type) throws Exception {
		String sf = getSourceFileFor(type);
		if (sf != null) {
			File sff = IO.getFile(sf);
			if (sff != null) {
				String names[] = {
					type.getShorterName(), type.getFQN(), type.getShortName()
						.replace('$', '.')
				};
				for (String name : names) {
					FileLine fl = Processor.findHeader(sff,
						Pattern.compile("(class|interface)\\s*" + name, Pattern.DOTALL));
					if (fl != null)
						fl.set(location);
				}
			}
			location.file(sf);
		}
	}

	public boolean assignable(String annoService, String inferredService) {
		if (annoService == null || annoService.isEmpty() || inferredService == null || inferredService.isEmpty()
			|| Object.class.getName()
				.equals(inferredService))
			return true;
		try {
			Clazz annoServiceClazz = findClass(getTypeRefFromFQN(annoService));
			Clazz inferredServiceClazz = findClass(getTypeRefFromFQN(inferredService));
			return assignable(annoServiceClazz, inferredServiceClazz);
		} catch (Exception e) {}
		// we couldn't determine
		return true;
	}

	public boolean assignable(Clazz annoServiceClazz, Clazz inferredServiceClazz) {
		if (annoServiceClazz == null || inferredServiceClazz == null)
			// we don't know what one of the classes is, assume assignable.
			return true;
		if (annoServiceClazz.equals(inferredServiceClazz))
			return true;
		if (!inferredServiceClazz.isInterface()) {
			if (annoServiceClazz.isInterface())
				return false;
			TypeRef zuper = annoServiceClazz.getSuper();
			if (zuper == null)
				return false;
			try {
				return assignable(findClass(zuper), inferredServiceClazz);
			} catch (Exception e) {
				// can't tell
				return true;
			}
		}
		TypeRef[] intfs = annoServiceClazz.getInterfaces();
		if (intfs != null) {
			for (TypeRef intf : intfs) {
				try {
					if (assignable(findClass(intf), inferredServiceClazz))
						return true;
				} catch (Exception e) {
					return true;
				}
			}
		}
		TypeRef superType = annoServiceClazz.getSuper();
		if (superType != null) {
			try {
				Clazz zuper = findClass(superType);
				if (zuper != null)
					return assignable(zuper, inferredServiceClazz);
				// cannot analyze super class
				return true;
			} catch (Exception e) {
				// cannot analyze super class
				return true;
			}
		}
		// no more superclasses, not assignable.
		return false;
	}

}

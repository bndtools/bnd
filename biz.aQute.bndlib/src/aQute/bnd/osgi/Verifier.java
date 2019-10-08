package aQute.bnd.osgi;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.service.verifier.VerifierPlugin;
import aQute.bnd.version.VersionRange;
import aQute.lib.base64.Base64;
import aQute.lib.filter.Filter;
import aQute.lib.io.IO;
import aQute.lib.regex.PatternConstants;
import aQute.libg.cryptography.Digester;
import aQute.libg.cryptography.SHA1;
import aQute.libg.qtokens.QuotedTokenizer;

//
// TODO check that component XML that refer to a properties file actually have such a file
//

public class Verifier extends Processor {

	private final Jar			dot;
	private final Manifest		manifest;
	private final Domain		main;

	private boolean				r3;
	private boolean				usesRequire;

	final static Pattern		EENAME							= Pattern.compile(																			//
		"CDC-1\\.0/Foundation-1\\.0"																														//
			+ "|CDC-1\\.1/Foundation-1\\.1"																													//
			+ "|OSGi/Minimum-1\\.[0-2]"																														//
			+ "|JRE-1\\.1"																																	//
			+ "|J2SE-1\\.[2-5]"																																//
			+ "|JavaSE-1\\.[6-8]"																															//
			+ "|JavaSE-9"																																	//
			+ "|JavaSE-[1-9][0-9]"																															//
			+ "|PersonalJava-1\\.[12]"																														//
			+ "|CDC-1\\.0/PersonalBasis-1\\.0"																												//
			+ "|CDC-1\\.0/PersonalJava-1\\.0"																												//
			+ "|CDC-1\\.1/PersonalBasis-1\\.1"																												//
			+ "|CDC-1\\.1/PersonalJava-1\\.1");
	public final static String	EES[]							= {																							//
		"CDC-1.0/Foundation-1.0",																															//
		"CDC-1.1/Foundation-1.1",																															//
		"OSGi/Minimum-1.0",																																	//
		"OSGi/Minimum-1.1",																																	//
		"OSGi/Minimum-1.2",																																	//
		"JRE-1.1",																																			//
		"J2SE-1.2",																																			//
		"J2SE-1.3",																																			//
		"J2SE-1.4",																																			//
		"J2SE-1.5",																																			//
		"JavaSE-1.6",																																		//
		"JavaSE-1.7",																																		//
		"JavaSE-1.8",																																		//
		"JavaSE-9",																																			//
		"JavaSE-10",																																		//
		"JavaSE-11",																																		//
		"JavaSE-12",																																		//
		"JavaSE-13",																																		//
		"JavaSE-14",																																						//
		"PersonalJava-1.1",																																	//
		"PersonalJava-1.2",																																	//
		"CDC-1.0/PersonalBasis-1.0",																														//
		"CDC-1.0/PersonalJava-1.0",																															//
		"CDC-1.1/PersonalBasis-1.1",																														//
		"CDC-1.1/PersonalJava-1.1"
	};
	public final static Pattern	ReservedFileNames				= Pattern
		.compile("CON(\\..+)?|PRN(\\..+)?|AUX(\\..+)?|CLOCK\\$|NUL(\\..+)?|COM[1-9](\\..+)?|LPT[1-9](\\..+)?|"
			+ "\\$Mft|\\$MftMirr|\\$LogFile|\\$Volume|\\$AttrDef|\\$Bitmap|\\$Boot|\\$BadClus|\\$Secure|"
			+ "\\$Upcase|\\$Extend|\\$Quota|\\$ObjId|\\$Reparse", Pattern.CASE_INSENSITIVE);

	final static Pattern		CARDINALITY_PATTERN				= Pattern.compile("single|multiple");
	final static Pattern		RESOLUTION_PATTERN				= Pattern.compile("optional|mandatory");
	final static Pattern		BUNDLEMANIFESTVERSION			= Pattern.compile("2");

	public final static Pattern	TOKEN							= Pattern.compile(PatternConstants.TOKEN);
	public final static String	EXTENDED_S						= "[-.\\w]+";
	public final static Pattern	EXTENDED_P						= Pattern.compile(EXTENDED_S);
	public final static String	QUOTEDSTRING					= "\"[^\"]*\"";
	public final static Pattern	QUOTEDSTRING_P					= Pattern.compile(QUOTEDSTRING);
	public final static String	ARGUMENT_S						= "(:?" + EXTENDED_S + ")|(?:" + QUOTEDSTRING + ")";
	public final static Pattern	ARGUMENT_P						= Pattern.compile(ARGUMENT_S);
	public final static String	SYMBOLICNAME_STRING				= PatternConstants.SYMBOLICNAME;
	public final static Pattern	SYMBOLICNAME					= Pattern.compile(SYMBOLICNAME_STRING);

	public final static String	VERSION_STRING					= "\\d{1,9}(\\.\\d{1,9}(\\.\\d{1,9}(\\."
		+ PatternConstants.TOKEN + ")?)?)?";
	public final static String	VERSION_S						= "\\d{1,9}(:?\\.\\d{1,9}(:?\\.\\d{1,9}(:?\\."
		+ PatternConstants.TOKEN + ")?)?)?";
	public final static Pattern	VERSION							= Pattern.compile(VERSION_STRING);
	public final static Pattern	VERSION_P						= Pattern.compile(VERSION_S);
	public final static Pattern	VERSIONRANGE					= Pattern
		.compile("((\\(|\\[)" + VERSION_STRING + "," + VERSION_STRING + "(\\]|\\)))|" + VERSION_STRING);
	public final static String	VERSION_RANGE_S					= "(?:(:?\\(|\\[)" + VERSION_S + "," + VERSION_S
		+ "(\\]|\\)))|" + VERSION_S;
	public final static Pattern	VERSIONRANGE_P					= VERSIONRANGE;

	final static Pattern		FILTEROP						= Pattern.compile("=|<=|>=|~=");
	final static Pattern		FILE							= Pattern
		.compile("/?[^/\"\n\r\u0000]+(/[^/\"\n\r\u0000]+)*");
	final static Pattern		WILDCARDPACKAGE					= Pattern.compile("((\\w)+(\\.(\\w)+)*(\\.\\*)?)|\\*");
	public final static Pattern	ISO639							= Pattern.compile("\\p{Upper}{2}");
	public final static Pattern	HEADER_PATTERN					= Pattern
		.compile("\\p{Alnum}" + PatternConstants.TOKEN);
	public final static Pattern	NUMBERPATTERN					= Pattern.compile("\\d+");
	public final static Pattern	FLOATPATTERN					= Pattern.compile("[-+]?\\d*\\.?\\d+([eE][-+]?\\d+)?");
	public final static Pattern	BOOLEANPATTERN					= Pattern.compile("true|false",
		Pattern.CASE_INSENSITIVE);
	public final static Pattern	PACKAGEPATTERN					= Pattern.compile(
		"\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");
	public final static Pattern	PACKAGEPATTERN_OR_EMPTY			= Pattern.compile(PACKAGEPATTERN + "|^$");
	public final static Pattern	MULTIPACKAGEPATTERN				= Pattern
		.compile("(\\s*" + PACKAGEPATTERN + ")(\\s*,\\s*" + PACKAGEPATTERN + ")*\\s*");
	public final static Pattern	PATHPATTERN						= Pattern.compile(".*");
	public final static Pattern	FQNPATTERN						= Pattern.compile(".*");
	public final static Pattern	URLPATTERN						= Pattern.compile(".*");
	public final static Pattern	ANYPATTERN						= Pattern.compile(".*");
	public final static Pattern	FILTERPATTERN					= Pattern.compile(".*");
	public final static Pattern	TRUEORFALSEPATTERN				= Pattern.compile("true|false|TRUE|FALSE");
	public final static Pattern	WILDCARDNAMEPATTERN				= Pattern.compile(".*");
	public final static Pattern	BUNDLE_ACTIVATIONPOLICYPATTERN	= Pattern.compile("lazy");

	public final static String	OSNAMES[]						= {
		"AIX",																																				// IBM
		"DigitalUnix",																																		// Compaq
		"Embos",																																			// Segger
																																							// Embedded
																																							// Software
																																							// Solutions
		"Epoc32",																																			// SymbianOS
																																							// Symbian
																																							// OS
		"FreeBSD",																																			// Free
																																							// BSD
		"HPUX",																																				// hp-ux
																																							// Hewlett
																																							// Packard
		"IRIX",																																				// Silicon
																																							// Graphics
		"Linux",																																			// Open
																																							// source
		"MacOS",																																			// Apple
		"NetBSD",																																			// Open
																																							// source
		"Netware",																																			// Novell
		"OpenBSD",																																			// Open
																																							// source
		"OS2",																																				// OS/2
																																							// IBM
		"QNX",																																				// procnto
																																							// QNX
		"Solaris",																																			// Sun
																																							// (almost
																																							// an
																																							// alias
																																							// of
																																							// SunOS)
		"SunOS",																																			// Sun
																																							// Microsystems
		"VxWorks",																																			// WindRiver
																																							// Systems
		"Windows95", "Win32", "Windows98", "WindowsNT", "WindowsCE", "Windows2000",																			// Win2000
		"Windows2003",																																		// Win2003
		"WindowsXP", "WindowsVista",
	};

	public final static String	PROCESSORNAMES[]				= {																							//
		//
		"68k",																																				// Motorola
																																							// 68000
		"ARM_LE",																																			// Intel
																																							// Strong
																																							// ARM.
																																							// Deprecated
																																							// because
																																							// it
																																							// does
																																							// not
		// specify the endianness. See the following two rows.
		"arm_le",																																			// Intel
																																							// Strong
																																							// ARM
																																							// Little
																																							// Endian
																																							// mode
		"arm_be",																																			// Intel
																																							// String
																																							// ARM
																																							// Big
																																							// Endian
																																							// mode
		"Alpha",																																			//
		"ia64n",																																			// Hewlett
																																							// Packard
																																							// 32
																																							// bit
		"ia64w",																																			// Hewlett
																																							// Packard
																																							// 64
																																							// bit
																																							// mode
		"Ignite",																																			// psc1k
																																							// PTSC
		"Mips",																																				// SGI
		"PArisc",																																			// Hewlett
																																							// Packard
		"PowerPC",																																			// power
																																							// ppc
																																							// Motorola/IBM
																																							// Power
																																							// PC
		"Sh4",																																				// Hitachi
		"Sparc",																																			// SUN
		"Sparcv9",																																			// SUN
		"S390",																																				// IBM
																																							// Mainframe
																																							// 31
																																							// bit
		"S390x",																																			// IBM
																																							// Mainframe
																																							// 64-bit
		"V850E",																																			// NEC
																																							// V850E
		"x86",																																				// pentium
																																							// i386
		"i486",																																				// i586
																																							// i686
																																							// Intel&
																																							// AMD
																																							// 32
																																							// bit
		"x86-64",
	};

	final Analyzer				analyzer;
	private Instructions		dynamicImports;
	private boolean				frombuilder;

	public Verifier(Jar jar) throws Exception {
		super(new Analyzer(jar));
		this.analyzer = (Analyzer) getParent();
		this.dot = jar;
		this.manifest = analyzer.calcManifest();
		this.main = Domain.domain(manifest);
		addClose(analyzer);
		getInfo(analyzer);
	}

	public Verifier(Analyzer analyzer) throws Exception {
		super(analyzer);
		this.analyzer = analyzer;
		this.dot = analyzer.getJar();
		this.manifest = dot.getManifest();
		this.main = Domain.domain(manifest);
	}

	private void verifyHeaders() {
		for (String h : main) {
			if (!HEADER_PATTERN.matcher(h)
				.matches())
				error("Invalid Manifest header: %s, pattern=%s", h, HEADER_PATTERN);
		}
	}

	/*
	 * Bundle-NativeCode ::= nativecode ( ',' nativecode )* ( ’,’ optional) ?
	 * nativecode ::= path ( ';' path )* // See 1.4.2 ( ';' parameter )+
	 * optional ::= ’*’
	 */
	public void verifyNative() {
		String nc = get(Constants.BUNDLE_NATIVECODE);
		doNative(nc);
	}

	public void doNative(String nc) {
		if (nc != null) {
			QuotedTokenizer qt = new QuotedTokenizer(nc, ",;=", false);
			char del;
			do {
				do {
					String name = qt.nextToken();
					if (name == null) {
						error("Can not parse name from bundle native code header: %s", nc);
						return;
					}
					del = qt.getSeparator();
					if (del == ';') {
						if (dot != null && !dot.exists(name)) {
							error("Native library not found in JAR: %s", name);
						}
					} else {
						String value = null;
						if (del == '=')
							value = qt.nextToken();

						String key = name.toLowerCase();
						if (key.equals("osname")) {
							// ...
						} else if (key.equals("osversion")) {
							// verify version range
							verify(value, VERSIONRANGE);
						} else if (key.equals("language")) {
							verify(value, ISO639);
						} else if (key.equals("processor")) {
							// verify(value, PROCESSORS);
						} else if (key.equals("selection-filter")) {
							// verify syntax filter
							verifyFilter(value);
						} else if (name.equals("*") && value == null) {
							// Wildcard must be at end.
							if (qt.nextToken() != null)
								error("Bundle-Native code header may only END in wildcard %s", nc);
						} else {
							warning("Unknown attribute in native code: %s=%s", name, value);
						}
						del = qt.getSeparator();
					}
				} while (del == ';');
			} while (del == ',');
		}
	}

	public boolean verifyFilter(String value) {
		String s = validateFilter(value);
		if (s == null)
			return true;

		error("%s", s);
		return false;
	}

	public static String validateFilter(String value) {
		try {
			verifyFilter(value, 0);
			return null;
		} catch (Exception e) {
			return "Not a valid filter: " + value + ": " + e;
		}
	}

	public static class BundleActivatorError extends aQute.bnd.util.dto.DTO {
		public final String				activatorClassName;
		public final ActivatorErrorType	errorType;

		public BundleActivatorError(String activatorName, ActivatorErrorType type) {
			activatorClassName = activatorName;
			errorType = type;
		}
	}

	public enum ActivatorErrorType {
		IS_INTERFACE,
		IS_ABSTRACT,
		NOT_PUBLIC,
		NO_SUITABLE_CONSTRUCTOR,
		NOT_AN_ACTIVATOR,
		DEFAULT_PACKAGE,
		NOT_ACCESSIBLE,
		IS_IMPORTED,
		NOT_SET,
		NO_RESULT_FROM_MACRO,
		MULTIPLE_TYPES,
		INVALID_TYPE_NAME;
	}

	private void verifyActivator() throws Exception {
		String bactivator = main.get(Constants.BUNDLE_ACTIVATOR);
		if (bactivator != null) {
			if (!PACKAGEPATTERN.matcher(bactivator)
				.matches()) {

				boolean allElementsAreTypes = true;
				for (String element : split(bactivator)) {
					if (!PACKAGEPATTERN.matcher(element.trim())
						.matches()) {
						allElementsAreTypes = false;
						break;
					}
				}

				if (allElementsAreTypes) {
					registerActivatorErrorLocation(error(
						"The Bundle-Activator header only supports a single type. The following types were found: %s. This usually happens when a macro resolves to multiple types",
						bactivator), bactivator, ActivatorErrorType.MULTIPLE_TYPES);
				} else {
					registerActivatorErrorLocation(
						error("A Bundle-Activator header is present and its value is not a valid type name %s",
							bactivator),
						bactivator, ActivatorErrorType.INVALID_TYPE_NAME);
				}
				return;
			}

			TypeRef ref = analyzer.getTypeRefFromFQN(bactivator);
			if (analyzer.getClassspace()
				.containsKey(ref)) {
				Clazz activatorClazz = analyzer.getClassspace()
					.get(ref);

				if (activatorClazz.isInterface()) {
					registerActivatorErrorLocation(
						error("The Bundle Activator %s is an interface and therefore cannot be instantiated.",
							bactivator),
						bactivator, ActivatorErrorType.IS_INTERFACE);
				} else {
					if (activatorClazz.isAbstract()) {
						registerActivatorErrorLocation(
							error("The Bundle Activator %s is abstract and therefore cannot be instantiated.",
								bactivator),
							bactivator, ActivatorErrorType.IS_ABSTRACT);
					}
					if (!activatorClazz.isPublic()) {
						registerActivatorErrorLocation(
							error("Bundle Activator classes must be public, and %s is not.", bactivator), bactivator,
							ActivatorErrorType.NOT_PUBLIC);
					}
					if (!activatorClazz.hasPublicNoArgsConstructor()) {
						registerActivatorErrorLocation(error(
							"Bundle Activator classes must have a public zero-argument constructor and %s does not.",
							bactivator), bactivator, ActivatorErrorType.NO_SUITABLE_CONSTRUCTOR);
					}

					if (!analyzer.assignable(activatorClazz.getFQN(), "org.osgi.framework.BundleActivator")) {

						registerActivatorErrorLocation(
							error("The Bundle Activator %s does not implement BundleActivator.", bactivator),
							bactivator, ActivatorErrorType.NOT_AN_ACTIVATOR);
					}
				}
				return;
			}

			PackageRef packageRef = ref.getPackageRef();
			if (packageRef.isDefaultPackage())
				registerActivatorErrorLocation(
					error("The Bundle Activator is not in the bundle and it is in the default package "), bactivator,
					ActivatorErrorType.DEFAULT_PACKAGE);
			else if (!analyzer.isImported(packageRef)) {
				registerActivatorErrorLocation(
					error(Constants.BUNDLE_ACTIVATOR + " not found on the bundle class path nor in imports: %s",
						bactivator),
					bactivator, ActivatorErrorType.NOT_ACCESSIBLE);
			} else {
				registerActivatorErrorLocation(warning(Constants.BUNDLE_ACTIVATOR
					+ " %s is being imported into the bundle rather than being contained inside it. This is usually a bundle packaging error",
					bactivator), bactivator, ActivatorErrorType.IS_IMPORTED);
			}
		} else if (parent != null) {
			// If we have access to the parent we can do deeper checking
			String raw = parent.getUnprocessedProperty(BUNDLE_ACTIVATOR, null);
			if (raw != null) {
				// The activator was specified, but nothing showed up.
				if (raw.isEmpty()) {
					registerActivatorErrorLocation(
						warning("A Bundle-Activator header was present but no activator class was defined"), "",
						ActivatorErrorType.NOT_SET);
				} else {
					registerActivatorErrorLocation(error(
						"A Bundle-Activator header is present but no activator class was found using the macro %s",
						raw), raw, ActivatorErrorType.NO_RESULT_FROM_MACRO);
				}
				return;
			}
		}
	}

	private void registerActivatorErrorLocation(SetLocation location, String activator, ActivatorErrorType errorType)
		throws Exception {
		FileLine fl = getHeader(Constants.BUNDLE_ACTIVATOR);
		location.header(Constants.BUNDLE_ACTIVATOR)
			.file(fl.file.getAbsolutePath())
			.line(fl.line)
			.length(fl.length)
			.details(new BundleActivatorError(activator, errorType));
	}

	private void verifyComponent() {
		String serviceComponent = main.get(Constants.SERVICE_COMPONENT);
		if (serviceComponent != null) {
			Parameters map = parseHeader(serviceComponent);
			for (String component : map.keySet()) {
				if (component.indexOf('*') < 0 && !dot.exists(component)) {
					error(Constants.SERVICE_COMPONENT + " entry can not be located in JAR: %s", component);
				} else {
					// validate component ...
				}
			}
		}
	}

	/**
	 * Check for unresolved imports. These are referrals that are not imported
	 * by the manifest and that are not part of our bundle class path. The are
	 * calculated by removing all the imported packages and contained from the
	 * referred packages.
	 *
	 * @throws Exception
	 */
	private void verifyUnresolvedReferences() throws Exception {

		//
		// If we're being called from the builder then this should
		// already have been done
		//

		if (isFrombuilder())
			return;

		Manifest m = analyzer.getJar()
			.getManifest();
		if (m == null) {
			error("No manifest");
			return;
		}

		Domain domain = Domain.domain(m);

		Set<PackageRef> unresolvedReferences = new TreeSet<>(analyzer.getReferred()
			.keySet());
		unresolvedReferences.removeAll(analyzer.getContained()
			.keySet());
		for (String pname : domain.getImportPackage()
			.keySet()) {
			PackageRef pref = analyzer.getPackageRef(pname);
			unresolvedReferences.remove(pref);
		}

		// Remove any java.** packages.
		for (Iterator<PackageRef> p = unresolvedReferences.iterator(); p.hasNext();) {
			PackageRef pack = p.next();
			if (pack.isJava())
				p.remove();
			else {
				// Remove any dynamic imports
				if (isDynamicImport(pack))
					p.remove();
			}
		}

		//
		// If there is a Require bundle, all bets are off and
		// we cannot verify anything
		//

		if (domain.getRequireBundle()
			.isEmpty()
			&& domain.get("ExtensionBundle-Activator") == null
			&& (domain.getFragmentHost() == null || domain.getFragmentHost()
				.getKey()
				.equals("system.bundle"))) {

			if (!unresolvedReferences.isEmpty()) {
				// Now we want to know the
				// classes that are the culprits
				Set<String> culprits = new HashSet<>();
				for (Clazz clazz : analyzer.getClassspace()
					.values()) {
					if (hasOverlap(unresolvedReferences, clazz.getReferred()))
						culprits.add(clazz.getAbsolutePath());
				}

				if (analyzer instanceof Builder)
					warning("Unresolved references to %s by class(es) %s on the " + Constants.BUNDLE_CLASSPATH + ": %s",
						unresolvedReferences, culprits, analyzer.getBundleClasspath()
							.keySet());
				else
					error("Unresolved references to %s by class(es) %s on the " + Constants.BUNDLE_CLASSPATH + ": %s",
						unresolvedReferences, culprits, analyzer.getBundleClasspath()
							.keySet());
				return;
			}
		} else if (isPedantic())
			warning("Use of " + Constants.REQUIRE_BUNDLE
				+ ", ExtensionBundle-Activator, or a system bundle fragment makes it impossible to verify unresolved references");
	}

	/**
	 * @param p
	 * @param pack
	 */
	private boolean isDynamicImport(PackageRef pack) {
		if (dynamicImports == null)
			dynamicImports = new Instructions(main.getDynamicImportPackage());

		if (dynamicImports.isEmpty())
			return false;

		return dynamicImports.matches(pack.getFQN());
	}

	private boolean hasOverlap(Set<?> a, Set<?> b) {
		for (Iterator<?> i = a.iterator(); i.hasNext();) {
			if (b.contains(i.next()))
				return true;
		}
		return false;
	}

	public void verify() throws Exception {
		verifyHeaders();
		verifyDirectives(Constants.EXPORT_PACKAGE, "uses:|mandatory:|include:|exclude:|" + IMPORT_DIRECTIVE,
			PACKAGEPATTERN, "package");
		verifyDirectives(Constants.IMPORT_PACKAGE, "resolution:", PACKAGEPATTERN, "package");
		verifyDirectives(Constants.REQUIRE_BUNDLE, "visibility:|resolution:", SYMBOLICNAME, "bsn");
		verifyDirectives(Constants.FRAGMENT_HOST, "extension:", SYMBOLICNAME, "bsn");
		verifyDirectives(Constants.PROVIDE_CAPABILITY,
			namespace -> "osgi.serviceloader".equals(namespace) ? "effective:|uses:|register:" : "effective:|uses:",
			null, null);
		verifyDirectives(Constants.REQUIRE_CAPABILITY, "effective:|resolution:|filter:|cardinality:", null, null);
		verifyDirectives(Constants.BUNDLE_SYMBOLICNAME, "singleton:|fragment-attachment:|mandatory:", SYMBOLICNAME,
			"bsn");

		verifyManifestFirst();
		verifyActivator();
		verifyActivationPolicy();
		verifyComponent();
		verifyNative();
		verifyImports();
		verifyExports();
		verifyUnresolvedReferences();
		verifySymbolicName();
		verifyListHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, EENAME, false);
		verifyHeader(Constants.BUNDLE_MANIFESTVERSION, BUNDLEMANIFESTVERSION, false);
		verifyHeader(Constants.BUNDLE_VERSION, VERSION, true);
		verifyListHeader(Constants.BUNDLE_CLASSPATH, FILE, false);
		verifyDynamicImportPackage();
		verifyBundleClasspath();
		verifyUses();
		if (usesRequire) {
			if (!getErrors().isEmpty()) {
				getWarnings().add(0,
					"Bundle uses Require Bundle, this can generate false errors because then not enough information is available without the required bundles");
			}
		}

		verifyRequirements();
		verifyCapabilities();
		verifyMetaPersistence();
		verifyPathNames();

		doVerifierPlugins();
	}

	private void doVerifierPlugins() {
		for (VerifierPlugin plugin : getPlugins(VerifierPlugin.class)) {
			try {
				Processor previous = beginHandleErrors(plugin.toString());
				try {
					plugin.verify(analyzer);
				} finally {
					endHandleErrors(previous);
				}
			} catch (Exception e) {
				e.printStackTrace(System.err);
				error("Verifier Plugin %s failed %s", plugin, e);
			}
		}
	}

	/**
	 * Verify of the path names in the JAR are valid on all OS's (mainly
	 * windows)
	 */
	void verifyPathNames() {
		if (!since(About._2_3))
			return;

		Set<String> invalidPaths = new HashSet<>();
		Pattern pattern = ReservedFileNames;
		setProperty("@", ReservedFileNames.pattern());
		String p = getProperty(INVALIDFILENAMES);
		unsetProperty("@");
		if (p != null) {
			try {
				pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);
			} catch (Exception e) {
				SetLocation error = exception(e, "%s is not a valid regular expression %s: %s", INVALIDFILENAMES, e, p);
				error.context(p)
					.header(INVALIDFILENAMES);
				return;
			}
		}

		Set<String> segments = new HashSet<>();
		for (String path : dot.getResources()
			.keySet()) {
			String parts[] = path.split("/");
			for (String part : parts) {
				if (segments.add(part) && pattern.matcher(part)
					.matches()) {
					invalidPaths.add(path);
				}
			}
		}

		if (invalidPaths.isEmpty())
			return;

		error(
			"Invalid file/directory names for Windows in JAR: %s. You can set the regular expression used with %s, the default expression is %s",
			invalidPaths, INVALIDFILENAMES, ReservedFileNames.pattern());
	}

	/**
	 * Verify that the imports properly use version ranges.
	 */
	private void verifyImports() {
		if (isStrict()) {
			Parameters map = parseHeader(manifest.getMainAttributes()
				.getValue(Constants.IMPORT_PACKAGE));
			Set<String> noimports = new HashSet<>();
			Set<String> toobroadimports = new HashSet<>();

			for (Entry<String, Attrs> e : map.entrySet()) {

				String key = Processor.removeDuplicateMarker(e.getKey());

				if (!isFQN(key)) {
					Location location = warning("Import-Package '%s' is not a valid package name, it must be an FQN",
						showUnicode(key)).location();
					location.header = Constants.IMPORT_PACKAGE;
					location.context = key;
				}

				String version = e.getValue()
					.get(Constants.VERSION_ATTRIBUTE);
				if (version == null) {
					if (!key.startsWith("javax.")) {
						noimports.add(key);
					}
				} else {
					if (!VERSIONRANGE.matcher(version)
						.matches()) {
						Location location = error("Import Package %s has an invalid version range syntax %s", key,
							version).location();
						location.header = Constants.IMPORT_PACKAGE;
						location.context = key;
					} else {
						try {
							VersionRange range = new VersionRange(version);
							if (!range.isRange()) {
								toobroadimports.add(key);
							}
							if (range.includeHigh() == false && range.includeLow() == false && range.getLow()
								.equals(range.getHigh())) {
								Location location = error(
									"Import Package %s has an empty version range syntax %s, likely want to use [%s,%s]",
									key, version, range.getLow(), range.getHigh()).location();
								location.header = Constants.IMPORT_PACKAGE;
								location.context = key;
							}
							// TODO check for exclude low, include high?
						} catch (Exception ee) {
							Location location = exception(ee,
								"Import Package %s has an invalid version range syntax %s: %s", key, version, ee)
									.location();
							location.header = Constants.IMPORT_PACKAGE;
							location.context = key;
						}
					}
				}
			}

			if (!noimports.isEmpty()) {
				Location location = error("Import Package clauses without version range (excluding javax.*): %s",
					noimports).location();
				location.header = Constants.IMPORT_PACKAGE;
			}
			if (!toobroadimports.isEmpty()) {
				Location location = error(
					"Import Package clauses which use a version instead of a version range. This imports EVERY later package and not as many expect until the next major number: %s",
					toobroadimports).location();
				location.header = Constants.IMPORT_PACKAGE;
			}
		}
	}

	/**
	 * Verify that the exports only use versions.
	 */
	private void verifyExports() {
		if (isStrict()) {
			Parameters map = parseHeader(manifest.getMainAttributes()
				.getValue(Constants.EXPORT_PACKAGE));
			Set<String> noexports = new HashSet<>();

			for (Entry<String, Attrs> e : map.entrySet()) {

				if (!analyzer.getContained()
					.containsFQN(e.getKey())) {
					SetLocation warning = warning("Export-Package or -exportcontents refers to missing package '%s'",
						e.getKey());
					warning.header(Constants.EXPORT_PACKAGE + "|" + Constants.EXPORT_CONTENTS);
					warning.context(e.getKey());
				}

				String version = e.getValue()
					.get(Constants.VERSION_ATTRIBUTE);
				if (version == null) {
					noexports.add(e.getKey());
				} else {
					if (!VERSION.matcher(version)
						.matches()) {
						Location location;
						if (VERSIONRANGE.matcher(version)
							.matches()) {
							location = error(
								"Export Package %s version is a range: %s; Exports do not allow for ranges.",
								e.getKey(), version).location();
						} else {
							location = error("Export Package %s version has invalid syntax: %s", e.getKey(), version)
								.location();
						}
						location.header = Constants.EXPORT_PACKAGE;
						location.context = e.getKey();
					}
				}

				if (e.getValue()
					.containsKey(Constants.SPECIFICATION_VERSION)) {
					Location location = error(
						"Export Package %s uses deprecated specification-version instead of version", e.getKey())
							.location();
					location.header = Constants.EXPORT_PACKAGE;
					location.context = e.getKey();
				}

				String mandatory = e.getValue()
					.get(Constants.MANDATORY_DIRECTIVE);
				if (mandatory != null) {
					Set<String> missing = new HashSet<>(split(mandatory));
					missing.removeAll(e.getValue()
						.keySet());
					if (!missing.isEmpty()) {
						Location location = error("Export Package %s misses mandatory attribute: %s", e.getKey(),
							missing).location();
						location.header = Constants.EXPORT_PACKAGE;
						location.context = e.getKey();
					}
				}
			}

			if (!noexports.isEmpty()) {
				Location location = error("Export Package clauses without version range: %s", noexports).location();
				location.header = Constants.EXPORT_PACKAGE;
			}
		}
	}

	private Object showUnicode(String key) {
		try (Formatter sb = new Formatter()) {
			for (int i = 0; i < key.length(); i++) {
				char c = key.charAt(i);
				if (c < 0x20 || c >= 0x7E) {
					sb.format("\\u%04X", (0xFFFF & c));
				} else {
					sb.format("%c", c);
				}
			}
			return sb.toString();
		}
	}

	private void verifyRequirements() throws IllegalArgumentException, Exception {
		Parameters map = parseHeader(manifest.getMainAttributes()
			.getValue(Constants.REQUIRE_CAPABILITY));
		for (String key : map.keySet()) {
			Attrs attrs = map.get(key);
			key = Processor.removeDuplicateMarker(key);
			verifyNamespace(key, "Require");

			verify(attrs, "filter:", FILTERPATTERN, false, "Requirement %s filter not correct", key);

			String filter = attrs.get("filter:");
			if (filter != null) {
				String verify = new Filter(filter).verify();
				if (verify != null)
					error("Invalid filter syntax in requirement %s=%s. Reason %s", key, attrs, verify);
			}
			verify(attrs, "cardinality:", CARDINALITY_PATTERN, false, "Requirement %s cardinality not correct", key);
			verify(attrs, "resolution:", RESOLUTION_PATTERN, false, "Requirement %s resolution not correct", key);

			if (key.equals("osgi.extender")) {
				// No requirements on extender
			} else if (key.equals("osgi.serviceloader")) {

			} else if (key.equals("osgi.contract")) {

			} else if (key.equals("osgi.service")) {

			} else if (key.equals("osgi.ee")) {

			} else if (key.equals("osgi.native")) {

			} else if (key.equals("osgi.identity")) {

			} else if (key.startsWith("osgi.wiring.")) {
				error("%s namespace must not be specified with generic requirements", key);
			}

			verifyAttrs(key, attrs);

			if (attrs.containsKey("mandatory:"))
				error("%s directive is intended for Capabilities, not Requirement %s", "mandatory:", key);
			if (attrs.containsKey("uses:"))
				error("%s directive is intended for Capabilities, not Requirement %s", "uses:", key);
		}
	}

	/**
	 * @param attrs
	 */
	void verifyAttrs(String key, Attrs attrs) {
		for (String a : attrs.keySet()) {
			String v = attrs.get(a);

			if (!a.endsWith(":")) {
				Attrs.Type t = attrs.getType(a);
				if ("version".equals(a)) {
					if (t != Attrs.Type.VERSION && t != Attrs.Type.VERSIONS)
						error(
							"Version attributes should always be of type Version or List<Version>, it is version:%s=%s for %s",
							t, v, key);
				} else
					verifyType(t, v);
			}
		}
	}

	private void verifyCapabilities() {
		Parameters map = parseHeader(manifest.getMainAttributes()
			.getValue(Constants.PROVIDE_CAPABILITY));
		for (String key : map.keySet()) {
			Attrs attrs = map.get(key);
			key = Processor.removeDuplicateMarker(key);
			verifyNamespace(key, "Provide");
			verify(attrs, "cardinality:", CARDINALITY_PATTERN, false, "Capability %s cardinality not correct", key);
			verify(attrs, "resolution:", RESOLUTION_PATTERN, false, "Capability %s resolution not correct", key);

			if (key.equals("osgi.extender")) {
				verify(attrs, "osgi.extender", SYMBOLICNAME, true, "%s must have the %s attribute set", key,
					"osgi.extender");
				verify(attrs, "version", VERSION, true, "%s must have the %s attribute set", key, "version");
			} else if (key.equals("osgi.serviceloader")) {
				verify(attrs, "register:", PACKAGEPATTERN_OR_EMPTY, false,
					"Service Loader extender register: directive not a fully qualified Java name");
			} else if (key.equals("osgi.contract")) {
				verify(attrs, "osgi.contract", SYMBOLICNAME, true, "%s must have the %s attribute set", key,
					"osgi.contract");
			} else if (key.equals("osgi.service")) {
				verify(attrs, "objectClass", MULTIPACKAGEPATTERN, true, "%s must have the %s attribute set", key,
					"objectClass");
			} else if (key.startsWith("osgi.wiring.") || key.equals("osgi.identity") || key.equals("osgi.ee")
				|| key.equals("osgi.native")) {
				error("%s namespace must not be specified with generic capabilities", key);
			}

			verifyAttrs(key, attrs);

			if (attrs.containsKey("filter:"))
				error("%s directive is intended for Requirements, not Capability %s", "filter:", key);
			if (attrs.containsKey("cardinality:"))
				error("%s directive is intended for Requirements, not Capability %s", "cardinality:", key);
			if (attrs.containsKey("resolution:"))
				error("%s directive is intended for Requirements, not Capability %s", "resolution:", key);
		}
	}

	private void verifyNamespace(String ns, String type) {
		if (!isBsn(ns)) {
			error("The %s-Capability with namespace %s is not a symbolic name", type, ns);
		}
	}

	private void verify(Attrs attrs, String ad, Pattern pattern, boolean mandatory, String msg, String... args) {
		String v = attrs.get(ad);
		if (v == null) {
			if (mandatory)
				error("Missing required attribute/directive %s", ad);
		} else {
			Matcher m = pattern.matcher(v);
			if (!m.matches())
				error(msg, (Object[]) args);
		}
	}

	private void verifyType(@SuppressWarnings("unused") Attrs.Type type, @SuppressWarnings("unused") String string) {

	}

	/**
	 * Verify if the header does not contain any other directives
	 *
	 * @param header
	 * @param directives
	 * @param namePattern
	 * @param type
	 * @throws Exception
	 */
	private void verifyDirectives(String header, String directives, Pattern namePattern, String type) throws Exception {
		verifyDirectives(header, namespace -> directives, namePattern, type);
	}

	/**
	 * Verify if the header does not contain any other directives
	 *
	 * @param header
	 * @param directives
	 * @param namePattern
	 * @param type
	 * @throws Exception
	 */
	private void verifyDirectives(String header, Function<String, String> directives, Pattern namePattern, String type)
		throws Exception {
		Parameters map = parseHeader(manifest.getMainAttributes()
			.getValue(header));
		for (Entry<String, Attrs> entry : map.entrySet()) {
			String pname = removeDuplicateMarker(entry.getKey());

			Pattern pattern = Pattern.compile(directives.apply(pname));

			if (namePattern != null) {
				if (!namePattern.matcher(pname)
					.matches()) {
					SetLocation l;
					if (isPedantic())
						l = error("Invalid %s name: '%s' in %s", type, showUnicode(pname), header);
					else
						l = warning("Invalid %s name: '%s' in %s", type, showUnicode(pname), header);

					Pattern hpat;
					if (Constants.EXPORT_PACKAGE.equals(header))
						hpat = Pattern.compile(Constants.EXPORT_PACKAGE + "|" + Constants.EXPORT_CONTENTS);
					else
						hpat = Pattern.compile(header);

					FileLine fileLine = analyzer.getHeader(hpat, Pattern.compile(pname, Pattern.LITERAL));
					fileLine.set(l);
					l.context(pname);
				}
			}

			for (String key : entry.getValue()
				.keySet()) {
				if (key.endsWith(":")) {
					if (!key.startsWith("x-")) {
						Matcher m = pattern.matcher(key);
						if (m.matches())
							continue;

						warning(
							"Unknown directive '%s' for namespace '%s' in '%s'. Allowed directives are [%s], and 'x-*'.",
							key, pname, header, pattern.toString()
								.replace('|', ','));
					}
				}
			}
		}
	}

	/**
	 * Verify the use clauses
	 */
	private void verifyUses() {
		// Set<String> uses = Create.set();
		// for ( Map<String,String> attrs : analyzer.getExports().values()) {
		// if ( attrs.containsKey(Constants.USES_DIRECTIVE)) {
		// String s = attrs.get(Constants.USES_DIRECTIVE);
		// uses.addAll( split(s));
		// }
		// }
		// uses.removeAll(analyzer.getExports().keySet());
		// uses.removeAll(analyzer.getImports().keySet());
		// if ( !uses.isEmpty())
		// warning(Constants.EXPORT_PACKAGE + " uses: directive contains
		// packages that are not imported nor exported: %s",
		// uses);
	}

	public boolean verifyActivationPolicy() {
		String policy = main.get(Constants.BUNDLE_ACTIVATIONPOLICY);
		if (policy == null)
			return true;

		return verifyActivationPolicy(policy);
	}

	public boolean verifyActivationPolicy(String policy) {
		Parameters map = parseHeader(policy);
		if (map.isEmpty())
			warning(Constants.BUNDLE_ACTIVATIONPOLICY + " is set but has no argument %s", policy);
		else if (map.size() > 1)
			warning(Constants.BUNDLE_ACTIVATIONPOLICY + " has too many arguments %s", policy);
		else {
			Map<String, String> s = map.get("lazy");
			if (s == null)
				warning(Constants.BUNDLE_ACTIVATIONPOLICY + " set but is not set to lazy: %s", policy);
			else
				return true;
		}

		return false;
	}

	public void verifyBundleClasspath() {
		Parameters bcp = main.getBundleClassPath();
		if (bcp.isEmpty() || bcp.containsKey(".") || bcp.containsKey("/"))
			return;

		for (String path : bcp.keySet()) {
			if (path.endsWith("/"))
				error("A " + Constants.BUNDLE_CLASSPATH + " entry must not end with '/': %s", path);

			if (dot.hasDirectory(path))
				// We assume that any classes are in a directory
				// and therefore do not care when the bundle is included
				return;
		}

		for (String path : dot.getResources()
			.keySet()) {
			if (path.endsWith(".class")) {
				warning("The " + Constants.BUNDLE_CLASSPATH
					+ " does not contain the actual bundle JAR (as specified with '.' in the "
					+ Constants.BUNDLE_CLASSPATH + ") but the JAR does contain classes. Is this intentional?");
				return;
			}
		}
	}

	/**
	 * <pre>
	 *          DynamicImport-Package ::= dynamic-description
	 *              ( ',' dynamic-description )*
	 *
	 *          dynamic-description::= wildcard-names ( ';' parameter )*
	 *          wildcard-names ::= wildcard-name ( ';' wildcard-name )*
	 *          wildcard-name ::= package-name
	 *                         | ( package-name '.*' ) // See 1.4.2
	 *                         | '*'
	 * </pre>
	 */
	private void verifyDynamicImportPackage() {
		verifyListHeader(Constants.DYNAMICIMPORT_PACKAGE, WILDCARDPACKAGE, true);
		String dynamicImportPackage = get(Constants.DYNAMICIMPORT_PACKAGE);
		if (dynamicImportPackage == null)
			return;

		Parameters map = main.getDynamicImportPackage();
		for (String name : map.keySet()) {
			name = name.trim();
			if (!verify(name, WILDCARDPACKAGE))
				error(Constants.DYNAMICIMPORT_PACKAGE + " header contains an invalid package name: %s", name);

			Map<String, String> sub = map.get(name);
			if (r3 && sub.size() != 0) {
				error(
					"DynamicPackage-Import has attributes on import: %s. This is however, an <=R3 bundle and attributes on this header were introduced in R4.",
					name);
			}
		}
	}

	private void verifyManifestFirst() {
		if (!dot.isManifestFirst()) {
			error("Invalid JAR stream: Manifest should come first to be compatible with JarInputStream, it was not");
		}
	}

	private void verifySymbolicName() {
		Parameters bsn = parseHeader(main.get(Constants.BUNDLE_SYMBOLICNAME));
		if (!bsn.isEmpty()) {
			if (bsn.size() > 1)
				error("More than one BSN specified %s", bsn);

			String name = bsn.keySet()
				.iterator()
				.next();
			if (!isBsn(name)) {
				error("Symbolic Name has invalid format: %s", name);
			}
		}
	}

	/**
	 * @param name the {@code String} to test
	 * @return {@code true} if the given {@code name} matches a Bundle Symbolic
	 *         Name, otherwise {@code false}
	 */
	public static boolean isBsn(String name) {
		return SYMBOLICNAME.matcher(name)
			.matches();
	}

	/**
	 * <pre>
	 *         filter ::= ’(’ filter-comp ’)’
	 *         filter-comp ::= and | or | not | operation
	 *         and ::= ’&amp;’ filter-list
	 *         or ::= ’|’ filter-list
	 *         not ::= ’!’ filter
	 *         filter-list ::= filter | filter filter-list
	 *         operation ::= simple | present | substring
	 *         simple ::= attr filter-type value
	 *         filter-type ::= equal | approx | greater | less
	 *         equal ::= ’=’
	 *         approx ::= ’&tilde;=’
	 *         greater ::= ’&gt;=’
	 *         less ::= ’&lt;=’
	 *         present ::= attr ’=*’
	 *         substring ::= attr ’=’ initial any final
	 *         inital ::= () | value
	 *         any ::= ’*’ star-value
	 *         star-value ::= () | value ’*’ star-value
	 *         final ::= () | value
	 *         value ::= &lt;see text&gt;
	 * </pre>
	 *
	 * @param expr the {@code String} to test
	 * @param index the index within {@code expr} to start with
	 * @return the index of the last character within {@code expr} that was
	 *         evaluated
	 */

	public static int verifyFilter(String expr, int index) {
		try {
			while (Character.isWhitespace(expr.charAt(index)))
				index++;

			if (expr.charAt(index) != '(')
				throw new IllegalArgumentException("Filter mismatch: expected ( at position " + index + " : " + expr);

			index++; // skip (

			while (Character.isWhitespace(expr.charAt(index)))
				index++;

			switch (expr.charAt(index)) {
				case '!' :
					index++; // skip !
					while (Character.isWhitespace(expr.charAt(index)))
						index++;

					if (expr.charAt(index) != '(')
						throw new IllegalArgumentException(
							"Filter mismatch: ! (not) must have one sub expression " + index + " : " + expr);
					while (Character.isWhitespace(expr.charAt(index)))
						index++;

					index = verifyFilter(expr, index);
					while (Character.isWhitespace(expr.charAt(index)))
						index++;
					if (expr.charAt(index) != ')')
						throw new IllegalArgumentException(
							"Filter mismatch: expected ) at position " + index + " : " + expr);
					return index + 1;

				case '&' :
				case '|' :
					index++; // skip operator
					while (Character.isWhitespace(expr.charAt(index)))
						index++;
					while (expr.charAt(index) == '(') {
						index = verifyFilter(expr, index);
						while (Character.isWhitespace(expr.charAt(index)))
							index++;
					}

					if (expr.charAt(index) != ')')
						throw new IllegalArgumentException(
							"Filter mismatch: expected ) at position " + index + " : " + expr);
					return index + 1; // skip )

				default :
					index = verifyFilterOperation(expr, index);
					if (expr.charAt(index) != ')')
						throw new IllegalArgumentException(
							"Filter mismatch: expected ) at position " + index + " : " + expr);
					return index + 1;
			}
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException("Filter mismatch: early EOF from " + index);
		}
	}

	static private int verifyFilterOperation(String expr, int index) {
		StringBuilder sb = new StringBuilder();
		while ("=><~()".indexOf(expr.charAt(index)) < 0) {
			sb.append(expr.charAt(index++));
		}
		String attr = sb.toString()
			.trim();
		if (attr.length() == 0)
			throw new IllegalArgumentException("Filter mismatch: attr at index " + index + " is 0");
		sb = new StringBuilder();
		while ("=><~".indexOf(expr.charAt(index)) >= 0) {
			sb.append(expr.charAt(index++));
		}
		String operator = sb.toString();
		if (!verify(operator, FILTEROP))
			throw new IllegalArgumentException("Filter error, illegal operator " + operator + " at index " + index);

		sb = new StringBuilder();
		while (")".indexOf(expr.charAt(index)) < 0) {
			switch (expr.charAt(index)) {
				case '\\' :
					if ("\\)(*".indexOf(expr.charAt(index + 1)) >= 0)
						index++;
					else
						throw new IllegalArgumentException("Filter error, illegal use of backslash at index " + index
							+ ". Backslash may only be used before * or () or \\");
			}
			sb.append(expr.charAt(index++));
		}
		return index;
	}

	private boolean verifyHeader(String name, Pattern regex, boolean error) {
		String value = manifest.getMainAttributes()
			.getValue(name);
		if (value == null)
			return false;

		QuotedTokenizer st = new QuotedTokenizer(value.trim(), ",");
		for (Iterator<String> i = st.getTokenSet()
			.iterator(); i.hasNext();) {
			if (!verify(i.next(), regex)) {
				if (error)
					error("Invalid value for %s, %s does not match %s", name, value, regex.pattern());
				else
					warning("Invalid value for %s, %s does not match %s", name, value, regex.pattern());
			}
		}
		return true;
	}

	static private boolean verify(String value, Pattern regex) {
		return regex.matcher(value)
			.matches();
	}

	private boolean verifyListHeader(String name, Pattern regex, boolean error) {
		String value = manifest.getMainAttributes()
			.getValue(name);
		if (value == null)
			return false;

		Parameters map = parseHeader(value);
		for (String header : map.keySet()) {
			if (!regex.matcher(header)
				.matches()) {
				if (error)
					error("Invalid value for %s, %s does not match %s", name, value, regex.pattern());
				else
					warning("Invalid value for %s, %s does not match %s", name, value, regex.pattern());
			}
		}
		return true;
	}

	// @Override
	// public String getProperty(String key, String deflt) {
	// if (properties == null)
	// return deflt;
	// return properties.getProperty(key, deflt);
	// }

	public static boolean isVersion(String version) {
		return version != null && VERSION.matcher(version)
			.matches();
	}

	public static boolean isIdentifier(String value) {
		if (value.length() < 1)
			return false;

		if (!Character.isJavaIdentifierStart(value.charAt(0)))
			return false;

		for (int i = 1; i < value.length(); i++) {
			if (!Character.isJavaIdentifierPart(value.charAt(i)))
				return false;
		}
		return true;
	}

	public static boolean isMember(String value, String[] matches) {
		for (String match : matches) {
			if (match.equals(value))
				return true;
		}
		return false;
	}

	public static boolean isFQN(String name) {
		if (name.length() == 0)
			return false;

		boolean start = true;

		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);

			if (start) {
				if (!Character.isJavaIdentifierStart(c))
					return false;
				start = false;
			} else {
				if (Character.isJavaIdentifierPart(c) || c == '$')
					continue;

				if (c == '.') {
					start = true;
				} else
					return false;
			}
		}

		return true;
	}

	/**
	 * Verify checksums
	 */
	/**
	 * Verify the checksums from the manifest against the real thing.
	 *
	 * @param all {@code true} if each resource must be digested, otherwise
	 *            {@code false}
	 * @throws Exception
	 */
	public void verifyChecksums(boolean all) throws Exception {
		Manifest m = dot.getManifest();
		if (m == null || m.getEntries()
			.isEmpty()) {
			if (all)
				error("Verify checksums with all but no digests");
			return;
		}

		List<String> missingDigest = new ArrayList<>();

		for (String path : dot.getResources()
			.keySet()) {
			if (path.equals("META-INF/MANIFEST.MF"))
				continue;

			Attributes a = m.getAttributes(path);
			String digest = a.getValue("SHA1-Digest");
			if (digest == null) {
				if (!path.matches(""))
					missingDigest.add(path);
			} else {
				byte[] d = Base64.decodeBase64(digest);
				SHA1 expected = new SHA1(d);
				try (Digester<SHA1> digester = SHA1.getDigester();
					InputStream in = dot.getResource(path)
						.openInputStream()) {
					IO.copy(in, digester);
					digester.digest();
					if (!expected.equals(digester.digest())) {
						error("Checksum mismatch %s, expected %s, got %s", path, expected, digester.digest());
					}
				}
			}
		}
		if (missingDigest.size() > 0) {
			error("Entries in the manifest are missing digests: %s", missingDigest);
		}
	}

	/**
	 * Verify the EXTENDED_S syntax
	 *
	 * @param key the {@code String} to test
	 * @return {@code true} if the given {@code String} matches the EXTENDED_S
	 *         syntax, otherwise {@code false}
	 */
	public static boolean isExtended(String key) {
		if (key == null)
			return false;

		return EXTENDED_P.matcher(key)
			.matches();
	}

	/**
	 * Verify the ARGUMENT_S syntax
	 *
	 * @param arg the {@code String} to test
	 * @return {@code true} if the given {@code String} matches the ARGUMENT_S
	 *         syntax, otherwise {@code false}
	 */
	public static boolean isArgument(String arg) {
		return arg != null && ARGUMENT_P.matcher(arg)
			.matches();
	}

	/**
	 * Verify the QUOTEDSTRING syntax
	 *
	 * @param s the {@code String} to test
	 * @return {@code true} if the given {@code String} matches the QUOTEDSTRING
	 *         syntax, otherwise {@code false}
	 */
	public static boolean isQuotedString(String s) {
		return s != null && QUOTEDSTRING_P.matcher(s)
			.matches();
	}

	/**
	 * Verify the VERSION_RANGE_S syntax
	 *
	 * @param range the {@code String} to test
	 * @return {@code true} if the given {@code String} matches the
	 *         VERSION_RANGE_S syntax, otherwise {@code false}
	 */
	public static boolean isVersionRange(String range) {
		return range != null && VERSIONRANGE_P.matcher(range)
			.matches();
	}

	/**
	 * Verify the Meta-Persistence header
	 *
	 * @throws Exception
	 */

	public void verifyMetaPersistence() throws Exception {
		List<String> list = new ArrayList<>();
		String mp = dot.getManifest()
			.getMainAttributes()
			.getValue(META_PERSISTENCE);
		for (String location : OSGiHeader.parseHeader(mp)
			.keySet()) {
			String[] parts = location.split("!/");

			Resource resource = dot.getResource(parts[0]);
			if (resource == null)
				list.add(location);
			else {
				if (parts.length > 1) {
					try (Jar jar = new Jar("", resource.openInputStream())) {
						if (jar.getResource(parts[1]) == null)
							list.add(location);
					} catch (Exception e) {
						list.add(location);
					}
				}
			}
		}
		if (list.isEmpty())
			return;

		error(Constants.META_PERSISTENCE + " refers to resources not in the bundle: %s", list)
			.header(Constants.META_PERSISTENCE);
	}

	/**
	 * @return the frombuilder
	 */
	public boolean isFrombuilder() {
		return frombuilder;
	}

	/**
	 * @param frombuilder the frombuilder to set
	 */
	public void setFrombuilder(boolean frombuilder) {
		this.frombuilder = frombuilder;
	}

	public static boolean isNumber(String number) {
		return NUMBERPATTERN.matcher(number)
			.matches();
	}
}

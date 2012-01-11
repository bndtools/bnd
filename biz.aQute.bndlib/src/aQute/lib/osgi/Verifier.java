package aQute.lib.osgi;

import java.util.*;
import java.util.jar.*;
import java.util.regex.*;

import aQute.lib.osgi.Descriptors.PackageRef;
import aQute.lib.osgi.Descriptors.TypeRef;
import aQute.libg.generics.*;
import aQute.libg.header.*;
import aQute.libg.qtokens.*;

public class Verifier extends Processor {

	final Jar								dot;
	final Manifest							manifest;
	final Attributes						main;
	final Map<String, Map<String, String>>	ignore	= newHashMap();

	// Map<String, Map<String, String>> referred = newHashMap();
	// Map<String, Map<String, String>> contained = newHashMap();
	// Map<String, Set<String>> uses = newHashMap();
	// Map<String, Map<String, String>> mimports;
	// Map<String, Map<String, String>> mdynimports;
	// Map<String, Map<String, String>> mexports;
	// List<Jar> bundleClasspath;
	// // to
	// // ignore
	//
	// Map<TypeRef, Clazz> classSpace;
	boolean									r3;
	boolean									usesRequire;
	boolean									fragment;

	final static Pattern					EENAME	= Pattern.compile("CDC-1\\.0/Foundation-1\\.0"
															+ "|CDC-1\\.1/Foundation-1\\.1"
															+ "|OSGi/Minimum-1\\.[1-9]"
															+ "|JRE-1\\.1" + "|J2SE-1\\.2"
															+ "|J2SE-1\\.3" + "|J2SE-1\\.4"
															+ "|J2SE-1\\.5" + "|JavaSE-1\\.6"
															+ "|JavaSE-1\\.7"
															+ "|PersonalJava-1\\.1"
															+ "|PersonalJava-1\\.2"
															+ "|CDC-1\\.0/PersonalBasis-1\\.0"
															+ "|CDC-1\\.0/PersonalJava-1\\.0");

	final static int						V1_1	= 45;
	final static int						V1_2	= 46;
	final static int						V1_3	= 47;
	final static int						V1_4	= 48;
	final static int						V1_5	= 49;
	final static int						V1_6	= 50;
	final static int						V1_7	= 51;
	final static int						V1_8	= 52;

	static class EE {
		String	name;
		int		target;

		EE(String name, int source, int target) {
			this.name = name;
			this.target = target;
		}
	}

	final static EE[]				ees								= {
			new EE("CDC-1.0/Foundation-1.0", V1_3, V1_1),
			new EE("CDC-1.1/Foundation-1.1", V1_3, V1_2),
			new EE("OSGi/Minimum-1.0", V1_3, V1_1),
			new EE("OSGi/Minimum-1.1", V1_3, V1_2),
			new EE("JRE-1.1", V1_1, V1_1), //
			new EE("J2SE-1.2", V1_2, V1_1), //
			new EE("J2SE-1.3", V1_3, V1_1), //
			new EE("J2SE-1.4", V1_3, V1_2), //
			new EE("J2SE-1.5", V1_5, V1_5), //
			new EE("JavaSE-1.6", V1_6, V1_6), //
			new EE("PersonalJava-1.1", V1_1, V1_1), //
			new EE("JavaSE-1.7", V1_7, V1_7), //
			new EE("PersonalJava-1.1", V1_1, V1_1), //
			new EE("PersonalJava-1.2", V1_1, V1_1),
			new EE("CDC-1.0/PersonalBasis-1.0", V1_3, V1_1),
			new EE("CDC-1.0/PersonalJava-1.0", V1_3, V1_1),
			new EE("CDC-1.1/PersonalBasis-1.1", V1_3, V1_2),
			new EE("CDC-1.1/PersonalJava-1.1", V1_3, V1_2)			};

	final static Pattern			BUNDLEMANIFESTVERSION			= Pattern.compile("2");
	public final static String		SYMBOLICNAME_STRING				= "[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*";
	public final static Pattern		SYMBOLICNAME					= Pattern
																			.compile(SYMBOLICNAME_STRING);

	public final static String		VERSION_STRING					= "[0-9]+(\\.[0-9]+(\\.[0-9]+(\\.[0-9A-Za-z_-]+)?)?)?";
	public final static Pattern		VERSION							= Pattern
																			.compile(VERSION_STRING);
	final static Pattern			FILTEROP						= Pattern.compile("=|<=|>=|~=");
	public final static Pattern		VERSIONRANGE					= Pattern.compile("((\\(|\\[)"
																			+ VERSION_STRING + ","
																			+ VERSION_STRING
																			+ "(\\]|\\)))|"
																			+ VERSION_STRING);
	final static Pattern			FILE							= Pattern
																			.compile("/?[^/\"\n\r\u0000]+(/[^/\"\n\r\u0000]+)*");
	final static Pattern			WILDCARDPACKAGE					= Pattern
																			.compile("((\\p{Alnum}|_)+(\\.(\\p{Alnum}|_)+)*(\\.\\*)?)|\\*");
	public final static Pattern		ISO639							= Pattern.compile("[A-Z][A-Z]");
	public final static Pattern		HEADER_PATTERN					= Pattern
																			.compile("[A-Za-z0-9][-a-zA-Z0-9_]+");
	public final static Pattern		TOKEN							= Pattern
																			.compile("[-a-zA-Z0-9_]+");

	public final static Pattern		NUMBERPATTERN					= Pattern.compile("\\d+");
	public final static Pattern		PACKAGEPATTERN					= Pattern
																			.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");
	public final static Pattern		PATHPATTERN						= Pattern.compile(".*");
	public final static Pattern		FQNPATTERN						= Pattern.compile(".*");
	public final static Pattern		URLPATTERN						= Pattern.compile(".*");
	public final static Pattern		ANYPATTERN						= Pattern.compile(".*");
	public final static Pattern		FILTERPATTERN					= Pattern.compile(".*");
	public final static Pattern		TRUEORFALSEPATTERN				= Pattern
																			.compile("true|false|TRUE|FALSE");
	public static final Pattern		WILDCARDNAMEPATTERN				= Pattern.compile(".*");
	public static final Pattern		BUNDLE_ACTIVATIONPOLICYPATTERN	= Pattern.compile("lazy");

	public final static String		EES[]							= { "CDC-1.0/Foundation-1.0",
			"CDC-1.1/Foundation-1.1", "OSGi/Minimum-1.0", "OSGi/Minimum-1.1", "OSGi/Minimum-1.2",
			"JRE-1.1", "J2SE-1.2", "J2SE-1.3", "J2SE-1.4", "J2SE-1.5", "JavaSE-1.6", "JavaSE-1.7",
			"PersonalJava-1.1", "PersonalJava-1.2", "CDC-1.0/PersonalBasis-1.0",
			"CDC-1.0/PersonalJava-1.0"								};

	public final static String		OSNAMES[]						= { "AIX", // IBM
			"DigitalUnix", // Compaq
			"Embos", // Segger Embedded Software Solutions
			"Epoc32", // SymbianOS Symbian OS
			"FreeBSD", // Free BSD
			"HPUX", // hp-ux Hewlett Packard
			"IRIX", // Silicon Graphics
			"Linux", // Open source
			"MacOS", // Apple
			"NetBSD", // Open source
			"Netware", // Novell
			"OpenBSD", // Open source
			"OS2", // OS/2 IBM
			"QNX", // procnto QNX
			"Solaris", // Sun (almost an alias of SunOS)
			"SunOS", // Sun Microsystems
			"VxWorks", // WindRiver Systems
			"Windows95", "Win32", "Windows98", "WindowsNT", "WindowsCE", "Windows2000", // Win2000
			"Windows2003", // Win2003
			"WindowsXP", "WindowsVista",							};

	public final static String		PROCESSORNAMES[]				= { //
																	//
			"68k", // Motorola 68000
			"ARM_LE", // Intel Strong ARM. Deprecated because it does not
			// specify the endianness. See the following two rows.
			"arm_le", // Intel Strong ARM Little Endian mode
			"arm_be", // Intel String ARM Big Endian mode
			"Alpha", //
			"ia64n",// Hewlett Packard 32 bit
			"ia64w",// Hewlett Packard 64 bit mode
			"Ignite", // psc1k PTSC
			"Mips", // SGI
			"PArisc", // Hewlett Packard
			"PowerPC", // power ppc Motorola/IBM Power PC
			"Sh4", // Hitachi
			"Sparc", // SUN
			"Sparcv9", // SUN
			"S390", // IBM Mainframe 31 bit
			"S390x", // IBM Mainframe 64-bit
			"V850E", // NEC V850E
			"x86", // pentium i386
			"i486", // i586 i686 Intel& AMD 32 bit
			"x86-64",												};

	final Analyzer					analyzer;
	private Collection<Instruction>	dynamicImports;

	public Verifier(Jar jar) throws Exception {
		this.analyzer = new Analyzer();
		this.analyzer.setJar(jar);
		this.manifest = this.analyzer.calcManifest();
		this.main = manifest.getMainAttributes();
		this.dot = jar;
		getInfo(analyzer);
	}

	public Verifier(Analyzer analyzer) throws Exception {
		this.analyzer = analyzer;
		this.dot = analyzer.getJar();
		this.manifest = dot.getManifest();
		this.main = manifest.getMainAttributes();
	}

	private void verifyHeaders() {
		for (Object element : main.keySet()) {
			Attributes.Name header = (Attributes.Name) element;
			String h = header.toString();
			if (!HEADER_PATTERN.matcher(h).matches())
				error("Invalid Manifest header: " + h + ", pattern=" + HEADER_PATTERN);
		}
	}

	/*
	 * Bundle-NativeCode ::= nativecode ( ',' nativecode )* ( ’,’ optional) ?
	 * nativecode ::= path ( ';' path )* // See 1.4.2 ( ';' parameter )+
	 * optional ::= ’*’
	 */
	public void verifyNative() {
		String nc = getLocalHeader("Bundle-NativeCode");
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
						error("Can not parse name from bundle native code header: " + nc);
						return;
					}
					del = qt.getSeparator();
					if (del == ';') {
						if (dot != null && !dot.exists(name)) {
							error("Native library not found in JAR: " + name);
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
								error("Bundle-Native code header may only END in wildcard: nc");
						} else {
							warning("Unknown attribute in native code: " + name + "=" + value);
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

		error(s);
		return false;
	}

	public static String validateFilter(String value) {
		try {
			verifyFilter(value, 0);
			return null;
		} catch (Exception e) {
			return "Not a valid filter: " + value + e.getMessage();
		}
	}

	private void verifyActivator() throws Exception {
		String bactivator = getLocalHeader("Bundle-Activator");
		if (bactivator != null) {
			TypeRef ref = analyzer.getTypeRefFromFQN(bactivator);
			if(analyzer.getClassspace().containsKey(ref))
				return;
			
			PackageRef packageRef = ref.getPackageRef();
			if (packageRef.isDefaultPackage())
				error("The Bundle Activator is not in the bundle and it is in the default package ");
			else if (!analyzer.isImported(packageRef)) {
				error("Bundle-Activator not found on the bundle class path nor in imports: "
						+ bactivator);
			}
		}
	}

	private void verifyComponent() {
		String serviceComponent = getLocalHeader("Service-Component");
		if (serviceComponent != null) {
			Map<String, Map<String, String>> map = parseHeader(serviceComponent);
			for (String component : map.keySet()) {
				if (component.indexOf("*") < 0 && !dot.exists(component)) {
					error("Service-Component entry can not be located in JAR: " + component);
				} else {
					// validate component ...
				}
			}
		}
	}

	/**
	 * Invalid exports are exports mentioned in the manifest but not found on
	 * the classpath. This can be calculated with: exports - contains.
	 * 
	 * Unfortunately, we also must take duplicate names into account. These
	 * duplicates are of course no erroneous.
	 */
	private void verifyInvalidExports() {
		
		Set<String> invalidExport = OSGiHeader.parseHeader( main.getValue(Constants.EXPORT_PACKAGE)).keySet();
		invalidExport.removeAll(analyzer.getContained().keySet());

		// We might have duplicate names that are marked for it. These
		// should not be counted. Should we test them against the contained
		// set? Hmm. If someone wants to hang himself by using duplicates than
		// I guess he can go ahead ... This is not a recommended practice
		for (Iterator<String> i = invalidExport.iterator(); i.hasNext();) {
			String pack = i.next();
			if (isDuplicate(pack)) {
				i.remove();
			}
		}

		if (!invalidExport.isEmpty())
			error("Exporting package %s that are not on the Bundle-Classpath", invalidExport,
					analyzer.getBundleClasspath().keySet());
	}

	/**
	 * Invalid imports are imports that we never refer to. They can be
	 * calculated by removing the referred packages from the imported packages.
	 * This leaves packages that the manifest imported but that we never use.
	 */
	private void verifyInvalidImports() {
		Set<String> invalidImport = newSet(analyzer.getImports().keySet());
		invalidImport.removeAll(analyzer.getReferred().keySet());
		// TODO Added this line but not sure why it worked before ...
		invalidImport.removeAll(analyzer.getContained().keySet());
		String bactivator = getLocalHeader(Analyzer.BUNDLE_ACTIVATOR);
		if (bactivator != null) {
			TypeRef ref = analyzer.getTypeRefFromFQN(bactivator);
			invalidImport.remove(ref.getPackageRef().getFQN());
		}
		if (isPedantic() && !invalidImport.isEmpty())
			warning("Importing packages %s that are never refered to by any class on the Bundle-Classpath: %s",
					invalidImport, analyzer.getBundleClasspath().keySet());
	}

	/**
	 * Check for unresolved imports. These are referrals that are not imported
	 * by the manifest and that are not part of our bundle class path. The are
	 * calculated by removing all the imported packages and contained from the
	 * referred packages.
	 */
	private void verifyUnresolvedReferences() {
		Set<String> unresolvedReferences = new TreeSet<String>(analyzer.getReferred().keySet());
		unresolvedReferences.removeAll(analyzer.getImports().keySet());
		unresolvedReferences.removeAll(analyzer.getContained().keySet());

		// Remove any java.** packages.
		for (Iterator<String> p = unresolvedReferences.iterator(); p.hasNext();) {
			String pack = p.next();
			if (pack.startsWith("java.") || ignore.containsKey(pack))
				p.remove();
			else {
				// Remove any dynamic imports
				if (isDynamicImport(pack))
					p.remove();
			}
		}

		if (!unresolvedReferences.isEmpty()) {
			// Now we want to know the
			// classes that are the culprits
			Set<String> culprits = new HashSet<String>();
			for (Clazz clazz : analyzer.getClassspace().values()) {
				if (hasOverlap(unresolvedReferences, clazz.getReferred()))
					culprits.add(clazz.getPath());
			}

			error("Unresolved references %s by class(es) %s on the Bundle-Classpath: %s",
					unresolvedReferences, culprits, analyzer.getBundleClasspath().keySet());
		}
	}

	/**
	 * @param p
	 * @param pack
	 */
	private boolean isDynamicImport(String pack) {
		if (dynamicImports == null)
			dynamicImports = Instruction.toInstruction(main
					.getValue(Constants.DYNAMICIMPORT_PACKAGE));

		return Instruction.matches(dynamicImports, pack);
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
		verifyDirectives("Export-Package", "uses:|mandatory:|include:|exclude:|" + IMPORT_DIRECTIVE);
		verifyDirectives("Import-Package", "resolution:");
		verifyDirectives("Require-Bundle", "visibility:|resolution:");
		verifyDirectives("Fragment-Host", "resolution:");
		verifyDirectives("Provide-Capability", "effective:|uses:");
		verifyDirectives("Require-Capability", "effective:|resolve:|filter:");
		verifyDirectives("Bundle-SymbolicName", "singleton:|fragment-attachment:|mandatory:");

		verifyManifestFirst();
		verifyActivator();
		verifyActivationPolicy();
		verifyComponent();
		verifyNative();
		verifyInvalidExports();
		verifyInvalidImports();
		verifyUnresolvedReferences();
		verifySymbolicName();
		verifyListHeader("Bundle-RequiredExecutionEnvironment", EENAME, false);
		verifyHeader("Bundle-ManifestVersion", BUNDLEMANIFESTVERSION, false);
		verifyHeader("Bundle-Version", VERSION, true);
		verifyListHeader("Bundle-Classpath", FILE, false);
		verifyDynamicImportPackage();
		verifyBundleClasspath();
		verifyUses();
		if (usesRequire) {
			if (!getErrors().isEmpty()) {
				getWarnings()
						.add(0,
								"Bundle uses Require Bundle, this can generate false errors because then not enough information is available without the required bundles");
			}
		}
	}

	/**
	 * Verify if the header does not contain any other directives
	 * 
	 * @param header
	 * @param directives
	 */
	private void verifyDirectives(String header, String directives) {
		Pattern pattern = Pattern.compile(directives);
		Map<String, Map<String, String>> map = parseHeader(manifest.getMainAttributes().getValue(
				header));
		for (Map.Entry<String, Map<String, String>> entry : map.entrySet()) {
			String pname = removeDuplicateMarker(entry.getKey());
			
			if (!PACKAGEPATTERN.matcher(pname).matches())
				if(isPedantic())
					error("Invalid package name: '%s'", pname);
				else
					warning("Invalid package name: '%s'", pname);

			for (String key : entry.getValue().keySet()) {
				if (key.endsWith(":")) {
					if (!key.startsWith("x-")) {
						Matcher m = pattern.matcher(key);
						if (m.matches())
							continue;

						warning("Unknown directive %s in %s, allowed directives are %s, and 'x-*'.",
								key, header, directives.replace('|', ','));
					}
				}
			}
		}
	}

	/**
	 * Verify the use clauses
	 */
	private void verifyUses() {
//		Set<String> uses = Create.set();
//		for ( Map<String,String> attrs : analyzer.getExports().values()) {
//			if ( attrs.containsKey(Constants.USES_DIRECTIVE)) {
//				String s = attrs.get(Constants.USES_DIRECTIVE);
//				uses.addAll( split(s));
//			}
//		}
//		uses.removeAll(analyzer.getExports().keySet());
//		uses.removeAll(analyzer.getImports().keySet());
//		if ( !uses.isEmpty()) 
//			warning("Export-Package uses: directive contains packages that are not imported nor exported: %s", uses);
	}

	public boolean verifyActivationPolicy() {
		String policy = getLocalHeader(Constants.BUNDLE_ACTIVATIONPOLICY);
		if (policy == null)
			return true;

		return verifyActivationPolicy(policy);
	}

	public boolean verifyActivationPolicy(String policy) {
		Map<String, Map<String, String>> map = parseHeader(policy);
		if (map.size() == 0)
			warning("Bundle-ActivationPolicy is set but has no argument %s", policy);
		else if (map.size() > 1)
			warning("Bundle-ActivationPolicy has too many arguments %s", policy);
		else {
			Map<String, String> s = map.get("lazy");
			if (s == null)
				warning("Bundle-ActivationPolicy set but is not set to lazy: %s", policy);
			else
				return true;
		}

		return false;
	}

	public void verifyBundleClasspath() {
		Map<String, Map<String, String>> bcp = parseHeader(getLocalHeader(Analyzer.BUNDLE_CLASSPATH));
		if (bcp.isEmpty() || bcp.containsKey("."))
			return;

		for (String path : bcp.keySet()) {
			if (path.endsWith("/"))
				error("A Bundle-ClassPath entry must not end with '/': %s", path);

			if (dot.getDirectories().containsKey(path))
				// We assume that any classes are in a directory
				// and therefore do not care when the bundle is included
				return;
		}

		for (String path : dot.getResources().keySet()) {
			if (path.endsWith(".class")) {
				warning("The Bundle-Classpath does not contain the actual bundle JAR (as specified with '.' in the Bundle-Classpath) but the JAR does contain classes. Is this intentional?");
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
		verifyListHeader("DynamicImport-Package", WILDCARDPACKAGE, true);
		String dynamicImportPackage = getLocalHeader("DynamicImport-Package");
		if (dynamicImportPackage == null)
			return;

		Map<String, Map<String, String>> map = parseHeader(dynamicImportPackage);
		for (String name : map.keySet()) {
			name = name.trim();
			if (!verify(name, WILDCARDPACKAGE))
				error("DynamicImport-Package header contains an invalid package name: " + name);

			Map<String, String> sub = map.get(name);
			if (r3 && sub.size() != 0) {
				error("DynamicPackage-Import has attributes on import: "
						+ name
						+ ". This is however, an <=R3 bundle and attributes on this header were introduced in R4. ");
			}
		}
	}

	private void verifyManifestFirst() {
		if (!dot.manifestFirst) {
			error("Invalid JAR stream: Manifest should come first to be compatible with JarInputStream, it was not");
		}
	}

	private void verifySymbolicName() {
		Map<String, Map<String, String>> bsn = parseHeader(getLocalHeader(Analyzer.BUNDLE_SYMBOLICNAME));
		if (!bsn.isEmpty()) {
			if (bsn.size() > 1)
				error("More than one BSN specified " + bsn);

			String name = (String) bsn.keySet().iterator().next();
			if (!isBsn(name)) {
				error("Symbolic Name has invalid format: " + name);
			}
		}
	}

	/**
	 * @param name
	 * @return
	 */
	public static boolean isBsn(String name) {
		return SYMBOLICNAME.matcher(name).matches();
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
	 * @param expr
	 * @param index
	 * @return
	 */

	public static int verifyFilter(String expr, int index) {
		try {
			while (Character.isWhitespace(expr.charAt(index)))
				index++;

			if (expr.charAt(index) != '(')
				throw new IllegalArgumentException("Filter mismatch: expected ( at position "
						+ index + " : " + expr);

			index++; // skip (

			while (Character.isWhitespace(expr.charAt(index)))
				index++;

			switch (expr.charAt(index)) {
			case '!':
				index++; // skip !
				while (Character.isWhitespace(expr.charAt(index)))
					index++;

				if (expr.charAt(index) != '(')
					throw new IllegalArgumentException(
							"Filter mismatch: ! (not) must have one sub expression " + index
									+ " : " + expr);
				while (Character.isWhitespace(expr.charAt(index)))
					index++;

				index = verifyFilter(expr, index);
				while (Character.isWhitespace(expr.charAt(index)))
					index++;
				if (expr.charAt(index) != ')')
					throw new IllegalArgumentException("Filter mismatch: expected ) at position "
							+ index + " : " + expr);
				return index + 1;

			case '&':
			case '|':
				index++; // skip operator
				while (Character.isWhitespace(expr.charAt(index)))
					index++;
				while (expr.charAt(index) == '(') {
					index = verifyFilter(expr, index);
					while (Character.isWhitespace(expr.charAt(index)))
						index++;
				}

				if (expr.charAt(index) != ')')
					throw new IllegalArgumentException("Filter mismatch: expected ) at position "
							+ index + " : " + expr);
				return index + 1; // skip )

			default:
				index = verifyFilterOperation(expr, index);
				if (expr.charAt(index) != ')')
					throw new IllegalArgumentException("Filter mismatch: expected ) at position "
							+ index + " : " + expr);
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
		String attr = sb.toString().trim();
		if (attr.length() == 0)
			throw new IllegalArgumentException("Filter mismatch: attr at index " + index + " is 0");
		sb = new StringBuilder();
		while ("=><~".indexOf(expr.charAt(index)) >= 0) {
			sb.append(expr.charAt(index++));
		}
		String operator = sb.toString();
		if (!verify(operator, FILTEROP))
			throw new IllegalArgumentException("Filter error, illegal operator " + operator
					+ " at index " + index);

		sb = new StringBuilder();
		while (")".indexOf(expr.charAt(index)) < 0) {
			switch (expr.charAt(index)) {
			case '\\':
				if ("\\)(*".indexOf(expr.charAt(index + 1)) >= 0)
					index++;
				else
					throw new IllegalArgumentException(
							"Filter error, illegal use of backslash at index " + index
									+ ". Backslash may only be used before * or () or \\");
			}
			sb.append(expr.charAt(index++));
		}
		return index;
	}

	private String getLocalHeader(String string) {
		return main.getValue(string);
	}

	private boolean verifyHeader(String name, Pattern regex, boolean error) {
		String value = manifest.getMainAttributes().getValue(name);
		if (value == null)
			return false;

		QuotedTokenizer st = new QuotedTokenizer(value.trim(), ",");
		for (Iterator<String> i = st.getTokenSet().iterator(); i.hasNext();) {
			if (!verify((String) i.next(), regex)) {
				String msg = "Invalid value for " + name + ", " + value + " does not match "
						+ regex.pattern();
				if (error)
					error(msg);
				else
					warning(msg);
			}
		}
		return true;
	}

	static private boolean verify(String value, Pattern regex) {
		return regex.matcher(value).matches();
	}

	private boolean verifyListHeader(String name, Pattern regex, boolean error) {
		String value = manifest.getMainAttributes().getValue(name);
		if (value == null)
			return false;

		Map<String, Map<String, String>> map = parseHeader(value);
		for (String header : map.keySet()) {
			if (!regex.matcher(header).matches()) {
				String msg = "Invalid value for " + name + ", " + value + " does not match "
						+ regex.pattern();
				if (error)
					error(msg);
				else
					warning(msg);
			}
		}
		return true;
	}

	public String getProperty(String key, String deflt) {
		if (properties == null)
			return deflt;
		return properties.getProperty(key, deflt);
	}

	public static boolean isVersion(String version) {
		return VERSION.matcher(version).matches();
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
		if (!Character.isJavaIdentifierStart(name.charAt(0)))
			return false;

		for (int i = 1; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isJavaIdentifierPart(c) || c == '$' || c == '.')
				continue;

			return false;
		}

		return true;
	}

}

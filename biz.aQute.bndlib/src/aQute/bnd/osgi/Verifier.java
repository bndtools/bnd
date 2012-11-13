package aQute.bnd.osgi;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.*;
import java.util.regex.*;

import aQute.bnd.header.*;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.lib.base64.*;
import aQute.lib.io.*;
import aQute.libg.cryptography.*;
import aQute.libg.qtokens.*;

public class Verifier extends Processor {

	private final Jar		dot;
	private final Manifest	manifest;
	private final Domain	main;

	private boolean			r3;
	private boolean			usesRequire;

	final static Pattern	EENAME	= Pattern.compile("CDC-1\\.0/Foundation-1\\.0" + "|CDC-1\\.1/Foundation-1\\.1"
											+ "|OSGi/Minimum-1\\.[1-9]" + "|JRE-1\\.1" + "|J2SE-1\\.2" + "|J2SE-1\\.3"
											+ "|J2SE-1\\.4" + "|J2SE-1\\.5" + "|JavaSE-1\\.6" + "|JavaSE-1\\.7"
											+ "|PersonalJava-1\\.1" + "|PersonalJava-1\\.2"
											+ "|CDC-1\\.0/PersonalBasis-1\\.0" + "|CDC-1\\.0/PersonalJava-1\\.0");

	final static int		V1_1	= 45;
	final static int		V1_2	= 46;
	final static int		V1_3	= 47;
	final static int		V1_4	= 48;
	final static int		V1_5	= 49;
	final static int		V1_6	= 50;
	final static int		V1_7	= 51;
	final static int		V1_8	= 52;

	static class EE {
		String	name;
		int		target;

		EE(String name, @SuppressWarnings("unused") int source, int target) {
			this.name = name;
			this.target = target;
		}

		@Override
		public String toString() {
			return name + "(" + target + ")";
		}
	}

	final static EE[]			ees								= {
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
			new EE("PersonalJava-1.2", V1_1, V1_1), new EE("CDC-1.0/PersonalBasis-1.0", V1_3, V1_1),
			new EE("CDC-1.0/PersonalJava-1.0", V1_3, V1_1), new EE("CDC-1.1/PersonalBasis-1.1", V1_3, V1_2),
			new EE("CDC-1.1/PersonalJava-1.1", V1_3, V1_2)
																};

	final static Pattern		CARDINALITY_PATTERN				= Pattern.compile("single|multiple");
	final static Pattern		RESOLUTION_PATTERN				= Pattern.compile("optional|mandatory");
	final static Pattern		BUNDLEMANIFESTVERSION			= Pattern.compile("2");
	public final static String	SYMBOLICNAME_STRING				= "[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*";
	public final static Pattern	SYMBOLICNAME					= Pattern.compile(SYMBOLICNAME_STRING);

	public final static String	VERSION_STRING					= "[0-9]{1,9}(\\.[0-9]{1,9}(\\.[0-9]{1,9}(\\.[0-9A-Za-z_-]+)?)?)?";
	public final static Pattern	VERSION							= Pattern.compile(VERSION_STRING);
	final static Pattern		FILTEROP						= Pattern.compile("=|<=|>=|~=");
	public final static Pattern	VERSIONRANGE					= Pattern.compile("((\\(|\\[)"

																+ VERSION_STRING + "," + VERSION_STRING + "(\\]|\\)))|"
																		+ VERSION_STRING);
	final static Pattern		FILE							= Pattern
																		.compile("/?[^/\"\n\r\u0000]+(/[^/\"\n\r\u0000]+)*");
	final static Pattern		WILDCARDPACKAGE					= Pattern
																		.compile("((\\p{Alnum}|_)+(\\.(\\p{Alnum}|_)+)*(\\.\\*)?)|\\*");
	public final static Pattern	ISO639							= Pattern.compile("[A-Z][A-Z]");
	public final static Pattern	HEADER_PATTERN					= Pattern.compile("[A-Za-z0-9][-a-zA-Z0-9_]+");
	public final static Pattern	TOKEN							= Pattern.compile("[-a-zA-Z0-9_]+");

	public final static Pattern	NUMBERPATTERN					= Pattern.compile("\\d+");
	public final static Pattern	PACKAGEPATTERN					= Pattern
																		.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");
	public final static Pattern	PATHPATTERN						= Pattern.compile(".*");
	public final static Pattern	FQNPATTERN						= Pattern.compile(".*");
	public final static Pattern	URLPATTERN						= Pattern.compile(".*");
	public final static Pattern	ANYPATTERN						= Pattern.compile(".*");
	public final static Pattern	FILTERPATTERN					= Pattern.compile(".*");
	public final static Pattern	TRUEORFALSEPATTERN				= Pattern.compile("true|false|TRUE|FALSE");
	public static final Pattern	WILDCARDNAMEPATTERN				= Pattern.compile(".*");
	public static final Pattern	BUNDLE_ACTIVATIONPOLICYPATTERN	= Pattern.compile("lazy");

	public final static String	EES[]							= {
			"CDC-1.0/Foundation-1.0", "CDC-1.1/Foundation-1.1", "OSGi/Minimum-1.0", "OSGi/Minimum-1.1",
			"OSGi/Minimum-1.2", "JRE-1.1", "J2SE-1.2", "J2SE-1.3", "J2SE-1.4", "J2SE-1.5", "JavaSE-1.6", "JavaSE-1.7",
			"PersonalJava-1.1", "PersonalJava-1.2", "CDC-1.0/PersonalBasis-1.0", "CDC-1.0/PersonalJava-1.0"
																};

	public final static String	OSNAMES[]						= {
			"AIX", // IBM
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
			"WindowsXP", "WindowsVista",
																};

	public final static String	PROCESSORNAMES[]				= { //
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
			"x86-64",
																};

	final Analyzer				analyzer;
	private Instructions		dynamicImports;

	public Verifier(Jar jar) throws Exception {
		this.analyzer = new Analyzer(this);
		this.analyzer.use(this);
		addClose(analyzer);
		this.analyzer.setJar(jar);
		this.manifest = this.analyzer.calcManifest();
		this.main = Domain.domain(manifest);
		this.dot = jar;
		getInfo(analyzer);
	}

	public Verifier(Analyzer analyzer) throws Exception {
		this.analyzer = analyzer;
		this.dot = analyzer.getJar();
		this.manifest = dot.getManifest();
		this.main = Domain.domain(manifest);
	}

	private void verifyHeaders() {
		for (String h : main) {
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
		String nc = get("Bundle-NativeCode");
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
		}
		catch (Exception e) {
			return "Not a valid filter: " + value + e.getMessage();
		}
	}

	private void verifyActivator() throws Exception {
		String bactivator = main.get("Bundle-Activator");
		if (bactivator != null) {
			TypeRef ref = analyzer.getTypeRefFromFQN(bactivator);
			if (analyzer.getClassspace().containsKey(ref))
				return;

			PackageRef packageRef = ref.getPackageRef();
			if (packageRef.isDefaultPackage())
				error("The Bundle Activator is not in the bundle and it is in the default package ");
			else if (!analyzer.isImported(packageRef)) {
				error("Bundle-Activator not found on the bundle class path nor in imports: " + bactivator);
			}
		}
	}

	private void verifyComponent() {
		String serviceComponent = main.get("Service-Component");
		if (serviceComponent != null) {
			Parameters map = parseHeader(serviceComponent);
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
	 * Check for unresolved imports. These are referrals that are not imported
	 * by the manifest and that are not part of our bundle class path. The are
	 * calculated by removing all the imported packages and contained from the
	 * referred packages.
	 */
	private void verifyUnresolvedReferences() {
		Set<PackageRef> unresolvedReferences = new TreeSet<PackageRef>(analyzer.getReferred().keySet());
		unresolvedReferences.removeAll(analyzer.getImports().keySet());
		unresolvedReferences.removeAll(analyzer.getContained().keySet());

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

		if (!unresolvedReferences.isEmpty()) {
			// Now we want to know the
			// classes that are the culprits
			Set<String> culprits = new HashSet<String>();
			for (Clazz clazz : analyzer.getClassspace().values()) {
				if (hasOverlap(unresolvedReferences, clazz.getReferred()))
					culprits.add(clazz.getAbsolutePath());
			}

			error("Unresolved references to %s by class(es) %s on the Bundle-Classpath: %s", unresolvedReferences,
					culprits, analyzer.getBundleClasspath().keySet());
		}
	}

	/**
	 * @param p
	 * @param pack
	 */
	private boolean isDynamicImport(PackageRef pack) {
		if (dynamicImports == null)
			dynamicImports = new Instructions(main.getDynamicImportPackage());

		return dynamicImports.matches(pack.getFQN());
	}

	private boolean hasOverlap(Set< ? > a, Set< ? > b) {
		for (Iterator< ? > i = a.iterator(); i.hasNext();) {
			if (b.contains(i.next()))
				return true;
		}
		return false;
	}

	public void verify() throws Exception {
		verifyHeaders();
		verifyDirectives("Export-Package", "uses:|mandatory:|include:|exclude:|" + IMPORT_DIRECTIVE, PACKAGEPATTERN,
				"package");
		verifyDirectives("Import-Package", "resolution:", PACKAGEPATTERN, "package");
		verifyDirectives("Require-Bundle", "visibility:|resolution:", SYMBOLICNAME, "bsn");
		verifyDirectives("Fragment-Host", "extension:", SYMBOLICNAME, "bsn");
		verifyDirectives("Provide-Capability", "effective:|uses:", null, null);
		verifyDirectives("Require-Capability", "effective:|resolution:|filter:", null, null);
		verifyDirectives("Bundle-SymbolicName", "singleton:|fragment-attachment:|mandatory:", SYMBOLICNAME, "bsn");

		verifyManifestFirst();
		verifyActivator();
		verifyActivationPolicy();
		verifyComponent();
		verifyNative();
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

		verifyRequirements();
		verifyCapabilities();
	}

	private void verifyRequirements() {
		Parameters map = parseHeader(manifest.getMainAttributes().getValue(Constants.REQUIRE_CAPABILITY));
		for (String key : map.keySet()) {
			Attrs attrs = map.get(key);
			verify(attrs, "filter:", FILTERPATTERN, false, "Requirement %s filter not correct", key);
			verify(attrs, "cardinality:", CARDINALITY_PATTERN, false, "Requirement %s cardinality not correct", key);
			verify(attrs, "resolution:", RESOLUTION_PATTERN, false, "Requirement %s resolution not correct", key);

			if (key.equals("osgi.extender")) {
				// No requirements on extender
			} else if (key.equals("osgi.serviceloader")) {
				verify(attrs, "register:", PACKAGEPATTERN, false,
						"Service Loader extender register: directive not a fully qualified Java name");
			} else if (key.equals("osgi.contract")) {

			} else if (key.equals("osgi.service")) {

			} else if (key.equals("osgi.ee")) {

			} else if (key.startsWith("osgi.wiring.") || key.startsWith("osgi.identity")) {
				error("osgi.wiring.* namespaces must not be specified with generic requirements/capabilities");
			}

			verifyAttrs(attrs);

			if (attrs.containsKey("mandatory:"))
				error("mandatory: directive is intended for Capabilities, not Requirement %s", key);

			if (attrs.containsKey("uses:"))
				error("uses: directive is intended for Capabilities, not Requirement %s", key);
		}
	}

	/**
	 * @param attrs
	 */
	void verifyAttrs(Attrs attrs) {
		for (String a : attrs.keySet()) {
			String v = attrs.get(a);

			if (!a.endsWith(":")) {
				Attrs.Type t = attrs.getType(a);
				if ("version".equals(a)) {
					if (t != Attrs.Type.VERSION)
						error("Version attributes should always be of type version, it is %s", t);
				} else
					verifyType(t, v);
			}
		}
	}

	private void verifyCapabilities() {
		Parameters map = parseHeader(manifest.getMainAttributes().getValue(Constants.PROVIDE_CAPABILITY));
		for (String key : map.keySet()) {
			Attrs attrs = map.get(key);
			verify(attrs, "cardinality:", CARDINALITY_PATTERN, false, "Requirement %s cardinality not correct", key);
			verify(attrs, "resolution:", RESOLUTION_PATTERN, false, "Requirement %s resolution not correct", key);

			if (key.equals("osgi.extender")) {
				verify(attrs, "osgi.extender", SYMBOLICNAME, true,
						"Extender %s must always have the osgi.extender attribute set", key);
				verify(attrs, "version", VERSION, true, "Extender %s must always have a version", key);
			} else if (key.equals("osgi.serviceloader")) {
				verify(attrs, "register:", PACKAGEPATTERN, false,
						"Service Loader extender register: directive not a fully qualified Java name");
			} else if (key.equals("osgi.contract")) {
				verify(attrs, "osgi.contract", SYMBOLICNAME, true,
						"Contracts %s must always have the osgi.contract attribute set", key);

			} else if (key.equals("osgi.service")) {
				verify(attrs, "objectClass", PACKAGEPATTERN, true,
						"osgi.service %s must have the objectClass attribute set", key);

			} else if (key.equals("osgi.ee")) {
				// TODO
			} else if (key.startsWith("osgi.wiring.") || key.startsWith("osgi.identity")) {
				error("osgi.wiring.* namespaces must not be specified with generic requirements/capabilities");
			}

			verifyAttrs(attrs);

			if (attrs.containsKey("filter:"))
				error("filter: directive is intended for Requirements, not Capability %s", key);
			if (attrs.containsKey("cardinality:"))
				error("cardinality: directive is intended for Requirements, not Capability %s", key);
			if (attrs.containsKey("resolution:"))
				error("resolution: directive is intended for Requirements, not Capability %s", key);
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
	 */
	private void verifyDirectives(String header, String directives, Pattern namePattern, String type) {
		Pattern pattern = Pattern.compile(directives);
		Parameters map = parseHeader(manifest.getMainAttributes().getValue(header));
		for (Entry<String,Attrs> entry : map.entrySet()) {
			String pname = removeDuplicateMarker(entry.getKey());

			if (namePattern != null) {
				if (!namePattern.matcher(pname).matches())
					if (isPedantic())
						error("Invalid %s name: '%s'", type, pname);
					else
						warning("Invalid %s name: '%s'", type, pname);
			}

			for (String key : entry.getValue().keySet()) {
				if (key.endsWith(":")) {
					if (!key.startsWith("x-")) {
						Matcher m = pattern.matcher(key);
						if (m.matches())
							continue;

						warning("Unknown directive %s in %s, allowed directives are %s, and 'x-*'.", key, header,
								directives.replace('|', ','));
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
		// warning("Export-Package uses: directive contains packages that are not imported nor exported: %s",
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
		if (map.size() == 0)
			warning("Bundle-ActivationPolicy is set but has no argument %s", policy);
		else if (map.size() > 1)
			warning("Bundle-ActivationPolicy has too many arguments %s", policy);
		else {
			Map<String,String> s = map.get("lazy");
			if (s == null)
				warning("Bundle-ActivationPolicy set but is not set to lazy: %s", policy);
			else
				return true;
		}

		return false;
	}

	public void verifyBundleClasspath() {
		Parameters bcp = main.getBundleClassPath();
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
		String dynamicImportPackage = get("DynamicImport-Package");
		if (dynamicImportPackage == null)
			return;

		Parameters map = main.getDynamicImportPackage();
		for (String name : map.keySet()) {
			name = name.trim();
			if (!verify(name, WILDCARDPACKAGE))
				error("DynamicImport-Package header contains an invalid package name: " + name);

			Map<String,String> sub = map.get(name);
			if (r3 && sub.size() != 0) {
				error("DynamicPackage-Import has attributes on import: " + name
						+ ". This is however, an <=R3 bundle and attributes on this header were introduced in R4. ");
			}
		}
	}

	private void verifyManifestFirst() {
		if (!dot.isManifestFirst()) {
			error("Invalid JAR stream: Manifest should come first to be compatible with JarInputStream, it was not");
		}
	}

	private void verifySymbolicName() {
		Parameters bsn = parseHeader(main.get(Analyzer.BUNDLE_SYMBOLICNAME));
		if (!bsn.isEmpty()) {
			if (bsn.size() > 1)
				error("More than one BSN specified " + bsn);

			String name = bsn.keySet().iterator().next();
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
						throw new IllegalArgumentException("Filter mismatch: ! (not) must have one sub expression "
								+ index + " : " + expr);
					while (Character.isWhitespace(expr.charAt(index)))
						index++;

					index = verifyFilter(expr, index);
					while (Character.isWhitespace(expr.charAt(index)))
						index++;
					if (expr.charAt(index) != ')')
						throw new IllegalArgumentException("Filter mismatch: expected ) at position " + index + " : "
								+ expr);
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
						throw new IllegalArgumentException("Filter mismatch: expected ) at position " + index + " : "
								+ expr);
					return index + 1; // skip )

				default :
					index = verifyFilterOperation(expr, index);
					if (expr.charAt(index) != ')')
						throw new IllegalArgumentException("Filter mismatch: expected ) at position " + index + " : "
								+ expr);
					return index + 1;
			}
		}
		catch (IndexOutOfBoundsException e) {
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
		String value = manifest.getMainAttributes().getValue(name);
		if (value == null)
			return false;

		QuotedTokenizer st = new QuotedTokenizer(value.trim(), ",");
		for (Iterator<String> i = st.getTokenSet().iterator(); i.hasNext();) {
			if (!verify(i.next(), regex)) {
				String msg = "Invalid value for " + name + ", " + value + " does not match " + regex.pattern();
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

		Parameters map = parseHeader(value);
		for (String header : map.keySet()) {
			if (!regex.matcher(header).matches()) {
				String msg = "Invalid value for " + name + ", " + value + " does not match " + regex.pattern();
				if (error)
					error(msg);
				else
					warning(msg);
			}
		}
		return true;
	}

	@Override
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

	/**
	 * Verify checksums
	 */
	/**
	 * Verify the checksums from the manifest against the real thing.
	 * 
	 * @param all
	 *            if each resources must be digested
	 * @return true if ok
	 * @throws Exception
	 */

	public void verifyChecksums(boolean all) throws Exception {
		Manifest m = dot.getManifest();
		if (m == null || m.getEntries().isEmpty()) {
			if (all)
				error("Verify checksums with all but no digests");
			return;
		}

		List<String> missingDigest = new ArrayList<String>();

		for (String path : dot.getResources().keySet()) {
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
				Digester<SHA1> digester = SHA1.getDigester();
				InputStream in = dot.getResource(path).openInputStream();
				IO.copy(in, digester);
				digester.digest();
				if (!expected.equals(digester.digest())) {
					error("Checksum mismatch %s, expected %s, got %s", path, expected, digester.digest());
				}
			}
		}
		if (missingDigest.size() > 0) {
			error("Entries in the manifest are missing digests: %s", missingDigest);
		}
	}
}

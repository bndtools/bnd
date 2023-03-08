package aQute.bnd.build.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.FilterParser.Expression;
import aQute.bnd.version.Version;
import aQute.lib.utf8properties.UTF8Properties;

public enum EE {


	OSGI_Minimum_1_0("OSGi/Minimum-1.0", "OSGi/Minimum", "1.0", 0),

	OSGI_Minimum_1_1("OSGi/Minimum-1.1", "OSGi/Minimum", "1.1", 1, OSGI_Minimum_1_0),

	OSGI_Minimum_1_2("OSGi/Minimum-1.2", "OSGi/Minimum", "1.2", 2, OSGI_Minimum_1_1),

	JRE_1_1("JRE-1.1", "JRE", "1.1", 1),

	J2SE_1_2("J2SE-1.2", "JavaSE", "1.2", 2, JRE_1_1),

	J2SE_1_3("J2SE-1.3", "JavaSE", "1.3", 3, J2SE_1_2, OSGI_Minimum_1_1),

	J2SE_1_4("J2SE-1.4", "JavaSE", "1.4", 4, J2SE_1_3, OSGI_Minimum_1_2),

	J2SE_1_5("J2SE-1.5", "JavaSE", "1.5", 5, J2SE_1_4),

	JavaSE_1_6("JavaSE-1.6", "JavaSE", "1.6", 6, J2SE_1_5),

	JavaSE_1_7("JavaSE-1.7", "JavaSE", "1.7", 7, JavaSE_1_6),

	JavaSE_compact1_1_8("JavaSE/compact1-1.8", "JavaSE/compact1", "1.8", 8, OSGI_Minimum_1_2),

	JavaSE_compact2_1_8("JavaSE/compact2-1.8", "JavaSE/compact2", "1.8", 8, JavaSE_compact1_1_8),

	JavaSE_compact3_1_8("JavaSE/compact3-1.8", "JavaSE/compact3", "1.8", 8, JavaSE_compact2_1_8),

	JavaSE_1_8("JavaSE-1.8", "JavaSE", "1.8", 8, JavaSE_1_7, JavaSE_compact3_1_8),

	JavaSE_9(9),
	JavaSE_10(10),
	JavaSE_11(11),
	JavaSE_12(12),
	JavaSE_13(13),
	JavaSE_14(14),
	JavaSE_15(15),
	JavaSE_16(16),
	JavaSE_17(17),
	JavaSE_18(18),
	JavaSE_19(19),
	JavaSE_20(20),
	JavaSE_21(21),
	JavaSE_22(22),
	JavaSE_23(23),
	JavaSE_24(24),

	UNKNOWN("<UNKNOWN>", "UNKNOWN", "0", 0);

	final public static int			MAX_SUPPORTED_RELEASE	= 24;

	private final String			eeName;
	private final String			capabilityName;
	private final String			versionLabel;
	private final Version			capabilityVersion;
	private final EE[]				compatible;
	private final int				release;
	private transient EnumSet<EE>	compatibleSet;
	private transient Parameters	packages	= null;
	private transient Parameters	modules		= null;

	/**
	 * For use by JavaSE_9 and later.
	 */
	EE(int release) {
		int version = ordinal() - 5;
		this.versionLabel = Integer.toString(version);
		this.eeName = "JavaSE-" + versionLabel;
		this.capabilityName = "JavaSE";
		this.capabilityVersion = new Version(version);
		this.compatible = null;
		this.release = release;
	}

	EE(String eeName, String capabilityName, String versionLabel, int release, EE... compatible) {
		this.eeName = eeName;
		this.capabilityName = capabilityName;
		this.versionLabel = versionLabel;
		this.capabilityVersion = new Version(versionLabel);
		this.compatible = compatible;
		this.release = release;
	}

	public String getEEName() {
		return eeName;
	}

	/**
	 * @return An array of EEs that this EE implicitly offers, through backwards
	 *         compatibility.
	 */
	public EE[] getCompatible() {
		EnumSet<EE> set = getCompatibleSet();
		return set.toArray(new EE[0]);
	}

	private static final EE[] values = values();

	private Optional<EE> previous() {
		int ordinal = ordinal() - 1;
		if (ordinal >= 0) {
			return Optional.of(values[ordinal]);
		}
		return Optional.empty();
	}

	private EnumSet<EE> getCompatibleSet() {
		if (compatibleSet != null) {
			return compatibleSet;
		}
		EnumSet<EE> set = EnumSet.noneOf(getDeclaringClass());
		if (compatible != null) {
			for (EE ee : compatible) {
				set.add(ee);
				set.addAll(ee.getCompatibleSet());
			}
		} else {
			Optional<EE> previous = previous();
			previous.ifPresent(ee -> {
				set.add(ee);
				set.addAll(ee.getCompatibleSet());
			});
		}
		return compatibleSet = set;
	}

	public String getCapabilityName() {
		return capabilityName;
	}

	public String getVersionLabel() {
		return versionLabel;
	}

	public Version getCapabilityVersion() {
		return capabilityVersion;
	}

	/**
	 * @return the java release target corresponding to this EE
	 */
	public OptionalInt getReleaseTarget() {
		Version version = getCapabilityVersion();
		int major = version.getMajor();
		if (major > 1) {
			return OptionalInt.of(major);
		}
		if (major == 1 && version.getMinor() > 5) {
			return OptionalInt.of(version.getMinor());
		}
		return OptionalInt.empty();
	}

	public static Optional<EE> highestFromTargetVersion(String targetVersion) {
		Version version = Optional.of(targetVersion)
			.map(Analyzer::cleanupVersion)
			.map(Version::new)
			.map(v -> {
				int major = v.getMajor();
				int minor = v.getMinor();
				// maps 8 to 1.8
				if ((major > 1) && (major < 9)) {
					minor = major;
					major = 1;
				}
				// drop the MICRO version since EEs don't have them
				return new Version(major, minor, 0);
			})
			// practically unreachable since NPE and invalid syntax are caught
			// earlier
			.orElseThrow(() -> new IllegalArgumentException(
				"Argument could not be recognized as a version string: " + targetVersion));
		return Arrays.stream(values)
			.filter(ee -> ee.capabilityVersion.compareTo(version) == 0)
			.sorted(Collections.reverseOrder())
			.findFirst();
	}

	public static EE parse(String str) {
		for (EE ee : values) {
			if (ee.eeName.equalsIgnoreCase(str))
				return ee;
		}
		return null;
	}

	/**
	 * Return the list of packages
	 *
	 * @throws IOException (Unchecked via {@link Exceptions})
	 */
	@SuppressWarnings("javadoc")
	public Parameters getPackages() {
		if (packages == null) {
			init();
		}
		return packages;
	}

	/**
	 * Return the list of modules
	 *
	 * @throws IOException (Unchecked via {@link Exceptions})
	 */
	@SuppressWarnings("javadoc")
	public Parameters getModules() {
		if (modules == null) {
			init();
		}
		return modules;
	}

	private void init() {
		try (InputStream stream = EE.class.getResourceAsStream(name() + ".properties")) {
			if (stream == null) {
				Optional<EE> previous = previous();
				packages = previous.map(EE::getPackages)
					.orElseGet(Parameters::new);
				modules = previous.map(EE::getModules)
					.orElseGet(Parameters::new);
				return;
			}
			UTF8Properties props = new UTF8Properties();
			props.load(stream);
			String packagesProp = props.getProperty("org.osgi.framework.system.packages");
			packages = new Parameters(packagesProp);
			String modulesProp = props.getProperty("jpms.modules");
			modules = new Parameters(modulesProp);
		} catch (IOException ioe) {
			throw Exceptions.duck(ioe);
		}
	}

	public int getRelease() {
		return release;
	}

	public int getMajorVersion() {
		return release + 44;
	}

	/**
	 * From a Require-Capability header, extract the Execution Environment
	 * capability and match against EEs. The EEs are traversed from highest
	 * release to lowest release, first matching release is returned.
	 *
	 * @param requireCapability the Require-Capability header
	 * @return the highest EE in the list
	 */
	public static Optional<EE> getEEFromRequirement(String requireCapability) {
		Parameters reqs = new Parameters(requireCapability);
		EE result = null;
		nextReq: for (Map.Entry<String, Attrs> e : reqs.entrySet()) {
			Attrs attrs = reqs.get(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
			if (attrs != null) {
				String filter = attrs.get(ExecutionEnvironmentNamespace.REQUIREMENT_FILTER_DIRECTIVE);
				FilterParser fp = new FilterParser();
				Expression expr = fp.parse(filter);
				EE[] values = EE.values();
				Map<String, Object> map = new HashMap<>();
				for (int i = values.length - 1; i >= 0; i--) {
					EE v = values[i];
					if (result != null && v.compareTo(result) <= 0)
						continue nextReq;

					map.put(ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE, v.getCapabilityVersion());
					if (expr.eval(map)) {
						result = v;
						continue nextReq;
					}
				}
			}
		}
		return Optional.ofNullable(result);
	}

	final static EE[] classFileVersionsMinus44 = {
		UNKNOWN, JRE_1_1, J2SE_1_2, J2SE_1_3, J2SE_1_4, J2SE_1_5, JavaSE_1_6, JavaSE_1_7, JavaSE_1_8, JavaSE_9,
		JavaSE_10, JavaSE_11, JavaSE_12, JavaSE_13, JavaSE_14, JavaSE_15, JavaSE_16, JavaSE_17, JavaSE_18, JavaSE_19,
		JavaSE_20, JavaSE_21, JavaSE_22, JavaSE_23, JavaSE_24
	};

	/**
	 * Return the EE associated with the given class file version
	 *
	 * @param majorVersion the class file major version
	 * @return the EE or empty
	 */
	public static EE getEEFromClassVersion(int majorVersion) {
		majorVersion -= 44;
		if (majorVersion < 0 || majorVersion > classFileVersionsMinus44.length)
			return UNKNOWN;
		return classFileVersionsMinus44[majorVersion];
	}

	/**
	 * Return the EE related to the release version
	 */

	public static EE getEEFromReleaseVersion(int releaseVersion) {
		for (int i = values().length; i >= 0; i--) {
			EE ee = values()[i];
			if (ee.release == releaseVersion)
				return ee;
		}
		return UNKNOWN;
	}
}

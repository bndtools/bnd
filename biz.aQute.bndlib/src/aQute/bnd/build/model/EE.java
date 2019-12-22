package aQute.bnd.build.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.version.Version;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.utf8properties.UTF8Properties;

public enum EE {

	OSGI_Minimum_1_0("OSGi/Minimum-1.0", "OSGi/Minimum", new Version("1.0")),

	OSGI_Minimum_1_1("OSGi/Minimum-1.1", "OSGi/Minimum", new Version("1.1"), OSGI_Minimum_1_0),

	OSGI_Minimum_1_2("OSGi/Minimum-1.2", "OSGi/Minimum", new Version("1.2"), OSGI_Minimum_1_1),

	JRE_1_1("JRE-1.1", "JRE", new Version("1.1")),

	J2SE_1_2("J2SE-1.2", "JavaSE", new Version("1.2"), JRE_1_1),

	J2SE_1_3("J2SE-1.3", "JavaSE", new Version("1.3"), J2SE_1_2, OSGI_Minimum_1_1),

	J2SE_1_4("J2SE-1.4", "JavaSE", new Version("1.4"), J2SE_1_3, OSGI_Minimum_1_2),

	J2SE_1_5("J2SE-1.5", "JavaSE", new Version("1.5"), J2SE_1_4),

	JavaSE_1_6("JavaSE-1.6", "JavaSE", new Version("1.6"), J2SE_1_5),

	JavaSE_1_7("JavaSE-1.7", "JavaSE", new Version("1.7"), JavaSE_1_6),

	JavaSE_compact1_1_8("JavaSE/compact1-1.8", "JavaSE/compact1", new Version("1.8"), OSGI_Minimum_1_2),

	JavaSE_compact2_1_8("JavaSE/compact2-1.8", "JavaSE/compact2", new Version("1.8"), JavaSE_compact1_1_8),

	JavaSE_compact3_1_8("JavaSE/compact3-1.8", "JavaSE/compact3", new Version("1.8"), JavaSE_compact2_1_8),

	JavaSE_1_8("JavaSE-1.8", "JavaSE", new Version("1.8"), JavaSE_1_7, JavaSE_compact3_1_8),

	JavaSE_9_0("JavaSE-9", "JavaSE", new Version("9"), JavaSE_1_8),

	JavaSE_10_0("JavaSE-10", "JavaSE", new Version("10"), JavaSE_9_0),

	JavaSE_11_0("JavaSE-11", "JavaSE", new Version("11"), JavaSE_10_0),

	JavaSE_12_0("JavaSE-12", "JavaSE", new Version("12"), JavaSE_11_0),

	JavaSE_13_0("JavaSE-13", "JavaSE", new Version("13"), JavaSE_12_0),
	JavaSE_14_0("JavaSE-14", "JavaSE", new Version("14"), JavaSE_13_0),

	UNKNOWN("Unknown", "unknown", new Version(0));

	private final String			eeName;
	private final String			capabilityName;
	private final Version			capabilityVersion;
	private final EE[]				compatible;
	private transient EnumSet<EE>	compatibleSet;
	private transient Parameters	packages	= null;
	private transient Parameters	modules		= null;

	EE(String name, String capabilityName, Version capabilityVersion, EE... compatible) {
		this.eeName = name;
		this.capabilityName = capabilityName;
		this.capabilityVersion = capabilityVersion;
		this.compatible = compatible;
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
		}
		return compatibleSet = set;
	}

	public String getCapabilityName() {
		return capabilityName;
	}

	public Version getCapabilityVersion() {
		return capabilityVersion;
	}

	public static Optional<EE> highestFromTargetVersion(String targetVersion) {
		Version version = Optional.of(targetVersion)
			.map(Analyzer::cleanupVersion)
			.map(Version::new)
			// drop the MICRO version since EEs don't have them
			.map(v -> new Version(v.getMajor(), v.getMinor(), 0))
			// practically unreachable since NPE and invalid syntax are caught
			// earlier
			.orElseThrow(() -> new IllegalArgumentException(
				"Argument could not be recognized as a version string: " + targetVersion));
		return Arrays.stream(values())
			.filter(ee -> ee.capabilityVersion.compareTo(version) == 0)
			.sorted(Collections.reverseOrder())
			.findFirst();
	}

	public static EE parse(String str) {
		for (EE ee : values()) {
			if (ee.eeName.equals(str))
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
				packages = new Parameters();
				modules = new Parameters();
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
}

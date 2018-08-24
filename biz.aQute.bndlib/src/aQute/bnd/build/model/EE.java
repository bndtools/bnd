package aQute.bnd.build.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;

import aQute.bnd.header.Parameters;
import aQute.bnd.version.Version;
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

	UNKNOWN("Unknown", "unknown", new Version(0));

	private final String			eeName;
	private final String			capabilityName;
	private final Version			capabilityVersion;
	private final EE[]				compatible;
	private transient EnumSet<EE>	compatibleSet;
	private transient Parameters	packages	= null;

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
	 * @throws IOException
	 */
	public Parameters getPackages() throws IOException {
		if (packages == null) {
			try (InputStream stream = EE.class.getResourceAsStream(name() + ".properties")) {
				if (stream == null)
					return new Parameters();
				UTF8Properties props = new UTF8Properties();
				props.load(stream);
				String exports = props.getProperty("org.osgi.framework.system.packages");
				packages = new Parameters(exports);
			}
		}
		return packages;
	}
}

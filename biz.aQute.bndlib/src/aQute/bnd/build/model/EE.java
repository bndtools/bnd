package aQute.bnd.build.model;

import java.util.*;

import aQute.bnd.version.*;

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

	JavaSE_1_8("JavaSE-1.8", "JavaSE", new Version("1.8"), JavaSE_1_7),

	JavaSE_1_9("JavaSE-1.9", "JavaSE", new Version("1.9"), JavaSE_1_8),

    UNKNOWN("Unknown","unknown", new Version(0));

    private final String eeName;
    private final String capabilityName;
    private final Version capabilityVersion;
    private final EE[] compatible;
	private transient EnumSet<EE>	compatibleSet;

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
     * @return An array of EEs that this EE implicitly offers, through backwards compatibility.
     */
    public EE[] getCompatible() {
		EnumSet<EE> set = getCompatibleSet();
		return set.toArray(new EE[set.size()]);
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
}

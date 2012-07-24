package aQute.bnd.build.model;

public enum EE {

    OSGI_Minimum_1_0("OSGi/Minimum-1.0"), OSGI_Minimum_1_1("OSGi/Minimum-1.1", OSGI_Minimum_1_0), OSGI_Minimum_1_2("OSGi/Minimum-1.2", OSGI_Minimum_1_0, OSGI_Minimum_1_1),

    JRE_1_1("JRE-1.1"), J2SE_1_2("J2SE-1.2", JRE_1_1), J2SE_1_3("J2SE-1.3", JRE_1_1, J2SE_1_2, OSGI_Minimum_1_0, OSGI_Minimum_1_1), J2SE_1_4("J2SE-1.4", JRE_1_1, J2SE_1_2, J2SE_1_3, OSGI_Minimum_1_0, OSGI_Minimum_1_1, OSGI_Minimum_1_2), J2SE_1_5(
            "J2SE-1.5", JRE_1_1, J2SE_1_2, J2SE_1_3, J2SE_1_4, OSGI_Minimum_1_0, OSGI_Minimum_1_1, OSGI_Minimum_1_2),

    JavaSE_1_6("JavaSE-1.6", JRE_1_1, J2SE_1_2, J2SE_1_3, J2SE_1_4, J2SE_1_5, OSGI_Minimum_1_0, OSGI_Minimum_1_1, OSGI_Minimum_1_2), JavaSE_1_7("JavaSE-1.7", JRE_1_1, J2SE_1_2, J2SE_1_3, J2SE_1_4, J2SE_1_5, JavaSE_1_6, OSGI_Minimum_1_0,
            OSGI_Minimum_1_1, OSGI_Minimum_1_2), JavaSE_1_8("JavaSE-1.8", JRE_1_1, J2SE_1_2, J2SE_1_3, J2SE_1_4, J2SE_1_5, JavaSE_1_6, JavaSE_1_7, OSGI_Minimum_1_0, OSGI_Minimum_1_1, OSGI_Minimum_1_2), JavaSE_1_9("JavaSE-1.9", JRE_1_1,
            J2SE_1_2, J2SE_1_3, J2SE_1_4, J2SE_1_5, JavaSE_1_6, JavaSE_1_7, JavaSE_1_8, OSGI_Minimum_1_0, OSGI_Minimum_1_1, OSGI_Minimum_1_2);

    private final String eeName;
    private final EE[] compatible;

    EE(String name, EE... compatible) {
        eeName = name;
        this.compatible = compatible;
    }

    public String getEEName() {
        return eeName;
    }

    /**
     * @return An array of EEs that this EE implicitly offers, through backwards compatibility.
     */
    public EE[] getCompatible() {
        return compatible != null ? compatible : new EE[0];
    }

    public static EE parse(String str) {
        for (EE ee : values()) {
            if (ee.eeName.equals(str))
                return ee;
        }
        throw new IllegalArgumentException("Unrecognised execution environment name: " + str);
    }
}

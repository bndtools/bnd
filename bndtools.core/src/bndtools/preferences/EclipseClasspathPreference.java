package bndtools.preferences;


public enum EclipseClasspathPreference {

    expose, hide;

    public static final String PREFERENCE_KEY = "eclipseClasspath";
    private static final EclipseClasspathPreference DEFAULT = expose;
    
    public static EclipseClasspathPreference parse(String string) {
        try {
            return valueOf(string);
        } catch (Exception e) {
            return DEFAULT;
        }
    }

}

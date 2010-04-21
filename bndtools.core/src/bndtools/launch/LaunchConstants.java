package bndtools.launch;



public interface LaunchConstants {

    public static String LAUNCH_ID_OSGI_RUNTIME = "bndtools.launch";

    public static String ATTR_FRAMEWORK_BSN = "frameworkBSN";
    public static String DEFAULT_FRAMEWORK_BSN = "org.eclipse.osgi";

    public static String ATTR_DYNAMIC_BUNDLES = "dynamicBundles";
    public static boolean DEFAULT_DYNAMIC_BUNDLES = true;

    public static String ATTR_CLEAN = "clean";
    public static boolean DEFAULT_CLEAN = false;

    public static String ATTR_LOGLEVEL = "logLevel";
    public static String DEFAULT_LOGLEVEL = "WARNING";

    public static String ATTR_LOG_OUTPUT = "logOutput";
    public static String VALUE_LOG_OUTPUT_CONSOLE = "console";
    public static String DEFAULT_LOG_OUTPUT = VALUE_LOG_OUTPUT_CONSOLE;

}
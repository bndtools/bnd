package bndtools.launch;

public interface LaunchConstants {

    public static String EXT_BND = ".bnd";
    public static String EXT_BNDRUN = ".bndrun";

    public static String LAUNCH_ID_OSGI_RUNTIME = "bndtools.launch";
    public static String LAUNCH_ID_OSGI_JUNIT = "bndtools.launch.junit";

    public static String ATTR_LAUNCH_TARGET = "launchTarget";
    public static String ATTR_DYNAMIC_BUNDLES = "dynamicBundles";
    public static boolean DEFAULT_DYNAMIC_BUNDLES = true;

    public static String ATTR_CLEAN = "clean";
    public static boolean DEFAULT_CLEAN = true;

    public static String ATTR_TRACE = "trace";
    public static boolean DEFAULT_TRACE = false;

    @Deprecated
    public static String ATTR_LOGLEVEL = "logLevel";

    @Deprecated
    public static final String ATTR_OLD_JUNIT_KEEP_ALIVE = "bndtools.runtime.junit.keepAlive";
    public static final String ATTR_JUNIT_KEEP_ALIVE = "junit.keepAlive";
    public static final boolean DEFAULT_JUNIT_KEEP_ALIVE = false;

    public static final int LAUNCH_STATUS_JUNIT = 999;
}
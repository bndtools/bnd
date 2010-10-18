package bndtools.launch;



public interface LaunchConstants {
    public static String EXT_BNDRUN = ".bndrun";

    public static String LAUNCH_ID_OSGI_RUNTIME = "bndtools.launch";
    public static String LAUNCH_ID_OSGI_JUNIT   = "bndtools.launch.junit";

    public static String ATTR_LAUNCH_TARGET = "launchTarget";
    public static String ATTR_DYNAMIC_BUNDLES = "dynamicBundles";
    public static boolean DEFAULT_DYNAMIC_BUNDLES = true;

    public static String ATTR_CLEAN = "clean";
    public static boolean DEFAULT_CLEAN = true;

    public static String ATTR_LOGLEVEL = "logLevel";
    public static String DEFAULT_LOGLEVEL = "WARNING";

    public static String ATTR_LOG_OUTPUT = "logOutput";
    public static String VALUE_LOG_OUTPUT_CONSOLE = "console";
    public static String DEFAULT_LOG_OUTPUT = VALUE_LOG_OUTPUT_CONSOLE;

    public static String ATTR_JUNIT_REPORTER = "junitReporter";
    public static String DEFAULT_JUNIT_REPORTER = "port";
    public static String ATTR_LAUNCHER_BUNDLE_PATH = "launcherBundlePath";

    // BndtoolsLauncher
    public static final String LAUNCHER_PREFIX = "bndtools.launcher";

    public static final String PROP_LAUNCH_LOGLEVEL = LAUNCHER_PREFIX + ".logLevel";
    public static final String PROP_LAUNCH_LOG_OUTPUT = LAUNCHER_PREFIX + ".logOutput";
    public static final String PROP_LAUNCH_CLEAN = LAUNCHER_PREFIX + ".clean";
    public static final String PROP_LAUNCH_DYNAMIC_BUNDLES = LAUNCHER_PREFIX + ".dynamicBundles";
    public static final String PROP_LAUNCH_RUNBUNDLES = LAUNCHER_PREFIX + ".runBundles";
    public static final String PROP_LAUNCH_SHUTDOWN_ON_ERROR = LAUNCHER_PREFIX + ".shutdownOnError";

    public static final String PROP_LAUNCH_STORAGE_DIR = LAUNCHER_PREFIX + ".storageDir";
    public static final String DEFAULT_LAUNCH_STORAGE_DIR_RUN = "runtimefw";
    public static final String DEFAULT_LAUNCH_STORAGE_DIR_TEST = "testfw";

    // BndtooslRuntimeJUnit
    public static final String JUNIT_PREFIX = "bndtools.runtime.junit";

    public static final String PROP_LAUNCH_JUNIT_REPORTER = JUNIT_PREFIX + ".reporter";
    public static final String PROP_LAUNCH_JUNIT_KEEP_ALIVE = JUNIT_PREFIX + ".keepAlive";
    public static final String PROP_LAUNCH_JUNIT_THREADPOOL_SIZE = JUNIT_PREFIX + ".threadPoolSize";
    public static final String PROP_LAUNCH_JUNIT_START_TIMEOUT = JUNIT_PREFIX + ".startTimeout";
    public static final String DEFAULT_LAUNCH_JUNIT_START_TIMEOUT = Integer.toString(45);

}
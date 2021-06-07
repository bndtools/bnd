package bndtools.launch;

public interface LaunchConstants {

	String	EXT_BND						= ".bnd";
	String	EXT_BNDRUN					= ".bndrun";

	String	LAUNCH_ID_OSGI_RUNTIME		= "bndtools.launch";
	String	LAUNCH_ID_OSGI_JUNIT		= "bndtools.launch.junit";

	String	ATTR_LAUNCH_TARGET			= "launchTarget";
	String	ATTR_DYNAMIC_BUNDLES		= "dynamicBundles";
	boolean	DEFAULT_DYNAMIC_BUNDLES		= true;

	String	ATTR_CLEAN					= "clean";
	boolean	DEFAULT_CLEAN				= true;

	String	ATTR_TRACE					= "trace";
	boolean	DEFAULT_TRACE				= false;

	@Deprecated
	String	ATTR_LOGLEVEL				= "logLevel";

	@Deprecated
	String	ATTR_OLD_JUNIT_KEEP_ALIVE	= "bndtools.runtime.junit.keepAlive";
	String	ATTR_JUNIT_KEEP_ALIVE		= "junit.keepAlive";
	boolean	DEFAULT_JUNIT_KEEP_ALIVE	= false;
	String	ATTR_RERUN_IDE				= "junit.rerunIDE";
	boolean	DEFAULT_RERUN_IDE			= false;

	int		LAUNCH_STATUS_JUNIT			= 999;
}

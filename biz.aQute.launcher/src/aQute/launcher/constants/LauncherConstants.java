package aQute.launcher.constants;

public interface LauncherConstants {
	String	LAUNCH_PROPERTIES		= "launch.properties";
	String	LAUNCH_REPORT			= "launch.report";
	String	LAUNCH_FRAMEWORK		= "launch.framework";
	String	LAUNCH_STORAGE_DIR		= "launch.storage.dir";
	String	LAUNCH_KEEP				= "launch.keep";
	String	LAUNCH_RUNBUNDLES		= "launch.bundles";
	String	LAUNCH_SYSTEMPACKAGES	= "launch.system.packages";
	String	LAUNCH_LOG_LEVEL		= "launch.loglevel";
	String	LAUNCH_TIMEOUT			= "launch.timeout";

	
	/**
	 * The command line arguments of the launcher. Launcher are not
	 * supposed to eat any arguments, they should use -D VM arguments
	 * so that applications can leverage the command line. The 
	 * launcher must register itself as a service under its impl.
	 * class with this property set to a String[].
	 */
	String	LAUNCHER_ARGUMENTS		= "launcher.arguments";

	// MUST BE ALIGNED WITH ProjectLauncher!
	int		OK						= 0;
	int		ERROR					= -2;
	int		WARNING					= -1;
	int		TIMEDOUT				= -3;
	int		UPDATE_NEEDED			= -4;
	int		CANCELED				= -5;
	int		DUPLICATE_BUNDLE		= -6;
	int		RESOLVE_ERROR			= -7;
	int		ACTIVATOR_ERROR			= -8;
	// Start custom errors from here
	int		CUSTOM_LAUNCHER			= -128;

}

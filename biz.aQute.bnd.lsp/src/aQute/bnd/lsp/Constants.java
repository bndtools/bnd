package aQute.bnd.lsp;

public interface Constants {
	String	LANGUAGE_ID					= "bnd";

	// Commands
	String	COMMAND_BUILD_PROJECT		= "bnd.build.project";
	String	COMMAND_BUILD_WORKSPACE		= "bnd.build.workspace";
	String	COMMAND_BUILD_CLEAN			= "bnd.build.clean";
	String	COMMAND_RESOLVE_BNDRUN		= "bnd.resolve";
	String	COMMAND_BASELINE			= "bnd.baseline";
	String	COMMAND_MACRO_EXPAND		= "bnd.macro.expand";
	String	COMMAND_REPO_LIST			= "bnd.repo.list";
	String	COMMAND_JAR_PRINT			= "bnd.jar.print";
	String	COMMAND_EFFECTIVE_PROPERTIES = "bnd.properties.effective";
	String	COMMAND_LAUNCH_PREPARE		= "bnd.launch.prepare";
	String	COMMAND_LAUNCH_DISPOSE		= "bnd.launch.dispose";

	// Notifications
	String	NOTIFICATION_STATUS			= "bnd/status";

	// Diagnostic Source
	String	DIAGNOSTIC_SOURCE_BND		= "bnd";
	String	DIAGNOSTIC_SOURCE_BND_JAVAC	= "bnd-javac";
}

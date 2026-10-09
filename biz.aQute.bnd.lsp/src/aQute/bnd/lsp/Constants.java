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
	String	COMMAND_JAR_PRINT_TEXT		= "bnd.jar.printText";
	String	COMMAND_EFFECTIVE_PROPERTIES = "bnd.properties.effective";
	String	COMMAND_RESOLUTION_ANALYZE	= "bnd.resolution.analyze";
	String	COMMAND_LAUNCH_PREPARE		= "bnd.launch.prepare";
	String	COMMAND_LAUNCH_DISPOSE		= "bnd.launch.dispose";
	String	COMMAND_REPOSITORIES_LIST		= "bnd.repositories.list";
	String	COMMAND_REPOSITORIES_BUNDLES	= "bnd.repositories.bundles";
	String	COMMAND_REPOSITORIES_VERSIONS	= "bnd.repositories.versions";
	String	COMMAND_REPOSITORIES_FEATURE	= "bnd.repositories.feature";
	String	COMMAND_REPOSITORIES_GET		= "bnd.repositories.get";
	String	COMMAND_REPOSITORIES_SEARCH		= "bnd.repositories.search";
	String	COMMAND_REPOSITORIES_ACTIONS	= "bnd.repositories.listActions";
	String	COMMAND_REPOSITORIES_RUN_ACTION	= "bnd.repositories.runAction";
	String	COMMAND_REPOSITORIES_REFRESH	= "bnd.repositories.reload";
	String	COMMAND_REPOSITORIES_PUT		= "bnd.repositories.put";
	String	COMMAND_REPOSITORIES_DOWNLOAD	= "bnd.repositories.fetch";
	String	COMMAND_WORKSPACE_OFFLINE		= "bnd.workspace.offline";

	// Notifications
	String	NOTIFICATION_STATUS			= "bnd/status";

	// Diagnostic Source
	String	DIAGNOSTIC_SOURCE_BND		= "bnd";
	String	DIAGNOSTIC_SOURCE_BND_JAVAC	= "bnd-javac";
}

package bndtools.editor.common;

import static bndtools.utils.EditorUtils.createHelpButton;
import static bndtools.utils.EditorUtils.createHelpButtonWithText;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;

/**
 * Helper containing help buttons for different parts / views which link to the
 * user manual.
 */
public final class HelpButtons {

	public static final Action					HELP_BTN_REPOSITORIES			= createHelpButton(
		"https://bndtools.org/manual/repositories-view.html",
		"The Repositories View provides a user-friendly interface to inspect and manage the bundle repositories that are available to your Bndtools projects. Click to open manual in the browser.");

	public static final Action					HELP_BTN_BNDTOOLS_EXPLORER		= createHelpButton(
		"https://bndtools.org/manual/packageexplorer.html",
		"The explorer provides an overview of the projects and their contents and allows advanced filtering. Click to open manual in the browser.");

	public static final Action					HELP_BTN_RESOLUTION_VIEW		= createHelpButton(
		"https://bndtools.org/manual/resolution-view.html",
		"The Resolution view shows the requirements and capabilities of one or multiple selected items, be they bnd.bnd files, JAR files, or entries in the Repositories view. This is useful for understanding dependencies as it provides information about what requirements are matched with what capabilities from the included resources. Click to open manual in the browser.");

	public static final Action					HELP_BTN_BND_EDITOR				= createHelpButton(
		"https://bndtools.org/manual/bndeditor.html",
		"This editor allows to edit bnd.bnd files, which define OSGi bundle metadata and build instructions for Java projects, encompassing sections for builtpath, imports, exports, bundle headers, and instructions to control the generation of the resulting OSGi bundle. Click to open manual in the browser.");

	public static final Action					HELP_BTN_BND_EDITOR_WORKSPACE	= createHelpButton(
		"https://bndtools.org/manual/bndeditor.html",
		"This editor allows to edit global .bnd files such as the main cnf/build.bnd, which serves as the central configuration hub for the entire bndtools workspace, allowing users to define and manage global build settings, plugins, repository references, and other overarching workspace properties. Click to open manual in the browser.");

	public static final ActionContributionItem	HELP_BTN_BND_EDITOR_RUN			= createHelpButtonWithText(
		"https://bndtools.org/manual/bndeditor.html#run", "Help",
		"The bnd editor for .bndrun files facilitates dependency management, automated resolution of required bundles, configuration of JVM and framework properties, direct launching of OSGi instances for testing, and the export of run configurations as executable JARs. Click to open manual in the browser.");

	public static final String					HELP_URL_RESOLUTIONRESULTSWIZARDPAGE	= "https://bnd.bndtools.org/chapters/250-resolving.html#resolving-1";

	private HelpButtons() {}
}

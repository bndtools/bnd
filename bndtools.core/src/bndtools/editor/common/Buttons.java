package bndtools.editor.common;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.ISharedImages;

/**
 * Helper containing help buttons for different parts / views which link to the
 * user manual.
 */
public final class Buttons {

	public static final Action					HELP_BTN_REPOSITORIES		= createHelpButton(
		"https://bndtools.org/manual/repositories-view.html",
		"The Repositories View provides a user-friendly interface to inspect and manage the bundle repositories that are available to your Bndtools projects. Click to open manual in the browser.");

	public static final Action					HELP_BTN_BNDTOOLS_EXPLORER	= createHelpButton(
		"https://bndtools.org/manual/packageexplorer.html",
		"The explorer provides an overview of the projects and their contents and allows advanced filtering. Click to open manual in the browser.");

	public static final Action					HELP_BTN_RESOLUTION_VIEW	= createHelpButton(
		"https://bndtools.org/manual/resolution-view.html",
		"The Resolution view shows the requirements and capabilities of one or multiple selected items, be they bnd.bnd files, JAR files, or entries in the Repositories view. This is useful for understanding dependencies as it provides information about what requirements are matched with what capabilities from the included resources. Click to open manual in the browser.");

	public static final Action					HELP_BTN_BND_EDITOR			= createHelpButton(
		"https://bndtools.org/manual/bndeditor.html",
		"This editor allows to edit bnd.bnd files, which define OSGi bundle metadata and build instructions for Java projects, encompassing sections for builtpath, imports, exports, bundle headers, and instructions to control the generation of the resulting OSGi bundle.");

	public static final ActionContributionItem	HELP_BTN_BND_EDITOR_RUN		= createHelpButtonWithText(
		"https://bndtools.org/manual/bndeditor.html#run", "Help",
		"The bnd editor for .bndrun files facilitates dependency management, automated resolution of required bundles, configuration of JVM and framework properties, direct launching of OSGi instances for testing, and the export of run configurations as executable JARs.");

	/**
	 * Creates a help button with icon and tooltip.
	 *
	 * @param url
	 * @param tooltipText
	 * @return
	 */
	private static Action createHelpButton(String url, String tooltipText) {
		Action btn = new Action("Help", IAction.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				Program.launch(url);
			}
		};
		btn.setEnabled(true);
		btn.setToolTipText(tooltipText);
		btn.setImageDescriptor(Icons.desc(ISharedImages.IMG_LCL_LINKTO_HELP));

		return btn;
	}

	/**
	 * Creates a helpbutton with icon, text and tooltip.
	 *
	 * @param url
	 * @param buttonText
	 * @param tooltipText
	 * @return
	 */
	private static ActionContributionItem createHelpButtonWithText(String url, String buttonText, String tooltipText) {
		Action btn = createHelpButton(url, tooltipText);
		btn.setText(buttonText);

		// the ActionContributionItem is required to display text below the icon
		// of the button
		ActionContributionItem helpContrib = new ActionContributionItem(btn);
		helpContrib.setMode(ActionContributionItem.MODE_FORCE_TEXT);

		return helpContrib;
	}

	private Buttons() {}
}

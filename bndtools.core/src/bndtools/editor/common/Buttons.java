package bndtools.editor.common;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.ISharedImages;

/**
 * Helper containing help buttons for different parts / views which link to the
 * user manual.
 */
public final class Buttons {

	public static final Action	HELP_BTN_REPOSITORIES		= createHelpButton(
		"https://bndtools.org/manual/repositories-view.html",
		"The Repositories View provides a user-friendly interface to inspect and manage the bundle repositories that are available to your Bndtools projects. Click to open manual in the browser.");

	public static final Action	HELP_BTN_BNDTOOLS_EXPLORER	= createHelpButton(
		"https://bndtools.org/manual/packageexplorer.html",
		"The explorer provides an overview of the projects and their contents and allows advanced filtering. Click to open manual in the browser.");

	public static final Action	HELP_BTN_RESOLUTION_VIEW	= createHelpButton(
		"https://bndtools.org/manual/resolution-view.html",
		"The Resolution view shows the requirements and capabilities of one or multiple selected items, be they bnd.bnd files, JAR files, or entries in the Repositories view. Click to open manual in the browser.");

	private static Action createHelpButton(String url, String tooltipText) {
		Action helpAction = new Action("Help", IAction.AS_PUSH_BUTTON) {
			@Override
			public void run() {
				Program.launch(url);
			}
		};
		helpAction.setEnabled(true);
		helpAction.setToolTipText(tooltipText);
		helpAction.setImageDescriptor(Icons.desc(ISharedImages.IMG_LCL_LINKTO_HELP));

		return helpAction;
	}

	private Buttons() {}
}

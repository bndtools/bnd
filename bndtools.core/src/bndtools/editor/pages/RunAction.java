package bndtools.editor.pages;

import org.bndtools.facade.ExtensionFacade;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;

public class RunAction extends Action {

	private final IEditorPart						editor;
	private final String							mode;
	private final ExtensionFacade<ILaunchShortcut2>	facade;

	public RunAction(IEditorPart editor, String mode) {
		super("Run OSGi", SWT.RIGHT);
		this.editor = editor;
		this.mode = mode;
		facade = new ExtensionFacade<>("bndtools.launch.RunShortcut", ILaunchShortcut2.class);
		facade.onClosedService((ref, obj) -> Display.getDefault()
			.syncExec(() -> setEnabled(facade.size() > 0)));
		facade.onNewService((ref, obj) -> Display.getDefault()
			.syncExec(() -> setEnabled(true)));
		setEnabled(facade.size() > 0);
	}

	@Override
	public void run() {
		facade.getRequiredService()
			.launch(editor, mode);
	}
}

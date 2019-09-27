package bndtools.editor.pages;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorPart;

import bndtools.launch.RunShortcut;

public class RunAction extends Action {

	private final IEditorPart	editor;
	private final String		mode;

	public RunAction(IEditorPart editor, String mode) {
		super("Run OSGi", SWT.RIGHT);
		this.editor = editor;
		this.mode = mode;
	}

	@Override
	public void run() {
		new RunShortcut().launch(editor, mode);
	}
}

package bndtools.wizards.bndfile;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class NewBndFileActionDelegate implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow		window;
	private IStructuredSelection	selection;

	@Override
	public void run(IAction action) {
		EmptyBndFileWizard wizard = new EmptyBndFileWizard();
		wizard.init(window.getWorkbench(), selection);

		WizardDialog dialog = new WizardDialog(window.getShell(), wizard);
		dialog.open();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			this.selection = (IStructuredSelection) selection;
		} else {
			this.selection = null;
		}
	}

	@Override
	public void dispose() {}

	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}

package org.bndtools.core.ui.wizards.index;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class GenerateIndexCommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structSel = (IStructuredSelection) selection;
			Object element = structSel.getFirstElement();
			if (element != null && element instanceof IContainer) {
				NewIndexWizard wizard = new NewIndexWizard();
				IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
				wizard.init(workbenchWindow.getWorkbench(), structSel);

				WizardDialog dialog = new WizardDialog(workbenchWindow.getShell(), wizard);
				dialog.open();
			}
		}
		return null;
	}

}

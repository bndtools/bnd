package org.bndtools.core.refactor.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class AdoptPomAction implements IObjectActionDelegate {

    private ISelection selection;
    private IWorkbenchPart targetPart;

    public void run(IAction action) {
        IFile pomFile = (IFile) ((IStructuredSelection) selection).getFirstElement();

        RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(new AdoptMavenProjectRefactoringWizard(pomFile));
        try {
            int run = operation.run(targetPart.getSite().getShell(), "Convert Maven Project");
        } catch (InterruptedException e) {
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        this.targetPart = targetPart;
    }

}

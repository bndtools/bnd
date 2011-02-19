package org.bndtools.core.refactor.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import bndtools.refactor.AdoptMavenPomProcessor;

public class AdoptMavenProjectRefactoringWizard extends RefactoringWizard {

    private final IFile pomFile;

    public AdoptMavenProjectRefactoringWizard(IFile pomFile) {
        super(new ProcessorBasedRefactoring(new AdoptMavenPomProcessor(pomFile)), WIZARD_BASED_USER_INTERFACE);
        this.pomFile = pomFile;
    }

    @Override
    protected void addUserInputPages() {
    }

    /*
    @Override
    public boolean performFinish() {
        try {
            final IProjectDescription desc = pomFile.getProject().getDescription();
            String[] natures = desc.getNatureIds();
            boolean found = false;
            for (String nature : natures) {
                if (BndProjectNature.NATURE_ID.equals(nature)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                String[] newNatures = new String[natures.length + 1];
                System.arraycopy(natures, 0, newNatures, 0, natures.length);
                newNatures[natures.length] = BndProjectNature.NATURE_ID;
                desc.setNatureIds(newNatures);

                getContainer().run(false, false, new IRunnableWithProgress() {
                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        try {
                            pomFile.getProject().setDescription(desc, monitor);
                        } catch (CoreException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
            }
        } catch (CoreException e) {
            ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error adding Bnd nature to project.", e));
            return false;
        } catch (InvocationTargetException e) {
            ErrorDialog.openError(getShell(), "Error", null, new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error adding Bnd nature to project.", e.getTargetException()));
            return false;
        } catch (InterruptedException e) {
            // Shouldn't happen because we're not forking
            e.printStackTrace();
            return false;
        }
        return super.performFinish();
    }
    */

}

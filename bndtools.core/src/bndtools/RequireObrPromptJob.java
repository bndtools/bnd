package bndtools;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import bndtools.wizards.workspace.RequiredObrIndexWizard;

public class RequireObrPromptJob extends UIJob {

    private final IProject project;
    private final List<String> urls;

    public RequireObrPromptJob(IProject project, List<String> urls) {
        super("requireObrPrompt");
        this.project = project;
        this.urls = urls;
    }

    @Override
    public IStatus runInUIThread(IProgressMonitor monitor) {
        RequiredObrIndexWizard wizard = new RequiredObrIndexWizard(project, urls);
        WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
        if (dialog.open() == Window.OK) {
            new AddObrIndexesToWorkspaceJob(wizard.getCheckedUrls()).schedule();
        }

        return Status.OK_STATUS;
    }

}

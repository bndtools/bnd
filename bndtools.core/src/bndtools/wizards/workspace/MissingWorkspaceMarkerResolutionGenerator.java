package bndtools.wizards.workspace;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class MissingWorkspaceMarkerResolutionGenerator implements IMarkerResolutionGenerator, IMarkerResolutionGenerator2 {

    @Override
    public boolean hasResolutions(IMarker marker) {
        return true;
    }

    @Override
    public IMarkerResolution[] getResolutions(IMarker marker) {
        return new IMarkerResolution[] {
                new IMarkerResolution() {
                    @Override
                    public void run(IMarker marker) {
                        IWorkbench workbench = PlatformUI.getWorkbench();
                        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

                        WorkspaceSetupWizard wizard = new WorkspaceSetupWizard();
                        wizard.init(workbench, StructuredSelection.EMPTY);
                        WizardDialog dialog = new WizardDialog(window.getShell(), wizard);
                        dialog.open();
                    }

                    @Override
                    public String getLabel() {
                        return "Open 'New Bnd OSGi Workspace' Wizard";
                    }
                }
        };
    }

}

package bndtools.wizards.bndfile;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.wizard.Wizard;

import aQute.bnd.build.Project;
import bndtools.api.IBndModel;

public class RunExportSelectionWizard extends Wizard {
    
    private final RunExportSelectionPage selectionPage;
    
    public RunExportSelectionWizard(IConfigurationElement[] configElems, IBndModel model, Project bndProject) {
        selectionPage = new RunExportSelectionPage("selection", configElems, model, bndProject);
        setForcePreviousAndNextButtons(true);
        
        addPage(selectionPage);
    }

    @Override
    public boolean performFinish() {
        return false;
    }

}

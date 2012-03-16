package org.bndtools.core.ui;

import org.eclipse.jface.wizard.Wizard;

import aQute.bnd.build.Project;
import bndtools.api.IBndModel;

public class AbstractRunDescriptionExportWizard extends Wizard implements IRunDescriptionExportWizard {

    private IBndModel model;
    private Project bndProject;

    public void setBndModel(IBndModel model, Project bndProject) {
        this.model = model;
        this.bndProject = bndProject;
    }
    
    @Override
    public boolean performFinish() {
        return false;
    }

}

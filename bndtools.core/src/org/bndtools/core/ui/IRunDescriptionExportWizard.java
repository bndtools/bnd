package org.bndtools.core.ui;

import org.eclipse.jface.wizard.IWizard;

import aQute.bnd.build.Project;
import bndtools.api.IBndModel;

public interface IRunDescriptionExportWizard extends IWizard {

    void setBndModel(IBndModel model, Project bndProject);

}

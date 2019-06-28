package org.bndtools.core.ui;

import org.eclipse.jface.wizard.IWizard;

import aQute.bnd.build.Project;
import aQute.bnd.build.model.BndEditModel;

public interface IRunDescriptionExportWizard extends IWizard {

	void setBndModel(BndEditModel model, Project bndProject);

}

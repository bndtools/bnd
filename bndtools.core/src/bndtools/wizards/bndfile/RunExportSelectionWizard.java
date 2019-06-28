package bndtools.wizards.bndfile;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.wizard.Wizard;

import aQute.bnd.build.Project;
import aQute.bnd.build.model.BndEditModel;

public class RunExportSelectionWizard extends Wizard {

	private final RunExportSelectionPage selectionPage;

	public RunExportSelectionWizard(IConfigurationElement[] configElems, BndEditModel model, Project bndProject) {
		selectionPage = new RunExportSelectionPage("selection", configElems, model, bndProject);
		setForcePreviousAndNextButtons(true);
		setNeedsProgressMonitor(true);

		addPage(selectionPage);
	}

	@Override
	public boolean performFinish() {
		return false;
	}

}

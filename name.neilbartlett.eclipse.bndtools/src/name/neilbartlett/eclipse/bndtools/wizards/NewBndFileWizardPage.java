package name.neilbartlett.eclipse.bndtools.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

public class NewBndFileWizardPage extends WizardNewFileCreationPage {

	public NewBndFileWizardPage(String pageName, IStructuredSelection selection) {
		super(pageName, selection);
	}
	
	@Override
	protected String getNewFileLabel() {
		return "Bnd File:";
	}
}

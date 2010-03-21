package name.neilbartlett.eclipse.bndtools.wizards;

import name.neilbartlett.eclipse.bndtools.editor.project.RepoBundleSelectionWizardPage;

public class NewBndProjectWizardBundlesPage extends RepoBundleSelectionWizardPage {

	private final BndWorkspaceConfigurationPage cnfPage;
	private boolean firstShown = true;

	public NewBndProjectWizardBundlesPage(String pageName, BndWorkspaceConfigurationPage cnfPage) {
		super(pageName);
		this.cnfPage = cnfPage;
	}

	@Override
	public void setVisible(boolean visible) {
		if(visible && firstShown) {
			cnfPage.createCnfProject();
			firstShown = false;
		}
		super.setVisible(visible);
	}
}

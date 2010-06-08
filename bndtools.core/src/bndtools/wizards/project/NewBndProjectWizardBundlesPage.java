package bndtools.wizards.project;

import bndtools.Plugin;
import bndtools.utils.SWTConcurrencyUtil;
import bndtools.wizards.repo.RepoBundleSelectionWizardPage;
import bndtools.wizards.workspace.InitialiseCnfProjectWizard;

public class NewBndProjectWizardBundlesPage extends RepoBundleSelectionWizardPage {

	private volatile boolean createdCnf = false;

	public NewBndProjectWizardBundlesPage(String pageName) {
		super(pageName);
	}

	@Override
	public void setVisible(boolean visible) {
		if(visible) {
			doCreateCnfIfNeeded();
		}
		super.setVisible(visible);
	}
	void doCreateCnfIfNeeded() {
		if(!createdCnf) {
			createdCnf = true;
            SWTConcurrencyUtil.execForDisplay(getShell().getDisplay(), new Runnable() {
                public void run() {
                    try {
                        InitialiseCnfProjectWizard wizard = new InitialiseCnfProjectWizard();
                        wizard.showIfNeeded(false);
                        refreshBundleList();
                    } catch (Exception e) {
                        Plugin.logError("Error refreshing repository display.", e);
                    }
                }
            });
		}
	}
}
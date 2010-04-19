package bndtools.wizards;

import bndtools.Plugin;
import bndtools.editor.project.RepoBundleSelectionWizardPage;
import bndtools.utils.SWTConcurrencyUtil;

public class NewBndProjectWizardBundlesPage extends RepoBundleSelectionWizardPage {

	private final BndWorkspaceConfigurationPage cnfPage;
	private volatile boolean createdCnf = false;

	public NewBndProjectWizardBundlesPage(String pageName, BndWorkspaceConfigurationPage cnfPage) {
		super(pageName);
		this.cnfPage = cnfPage;
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
			cnfPage.createCnfProject();
			createdCnf = true;
            SWTConcurrencyUtil.execForDisplay(getShell().getDisplay(), new Runnable() {
                public void run() {
                    try {
                        refreshBundleList();
                    } catch (Exception e) {
                        Plugin.logError("Error refreshing repository display.", e);
                    }
                }
            });
		}
	}
}
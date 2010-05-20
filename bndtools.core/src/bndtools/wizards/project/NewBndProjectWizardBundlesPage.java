package bndtools.wizards.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardDialog;

import aQute.bnd.build.Project;
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
                        IProject cnf = ResourcesPlugin.getWorkspace().getRoot().getProject(Project.BNDCNF);
                        if(cnf == null || !cnf.exists() || !cnf.isOpen()) {
                            IPreferenceStore store = Plugin.getDefault().getPreferenceStore();
                            boolean hideWizard = store.getBoolean(Plugin.PREF_HIDE_INITIALISE_CNF_WIZARD);

                            if(!hideWizard) {
                                InitialiseCnfProjectWizard wizard = new InitialiseCnfProjectWizard();
                                WizardDialog dialog = new WizardDialog(getShell(), wizard);
                                dialog.open();
                            }
                        }

                        refreshBundleList();
                    } catch (Exception e) {
                        Plugin.logError("Error refreshing repository display.", e);
                    }
                }
            });
		}
	}
}
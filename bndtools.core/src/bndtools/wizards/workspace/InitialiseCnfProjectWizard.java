package bndtools.wizards.workspace;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import bndtools.Plugin;

public class InitialiseCnfProjectWizard extends Wizard implements IImportWizard {

    private final InitialiseCnfProjectIntroWizardPage introPage = new InitialiseCnfProjectIntroWizardPage("introPage"); //$NON-NLS-1$
	private final RepositorySelectionPage repoPage = new RepositorySelectionPage("repoPage"); //$NON-NLS-1$

	private IWorkbench workbench;
	private IStructuredSelection selection;

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}
	@Override
	public void addPages() {
	    addPage(introPage);
		addPage(repoPage);
	}
	@Override
	public boolean performFinish() {
		return repoPage.createCnfProject();
	}
	@Override
	public boolean performCancel() {
	    IPreferenceStore store = Plugin.getDefault().getPreferenceStore();
	    boolean hide = store.getBoolean(Plugin.PREF_HIDE_INITIALISE_CNF_ADVICE);

	    if(!hide) {
	        MessageDialogWithToggle dialog = MessageDialogWithToggle.openInformation(getShell(), Messages.InitialiseCnfProjectWizard_info_dialog_popup, Messages.InitialiseCnfProjectWizard_info_dialog_message, Messages.InitialiseCnfProjectWizard_info_dialog_donotshow, false, null, null);
	        if(dialog.getToggleState()) {
	            store.setValue(Plugin.PREF_HIDE_INITIALISE_CNF_ADVICE, true);
	        }
	    }
	    return super.performCancel();
	}
}
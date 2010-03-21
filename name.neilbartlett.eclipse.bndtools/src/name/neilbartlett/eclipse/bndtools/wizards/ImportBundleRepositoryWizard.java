package name.neilbartlett.eclipse.bndtools.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class ImportBundleRepositoryWizard extends Wizard implements IImportWizard {
	
	private final BndWorkspaceConfigurationPage repoPage = new BndWorkspaceConfigurationPage("repoPage");

	private IWorkbench workbench;
	private IStructuredSelection selection;

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.workbench = workbench;
		this.selection = selection;
	}
	@Override
	public void addPages() {
		addPage(repoPage);
	}
	@Override
	public boolean performFinish() {
		return repoPage.createCnfProject();
	}
}
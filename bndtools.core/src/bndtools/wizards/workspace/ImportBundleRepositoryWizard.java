package bndtools.wizards.workspace;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class ImportBundleRepositoryWizard extends Wizard implements IImportWizard {

    private final RepositorySelectionPage repoPage = new RepositorySelectionPage("repoPage");
    private final RemoteRepositoryBundleSelectionPage bundlePage = new RemoteRepositoryBundleSelectionPage("bundlePage");

    private IWorkbench workbench;
    private IStructuredSelection selection;

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
        this.selection = selection;
    }

    @Override
    public boolean needsProgressMonitor() {
        return true;
    }

    void setDescription(String description) {
        repoPage.setDescription(description);
    }

    @Override
    public void addPages() {
        repoPage.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                bundlePage.setRepository(repoPage.getSelectedRepository());
            }
        });

        addPage(repoPage);
        addPage(bundlePage);
    }

    @Override
    public boolean performFinish() {
        return repoPage.createCnfProject();
    }
}
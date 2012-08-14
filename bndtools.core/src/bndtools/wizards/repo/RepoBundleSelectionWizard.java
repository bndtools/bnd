package bndtools.wizards.repo;

import java.util.List;

import org.eclipse.jface.wizard.Wizard;

import aQute.bnd.build.Project;
import aQute.bnd.build.model.clauses.VersionedClause;

public class RepoBundleSelectionWizard extends Wizard {

    private final RepoBundleSelectionWizardPage selectionPage = new RepoBundleSelectionWizardPage();

    /**
     * Create a wizard for editing the specified list of bundles. The supplied collection will be modified by this
     * wizard.
     * 
     * @param bundles
     *            A mutable collection of bundles.
     * @throws Exception
     */
    public RepoBundleSelectionWizard(@SuppressWarnings("unused") final Project project, List<VersionedClause> bundles) throws Exception {
        selectionPage.setSelectedBundles(bundles);
        addPage(selectionPage);
    }

    @Override
    public boolean performFinish() {
        return true;
    }

    public void setSelectionPageTitle(String title) {
        selectionPage.setTitle(title);
    }

    public void setSelectionPageDescription(String description) {
        selectionPage.setDescription(description);
    }

    public List<VersionedClause> getSelectedBundles() {
        return selectionPage.getSelectedBundles();
    }

}
package bndtools.wizards.repo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.utils.log.Logger;
import org.eclipse.jface.wizard.Wizard;
import org.osgi.framework.BundleContext;

import aQute.bnd.build.Project;
import bndtools.Plugin;
import bndtools.bindex.BundleSelectionIndexer;
import bndtools.bindex.LocalRepositoryIndexer;
import bndtools.model.clauses.VersionedClause;
import bndtools.wizards.workspace.DependentResourcesWizardPage;

public class RepoBundleSelectionWizard extends Wizard {

    private final RepositoryAdmin repoAdmin;

	private final RepoBundleSelectionWizardPage selectionPage = new RepoBundleSelectionWizardPage("bundleSelect");
	private final DependentResourcesWizardPage requirementsPage;

    private final boolean useResolver;

    /**
     * Create a wizard for editing the specified list of bundles. The supplied
     * collection will be modified by this wizard.
     *
     * @param bundles
     *            A mutable collection of bundles.
     */
    public RepoBundleSelectionWizard(final Project project, List<VersionedClause> bundles, boolean useResolver) {
        this.useResolver = useResolver;

        BundleContext context = Plugin.getDefault().getBundleContext();
        repoAdmin = new RepositoryAdminImpl(context, new Logger(context));
        final BundleSelectionIndexer selectionIndexer = new BundleSelectionIndexer(project);
        requirementsPage = new DependentResourcesWizardPage(repoAdmin, selectionIndexer);

        selectionPage.setSelectedBundles(bundles);
        addPage(selectionPage);

        if (useResolver) {
            LocalRepositoryIndexer repoIndexer = new LocalRepositoryIndexer(true);

            addPage(requirementsPage);

            selectionPage.addPropertyChangeListener(RepoBundleSelectionWizardPage.PROP_SELECTION, new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    List<VersionedClause> sel = selectionPage.getSelectedBundles();
                    selectionIndexer.setSelection(sel);
                    requirementsPage.setSelectedBundles(project, sel);
                }
            });
        }
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
        List<VersionedClause> result;

        if (useResolver) {
            List<VersionedClause> selected = selectionPage.getSelectedBundles();
            List<Resource> required = requirementsPage.getRequired();
            Resolver resolver = requirementsPage.getResolver();

            result = new ArrayList<VersionedClause>(selected.size() + required.size());
            result.addAll(selected);

            for (Resource resource : required) {
                String bsn = resource.getSymbolicName();
                Reason[] reason = resolver.getReason(resource);
                // WTF
            }
        } else {
            result = selectionPage.getSelectedBundles();
        }
        return result;
    }

}
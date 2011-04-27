package bndtools.wizards.repo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.utils.log.Logger;
import org.eclipse.jface.wizard.Wizard;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import aQute.bnd.build.Project;
import bndtools.Plugin;
import bndtools.bindex.BundleSelectionIndexer;
import bndtools.model.clauses.VersionedClause;
import bndtools.wizards.workspace.DependentResourcesWizardPage;

public class RepoBundleSelectionWizard extends Wizard {

    private final RepositoryAdmin repoAdmin;

	private final Project sourceProject;
    private final boolean useResolver;

    private final RepoBundleSelectionWizardPage selectionPage;
    private final DependentResourcesWizardPage requirementsPage;

    /**
     * Create a wizard for editing the specified list of bundles. The supplied
     * collection will be modified by this wizard.
     *
     * @param bundles
     *            A mutable collection of bundles.
     */
    public RepoBundleSelectionWizard(final Project project, List<VersionedClause> bundles, boolean useResolver) {
        this.sourceProject = project;
        this.useResolver = useResolver;

        selectionPage = new RepoBundleSelectionWizardPage(project);

        BundleContext context = Plugin.getDefault().getBundleContext();
        repoAdmin = new RepositoryAdminImpl(context, new Logger(context));
        final BundleSelectionIndexer selectionIndexer = new BundleSelectionIndexer(project);
        requirementsPage = new DependentResourcesWizardPage(repoAdmin, selectionIndexer);

        selectionPage.setSelectedBundles(bundles);
        addPage(selectionPage);

        if (useResolver) {
            requirementsPage.addRepositoryIndexProvider(new LocalRepositoryIndexProvider());
            selectionIndexer.setSelection(bundles);
            requirementsPage.setSelectedBundles(project, bundles);
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
            Collection<Resource> selected = requirementsPage.getSelected();
            List<Resource> required = requirementsPage.getRequired();

            result = new ArrayList<VersionedClause>(selected.size() + required.size());
            for (Resource resource : selected) {
                result.add(toVersionedClause(resource));
            }
            for (Resource resource : required) {
                result.add(toVersionedClause(resource));
            }
        } else {
            result = selectionPage.getSelectedBundles();
        }
        return result;
    }

    VersionedClause toVersionedClause(Resource resource) {
        String bsn = resource.getSymbolicName();
        Version version = resource.getVersion();

        VersionedClause clause = new VersionedClause(bsn, new HashMap<String, String>());
        if (version != null)
            clause.setVersionRange(version.toString());
        return clause;
    }

}
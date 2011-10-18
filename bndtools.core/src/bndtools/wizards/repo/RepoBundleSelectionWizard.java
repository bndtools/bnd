package bndtools.wizards.repo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.utils.log.Logger;
import org.eclipse.jface.wizard.Wizard;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.service.OBRResolutionMode;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import bndtools.Plugin;
import bndtools.bindex.IRepositoryIndexProvider;
import bndtools.bindex.WorkspaceIndexer;
import bndtools.model.clauses.VersionedClause;
import bndtools.wizards.workspace.DependentResourcesWizardPage;
import bndtools.wizards.workspace.RepositoryResourceRequestor;

public class RepoBundleSelectionWizard extends Wizard {

    private static final String WORKSPACE_CATEGORY = "__workspace__";

    private final RepositoryAdmin repoAdmin;

    private final boolean useResolver;

    private final RepoBundleSelectionWizardPage selectionPage;
    private final DependentResourcesWizardPage requirementsPage;

    /**
     * Create a wizard for editing the specified list of bundles. The supplied
     * collection will be modified by this wizard.
     *
     * @param bundles
     *            A mutable collection of bundles.
     * @throws Exception
     */
    public RepoBundleSelectionWizard(final Project project, List<VersionedClause> bundles, boolean useResolver, EnumSet<OBRResolutionMode> resolutionModes) throws Exception {
        this.useResolver = useResolver;

        selectionPage = new RepoBundleSelectionWizardPage(project, resolutionModes);

        BundleContext context = Plugin.getDefault().getBundleContext();
        repoAdmin = new RepositoryAdminImpl(new DummyBundleContext(), new Logger(context));

        // Setup repository indexers / index providers
        PluginIndexProvider localRepoIndex = new PluginIndexProvider(resolutionModes);
        WorkspaceIndexer workspaceIndex = new WorkspaceIndexer(WORKSPACE_CATEGORY);
        final List<IRepositoryIndexProvider> indexList = Arrays.asList(new IRepositoryIndexProvider[] { localRepoIndex, workspaceIndex });

        Container systemBundle = project.getBundle("org.apache.felix.framework", null, Strategy.HIGHEST, null);
        requirementsPage = new DependentResourcesWizardPage(repoAdmin, systemBundle.getFile(), indexList);

        if (project.getRunBuilds()) {
            File[] builds = project.build();
            selectionPage.setImplicitBundles(builds);
        }
        selectionPage.setSelectedBundles(bundles);
        addPage(selectionPage);

        if (useResolver) {
            requirementsPage.addRepositoryIndexProvider(localRepoIndex);
            requirementsPage.addRepositoryIndexProvider(workspaceIndex);

            requirementsPage.setSelectedResourcesRequestor(new RepositoryResourceRequestor(repoAdmin, indexList, bundles, project));
            addPage(requirementsPage);

            selectionPage.addPropertyChangeListener(RepoBundleSelectionWizardPage.PROP_SELECTION, new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    List<VersionedClause> sel = selectionPage.getSelectedBundles();
                    requirementsPage.setSelectedResourcesRequestor(new RepositoryResourceRequestor(repoAdmin, indexList, sel, project));
                }
            });
        }
        setNeedsProgressMonitor(true);
    }

	File getSystemBundleFile(Project project) {
	    try {
            Container container = project.getBundle("org.apache.felix.framework", null, Strategy.HIGHEST, null);
            return container.getFile();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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

        boolean workspace = false;
        for (String category : resource.getCategories()) {
            if (WORKSPACE_CATEGORY.equals(category)) {
                workspace = true;
                break;
            }
        }

        VersionedClause clause = new VersionedClause(bsn, new HashMap<String, String>());
        if (workspace) {
            clause.setVersionRange("latest");
        } else {
            Version version = resource.getVersion();
            if (version != null) {
                Version newVersion = new Version(version.getMajor(), version.getMinor(), version.getMicro());
                clause.setVersionRange(newVersion.toString());
            }
        }
        return clause;
    }

}
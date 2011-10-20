package bndtools.wizards.obr;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.bundlerepository.Resource;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.wizard.Wizard;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import aQute.libg.version.VersionRange;
import bndtools.api.IBndModel;
import bndtools.model.clauses.VersionedClause;

public class ObrResolutionWizard extends Wizard {

    private final ObrIndexSelectionPage indexSelectionPage = new ObrIndexSelectionPage();
    private final ObrResultsWizardPage resultsPage;
    private final IBndModel model;

    public ObrResolutionWizard(IBndModel model, IFile file) {
        this.model = model;

        resultsPage = new ObrResultsWizardPage(model, file);
        resultsPage.setRepositories(indexSelectionPage.getSelectedRepos());

        setWindowTitle("Resolve");
        setNeedsProgressMonitor(true);

        addPage(indexSelectionPage);
        addPage(resultsPage);

        indexSelectionPage.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                resultsPage.setRepositories(indexSelectionPage.getSelectedRepos());
            }
        });
    }

    @Override
    public boolean performFinish() {
        List<Resource> resources = resultsPage.getSelectedResources();
        List<VersionedClause> runBundles = new ArrayList<VersionedClause>(resources.size());

        for (Resource resource : resources) {
            VersionedClause runBundle = resourceToRunBundle(resource);
            runBundles.add(runBundle);
        }
        model.setRunBundles(runBundles);
        return true;
    }

    private VersionedClause resourceToRunBundle(Resource resource) {
        String bsn = resource.getSymbolicName();

        Map<String, String> attribs = new HashMap<String, String>();
        VersionRange versionRange = createVersionRange(resource.getVersion());
        attribs.put(Constants.VERSION_ATTRIBUTE, versionRange.toString());

        return new VersionedClause(bsn, attribs);

    }

    private VersionRange createVersionRange(Version version) {
        Version base = new Version(version.getMajor(), version.getMinor(), version.getMicro());
        Version next = new Version(version.getMajor(), version.getMinor(), version.getMicro() + 1);

        return new VersionRange(String.format("[%s,%s)", base, next));
    }

}

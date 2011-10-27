package bndtools.wizards.obr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.bundlerepository.Resource;
import org.bndtools.core.obr.ObrResolutionResult;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.wizard.Wizard;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import aQute.libg.version.VersionRange;
import bndtools.BndConstants;
import bndtools.WorkspaceObrProvider;
import bndtools.api.IBndModel;
import bndtools.model.clauses.VersionedClause;

public class ObrResolutionWizard extends Wizard {

    private final ObrResultsWizardPage resultsPage;
    private final IBndModel model;

    public ObrResolutionWizard(IBndModel model, IFile file, ObrResolutionResult result) {
        this.model = model;

        resultsPage = new ObrResultsWizardPage(model, file);
        resultsPage.setResult(result);

        setWindowTitle("Resolve");
        setNeedsProgressMonitor(true);

        addPage(resultsPage);
    }

    @Override
    public boolean performFinish() {
        ObrResolutionResult result = resultsPage.getResult();
        if (result != null) {
            List<Resource> resources = result.getRequired();
            List<VersionedClause> runBundles = new ArrayList<VersionedClause>(resources.size());

            for (Resource resource : resources) {
                VersionedClause runBundle = resourceToRunBundle(resource);
                runBundles.add(runBundle);
            }
            model.setRunBundles(runBundles);
            return true;
        }
        return false;
    }

    private VersionedClause resourceToRunBundle(Resource resource) {
        String bsn = resource.getSymbolicName();

        // Map version range string, using "latest" for any workspace resources
        Map<String, String> attribs = new HashMap<String, String>();
        String versionRangeStr;
        if (isWorkspace(resource)) {
            versionRangeStr = "latest";
        } else {
            VersionRange versionRange = createVersionRange(resource.getVersion());
            versionRangeStr = versionRange.toString();
        }
        attribs.put(Constants.VERSION_ATTRIBUTE, versionRangeStr);

        // Add the resolved URI of the resource
        attribs.put(BndConstants.RESOLUTION_URI_ATTRIBUTE, resource.getURI());

        return new VersionedClause(bsn, attribs);

    }

    private boolean isWorkspace(Resource resource) {
        String[] categories = resource.getCategories();
        for (String category : categories) {
            if (WorkspaceObrProvider.CATEGORY_WORKSPACE.equals(category))
                return true;
        }
        return false;
    }

    private VersionRange createVersionRange(Version version) {
        Version base = new Version(version.getMajor(), version.getMinor(), version.getMicro());
        Version next = new Version(version.getMajor(), version.getMinor(), version.getMicro() + 1);

        return new VersionRange(String.format("[%s,%s)", base, next));
    }

}

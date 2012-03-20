package bndtools.wizards.obr;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.bundlerepository.Resource;
import org.bndtools.core.obr.ObrResolutionResult;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.wizard.Wizard;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.io.IO;
import aQute.libg.header.Attrs;
import aQute.libg.version.VersionRange;
import bndtools.BndConstants;
import bndtools.Plugin;
import bndtools.WorkspaceObrProvider;
import bndtools.api.IBndModel;
import bndtools.model.clauses.VersionedClause;

public class ObrResolutionWizard extends Wizard {

    private static final String PATHS_EXTENSION = ".resolved";

    private final ObrResultsWizardPage resultsPage;
    private final Comparator<Entry<String, String>> clauseAttributeSorter = new Comparator<Map.Entry<String,String>>() {
        public int compare(Entry<String, String> e1, Entry<String, String> e2) {
            // Reverse lexical ordering on keys
            return e2.getKey().compareTo(e1.getKey());
        }
    };

    private final IBndModel model;
    private final IFile file;

    public ObrResolutionWizard(IBndModel model, IFile file, ObrResolutionResult result) {
        this.model = model;
        this.file = file;

        resultsPage = new ObrResultsWizardPage(model, file);
        resultsPage.setResult(result);

        setWindowTitle("Resolve");
        setNeedsProgressMonitor(true);

        addPage(resultsPage);
    }

    @Override
    public boolean performFinish() {
        List<Resource> resources;

        ObrResolutionResult result = resultsPage.getResult();
        if (result != null && result.isResolved())
            resources = result.getRequired();
        else
            resources = Collections.emptyList();

        // Open stream for physical paths list in target dir
        PrintStream pathsStream = null;
        try {
            Project project = Workspace.getProject(file.getProject().getLocation().toFile());
            File targetDir = project.getTarget();
            targetDir.mkdirs();

            File pathsFile = new File(targetDir, file.getName() + PATHS_EXTENSION);
            pathsStream = new PrintStream(pathsFile);
        } catch (Exception e) {
            Plugin.logError("Unable to write resolved path list in target directory for project " + file.getProject().getName(), e);
        }

        // Generate -runbundles and path list
        try {
            List<VersionedClause> runBundles = new ArrayList<VersionedClause>(resources.size());
            for (Resource resource : resources) {
                VersionedClause runBundle = resourceToRunBundle(resource);
                runBundles.add(runBundle);

                if (pathsStream != null) {
                    VersionedClause runBundleWithUri = runBundle.clone();
                    runBundleWithUri.getAttribs().put(BndConstants.RESOLUTION_URI_ATTRIBUTE, resource.getURI());

                    StringBuilder builder = new StringBuilder();
                    runBundleWithUri.formatTo(builder, clauseAttributeSorter);

                    pathsStream.println(builder.toString());
                }
            }
            model.setRunBundles(runBundles);
        } finally {
            IO.close(pathsStream);
        }

        return true;
    }

    private VersionedClause resourceToRunBundle(Resource resource) {
        String bsn = resource.getSymbolicName();

        // Map version range string, using "latest" for any workspace resources
        Attrs attribs = new Attrs();
        String versionRangeStr;
        if (isWorkspace(resource)) {
            versionRangeStr = "latest";
        } else {
            VersionRange versionRange = createVersionRange(resource.getVersion());
            versionRangeStr = versionRange.toString();
        }
        attribs.put(Constants.VERSION_ATTRIBUTE, versionRangeStr);

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

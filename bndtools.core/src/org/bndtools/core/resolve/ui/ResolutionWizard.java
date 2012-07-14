package org.bndtools.core.resolve.ui;

import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bndtools.core.resolve.ResolutionResult;
import org.bndtools.core.utils.resources.ResourceUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.wizard.Wizard;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.osgi.VersionRange;
import aQute.lib.io.IO;
import bndtools.BndConstants;
import bndtools.Logger;
import bndtools.api.ILogger;

public class ResolutionWizard extends Wizard {
    private static final ILogger logger = Logger.getLogger();

    private static final String PATHS_EXTENSION = ".resolved";

    private final ResolutionResultsWizardPage resultsPage;
    private final Comparator<Entry<String,String>> clauseAttributeSorter = new Comparator<Map.Entry<String,String>>() {
        public int compare(Entry<String,String> e1, Entry<String,String> e2) {
            /*
             * Reverse lexical ordering on keys
             */
            return e2.getKey().compareTo(e1.getKey());
        }
    };

    private final BndEditModel model;
    private final IFile file;

    public ResolutionWizard(BndEditModel model, IFile file, ResolutionResult result) {
        this.model = model;
        this.file = file;

        resultsPage = new ResolutionResultsWizardPage(model);
        resultsPage.setResult(result);

        setWindowTitle("Resolve");
        setNeedsProgressMonitor(true);

        addPage(resultsPage);
    }

    @Override
    public boolean performFinish() {
        Collection<Resource> resources;

        ResolutionResult result = resultsPage.getResult();
        if (result != null && result.getOutcome() == ResolutionResult.Outcome.Resolved)
            resources = result.getResolve().getRequiredResources();
        else
            resources = Collections.emptyList();

        // Open stream for physical paths list in target dir
        PrintStream pathsStream = null;
        try {
            Project project = Workspace.getProject(file.getProject().getLocation().toFile());
            File targetDir = project.getTarget();
            targetDir.mkdirs();

            File pathsFile = new File(targetDir, file.getName() + PATHS_EXTENSION);
            pathsStream = new PrintStream(pathsFile, "UTF-8");
        } catch (Exception e) {
            logger.logError("Unable to write resolved path list in target directory for project " + file.getProject().getName(), e);
        }

        // Generate -runbundles and path list
        try {
            List<VersionedClause> runBundles = new ArrayList<VersionedClause>(resources.size());
            for (Resource resource : resources) {
                VersionedClause runBundle = resourceToRunBundle(resource);
                runBundles.add(runBundle);

                if (pathsStream != null) {
                    VersionedClause runBundleWithUri = runBundle.clone();
                    URI uri = ResourceUtils.getURI(ResourceUtils.getContentCapability(resource));
                    runBundleWithUri.getAttribs().put(BndConstants.RESOLUTION_URI_ATTRIBUTE, uri.toString());

                    StringBuilder builder = new StringBuilder();
                    runBundleWithUri.formatTo(builder, clauseAttributeSorter);

                    pathsStream.println(builder.toString());
                }
            }
            model.setRunBundles(runBundles);
        } finally {
            if (pathsStream != null) {
                IO.close(pathsStream);
            }
        }

        return true;
    }

    private static VersionedClause resourceToRunBundle(Resource resource) {
        Capability idCap = ResourceUtils.getIdentityCapability(resource);
        String identity = ResourceUtils.getIdentity(idCap);

        // Map version range string, using "latest" for any workspace resources
        Attrs attribs = new Attrs();
        String versionRangeStr;
        if (isWorkspace(resource)) {
            versionRangeStr = "latest";
        } else {
            Version version = ResourceUtils.getVersion(idCap);
            VersionRange versionRange = createVersionRange(version);
            versionRangeStr = versionRange.toString();
        }
        attribs.put(Constants.VERSION_ATTRIBUTE, versionRangeStr);

        return new VersionedClause(identity, attribs);

    }

    private static boolean isWorkspace(Resource resource) {
        /*
         * TODO String[] categories = resource.getCategories(); for (String category : categories) { if
         * (WorkspaceObrProvider.CATEGORY_WORKSPACE.equals(category)) return true; }
         */
        return false;
    }

    private static VersionRange createVersionRange(Version version) {
        Version base = new Version(version.getMajor(), version.getMinor(), version.getMicro());
        Version next = new Version(version.getMajor(), version.getMinor(), version.getMicro() + 1);

        return new VersionRange(String.format("[%s,%s)", base, next));
    }

}

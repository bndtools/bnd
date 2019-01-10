package org.bndtools.core.resolve.ui;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.core.resolve.ResolutionResult;
import org.bndtools.utils.resources.ResourceUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.wizard.Wizard;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import aQute.bnd.build.Project;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.header.Attrs;
import aQute.bnd.version.VersionRange;
import aQute.lib.io.IO;
import bndtools.BndConstants;

public class ResolutionWizard extends Wizard {

    private static final String VERSION_SNAPSHOT = "snapshot";
    private static final String CAPABILITY_WORKSPACE = "bndtools.workspace";
    private static final String RESOLVED_PATHS_EXTENSION = ".resolved";

    private final ILogger logger = Logger.getLogger(ResolutionWizard.class);

    private final ResolutionResultsWizardPage resultsPage;
    private final Comparator<Entry<String, String>> clauseAttributeSorter = new Comparator<Map.Entry<String, String>>() {
        @Override
        public int compare(Entry<String, String> e1, Entry<String, String> e2) {
            // Reverse lexical ordering on keys
            return e2.getKey()
                .compareTo(e1.getKey());
        }
    };

    private final BndEditModel model;
    private final IFile file;
    private boolean preserveRunBundleUnresolved;

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
        if (result != null && result.getOutcome() == ResolutionResult.Outcome.Resolved) {
            resources = result.getResourceWirings()
                .keySet();
        } else if (preserveRunBundleUnresolved) {
            return true;
        } else {
            resources = Collections.emptyList();
        }

        // Open stream for physical paths list in target dir
        PrintStream pathsStream = null;
        try {
            File targetDir;

            Project bndProject = model.getProject();
            targetDir = bndProject.getTargetDir();
            if (targetDir == null)
                targetDir = file.getLocation()
                    .toFile()
                    .getParentFile();

            if (!targetDir.exists() && !targetDir.mkdirs()) {
                throw new IOException("Could not create target directory " + targetDir);
            }

            File pathsFile = new File(targetDir, file.getName() + RESOLVED_PATHS_EXTENSION);
            pathsStream = new PrintStream(pathsFile, "UTF-8");
        } catch (Exception e) {
            logger.logError("Unable to write resolved path list in target directory for project " + file.getProject()
                .getName(), e);
        }

        // Generate -runbundles and path list
        try {
            List<VersionedClause> runBundles = new ArrayList<VersionedClause>(resources.size());
            for (Resource resource : resources) {
                VersionedClause runBundle = resourceToRunBundle(resource);

                // [cs] Skip dups
                if (runBundles.contains(runBundle)) {
                    continue;
                }
                runBundles.add(runBundle);

                if (pathsStream != null) {
                    URI uri;
                    try {
                        uri = ResourceUtils.getURI(ResourceUtils.getContentCapability(resource));
                    } catch (IllegalArgumentException e) {
                        logger.logError("Resource has no content capability: " + ResourceUtils.getIdentity(resource), e);
                        continue;
                    }
                    VersionedClause runBundleWithUri = runBundle.clone();
                    runBundleWithUri.getAttribs()
                        .put(BndConstants.RESOLUTION_URI_ATTRIBUTE, uri.toString());

                    StringBuilder builder = new StringBuilder();
                    runBundleWithUri.formatTo(builder, clauseAttributeSorter);

                    pathsStream.println(builder.toString());
                }
            }
            Collections.sort(runBundles, new Comparator<VersionedClause>() {
                @Override
                public int compare(VersionedClause vc1, VersionedClause vc2) {
                    int diff = vc1.getName()
                        .compareTo(vc2.getName());
                    if (diff != 0)
                        return diff;
                    String r1 = vc1.getVersionRange();
                    if (r1 == null)
                        r1 = "";
                    String r2 = vc2.getVersionRange();
                    if (r2 == null)
                        r2 = "";
                    return r1.compareTo(r2);
                }
            });
            // Do not change the order of existing runbundles because they migh have been ordered manually
            List<VersionedClause> diffAddBundles = new ArrayList<>(runBundles);

            List<VersionedClause> oldRunBundles = model.getRunBundles();
            if (oldRunBundles == null)
                oldRunBundles = Collections.emptyList();
            else
                diffAddBundles.removeAll(oldRunBundles);

            List<VersionedClause> diffRemvedBundles = new ArrayList<>(oldRunBundles);
            diffRemvedBundles.removeAll(runBundles);
            List<VersionedClause> updatedRunBundles = new ArrayList<>(oldRunBundles);
            updatedRunBundles.addAll(diffAddBundles);
            updatedRunBundles.removeAll(diffRemvedBundles);
            // do not use getRunBundles().addAll, because it will not reflect in UI or File
            model.setRunBundles(updatedRunBundles);
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
            versionRangeStr = VERSION_SNAPSHOT;
        } else {
            Version version = ResourceUtils.getVersion(idCap);
            VersionRange versionRange = createVersionRange(version);
            versionRangeStr = versionRange.toString();
        }
        attribs.put(Constants.VERSION_ATTRIBUTE, versionRangeStr);

        return new VersionedClause(identity, attribs);

    }

    private static boolean isWorkspace(Resource resource) {
        List<Capability> workspaceCaps = resource.getCapabilities(CAPABILITY_WORKSPACE);
        return workspaceCaps != null && !workspaceCaps.isEmpty();
    }

    private static VersionRange createVersionRange(Version version) {
        Version base = new Version(version.getMajor(), version.getMinor(), version.getMicro());
        Version next = new Version(version.getMajor(), version.getMinor(), version.getMicro() + 1);

        return new VersionRange(String.format("[%s,%s)", base, next));
    }

    public void setAllowFinishUnresolved(boolean allowFinishUnresolved) {
        resultsPage.setAllowCompleteUnresolved(allowFinishUnresolved);
    }

    public void setPreserveRunBundlesUnresolved(boolean preserve) {
        this.preserveRunBundleUnresolved = preserve;
    }

}

package bndtools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.stream.StreamResult;

import net.jcip.annotations.ThreadSafe;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.service.bindex.BundleIndexer;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.OBRIndexProvider;
import aQute.bnd.service.OBRResolutionMode;
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.io.IO;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Instruction;
import aQute.lib.osgi.Jar;
import aQute.libg.sax.SAXUtil;
import aQute.libg.version.Version;
import aQute.libg.version.VersionRange;
import bndtools.api.ILogger;
import bndtools.bindex.AbsoluteizeContentFilter;
import bndtools.bindex.CategoryInsertionContentFilter;
import bndtools.types.Pair;

@ThreadSafe
public class WorkspaceObrProvider implements RepositoryPlugin, OBRIndexProvider {

    private static final String RANGE_SNAPSHOT = "snapshot";
    private static final String RANGE_LATEST = "latest";
    private static final String RANGE_PROJECT = "project";
    private static final String NAME = "Workspace";
    private static final String INDEX_FILENAME = "ws-obr-index.xml";
    // Generate warnings if the index generation takes longer than this
    // (millisecs)
    private static final long WARNING_THRESHOLD_TIME = 1000;

    public static final String CATEGORY_WORKSPACE = "__WORKSPACE";

    private final ILogger logger;
    private final File stateDir;
    private final File indexFile;
    private Workspace workspace;

    WorkspaceObrProvider(ILogger logger) {
        this.logger = logger;
        this.stateDir = Plugin.getDefault().getStateLocation().toFile();
        this.indexFile = new File(stateDir, INDEX_FILENAME);
    }

    void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public synchronized void reset() throws Exception {
        if (indexFile.exists() && !indexFile.delete())
            throw new IOException(String.format("Failed to reset Workspace OBR provider: could not delete index file %s", indexFile));
    }

    private Set<File> gatherWorkspaceFiles(IProgressMonitor monitor) throws CoreException {
        final Set<File> jars = new HashSet<File>();
        IWorkspaceRunnable operation = new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
                Collection<Project> projects;
                try {
                    projects = workspace.getAllProjects();
                } catch (Exception e) {
                    throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Unable to query bnd projects from workspace.", e));
                }
                for (Project project : projects) {
                    try {
                        for (Builder sub : project.getSubBuilders()) {
                            File outputFile = getOutputFile(project, sub.getBsn());
                            if (outputFile.isFile())
                                jars.add(outputFile);
                        }
                    } catch (Exception e) {
                        throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Unable to query bnd output files from workspace.", e));
                    }
                }
            }
        };
        ResourcesPlugin.getWorkspace().run(operation, monitor);
        return jars;
    }

    public synchronized Collection<URI> getOBRIndexes() throws IOException {
        regenerateIndex();
        return Collections.singletonList(indexFile.getAbsoluteFile().toURI());
    }

    private synchronized boolean needsRegeneration() {
        if (!indexFile.exists())
            return true;
        return Central.needsIndexing();
    }

    private synchronized void regenerateIndex() throws IOException {
        if (!needsRegeneration())
            return;
        long startingTime = System.currentTimeMillis();

        // Get an indexer
        BundleIndexer indexer = workspace.getPlugin(BundleIndexer.class);
        if (indexer == null)
            throw new IllegalStateException("No Bundle Indexer service available");

        // Get the files to index
        Set<File> jars;
        try {
            jars = gatherWorkspaceFiles(null);
        } catch (CoreException e) {
            logger.logError("Error generating workspace OBR index.", e);
            throw new IOException("Unable to generate workspace OBR index, see Error Log for details.");
        }
        if (jars.isEmpty())
            return;

        // Do the generation
        File tempFile = null;
        FileOutputStream out = null;
        try {
            // Needed because bindex relativizes the URIs to the repository root
            // even if we don't want it to!
            AbsoluteizeContentFilter absoluteizeFilter = new AbsoluteizeContentFilter(workspace.getBase().getCanonicalFile().toURI().toURL().toString());
            XMLReader pipeline = SAXUtil.buildPipeline(new StreamResult(indexFile), absoluteizeFilter, new CategoryInsertionContentFilter(CATEGORY_WORKSPACE));
            tempFile = File.createTempFile(".obrtemp", ".xml", stateDir);

            Map<String,String> config = new HashMap<String,String>();
            config.put(BundleIndexer.REPOSITORY_NAME, "Bndtools Workspace Repository");
            config.put(BundleIndexer.ROOT_URL, workspace.getBase().getCanonicalFile().toURI().toURL().toString());
            out = new FileOutputStream(tempFile);
            indexer.index(jars, out, config);

            pipeline.parse(new InputSource(new FileInputStream(tempFile)));
        } catch (Exception e) {
            logger.logError("Unable to generate workspace OBR index.", e);
            throw new IOException("Unable to generate workspace OBR index, see Error Log for details");
        } finally {
            long timeTaken = System.currentTimeMillis() - startingTime;
            if (timeTaken >= WARNING_THRESHOLD_TIME)
                logger.logWarning(String.format("Workspace OBR index generation took longer than %dms (time taken was %dms).", WARNING_THRESHOLD_TIME, timeTaken), null);
            if (out != null)
                IO.close(out);
            if (tempFile != null)
                tempFile.delete();
        }
    }

    public Set<OBRResolutionMode> getSupportedModes() {
        return EnumSet.allOf(OBRResolutionMode.class);
    }

    public String getName() {
        return NAME;
    }

    @Override
    public String toString() {
        return NAME;
    }

    public File[] get(String bsn, String range) throws Exception {
        return get(bsn, range, false);
    }

    public File[] get(String bsn, String range, boolean exact) throws Exception {
        File[] result = null;

        Pair<Project,Builder> found = findFromWorkspace(bsn);
        if (found != null) {
            File bundleFile = getOutputFile(found.getFirst(), bsn);
            if (bundleFile.isFile()) {
                if (matchVersion(range, found.getSecond().getVersion(), exact))
                    result = new File[] {
                        bundleFile
                    };
            }
        }

        return result;
    }

    private static boolean matchVersion(String range, String version, boolean exact) {
        if (range == null || range.trim().length() == 0)
            return true;
        if (RANGE_PROJECT.equals(range) || RANGE_LATEST.equals(range) || RANGE_SNAPSHOT.equals(range))
            return true;

        VersionRange vr = new VersionRange(range);
        Version v = Version.parseVersion(version);

        boolean result;
        if (exact) {
            if (vr.isRange())
                result = false;
            else
                result = vr.getHigh().equals(v);
        } else {
            result = vr.includes(v);
        }
        return result;
    }

    private Pair<Project,Builder> findFromWorkspace(String bsn) throws Exception {
        String pname = bsn;
        while (true) {
            Project p = workspace.getProject(pname);
            if (p != null && p.isValid()) {
                Builder sub = getSubBuilder(p, bsn);
                if (sub != null)
                    return Pair.newInstance(p, sub);
            }

            int n = pname.lastIndexOf('.');
            if (n <= 0)
                return null;
            pname = pname.substring(0, n);
        }
    }

    private static Builder getSubBuilder(Project project, String bsn) throws Exception {
        for (Builder sub : project.getSubBuilders()) {
            if (sub.getBsn().equals(bsn))
                return sub;
        }
        return null;
    }

    private static File getOutputFile(Project project, String bsn) throws Exception {
        return new File(project.getTarget(), bsn + ".jar");
    }

    public File get(String bsn, String range, Strategy strategy, Map<String,String> properties) throws Exception {
        File[] files = get(bsn, range, strategy == Strategy.EXACT);
        if (files == null || files.length == 0)
            return null;
        if (files.length > 1)
            throw new IllegalStateException(String.format("Bundle with BSN %s is available from multiple workspace locations: %s." + bsn, Arrays.toString(files)));
        return files[0];
    }

    public boolean canWrite() {
        return false;
    }

    public File put(Jar jar) throws Exception {
        throw new UnsupportedOperationException("Cannot write directly to the Workspace repository");
    }

    public List<String> list(String regex) throws Exception {
        Instruction pattern = null;
        if (regex != null)
            pattern = new Instruction(regex);

        List<String> result = new LinkedList<String>();
        Collection<Project> projects = workspace.getAllProjects();
        for (Project project : projects) {
            for (Builder sub : project.getSubBuilders()) {
                String bsn = sub.getBsn();
                if (pattern == null || pattern.matches(bsn))
                    result.add(bsn);
            }
        }

        return result;
    }

    public List<Version> versions(String bsn) throws Exception {
        Pair<Project,Builder> found = findFromWorkspace(bsn);
        if (found == null)
            return Collections.emptyList();

        Version version = Version.parseVersion(found.getSecond().getVersion());
        return Collections.singletonList(version);
    }

    public String getLocation() {
        return NAME;
    }
}

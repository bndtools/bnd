package bndtools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.stream.StreamResult;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.service.bindex.BundleIndexer;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.OBRIndexProvider;
import aQute.bnd.service.OBRResolutionMode;
import aQute.lib.osgi.Builder;
import aQute.libg.sax.SAXUtil;
import bndtools.bindex.AbsoluteizeContentFilter;
import bndtools.bindex.CategoryInsertionContentFilter;

@ThreadSafe
public class WorkspaceObrProvider implements OBRIndexProvider {

    public static final String CATEGORY_WORKSPACE = "__WORKSPACE";

    // Generate warnings if the index generation takes longer than this (millisecs)
    private static final long WARNING_THRESHOLD_TIME = 1000;

    private final File indexFile;
    private Workspace workspace;

    @GuardedBy("this")
    private final Map<Project, File[]> projectFileMap = new HashMap<Project, File[]>();
    @GuardedBy("this")
    private boolean initialised = false;

    WorkspaceObrProvider() {
        IPath stateLocation = Plugin.getDefault().getStateLocation();
        indexFile = new File(stateLocation.toFile(), "workspace-index.xml");
    }

    void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public synchronized void initialise() {
        if (initialised)
            return;

        try {
            rebuildAll();
        } catch (Exception e) {
            Plugin.logError("Error initialising workspace OBR index.", e);
        } finally {
            initialised = true;
        }
    }

    public synchronized void rebuildAll() throws Exception {
        Collection<Project> projects = workspace.getAllProjects();
        for (Project project : projects) {
            File targetDir = project.getTarget();

            Collection<? extends Builder> builders = project.getSubBuilders();
            List<File> targetFileList = new ArrayList<File>(builders.size());

            for (Builder builder : builders) {
                File targetFile = new File(targetDir, builder.getBsn() + ".jar");
                targetFileList.add(targetFile);
            }

            projectFileMap.put(project, targetFileList.toArray(new File[targetFileList.size()]));
        }
        rebuildIndex();
    }

    public synchronized void replaceProjectFiles(Project project, File[] files) throws Exception {
        projectFileMap.put(project, files);

        // TODO: can we be more efficient than rebuilding the whole workspace index each time one project changes??
        rebuildIndex();
    }

    public synchronized void rebuildIndex() throws Exception {
        long startingTime = System.currentTimeMillis();

        BundleIndexer indexer = workspace.getPlugin(BundleIndexer.class);
        if (indexer == null)
            throw new IllegalStateException("No Bundle Indexer service available");

        Set<File> jars = new HashSet<File>();
        for (File[] files : projectFileMap.values()) {
            for (File file : files) {
                if (file.exists()) jars.add(file.getCanonicalFile());
            }
        }
        if (jars.isEmpty())
            return;

        // Needed because bindex relativizes the URIs to the repository root even if we don't want it to!
        AbsoluteizeContentFilter absoluteizeFilter = new AbsoluteizeContentFilter(workspace.getBase().getCanonicalFile().toURI().toURL().toString());

        XMLReader pipeline = SAXUtil.buildPipeline(new StreamResult(indexFile), absoluteizeFilter, new CategoryInsertionContentFilter(CATEGORY_WORKSPACE));

        File tempFile = File.createTempFile("workspace-repository", ".xml");
        try {
            Map<String, String> config = new HashMap<String, String>();
            config.put(BundleIndexer.REPOSITORY_NAME, "Bndtools Workspace Repository");
            config.put(BundleIndexer.ROOT_URL, workspace.getBase().getCanonicalFile().toURI().toURL().toString());
            indexer.index(jars, new FileOutputStream(tempFile), config);
            pipeline.parse(new InputSource(new FileInputStream(tempFile)));
        } finally {
            tempFile.delete();

            long timeTaken = System.currentTimeMillis() - startingTime;
            if (timeTaken >= WARNING_THRESHOLD_TIME)
                Plugin.log(new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0, String.format("Workspace OBR index generation took longer than %dms (time taken was %dms).", WARNING_THRESHOLD_TIME, timeTaken), null));
        }
    }

    public Collection<URL> getOBRIndexes() throws IOException {
        initialise();
        return Collections.singletonList(indexFile.toURI().toURL());
    }

    public Set<OBRResolutionMode> getSupportedModes() {
        return EnumSet.allOf(OBRResolutionMode.class);
    }

    @Override
    public String toString() {
        return "Workspace";
    }
}

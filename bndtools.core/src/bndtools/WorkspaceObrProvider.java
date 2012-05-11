package bndtools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
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
import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.deployer.repository.FixedIndexedRepo;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Jar;
import aQute.libg.sax.SAXUtil;
import aQute.libg.version.Version;
import bndtools.bindex.AbsoluteizeContentFilter;
import bndtools.bindex.CategoryInsertionContentFilter;

@ThreadSafe
public class WorkspaceObrProvider implements RepositoryPlugin, OBRIndexProvider {

    private static final String NAME = "Workspace";

    public static final String CATEGORY_WORKSPACE = "__WORKSPACE";

    // Generate warnings if the index generation takes longer than this (millisecs)
    private static final long WARNING_THRESHOLD_TIME = 1000;

    private final FixedIndexedRepo repository = new FixedIndexedRepo();
    private final File indexFile;
    
    private Workspace workspace;

    @GuardedBy("this")
    private final Map<Project, File[]> projectFileMap = new HashMap<Project, File[]>();

    WorkspaceObrProvider() {
        IPath stateLocation = Plugin.getDefault().getStateLocation();
        indexFile = new File(stateLocation.toFile(), "workspace-index.xml");
        
        Map<String, String> config = new HashMap<String, String>();
        config.put(FixedIndexedRepo.PROP_REPO_TYPE, FixedIndexedRepo.REPO_TYPE_OBR);
        config.put(FixedIndexedRepo.PROP_LOCATIONS, indexFile.getAbsoluteFile().toURI().toString());
        repository.setProperties(config);
    }

    void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public synchronized void reset() throws Exception {
        refreshProjects();
        rebuildIndex();
        repository.reset();
    }
    
    /**
     * Refresh the project->file map from the actual projects
     * @throws Exception
     */
    protected synchronized void refreshProjects() throws Exception {
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
    }

    public synchronized void replaceProjectFiles(Project project, File[] files) throws Exception {
        projectFileMap.put(project, files);
        // TODO: can we be more efficient than rebuilding the whole workspace index each time one project changes??
        rebuildIndex();
    }

    private synchronized void rebuildIndex() throws Exception {
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
        try {
            return repository.getIndexLocations();
        } catch (Exception e) {
            throw new IOException(e.toString());
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
        return repository.get(bsn, range);
    }

    public File get(String bsn, String range, Strategy strategy, Map<String, String> properties) throws Exception {
        return repository.get(bsn, range, strategy, properties);
    }

    public boolean canWrite() {
        return false;
    }

    public File put(Jar jar) throws Exception {
        throw new UnsupportedOperationException("Cannot write directly to the Workspace repository");
    }

    public List<String> list(String regex) throws Exception {
        return repository.list(regex);
    }

    public List<Version> versions(String bsn) throws Exception {
        return repository.versions(bsn);
    }

    public String getLocation() {
        return repository.getLocation();
    }
}

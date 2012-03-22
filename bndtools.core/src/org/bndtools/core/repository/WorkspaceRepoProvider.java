package org.bndtools.core.repository;

import java.io.File;
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

import net.jcip.annotations.GuardedBy;

import org.osgi.service.indexer.ResourceIndexer;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.service.IndexProvider;
import aQute.bnd.service.ResolutionPhase;
import aQute.lib.osgi.Builder;
import bndtools.api.ILogger;

public class WorkspaceRepoProvider implements IndexProvider {

    // Generate warnings if the index generation takes longer than this (millisecs)
    private static final long WARNING_THRESHOLD_TIME = 1000;

    private final File indexFile;
    private final ResourceIndexer indexer;
    private final ILogger logger;
    
    private Workspace workspace;

    @GuardedBy("this")
    private final Map<Project, File[]> projectFileMap = new HashMap<Project, File[]>();
    @GuardedBy("this")
    private boolean initialised = false;


    public WorkspaceRepoProvider(File indexFile, ResourceIndexer indexer, ILogger logger) {
        this.indexer = indexer;
        this.indexFile = indexFile;
        this.logger = logger;
    }
    
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public synchronized void initialise() {
        if (initialised)
            return;

        try {
            rebuildAll();
        } catch (Exception e) {
            logger.logError("Error initialising workspace OBR index.", e);
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
        
        Set<File> jars = gatherFiles();
        
        String baseUrl = workspace.getBase().getCanonicalFile().toURI().toURL().toString();
        
        Map<String, String> config = new HashMap<String, String>();
        config.put(ResourceIndexer.REPOSITORY_NAME, "Bndtools Workspace Repository");
        config.put(ResourceIndexer.ROOT_URL, baseUrl);
        config.put(ResourceIndexer.PRETTY, "true");
        config.put(ResourceIndexer.URL_TEMPLATE, baseUrl + "%p%f");
        
        try {
            
            indexer.index(jars, new FileOutputStream(indexFile), config);
        } finally {
            long timeTaken = System.currentTimeMillis() - startingTime;
            if (timeTaken >= WARNING_THRESHOLD_TIME)
                logger.logWarning(String.format("Workspace index generation took longer than %dms (time taken was %dms).", WARNING_THRESHOLD_TIME, timeTaken), null);
        }
    }

    private Set<File> gatherFiles() throws IOException {
        Set<File> jars = new HashSet<File>();
        for (File[] files : projectFileMap.values()) {
            for (File file : files) {
                if (file.exists()) jars.add(file.getCanonicalFile());
            }
        }
        return jars;
    }

    public List<URL> getIndexLocations() throws IOException {
        initialise();
        return Collections.singletonList(indexFile.toURI().toURL());
    }

    public Set<ResolutionPhase> getSupportedPhases() {
        return EnumSet.allOf(ResolutionPhase.class);
    }
    
    @Override
    public String toString() {
        return "<<Workspace>>";
    }

}

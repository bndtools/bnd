package org.bndtools.core.build.indexer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bndtools.build.api.AbstractBuildListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.osgi.service.indexer.Builder;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceAnalyzer;
import org.osgi.service.indexer.ResourceIndexer;
import org.osgi.service.indexer.impl.RepoIndex;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.io.IO;
import bndtools.Central;
import bndtools.LogServiceAdapter;
import bndtools.Logger;
import bndtools.WorkspaceR5Repository;
import bndtools.api.ILogger;

public class BuiltBundleIndexer extends AbstractBuildListener {

    private static final String INDEX_FILENAME = ".index";

    private final ILogger logger = Logger.getLogger();
    private final LogServiceAdapter logAdapter;

    public BuiltBundleIndexer() {
        logAdapter = new LogServiceAdapter(logger);
    }

    @Override
    public void builtBundles(final IProject project, IPath[] paths) {
        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        final URI workspaceRootUri = wsroot.getLocationURI();

        Set<File> files = new HashSet<File>();
        for (IPath path : paths) {
            try {
                IFile ifile = wsroot.getFile(path);
                File file = ifile.getLocation().toFile();
                files.add(file);
            } catch (IllegalArgumentException e) {
                System.err.println("### Error processing path: " + path);
                e.printStackTrace();
            }
        }

        // Generate the index file
        File indexFile;
        OutputStream output = null;
        try {
            Project model = Workspace.getProject(project.getLocation().toFile());
            File target = model.getTarget();
            indexFile = new File(target, INDEX_FILENAME);

            IFile indexPath = wsroot.getFile(Central.toPath(indexFile));

            // Create the indexer and add ResourceAnalyzers from plugins
            RepoIndex indexer = new RepoIndex(logAdapter);
            List<ResourceAnalyzer> analyzers = Central.getWorkspace().getPlugins(ResourceAnalyzer.class);
            for (ResourceAnalyzer analyzer : analyzers) {
                indexer.addAnalyzer(analyzer, null);
            }

            // Use an analyzer to add a marker capability to workspace resources
            indexer.addAnalyzer(new ResourceAnalyzer() {
                public void analyzeResource(Resource resource, List<Capability> capabilities, List<Requirement> requirements) throws Exception {
                    Capability cap = new Builder().setNamespace("bndtools.workspace").addAttribute("bndtools.workspace", workspaceRootUri.toString()).addAttribute("project.path", project.getFullPath().toString()).buildCapability();
                    capabilities.add(cap);
                }
            }, null);

            Map<String,String> config = new HashMap<String,String>();
            config.put(ResourceIndexer.REPOSITORY_NAME, project.getName());
            config.put(ResourceIndexer.ROOT_URL, project.getLocation().toFile().toURI().toString());
            config.put(ResourceIndexer.PRETTY, "true");

            output = new FileOutputStream(indexFile);
            indexer.index(files, output, config);
            IO.close(output);
            indexPath.refreshLocal(IResource.DEPTH_ZERO, null);
            if (indexPath.exists())
                indexPath.setDerived(true);
        } catch (Exception e) {
            logger.logError(MessageFormat.format("Failed to generate index file for bundles in project {0}.", project.getName()), e);
            return;
        } finally {
            IO.close(output);
        }

        // Parse the index and add to the workspace repository
        FileInputStream input = null;
        try {
            input = new FileInputStream(indexFile);
            WorkspaceR5Repository workspaceRepo = Central.getWorkspaceR5Repository();
            workspaceRepo.loadProjectIndex(project, input, project.getLocation().toFile().toURI());
        } catch (Exception e) {
            logger.logError("Failed to update workspace index.", e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    /* ignore */
                }
            }
        }
    }

}

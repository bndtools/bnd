package org.bndtools.core.build.indexer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import org.bndtools.build.api.AbstractBuildListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.osgi.resource.Resource;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.lib.deployer.repository.api.IRepositoryContentProvider;
import aQute.lib.deployer.repository.api.IRepositoryIndexProcessor;
import aQute.lib.deployer.repository.api.Referral;
import aQute.lib.deployer.repository.providers.R5RepoContentProvider;
import aQute.lib.io.IO;
import bndtools.Central;
import bndtools.LogServiceAdapter;
import bndtools.Logger;
import bndtools.WorkspaceR5Repository;
import bndtools.api.ILogger;

public class BuiltBundleIndexer extends AbstractBuildListener {

    private static final String INDEX_FILENAME = ".index";

    private static final String WORKSPACE_REPO_NAME = "Bndtools Workspace";

    private final IRepositoryContentProvider contentProvider = new R5RepoContentProvider();
    private final ILogger logger = Logger.getLogger();
    private final LogServiceAdapter logAdapter;

    public BuiltBundleIndexer() {
        logAdapter = new LogServiceAdapter(logger);
    }

    @Override
    public void builtBundles(final IProject project, IPath[] paths) {
        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        URI workspaceRootUri = wsroot.getLocationURI();

        Set<File> files = new HashSet<File>();
        for (IPath path : paths) {
            IFile ifile = wsroot.getFile(path);
            File file = ifile.getLocation().toFile();
            files.add(file);
        }

        OutputStream output = null;
        File indexFile;

        // Generate the index file
        try {
            Project model = Workspace.getProject(project.getLocation().toFile());
            File target = model.getTarget();
            indexFile = new File(target, INDEX_FILENAME);

            output = new FileOutputStream(indexFile);
            contentProvider.generateIndex(files, output, WORKSPACE_REPO_NAME, workspaceRootUri, true, Central.getWorkspace(), logAdapter);
            IO.close(output);
        } catch (Exception e) {
            logger.logError(MessageFormat.format("Failed to generate index file for bundles in project {0}.", project.getName()), e);
            return;
        } finally {
            IO.close(output);
        }

        // Parse the index and add to the workspace repository
        try {
            FileInputStream input = new FileInputStream(indexFile);
            final WorkspaceR5Repository workspaceRepo = Central.getWorkspaceR5Repository();
            workspaceRepo.cleanProject(project);

            IRepositoryIndexProcessor processor = new IRepositoryIndexProcessor() {
                public void processResource(Resource resource) {
                    workspaceRepo.addResource(project, resource);
                }

                public void processReferral(URI parentUri, Referral referral, int maxDepth, int currentDepth) {
                    // ignore, we don't generate referrals here
                }
            };

            contentProvider.parseIndex(input, workspaceRootUri, processor, logAdapter);
        } catch (Exception e) {
            logger.logError(MessageFormat.format("Failed to process index file for bundles in project {0}.", project.getName()), e);
        }
    }

}

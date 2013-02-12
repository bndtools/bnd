package bndtools;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.deployer.repository.CapabilityIndex;
import aQute.bnd.deployer.repository.api.IRepositoryContentProvider;
import aQute.bnd.deployer.repository.api.IRepositoryIndexProcessor;
import aQute.bnd.deployer.repository.api.Referral;
import aQute.bnd.deployer.repository.providers.R5RepoContentProvider;
import aQute.lib.io.IO;
import bndtools.api.ILogger;

public class WorkspaceR5Repository implements Repository {

    private static final String NAME = "Workspace";

    private final Map<IProject,CapabilityIndex> projectMap = new HashMap<IProject,CapabilityIndex>();
    private final IRepositoryContentProvider contentProvider = new R5RepoContentProvider();

    private final ILogger logger = Logger.getLogger();
    private final LogService logAdapter = new LogServiceAdapter(logger);

    void init() throws Exception {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (IProject project : projects) {
            Project model = Workspace.getProject(project.getLocation().toFile());
            if (model != null) {
                File targetDir = model.getTarget();
                if (targetDir != null) {
                    File indexFile = new File(targetDir, ".index");
                    if (indexFile != null && indexFile.isFile()) {
                        loadProjectIndex(project, new FileInputStream(indexFile), project.getLocation().toFile().toURI());
                    }
                }
            }
        }
    }

    public void loadProjectIndex(final IProject project, InputStream index, URI baseUri) {
        synchronized (projectMap) {
            try {
                cleanProject(project);

                IRepositoryIndexProcessor processor = new IRepositoryIndexProcessor() {
                    public void processResource(Resource resource) {
                        addResource(project, resource);
                    }

                    public void processReferral(URI parentUri, Referral referral, int maxDepth, int currentDepth) {
                        // ignore: we don't create any referrals
                    }
                };
                contentProvider.parseIndex(index, baseUri, processor, logAdapter);
            } catch (Exception e) {
                logger.logError(MessageFormat.format("Failed to process index file for bundles in project {0}.", project.getName()), e);
            } finally {
                IO.close(index);
            }
        }
    }

    private void cleanProject(IProject project) {
        CapabilityIndex index = projectMap.get(project);
        if (index != null)
            index.clear();
    }

    private void addResource(IProject project, Resource resource) {
        CapabilityIndex index = projectMap.get(project);
        if (index == null) {
            index = new CapabilityIndex();
            projectMap.put(project, index);
        }
        index.addResource(resource);
    }

    public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
        Map<Requirement,Collection<Capability>> result = new HashMap<Requirement,Collection<Capability>>();
        for (Requirement requirement : requirements) {
            List<Capability> matches = new LinkedList<Capability>();
            result.put(requirement, matches);

            for (Entry<IProject,CapabilityIndex> entry : projectMap.entrySet()) {
                CapabilityIndex capabilityIndex = entry.getValue();
                capabilityIndex.appendMatchingCapabilities(requirement, matches);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return NAME;
    }

}

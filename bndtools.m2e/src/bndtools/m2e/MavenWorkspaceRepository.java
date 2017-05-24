package bndtools.m2e;

import java.io.File;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import org.apache.maven.project.MavenProject;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.version.Version;
import aQute.lib.collections.SortedList;

public class MavenWorkspaceRepository extends BaseRepository implements Repository, RepositoryPlugin, IMavenProjectChangedListener {

    private final static ILogger logger = Logger.getLogger(MavenWorkspaceRepository.class);

    private final IMavenProjectRegistry mavenProjectRegistry = MavenPlugin.getMavenProjectRegistry();

    private boolean inited = false;

    private final Map<String,Entry<IMavenProjectFacade,MavenProject>> bsnMap = new HashMap<>();

    @Override
    public Map<Requirement,Collection<Capability>> findProviders(Collection< ? extends Requirement> requirements) {
        return Collections.emptyMap();
    }

    @Override
    public PutResult put(InputStream stream, PutOptions options) throws Exception {
        throw new IllegalStateException(getName() + " is read-only");
    }

    @Override
    public File get(final String bsn, final Version version, Map<String,String> properties, final DownloadListener... listeners) throws Exception {
        if (!inited) {
            init();
        }

        final Entry<IMavenProjectFacade,MavenProject> entry = bsnMap.get(bsn);

        if (entry == null)
            return null;

        final MavenProject mavenProject = entry.getValue();

        // add the eclipse project that this comes from so we can look it up in the launch
        // see bndtools.launch.BndDependencySourceContainer.createSourceContainers()
        final String projectName = entry.getKey().getProject().getName();
        properties.put("sourceProjectName", projectName);

        final File bundleFile = getBundleFile(mavenProject);

        if (!bundleFile.exists()) {
            for (DownloadListener listener : listeners) {
                try {
                    listener.failure(bundleFile, "Could not get bundle file for " + bsn + ":" + version);
                } catch (Throwable t) {
                    logger.logError("Download listener error", t);
                }
            }

            return null;
        }

        for (DownloadListener listener : listeners) {
            try {
                listener.success(bundleFile);
            } catch (Throwable t) {
                logger.logError("Download listener error", t);
            }
        }

        return bundleFile;
    }

    private File getBundleFile(final MavenProject mavenProject) {
        return new File(mavenProject.getBuild().getDirectory(), mavenProject.getBuild().getFinalName() + ".jar");
    }

    private String getBsnFromMavenProject(MavenProject mavenProject) throws Exception {
        final File bundleFile = getBundleFile(mavenProject);

        if (bundleFile.exists()) {
            Domain domain = Domain.domain(bundleFile);
            String bsn = domain.getBundleSymbolicName().getKey();
            return bsn;
        }

        return null;
    }

    private void init() {
        inited = true;

        final IProgressMonitor monitor = new NullProgressMonitor();

        for (IMavenProjectFacade projectFacade : mavenProjectRegistry.getProjects()) {
            final IProject project = projectFacade.getProject();

            try {
                final MavenProject mavenProject = getMavenProject(projectFacade, monitor);

                final String bsn = getBsnFromMavenProject(mavenProject);

                if (bsn != null) {
                    Entry<IMavenProjectFacade,MavenProject> entry = new AbstractMap.SimpleImmutableEntry<>(projectFacade, mavenProject);
                    bsnMap.put(bsn, entry);
                }
            } catch (Exception e) {
                logger.logError("Unable to get bundle symbolic name for " + project.getName(), e);
            }
        }

        mavenProjectRegistry.addMavenProjectChangedListener(this);
    }

    private MavenProject getMavenProject(final IMavenProjectFacade projectFacade, final IProgressMonitor monitor) throws CoreException {
        MavenProject mavenProject = projectFacade.getMavenProject();

        if (mavenProject == null) {
            mavenProject = projectFacade.getMavenProject(monitor);
        }

        return mavenProject;
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public List<String> list(String pattern) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public SortedSet<Version> versions(String bsn) throws Exception {
        if (!inited) {
            init();
        }

        Version version = null;

        final Entry<IMavenProjectFacade,MavenProject> entry = bsnMap.get(bsn);

        if (entry != null) {
            final MavenProject mavenProject = entry.getValue();
            final File bundleFile = getBundleFile(mavenProject);

            if (bundleFile.exists()) {
                Domain domain = Domain.domain(bundleFile);
                version = new Version(domain.getBundleVersion());
            }
        }

        if (version == null) {
            return SortedList.empty();
        }

        return new SortedList<Version>(version);
    }

    @Override
    public String getName() {
        return "Maven Workspace Repository";
    }

    @Override
    public String getLocation() {
        Location location = Platform.getInstanceLocation();

        if (location != null) {
            return location.getURL().toString();
        }

        return null;
    }

    @Override
    public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
        if (events == null)
            return;

        for (MavenProjectChangedEvent event : events) {
            final IMavenProjectFacade oldProject = event.getOldMavenProject();

            final Iterator<Entry<String,Entry<IMavenProjectFacade,MavenProject>>> entries = bsnMap.entrySet().iterator();

            while (entries.hasNext()) {
                final Entry<String,Entry<IMavenProjectFacade,MavenProject>> entry = entries.next();

                if (entry.getValue().getKey().equals(oldProject)) {
                    String bsn = entry.getKey();

                    bsnMap.remove(bsn);
                    break;
                }
            }

            final IMavenProjectFacade newProject = event.getMavenProject();

            try {
                final MavenProject newMavenProject = getMavenProject(newProject, monitor);

                final String newBsn = getBsnFromMavenProject(newMavenProject);
                final Entry<IMavenProjectFacade,MavenProject> newEntry = new SimpleImmutableEntry<>(newProject, newMavenProject);

                bsnMap.put(newBsn, newEntry);
            } catch (Exception e) {
                logger.logError("Error getting bsn for new project " + newProject.getProject().getName(), e);
            }
        }
    }

    void cleanup() {
        mavenProjectRegistry.removeMavenProjectChangedListener(this);
    }

}

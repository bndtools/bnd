package bndtools.m2e;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.MavenJdtPlugin;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.Refreshable;
import aQute.bnd.version.Version;
import bndtools.central.Central;

public class MavenImplicitProjectRepository extends AbstractMavenRepository implements Refreshable {

    private static final Logger logger = LoggerFactory.getLogger(MavenImplicitProjectRepository.class);

    private final IClasspathManager buildpathManager = MavenJdtPlugin.getDefault()
        .getBuildpathManager();

    private volatile FileSetRepository fileSetRepository;

    private final IMavenProjectFacade projectFacade;

    public MavenImplicitProjectRepository(IResource targetResource) {
        projectFacade = mavenProjectRegistry.getProject(targetResource.getProject());

        createRepo(projectFacade, new NullProgressMonitor());

        mavenProjectRegistry.addMavenProjectChangedListener(this);
    }

    @Override
    public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
        return fileSetRepository.findProviders(requirements);
    }

    @Override
    public File get(final String bsn, final Version version, Map<String, String> properties, final DownloadListener... listeners) throws Exception {
        return fileSetRepository.get(bsn, version, properties, listeners);
    }

    @Override
    public String getName() {
        return projectFacade.getProject()
            .getName();
    }

    @Override
    public File getRoot() throws Exception {
        return projectFacade.getPomFile();
    }

    @Override
    public List<String> list(String pattern) throws Exception {
        return fileSetRepository.list(pattern);
    }

    @Override
    public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
        if (events == null)
            return;

        for (MavenProjectChangedEvent event : events) {
            final IMavenProjectFacade mavenProjectFacade = event.getMavenProject();

            if (!mavenProjectFacade.getProject()
                .equals(projectFacade.getProject()) && (event.getFlags() != MavenProjectChangedEvent.FLAG_DEPENDENCIES)) {

                continue;
            }

            createRepo(mavenProjectFacade, monitor);
        }
    }

    @Override
    public boolean refresh() throws Exception {
        return true;
    }

    @Override
    public SortedSet<Version> versions(String bsn) throws Exception {
        return fileSetRepository.versions(bsn);
    }

    private void createRepo(IMavenProjectFacade mavenProjectFacade, IProgressMonitor monitor) {
        try {
            List<File> toIndex = new ArrayList<>();

            File file = guessBundleFile(mavenProjectFacade);
            if (file != null) {
                toIndex.add(file);
            }

            IClasspathEntry[] classpath = buildpathManager.getClasspath(mavenProjectFacade.getProject(), IClasspathManager.CLASSPATH_RUNTIME, true, monitor);

            for (IClasspathEntry cpe : classpath) {
                IClasspathAttribute[] extraAttributes = cpe.getExtraAttributes();

                for (IClasspathAttribute ea : extraAttributes) {
                    if (ea.getName()
                        .equals("maven.scope")
                        && (ea.getValue()
                            .equals("compile")
                            || ea.getValue()
                                .equals("runtime"))) {

                        if (cpe.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                            file = getOutputFile(extraAttributes);
                            if (file != null) {
                                toIndex.add(file);
                            }
                        } else if ((cpe.getEntryKind() == IClasspathEntry.CPE_LIBRARY) && (cpe.getContentKind() == IPackageFragmentRoot.K_BINARY)) {
                            file = cpe.getPath()
                                .toFile();
                            if (file != null) {
                                toIndex.add(file);
                            }
                        }
                    }
                }
            }

            fileSetRepository = new FileSetRepository(getName(), toIndex);

            Central.refreshPlugin(this);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Failed to create implicit repository for m2e project {}", getName(), e);
            }
        }
    }

    private File getOutputFile(IClasspathAttribute[] extraAttributes) {
        String groupId = null, artifactId = null, version = null;

        for (IClasspathAttribute attribute : extraAttributes) {
            if ("maven.groupId".equals(attribute.getName())) {
                groupId = attribute.getValue();
            } else if ("maven.artifactId".equals(attribute.getName())) {
                artifactId = attribute.getValue();
            } else if ("maven.version".equals(attribute.getName())) {
                version = attribute.getValue();
            }
        }

        IMavenProjectFacade mavenProjectFacade = mavenProjectRegistry.getMavenProject(groupId, artifactId, version);

        return guessBundleFile(mavenProjectFacade);
    }

}

package bndtools.m2e;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.maven.lib.configuration.Bndruns;
import aQute.bnd.maven.lib.configuration.Bundles;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.maven.lib.resolve.BndrunContainer.Builder;
import aQute.bnd.maven.lib.resolve.Scope;
import aQute.bnd.repository.fileset.FileSetRepository;
import aQute.bnd.service.Refreshable;
import aQute.bnd.version.Version;
import aQute.lib.exceptions.Exceptions;
import bndtools.central.Central;

public class MavenImplicitProjectRepository extends AbstractMavenRepository implements Refreshable {

    private static final Logger logger = LoggerFactory.getLogger(MavenImplicitProjectRepository.class);

    private volatile FileSetRepository fileSetRepository;

    private final IMavenProjectFacade projectFacade;
    private final File bndrunFile;

    public MavenImplicitProjectRepository(IMavenProjectFacade projectFacade, File bndrunFile) {
        this.projectFacade = projectFacade;
        this.bndrunFile = bndrunFile;

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

    @SuppressWarnings({
        "deprecation", "unchecked"
    })
    private void createRepo(IMavenProjectFacade projectFacade, IProgressMonitor monitor) {
        try {
            MavenProject mavenProject = getMavenProject(projectFacade);
            IMavenExecutionContext executionContext = maven.getExecutionContext();
            MavenSession mavenSession;

            if (executionContext == null) {
                executionContext = maven.createExecutionContext();
                mavenSession = maven.createSession(executionContext.getExecutionRequest(), mavenProject);
            } else {
                mavenSession = executionContext.getSession();
            }

            mavenSession.setCurrentProject(mavenProject);

            Builder containerBuilder = new BndrunContainer.Builder(mavenProject, mavenSession, mavenSession.getRepositorySession(), lookupComponent(ProjectDependenciesResolver.class),
                lookupComponent(org.apache.maven.artifact.factory.ArtifactFactory.class), lookupComponent(RepositorySystem.class));

            MojoExecution mojoExecution = projectFacade.getMojoExecutions("biz.aQute.bnd", "bnd-resolver-maven-plugin", monitor, "resolve")
                .stream()
                .filter(exe -> containsBndrun(exe, mavenProject, monitor))
                .findFirst()
                .orElse(null);

            if (mojoExecution != null) {
                Bundles bundles = maven.getMojoParameterValue(mavenProject, mojoExecution, "bundles", Bundles.class, monitor);
                if (bundles == null) {
                    bundles = new Bundles();
                }

                containerBuilder.setBundles(bundles.getFiles(mavenProject.getBasedir()));

                containerBuilder.setIncludeDependencyManagement(maven.getMojoParameterValue(mavenProject, mojoExecution, "includeDependencyManagement", Boolean.class, monitor));

                Set<String> scopeValues = maven.getMojoParameterValue(mavenProject, mojoExecution, "scopes", Set.class, monitor);
                containerBuilder.setScopes(scopeValues.stream()
                    .map(Scope::valueOf)
                    .collect(Collectors.toSet()));

                containerBuilder.setUseMavenDependencies(maven.getMojoParameterValue(mavenProject, mojoExecution, "useMavenDependencies", Boolean.class, monitor));
            }

            fileSetRepository = containerBuilder.build()
                .getFileSetRepository();
            fileSetRepository.list(null);

            Central.refreshPlugin(this);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("Failed to create implicit repository for m2e project {}", getName(), e);
            }
        }
    }

    private boolean containsBndrun(MojoExecution mojoExecution, MavenProject mavenProject, IProgressMonitor monitor) {
        try {
            Bndruns bndruns = maven.getMojoParameterValue(mavenProject, mojoExecution, "bndruns", Bndruns.class, monitor);

            return bndruns.getFiles(mavenProject.getBasedir())
                .contains(bndrunFile);
        } catch (Exception e) {
            throw Exceptions.duck(e);
        }
    }

}

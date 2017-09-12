package bndtools.m2e;

import static org.eclipse.core.resources.IResourceChangeEvent.POST_BUILD;
import static org.eclipse.core.resources.IncrementalProjectBuilder.FULL_BUILD;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.LocalArtifactRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

import aQute.lib.exceptions.Exceptions;

public class IndexConfigurator extends AbstractProjectConfigurator implements IResourceChangeListener {

    /**
     * We use this to replace the standard workspace repository because that feeds target/classes into the indexer and
     * causes it to blow up. Instead this repository feeds in target/finalName.jar which does not!
     */
    private final class IndexerWorkspaceRepository extends LocalArtifactRepository implements WorkspaceReader {

        private final SubMonitor progress = SubMonitor.convert(null);

        private final WorkspaceRepository wr = new WorkspaceRepository("index", getClass());

        @Override
        public boolean hasLocalMetadata() {
            return false;
        }

        @Override
        public Artifact find(Artifact artifact) {
            File file = find(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType());

            if (file != null) {
                artifact.setFile(file);
                artifact.setResolved(true);
            }

            return artifact;
        }

        private File find(String groupId, String artifactId, String version, String extension) {
            IMavenProjectFacade found = MavenPlugin.getMavenProjectRegistry().getMavenProject(groupId, artifactId, version);

            if (found != null) {
                return getMavenOutputFile(extension, found, progress.newChild(1));
            }
            return null;
        }

        @Override
        public File findArtifact(org.eclipse.aether.artifact.Artifact artifact) {
            return find(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getExtension());
        }

        @Override
        public List<String> findVersions(org.eclipse.aether.artifact.Artifact artifact) {
            List<String> versions = new ArrayList<>();
            for (IMavenProjectFacade facade : MavenPlugin.getMavenProjectRegistry().getProjects()) {
                ArtifactKey key = facade.getArtifactKey();
                if (key.getArtifactId().equals(artifact.getArtifactId()) && key.getGroupId().equals(artifact.getGroupId())) {
                    versions.add(key.getVersion());
                }
            }
            return versions;
        }

        @Override
        public WorkspaceRepository getRepository() {
            return wr;
        }
    }

    /**
     * This method finds the relevant file in the workspace if it exists
     *
     * @param extension
     * @param found
     * @param monitor
     * @return
     */
    private File getMavenOutputFile(String extension, IMavenProjectFacade found, IProgressMonitor monitor) {
        File f = null;

        if ("pom".equals(extension)) {
            f = found.getPomFile();
        } else {
            MavenProject mp = null;
            try {
                mp = getMavenProject(found, monitor);
            } catch (CoreException e) {}

            if (mp != null) {
                String outputFileName = mp.getBuild().getFinalName() + "." + (extension == null ? "jar" : extension);
                File check = new File(mp.getBuild().getDirectory(), outputFileName);
                if (check.exists()) {
                    f = check;
                }
            }
        }

        return f;
    }

    /**
     * We have to add a listener to trigger builds when dependent projects build
     */
    public IndexConfigurator() {
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this, POST_BUILD);
    }

    @Override
    public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {}

    /**
     * We have to temporarily override the ongoing maven build by creating a new context. This allows us to replace the
     * workspace repository with our own.
     */
    @Override
    public AbstractBuildParticipant getBuildParticipant(final IMavenProjectFacade projectFacade, MojoExecution execution, final IPluginExecutionMetadata executionMetadata) {

        return new MojoExecutionBuildParticipant(execution, true, false) {

            @Override
            public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {

                if (appliesToBuildKind(kind)) {
                    final SubMonitor progress = SubMonitor.convert(monitor, "Executing indexer plugin", 2);

                    final IMaven maven = MavenPlugin.getMaven();

                    IMavenExecutionContext context = maven.createExecutionContext();
                    context.getExecutionRequest().setWorkspaceReader(new IndexerWorkspaceRepository());

                    final MavenProject mavenProject = getMavenProject(projectFacade, progress.newChild(1));

                    context.execute(new ICallable<Void>() {
                        @Override
                        public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
                            maven.execute(mavenProject, getMojoExecution(), monitor);

                            IPath buildDirPath = Path.fromOSString(mavenProject.getBuild().getDirectory());
                            IProject project = projectFacade.getProject();
                            IPath projectPath = project.getLocation();
                            IPath relativeBuildDirPath = buildDirPath.makeRelativeTo(projectPath);
                            IFolder buildDir = project.getFolder(relativeBuildDirPath);
                            buildDir.refreshLocal(IResource.DEPTH_INFINITE, progress.newChild(1));
                            return null;
                        }
                    }, progress.newChild(1));
                }
                return null;
            }
        };
    }

    private MavenProject getMavenProject(final IMavenProjectFacade projectFacade, IProgressMonitor monitor) throws CoreException {
        MavenProject mavenProject = projectFacade.getMavenProject();

        if (mavenProject == null) {
            mavenProject = projectFacade.getMavenProject(monitor);
        }

        monitor.done();

        return mavenProject;
    }

    /**
     * This method triggers a rebuild of all indexes for whom an output file in a dependent project has changed. A quick
     * check is done to see if the change might be interesting, and if so a Job is scheduled to do a more comprehensive
     * check (and possibly a build).
     * <p>
     * As this listener may be called a lot for "uninteresting projects", this seems like the best option, but it could
     * be changed later with implementation experience.
     */
    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
        for (IMavenProjectFacade facade : MavenPlugin.getMavenProjectRegistry().getProjects()) {
            mojos: for (MojoExecutionKey key : facade.getMojoExecutionMapping().keySet()) {
                if ("biz.aQute.bnd".equals(key.getGroupId()) && "bnd-indexer-maven-plugin".equals(key.getArtifactId())) {

                    // This is an indexer project - if any referenced projects were part of the build
                    // then trigger it
                    try {
                        for (IProject p : facade.getProject().getReferencedProjects()) {
                            if (p.equals(facade.getProject())) {
                                continue;
                            }
                            if (event.getDelta().findMember(p.getFullPath()) != null) {
                                final IMavenProjectFacade indexerFacade = facade;
                                Job job = new WorkspaceJob("Rebuilding index project " + facade.getProject().getName()) {
                                    @Override
                                    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {

                                        // Do a longer check to see if we need to rebuild

                                        final SubMonitor progress = SubMonitor.convert(monitor);
                                        MavenProject project = getMavenProject(indexerFacade, progress.newChild(1));

                                        Map<ArtifactKey,String> keysToTypes = new HashMap<>();
                                        for (Artifact a : project.getArtifacts()) {
                                            keysToTypes.put(new ArtifactKey(a), a.getType());
                                        }

                                        for (IProject p : indexerFacade.getProject().getReferencedProjects()) {
                                            IMavenProjectFacade pf = MavenPlugin.getMavenProjectRegistry().getProject(p);
                                            if (pf != null) {
                                                String type = keysToTypes.get(pf.getArtifactKey());
                                                if (type != null) {
                                                    File dep = getMavenOutputFile(type, pf, monitor);
                                                    IPath depPath = Path.fromOSString(dep.getAbsolutePath());
                                                    IPath projectRelativePath = p.getFile(depPath.makeRelativeTo(p.getLocation())).getFullPath();
                                                    if (event.getDelta().findMember(projectRelativePath) != null) {
                                                        indexerFacade.getProject().build(FULL_BUILD, monitor);
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        return Status.OK_STATUS;
                                    }
                                };
                                job.setRule(facade.getProject().getWorkspace().getRoot());
                                job.schedule();
                                break mojos;
                            }
                        }
                    } catch (CoreException e) {
                        Exceptions.duck(e);
                    }
                }
            }
        }
    }
}

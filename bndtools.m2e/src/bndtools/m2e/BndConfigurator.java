package bndtools.m2e;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.build.api.IProjectDecorator;
import org.bndtools.build.api.IProjectDecorator.BndProjectInfo;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
import org.eclipse.m2e.core.embedder.ICallable;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.IMavenExecutionContext;
import org.eclipse.m2e.core.internal.MavenPluginActivator;
import org.eclipse.m2e.core.internal.project.registry.ProjectRegistryManager;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Packages;

public class BndConfigurator extends AbstractProjectConfigurator {

    ILogger logger = Logger.getLogger(BndConfigurator.class);

    public static class MavenProjectInfo implements BndProjectInfo {

        private final MavenProject project;

        private final Analyzer analyzer;

        public MavenProjectInfo(MavenProject project) throws Exception {
            super();
            this.project = project;
            File file = project.getArtifact().getFile();
            if (file == null) {
                throw new IllegalStateException("The output file for project " + project.getName() + " does not exist");
            }
            analyzer = new Analyzer(new Jar(file));
            analyzer.analyze();
        }

        @Override
        public Collection<File> getSourcePath() throws Exception {
            List<File> sourcePath = new ArrayList<>();
            for (String path : project.getCompileSourceRoots()) {
                sourcePath.add(new File(path));
            }
            return sourcePath;
        }

        @Override
        public Packages getExports() {
            return analyzer.getExports();
        }

        @Override
        public Packages getImports() {
            return analyzer.getImports();
        }

        @Override
        public Packages getContained() {
            return analyzer.getContained();
        }

    }

    @Override
    public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {}

    @Override
    public AbstractBuildParticipant getBuildParticipant(final IMavenProjectFacade projectFacade, MojoExecution execution, IPluginExecutionMetadata executionMetadata) {
        return new MojoExecutionBuildParticipant(execution, true, true) {
            @Override
            public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
                // build mojo like normal
                final Set<IProject> build = super.build(kind, monitor);

                // now we make sure jar is built in separate job, doing this during maven builder will throw lifecycle errors
                final IProject project = projectFacade.getProject();

                Job job = new WorkspaceJob("Executing " + project.getName() + " jar:jar goal") {
                    @Override
                    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
                        SubMonitor progress = SubMonitor.convert(monitor, 3);
                        execJarMojo(projectFacade, progress.newChild(1, SubMonitor.SUPPRESS_NONE));

                        // Find the maven output directory (usually "target")
                        MavenProject mvnProject = getMavenProject(projectFacade, progress.newChild(1));
                        IPath buildDirPath = Path.fromOSString(mvnProject.getBuild().getDirectory());
                        IPath projectPath = project.getLocation();
                        IPath relativeBuildDirPath = buildDirPath.makeRelativeTo(projectPath);
                        IFolder buildDir = project.getFolder(relativeBuildDirPath);

                        if (buildDir != null) {
                            // TODO: there *may* be a remaining issue here if a source-generation plugin gets triggered by the above invocation of the jar:jar goal.
                            // This could cause Eclipse to think that the Java sources are dirty and queue the project for rebuilding, thus entering an infinite loop.
                            // One solution would be to find the output artifact jar and refresh ONLY that. However we have not been able to create the condition we
                            // are worried about so we are deferring any extra work on this until it's shown to be a real problem.
                            buildDir.refreshLocal(IResource.DEPTH_INFINITE, progress.newChild(1));
                        } else {
                            Logger.getLogger(BndConfigurator.class).logError(String.format("Project build folder '%s' does not exist, or is not a child of the project path '%s'", buildDirPath, projectPath), null);
                            progress.worked(1);
                        }

                        return Status.OK_STATUS;
                    }
                };
                job.setRule(project);
                job.schedule();

                return build;
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

    private void execJarMojo(final IMavenProjectFacade projectFacade, IProgressMonitor monitor) throws CoreException {
        final IMaven maven = MavenPlugin.getMaven();
        ProjectRegistryManager projectRegistryManager = MavenPluginActivator.getDefault().getMavenProjectManagerImpl();

        ResolverConfiguration resolverConfiguration = new ResolverConfiguration();
        resolverConfiguration.setResolveWorkspaceProjects(true);

        IMavenExecutionContext context = projectRegistryManager.createExecutionContext(projectFacade.getPom(), resolverConfiguration);

        context.execute(new ICallable<Void>() {
            @Override
            public Void call(IMavenExecutionContext context, IProgressMonitor monitor) throws CoreException {
                SubMonitor progress = SubMonitor.convert(monitor);
                MavenProject mavenProject = getMavenProject(projectFacade, progress.newChild(1));

                MavenExecutionPlan plan = maven.calculateExecutionPlan(mavenProject, Arrays.asList("jar:jar"), true, monitor);
                List<MojoExecution> mojoExecutions = plan.getMojoExecutions();

                if (mojoExecutions != null) {
                    for (MojoExecution mojoExecution : mojoExecutions) {
                        maven.execute(mavenProject, mojoExecution, progress.newChild(1));
                    }
                }

                // We can now decorate based on the build we just did.
                try {
                    IProjectDecorator decorator = Injector.ref.get();
                    if (decorator != null) {
                        BndProjectInfo info = new MavenProjectInfo(mavenProject);
                        decorator.updateDecoration(projectFacade.getProject(), info);
                    }
                } catch (Exception e) {
                    logger.logError("Failed to decorate project " + projectFacade.getProject().getName(), e);
                }

                return null;
            }
        }, monitor);
    }

    @Component
    public static class Injector {

        private static AtomicReference<IProjectDecorator> ref = new AtomicReference<IProjectDecorator>();

        @Reference
        void setDecorator(IProjectDecorator decorator) {
            ref.set(decorator);
        }

        void unsetDecorator(IProjectDecorator decorator) {
            ref.compareAndSet(decorator, null);
        }
    }
}

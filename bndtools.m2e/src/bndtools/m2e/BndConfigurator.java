package bndtools.m2e;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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

public class BndConfigurator extends AbstractProjectConfigurator {

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
                        execJarMojo(projectFacade, monitor);

                        project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

                        return Status.OK_STATUS;
                    }
                };
                job.setRule(project);
                job.schedule();

                return build;
            }
        };
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
                MavenProject mavenProject = projectFacade.getMavenProject();

                if (mavenProject == null) {
                    mavenProject = projectFacade.getMavenProject(monitor);
                }

                MavenExecutionPlan plan = maven.calculateExecutionPlan(mavenProject, Arrays.asList("jar:jar"), true, monitor);
                List<MojoExecution> mojoExecutions = plan.getMojoExecutions();

                if (mojoExecutions != null) {
                    for (MojoExecution mojoExecution : mojoExecutions) {
                        maven.execute(mavenProject, mojoExecution, monitor);
                    }
                }

                return null;
            }
        }, monitor);
    }

}

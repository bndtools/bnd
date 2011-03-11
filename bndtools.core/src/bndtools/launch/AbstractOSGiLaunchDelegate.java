package bndtools.launch;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.Workspace;
import bndtools.Central;
import bndtools.Plugin;
import bndtools.builder.BndBuildJob;
import bndtools.builder.BndProjectNature;

public abstract class AbstractOSGiLaunchDelegate extends JavaLaunchDelegate {

    protected void waitForBuilds(IProgressMonitor monitor) {
        SubMonitor progress = SubMonitor.convert(monitor, "Waiting for background Bnd builds to complete...", 1);
        try {
            Job.getJobManager().join(BndBuildJob.class, progress.newChild(1));
        } catch (OperationCanceledException e) {
            // Ignore
        } catch (InterruptedException e) {
            // Ignore
        }
    }
    protected Project getBndProject(ILaunchConfiguration configuration) throws CoreException {
        Project result;

        String target = configuration.getAttribute(LaunchConstants.ATTR_LAUNCH_TARGET, (String) null);
        if(target == null) {
            // For compatibility with launches created in previous versions
            target = getJavaProjectName(configuration);
        }
        if(target == null) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd launch target was not specified", null));
        }

        IResource targetResource = ResourcesPlugin.getWorkspace().getRoot().findMember(target);
        if(targetResource == null)
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Bnd launch target \"{0}\" does not exist.", target), null));

        IProject project = targetResource.getProject();
        File projectDir = project.getLocation().toFile();
        if(targetResource.getType() == IResource.FILE) {
            if(!targetResource.getName().endsWith(LaunchConstants.EXT_BNDRUN))
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Bnd launch target file \"{0}\" is not a .bndrun file.", target), null));

            // Get the synthetic "run" project (based on a .bndrun file)
            File runFile = targetResource.getLocation().toFile();
            try {
                result = new Project(Central.getWorkspace(), projectDir, runFile);
            } catch (Exception e) {
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to create synthetic project for run file {0} in project {1}.", targetResource.getProjectRelativePath().toString(), project.getName()), e));
            }
        } else if(targetResource.getType() == IResource.PROJECT) {
            // Use the main project (i.e. bnd.bnd)
            if(!project.hasNature(BndProjectNature.NATURE_ID))
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("The configured run project \"{0}\"is not a Bnd project.", project.getName()), null));
            try {
                result = Workspace.getProject(projectDir);
            } catch (Exception e) {
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to retrieve Bnd project model for project \"{0}\".", project.getName()), null));
            }
        } else {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("The specified launch target \"{0}\" is not recognised as a Bnd project or .bndrun file.", targetResource.getFullPath().toString()), null));
        }

        return result;
    }

    protected abstract ProjectLauncher getProjectLauncher() throws CoreException;

    @Override
    public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
        Collection<String> paths = getProjectLauncher().getClasspath();
        return paths.toArray(new String[paths.size()]);
    }

    @Override
    public String getMainTypeName(ILaunchConfiguration configuration) throws CoreException {
        return getProjectLauncher().getMainTypeName();
    }

    @Override
    public File verifyWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
        try {
            Project project = getBndProject(configuration);
            return (project != null) ? project.getBase() : null;
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error getting working directory for Bnd project.", e));
        }
    }

    @Override
    public String getVMArguments(ILaunchConfiguration configuration) throws CoreException {
        StringBuilder builder = new StringBuilder();
        Collection<String> runVM = getProjectLauncher().getRunVM();
        for (Iterator<String> iter = runVM.iterator(); iter.hasNext();) {
            builder.append(iter.next());
            if (iter.hasNext()) builder.append(" ");
        }
        String args = builder.toString();

        args = addJavaLibraryPath(configuration, args);
        return args;
    }

    @Override
    public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
        StringBuilder builder = new StringBuilder();

        Collection<String> args = getProjectLauncher().getArguments();
        for (Iterator<String> iter = args.iterator(); iter.hasNext();) {
            builder.append(iter.next());
            if (iter.hasNext()) builder.append(" ");
        }

        return builder.toString();
    }

    protected String addJavaLibraryPath(ILaunchConfiguration configuration, String args) throws CoreException {
        // Following code copied from AbstractJavaLaunchConfigurationDelegate
        int libraryPath = args.indexOf("-Djava.library.path"); //$NON-NLS-1$
        if (libraryPath < 0) {
            // if a library path is already specified, do not override
            String[] javaLibraryPath = getJavaLibraryPath(configuration);
            if (javaLibraryPath != null && javaLibraryPath.length > 0) {
                StringBuffer path = new StringBuffer(args);
                path.append(" -Djava.library.path="); //$NON-NLS-1$
                path.append("\""); //$NON-NLS-1$
                for (int i = 0; i < javaLibraryPath.length; i++) {
                    if (i > 0) {
                        path.append(File.pathSeparatorChar);
                    }
                    path.append(javaLibraryPath[i]);
                }
                path.append("\""); //$NON-NLS-1$
                args = path.toString();
            }
        }
        return args;
    }
}
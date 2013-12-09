package bndtools.launch.util;

import java.io.File;
import java.text.MessageFormat;

import org.bndtools.api.BndtoolsConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;

import aQute.bnd.build.Project;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.launch.LaunchConstants;

public final class LaunchUtils {

    private LaunchUtils() {}

    public static IResource getTargetResource(ILaunchConfiguration configuration) throws CoreException {
        String target = configuration.getAttribute(LaunchConstants.ATTR_LAUNCH_TARGET, (String) null);
        if (target == null || target.length() == 0) {
            return null;
        }

        IResource targetResource = ResourcesPlugin.getWorkspace().getRoot().findMember(target);
        if (targetResource == null)
            return null;

        return targetResource;
    }

    public static String getLaunchProjectName(IResource launchResource) {
        String result;

        IProject project = launchResource.getProject();
        Project bnd;
        try {
            bnd = Central.getWorkspace().getProject(project.getName());
        } catch (Exception e) {
            bnd = null;
        }

        result = (bnd != null) ? bnd.getName() : Project.BNDCNF;
        return result;
    }

    public static Project getBndProject(ILaunchConfiguration configuration) throws CoreException {
        IResource targetResource = getTargetResource(configuration);
        if (targetResource == null)
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Bnd launch target was not specified, or does not exist.", null));
        return getBndProject(targetResource);
    }

    public static Project getBndProject(IResource targetResource) throws CoreException {
        Project result;

        IProject project = targetResource.getProject();
        File projectDir = project.getLocation().toFile();

        if (targetResource.getType() == IResource.FILE && targetResource.getName().endsWith(LaunchConstants.EXT_BNDRUN)) {
            if (!targetResource.getName().endsWith(LaunchConstants.EXT_BNDRUN))
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Bnd launch target file \"{0}\" is not a .bndrun file.", targetResource.getFullPath().toString()), null));

            // Get the synthetic "run" project (based on a .bndrun file)
            File runFile = targetResource.getLocation().toFile();
            File bndbnd = new File(runFile.getParentFile(), Project.BNDFILE);
            try {
                Project parent = new Project(Central.getWorkspace(), projectDir, bndbnd);
                result = new Project(Central.getWorkspace(), projectDir, runFile);
                result.setParent(parent);
            } catch (Exception e) {
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to create synthetic project for run file {0} in project {1}.", targetResource.getProjectRelativePath().toString(),
                        project.getName()), e));
            }
        } else if (targetResource.getType() == IResource.PROJECT || targetResource.getName().equals(Project.BNDFILE)) {
            // Use the main project (i.e. bnd.bnd)
            if (!project.hasNature(BndtoolsConstants.NATURE_ID))
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("The configured run project \"{0}\"is not a Bnd project.", project.getName()), null));
            try {
                result = Central.getProject(projectDir);
            } catch (Exception e) {
                throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to retrieve Bnd project model for project \"{0}\".", project.getName()), null));
            }
        } else {
            throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("The specified launch target \"{0}\" is not recognised as a bnd project, bnd.bnd or .bndrun file.", targetResource.getFullPath()
                    .toString()), null));
        }

        return result;
    }
}

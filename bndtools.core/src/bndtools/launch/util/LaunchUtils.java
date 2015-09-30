package bndtools.launch.util;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
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

    public static Project getBndProject(ILaunchConfiguration configuration) throws Exception {
        IResource targetResource = getTargetResource(configuration);
        if (targetResource == null)
            throw new IllegalArgumentException("Bnd launch target was not specified, or does not exist.", null);

        return getBndProject(targetResource);
    }

    public static Run getBndProject(IResource targetResource) throws Exception {
        Run run;

        Workspace ws = Central.getWorkspaceIfPresent();
        if (targetResource.getType() == IResource.PROJECT) {
            // This is a bnd project --> find the bnd.bnd file
            IProject project = (IProject) targetResource;
            if (ws == null)
                throw new Exception(String.format("Cannot load bnd project for directory %s: no bnd workspace found", project.getLocation()));
            File bndFile = project.getFile(Project.BNDFILE).getLocation().toFile();
            if (bndFile == null || !bndFile.isFile())
                throw new Exception(String.format("Failed to load bnd project for directory %s: %s does not exist or is not a file.", project.getLocation(), Project.BNDFILE));

            run = Run.createRun(ws, bndFile);
        } else if (targetResource.getType() == IResource.FILE) {
            // This is file, use directly
            File file = targetResource.getLocation().toFile();
            if (file == null || !file.isFile())
                throw new Exception(String.format("Failed to create bnd launch configuration: %s does not exist or is not a file.", file));
            run = Run.createRun(ws, file);
        } else {
            throw new Exception(String.format("Cannot create a bnd launch configuration for %s: not a project or file resource.", targetResource.getLocation()));
        }
        return run;
    }
}

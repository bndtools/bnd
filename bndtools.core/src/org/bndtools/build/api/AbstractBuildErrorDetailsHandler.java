package org.bndtools.build.api;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import aQute.bnd.build.Project;

public abstract class AbstractBuildErrorDetailsHandler implements BuildErrorDetailsHandler {

    public static final IResource getDefaultResource(IProject project) {
        IResource resource;
        IFile bndFile = project.getFile(Project.BNDFILE);
        if (bndFile == null || !bndFile.exists())
            resource = project;
        else
            resource = bndFile;
        return resource;
    }

}

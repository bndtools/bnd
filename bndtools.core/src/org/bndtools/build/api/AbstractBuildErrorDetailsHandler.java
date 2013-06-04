package org.bndtools.build.api;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IMarkerResolution;

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

    public List<IMarkerResolution> getResolutions(IMarker marker) {
        return Collections.emptyList();
    }

}

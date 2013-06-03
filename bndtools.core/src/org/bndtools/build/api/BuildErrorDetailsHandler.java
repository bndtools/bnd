package org.bndtools.build.api;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import aQute.bnd.build.Project;
import aQute.bnd.osgi.Processor;
import aQute.service.reporter.Report.Location;

/**
 * Implementations of this interface handle a specific type of build error detail as returned from bnd's
 * {@link Processor#getLocation(String)} method.
 * 
 * @author Neil Bartlett <njbartlett@gmail.com>
 */
public interface BuildErrorDetailsHandler {

    IResource findMarkerTargetResource(IProject project, Project model, Location location) throws Exception;

    Map<String,Object> createMarkerAttributes(IProject project, Project model, Location location, IResource resource) throws Exception;

}

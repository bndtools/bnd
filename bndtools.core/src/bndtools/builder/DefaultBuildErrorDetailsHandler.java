package bndtools.builder;

import java.util.HashMap;
import java.util.Map;

import org.bndtools.build.api.AbstractBuildErrorDetailsHandler;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import aQute.bnd.build.Project;
import aQute.service.reporter.Report.Location;

final class DefaultBuildErrorDetailsHandler extends AbstractBuildErrorDetailsHandler {

    static final DefaultBuildErrorDetailsHandler INSTANCE = new DefaultBuildErrorDetailsHandler();

    public IResource findMarkerTargetResource(IProject project, Project model, Location location) throws Exception {
        return getDefaultResource(project);
    }

    public Map<String,Object> createMarkerAttributes(IProject project, Project model, Location location, IResource resource) throws Exception {
        Map<String,Object> map = new HashMap<String,Object>();
        map.put(IMarker.MESSAGE, location.message.trim());
        return map;
    }

}
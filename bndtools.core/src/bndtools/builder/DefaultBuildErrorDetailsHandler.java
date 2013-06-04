package bndtools.builder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bndtools.build.api.AbstractBuildErrorDetailsHandler;
import org.bndtools.build.api.MarkerData;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;

import aQute.bnd.build.Project;
import aQute.service.reporter.Report.Location;

final class DefaultBuildErrorDetailsHandler extends AbstractBuildErrorDetailsHandler {

    static final DefaultBuildErrorDetailsHandler INSTANCE = new DefaultBuildErrorDetailsHandler();

    public List<MarkerData> generateMarkerData(IProject project, Project model, Location location) throws Exception {
        Map<String,Object> attribs = new HashMap<String,Object>();
        attribs.put(IMarker.MESSAGE, location.message.trim());

        return Collections.singletonList(new MarkerData(getDefaultResource(project), attribs, false));
    }

}
package bndtools.editor.project;

import java.util.Arrays;
import java.util.List;

import org.bndtools.build.api.AbstractBuildErrorDetailsHandler;
import org.bndtools.build.api.MarkerData;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IMarkerResolution;

import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.PropertyConflict;
import aQute.service.reporter.Report.Location;

public class PropertyConflictDetailsHandler extends AbstractBuildErrorDetailsHandler {
	@Override
	public List<MarkerData> generateMarkerData(IProject project, Processor model, Location location) throws Exception {
		PropertyConflict conflict = (PropertyConflict) location.details;
		return List.of(new MarkerData(IncludeConflictDetector.target(location, getDefaultResource(project)),
			IncludeConflictDetector.attributes(conflict, location, model), conflict.mergeable()));
	}

	@Override
	public List<IMarkerResolution> getResolutions(IMarker marker) {
		return Arrays.asList(new IncludeConflictMarkerResolutionGenerator().getResolutions(marker));
	}
}

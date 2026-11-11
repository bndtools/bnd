package bndtools.editor.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bndtools.build.api.AbstractBuildErrorDetailsHandler;
import org.bndtools.build.api.MarkerData;
import org.bndtools.core.editors.MarkerResolutionProposal;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
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

	@Override
	public List<ICompletionProposal> getProposals(IMarker marker) {
		List<ICompletionProposal> proposals = new ArrayList<>();
		for (IMarkerResolution resolution : getResolutions(marker)) {
			proposals.add(new MarkerResolutionProposal(resolution, marker));
		}
		return proposals;
	}
}

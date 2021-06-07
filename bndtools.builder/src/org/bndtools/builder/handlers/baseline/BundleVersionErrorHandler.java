package org.bndtools.builder.handlers.baseline;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.build.api.AbstractBuildErrorDetailsHandler;
import org.bndtools.build.api.MarkerData;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.osgi.framework.Constants;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.differ.Baseline.BundleInfo;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Processor.FileLine;
import aQute.service.reporter.Report.Location;
import bndtools.central.Central;

public class BundleVersionErrorHandler extends AbstractBuildErrorDetailsHandler {

	private static final String		PROP_SUGGESTED_VERSION			= "suggestedVersion";

	private final static String		VERSION_ACCEPTING_MACRO_STRING	= "(\\d+)\\.(\\d+)\\.(\\d+)\\.([-.${}\\w]+)";		//$NON-NLS-1$
	private final static Pattern	VERSION_ACCEPTING_MACRO			= Pattern.compile(VERSION_ACCEPTING_MACRO_STRING);

	@Override
	public List<MarkerData> generateMarkerData(IProject project, Project model, Location location) throws Exception {

		List<MarkerData> result = new LinkedList<>();

		BundleInfo info = (BundleInfo) location.details;
		try (ProjectBuilder pb = model.getBuilder(null)) {
			for (Builder builder : pb.getSubBuilders()) {
				if (builder.getBsn()
					.equals(info.bsn)) {
					String currentVersion = builder.getUnprocessedProperty(Constants.BUNDLE_VERSION, null);
					FileLine loc = builder.getHeader(Constants.BUNDLE_VERSION, currentVersion);

					Map<String, Object> attribs = new HashMap<>();
					attribs.put(IMarker.MESSAGE, location.message);
					attribs.put(IMarker.LINE_NUMBER, loc.line);
					attribs.put(IMarker.CHAR_START, loc.start);
					attribs.put(IMarker.CHAR_END, loc.end);
					attribs.put(BndtoolsConstants.BNDTOOLS_MARKER_PROJECT_ATTR, project.getName());

					String qualifier = null;
					if (currentVersion != null) {
						Matcher m = VERSION_ACCEPTING_MACRO.matcher(currentVersion);
						if (m.matches()) {
							qualifier = m.group(4);
						}
					}
					attribs.put(PROP_SUGGESTED_VERSION,
						info.suggestedVersion.toString() + (qualifier != null ? '.' + qualifier : ""));

					IResource bndFile = Central.toResource(loc.file);
					result.add(new MarkerData(bndFile, attribs, true, BndtoolsConstants.MARKER_JAVA_BASELINE));
				}
			}
		}

		return result;
	}

	@Override
	public List<ICompletionProposal> getProposals(IMarker marker) {
		List<ICompletionProposal> result = new LinkedList<>();

		String suggestedVersion = marker.getAttribute(PROP_SUGGESTED_VERSION, null);
		int start = marker.getAttribute(IMarker.CHAR_START, 0);
		int end = marker.getAttribute(IMarker.CHAR_END, 0);
		CompletionProposal proposal = new CompletionProposal(Constants.BUNDLE_VERSION + ": " + suggestedVersion, start,
			end - start, end, null, "Change bundle version to " + suggestedVersion, null, null);
		result.add(proposal);

		return result;
	}

}

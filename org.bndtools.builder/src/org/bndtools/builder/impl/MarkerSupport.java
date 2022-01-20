package org.bndtools.builder.impl;

import static org.bndtools.api.BndtoolsConstants.BNDTOOLS_MARKER_CONTEXT_ATTR;
import static org.bndtools.api.BndtoolsConstants.BNDTOOLS_MARKER_FILE_ATTR;
import static org.bndtools.api.BndtoolsConstants.BNDTOOLS_MARKER_HEADER_ATTR;
import static org.bndtools.api.BndtoolsConstants.BNDTOOLS_MARKER_PROJECT_ATTR;
import static org.bndtools.api.BndtoolsConstants.BNDTOOLS_MARKER_REFERENCE_ATTR;
import static org.bndtools.api.BndtoolsConstants.MARKER_BND_BLOCKER;
import static org.bndtools.api.BndtoolsConstants.MARKER_BND_MISSING_WORKSPACE;
import static org.bndtools.api.BndtoolsConstants.MARKER_BND_PATH_PROBLEM;
import static org.bndtools.api.BndtoolsConstants.MARKER_BND_PROBLEM;
import static org.bndtools.api.BndtoolsConstants.MARKER_BND_WORKSPACE_PROBLEM;
import static org.bndtools.api.BndtoolsConstants.MARKER_JAVA_BASELINE;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.bndtools.api.BndtoolsConstants;
import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.builder.BuildErrorDetailsHandler;
import org.bndtools.api.builder.MarkerData;
import org.bndtools.build.api.AbstractBuildErrorDetailsHandler;
import org.bndtools.build.api.BuildErrorDetailsHandlers;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
import aQute.service.reporter.Report.Location;

class MarkerSupport {
	private static final ILogger			logger			= Logger.getLogger(MarkerSupport.class);
	private final IProject					project;

	private static final org.slf4j.Logger	consoleLogger	= LoggerFactory.getLogger(MarkerSupport.class);

	MarkerSupport(IProject project) {
		this.project = project;
	}

	boolean hasBlockingErrors(DeltaWrapper dw) {
		try {
			if (containsError(dw, findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER)))
				return true;

			if (containsError(dw, findMarkers(MARKER_BND_PATH_PROBLEM)))
				return true;

			if (containsError(dw, findMarkers(MARKER_BND_MISSING_WORKSPACE)))
				return true;

			return false;
		} catch (CoreException e) {
			logger.logError("Error looking for project problem markers", e);
			return false;
		}
	}

	private IMarker[] findMarkers(String markerType) throws CoreException {
		IMarker[] markers = project.findMarkers(markerType, false, IResource.DEPTH_INFINITE);
		return markers;
	}

	void setMarkers(Processor model, String markerType) throws Exception {
		deleteMarkers(markerType);
		createMarkers(model, IMarker.SEVERITY_ERROR, model.getErrors(), markerType);
		createMarkers(model, IMarker.SEVERITY_WARNING, model.getWarnings(), markerType);
	}

	void deleteMarkers(String markerType) throws CoreException {
		if (markerType.equals("*")) {
			deleteMarkers(MARKER_BND_BLOCKER);
			deleteMarkers(MARKER_BND_PROBLEM);
			deleteMarkers(MARKER_BND_PATH_PROBLEM);
			deleteMarkers(MARKER_BND_WORKSPACE_PROBLEM);
			deleteMarkers(MARKER_BND_MISSING_WORKSPACE);
			deleteMarkers(MARKER_JAVA_BASELINE);
		} else {
			if (project.exists() && project.isAccessible()) {
				project.deleteMarkers(markerType, true, IResource.DEPTH_INFINITE);
				deleteCnfMarkersForThisProject(markerType);
			}
		}
	}

	private void deleteCnfMarkersForThisProject(String markerType) throws CoreException {
		IProject cnf = ResourcesPlugin.getWorkspace()
			.getRoot()
			.getProject("cnf");
		if (cnf != null && cnf.isOpen()) {
			IMarker[] markers = cnf.findMarkers(markerType, true, IResource.DEPTH_INFINITE);
			if (markers != null) {
				for (IMarker marker : markers) {
					String projectName = marker.getAttribute(BndtoolsConstants.BNDTOOLS_MARKER_PROJECT_ATTR, null);
					if (projectName == null || projectName.equals(project.getName())) {
						marker.delete();
					}
				}
			}
		}
	}

	private void createMarkers(Processor model, int severity, Collection<String> msgs, String markerType)
		throws Exception {
		for (String msg : msgs.toArray(new String[0])) {
			createMarker(model, severity, msg, markerType);
		}
	}

	void createMarker(Processor model, int severity, String formatted, String markerType) throws Exception {
		Location location = model != null ? model.getLocation(formatted) : null;
		if (location != null) {
			String type = location.details != null ? location.details.getClass()
				.getName() : null;
			BuildErrorDetailsHandler handler = BuildErrorDetailsHandlers.INSTANCE.findHandler(type);

			List<MarkerData> markers = handler.generateMarkerData(project, model, location);
			if (!markers.isEmpty()) {
				for (MarkerData markerData : markers) {
					IResource resource = markerData.getResource();
					if (resource != null && resource.exists()) {
						String typeOverride = markerData.getTypeOverride();
						IMarker marker = resource.createMarker(typeOverride != null ? typeOverride : markerType);
						marker.setAttribute(IMarker.SEVERITY, severity);
						marker.setAttribute("$bndType", type);

						//
						// Set location information
						if (location.header != null)
							marker.setAttribute(BNDTOOLS_MARKER_HEADER_ATTR, location.header);
						if (location.context != null)
							marker.setAttribute(BNDTOOLS_MARKER_CONTEXT_ATTR, location.context);
						if (location.file != null)
							marker.setAttribute(BNDTOOLS_MARKER_FILE_ATTR, location.file);
						if (location.reference != null)
							marker.setAttribute(BNDTOOLS_MARKER_REFERENCE_ATTR, location.reference);

						marker.setAttribute(BNDTOOLS_MARKER_PROJECT_ATTR, project.getName());

						marker.setAttribute(BuildErrorDetailsHandler.PROP_HAS_RESOLUTIONS, markerData.hasResolutions());
						for (Entry<String, Object> attrib : markerData.getAttribs()
							.entrySet())
							marker.setAttribute(attrib.getKey(), attrib.getValue());
					}
				}
				return;
			}
		}

		String defaultResource = model instanceof Project ? Project.BNDFILE
			: model instanceof Workspace ? Workspace.BUILDFILE : null;
		IResource resource = AbstractBuildErrorDetailsHandler.getDefaultResource(project, defaultResource);
		if (resource.exists()) {
			IMarker marker = resource.createMarker(markerType);
			marker.setAttribute(IMarker.SEVERITY, severity);
			marker.setAttribute(IMarker.MESSAGE, formatted);
		}
	}

	private static boolean containsError(DeltaWrapper dw, IMarker[] markers) {
		if (markers != null)
			for (IMarker marker : markers) {

				int severity = marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
				if (severity == IMarker.SEVERITY_ERROR) {

					//
					// markers in the test folder don't count.
					// They should not rebuild nor stop building
					// the target
					//

					if (dw.isTestBin(marker.getResource()))
						continue;

					consoleLogger.debug("error marker {}", marker);
					return true;
				}
			}
		return false;
	}
}

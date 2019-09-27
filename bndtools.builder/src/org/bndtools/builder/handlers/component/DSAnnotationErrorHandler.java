package org.bndtools.builder.handlers.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bndtools.build.api.AbstractBuildErrorDetailsHandler;
import org.bndtools.build.api.MarkerData;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.build.Project;
import aQute.bnd.component.error.DeclarativeServicesAnnotationError;
import aQute.service.reporter.Report.Location;

public class DSAnnotationErrorHandler extends AbstractBuildErrorDetailsHandler {

	@Override
	public List<MarkerData> generateMarkerData(IProject project, Project model, Location location) throws Exception {
		List<MarkerData> result = new ArrayList<>();

		DeclarativeServicesAnnotationError dsError = (DeclarativeServicesAnnotationError) location.details;

		IJavaProject javaProject = JavaCore.create(project);

		Map<String, Object> attribs = new HashMap<>();
		attribs.put(IMarker.MESSAGE, location.message.trim());

		MarkerData md = null;
		if (dsError.className != null) {
			if (dsError.methodName != null && dsError.methodSignature != null) {
				md = createMethodMarkerData(javaProject, dsError.className, dsError.methodName, dsError.methodSignature,
					attribs, false);
			}

			if (dsError.fieldName != null) {
				md = createFieldMarkerData(javaProject, dsError.className, dsError.fieldName, attribs, false);
			}

			if (md == null) {
				md = createTypeMarkerData(javaProject, dsError.className, attribs, false);
			}
		}

		if (md != null) {
			result.add(md);
		} else {
			// No other marker could be created, so add a marker to the bnd file
			result.add(new MarkerData(getDefaultResource(project), attribs, false));
		}

		return result;
	}

}

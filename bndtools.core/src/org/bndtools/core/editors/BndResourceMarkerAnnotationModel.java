package org.bndtools.core.editors;

import org.bndtools.build.api.BuildErrorDetailsHandler;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;

public class BndResourceMarkerAnnotationModel extends ResourceMarkerAnnotationModel {

	public BndResourceMarkerAnnotationModel(IResource resource) {
		super(resource);
	}

	@Override
	protected MarkerAnnotation createMarkerAnnotation(IMarker marker) {
		MarkerAnnotation annotation = super.createMarkerAnnotation(marker);

		boolean fixable = marker.getAttribute(BuildErrorDetailsHandler.PROP_HAS_RESOLUTIONS, false);
		annotation.setQuickFixable(fixable);

		return annotation;
	}

}

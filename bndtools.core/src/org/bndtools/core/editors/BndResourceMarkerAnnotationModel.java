package org.bndtools.core.editors;

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

		boolean fixable = new BndMarkerQuickAssistProcessor().canFix(annotation);
		annotation.setQuickFixable(fixable);

		return annotation;
	}

}

package org.bndtools.core.editors;

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.MarkerAnnotation;

public class BndMarkerAnnotationHover implements IAnnotationHover {

	@Override
	public String getHoverInfo(ISourceViewer sourceViewer, int lineNum) {
		@SuppressWarnings("rawtypes")
		Iterator iter = sourceViewer.getAnnotationModel()
			.getAnnotationIterator();
		while (iter.hasNext()) {
			Object annotation = iter.next();
			if (annotation instanceof MarkerAnnotation) {
				IMarker marker = ((MarkerAnnotation) annotation).getMarker();

				int markerLine = marker.getAttribute(IMarker.LINE_NUMBER, 0);
				// Hover line is zero-based and marker line is one-based. FML.
				if (markerLine == lineNum + 1) {
					return marker.getAttribute(IMarker.MESSAGE, null);
				}
			}
		}

		return null;
	}

}

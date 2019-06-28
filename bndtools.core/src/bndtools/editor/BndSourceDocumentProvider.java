package bndtools.editor;

import org.bndtools.core.editors.BndResourceMarkerAnnotationModel;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

public class BndSourceDocumentProvider extends TextFileDocumentProvider {
	@Override
	protected IAnnotationModel createAnnotationModel(IFile file) {
		return new BndResourceMarkerAnnotationModel(file);
	}

	@Override
	public String getDefaultEncoding() {
		return "UTF-8";
	}

}

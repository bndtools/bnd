package org.bndtools.core.editors.pkginfo;

import org.bndtools.core.editors.BndResourceMarkerAnnotationModel;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;

public class PackageInfoDocumentProvider extends TextFileDocumentProvider {

	@Override
	protected IAnnotationModel createAnnotationModel(IFile file) {
		return new BndResourceMarkerAnnotationModel(file);
	}

}

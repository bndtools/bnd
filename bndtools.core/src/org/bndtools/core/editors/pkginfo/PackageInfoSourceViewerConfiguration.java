package org.bndtools.core.editors.pkginfo;

import org.bndtools.core.editors.BndMarkerQuickAssistProcessor;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

public class PackageInfoSourceViewerConfiguration extends TextSourceViewerConfiguration {

	@Override
	public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
		QuickAssistAssistant assistant = new QuickAssistAssistant();

		assistant.setQuickAssistProcessor(new BndMarkerQuickAssistProcessor());

		return assistant;
	}

}

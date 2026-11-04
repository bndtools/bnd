package org.bndtools.core.editors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.junit.jupiter.api.Test;

public class BndMarkerQuickAssistProcessorTest {

	@Test
	void offersFallbackForEveryMarkerOnSameLine() throws Exception {
		IMarker first = mock(IMarker.class);
		IMarker second = mock(IMarker.class);
		when(first.getType()).thenReturn(IMarker.PROBLEM);
		when(second.getType()).thenReturn(IMarker.PROBLEM);
		IMarkerResolution firstResolution = mock(IMarkerResolution.class);
		IMarkerResolution secondResolution = mock(IMarkerResolution.class);
		when(firstResolution.getLabel()).thenReturn("first");
		when(secondResolution.getLabel()).thenReturn("second");
		MarkerAnnotation firstAnnotation = mock(MarkerAnnotation.class);
		MarkerAnnotation secondAnnotation = mock(MarkerAnnotation.class);
		when(firstAnnotation.getMarker()).thenReturn(first);
		when(secondAnnotation.getMarker()).thenReturn(second);
		AnnotationModel model = new AnnotationModel();
		model.addAnnotation(firstAnnotation, new Position(0, 5));
		model.addAnnotation(secondAnnotation, new Position(0, 5));
		ISourceViewer viewer = mock(ISourceViewer.class);
		when(viewer.getDocument()).thenReturn(new Document("value\n"));
		when(viewer.getAnnotationModel()).thenReturn(model);
		IQuickAssistInvocationContext context = mock(IQuickAssistInvocationContext.class);
		when(context.getSourceViewer()).thenReturn(viewer);
		BndMarkerQuickAssistProcessor processor = new BndMarkerQuickAssistProcessor() {
			@Override
			public boolean canFix(Annotation annotation) {
				return true;
			}

			@Override
			IMarkerResolution[] getMarkerResolutions(IMarker marker) {
				return new IMarkerResolution[] {marker == first ? firstResolution : secondResolution};
			}
		};
		assertThat(processor.computeQuickAssistProposals(context)).extracting(proposal -> proposal.getDisplayString())
			.containsExactlyInAnyOrder("first", "second");
	}

	@Test
	void missingViewerReturnsNoCompletions() {
		IQuickAssistInvocationContext context = mock(IQuickAssistInvocationContext.class);
		assertThat(new BndMarkerQuickAssistProcessor().computeQuickAssistProposals(context))
			.hasSize(1).allMatch(proposal -> proposal instanceof NoCompletionsProposal);
	}
}
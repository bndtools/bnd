package org.bndtools.core.editors;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bndtools.build.api.BuildErrorDetailsHandler;
import org.bndtools.build.api.BuildErrorDetailsHandlers;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import bndtools.editor.project.IncludeConflictDetector;

public class BndMarkerQuickAssistProcessor implements IQuickAssistProcessor {

	@Override
	public boolean canAssist(IQuickAssistInvocationContext context) {
		return false;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public boolean canFix(Annotation annotation) {
		if (annotation instanceof MarkerAnnotation) {
			IMarker marker = ((MarkerAnnotation) annotation).getMarker();
			if (marker == null || !marker.exists())
				return false;
			if (marker.getAttribute(BuildErrorDetailsHandler.PROP_HAS_RESOLUTIONS, false))
				return true;
			if (marker.getAttribute(IncludeConflictDetector.ATTR_MERGEABLE, false))
				return true;
			try {
				return IDE.getMarkerHelpRegistry().hasResolutions(marker);
			} catch (Throwable e) {
				// ignore
			}
		}
		return false;
	}

	private boolean isAtPosition(int offset, Position pos, IDocument document) {
		if (pos == null)
			return false;
		if (offset >= pos.getOffset() && offset <= (pos.getOffset() + pos.getLength()))
			return true;
		if (document != null) {
			try {
				int targetLine = document.getLineOfOffset(offset);
				int startLine = document.getLineOfOffset(pos.getOffset());
				int endLine = document.getLineOfOffset(Math.max(pos.getOffset(), pos.getOffset() + pos.getLength() - 1));
				if (targetLine >= startLine && targetLine <= endLine)
					return true;
			} catch (BadLocationException e) {
				// ignore
			}
		}
		return false;
	}

	@Override
	public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext context) {
		List<ICompletionProposal> proposals = new LinkedList<>();

		ISourceViewer viewer = context.getSourceViewer();
		IDocument document = viewer != null ? viewer.getDocument() : null;
		IAnnotationModel model = viewer != null ? viewer.getAnnotationModel() : null;

		if (model != null) {
			@SuppressWarnings("rawtypes")
			Iterator iter = model.getAnnotationIterator();
			while (iter.hasNext()) {
				Annotation annotation = (Annotation) iter.next();
				if (annotation instanceof MarkerAnnotation && canFix(annotation)) {
					Position position = model.getPosition(annotation);
					if (isAtPosition(context.getOffset(), position, document)) {
						IMarker marker = ((MarkerAnnotation) annotation).getMarker();
						String errorType = marker.getAttribute("$bndType", null);
						if (errorType != null) {
							BuildErrorDetailsHandler handler = BuildErrorDetailsHandlers.INSTANCE.findHandler(errorType);
							if (handler != null) {
								List<ICompletionProposal> handlerProposals = handler.getProposals(marker);
								if (handlerProposals != null && !handlerProposals.isEmpty()) {
									proposals.addAll(handlerProposals);
								} else {
									List<IMarkerResolution> resolutions = handler.getResolutions(marker);
									if (resolutions != null) {
										for (IMarkerResolution resolution : resolutions) {
											proposals.add(new MarkerResolutionProposal(resolution, marker));
										}
									}
								}
							}
						}
						if (proposals.isEmpty()) {
							try {
								IMarkerResolution[] ideResolutions = IDE.getMarkerHelpRegistry().getResolutions(marker);
								if (ideResolutions != null) {
									for (IMarkerResolution resolution : ideResolutions) {
										proposals.add(new MarkerResolutionProposal(resolution, marker));
									}
								}
							} catch (Throwable e) {
								// ignore
							}
						}
					}
				}
			}
		}

		if (proposals.isEmpty()) {
			proposals.add(new NoCompletionsProposal());
		}

		return proposals.toArray(new ICompletionProposal[0]);
	}

}

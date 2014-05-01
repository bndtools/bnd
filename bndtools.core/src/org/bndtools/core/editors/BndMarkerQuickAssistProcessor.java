package org.bndtools.core.editors;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bndtools.build.api.BuildErrorDetailsHandler;
import org.bndtools.build.api.BuildErrorDetailsHandlers;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.MarkerAnnotation;

public class BndMarkerQuickAssistProcessor implements IQuickAssistProcessor {

    public boolean canAssist(IQuickAssistInvocationContext context) {
        return false;
    }

    public String getErrorMessage() {
        return null;
    }

    public boolean canFix(Annotation annotation) {
        if (annotation instanceof MarkerAnnotation) {
            IMarker marker = ((MarkerAnnotation) annotation).getMarker();
            return marker.getAttribute(BuildErrorDetailsHandler.PROP_HAS_RESOLUTIONS, false);
        }
        return false;
    }

    private boolean isAtPosition(int offset, Position pos) {
        return (pos != null) && (offset >= pos.getOffset() && offset <= (pos.getOffset() + pos.getLength()));
    }

    public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext context) {
        List<ICompletionProposal> proposals = new LinkedList<ICompletionProposal>();

        ISourceViewer viewer = context.getSourceViewer();
        @SuppressWarnings("unused")
        IDocument document = viewer.getDocument();
        IAnnotationModel model = viewer.getAnnotationModel();

        @SuppressWarnings("rawtypes")
        Iterator iter = model.getAnnotationIterator();
        while (iter.hasNext()) {
            Annotation annotation = (Annotation) iter.next();
            if (annotation instanceof MarkerAnnotation && canFix(annotation)) {
                Position position = model.getPosition(annotation);
                if (isAtPosition(context.getOffset(), position)) {
                    IMarker marker = ((MarkerAnnotation) annotation).getMarker();
                    String errorType = marker.getAttribute("$bndType", null);
                    if (errorType != null) {
                        BuildErrorDetailsHandler handler = BuildErrorDetailsHandlers.INSTANCE.findHandler(errorType);
                        if (handler != null) {
                            proposals.addAll(handler.getProposals(marker));
                        }
                    }
                }
            }
        }

        if (proposals.isEmpty()) {
            proposals.add(new NoCompletionsProposal());
        }

        return proposals.toArray(new ICompletionProposal[proposals.size()]);
    }

}

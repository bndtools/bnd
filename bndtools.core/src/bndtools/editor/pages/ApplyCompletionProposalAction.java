package bndtools.editor.pages;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.texteditor.ITextEditor;

public class ApplyCompletionProposalAction extends Action {

    private final ICompletionProposal proposal;
    private final ITextEditor textEditor;
    private final FormEditor mainEditor;
    private final String switchToPageId;

    public ApplyCompletionProposalAction(ICompletionProposal proposal, ITextEditor textEditor, FormEditor mainEditor, String switchToPageId) {
        this.proposal = proposal;
        this.textEditor = textEditor;
        this.mainEditor = mainEditor;
        this.switchToPageId = switchToPageId;
    }

    @Override
    public String getText() {
        return proposal.getDisplayString();
    }

    @Override
    public void run() {
        assert (proposal != null);
        assert (textEditor != null);
        assert (mainEditor != null);
        assert (switchToPageId != null);

        mainEditor.setActivePage(switchToPageId);

        IDocument document = textEditor.getDocumentProvider().getDocument(mainEditor.getEditorInput());
        proposal.apply(document);

        Point selection = proposal.getSelection(document);
        if (selection != null)
            textEditor.selectAndReveal(selection.x, 0);
    }
}
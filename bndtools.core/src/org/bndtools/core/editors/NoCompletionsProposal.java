package org.bndtools.core.editors;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

/**
 * Proposal telling that there are no proposals available.
 * <p>
 * Applying this proposal does nothing.
 * </p>
 * 
 * @since 3.3
 */
public final class NoCompletionsProposal implements ICompletionProposal {

    /*
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)
     */
    public void apply(IDocument document) {
        // do nothing
    }

    /*
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
     */
    public String getAdditionalProposalInfo() {
        return null;
    }

    /*
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getContextInformation()
     */
    public IContextInformation getContextInformation() {
        return null;
    }

    /*
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString()
     */
    public String getDisplayString() {
        return "No suggestions available";
    }

    /*
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
     */
    public Image getImage() {
        return null;
    }

    /*
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection(org.eclipse.jface.text.IDocument)
     */
    public Point getSelection(IDocument document) {
        return null;
    }

}

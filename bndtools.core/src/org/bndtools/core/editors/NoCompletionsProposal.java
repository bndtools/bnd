package org.bndtools.core.editors;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

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
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.
	 * eclipse.jface.text.IDocument)
	 */
	@Override
	public void apply(IDocument document) {
		// do nothing
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#
	 * getAdditionalProposalInfo()
	 */
	@Override
	public String getAdditionalProposalInfo() {
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#
	 * getContextInformation()
	 */
	@Override
	public IContextInformation getContextInformation() {
		return null;
	}

	/*
	 * @see
	 * org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString
	 * ()
	 */
	@Override
	public String getDisplayString() {
		return "No suggestions available";
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
	 */
	@Override
	public Image getImage() {
		return null;
	}

	/*
	 * @see
	 * org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection(org
	 * .eclipse.jface.text.IDocument)
	 */
	@Override
	public Point getSelection(IDocument document) {
		return null;
	}

}

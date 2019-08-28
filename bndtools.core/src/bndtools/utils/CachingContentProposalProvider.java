package bndtools.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

public abstract class CachingContentProposalProvider implements IContentProposalProvider, IContentProposalListener2 {

	protected String									initialContent		= null;
	protected Collection<? extends IContentProposal>	initialProposals	= null;

	@Override
	public final IContentProposal[] getProposals(String contents, int position) {
		Collection<? extends IContentProposal> currentProposals;

		if (initialProposals == null || initialContent == null || contents.length() < initialContent.length()) {
			currentProposals = doGenerateProposals(contents, position);
			initialContent = contents;
			initialProposals = currentProposals;
		} else {
			List<IContentProposal> temp = new ArrayList<>(initialProposals.size());
			for (IContentProposal proposal : initialProposals) {
				if (match(contents, position, proposal)) {
					temp.add(proposal);
				}
			}
			currentProposals = temp;
		}

		return currentProposals.toArray(new IContentProposal[0]);
	}

	protected abstract Collection<? extends IContentProposal> doGenerateProposals(String contents, int position);

	protected abstract boolean match(String contents, int position, IContentProposal proposal);

	public void reset() {
		initialContent = null;
		initialProposals = null;
	}

	@Override
	public void proposalPopupClosed(ContentProposalAdapter adapter) {
		reset();
	}

	@Override
	public void proposalPopupOpened(ContentProposalAdapter adapter) {}
}

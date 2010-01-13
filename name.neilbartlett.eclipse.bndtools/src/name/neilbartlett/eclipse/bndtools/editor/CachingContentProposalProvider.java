package name.neilbartlett.eclipse.bndtools.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

public abstract class CachingContentProposalProvider implements IContentProposalProvider, IContentProposalListener2 {
	
	protected String initialContent = null;
	protected List<IContentProposal> initialProposals = null;

	public final IContentProposal[] getProposals(String contents, int position) {
		String prefix = contents.substring(0, position);
		List<IContentProposal> currentProposals;
		
		if(initialProposals == null || initialContent == null || prefix.length() < initialContent.length()) {
			currentProposals = doGenerateProposals(prefix);
			initialContent = prefix;
			initialProposals = currentProposals;
		} else {
			currentProposals = new ArrayList<IContentProposal>(initialProposals.size());
			for (IContentProposal proposal : initialProposals) {
				if(match(prefix, proposal)) {
					currentProposals.add(proposal);
				}
			}
		}
		
		return currentProposals.toArray(new IContentProposal[currentProposals.size()]);
	}

	protected abstract List<IContentProposal> doGenerateProposals(String prefix);
	
	protected abstract boolean match(String prefix, IContentProposal proposal);
	
	public void reset() {
		initialContent = null;
		initialProposals = null;
	}
	public void proposalPopupClosed(ContentProposalAdapter adapter) {
		reset();
	}
	public void proposalPopupOpened(ContentProposalAdapter adapter) {
	}
}

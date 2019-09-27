package bndtools.editor.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import aQute.bnd.help.Syntax;

public class BndCompletionProcessor implements IContentAssistProcessor {

	private static final Pattern PREFIX_PATTERN = Pattern.compile("^(?:.*\\s)*(.*)$");

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		try {
			String pre = viewer.getDocument()
				.get(0, offset);
			Matcher matcher = PREFIX_PATTERN.matcher(pre);
			if (matcher.matches()) {
				String prefix = matcher.group(1);
				ICompletionProposal[] found = proposals(prefix, offset);
				if (found.length == 1) {
					found[0].apply(viewer.getDocument());
					viewer.setSelectedRange(offset + (found[0].getDisplayString()
						.length() - prefix.length() + 2), 0);
					return new ICompletionProposal[0];
				}
				return found;
			}
			return proposals(null, offset);
		} catch (BadLocationException e) {
			return proposals(null, offset);
		}
	}

	private static ICompletionProposal[] proposals(String prefix, int offset) {
		ArrayList<ICompletionProposal> results = new ArrayList<>(Syntax.HELP.size());
		for (Syntax s : Syntax.HELP.values()) {
			if (prefix == null || s.getHeader()
				.startsWith(prefix)) {
				IContextInformation info = new ContextInformation(s.getHeader(), s.getHeader());
				String text = prefix == null ? s.getHeader()
					: s.getHeader()
						.substring(prefix.length());
				results.add(new CompletionProposal(text + ": ", offset, 0, text.length() + 2, null, s.getHeader(), info, //$NON-NLS-1$
					s.getLead()));
			}
		}
		Collections.sort(results, (p1, p2) -> p1.getDisplayString()
			.compareTo(p2.getDisplayString()));
		return results.toArray(new ICompletionProposal[0]);
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] {
			'-'
		};
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return new char[] {
			'-'
		};
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

}

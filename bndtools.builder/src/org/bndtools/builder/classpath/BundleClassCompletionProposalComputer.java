package org.bndtools.builder.classpath;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

public class BundleClassCompletionProposalComputer implements IJavaCompletionProposalComputer {

	@Override
	public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context,
		IProgressMonitor monitor) {
		List<ICompletionProposal> result = new LinkedList<>();

		if (!(context instanceof JavaContentAssistInvocationContext)) {
			return Collections.emptyList();
		}

		try {
			int offset = context.getInvocationOffset();
			CharSequence prefix = context.computeIdentifierPrefix();

			result.add(new CompletionProposal("foobar", offset - prefix.length(), prefix.length(),
				offset - prefix.length() + 6));
			result.add(new CompletionProposal("fizzbuzz", offset - prefix.length(), prefix.length(),
				offset - prefix.length() + 8));
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}

	@Override
	public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext context,
		IProgressMonitor monitor) {
		List<IContextInformation> result = new LinkedList<>();

		result.add(new ContextInformation("contextDisplayString", "informationDisplayString"));

		return result;
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public void sessionStarted() {}

	@Override
	public void sessionEnded() {}
}

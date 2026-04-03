package org.bndtools.refactor.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;

/**
 * Base class for IQuickFixProcessor's
 */
public abstract class BaseRefactorer implements IQuickFixProcessor {

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		return true;
	}

	@Override
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations)
		throws CoreException {
		try {
			ICompilationUnit compilationUnit = context.getCompilationUnit();
			RefactorAssistant assistant = new RefactorAssistant(context.getASTRoot(), context.getCompilationUnit());
			Cursor<?> cursor = assistant.cursor(context.getCoveringNode());
			ProposalBuilder proposals = new ProposalBuilder(assistant);

			addCompletions(proposals, assistant, cursor, context);
			return proposals.proposals();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new CoreException(Status.error("failed to to make corrections", e));
		}
	}

	public abstract void addCompletions(ProposalBuilder builder, RefactorAssistant assistant, Cursor<?> target,
		IInvocationContext context);

}

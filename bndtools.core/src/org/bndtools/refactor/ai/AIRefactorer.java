package org.bndtools.refactor.ai;

import org.bndtools.refactor.ai.api.OpenAI;
import org.bndtools.refactor.util.BaseRefactorer;
import org.bndtools.refactor.util.Cursor;
import org.bndtools.refactor.util.ProposalBuilder;
import org.bndtools.refactor.util.RefactorAssistant;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.ReplaceEdit;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class AIRefactorer extends BaseRefactorer implements IQuickFixProcessor {

	final OpenAI openai;

	@Activate
	public AIRefactorer(@Reference
	OpenAI openai) {
		this.openai = openai;
	}

	@Override
	public void addCompletions(ProposalBuilder builder, RefactorAssistant assistant, Cursor<?> target,
		IInvocationContext context) {

		try {
			int start = context.getSelectionOffset();
			int end = start + context.getSelectionLength();
			if (start == end) {
				ASTNode covered = context.getCoveredNode();
				if (covered == null) {
					covered = context.getCoveringNode();
				}
				while (covered instanceof Name) {
					covered = covered.getParent();
				}
				if (covered == null)
					return;

				start = covered.getStartPosition();
				end = start + covered.getLength();
			}
			String completeSource = assistant.getSource();
			String source = assistant.getSource()
				.substring(start, end);

			final int begin = start, l = end - start;
			builder.build("ai", "Open AI", "openai", -10, () -> {
				OpenAIDialog dialog = new OpenAIDialog(new Shell(), source, openai, s -> {
					Job job = Job.create("update text buffer", mon -> {
						ICompilationUnit compilationUnit = context.getCompilationUnit();
						if (compilationUnit.getSource()
							.equals(completeSource)) {
							ReplaceEdit re = new ReplaceEdit(begin, l, s);
							compilationUnit.applyTextEdit(re, null);
						}
					});
					job.schedule();
				});
				dialog.open();
			});
		} catch (Exception e) {
			e.printStackTrace();
			// ok no proposal
		}
	}

}

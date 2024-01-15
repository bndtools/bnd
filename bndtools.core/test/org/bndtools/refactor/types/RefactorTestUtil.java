package org.bndtools.refactor.types;

import static org.assertj.core.api.Assertions.assertThat;

import org.bndtools.refactor.util.BaseRefactorer;
import org.bndtools.refactor.util.Cursor;
import org.bndtools.refactor.util.ProposalBuilder;
import org.bndtools.refactor.util.ProposalBuilder.Proposal;
import org.bndtools.refactor.util.RefactorAssistant;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

public class RefactorTestUtil<T extends BaseRefactorer> {

	final BaseRefactorer fp;

	public RefactorTestUtil(BaseRefactorer fp) {
		this.fp = fp;
	}

	public record Scenario(String source, String target, String selector, String proposal) {}

	public void testRefactoring(Scenario s) throws Exception {
		RefactorAssistant assistant = new RefactorAssistant(s.source);
		assertThat(s.source).isEqualTo(assistant.getCompilationUnit()
			.toString());

		ProposalBuilder proposalBuilder = new ProposalBuilder(assistant, false);
		Cursor<?> cursor = assistant.getCursor(s.selector);

		assertThat(cursor.getNode()).isPresent();

		fp.addCompletions(proposalBuilder, assistant, cursor, null);
		if (s.proposal == null) {
			assertThat(proposalBuilder.proposals()).isEmpty();
		} else {
			Proposal p = proposalBuilder.getProposal(s.proposal)
				.orElseThrow((() -> new IllegalArgumentException(
					"no such proposal " + s.proposal + " found " + proposalBuilder.getProposals()
						.keySet())));

			p.complete()
				.accept();
			assistant.fixup();
			IDocument d = new Document(s.source);
			assistant.apply(d, null);
			RefactorAssistant rr = new RefactorAssistant(d.get());
			assertThat(rr.getCompilationUnit()
				.toString()).isEqualTo(s.target);
		}
	}

}

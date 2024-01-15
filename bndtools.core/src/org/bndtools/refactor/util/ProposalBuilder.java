package org.bndtools.refactor.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * A Proposal Builder is a set of proposal and convenience methods to make the
 * proposals.
 */
public class ProposalBuilder {

	final Map<String, Proposal>	proposals	= new HashMap<>();
	final RefactorAssistant		assistant;
	final boolean				images;

	String						id;
	Image						image;
	int							relevance;
	boolean						autoInsertable;
	StyledString				displayString;
	Complete					complete;
	String						additionalInfo;

	/**
	 * A proposal
	 */
	public record Proposal(RefactorAssistant assistant, String id, Image image, StyledString displayString,
		String additionalInfo, int relevance, Complete complete, boolean autoInsertable)
		implements IJavaCompletionProposal, ICompletionProposalExtension6, ICompletionProposalExtension4 {

		@Override
		public void apply(IDocument document) {
			try {
				complete.accept();
				assistant.fixup();
				assistant.apply(document, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public Point getSelection(IDocument document) {
			return null;
		}

		@Override
		public String getAdditionalProposalInfo() {
			return additionalInfo;
		}

		@Override
		public String getDisplayString() {
			return displayString.getString();
		}

		@Override
		public Image getImage() {
			return image;
		}

		@Override
		public IContextInformation getContextInformation() {
			return null;
		}

		@Override
		public boolean isAutoInsertable() {
			return autoInsertable;
		}

		@Override
		public StyledString getStyledDisplayString() {
			return displayString;
		}

		@Override
		public int getRelevance() {
			return relevance;
		}
	}

	/**
	 * Is the interface to implement to complete a proposal. An exception can be
	 * thrown for convenience.
	 */
	public interface Complete {
		void accept() throws Exception;
	}

	/**
	 * Create a proposal builder with an assistant.
	 *
	 * @param assistant the assistant
	 * @param images for testing, make it false and it won't attempt to do image
	 *            stuff in a test
	 */
	public ProposalBuilder(RefactorAssistant assistant, boolean images) {
		this.images = images;
		this.assistant = assistant;
	}

	/**
	 * Create a Proposal Builder
	 *
	 * @param assistant the assistant
	 */
	public ProposalBuilder(RefactorAssistant assistant) {
		this(assistant, true);
	}

	public ProposalBuilder set(String id, String displayString, String iconName, int relevant,
		Complete applyRefactoring) {
		this.id = id;
		return displayString(displayString).iconName(iconName)
			.relevance(relevant)
			.complete(applyRefactoring);
	}

	/**
	 * Create a proposal
	 *
	 * @param id the id of the proposal
	 * @param displayString a display string
	 * @param iconName the name of the icon, see {@link Icons}
	 * @param relevance the relevance of this proposal. higher values more
	 *            relevant
	 * @param applyRefactoring the callback
	 * @return this
	 */
	public ProposalBuilder build(String id, String displayString, String iconName, int relevance,
		Complete applyRefactoring) {
		return set(id, displayString, iconName, relevance, applyRefactoring).add();
	}

	/**
	 * When used with builder model, add the current build structure
	 *
	 * @return this
	 */
	public ProposalBuilder add() {
		assert displayString != null;
		Proposal proposal = new Proposal(assistant, id, image, displayString, additionalInfo, relevance, complete,
			autoInsertable);
		proposals.put(id, proposal);
		resetFields();

		return this;
	}

	void resetFields() {
		id = null;
		image = null;
		relevance = 0;
		autoInsertable = false;
		displayString = null;
		complete = null;
		additionalInfo = null;
	}

	/**
	 * Complete the callback
	 *
	 * @param applyRefactoring the callback
	 * @return this
	 */
	public ProposalBuilder complete(Complete applyRefactoring) {
		assert this.complete == null : "already set";
		this.complete = applyRefactoring;
		return this;
	}

	public ProposalBuilder relevance(int relevance) {
		assert this.relevance == 0 : "already set";
		this.relevance = relevance;
		return this;
	}

	public ProposalBuilder autoInsertable() {
		assert !this.autoInsertable : "already set";
		autoInsertable = true;
		return this;
	}

	private ProposalBuilder iconName(String iconName) {
		assert this.image == null : "already set";
		if (images)
			this.image = Icons.image(iconName, true);
		return this;
	}

	public ProposalBuilder displayString(String displayString) {
		assert this.displayString == null : "already set";
		this.displayString = new StyledString(displayString);
		return this;
	}

	public ProposalBuilder displayString(StyledString displayString) {
		if (displayString != null) {
			assert this.displayString == null : "already set";
			this.displayString = displayString;
		}
		return this;
	}

	public IJavaCompletionProposal[] proposals() {
		return proposals.values()
			.toArray(IJavaCompletionProposal[]::new);
	}

	public int size() {
		return proposals.size();
	}

	public Map<String, Proposal> getProposals() {
		return Collections.unmodifiableMap(proposals);
	}

	public Optional<Proposal> getProposal(String id) {
		return Optional.ofNullable(proposals.get(id));
	}

	public ProposalBuilder additionalInfo(String string) {
		additionalInfo = string;
		return this;
	}

	public RefactorAssistant getAssistant() {
		return assistant;
	}
}

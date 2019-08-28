package bndtools.utils;

import org.eclipse.jface.fieldassist.IContentProposal;

class ErrorContentProposal implements IContentProposal {

	private final String message;

	public ErrorContentProposal(String message) {
		this.message = message;
	}

	@Override
	public String getContent() {
		return "";
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public String getLabel() {
		return message;
	}

	@Override
	public int getCursorPosition() {
		return 0;
	}
}

package name.neilbartlett.eclipse.bndtools.utils;

import org.eclipse.jface.fieldassist.IContentProposal;

class ErrorContentProposal implements IContentProposal {

	private final String message;

	public ErrorContentProposal(String message) {
		this.message = message;
	}

	public String getContent() {
		return "";
	}

	public String getDescription() {
		return null;
	}

	public String getLabel() {
		return message;
	}

	public int getCursorPosition() {
		return 0;
	}
}
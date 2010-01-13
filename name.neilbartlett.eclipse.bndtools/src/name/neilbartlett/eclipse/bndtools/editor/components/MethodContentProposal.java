package name.neilbartlett.eclipse.bndtools.editor.components;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.fieldassist.IContentProposal;

public class MethodContentProposal implements IContentProposal {
	
	private final IMethod method;

	public MethodContentProposal(IMethod method) {
		this.method = method;
	}
	public String getContent() {
		return method.getElementName();
	}
	public int getCursorPosition() {
		return method.getElementName().length();
	}
	public String getDescription() {
		return null;
	}
	public String getLabel() {
		return method.getElementName() + "(): " + method.getDeclaringType().getElementName();
	}
	public IMethod getMethod() {
		return method;
	}
}

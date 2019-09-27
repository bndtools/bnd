package bndtools.utils;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

public class JavaTypeContentProposal extends JavaContentProposal {

	private final IType element;

	public JavaTypeContentProposal(IType element) throws JavaModelException {
		super(element.getPackageFragment()
			.getElementName(), element.getElementName(), element.isInterface());
		this.element = element;
	}

	public IType getType() {
		return element;
	}
}

package name.neilbartlett.eclipse.bndtools.editor.components;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;

public class MethodProposalLabelProvider extends StyledCellLabelProvider implements ILabelProvider {
	@Override
	public void update(ViewerCell cell) {
		StyledString styledString = getStyledString(cell.getElement());
		cell.setText(styledString.getString());
		cell.setStyleRanges(styledString.getStyleRanges());
	}
	public String getText(Object element) {
		return getStyledString(element).getString();
	}
	private StyledString getStyledString(Object element) {
		MethodContentProposal proposal = (MethodContentProposal) element;
		
		IMethod method = proposal.getMethod();
		String methodName = method.getElementName();
		IType type = method.getDeclaringType();
		String typeName = type.getElementName();
		
		StyledString styledString = new StyledString(methodName);
		styledString.append(": " + type.getElementName(), StyledString.QUALIFIER_STYLER);
		
		return styledString;
	}
	public Image getImage(Object element) {
		return null;
	}
}

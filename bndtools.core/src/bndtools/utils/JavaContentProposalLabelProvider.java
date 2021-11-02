package bndtools.utils;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class JavaContentProposalLabelProvider extends LabelProvider {

	private static final Image	classImg		= Icons.image("class");
	private static final Image	interfaceImg	= Icons.image("interface");

	@Override
	public Image getImage(Object element) {
		Image result = null;

		if (element instanceof JavaContentProposal) {
			result = classImg;
			if (((JavaContentProposal) element).isInterface()) {
				result = interfaceImg;
			} else {
				result = classImg;
			}
		}

		return result;
	}

	@Override
	public String getText(Object element) {
		IContentProposal proposal = (IContentProposal) element;

		return proposal.getLabel();
	}
}

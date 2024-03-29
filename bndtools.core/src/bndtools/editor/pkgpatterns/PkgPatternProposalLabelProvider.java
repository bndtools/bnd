package bndtools.editor.pkgpatterns;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class PkgPatternProposalLabelProvider extends LabelProvider {

	private final Image	singleImg	= Icons.image("package");
	private final Image	multiImg	= Icons.image("packages");

	@Override
	public String getText(Object element) {
		return ((PkgPatternProposal) element).getLabel();
	}

	@Override
	public Image getImage(Object element) {
		boolean wildcard = ((PkgPatternProposal) element).isWildcard();
		return wildcard ? multiImg : singleImg;
	}

}

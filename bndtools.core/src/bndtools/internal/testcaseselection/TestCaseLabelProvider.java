package bndtools.internal.testcaseselection;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class TestCaseLabelProvider extends LabelProvider {

	private final static Image junitImg = Icons.image("icons/test.gif");

	@Override
	public String getText(Object element) {
		String result;
		if (element instanceof String)
			result = (String) element;
		else if (element instanceof IPath)
			result = ((IPath) element).toString()
				.replace('/', '.');
		else
			result = "<error>"; //$NON-NLS-1$
		return result;
	}

	@Override
	public Image getImage(Object element) {
		return junitImg;
	}

}

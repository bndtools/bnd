package bndtools.editor.common;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class PrivatePackageTableLabelProvider extends LabelProvider implements ITableLabelProvider {

	private final Image packageImg = Icons.desc("package")
		.createImage();

	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		Image image = null;
		if (columnIndex == 0)
			image = packageImg;
		return image;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		String text = null;
		if (columnIndex == 0) {
			text = (String) element;
		}
		return text;
	}

	@Override
	public void dispose() {
		super.dispose();
		packageImg.dispose();
	}
}

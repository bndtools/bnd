package bndtools.utils;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;

public class ClassPathLabelProvider extends StyledCellLabelProvider {

	private final static Image	jarImg		= Icons.image("jar");
	private final static Image	folderImg	= Icons.image("/icons/fldr_obj.gif");

	@Override
	public void update(ViewerCell cell) {
		IPath path = (IPath) cell.getElement();

		cell.setText(path.toString());
		if (path.hasTrailingSeparator())
			cell.setImage(folderImg);
		else
			cell.setImage(jarImg);
	}
}

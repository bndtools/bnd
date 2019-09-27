package bndtools.utils;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class ClassPathLabelProvider extends StyledCellLabelProvider {

	private final Image	jarImg		= Icons.desc("jar")
		.createImage();
	private final Image	folderImg	= AbstractUIPlugin
		.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/fldr_obj.gif")
		.createImage();

	@Override
	public void update(ViewerCell cell) {
		IPath path = (IPath) cell.getElement();

		cell.setText(path.toString());
		if (path.hasTrailingSeparator())
			cell.setImage(folderImg);
		else
			cell.setImage(jarImg);
	}

	@Override
	public void dispose() {
		super.dispose();
		jarImg.dispose();
		folderImg.dispose();
	}
}

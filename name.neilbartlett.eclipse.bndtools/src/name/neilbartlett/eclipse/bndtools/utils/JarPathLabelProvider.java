package name.neilbartlett.eclipse.bndtools.utils;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class JarPathLabelProvider extends StyledCellLabelProvider {
	
	private final Image jarImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/jar_obj.gif").createImage();
	
	@Override
	public void update(ViewerCell cell) {
		IPath path = (IPath) cell.getElement();
		
		cell.setText(path.toString());
		cell.setImage(jarImg);
	}
	
	@Override
	public void dispose() {
		super.dispose();
		jarImg.dispose();
	}
}

package name.neilbartlett.eclipse.bndtools.editor.components;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ProvideLabelProvider extends LabelProvider {
	
	private Image interfaceImage = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/interface_obj.gif").createImage();
	
	@Override
	public Image getImage(Object element) {
		return interfaceImage;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		interfaceImage.dispose();
	}
}

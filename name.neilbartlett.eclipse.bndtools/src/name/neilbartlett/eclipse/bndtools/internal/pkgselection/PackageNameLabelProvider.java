package name.neilbartlett.eclipse.bndtools.internal.pkgselection;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class PackageNameLabelProvider extends LabelProvider {
	
	private Image packageImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/package_obj.gif").createImage();
	
	@Override
	public String getText(Object element) {
		return (String) element;
	}
	
	@Override
	public Image getImage(Object element) {
		return packageImg;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		packageImg.dispose();
	}
}

package name.neilbartlett.eclipse.bndtools.editor.components;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.editor.model.ServiceComponent;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ServiceComponentLabelProvider extends LabelProvider {
	
	private Image classImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/class_obj.gif").createImage();
	private Image pkgImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_obj.gif").createImage();
	private Image xmlImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/xml_file_obj.gif").createImage();
	
	@Override
	public String getText(Object element) {
		String pattern = ((ServiceComponent) element).getName();
		return pattern;
	}
	
	@Override
	public Image getImage(Object element) {
		Image result = null;
		
		ServiceComponent component = (ServiceComponent) element;
		String pattern = component.getName();
        if(component.isPath()) {
        	result = xmlImg;
        } else if(pattern.endsWith(".*")) {
        	result = pkgImg;
        } else {
        	result = classImg;
        }
        
        return result;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		classImg.dispose();
		pkgImg.dispose();
		xmlImg.dispose();
	}
}

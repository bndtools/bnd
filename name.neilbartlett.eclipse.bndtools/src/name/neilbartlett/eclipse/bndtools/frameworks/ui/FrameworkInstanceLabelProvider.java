/**
 * 
 */
package name.neilbartlett.eclipse.bndtools.frameworks.ui;

import java.util.ArrayList;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class FrameworkInstanceLabelProvider extends LabelProvider implements ITableLabelProvider {
	
	private final Device device;
	private final List<Image> images = new ArrayList<Image>();
	
	private final ImageDescriptor errorOverlay = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/error_overlay.gif");
	private final ImageDescriptor warningOverlay = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/warning_overlay.gif");

	public FrameworkInstanceLabelProvider(Device device) {
		this.device = device;
	}
	
	public Image getColumnImage(Object element, int columnIndex) {
		Image icon = null;
		
		if(columnIndex == 0) {
			IFrameworkInstance instance = (IFrameworkInstance) element;
			icon = instance.createIcon(device);
			images.add(icon);
			
			IStatus instanceStatus = instance.getStatus();
			if(instanceStatus.getSeverity() == IStatus.ERROR) {
				DecorationOverlayIcon overlayIcon = new DecorationOverlayIcon(icon, errorOverlay, IDecoration.BOTTOM_RIGHT);
				icon = overlayIcon.createImage(device);
				images.add(icon);
			} else if(instanceStatus.getSeverity() == IStatus.WARNING) {
				DecorationOverlayIcon overlayIcon = new DecorationOverlayIcon(icon, warningOverlay, IDecoration.BOTTOM_RIGHT);
				icon = overlayIcon.createImage(device);
				images.add(icon);
			}
			
		}
		
		return icon;
	}
	public String getColumnText(Object element, int columnIndex) {
		IFrameworkInstance instance = (IFrameworkInstance) element;
		String text;
		
		switch(columnIndex) {
		case 0:
			text = instance.getDisplayString();
			break;
		case 1:
			text = instance.getInstancePath().toString();
			break;
		default:
			text = "<<ERROR>>";
		}
		
		return text;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		for (Image image : images) {
			image.dispose();
		}
	}
}
/**
 * 
 */
package name.neilbartlett.eclipse.bndtools.classpath;

import java.util.ArrayList;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.frameworks.IFrameworkInstance;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;

public class FrameworkInstanceLabelProvider extends LabelProvider implements ITableLabelProvider {
	
	private final Device device;
	private final List<Image> images = new ArrayList<Image>();

	public FrameworkInstanceLabelProvider(Device device) {
		this.device = device;
	}
	
	public Image getColumnImage(Object element, int columnIndex) {
		Image icon = null;
		
		if(columnIndex == 0) {
			IFrameworkInstance instance = (IFrameworkInstance) element;
			icon = instance.createIcon(device);
			images.add(icon);
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
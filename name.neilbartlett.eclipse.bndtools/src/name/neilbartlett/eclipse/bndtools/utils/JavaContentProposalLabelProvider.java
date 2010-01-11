package name.neilbartlett.eclipse.bndtools.utils;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class JavaContentProposalLabelProvider extends LabelProvider {
	
	private Image classImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/class_obj.gif").createImage();
	private Image interfaceImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/interface_obj.gif").createImage();
	
	@Override
	public Image getImage(Object element) {
		Image result = null;
		
		if(element instanceof JavaContentProposal) {
			result = classImg;
			if(((JavaContentProposal) element).isInterface()) {
				result = interfaceImg;
			} else {
				result = classImg;
			}
		}
		
		return result;
	}
	
	@Override
	public String getText(Object element) {
		IContentProposal proposal = (IContentProposal) element;
		
		return proposal.getLabel();
	}
	
	@Override
	public void dispose() {
		super.dispose();
		classImg.dispose();
		interfaceImg.dispose();
	}
}
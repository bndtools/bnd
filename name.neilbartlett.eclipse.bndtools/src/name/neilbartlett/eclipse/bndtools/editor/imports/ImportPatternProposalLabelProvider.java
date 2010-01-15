package name.neilbartlett.eclipse.bndtools.editor.imports;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ImportPatternProposalLabelProvider extends LabelProvider {

	private Image singleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_obj.gif").createImage();
	private Image multiImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/packages.gif").createImage();
	
	@Override
	public String getText(Object element) {
		return ((ImportPatternProposal) element).getLabel();
	}
	@Override
	public Image getImage(Object element) {
		boolean wildcard = ((ImportPatternProposal) element).isWildcard();
		return wildcard ? multiImg : singleImg;
	}
	@Override
	public void dispose() {
		super.dispose();
		singleImg.dispose();
		multiImg.dispose();
	}
}

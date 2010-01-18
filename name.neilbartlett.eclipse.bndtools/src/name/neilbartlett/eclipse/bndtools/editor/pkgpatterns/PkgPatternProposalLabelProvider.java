package name.neilbartlett.eclipse.bndtools.editor.pkgpatterns;

import name.neilbartlett.eclipse.bndtools.Plugin;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class PkgPatternProposalLabelProvider extends LabelProvider {

	private Image singleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_obj.gif").createImage();
	private Image multiImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/packages.gif").createImage();
	
	@Override
	public String getText(Object element) {
		return ((PkgPatternProposal) element).getLabel();
	}
	@Override
	public Image getImage(Object element) {
		boolean wildcard = ((PkgPatternProposal) element).isWildcard();
		return wildcard ? multiImg : singleImg;
	}
	@Override
	public void dispose() {
		super.dispose();
		singleImg.dispose();
		multiImg.dispose();
	}
}

package name.neilbartlett.eclipse.bndtools.editor.imports;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.editor.model.ImportPattern;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ImportPatternLabelProvider extends StyledCellLabelProvider {
	
	private final Image packageImg;
	private final Image packageOptImg;
	
	public ImportPatternLabelProvider() {
		ImageDescriptor packageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_obj.gif");
		packageImg = packageDescriptor.createImage();
		
		ImageDescriptor questionOverlay = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/question_overlay.gif");
		packageOptImg = new DecorationOverlayIcon(packageImg, questionOverlay, IDecoration.TOP_LEFT).createImage();
	}
	@Override
	public void update(ViewerCell cell) {
		ImportPattern pattern = (ImportPattern) cell.getElement();
		
		cell.setImage(pattern.isOptional() ? packageOptImg : packageImg);
		
		StyledString styledString = new StyledString(pattern.getName());
		String versionRange = pattern.getVersionRange();
		if(versionRange != null) {
			styledString.append(" - " + versionRange, StyledString.QUALIFIER_STYLER);
		}
		cell.setText(styledString.getString());
		cell.setStyleRanges(styledString.getStyleRanges());
	}
	@Override
	public void dispose() {
		super.dispose();
		packageImg.dispose();
		packageOptImg.dispose();
	}
}

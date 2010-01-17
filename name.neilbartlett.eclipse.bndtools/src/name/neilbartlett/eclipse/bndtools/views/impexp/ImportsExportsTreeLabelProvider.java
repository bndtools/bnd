package name.neilbartlett.eclipse.bndtools.views.impexp;

import static name.neilbartlett.eclipse.bndtools.views.impexp.ImportsExportsTreeContentProvider.EXPORTS_PLACEHOLDER;
import static name.neilbartlett.eclipse.bndtools.views.impexp.ImportsExportsTreeContentProvider.IMPORTS_PLACEHOLDER;

import java.util.Map;
import java.util.Map.Entry;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.UIConstants;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.lib.osgi.Constants;

public class ImportsExportsTreeLabelProvider extends StyledCellLabelProvider {
	
	private final Image pkgFolderImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/packagefolder_obj.gif").createImage();
	
	private final Image packageImg;
	private final Image packageOptImg;
	
	public ImportsExportsTreeLabelProvider() {
		ImageDescriptor packageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_obj.gif");
		packageImg = packageDescriptor.createImage();
		
		ImageDescriptor questionOverlay = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/question_overlay.gif");
		packageOptImg = new DecorationOverlayIcon(packageImg, questionOverlay, IDecoration.TOP_LEFT).createImage();
	}
	
	public void dispose() {
		super.dispose();
		pkgFolderImg.dispose();
		packageImg.dispose();
		packageOptImg.dispose();
	};
	
	@Override
	public void update(ViewerCell cell) {
		if(cell.getElement() == IMPORTS_PLACEHOLDER) {
			if(cell.getColumnIndex() == 0) {
				cell.setImage(pkgFolderImg);
				cell.setText("Import Packages");
			}
		} else if(cell.getElement() == EXPORTS_PLACEHOLDER) {
			if(cell.getColumnIndex() == 0) {
				cell.setImage(pkgFolderImg);
				cell.setText("Export Packages");
			}
		} else {
			@SuppressWarnings("unchecked")
			Entry<String, Map<String,String>> entry = (Entry<String, Map<String, String>>) cell.getElement();
			switch(cell.getColumnIndex()) {
			case 0:
				StyledString styledString = new StyledString(entry.getKey());
				String resolution = entry.getValue().get(Constants.RESOLUTION_DIRECTIVE);
				if(resolution != null)
					styledString.append(" <" + resolution + ">", UIConstants.ITALIC_QUALIFIER_STYLER);
				cell.setText(styledString.getString());
				cell.setStyleRanges(styledString.getStyleRanges());
				cell.setImage(packageImg);
				break;
			case 1:
				cell.setText(entry.getValue().get(Constants.VERSION_ATTRIBUTE));
				break;
			case 2:
				// Show the attributes excluding "resolution:" and "version"
				Map<String, String> attribs = entry.getValue();
				StringBuilder builder = new StringBuilder();
				boolean first = true;
				for (Entry<String,String> attribEntry : attribs.entrySet()) {
					if(!first) builder.append(';');
					if(!Constants.VERSION_ATTRIBUTE.equals(attribEntry.getKey())
							&& !Constants.RESOLUTION_DIRECTIVE.equals(attribEntry.getKey())) {
						builder.append(attribEntry.getKey()).append('=').append(attribEntry.getValue());
					}
				}
				cell.setText(builder.toString());
				break;
			}
		}
	}
}

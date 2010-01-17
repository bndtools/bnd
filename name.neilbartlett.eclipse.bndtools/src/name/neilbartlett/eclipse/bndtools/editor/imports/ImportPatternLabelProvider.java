package name.neilbartlett.eclipse.bndtools.editor.imports;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.UIConstants;
import name.neilbartlett.eclipse.bndtools.editor.model.ImportPattern;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ImportPatternLabelProvider extends StyledCellLabelProvider {
	
	private final Image packageImg;
	
	public ImportPatternLabelProvider() {
		packageImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_obj.gif").createImage();
	}
	
	@Override
	public void update(ViewerCell cell) {
		ImportPattern pattern = (ImportPattern) cell.getElement();
		
		cell.setImage(packageImg);
		
		StyledString styledString = new StyledString(pattern.getName());
		if(pattern.isOptional()) {
			styledString.append(" <optional>", UIConstants.ITALIC_QUALIFIER_STYLER);
		}
		String versionRange = pattern.getVersionRange();
		if(versionRange != null) {
			styledString.append(": " + versionRange, StyledString.COUNTER_STYLER);
		}
		cell.setText(styledString.getString());
		cell.setStyleRanges(styledString.getStyleRanges());
	}
	@Override
	public void dispose() {
		super.dispose();
		packageImg.dispose();
	}
}

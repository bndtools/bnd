package name.neilbartlett.eclipse.bndtools.editor.project;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.editor.model.VersionedClause;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class VersionedClauseLabelProvider extends StyledCellLabelProvider {
	
	Image bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();
	
	@Override
	public void update(ViewerCell cell) {
		VersionedClause clause = (VersionedClause) cell.getElement();
		
		int col = cell.getColumnIndex();
		if(col == 0) {
			cell.setText(clause.getName());
			cell.setImage(bundleImg);
		} else if(col == 1) {
			String version = clause.getVersionRange();
			if(version != null) {
				StyledString string = new StyledString(version, StyledString.COUNTER_STYLER);
				cell.setText(string.getString());
				cell.setStyleRanges(string.getStyleRanges());
			}
		}
	}
	@Override
	public void dispose() {
		super.dispose();
		bundleImg.dispose();
	};
}

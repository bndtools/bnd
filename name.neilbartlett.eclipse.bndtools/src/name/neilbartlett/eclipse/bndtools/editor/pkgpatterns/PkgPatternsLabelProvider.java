package name.neilbartlett.eclipse.bndtools.editor.pkgpatterns;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.UIConstants;
import name.neilbartlett.eclipse.bndtools.editor.model.HeaderClause;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.lib.osgi.Constants;

public class PkgPatternsLabelProvider extends StyledCellLabelProvider {
	
	private final Image packageImg;
	
	public PkgPatternsLabelProvider() {
		packageImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_obj.gif").createImage();
	}
	@Override
	public void update(ViewerCell cell) {
		HeaderClause clause = (HeaderClause) cell.getElement();
		cell.setImage(packageImg);
		
		StyledString styledString = new StyledString(clause.getName());
		String resolution = clause.getAttribs().get(Constants.RESOLUTION_DIRECTIVE);
		if(org.osgi.framework.Constants.RESOLUTION_OPTIONAL.equals(resolution)) {
			styledString.append(" <optional>", UIConstants.ITALIC_QUALIFIER_STYLER);
		}
		String version = clause.getAttribs().get(org.osgi.framework.Constants.VERSION_ATTRIBUTE);
		if(version != null) {
			styledString.append(": " + version, StyledString.COUNTER_STYLER);
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

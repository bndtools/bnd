package bndtools.model.clauses;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class VersionedClauseLabelProvider extends StyledCellLabelProvider {

    Image bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();

    @Override
    public void update(ViewerCell cell) {
        VersionedClause clause = (VersionedClause) cell.getElement();
        StyledString label = new StyledString(clause.getName());
        String version = clause.getVersionRange();
        if (version != null) {
            label.append(" " + version, StyledString.COUNTER_STYLER);
        }
        cell.setText(label.getString());
        cell.setStyleRanges(label.getStyleRanges());
        cell.setImage(bundleImg);
    }

    @Override
    public void dispose() {
        super.dispose();
        bundleImg.dispose();
    };
}

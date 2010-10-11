package bndtools.wizards.workspace;

import org.apache.felix.bundlerepository.Resource;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

class ResourceLabelProvider extends StyledCellLabelProvider {

    private Image bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();

    @Override
    public void update(ViewerCell cell) {
        Resource resource = (Resource) cell.getElement();

        StyledString string = new StyledString(resource.getSymbolicName());
        string.append(" (" + resource.getVersion() + ")", StyledString.COUNTER_STYLER);
        string.append(" " + resource.getURI(), StyledString.DECORATIONS_STYLER);

        cell.setText(string.getString());
        cell.setStyleRanges(string.getStyleRanges());
        cell.setImage(bundleImg);
    }

    @Override
    public void dispose() {
        super.dispose();
        bundleImg.dispose();
    }
}
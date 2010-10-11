package bndtools.shared;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class URLLabelProvider extends StyledCellLabelProvider {

    private final Image linkImg;

    public URLLabelProvider(Device device) {
        linkImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/link.png").createImage(device);
    }

    @Override
    public void update(ViewerCell cell) {
        cell.setImage(linkImg);

        Object element = cell.getElement();
        if (element instanceof OBRLink) {
            StyledString label = ((OBRLink) element).getLabel();
            cell.setStyleRanges(label.getStyleRanges());
            cell.setText(label.getString());
        } else {
            cell.setText(element == null ? "null" : element.toString());
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        linkImg.dispose();
    }
}
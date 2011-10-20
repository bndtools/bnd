package bndtools.wizards.workspace;

import org.apache.felix.bundlerepository.Resource;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Version;

import bndtools.Plugin;

public class ResourceLabelProvider extends StyledCellLabelProvider {

    private Image bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();

    @Override
    public void update(ViewerCell cell) {
        Resource resource = (Resource) cell.getElement();

        String name = resource.getSymbolicName();
        if (name == null)
            name = resource.toString();
        if (name == null)
            name = "<unknown>";

        StyledString string = new StyledString(name);

        Version version = resource.getVersion();
        if (version != null)
            string.append(" (" + resource.getVersion() + ")", StyledString.COUNTER_STYLER);

//        String uri = resource.getURI();
//        if (uri != null)
//            string.append(" " + uri, StyledString.DECORATIONS_STYLER);

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
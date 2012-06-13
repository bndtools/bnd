package bndtools.wizards.workspace;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Resource;
import org.bndtools.core.utils.filters.ObrConstants;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Version;

import bndtools.Plugin;

public class ReasonLabelProvider extends StyledCellLabelProvider {

    private Image bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();
    private Image packageImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_imp.gif").createImage();
    private Image eeImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/link.png").createImage();
    private Image serviceImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/service.png").createImage();

    @Override
    public void update(ViewerCell cell) {
        Reason reason = (Reason) cell.getElement();

        Resource resource = reason.getResource();
        String name = resource.getSymbolicName();
        if (name == null)
            name = resource.toString();
        if (name == null)
            name = "<unknown>";

        StyledString string = new StyledString(name);

        Version version = resource.getVersion();
        if (version != null)
            string.append(" " + resource.getVersion(), StyledString.COUNTER_STYLER);

        string.append(" ");
        string.append(reason.getRequirement().getFilter());

        cell.setText(string.getString());
        cell.setStyleRanges(string.getStyleRanges());

        setImage(cell, reason.getRequirement().getName());
    }

    private void setImage(ViewerCell cell, String requirementName) {

        if (ObrConstants.REQUIREMENT_BUNDLE.equals(requirementName)) {
            cell.setImage(bundleImg);
        } else if (ObrConstants.REQUIREMENT_PACKAGE.equals(requirementName)) {
            cell.setImage(packageImg);
        } else if (ObrConstants.REQUIREMENT_EE.equals(requirementName)) {
            cell.setImage(eeImg);
        } else {
            cell.setImage(serviceImg);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        bundleImg.dispose();
        packageImg.dispose();
        eeImg.dispose();
        serviceImg.dispose();
    }
}
package bndtools.wizards.workspace;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Requirement;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class UnresolvedReasonLabelProvider extends StyledCellLabelProvider {

    private Image pkgImg;
    private Image bundleImg;
    private Image errorImg;
    private Image serviceImg;

    @Override
    public void update(ViewerCell cell) {
        Reason reason = (Reason) cell.getElement();

        Requirement requirement = reason.getRequirement();
        String requireType = requirement.getName();

        String label = requirement.getFilter();
        if ("package".equals(requireType)) {
            if (pkgImg == null) pkgImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_obj.gif").createImage();
            cell.setImage(pkgImg);
        } else if ("bundle".equals(requireType)) {
            if (bundleImg == null) bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();
            cell.setImage(bundleImg);
        } else if ("service".equals(requireType)) {
            if (serviceImg == null) serviceImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/service.png").createImage();
            cell.setImage(serviceImg);
        } else if ("mode".equals(requireType)) {
            if (errorImg == null) errorImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/cross.png").createImage();
            cell.setImage(errorImg);

            try {
                String mode = extract("mode", requirement.getFilter());
                label = String.format("Illegal resolver mode \"%s\"", mode, reason.getResource());
            } catch (IllegalArgumentException e) {
                label = "Illegal resolver mode";
            }
        } else {
            label = requirement.toString();
        }

        StyledString styledLabel = new StyledString(label);
        styledLabel.append(" FROM: " + reason.getResource(), StyledString.COUNTER_STYLER);
        cell.setText(styledLabel.toString());
        cell.setStyleRanges(styledLabel.getStyleRanges());
    }

    String extract(String name, String filter) throws IllegalArgumentException {
        Pattern regex = Pattern.compile("\\(" + name + "=([^\\)]*)\\)");
        Matcher matcher = regex.matcher(filter);

        if (!matcher.matches())
            throw new IllegalArgumentException();

        return matcher.group(1);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (pkgImg != null) pkgImg.dispose();
        if (bundleImg != null) bundleImg.dispose();
        if (errorImg != null) errorImg.dispose();
    }
}

package bndtools.model.obr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.bundlerepository.Requirement;
import org.bndtools.core.utils.filters.ObrConstants;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.UIConstants;

public class RequirementLabelProvider extends StyledCellLabelProvider {

    private Image pkgImg;
    private Image bundleImg;
    private Image errorImg;
    private Image serviceImg;

    private Matcher findValueRegion(String name, String filter) throws IllegalArgumentException {
        String pattern = "\\(" + name + "=([^\\)]*)\\)";
        Pattern regex = Pattern.compile(pattern);
        return regex.matcher(filter);
    }


    public StyledString getLabel(Requirement requirement) {
        String filter = requirement.getFilter();
        StyledString label = new StyledString(filter, StyledString.QUALIFIER_STYLER);

        String propertyName;
        if (ObrConstants.REQUIREMENT_BUNDLE.equals(requirement.getName()))
            propertyName = ObrConstants.FILTER_BSN;
        else if (ObrConstants.REQUIREMENT_PACKAGE.equals(requirement.getName()))
            propertyName = ObrConstants.FILTER_PACKAGE;
        else if (ObrConstants.REQUIREMENT_SERVICE.equals(requirement.getName()))
            propertyName = ObrConstants.FILTER_SERVICE;
        else
            propertyName = null;

        if (propertyName != null) {
            Matcher matcher = findValueRegion(propertyName, filter);
            while (matcher.find()) {
                int begin = matcher.start(1);
                int end = matcher.end(1);
                label.setStyle(begin, end - begin, UIConstants.BOLD_STYLER);
            }
        }
        return label;
    }

    public Image getIcon(Requirement requirement) {
        String requireType = requirement.getName();
        if ("package".equals(requireType)) {
            if (pkgImg == null) pkgImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_obj.gif").createImage();
            return pkgImg;
        } else if ("bundle".equals(requireType)) {
            if (bundleImg == null) bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();
            return bundleImg;
        } else if ("service".equals(requireType)) {
            if (serviceImg == null) serviceImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/service-tiny.png").createImage();
            return serviceImg;
        }
        return null;
    }

    @Override
    public void update(ViewerCell cell) {
        Requirement requirement = (Requirement) cell.getElement();

        cell.setImage(getIcon(requirement));

        StyledString label = getLabel(requirement);
        cell.setText(label.getString());
        cell.setStyleRanges(label.getStyleRanges());
    }

    @Override
    public void dispose() {
        super.dispose();
        if (pkgImg != null) pkgImg.dispose();
        if (bundleImg != null) bundleImg.dispose();
        if (errorImg != null) errorImg.dispose();
    }

}


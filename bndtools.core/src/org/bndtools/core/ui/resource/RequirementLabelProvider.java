package org.bndtools.core.ui.resource;

import java.io.IOException;
import java.net.URLEncoder;

import org.bndtools.utils.jface.ImageCachingLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.osgi.resource.Requirement;

import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.FilterParser.Expression;
import bndtools.Plugin;

public class RequirementLabelProvider extends ImageCachingLabelProvider {

    public RequirementLabelProvider() {
        super(Plugin.PLUGIN_ID);
    }

    @Override
    public void update(ViewerCell cell) {
        Object element = cell.getElement();
        if (element instanceof Requirement) {
            Requirement requirement = (Requirement) element;

            StyledString label = getLabel(requirement);

            cell.setText(label.getString());
            cell.setStyleRanges(label.getStyleRanges());

            Image icon = getImage(R5LabelFormatter.getNamespaceImagePath(requirement.getNamespace()), true);
            if (icon != null)
                cell.setImage(icon);
        }
    }

    protected StyledString getLabel(Requirement requirement) {
        StyledString label = new StyledString();
        return getLabel(label, requirement);
    }

    protected StyledString getLabel(StyledString label, Requirement requirement) {
        R5LabelFormatter.appendRequirementLabel(label, requirement);
        return label;
    }

    /**
     * This method return a query string (on jpm) based on a filter. This is not exact, but should in general give a
     * list to work from
     *
     * @param req
     *            The requirement searched
     * @return a query string where p: is for package and bsn: is for bundle symbolic name.
     */
    public static String requirementToUrl(Requirement req) throws IOException {
        FilterParser fp = new FilterParser();
        Expression expression = fp.parse(req);

        String s = expression.query();

        return "http://jpm4j.org/#!/search?q=" + URLEncoder.encode(s, "UTF-8");
    }
}

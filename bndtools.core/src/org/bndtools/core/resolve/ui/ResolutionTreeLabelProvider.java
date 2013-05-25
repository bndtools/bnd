package org.bndtools.core.resolve.ui;

import java.util.Map.Entry;

import org.bndtools.core.ui.resource.AbstractR5LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class ResolutionTreeLabelProvider extends AbstractR5LabelProvider {

    @Override
    public void update(ViewerCell cell) {
        Object element = cell.getElement();

        StyledString label = new StyledString();
        Image icon = null;

        if (element instanceof ResolutionTreeItem) {
            ResolutionTreeItem item = (ResolutionTreeItem) element;
            appendCapability(label, item.getCapability());

            // Get the icon from the capability namespace
            icon = getIcon(item.getCapability().getNamespace());
        } else if (element instanceof Requirement) {
            Requirement requirement = (Requirement) element;
            if (Namespace.RESOLUTION_OPTIONAL.equals(requirement.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE)))
                label.append(" OPTIONALLY", StyledString.QUALIFIER_STYLER);
            label.append(" REQUIRED BY ", StyledString.QUALIFIER_STYLER);

            Resource resource = requirement.getResource();
            if (resource != null)
                appendResourceLabel(label, resource);
            else
                label.append(" INITIAL");

            label.append(" [", StyledString.QUALIFIER_STYLER);
            boolean first = true;
            for (Entry<String,String> entry : requirement.getDirectives().entrySet()) {
                String key = entry.getKey();
                if (!key.equals(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE)) {
                    if (!first)
                        label.append(",", StyledString.QUALIFIER_STYLER);
                    first = false;
                    label.append(key + ":=" + entry.getValue(), StyledString.QUALIFIER_STYLER);
                }
            }
            label.append("]", StyledString.QUALIFIER_STYLER);
        }

        cell.setText(label.getString());
        cell.setStyleRanges(label.getStyleRanges());
        cell.setImage(icon);
    }
}

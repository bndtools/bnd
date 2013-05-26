package org.bndtools.core.ui.resource;

import org.bndtools.core.utils.jface.ImageCachingLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.osgi.resource.Requirement;

public class RequirementLabelProvider extends ImageCachingLabelProvider {

    @Override
    public void update(ViewerCell cell) {
        Object element = cell.getElement();
        if (element instanceof Requirement) {
            Requirement requirement = (Requirement) element;

            StyledString label = getLabel(requirement);

            cell.setText(label.getString());
            cell.setStyleRanges(label.getStyleRanges());

            getImage(R5LabelFormatter.getNamespaceImagePath(requirement.getNamespace()), true);
        }
    }

    protected StyledString getLabel(Requirement requirement) {
        StyledString label = new StyledString();
        R5LabelFormatter.appendRequirementLabel(label, requirement);
        return label;
    }

}

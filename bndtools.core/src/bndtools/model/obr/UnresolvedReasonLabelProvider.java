package bndtools.model.obr;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Resource;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;

public class UnresolvedReasonLabelProvider extends RequirementLabelProvider {

    @Override
    public void update(ViewerCell cell) {
        Reason reason = (Reason) cell.getElement();

        cell.setImage(getIcon(reason.getRequirement()));

        String resourceName = getLabel(reason.getResource());

        StyledString label = getLabel(reason.getRequirement()).append(" FROM: ").append(resourceName, StyledString.COUNTER_STYLER);
        cell.setText(label.getString());
        cell.setStyleRanges(label.getStyleRanges());
    }

    public String getLabel(Resource resource) {
        String resourceName = (resource != null && resource.getId() != null) ? resource.getId() : "<<initial>>";
        return resourceName;
    }

}

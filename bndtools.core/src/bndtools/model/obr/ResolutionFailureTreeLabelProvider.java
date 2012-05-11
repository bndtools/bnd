package bndtools.model.obr;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Resource;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;

public class ResolutionFailureTreeLabelProvider extends RequirementLabelProvider {


    @Override
    public void update(ViewerCell cell) {
        Reason reason = (Reason) cell.getElement();

        cell.setImage(getIcon(reason.getRequirement()));

        StyledString label = new StyledString(getLabel(reason.getResource()), StyledString.COUNTER_STYLER);
        label.append(" ---> ");
        label.append(getLabel(reason.getRequirement()));
        cell.setText(label.getString());
        cell.setStyleRanges(label.getStyleRanges());
    }

    public String getLabel(Resource resource) {
        String resourceName = (resource != null && resource.getId() != null) ? resource.getId() : "INITIAL";
        return resourceName;
    }
}

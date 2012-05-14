package bndtools.model.obr;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Resource;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;

import bndtools.UIConstants;

public class ResolutionFailureFlatLabelProvider extends RequirementLabelProvider {

    private static final String LABEL_INITIAL = "INITIAL";

    @Override
    public void update(ViewerCell cell) {
        Reason reason = (Reason) cell.getElement();

        cell.setImage(getIcon(reason.getRequirement()));

        StyledString label = getLabel(reason.getResource());
        label.append(" requires ", StyledString.QUALIFIER_STYLER);
        label.append(getLabel(reason.getRequirement()));

        cell.setText(label.getString());
        cell.setStyleRanges(label.getStyleRanges());
    }

    private StyledString getLabel(Resource resource) {
        StyledString label;
        if (resource == null || resource.getId() == null) {
            label = new StyledString(LABEL_INITIAL, UIConstants.BOLD_STYLER);
        } else {
            label = new StyledString(resource.getSymbolicName(), UIConstants.BOLD_STYLER);
            if (resource.getVersion() != null)
                label.append(" " + resource.getVersion().toString(), StyledString.COUNTER_STYLER);
        }
        return label;
    }
    
}

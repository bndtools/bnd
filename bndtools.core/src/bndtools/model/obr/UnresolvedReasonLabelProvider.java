package bndtools.model.obr;

import org.apache.felix.bundlerepository.Reason;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;

public class UnresolvedReasonLabelProvider extends RequirementLabelProvider {

    @Override
    public void update(ViewerCell cell) {
        Reason reason = (Reason) cell.getElement();

        cell.setImage(getIcon(reason.getRequirement()));

        StyledString label = getLabel(reason.getRequirement()).append(" FROM: ").append(reason.getResource().toString(), StyledString.COUNTER_STYLER);
        cell.setText(label.getString());
        cell.setStyleRanges(label.getStyleRanges());
    }

}

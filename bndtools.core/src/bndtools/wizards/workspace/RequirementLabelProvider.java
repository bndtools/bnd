package bndtools.wizards.workspace;

import org.apache.felix.bundlerepository.Requirement;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

public class RequirementLabelProvider extends StyledCellLabelProvider {
    @Override
    public void update(ViewerCell cell) {
        Requirement requirement = (Requirement) cell.getElement();


        cell.setText(requirement.getFilter() + " [" + requirement.getName() + "]");
    }
}

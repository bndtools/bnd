package bndtools.model.resolution;

import org.bndtools.core.ui.icons.Icons;
import org.bndtools.core.ui.resource.R5LabelFormatter;
import org.bndtools.core.ui.resource.RequirementLabelProvider;
import org.bndtools.utils.jface.StrikeoutStyler;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;

import aQute.bnd.osgi.Clazz;

public class RequirementWrapperLabelProvider extends RequirementLabelProvider {

    private final Styler strikeout = new StrikeoutStyler(StyledString.QUALIFIER_STYLER);

    public RequirementWrapperLabelProvider(boolean shortenNamespaces) {
        super(shortenNamespaces);
    }

    @Override
    public void update(ViewerCell cell) {
        Object element = cell.getElement();
        if (element instanceof RequirementWrapper) {
            RequirementWrapper rw = (RequirementWrapper) element;

            Image icon = getImage(R5LabelFormatter.getNamespaceImagePath(rw.requirement.getNamespace()), shortenNamespaces);
            if (icon != null)
                cell.setImage(icon);

            StyledString label = getLabel(rw.requirement);
            if (rw.resolved)
                label.setStyle(0, label.length(), strikeout);

            cell.setText(label.getString());
            cell.setStyleRanges(label.getStyleRanges());
        } else if (element instanceof Clazz) {
            cell.setImage(getImage(Icons.path("class.gif"), false));

            String pkg;
            String className;

            String fqn = ((Clazz) element).getFQN();
            int dot = fqn.lastIndexOf('.');
            if (dot >= 0) {
                pkg = fqn.substring(0, dot);
                className = fqn.substring(dot + 1);
            } else {
                pkg = "<default package>";
                className = fqn;
            }

            StyledString label = new StyledString(className);
            label.append(" - " + pkg, StyledString.QUALIFIER_STYLER);

            cell.setText(label.getString());
            cell.setStyleRanges(label.getStyleRanges());
        }
    }

}

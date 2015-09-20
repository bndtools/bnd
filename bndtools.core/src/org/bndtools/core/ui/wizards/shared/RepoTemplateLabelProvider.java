package org.bndtools.core.ui.wizards.shared;

import org.bndtools.core.ui.icons.Icons;
import org.bndtools.templating.Category;
import org.bndtools.templating.Template;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import bndtools.UIConstants;

public class RepoTemplateLabelProvider extends StyledCellLabelProvider {

    private static final Image IMG_FOLDER = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

    private final Image imgTemplate = Icons.desc("template").createImage();

    @Override
    public void update(ViewerCell cell) {
        Object element = cell.getElement();

        if (element instanceof Category) {
            Category cat = (Category) element;
            String name = cat.getName();
            cell.setText(name != null ? name : "Uncategorised");
            cell.setImage(IMG_FOLDER);
        } else if (element instanceof Template) {
            Template template = (Template) element;

            StyledString label = new StyledString(template.getName(), UIConstants.BOLD_STYLER);

            cell.setText(label.toString());
            cell.setStyleRanges(label.getStyleRanges());
            cell.setImage(imgTemplate);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        imgTemplate.dispose();
    }
}

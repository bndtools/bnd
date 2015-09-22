package org.bndtools.core.ui.wizards.shared;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.bndtools.templating.Category;
import org.bndtools.templating.Template;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import bndtools.UIConstants;

public class RepoTemplateLabelProvider extends StyledCellLabelProvider {

    private static final Image IMG_FOLDER = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

    private final List<Image> loadedImages = new LinkedList<>();

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

            try {
                InputStream iconStream = template.getIconData();
                if (iconStream != null) {
                    Image image = new Image(getViewer().getControl().getDisplay(), new ImageData(iconStream));
                    loadedImages.add(image);
                    cell.setImage(image);
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        for (Image image : loadedImages) {
            if (!image.isDisposed())
                image.dispose();
        }
    }
}

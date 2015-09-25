package org.bndtools.core.ui.wizards.shared;

import java.util.Map;

import org.bndtools.templating.Category;
import org.bndtools.templating.Template;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Version;

import bndtools.UIConstants;

public class RepoTemplateLabelProvider extends StyledCellLabelProvider {

    private static final Image IMG_FOLDER = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

    private final Map<Template,Image> loadedImages;

    public RepoTemplateLabelProvider(Map<Template,Image> loadedImages) {
        this.loadedImages = loadedImages;
    }

    @Override
    public void update(ViewerCell cell) {
        Object element = cell.getElement();

        if (element instanceof Category) {
            Category cat = (Category) element;
            cell.setText(cat.getName());
            cell.setImage(IMG_FOLDER);
        } else if (element instanceof Template) {
            Template template = (Template) element;

            // Name
            StyledString label = new StyledString(template.getName(), UIConstants.BOLD_STYLER);
            label.append(" ");

            // Version, with all segments except qualifier in bold
            Version version = template.getVersion() != null ? template.getVersion() : Version.emptyVersion;
            label.append(String.format("%d.%d.%d", version.getMajor(), version.getMinor(), version.getMicro()), UIConstants.BOLD_COUNTER_STYLER);
            label.append("." + version.getQualifier(), StyledString.COUNTER_STYLER);

            label.append(" \u2014 [", StyledString.QUALIFIER_STYLER).append(template.getShortDescription(), StyledString.QUALIFIER_STYLER).append("]", StyledString.QUALIFIER_STYLER);

            cell.setText(label.toString());
            cell.setStyleRanges(label.getStyleRanges());

            Image image = loadedImages.get(template);
            if (image != null)
                cell.setImage(image);
        }
    }

}

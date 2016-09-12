package org.bndtools.core.ui.wizards.shared;

import java.util.Map;

import org.bndtools.templating.Category;
import org.bndtools.templating.Template;
import org.bndtools.utils.jface.BoldStyler;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Version;

public class RepoTemplateLabelProvider extends StyledCellLabelProvider {

    private static final Image IMG_FOLDER = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

    private final Map<Template,Image> loadedImages;
    private final Image defaultIcon;

    public RepoTemplateLabelProvider(Map<Template,Image> loadedImages, Image defaultIcon) {
        this.loadedImages = loadedImages;
        this.defaultIcon = defaultIcon;
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
            StyledString label = new StyledString(template.getName(), BoldStyler.INSTANCE_DEFAULT);

            // Version, with all segments except qualifier in bold
            Version version = template.getVersion();
            if (version != null) {
                label.append(" ");
                label.append(String.format("%d.%d.%d", version.getMajor(), version.getMinor(), version.getMicro()), BoldStyler.INSTANCE_COUNTER);
                String q = version.getQualifier();
                if (q != null && !q.isEmpty())
                    label.append("." + q, StyledString.COUNTER_STYLER);
            }

            String description = template.getShortDescription();
            if (description != null) {
                label.append(" \u2014 [", StyledString.QUALIFIER_STYLER).append(template.getShortDescription(), StyledString.QUALIFIER_STYLER).append("]", StyledString.QUALIFIER_STYLER);
            }

            cell.setText(label.toString());
            cell.setStyleRanges(label.getStyleRanges());

            Image image = loadedImages.get(template);
            if (image == null)
                cell.setImage(defaultIcon);
            else
                cell.setImage(image);
        }
    }

}

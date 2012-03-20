package bndtools.editor.workspace;

import java.util.LinkedList;
import java.util.List;

import org.bndtools.core.utils.jface.StrikeoutStyler;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class PluginDeclarationLabelProvider extends StyledCellLabelProvider {

    private List<Image> images = new LinkedList<Image>();

    @Override
    public void update(ViewerCell cell) {
        IConfigurationElement element = (IConfigurationElement) cell.getElement();
        
        boolean deprecated = element.getAttribute("deprecated") != null;
        
        Styler mainStyler = deprecated ? new StrikeoutStyler(null) : null;
        StyledString label = new StyledString(element.getAttribute("name"), mainStyler);
        
        Styler classStyle = deprecated ? new StrikeoutStyler(StyledString.QUALIFIER_STYLER) : StyledString.QUALIFIER_STYLER;
        label.append(" [" + element.getAttribute("class") + "]", classStyle);
        
        cell.setText(label.toString());
        cell.setStyleRanges(label.getStyleRanges());

        String iconPath = element.getAttribute("icon");
        if (iconPath != null) {
            ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(element.getContributor().getName(), iconPath);
            if (descriptor != null) {
                Image image = descriptor.createImage();
                images.add(image);
                cell.setImage(image);
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        for (Image image : images) {
            image.dispose();
        }
    }

}

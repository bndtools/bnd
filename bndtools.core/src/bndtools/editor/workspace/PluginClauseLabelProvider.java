package bndtools.editor.workspace;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.model.clauses.HeaderClause;

public class PluginClauseLabelProvider extends StyledCellLabelProvider {

    private final Map<String, ImageDescriptor> imageDescriptors = new HashMap<String, ImageDescriptor>();
    private final Map<String, Image> images = new HashMap<String, Image>();

    public PluginClauseLabelProvider() {
        IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(Plugin.PLUGIN_ID, "bndPlugins");
        for (IConfigurationElement element : elements) {
            String className = element.getAttribute("class");
            String iconPath = element.getAttribute("icon");

            if (iconPath != null) {
                ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(element.getContributor().getName(), iconPath);
                if (descriptor != null)
                    imageDescriptors.put(className, descriptor);
            }
        }
    }

    @Override
    public void update(ViewerCell cell) {
        HeaderClause header = (HeaderClause) cell.getElement();

        StyledString label = new StyledString(header.getName());

        Map<String, String> attribs = header.getAttribs();
        if (!attribs.isEmpty())
            label.append(" ");
        for (Iterator<Entry<String, String>> iter = attribs.entrySet().iterator(); iter.hasNext(); ) {
            Entry<String, String> entry = iter.next();
            label.append(entry.getKey(), StyledString.QUALIFIER_STYLER);
            label.append("=", StyledString.QUALIFIER_STYLER);
            label.append(entry.getValue(), StyledString.COUNTER_STYLER);

            if (iter.hasNext())
                label.append(", ");
        }

        cell.setText(label.toString());
        cell.setStyleRanges(label.getStyleRanges());

        Image image = images.get(header.getName());
        if (image == null) {
            ImageDescriptor descriptor = imageDescriptors.get(header.getName());
            if (descriptor != null) {
                image = descriptor.createImage();
                images.put(header.getName(), image);
            }
        }
        if (image == null) {
            image = images.get("__DEFAULT__");
            if (image == null) {
                image = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/plugin.png").createImage();
                images.put("__DEFAULT__", image);
            }
        }
        cell.setImage(image);
    }

    @Override
    public void dispose() {
        super.dispose();
        for (Image image : images.values()) {
            if (!image.isDisposed()) image.dispose();
        }
    }
}

package org.bndtools.core.utils.jface;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class ImageCachingLabelProvider extends StyledCellLabelProvider {

    private final Map<String,Image> cache = new HashMap<String,Image>();

    protected synchronized Image getImage(String path, boolean returnMissingImageOnError) {
        Image image = cache.get(path);
        if (image != null)
            return image;

        ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, path);
        image = descriptor.createImage(returnMissingImageOnError);
        cache.put(path, image);

        return image;
    }

    @Override
    public void dispose() {
        super.dispose();

        synchronized (this) {
            for (Entry<String,Image> entry : cache.entrySet()) {
                Image image = entry.getValue();
                if (!image.isDisposed())
                    image.dispose();
            }
        }
    }
}

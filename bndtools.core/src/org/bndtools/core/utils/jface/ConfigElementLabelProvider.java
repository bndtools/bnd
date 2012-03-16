package org.bndtools.core.utils.jface;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;

public class ConfigElementLabelProvider extends StyledCellLabelProvider {

    private final Device device;
    private final Image defaultImg;
    
    private final Map<ImageDescriptor, Image> imgCache = new HashMap<ImageDescriptor, Image>();

    public ConfigElementLabelProvider(Device device, String defaultIconPath) {
        this.device = device;
        if (defaultIconPath != null)
            defaultImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, defaultIconPath).createImage(device);
        else
            defaultImg = null;
    }

    @Override
    public void update(ViewerCell cell) {
        IConfigurationElement element = (IConfigurationElement) cell.getElement();
        cell.setText(element.getAttribute("name"));

        Image icon = defaultImg;

        String iconPath = element.getAttribute("icon");
        if (iconPath != null) {
            ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(element.getContributor().getName(), iconPath);
            if (descriptor != null) {
                icon = imgCache.get(descriptor);
                if (icon == null) {
                    icon = descriptor.createImage(device);
                    imgCache.put(descriptor, icon);
                }
            }
        }

        if (icon != null)
            cell.setImage(icon);
    }

    @Override
    public void dispose() {
        super.dispose();
        defaultImg.dispose();
        for (Image cached: imgCache.values()) {
            cached.dispose();
        }
    }
}
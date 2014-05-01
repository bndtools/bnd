package org.bndtools.core.ui;

import java.util.HashMap;
import java.util.Map;

import org.bndtools.utils.eclipse.ConfigurationElementCategory;
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

    private final Map<ImageDescriptor,Image> imgCache = new HashMap<ImageDescriptor,Image>();

    public ConfigElementLabelProvider(Device device, String defaultIconPath) {
        this.device = device;
        if (defaultIconPath != null)
            defaultImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, defaultIconPath).createImage(device);
        else
            defaultImg = null;
    }

    @Override
    public void update(ViewerCell cell) {
        Image icon = defaultImg;
        ImageDescriptor iconDescriptor;

        Object data = cell.getElement();
        if (data instanceof ConfigurationElementCategory) {
            ConfigurationElementCategory category = (ConfigurationElementCategory) data;
            cell.setText(category.toString());
            iconDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/fldr_obj.gif");
        } else if (data instanceof IConfigurationElement) {
            IConfigurationElement element = (IConfigurationElement) data;
            cell.setText(element.getAttribute("name"));

            String iconPath = element.getAttribute("icon");
            if (iconPath != null)
                iconDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(element.getContributor().getName(), iconPath);
            else
                iconDescriptor = null;
        } else {
            cell.setText("<<ERROR>>");
            iconDescriptor = null;
        }

        if (iconDescriptor != null) {
            icon = imgCache.get(iconDescriptor);
            if (icon == null) {
                icon = iconDescriptor.createImage(device);
                imgCache.put(iconDescriptor, icon);
            }
        }

        if (icon != null)
            cell.setImage(icon);
    }

    @Override
    public void dispose() {
        super.dispose();
        safeDispose(defaultImg);
        for (Image cached : imgCache.values()) {
            safeDispose(cached);
        }
    }

    private void safeDispose(Image img) {
        if (img != null && !img.isDisposed())
            img.dispose();
    }
}
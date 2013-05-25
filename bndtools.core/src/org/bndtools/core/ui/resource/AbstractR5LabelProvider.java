package org.bndtools.core.ui.resource;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.service.ServiceNamespace;

import bndtools.Plugin;

public class AbstractR5LabelProvider extends StyledCellLabelProvider {

    protected Image genericImg;
    protected Image pkgImg;
    protected Image bundleImg;
    protected Image serviceImg;
    protected Image javaImg;

    public Image getIcon(String namespace) {
        if (IdentityNamespace.IDENTITY_NAMESPACE.equals(namespace)) {
            return getGenericImage();
        } else if (BundleNamespace.BUNDLE_NAMESPACE.equals(namespace)) {
            return getBundleImage();
        } else if (PackageNamespace.PACKAGE_NAMESPACE.equals(namespace)) {
            return getPackageImage();
        } else if (ServiceNamespace.SERVICE_NAMESPACE.equals(namespace)) {
            return getServiceImage();
        } else if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(namespace)) {
            return getJavaImage();
        }
        return null;
    }

    protected Image getGenericImage() {
        if (genericImg == null)
            genericImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/bullet_green.png").createImage();
        return genericImg;
    }

    protected Image getServiceImage() {
        if (serviceImg == null)
            serviceImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/service-tiny.png").createImage();
        return serviceImg;
    }

    protected Image getBundleImage() {
        if (bundleImg == null)
            bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/brick.png").createImage();
        return bundleImg;
    }

    protected Image getPackageImage() {
        if (pkgImg == null)
            pkgImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/package_obj.gif").createImage();
        return pkgImg;
    }

    protected Image getJavaImage() {
        if (javaImg == null)
            javaImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/java.png").createImage();
        return javaImg;
    }

    protected void disposeImg(Image img) {
        if (img != null && !img.isDisposed())
            img.dispose();
    }

}

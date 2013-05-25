package org.bndtools.core.ui.resource;

import java.util.Map.Entry;

import org.bndtools.core.utils.resources.ResourceUtils;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.contract.ContractNamespace;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;

import bndtools.Plugin;
import bndtools.UIConstants;

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
        return getGenericImage();
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

    protected void appendCapability(StyledString label, Capability cap) {
        String ns = cap.getNamespace();

        Object nsValue = cap.getAttributes().get(ns);
        String versionAttributeName = getVersionAttributeName(ns);
        if (nsValue != null) {
            label.append(ns + "=", StyledString.QUALIFIER_STYLER);
            label.append(nsValue.toString(), UIConstants.BOLD_STYLER);

            if (versionAttributeName != null) {
                Object version = cap.getAttributes().get(versionAttributeName);
                if (version != null) {
                    label.append("," + versionAttributeName, StyledString.QUALIFIER_STYLER);
                    label.append(version.toString(), UIConstants.BOLD_COUNTER_STYLER);
                }
            }
        } else {
            label.append(ns, UIConstants.BOLD_STYLER);
        }
        label.append(" ", StyledString.QUALIFIER_STYLER);

        label.append("[", StyledString.QUALIFIER_STYLER);
        boolean first = true;
        for (Entry<String,Object> entry : cap.getAttributes().entrySet()) {
            String key = entry.getKey();
            if (!key.equals(ns) && !key.equals(versionAttributeName)) {
                if (!first)
                    label.append(",", StyledString.QUALIFIER_STYLER);
                first = false;
                label.append(key + "=", StyledString.QUALIFIER_STYLER);
                label.append(entry.getValue() != null ? entry.getValue().toString() : "<null>", StyledString.QUALIFIER_STYLER);
            }
        }
        label.append("]", StyledString.QUALIFIER_STYLER);

    }

    protected void appendResourceLabel(StyledString label, Resource resource) {
        Capability identity = ResourceUtils.getIdentityCapability(resource);
        String name = ResourceUtils.getIdentity(identity);
        if (name == null)
            name = resource.toString();
        if (name == null)
            name = "<unknown>";
        label.append(name, UIConstants.BOLD_STYLER);

        Version version = ResourceUtils.getVersion(identity);
        if (version != null)
            label.append(" " + version, StyledString.COUNTER_STYLER);
    }

    protected String getVersionAttributeName(String ns) {
        String r = null;

        if (ns == null)
            r = null;
        else if (ns.equals(IdentityNamespace.IDENTITY_NAMESPACE))
            r = IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE;
        else if (ns.equals(ContentNamespace.CONTENT_NAMESPACE))
            r = null;
        else if (ns.equals(BundleNamespace.BUNDLE_NAMESPACE))
            r = BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
        else if (ns.equals(HostNamespace.HOST_NAMESPACE))
            r = HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE;
        else if (ns.equals(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE))
            r = ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE;
        else if (ns.equals(PackageNamespace.PACKAGE_NAMESPACE))
            r = PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE;
        else if (ns.equals(ExtenderNamespace.EXTENDER_NAMESPACE))
            r = ExtenderNamespace.CAPABILITY_VERSION_ATTRIBUTE;
        else if (ns.equals(ContractNamespace.CONTRACT_NAMESPACE))
            r = ContractNamespace.CAPABILITY_VERSION_ATTRIBUTE;
        else if (ns.equals(ServiceNamespace.SERVICE_NAMESPACE))
            r = null;

        return r;
    }

}

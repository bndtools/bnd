package org.bndtools.core.resolve.ui;

import org.bndtools.core.utils.resources.ResourceUtils;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import bndtools.Plugin;

public class UnresolvedRequirementsLabelProvider extends StyledCellLabelProvider {

    private final Image bundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/brick.png").createImage();
    private final Image packageImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/package_imp.gif").createImage();
    private final Image eeImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/link.png").createImage();
    private final Image serviceImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/service.png").createImage();

    @Override
    public void update(ViewerCell cell) {
        Requirement requirement = (Requirement) cell.getElement();

        Resource resource = requirement.getResource();
        Capability identity = ResourceUtils.getIdentityCapability(resource);

        StyledString label = new StyledString(requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));

        if (Namespace.RESOLUTION_OPTIONAL.equals(requirement.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE)))
            label.append(" OPTIONALLY", StyledString.QUALIFIER_STYLER);
        label.append(" REQUIRED BY ", StyledString.QUALIFIER_STYLER);

        String bsn = ResourceUtils.getIdentity(identity);
        if (bsn == null)
            bsn = resource.toString();
        if (bsn == null)
            bsn = "<unknown>";
        label.append(bsn);

        Version version = ResourceUtils.getVersion(identity);
        if (version != null)
            label.append(" " + version, StyledString.COUNTER_STYLER);

        cell.setText(label.getString());
        cell.setStyleRanges(label.getStyleRanges());
        setImage(cell, requirement.getNamespace());
    }

    private void setImage(ViewerCell cell, String namespace) {
        if (BundleNamespace.BUNDLE_NAMESPACE.equals(namespace)) {
            cell.setImage(bundleImg);
        } else if (PackageNamespace.PACKAGE_NAMESPACE.equals(namespace)) {
            cell.setImage(packageImg);
        } else if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(namespace)) {
            cell.setImage(eeImg);
        } else if (ServiceNamespace.SERVICE_NAMESPACE.equals(namespace)) {
            cell.setImage(serviceImg);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        bundleImg.dispose();
        packageImg.dispose();
        eeImg.dispose();
        serviceImg.dispose();
    }
}

package org.bndtools.core.ui.resource;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.core.utils.resources.ResourceUtils;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

import bndtools.Plugin;
import bndtools.UIConstants;

public class RequirementLabelProvider extends StyledCellLabelProvider {

    private final ConcurrentMap<String,Pattern> namespacePatterns = new ConcurrentHashMap<String,Pattern>();

    private Image genericImg;
    private Image pkgImg;
    private Image bundleImg;
    private Image serviceImg;
    private Image javaImg;

    private Pattern getFilterPattern(String name) {
        Pattern pattern = namespacePatterns.get(name);
        if (pattern == null) {
            pattern = Pattern.compile("\\(" + name + "[<>]?=([^\\)]*)\\)");
            Pattern existing = namespacePatterns.putIfAbsent(name, pattern);
            if (existing != null)
                pattern = existing;
        }
        return pattern;
    }

    public StyledString getLabel(Requirement requirement) {
        String filter = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);

        StyledString label = new StyledString(filter, StyledString.QUALIFIER_STYLER);
        String namespace = requirement.getNamespace();
        if (namespace != null) {
            applyStyle(namespace, UIConstants.BOLD_STYLER, label);

            String versionAttrib = ResourceUtils.getVersionAttributeForNamespace(namespace);
            if (versionAttrib != null)
                applyStyle(versionAttrib, UIConstants.BOLD_COUNTER_STYLER, label);
        }
        return label;
    }

    private void applyStyle(String name, Styler styler, StyledString label) {
        Matcher matcher = getFilterPattern(name).matcher(label.getString());
        while (matcher.find()) {
            int begin = matcher.start(1);
            int end = matcher.end(1);
            label.setStyle(begin, end - begin, styler);
        }
    }

    public Image getIcon(Requirement requirement) {
        String ns = requirement.getNamespace();
        if (IdentityNamespace.IDENTITY_NAMESPACE.equals(ns)) {
            return getGenericImage();
        } else if (BundleNamespace.BUNDLE_NAMESPACE.equals(ns)) {
            return getBundleImage();
        } else if (PackageNamespace.PACKAGE_NAMESPACE.equals(ns)) {
            return getPackageImage();
        } else if (ServiceNamespace.SERVICE_NAMESPACE.equals(ns)) {
            return getServiceImage();
        } else if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(ns)) {
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

    @Override
    public void update(ViewerCell cell) {
        Object element = cell.getElement();
        if (element instanceof Requirement) {
            Requirement requirement = (Requirement) element;

            StyledString label = getLabel(requirement);
            cell.setText(label.getString());
            cell.setStyleRanges(label.getStyleRanges());

            cell.setImage(getIcon(requirement));
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        disposeImg(pkgImg);
        disposeImg(bundleImg);
        disposeImg(serviceImg);
        disposeImg(genericImg);
        disposeImg(javaImg);
    }

    private void disposeImg(Image img) {
        if (img != null && !img.isDisposed())
            img.dispose();
    }

}

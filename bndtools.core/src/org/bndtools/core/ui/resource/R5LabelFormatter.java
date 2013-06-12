package org.bndtools.core.ui.resource;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.utils.resources.ResourceUtils;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
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
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;

import bndtools.UIConstants;

public class R5LabelFormatter {

    private static final ConcurrentMap<String,Pattern> FILTER_PATTERNS = new ConcurrentHashMap<String,Pattern>();

    public static String getVersionAttributeName(String ns) {
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

    public static String getNamespaceImagePath(String ns) {
        String r = "icons/bullet_green.png"; // generic green dot

        if (BundleNamespace.BUNDLE_NAMESPACE.equals(ns) || HostNamespace.HOST_NAMESPACE.equals(ns))
            r = "icons/brick.png";
        else if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(ns))
            r = "icons/java.png";
        else if (PackageNamespace.PACKAGE_NAMESPACE.equals(ns))
            r = "icons/package_obj.gif";
        else if (ServiceNamespace.SERVICE_NAMESPACE.equals(ns))
            r = "icons/service-tiny.png";

        return r;
    }

    public static void appendCapability(StyledString label, Capability cap) {
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

    public static void appendResourceLabel(StyledString label, Resource resource) {
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

    public static void appendRequirementLabel(StyledString label, Requirement requirement) {
        String filter = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
        if (filter == null) {
            // not a proper requirement... maybe a substitution?
            label.append("[parse error]", StyledString.QUALIFIER_STYLER);
        } else {
            label.append(filter, StyledString.QUALIFIER_STYLER);
            String namespace = requirement.getNamespace();
            if (namespace != null) {
                applyPattern(getFilterPattern(namespace), UIConstants.BOLD_STYLER, label);

                String versionAttrib = ResourceUtils.getVersionAttributeForNamespace(namespace);
                if (versionAttrib != null)
                    applyPattern(getFilterPattern(versionAttrib), UIConstants.BOLD_COUNTER_STYLER, label);
            }
        }
    }

    private static Pattern getFilterPattern(String name) {
        Pattern pattern = FILTER_PATTERNS.get(name);
        if (pattern == null) {
            pattern = Pattern.compile("\\(" + name + "[<>]?=([^\\)]*)\\)");
            Pattern existing = FILTER_PATTERNS.putIfAbsent(name, pattern);
            if (existing != null)
                pattern = existing;
        }
        return pattern;
    }

    private static void applyPattern(Pattern pattern, Styler styler, StyledString label) {
        if (pattern == null)
            return;
        Matcher matcher = pattern.matcher(label.getString());
        while (matcher.find()) {
            int begin = matcher.start(1);
            int end = matcher.end(1);
            label.setStyle(begin, end - begin, styler);
        }
    }

}

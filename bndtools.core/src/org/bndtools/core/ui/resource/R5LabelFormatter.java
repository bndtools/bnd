package org.bndtools.core.ui.resource;

import java.util.Map.Entry;

import org.bndtools.utils.resources.ResourceUtils;
import org.eclipse.jface.viewers.StyledString;
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

import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.FilterParser.Expression;
import aQute.bnd.osgi.resource.FilterParser.Op;
import aQute.bnd.osgi.resource.FilterParser.RangeExpression;
import aQute.bnd.osgi.resource.FilterParser.SimpleExpression;
import aQute.bnd.osgi.resource.FilterParser.WithRangeExpression;
import bndtools.UIConstants;

public class R5LabelFormatter {
    static FilterParser filterParser = new FilterParser();

    public static String getVersionAttributeName(String ns) {
        String r;

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
        else
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
        if (name == null) {
            if (resource != null) {
                name = resource.toString();
            } else {
                name = "<unknown>";
            }
        }
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
        } else
            try {
                String namespace = requirement.getNamespace();
                if (!IdentityNamespace.IDENTITY_NAMESPACE.equals(namespace)) {
                    String category = FilterParser.namespaceToCategory(namespace);
                    if (category != null && category.length() > 0)
                        label.append(category + ": ", StyledString.QUALIFIER_STYLER);
                }

                FilterParser fp = new FilterParser();
                String filterStr = requirement.getDirectives().get("filter");
                if (filterStr == null) {
                    label.append("<no filter>", UIConstants.ERROR_STYLER);
                } else {
                    Expression exp = fp.parse(filter);
                    if (exp instanceof WithRangeExpression) {
                        label.append(((WithRangeExpression) exp).printExcludingRange(), UIConstants.BOLD_STYLER);
                        RangeExpression range = ((WithRangeExpression) exp).getRangeExpression();
                        if (range != null)
                            label.append(" ").append(formatRangeString(range), StyledString.COUNTER_STYLER);
                    } else {
                        label.append(exp.toString());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

    public static String formatRangeString(RangeExpression range) {
        StringBuilder sb = new StringBuilder();

        SimpleExpression low = range.getLow();
        if (low == null) {
            sb.append("[0");
        } else {
            if (low.getOp() == Op.GREATER)
                sb.append("(");
            else
                sb.append("[");
            sb.append(low.getValue());
        }

        sb.append(", ");

        SimpleExpression high = range.getHigh();
        if (high == null) {
            sb.append("\u221e]"); // INFINITY Unicode: U+221E, UTF-8: E2 88 9E
        } else {
            sb.append(high.getValue());
            if (high.getOp() == Op.LESS)
                sb.append(")");
            else
                sb.append("]");
        }
        return sb.toString();
    }

}

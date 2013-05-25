package org.bndtools.core.ui.resource;

import java.util.Iterator;
import java.util.Map.Entry;

import org.bndtools.core.utils.resources.ResourceUtils;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

import bndtools.UIConstants;

public class WireLabelProvider extends RequirementLabelProvider {

    @Override
    public void update(ViewerCell cell) {
        Wire wire = (Wire) cell.getElement();

        String ns = wire.getCapability().getNamespace();
        cell.setImage(getIcon(ns));

        StyledString label = new StyledString();

        // Format the capability
        label.append("[", StyledString.QUALIFIER_STYLER);
        for (Iterator<Entry<String,Object>> iter = wire.getCapability().getAttributes().entrySet().iterator(); iter.hasNext();) {
            Entry<String,Object> entry = iter.next();
            label.append(entry.getKey() + "=", StyledString.QUALIFIER_STYLER);
            label.append(entry.getValue() != null ? entry.getValue().toString() : "<null>", UIConstants.BOLD_STYLER);
            if (iter.hasNext())
                label.append(",", StyledString.QUALIFIER_STYLER);
        }
        label.append("]", StyledString.QUALIFIER_STYLER);

        // Format the middle bit
        if (Namespace.RESOLUTION_OPTIONAL.equals(wire.getRequirement().getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE)))
            label.append(" OPTIONALLY", StyledString.QUALIFIER_STYLER);
        label.append(" REQUIRED BY ", StyledString.QUALIFIER_STYLER);

        Resource resource = wire.getRequirer();
        if (resource != null)
            appendResourceLabel(label, resource);
        else
            label.append(" INITIAL");

        cell.setText(label.getString());
        cell.setStyleRanges(label.getStyleRanges());
    }

    protected void appendResourceLabel(StyledString label, Resource resource) {
        Capability identity = ResourceUtils.getIdentityCapability(resource);
        String name = ResourceUtils.getIdentity(identity);
        if (name == null)
            name = resource.toString();
        if (name == null)
            name = "<unknown>";
        label.append(name);

        Version version = ResourceUtils.getVersion(identity);
        if (version != null)
            label.append(" " + version, StyledString.COUNTER_STYLER);
    }

}

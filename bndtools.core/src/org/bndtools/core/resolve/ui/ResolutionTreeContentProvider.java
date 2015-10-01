package org.bndtools.core.resolve.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

public class ResolutionTreeContentProvider implements ITreeContentProvider {

    @SuppressWarnings("unused")
    private boolean optional;
    private Map<Resource,List<Wire>> resolution;

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public void setResolution(Map<Resource,List<Wire>> resolution) {
        this.resolution = resolution;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

    @Override
    public Object[] getElements(Object input) {
        return getChildren(input);
    }

    @Override
    public Object[] getChildren(Object parent) {
        Object[] result;

        if (parent instanceof Resource) {
            Resource parentResource = (Resource) parent;

            Map<Capability,ResolutionTreeItem> items = new HashMap<Capability,ResolutionTreeItem>();
            List<Wire> wires = resolution.get(parentResource);
            processWires(wires, items);

            result = items.values().toArray(new Object[items.size()]);
        } else if (parent instanceof Requirement) {
            result = getChildren(((Requirement) parent).getResource());
        } else if (parent instanceof ResolutionTreeItem) {
            ResolutionTreeItem item = (ResolutionTreeItem) parent;
            List<Wire> wires = item.getWires();
            List<Requirement> reqs = new ArrayList<Requirement>();
            for (Wire wire : wires)
                reqs.add(wire.getRequirement());
            result = reqs.toArray(new Object[reqs.size()]);
        } else {
            result = null;
        }

        return result;
    }

    private static void processWires(Collection<Wire> wires, Map<Capability,ResolutionTreeItem> items) {
        if (wires != null) {
            for (Wire wire : wires) {
                ResolutionTreeItem item = items.get(wire.getCapability());
                if (item == null) {
                    item = new ResolutionTreeItem(wire.getCapability());
                    items.put(wire.getCapability(), item);
                }
                item.addWire(wire);
            }
        }
    }

    @Override
    public Object getParent(Object object) {
        return null;
    }

    @Override
    public boolean hasChildren(Object object) {
        return true;
    }

    @Override
    public void dispose() {}

}

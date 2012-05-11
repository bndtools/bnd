package bndtools.model.obr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ResolutionFailureTreeContentProvider implements ITreeContentProvider {
    
    private final List<Reason> roots = new ArrayList<Reason>();
    private final Map<Capability, Resource> capabilities = new HashMap<Capability, Resource>();
    private final Map<Resource, List<Reason>> unresolved = new HashMap<Resource, List<Reason>>();
    
    private Resolver resolver;
    
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        roots.clear();
        capabilities.clear();
        unresolved.clear();
        
        resolver = (Resolver) newInput;
        for (Reason reason : resolver.getUnsatisfiedRequirements()) {
            Resource requiredResource = reason.getResource();
            if (isRoot(requiredResource)) {
                roots.add(reason);
            } else {
                addUnresolved(requiredResource, reason);
                addCapabilities(requiredResource);
            }
        }
    }
    
    private void addUnresolved(Resource resource, Reason reason) {
        List<Reason> reasons = unresolved.get(resource);
        if (reasons == null) {
            reasons = new LinkedList<Reason>();
            unresolved.put(resource, reasons);
        }
        reasons.add(reason);
    }

    private void addCapabilities(Resource resource) {
        Capability[] caps = resource.getCapabilities();
        for (Capability cap : caps) {
            capabilities.put(cap, resource);
        }
    }

    public boolean isRoot(Resource resource) {
        return resource == null || resource.getId() == null;
    }
    
    public Object[] getElements(Object input) {
        return (Reason[]) roots.toArray(new Reason[roots.size()]);
    }

    public void dispose() {
    }

    public Object[] getChildren(Object parentElem) {
        List<Reason> potentials = new LinkedList<Reason>();
        
        Reason sourceReason = (Reason) parentElem;
        Requirement sourceReq = sourceReason.getRequirement();
        
        for (Entry<Capability, Resource> entry : capabilities.entrySet()) {
            if (sourceReq.isSatisfied(entry.getKey())) {
                Resource potentialProvider = entry.getValue();
                List<Reason> potentialReasons = unresolved.get(potentialProvider);
                if (potentialReasons != null)
                    potentials.addAll(potentialReasons);
            }
        }
        
        return (Reason[]) potentials.toArray(new Reason[potentials.size()]);
    }

    public Object getParent(Object element) {
        return null;
    }

    public boolean hasChildren(Object parentElement) {
        return true;
    }

}

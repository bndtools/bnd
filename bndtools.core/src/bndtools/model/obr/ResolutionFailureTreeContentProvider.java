package bndtools.model.obr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ResolutionFailureTreeContentProvider implements ITreeContentProvider {

    // private final Comparator<Requirement> requirementComparator = new
    // RequirementComparator();
    // private final Comparator<Capability> capabilityComparator = new
    // CapabilityComparator(new PropertyComparator());
    // private final Comparator<Resource> resourceComparator = new
    // ResourceComparator();

    Set<Requirement> roots = new HashSet<Requirement>();
    Map<Resource,List<Reason>> unresolved = new HashMap<Resource,List<Reason>>();
    Map<Capability,Set<Resource>> capabilities = new HashMap<Capability,Set<Resource>>();

    Resolver resolver;

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        roots.clear();
        unresolved.clear();
        capabilities.clear();

        resolver = (Resolver) newInput;
        if (resolver == null)
            return;

        for (Reason reason : resolver.getUnsatisfiedRequirements()) {
            Resource requiredResource = reason.getResource();
            if (isRoot(requiredResource)) {
                roots.add(reason.getRequirement());
            } else {
                try {
                    addUnresolved(requiredResource, reason);
                    addCapabilities(requiredResource);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    private static boolean isRoot(Resource resource) {
        return resource == null || resource.getId() == null;
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
            Set<Resource> resources = capabilities.get(cap);
            if (resources == null) {
                resources = new HashSet<Resource>();
                capabilities.put(cap, resources);
            }
            resources.add(resource);
        }
    }

    private PotentialMatch findPotentialMatch(Requirement requirement) {
        Set<Resource> resources = new HashSet<Resource>();

        for (Entry<Capability,Set<Resource>> entry : capabilities.entrySet()) {
            if (requirement.isSatisfied(entry.getKey())) {
                for (Resource potential : entry.getValue()) {
                    resources.add(potential);
                }
            }
        }

        return new PotentialMatch(requirement, resources);
    }

    public Object[] getElements(Object input) {
        List<PotentialMatch> matches = new ArrayList<PotentialMatch>(roots.size());
        for (Requirement requirement : roots) {
            PotentialMatch match = findPotentialMatch(requirement);
            matches.add(match);
        }
        return (PotentialMatch[]) matches.toArray(new PotentialMatch[matches.size()]);
    }

    public Object[] getChildren(Object parentElem) {
        Object[] children;
        if (parentElem instanceof PotentialMatch) {
            Collection<Resource> resources = ((PotentialMatch) parentElem).getResources();
            children = (Resource[]) resources.toArray(new Resource[resources.size()]);
        } else if (parentElem instanceof Resource) {
            List<Reason> reasons = unresolved.get(parentElem);
            List<PotentialMatch> matches = new LinkedList<PotentialMatch>();
            for (Reason reason : reasons) {
                PotentialMatch match = findPotentialMatch(reason.getRequirement());
                matches.add(match);
            }
            children = matches.toArray(new Object[matches.size()]);
        } else {
            children = null;
        }
        return children;
    }

    public Object getParent(Object element) {
        return null;
    }

    public boolean hasChildren(Object parentElement) {
        if (parentElement instanceof PotentialMatch) {
            return !((PotentialMatch) parentElement).getResources().isEmpty();
        }
        return true;
    }

    public void dispose() {}

}

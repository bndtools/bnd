package org.bndtools.core.obr;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.eclipse.core.runtime.IStatus;

import bndtools.model.obr.ReasonComparator;
import bndtools.model.obr.RequirementComparator;
import bndtools.model.obr.ResourceComparator;

public class ObrResolutionResult {

    private final boolean resolved;
    private final IStatus status;
    private final List<Resource> required;
    private final List<Resource> optional;
    private final Resolver resolver;

    public ObrResolutionResult(Resolver resolver, boolean resolved, IStatus status, List<Resource> required, List<Resource> optional) {
        this.resolver = resolver;
        this.resolved = resolved;
        this.status = status;
        this.required = required;
        this.optional = optional;
    }
    
    public Resolver getResolver() {
        return resolver;
    }

    public boolean isResolved() {
        return resolved;
    }

    public IStatus getStatus() {
        return status;
    }

    public List<Resource> getRequired() {
        return required;
    }

    public List<Resource> getOptional() {
        return optional;
    }

    public Reason[] getReason(Resource resource) {
        if (resolver == null)
            return new Reason[0];
        
        Reason[] reasons = resolver.getReason(resource);
        
        if (reasons == null)
            reasons = new Reason[0];
        
        // De-dupe. The resolver returns two copies of everything...?!
        Set<Reason> set = new TreeSet<Reason>(new ReasonComparator(new ResourceComparator(), new RequirementComparator()));
        for (Reason reason : reasons) {
            set.add(reason);
        }
        return set.toArray(new Reason[set.size()]);
    }
}

package biz.aQute.resolve;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.service.Registry;
import biz.aQute.resolve.internal.BndrunResolveContext;

public class ResolveProcess {

    private Collection<Resource> requiredResources;
    private Map<Resource,Collection<Requirement>> optionalResources;
    private ResolutionException resolutionException;

    public boolean resolve(BndEditModel inputModel, Registry pluginRegistry, Resolver resolver) {
        BndrunResolveContext resolveContext = new BndrunResolveContext(inputModel, pluginRegistry);
        try {
            Map<Resource,List<Wire>> result = resolver.resolve(resolveContext);

            // Find required resources
            requiredResources = new HashSet<Resource>(result.size());
            for (Resource resource : result.keySet()) {
                if (!resolveContext.isInputRequirementsResource(resource) && !resolveContext.isFrameworkResource(resource)) {
                    requiredResources.add(resource);
                }
            }

            // Find optional resources
            optionalResources = new HashMap<Resource,Collection<Requirement>>();
            for (Entry<Requirement,List<Capability>> entry : resolveContext.getOptionalRequirements().entrySet()) {
                Requirement req = entry.getKey();
                Resource requirer = req.getResource();
                if (requiredResources.contains(requirer)) {
                    List<Capability> providers = entry.getValue();
                    for (Capability provider : providers) {
                        Collection<Requirement> reasons = optionalResources.get(provider.getResource());
                        if (reasons == null) {
                            reasons = new LinkedList<Requirement>();
                            optionalResources.put(provider.getResource(), reasons);
                        }
                        reasons.add(req);
                    }
                }
            }

            return true;
        } catch (ResolutionException e) {
            resolutionException = e;
            return false;
        }
    }

    public ResolutionException getResolutionException() {
        return resolutionException;
    }

    public Collection<Resource> getRequiredResources() {
        return requiredResources != null ? Collections.unmodifiableCollection(requiredResources) : Collections.<Resource> emptyList();
    }

    public Collection<Resource> getOptionalResources() {
        return optionalResources != null ? Collections.unmodifiableCollection(optionalResources.keySet()) : Collections.<Resource> emptyList();
    }

    public Collection<Requirement> getReasons(Resource resource) {
        Collection<Requirement> reasons = optionalResources.get(resource);
        return reasons != null ? Collections.unmodifiableCollection(reasons) : Collections.<Requirement> emptyList();
    }

}

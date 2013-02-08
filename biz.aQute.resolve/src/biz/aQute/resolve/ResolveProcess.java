package biz.aQute.resolve;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.log.LogService;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.service.Registry;
import biz.aQute.resolve.internal.BndrunResolveContext;

public class ResolveProcess {

    private Map<Resource,Collection<Requirement>> requiredResources;
    private Map<Resource,Collection<Requirement>> optionalResources;
    private ResolutionException resolutionException;

    public boolean resolve(BndEditModel inputModel, Registry pluginRegistry, Resolver resolver, LogService log) {
        BndrunResolveContext resolveContext = new BndrunResolveContext(inputModel, pluginRegistry, log);
        try {
            Map<Resource,List<Wire>> result = resolver.resolve(resolveContext);

            // Find required resources
            Set<Resource> requiredResourceSet = new HashSet<Resource>(result.size());
            for (Resource resource : result.keySet()) {
                if (!resolveContext.isInputRequirementsResource(resource) && !resolveContext.isFrameworkResource(resource)) {
                    requiredResourceSet.add(resource);
                }
            }

            // Process the mandatory requirements and save them as reasons against the required resources
            requiredResources = new HashMap<Resource,Collection<Requirement>>(requiredResourceSet.size());
            for (Entry<Requirement,List<Capability>> entry : resolveContext.getMandatoryRequirements().entrySet()) {
                Requirement req = entry.getKey();
                Resource requirer = req.getResource();
                if (requiredResourceSet.contains(requirer)) {
                    List<Capability> caps = entry.getValue();

                    for (Capability cap : caps) {
                        Resource requiredResource = cap.getResource();
                        if (requiredResourceSet.remove(requiredResource)) {
                            Collection<Requirement> reasons = requiredResources.get(requiredResource);
                            if (reasons == null) {
                                reasons = new LinkedList<Requirement>();
                                requiredResources.put(requiredResource, reasons);
                            }
                            reasons.add(req);
                        }
                    }
                }
            }
            // Add the remaining resources in the requiredResourceSet (these come from initial requirements)
            for (Resource resource : requiredResourceSet)
                requiredResources.put(resource, Collections.<Requirement> emptyList());

            // Find optional resources
            processOptionalRequirements(resolveContext);

            return true;
        } catch (ResolutionException e) {
            resolutionException = e;
            return false;
        }
    }

    private void processOptionalRequirements(BndrunResolveContext resolveContext) {
        optionalResources = new HashMap<Resource,Collection<Requirement>>();
        for (Entry<Requirement,List<Capability>> entry : resolveContext.getOptionalRequirements().entrySet()) {
            Requirement req = entry.getKey();
            Resource requirer = req.getResource();
            if (requiredResources.containsKey(requirer)) {
                List<Capability> providers = entry.getValue();
                for (Capability provider : providers) {
                    Resource providerResource = provider.getResource();
                    if (requirer != providerResource && !requiredResources.containsKey(providerResource)) {
                        Collection<Requirement> reasons = optionalResources.get(provider.getResource());
                        if (reasons == null) {
                            reasons = new LinkedList<Requirement>();
                            optionalResources.put(provider.getResource(), reasons);
                        }
                        reasons.add(req);
                    }
                }
            }
        }
    }

    public ResolutionException getResolutionException() {
        return resolutionException;
    }

    public Collection<Resource> getRequiredResources() {
        return requiredResources != null ? Collections.unmodifiableCollection(requiredResources.keySet()) : Collections.<Resource> emptyList();
    }

    public Collection<Resource> getOptionalResources() {
        return optionalResources != null ? Collections.unmodifiableCollection(optionalResources.keySet()) : Collections.<Resource> emptyList();
    }

    public Collection<Requirement> getReasons(Resource resource) {
        Collection<Requirement> reasons = requiredResources.get(resource);
        if (reasons == null)
            reasons = optionalResources.get(resource);
        return reasons != null ? Collections.unmodifiableCollection(reasons) : Collections.<Requirement> emptyList();
    }

}

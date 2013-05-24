package biz.aQute.resolve;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.log.LogService;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.service.Registry;
import biz.aQute.resolve.internal.BndrunResolveContext;

public class ResolveProcess {

    private Map<Resource,List<Wire>> required;
    // private Map<URI,Map<Capability,Collection<Requirement>>> optionalReasons;

    private ResolutionException resolutionException;

    public boolean resolve(BndEditModel inputModel, Registry pluginRegistry, Resolver resolver, LogService log) {
        try {
            // Resolve required resources
            BndrunResolveContext resolveContext = new BndrunResolveContext(inputModel, pluginRegistry, log);
            Map<Resource,List<Wire>> wirings = resolver.resolve(resolveContext);
            required = invertWirings(wirings);
            removeFrameworkAndInputResources(required, resolveContext);

            return true;
        } catch (ResolutionException e) {
            resolutionException = e;
            return false;
        }
    }

    /*
     * private void processOptionalRequirements(BndrunResolveContext resolveContext) { optionalReasons = new
     * HashMap<URI,Map<Capability,Collection<Requirement>>>(); for (Entry<Requirement,List<Capability>> entry :
     * resolveContext.getOptionalRequirements().entrySet()) { Requirement req = entry.getKey(); Resource requirer =
     * req.getResource(); if (requiredReasons.containsKey(getResourceURI(requirer))) { List<Capability> caps =
     * entry.getValue(); for (Capability cap : caps) { Resource providerResource = cap.getResource(); URI resourceUri =
     * getResourceURI(providerResource); if (requirer != providerResource) { // &&
     * !requiredResources.containsKey(providerResource)) Map<Capability,Collection<Requirement>> resourceReasons =
     * optionalReasons.get(cap.getResource()); if (resourceReasons == null) { resourceReasons = new
     * HashMap<Capability,Collection<Requirement>>(); optionalReasons.put(resourceUri, resourceReasons);
     * urisToResources.put(resourceUri, providerResource); } Collection<Requirement> capRequirements =
     * resourceReasons.get(cap); if (capRequirements == null) { capRequirements = new LinkedList<Requirement>();
     * resourceReasons.put(cap, capRequirements); } capRequirements.add(req); } } } } }
     */

    private static void removeFrameworkAndInputResources(Map<Resource,List<Wire>> resourceMap, BndrunResolveContext rc) {
        for (Iterator<Entry<Resource,List<Wire>>> iter = resourceMap.entrySet().iterator(); iter.hasNext();) {
            Entry<Resource,List<Wire>> entry = iter.next();
            if (rc.isFrameworkResource(entry.getKey()))
                iter.remove();
            else if (rc.isInputRequirementsResource(entry.getKey()))
                iter.remove();
        }
    }

    /**
     * Inverts the wiring map from the resolver. Whereas the resolver returns a map of resources and the list of wirings
     * FROM each resource, we want to know the list of wirings TO that resource. This is in order to show the user the
     * reasons for each resource being present in the result.
     */
    private static Map<Resource,List<Wire>> invertWirings(Map<Resource, ? extends Collection<Wire>> wirings) {
        Map<Resource,List<Wire>> inverted = new HashMap<Resource,List<Wire>>();
        for (Entry<Resource, ? extends Collection<Wire>> entry : wirings.entrySet()) {
            Resource requirer = entry.getKey();
            for (Wire wire : entry.getValue()) {
                Resource provider = wire.getProvider();

                // Filter out self-capabilities, i.e. requirer and provider are same
                if (provider == requirer)
                    continue;

                List<Wire> incoming = inverted.get(provider);
                if (incoming == null) {
                    incoming = new LinkedList<Wire>();
                    inverted.put(provider, incoming);
                }
                incoming.add(wire);
            }
        }
        return inverted;
    }

    public ResolutionException getResolutionException() {
        return resolutionException;
    }

    public Collection<Resource> getRequiredResources() {
        if (required == null)
            return Collections.emptyList();
        return Collections.unmodifiableCollection(required.keySet());
    }

    public Collection<Wire> getReasons(Resource resource) {
        Collection<Wire> wires = required.get(resource);
        if (wires == null)
            wires = Collections.emptyList();
        return wires;
    }

}

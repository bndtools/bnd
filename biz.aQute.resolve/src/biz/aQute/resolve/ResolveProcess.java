package biz.aQute.resolve;

import static org.osgi.framework.namespace.BundleNamespace.BUNDLE_NAMESPACE;
import static org.osgi.framework.namespace.IdentityNamespace.IDENTITY_NAMESPACE;
import static org.osgi.resource.Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE;
import static org.osgi.resource.Namespace.RESOLUTION_OPTIONAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.felix.resolver.reason.ReasonException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.log.LogService;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.service.resolver.Resolver;

import aQute.bnd.build.Project;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.WireImpl;
import aQute.bnd.service.Registry;
import aQute.lib.strings.Strings;
import aQute.libg.generics.Create;
import aQute.libg.tuple.Pair;

public class ResolveProcess {

	private static final String			ARROW	= "      ⇒ ";

	private Map<Resource, List<Wire>>	required;
	private Map<Resource, List<Wire>>	optional;

	private ResolutionException			resolutionException;

	public Map<Resource, List<Wire>> resolveRequired(BndEditModel inputModel, Registry plugins, Resolver resolver,
		Collection<ResolutionCallback> callbacks, LogService log) throws ResolutionException {
		try {
			return resolveRequired(inputModel.getProperties(), inputModel.getProject(), plugins, resolver, callbacks,
				log);
		} catch (Exception e) {
			if (e instanceof ResolutionException) {
				throw (ResolutionException) e;
			}
			throw new ResolutionException(e);
		}
	}

	public Map<Resource, List<Wire>> resolveRequired(Processor properties, Project project, Registry plugins,
		Resolver resolver, Collection<ResolutionCallback> callbacks, LogService log) throws ResolutionException {
		required = new HashMap<>();
		optional = new HashMap<>();

		BndrunResolveContext rc = new BndrunResolveContext(properties, project, plugins, log);
		rc.addCallbacks(callbacks);
		// 1. Resolve initial requirements
		Map<Resource, List<Wire>> wirings;
		try {
			wirings = resolver.resolve(rc);
		} catch (ResolutionException re) {
			throw augment(rc, re);
		}

		// 2. Save initial requirement resolution
		Pair<Resource, List<Wire>> initialRequirement = null;
		for (Map.Entry<Resource, List<Wire>> wiring : wirings.entrySet()) {
			if (rc.getInputResource() == wiring.getKey()) {
				initialRequirement = new Pair<>(wiring.getKey(), wiring.getValue());
				break;
			}
		}

		// 3. Save the resolved root resources
		final List<Resource> resources = new ArrayList<>();
		for (Resource r : rc.getMandatoryResources()) {
			reqs: for (Requirement req : r.getRequirements(null)) {
				for (Resource found : wirings.keySet()) {
					String filterStr = req.getDirectives()
						.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
					try {
						org.osgi.framework.Filter filter = filterStr != null
							? org.osgi.framework.FrameworkUtil.createFilter(filterStr)
							: null;

						for (Capability c : found.getCapabilities(req.getNamespace())) {
							if (filter != null && filter.matches(c.getAttributes())) {
								resources.add(found);
								continue reqs;
							}
						}
					} catch (InvalidSyntaxException e) {}
				}
			}
		}

		// 4. Add any 'osgi.wiring.bundle' requirements
		List<Resource> wiredBundles = new ArrayList<>();
		for (Resource resource : resources) {
			addWiredBundle(wirings, resource, wiredBundles);
		}
		for (Resource resource : wiredBundles) {
			if (!resources.contains(resource)) {
				resources.add(resource);
			}
		}

		final Map<Resource, List<Wire>> discoveredOptional = new LinkedHashMap<>();

		// 5. Resolve the rest
		BndrunResolveContext rc2 = new BndrunResolveContext(properties, project, plugins, log) {

			@Override
			public Collection<Resource> getMandatoryResources() {
				return resources;
			}

			@Override
			public boolean isInputResource(Resource resource) {
				for (Resource r : resources) {
					if (AbstractResolveContext.resourceIdentityEquals(r, resource)) {
						return true;
					}
				}
				return false;
			}

			@Override
			public List<Capability> findProviders(Requirement requirement) {

				List<Capability> toReturn = super.findProviders(requirement);

				if (toReturn.isEmpty() && isEffective(requirement)
					&& RESOLUTION_OPTIONAL.equals(requirement.getDirectives()
						.get(REQUIREMENT_RESOLUTION_DIRECTIVE))) {
					// We have an effective optional requirement that is
					// unmatched
					// AbstractResolveContext deliberately does not include
					// optionals,
					// so we force a repo check here so that we can populate
					// the optional
					// map

					for (Capability cap : findProvidersFromRepositories(requirement, new LinkedHashSet<>())) {

						Resource optionalRes = cap.getResource();

						List<Wire> list = discoveredOptional.get(optionalRes);

						if (list == null) {
							list = new ArrayList<>();
							discoveredOptional.put(optionalRes, list);
						}
						WireImpl candidateWire = new WireImpl(cap, requirement);
						if (!list.contains(candidateWire))
							list.add(candidateWire);
					}
				}

				return toReturn;
			}

		};

		rc2.addCallbacks(callbacks);
		try {
			wirings = resolver.resolve(rc2);
		} catch (ResolutionException re) {
			throw augment(rc2, re);
		}
		if (initialRequirement != null) {
			wirings.put(initialRequirement.getFirst(), initialRequirement.getSecond());
		}

		Map<Resource, List<Wire>> result = invertWirings(wirings, rc2);
		removeFrameworkAndInputResources(result, rc2);
		required.putAll(result);
		optional = tidyUpOptional(wirings, discoveredOptional, log);
		return result;
	}

	/*
	 * The Felix resolver reports an initial resource as unresolved if one of
	 * its requirements cannot be found, even though it is in the repo. This
	 * method will (try to) analyze what is actually missing. This is not
	 * perfect but should give some more diagnostics in most cases.
	 */
	public static ResolutionException augment(AbstractResolveContext context, ResolutionException re) {
		Set<Requirement> unresolved = Create.set();
		unresolved.addAll(re.getUnresolvedRequirements());
		unresolved.addAll(context.getFailed());
		return augment(unresolved, context, re);
	}

	public static ResolutionException augment(ResolveContext context, ResolutionException re)
		throws ResolutionException {
		return augment(re.getUnresolvedRequirements(), context, re);
	}

	/**
	 * Produce a 'chain of responsibility' for the resolution failure, including
	 * optional requirements.
	 *
	 * @param re the resolution exception
	 * @return the report
	 */
	public static String format(ResolutionException re) {
		return format(re, true);
	}

	/**
	 * Produce a 'chain of responsibility' for the resolution failure.
	 *
	 * @param re the resolution exception
	 * @param reportOptional if true, optional requirements are listed in the
	 *            output
	 * @return the report
	 */
	public static String format(ResolutionException re, boolean reportOptional) {
		List<Requirement> chain = new ArrayList<>();

		Throwable cause = re;
		while (cause != null) {
			if (cause instanceof ReasonException) {
				ReasonException mre = (ReasonException) cause;

				// there will only be one entry here
				chain.addAll(mre.getUnresolvedRequirements());
			}

			cause = cause.getCause();
		}

		Map<Boolean, List<Requirement>> requirements = re.getUnresolvedRequirements()
			.stream()
			.filter(req -> !chain.contains(req))
			.collect(Collectors.partitioningBy(ResolveProcess::isOptional));

		try (Formatter f = new Formatter()) {
			f.format("Resolution failed. Capabilities satisfying the following requirements could not be found:");
			String prefix = "    ";
			for (Requirement req : chain) {
				f.format("%n%s[%s]", prefix, req.getResource());
				if ("    ".equals(prefix))
					prefix = ARROW;
				else
					prefix = "    " + prefix;
				format(f, prefix, req);
				prefix = "    " + prefix;
			}

			requirements.get(Boolean.FALSE)
				.stream()
				.collect(Collectors.groupingBy(Requirement::getResource))
				.forEach(formatGroup(f));

			List<Requirement> optional = requirements.get(Boolean.TRUE);

			if (!optional.isEmpty() && reportOptional) {
				f.format("%nThe following requirements are optional:");
				optional.stream()
					.collect(Collectors.groupingBy(Requirement::getResource))
					.forEach(formatGroup(f));
			}

			return f.toString();
		}
	}

	static BiConsumer<? super Resource, ? super List<Requirement>> formatGroup(Formatter f) {
		return (resource, list) -> {
			f.format("%n    [%s]", resource);
			list.forEach(req -> {
				format(f, ARROW, req);
			});
		};
	}

	static void format(Formatter f, String prefix, Requirement req) {
		String filter = req.getDirectives()
			.get("filter");

		f.format("%n%s%s: %s", prefix, req.getNamespace(), filter);
	}

	public static String format(Collection<Requirement> requirements) {
		Set<Requirement> mandatory = new HashSet<>();
		Set<Requirement> optional = new HashSet<>();
		for (Requirement req : requirements) {
			if (isOptional(req))
				optional.add(req);
			else
				mandatory.add(req);
		}
		try (Formatter f = new Formatter()) {
			f.format("%n  Mandatory:");
			for (Requirement req : mandatory) {
				f.format("%n    [%-19s] %s", req.getNamespace(), req);
			}
			f.format("%n  Optional:");
			for (Requirement req : optional) {
				f.format("%n    [%-19s] %s", req.getNamespace(), req);
			}
			return f.toString();
		}
	}

	private static boolean isOptional(Requirement req) {
		String resolution = req.getDirectives()
			.get(Constants.RESOLUTION);

		if (resolution == null) {
			return false;
		}

		return Constants.OPTIONAL.equals(resolution);
	}

	private static ResolutionException augment(Collection<Requirement> unresolved, ResolveContext context,
		ResolutionException re) {
		if (unresolved.isEmpty()) {
			return re;
		}
		long deadline = System.currentTimeMillis() + 1000L;
		Set<Requirement> list = new HashSet<>(unresolved);
		Set<Resource> resources = new HashSet<>();
		try {
			for (Requirement r : unresolved) {
				Requirement find = missing(context, r, resources, deadline);
				if (find != null) {
					list.add(find);
				}
			}
		} catch (TimeoutException toe) {}
		return new ResolutionException(re.getMessage(), re, list);
	}

	/*
	 * Recursively traverse all requirement's resource requirement's
	 */
	private static Requirement missing(ResolveContext context, Requirement rq, Set<Resource> resources, long deadline)
		throws TimeoutException {
		resources.add(rq.getResource());

		if (deadline < System.currentTimeMillis())
			throw new TimeoutException();

		List<Capability> providers = context.findProviders(rq);

		//
		// This requirement cannot be found
		//

		if (providers.isEmpty())
			return rq;

		//
		// We first search breadth first for a capability that
		// satisfies our requirement and its 1st level requirements.
		//

		Set<Resource> candidates = new HashSet<>();

		Requirement missing = null;
		caps: for (Capability cap : providers) {

			for (Requirement sub : cap.getResource()
				.getRequirements(null)) {
				List<Capability> subProviders = context.findProviders(sub);
				if (subProviders.isEmpty()) {
					if (missing == null)
						missing = sub;

					//
					// this cap lacks its 1st level requirement
					// so try next capability
					//

					continue caps;
				}
			}

			//
			// We found a capability for our requirement
			// that matches, of course its resource might fail
			// later

			candidates.add(cap.getResource());
		}

		//
		// If we have no candidates, then we fail ...
		// missing is set then since at least 1 cap must have failed
		// and set missing since #providers > 0. I.e. our requirement
		// found a candidate, but no candidate succeeded to be satisfied.
		// Missing then contains the first missing requirement

		if (candidates.isEmpty()) {
			assert missing != null;
			return missing;
		}

		Requirement initialMissing = missing;
		missing = null;

		//
		// candidates now contains the resources that are potentially
		// able to satisfy our requirements.
		//
		candidates.removeAll(resources);
		resources.addAll(candidates);

		resource: for (Resource resource : candidates) {
			for (Requirement requirement : resource.getRequirements(null)) {
				Requirement r1 = missing(context, requirement, resources, deadline);
				if (r1 != null && missing != null) {
					missing = r1;
					continue resource;
				}
			}

			// A Fully matching resource

			return null;
		}

		//
		// None of the resources was resolvable
		//

		return missing == null ? initialMissing : missing;
	}

	private void addWiredBundle(Map<Resource, List<Wire>> wirings, Resource resource, List<Resource> result) {
		List<Requirement> reqs = resource.getRequirements(BUNDLE_NAMESPACE);
		for (Requirement req : reqs) {
			List<Wire> wrs = wirings.get(resource);
			for (Wire w : wrs) {
				if (w.getRequirement()
					.equals(req)) {
					Resource res = w.getProvider();
					if (res != null) {
						if (!result.contains(res)) {
							result.add(res);
							addWiredBundle(wirings, res, result);
						}
					}
				}
			}
		}
	}

	/*
	 * private void processOptionalRequirements(BndrunResolveContext
	 * resolveContext) { optionalReasons = new
	 * HashMap<URI,Map<Capability,Collection<Requirement>>>(); for
	 * (Entry<Requirement,List<Capability>> entry :
	 * resolveContext.getOptionalRequirements().entrySet()) { Requirement req =
	 * entry.getKey(); Resource requirer = req.getResource(); if
	 * (requiredReasons.containsKey(getResourceURI(requirer))) {
	 * List<Capability> caps = entry.getValue(); for (Capability cap : caps) {
	 * Resource providerResource = cap.getResource(); URI resourceUri =
	 * getResourceURI(providerResource); if (requirer != providerResource) { //
	 * && !requiredResources.containsKey(providerResource))
	 * Map<Capability,Collection<Requirement>> resourceReasons =
	 * optionalReasons.get(cap.getResource()); if (resourceReasons == null) {
	 * resourceReasons = new HashMap<Capability,Collection<Requirement>>();
	 * optionalReasons.put(resourceUri, resourceReasons);
	 * urisToResources.put(resourceUri, providerResource); }
	 * Collection<Requirement> capRequirements = resourceReasons.get(cap); if
	 * (capRequirements == null) { capRequirements = new
	 * LinkedList<Requirement>(); resourceReasons.put(cap, capRequirements); }
	 * capRequirements.add(req); } } } } }
	 */

	private static void removeFrameworkAndInputResources(Map<Resource, List<Wire>> resourceMap,
		AbstractResolveContext rc) {
		resourceMap.keySet()
			.removeIf(rc::isSystemResource);
	}

	/**
	 * Inverts the wiring map from the resolver. Whereas the resolver returns a
	 * map of resources and the list of wirings FROM each resource, we want to
	 * know the list of wirings TO that resource. This is in order to show the
	 * user the reasons for each resource being present in the result.
	 */
	private static Map<Resource, List<Wire>> invertWirings(Map<Resource, ? extends Collection<Wire>> wirings,
		AbstractResolveContext rc) {
		Map<Resource, List<Wire>> inverted = new HashMap<>();
		for (Entry<Resource, ? extends Collection<Wire>> entry : wirings.entrySet()) {
			Resource requirer = entry.getKey();
			for (Wire wire : entry.getValue()) {
				Resource provider = findResolvedProvider(wire, wirings.keySet(), rc);

				// Filter out self-capabilities, i.e. requirer and provider are
				// same
				if (provider == requirer)
					continue;

				List<Wire> incoming = inverted.get(provider);
				if (incoming == null) {
					incoming = new LinkedList<>();
					inverted.put(provider, incoming);
				}
				incoming.add(wire);
			}
		}
		return inverted;
	}

	private static Resource findResolvedProvider(Wire wire, Set<Resource> resources, AbstractResolveContext rc) {
		// Make sure not to add new resources into the result. The resolver
		// already created the closure of all the needed resources. We need to
		// find the key in the result that already provides the capability
		// defined by this wire.

		Capability capability = wire.getCapability();
		Resource resource = capability.getResource();
		if (rc.isSystemResource(resource) || (ResourceUtils.isFragment(resource) && resources.contains(resource))) {
			return resource;
		}

		for (Resource resolved : resources) {
			for (Capability resolvedCap : resolved.getCapabilities(capability.getNamespace())) {
				if (ResourceUtils.matches(wire.getRequirement(), resolvedCap)) {
					return resolved;
				}
			}
		}

		// It shouldn't be possible to arrive here!
		throw new IllegalStateException(
			Strings.format("The capability for wire %s was not associated with a resource in the resolution", wire));
	}

	private static Map<Resource, List<Wire>> tidyUpOptional(Map<Resource, List<Wire>> required,
		Map<Resource, List<Wire>> discoveredOptional, LogService log) {
		Map<Resource, List<Wire>> toReturn = new HashMap<>();

		Set<Capability> requiredIdentities = new HashSet<>();
		for (Resource r : required.keySet()) {
			Capability normalisedIdentity = toPureIdentity(r, log);
			if (normalisedIdentity != null) {
				requiredIdentities.add(normalisedIdentity);
			}
		}

		Set<Capability> acceptedIdentities = new HashSet<>();

		for (Entry<Resource, List<Wire>> entry : discoveredOptional.entrySet()) {
			// If we're required we are not also optional
			Resource optionalResource = entry.getKey();
			if (required.containsKey(optionalResource)) {
				continue;
			}
			// If another resource with the same identity is required
			// then we defer to it, otherwise we will get the an optional
			// resource showing up from a different repository
			Capability optionalIdentity = toPureIdentity(optionalResource, log);
			if (requiredIdentities.contains(optionalIdentity)) {
				continue;
			}

			// Only wires to required resources should kept
			List<Wire> validWires = new ArrayList<>();
			optional: for (Wire optionalWire : entry.getValue()) {
				Resource requirer = optionalWire.getRequirer();
				Capability requirerIdentity = toPureIdentity(requirer, log);
				if (required.containsKey(requirer)) {
					Requirement req = optionalWire.getRequirement();
					// Somebody does require this - do they have a match
					// already?
					List<Wire> requiredWires = required.get(requirer);
					for (Wire requiredWire : requiredWires) {
						if (req.equals(requiredWire.getRequirement())) {
							continue optional;
						}
					}
					validWires.add(optionalWire);
				}
			}
			// If there is at least one valid wire then we want the optional
			// resource, but only if we don't already have one for that identity
			// This can happen if the same resource is in multiple repos
			if (!validWires.isEmpty()) {
				if (acceptedIdentities.add(optionalIdentity)) {
					toReturn.put(optionalResource, validWires);
				} else {
					log.log(LogService.LOG_INFO, "Discarding the optional resource " + optionalResource
						+ " because another optional resource with the identity " + optionalIdentity
						+ " has already been selected. This usually happens when the same bundle is present in multiple repositories.");
				}
			}
		}

		return toReturn;
	}

	private static Capability toPureIdentity(Resource r, LogService log) {
		List<Capability> capabilities = r.getCapabilities(IDENTITY_NAMESPACE);
		if (capabilities.size() != 1) {
			log.log(LogService.LOG_WARNING,
				"The resource " + r + " has the wrong number of identity capabilities " + capabilities.size());
			return null;
		}
		try {
			return CapReqBuilder.copy(capabilities.get(0), null);
		} catch (Exception e) {
			log.log(LogService.LOG_ERROR, "Unable to copy the capability " + capabilities.get(0));
			return null;
		}
	}

	public ResolutionException getResolutionException() {
		return resolutionException;
	}

	public Collection<Resource> getRequiredResources() {
		if (required == null)
			return Collections.emptyList();
		return Collections.unmodifiableCollection(required.keySet());
	}

	public Collection<Resource> getOptionalResources() {
		if (optional == null)
			return Collections.emptyList();
		return Collections.unmodifiableCollection(optional.keySet());
	}

	public Collection<Wire> getRequiredReasons(Resource resource) {
		Collection<Wire> wires = required.get(resource);
		if (wires == null)
			wires = Collections.emptyList();
		return wires;
	}

	public Collection<Wire> getOptionalReasons(Resource resource) {
		Collection<Wire> wires = optional.get(resource);
		if (wires == null)
			wires = Collections.emptyList();
		return wires;
	}

	public Map<Resource, List<Wire>> getRequiredWiring() {
		return Collections.unmodifiableMap(required);
	}

	public Map<Resource, List<Wire>> getOptionalWiring() {
		return Collections.unmodifiableMap(optional);
	}

}

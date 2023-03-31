package aQute.bnd.osgi.resource;

import static aQute.lib.collections.Logic.retain;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.RepositoryContent;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;
import aQute.bnd.service.resource.SupportingResource;
import aQute.bnd.unmodifiable.Lists;

class ResourceImpl implements Resource, SupportingResource, Comparable<Resource>, RepositoryContent {
	boolean								built	= false;
	List<CapabilityImpl>				allCapabilities;
	Map<String, List<CapabilityImpl>>	capabilityMap;
	List<RequirementImpl>				allRequirements;
	Map<String, List<RequirementImpl>>	requirementMap;
	List<Resource>						supportingResources;
	Map<URI, String>					locations;
	int									hashCode	= -1;

	@SuppressWarnings({
		"rawtypes", "unchecked"
	})
	@Override
	public List<Capability> getCapabilities(String namespace) {
		assert built;
		if (namespace == null)
			return (List) allCapabilities;

		return (List) capabilityMap.getOrDefault(namespace, Collections.emptyList());
	}

	@SuppressWarnings({
		"rawtypes", "unchecked"
	})
	@Override
	public List<Requirement> getRequirements(String namespace) {
		assert built;
		if (namespace == null)
			return (List) allRequirements;

		return (List) requirementMap.getOrDefault(namespace, Collections.emptyList());
	}

	@Override
	public String toString() {

		if (!built) {
			return "Resource[not built yet]";
		}

		List<Capability> identities = getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		if (identities.size() == 1) {
			Map<String, Object> attributes = identities.get(0)
				.getAttributes();
			String id = String.valueOf(attributes.get(IdentityNamespace.IDENTITY_NAMESPACE));

			return switch (id) {
				case Constants.IDENTITY_INITIAL_RESOURCE, Constants.IDENTITY_SYSTEM_RESOURCE -> id;
				default -> {
					StringBuilder builder = new StringBuilder(id);
					Object version = attributes.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
					if (version != null) {
						builder.append(" version=")
							.append(version);
					}
					Object type = attributes.getOrDefault(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
						IdentityNamespace.TYPE_UNKNOWN);
					if (!IdentityNamespace.TYPE_BUNDLE.equals(type)) {
						builder.append(" type=")
							.append(type);
					}
					yield builder.toString();
				}
			};
		}
		// Generic toString
		return new StringBuilder("ResourceImpl [caps=").append(allCapabilities)
			.append(", reqs=")
			.append(allRequirements)
			.append(']')
			.toString();
	}

	@Override
	public int compareTo(Resource o) {
		return ResourceUtils.compareTo(this, o);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;

		if (other instanceof Resource resource) {

			Map<URI, String> otherLocations = (resource instanceof ResourceImpl resourceImpl) ? resourceImpl.locations
				: ResourceUtils.getLocations(resource);

			Collection<URI> overlap = retain(locations.keySet(), otherLocations.keySet());

			return overlap.stream()
				.anyMatch(uri -> {
					String thisSha = locations.get(uri);
					String otherSha = otherLocations.get(uri);
					return Objects.equals(thisSha, otherSha);
				});

		}
		return false;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public InputStream getContent() {
		assert built;

		try {
			ContentCapability c = ResourceUtils.getContentCapability(this);
			URI url = c.url();
			return url.toURL()
				.openStream();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Resource> getSupportingResources() {
		assert built;
		return supportingResources;
	}

	SupportingResource build(Map<String, List<CapabilityImpl>> capabilities,
		Map<String, List<RequirementImpl>> requirements, List<Resource> support) {
		assert !this.built;
		this.built = true;
		this.allCapabilities = Collections.unmodifiableList(flatten(capabilities));
		this.allRequirements = Collections.unmodifiableList(flatten(requirements));
		this.supportingResources = Collections.unmodifiableList(new ArrayList<>(support));
		this.capabilityMap = allCapabilities.stream()
			.collect(groupingBy(Capability::getNamespace, Lists.toList()));
		this.requirementMap = allRequirements.stream()
			.collect(groupingBy(Requirement::getNamespace, Lists.toList()));
		this.locations = ResourceUtils.getLocations(this);
		this.hashCode = locations.hashCode();
		return this;
	}

	static <T extends CapReq> List<T> flatten(Map<String, List<T>> capabilities) {
		return capabilities.values()
			.stream()
			.flatMap(List<T>::stream)
			.distinct()
			.sorted(ResourceImpl::compare)
			.collect(toList());
	}

	/**
	 * We order the wiring namespaces ahead of the other namespaces. This makes
	 * the resolver happier in some tests which otherwise fail when using simple
	 * namespace ordering.
	 */

	static <T extends CapReq> int compare(T left, T right) {
		return map(left).compareTo(map(right));
	}

	static String map(CapReq capreq) {
		return switch (capreq.getNamespace()) {
			case IdentityNamespace.IDENTITY_NAMESPACE -> " 1";
			case PackageNamespace.PACKAGE_NAMESPACE -> " 2";
			case BundleNamespace.BUNDLE_NAMESPACE -> " 3";
			case HostNamespace.HOST_NAMESPACE -> " 4";
			default -> capreq.getNamespace();
		};
	}

}

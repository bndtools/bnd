package aQute.bnd.osgi.resource;

import static aQute.bnd.osgi.resource.ResourceUtils.getLocations;
import static aQute.lib.collections.Logic.retain;
import static java.util.Collections.unmodifiableList;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.RepositoryContent;

import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;

class ResourceImpl implements Resource, Comparable<Resource>, RepositoryContent {

	private volatile List<Capability>				allCapabilities;
	private volatile Map<String, List<Capability>>	capabilityMap;
	private volatile List<Requirement>				allRequirements;
	private volatile Map<String, List<Requirement>>	requirementMap;

	private transient Map<URI, String>				locations;

	void setCapabilities(List<Capability> capabilities) {
		Map<String, List<Capability>> prepare = new HashMap<>();
		for (Capability capability : capabilities) {
			List<Capability> list = prepare.get(capability.getNamespace());
			if (list == null) {
				list = new LinkedList<>();
				prepare.put(capability.getNamespace(), list);
			}
			list.add(capability);
		}
		for (Map.Entry<String, List<Capability>> entry : prepare.entrySet()) {
			entry.setValue(unmodifiableList(new ArrayList<>(entry.getValue())));
		}

		allCapabilities = unmodifiableList(new ArrayList<>(capabilities));
		capabilityMap = prepare;
	}

	@Override
	public List<Capability> getCapabilities(String namespace) {
		List<Capability> caps = (namespace != null) ? capabilityMap.get(namespace) : allCapabilities;

		return (caps != null) ? caps : Collections.emptyList();
	}

	void setRequirements(List<Requirement> requirements) {
		Map<String, List<Requirement>> prepare = new HashMap<>();
		for (Requirement requirement : requirements) {
			List<Requirement> list = prepare.get(requirement.getNamespace());
			if (list == null) {
				list = new LinkedList<>();
				prepare.put(requirement.getNamespace(), list);
			}
			list.add(requirement);
		}
		for (Map.Entry<String, List<Requirement>> entry : prepare.entrySet()) {
			entry.setValue(unmodifiableList(new ArrayList<>(entry.getValue())));
		}

		allRequirements = unmodifiableList(new ArrayList<>(requirements));
		requirementMap = prepare;
	}

	@Override
	public List<Requirement> getRequirements(String namespace) {
		List<Requirement> reqs = (namespace != null) ? requirementMap.get(namespace) : allRequirements;

		return (reqs != null) ? reqs : Collections.emptyList();
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		List<Capability> identities = getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		if (identities.size() == 1) {
			Capability idCap = identities.get(0);
			Object id = idCap.getAttributes()
				.get(IdentityNamespace.IDENTITY_NAMESPACE);
			builder.append(id);

			Object version = idCap.getAttributes()
				.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
			if (version != null) {
				builder.append(" version=")
					.append(version);
			}
		} else {
			// Generic toString
			builder.append("ResourceImpl [caps=");
			builder.append(allCapabilities);
			builder.append(", reqs=");
			builder.append(allRequirements);
			builder.append("]");
		}
		return builder.toString();
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

		if (other == null || !(other instanceof Resource))
			return false;

		Map<URI, String> thisLocations = getContentURIs();
		Map<URI, String> otherLocations;

		if (other instanceof ResourceImpl) {
			otherLocations = ((ResourceImpl) other).getContentURIs();
		} else {
			otherLocations = getLocations((Resource) other);
		}

		Collection<URI> overlap = retain(thisLocations.keySet(), otherLocations.keySet());

		for (URI uri : overlap) {
			String thisSha = thisLocations.get(uri);
			String otherSha = otherLocations.get(uri);
			if (thisSha == otherSha)
				return true;

			if (thisSha != null && otherSha != null) {
				if (thisSha.equals(otherSha))
					return true;
			}
		}

		return false;
	}

	public Map<URI, String> getContentURIs() {
		if (locations == null) {
			locations = ResourceUtils.getLocations(this);
		}
		return locations;
	}

	@Override
	public int hashCode() {
		return getContentURIs().hashCode();
	}

	@Override
	public InputStream getContent() {
		try {
			ContentCapability c = ResourceUtils.getContentCapability(this);
			URI url = c.url();
			return url.toURL()
				.openStream();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}

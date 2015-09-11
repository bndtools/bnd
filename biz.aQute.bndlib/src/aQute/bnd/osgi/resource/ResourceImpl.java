package aQute.bnd.osgi.resource;

import static aQute.bnd.osgi.resource.ResourceUtils.getLocations;
import static aQute.lib.collections.Logic.retain;

import java.net.URI;
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

import aQute.bnd.osgi.resource.ResourceUtils.IdentityCapability;
import aQute.bnd.version.Version;

class ResourceImpl implements Resource, Comparable<Resource> {

	private List<Capability>				allCapabilities;
	private Map<String,List<Capability>>	capabilityMap;

	private List<Requirement>				allRequirements;
	private Map<String,List<Requirement>>	requirementMap;
	private Map<URI,String>					locations;

	void setCapabilities(List<Capability> capabilities) {
		allCapabilities = capabilities;

		capabilityMap = new HashMap<String,List<Capability>>();
		for (Capability capability : capabilities) {
			List<Capability> list = capabilityMap.get(capability.getNamespace());
			if (list == null) {
				list = new LinkedList<Capability>();
				capabilityMap.put(capability.getNamespace(), list);
			}
			list.add(capability);
		}
	}

	public List<Capability> getCapabilities(String namespace) {
		List<Capability> caps = allCapabilities;
		if (namespace != null)
			caps = capabilityMap.get(namespace);
		if (caps == null || caps.isEmpty())
			return Collections.emptyList();

		return Collections.unmodifiableList(caps);
	}

	void setRequirements(List<Requirement> requirements) {
		allRequirements = requirements;

		requirementMap = new HashMap<String,List<Requirement>>();
		for (Requirement requirement : requirements) {
			List<Requirement> list = requirementMap.get(requirement.getNamespace());
			if (list == null) {
				list = new LinkedList<Requirement>();
				requirementMap.put(requirement.getNamespace(), list);
			}
			list.add(requirement);
		}
	}

	public List<Requirement> getRequirements(String namespace) {
		List<Requirement> reqs = allRequirements;
		if (namespace != null)
			reqs = requirementMap.get(namespace);
		if (reqs == null || reqs.isEmpty())
			return Collections.emptyList();

		return Collections.unmodifiableList(reqs);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		List<Capability> identities = getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
		if (identities != null && identities.size() == 1) {
			Capability idCap = identities.get(0);
			Object id = idCap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
			Object version = idCap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);

			builder.append(id).append(" version=").append(version);
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
		IdentityCapability me = ResourceUtils.getIdentityCapability(this);
		IdentityCapability them = ResourceUtils.getIdentityCapability(o);

		String myName = me.osgi_identity();
		String theirName = them.osgi_identity();
		if (myName == theirName)
			return 0;

		if (myName == null)
			return -1;

		if (theirName == null)
			return 1;

		int n = myName.compareTo(theirName);
		if (n != 0)
			return n;

		Version myVersion = me.version();
		Version theirVersion = them.version();

		if (myVersion == theirVersion)
			return 0;

		if (myVersion == null)
			return -1;

		if (theirVersion == null)
			return 1;

		return myVersion.compareTo(theirVersion);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;

		if (other == null || !(other instanceof Resource))
			return false;

		Map<URI,String> thisLocations = getContentURIs();
		Map<URI,String> otherLocations;

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

	public Map<URI,String> getContentURIs() {
		if (locations == null) {
			locations = ResourceUtils.getLocations(this);
		}
		return locations;
	}

	@Override
	public int hashCode() {
		return getContentURIs().hashCode();
	}
}

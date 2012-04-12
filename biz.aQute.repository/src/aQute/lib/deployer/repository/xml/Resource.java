package aQute.lib.deployer.repository.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.Constants;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Namespaces;
import org.osgi.service.indexer.Requirement;

import aQute.lib.collections.MultiMap;

/**
 * @immutable
 * @author Neil Bartlett
 */
public class Resource {
	
	private final String baseUrl;

	private final MultiMap<String, Capability> capabilities = new MultiMap<String, Capability>();
	private final MultiMap<String, Requirement> requires = new MultiMap<String, Requirement>();

	private Resource(String baseUrl, Collection<? extends Capability> capabilities, Collection<? extends Requirement> requires) {
		this.baseUrl = baseUrl;
		for (Capability capability : capabilities) {
			this.capabilities.add(capability.getNamespace(), capability);
		}
		for (Requirement requirement : requires) {
			this.requires.add(requirement.getNamespace(), requirement);
		}
	}
	
	public static class Builder {
		private String baseUrl = null;
		private final List<Capability> capabilities = new LinkedList<Capability>();
		private final List<Requirement> requires = new LinkedList<Requirement>();
		
		public Builder setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
			return this;
		}
		public Builder addCapability(Capability capability) {
			this.capabilities.add(capability);
			return this;
		}
		public Builder addRequirement(Requirement require) {
			this.requires.add(require);
			return this;
		}
		
		public Resource build() {
			return new Resource(baseUrl, Collections.unmodifiableList(capabilities), Collections.unmodifiableList(requires));
		}
	}
	
	public String getBaseUrl() {
		return baseUrl;
	}

	public List<Capability> getCapabilities(String namespace) {
		List<Capability> list = capabilities.get(namespace);
		return list != null ? Collections.unmodifiableList(list) : Collections.<Capability>emptyList();
	}

	public Capability findPackageCapability(String pkgName) {
		List<Capability> list = capabilities.get(Namespaces.NS_WIRING_PACKAGE);
		if (list != null) for (Capability capability : list) {
			if (pkgName.equals(capability.getAttributes().get(Namespaces.NS_WIRING_PACKAGE)))
				return capability;
		}
		return null;
	}
	
	/*
	public Requirement findPackageRequirement(String pkgName) {
		List<Requirement> list = requires.get(Namespaces.NS_WIRING_PACKAGE);
		if (list != null) for (Requirement requirement : list) {
			if (pkgName.equals(requirement).)
		}
	}
	*/
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Resource [capabilities=").append(capabilities)
				.append(", requirements=").append(requires)
				.append("]");
		return builder.toString();
	}

	public String getIdentity() {
		String bsn = null;
		List<Capability> list = capabilities.get(Namespaces.NS_IDENTITY);
		if (list != null && !list.isEmpty()) {
			bsn = (String) list.get(0).getAttributes().get(Namespaces.NS_IDENTITY);
		}
		return bsn;
	}

	public String getVersion() {
		String version = null;
		List<Capability> list = capabilities.get(Namespaces.NS_IDENTITY);
		if (list != null && !list.isEmpty()) {
			version = (String) list.get(0).getAttributes().get(Namespaces.ATTR_VERSION);
		}
		return version;
	}

	public String getContentUrl() {
		String url = null;
		List<Capability> list = capabilities.get(Namespaces.NS_CONTENT);
		if (list != null && !list.isEmpty())
			url = (String) list.get(0).getAttributes().get(Namespaces.ATTR_CONTENT_URL);
		return url;
	}

}

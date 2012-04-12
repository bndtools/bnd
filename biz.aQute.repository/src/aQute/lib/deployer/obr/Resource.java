package aQute.lib.deployer.obr;

import java.util.*;

/**
 * @immutable
 * @author Neil Bartlett
 */
public class Resource {
	
	private final String id;
	private final String presentationName;
	private final String symbolicName;
	private final String baseUrl;
	private final String url;
	private final String version;
	private final List<Capability> capabilities;
	private final List<Require> requires;

	private Resource(String id, String presentationName, String symbolicName, String baseUrl, String url, String version, List<Capability> capabilities, List<Require> requires) {
		this.id = id;
		this.presentationName = presentationName;
		this.symbolicName = symbolicName;
		this.baseUrl = baseUrl;
		this.url = url;
		this.version = version;
		
		this.capabilities = capabilities;
		this.requires = requires;
	}
	
	public static class Builder {
		private String id;
		private String presentationName;
		private String symbolicName;
		private String baseUrl;
		private String url;
		private String version;
		private final List<Capability> capabilities = new LinkedList<Capability>();
		private final List<Require> requires = new LinkedList<Require>();
		
		public Builder setId(String id) {
			this.id = id;
			return this;
		}
		public Builder setPresentationName(String presentationName) {
			this.presentationName = presentationName;
			return this;
		}
		public Builder setSymbolicName(String symbolicName) {
			this.symbolicName = symbolicName;
			return this;
		}
		public Builder setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
			return this;
		}
		public Builder setUrl(String url) {
			this.url = url;
			return this;
		}
		public Builder setVersion(String version) {
			this.version = version;
			return this;
		}
		public Builder addCapability(Capability capability) {
			this.capabilities.add(capability);
			return this;
		}
		public Builder addCapability(Capability.Builder capabilityBuilder) {
			this.capabilities.add(capabilityBuilder.build());
			return this;
		}
		public Builder addRequire(Require require) {
			this.requires.add(require);
			return this;
		}
		
		public Resource build() {
			if (id == null) throw new IllegalStateException("'id' field is not initialised");
			if (symbolicName == null) throw new IllegalStateException("'symbolicName' field is not initialised");
			if (url == null) throw new IllegalStateException("'url' field is not initialised");
			
			return new Resource(id, presentationName, symbolicName, baseUrl, url, version, Collections.unmodifiableList(capabilities), Collections.unmodifiableList(requires));
		}
	}

	public String getId() {
		return id;
	}

	public String getPresentationName() {
		return presentationName;
	}

	public String getSymbolicName() {
		return symbolicName;
	}
	
	public String getBaseUrl() {
		return baseUrl;
	}

	public String getUrl() {
		return url;
	}

	public String getVersion() {
		return version;
	}

	public List<Capability> getCapabilities() {
		return capabilities;
	}
	
	public Capability findPackageCapability(String pkgName) {
		for (Capability capability : capabilities) {
			if (CapabilityType.PACKAGE.getTypeName().equals(capability.getName())) {
				List<Property> props = capability.getProperties();
				for (Property prop : props) {
					if (Property.PACKAGE.equals(prop.getName())) {
						if (pkgName.equals(prop.getValue()))
							return capability;
						else
							break;
					}
				}
			}
		}
		return null;
	}


	
	public List<Require> getRequires() {
		return requires;
	}
	
	public Require findRequire(String name) {
		for (Require require : requires) {
			if (name.equals(require.getName()))
				return require;
		}
		return null;
	}
	
	public Require findPackageRequire(String usesPkgName) {
		String matchString = String.format("(package=%s)", usesPkgName);
		
		for (Require require : requires) {
			if (CapabilityType.PACKAGE.getTypeName().equals(require.getName())) {
				String filter = require.getFilter();
				if (filter.indexOf(matchString) > -1)
					return require;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Resource [id=").append(id)
				.append(", presentationName=").append(presentationName)
				.append(", symbolicName=").append(symbolicName)
				.append(", baseUrl=").append(baseUrl)
				.append(", url=").append(url).append(", version=")
				.append(version).append(", capabilities=").append(capabilities)
				.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((baseUrl == null) ? 0 : baseUrl.hashCode());
		result = prime * result
				+ ((capabilities == null) ? 0 : capabilities.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime
				* result
				+ ((presentationName == null) ? 0 : presentationName.hashCode());
		result = prime * result
				+ ((symbolicName == null) ? 0 : symbolicName.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Resource other = (Resource) obj;
		if (baseUrl == null) {
			if (other.baseUrl != null)
				return false;
		} else if (!baseUrl.equals(other.baseUrl))
			return false;
		if (capabilities == null) {
			if (other.capabilities != null)
				return false;
		} else if (!capabilities.equals(other.capabilities))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (presentationName == null) {
			if (other.presentationName != null)
				return false;
		} else if (!presentationName.equals(other.presentationName))
			return false;
		if (symbolicName == null) {
			if (other.symbolicName != null)
				return false;
		} else if (!symbolicName.equals(other.symbolicName))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

}

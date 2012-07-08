package aQute.lib.osgi.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

class CapReq implements Capability, Requirement {
	
	static enum MODE { Capability, Requirement }
	
	private final MODE mode;
	private final String	namespace;
	private final Resource	resource;
	private final Map<String,String>	directives;
	private final Map<String,Object>	attributes;

	CapReq(MODE mode, String namespace, Resource resource, Map<String, String> directives, Map<String, Object> attributes) {
		this.mode = mode;
		this.namespace = namespace;
		this.resource = resource;
		this.directives = new HashMap<String,String>(directives);
		this.attributes = new HashMap<String,Object>(attributes);
	}

	public String getNamespace() {
		return namespace;
	}

	public Map<String,String> getDirectives() {
		return Collections.unmodifiableMap(directives);
	}

	public Map<String,Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	public Resource getResource() {
		return resource;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + ((directives == null) ? 0 : directives.hashCode());
		result = prime * result + ((mode == null) ? 0 : mode.hashCode());
		result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
		result = prime * result + ((resource == null) ? 0 : resource.hashCode());
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
		CapReq other = (CapReq) obj;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (directives == null) {
			if (other.directives != null)
				return false;
		} else if (!directives.equals(other.directives))
			return false;
		if (mode != other.mode)
			return false;
		if (namespace == null) {
			if (other.namespace != null)
				return false;
		} else if (!namespace.equals(other.namespace))
			return false;
		if (resource == null) {
			if (other.resource != null)
				return false;
		} else if (!resource.equals(other.resource))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (mode == MODE.Capability) {
			Object value = attributes.get(namespace);
			builder.append(namespace).append('=').append(value);
		} else {
			String filter = directives.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
			builder.append(filter);
			if (Namespace.RESOLUTION_OPTIONAL.equals(directives.get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE))) {
				builder.append("%OPT");
			}
		}
		return builder.toString();
	}

}

package aQute.bnd.osgi.resource;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

class CapReq {

	enum MODE {
		Capability,
		Requirement
	}

	private final MODE					mode;
	private final String				namespace;
	private final Resource				resource;
	private final Map<String, String>	directives;
	private final Map<String, Object>	attributes;
	private transient int				hashCode	= 0;

	CapReq(MODE mode, String namespace, Resource resource, Map<String, String> directives,
		Map<String, Object> attributes) {
		this.mode = requireNonNull(mode);
		this.namespace = requireNonNull(namespace);
		this.resource = resource;
		this.directives = unmodifiableMap(new HashMap<>(directives));
		this.attributes = unmodifiableMap(new HashMap<>(attributes));
	}

	public String getNamespace() {
		return namespace;
	}

	public Map<String, String> getDirectives() {
		return directives;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public Resource getResource() {
		return resource;
	}

	@Override
	public int hashCode() {
		if (hashCode != 0) {
			return hashCode;
		}
		return hashCode = Objects.hash(attributes, directives, mode, namespace, resource);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof CapReq)
			return equalsNative((CapReq) obj);
		if ((mode == MODE.Capability) && (obj instanceof Capability))
			return equalsCap((Capability) obj);
		if ((mode == MODE.Requirement) && (obj instanceof Requirement))
			return equalsReq((Requirement) obj);
		return false;
	}

	private boolean equalsCap(Capability other) {
		if (!Objects.equals(namespace, other.getNamespace()))
			return false;
		if (!Objects.equals(attributes, other.getAttributes()))
			return false;
		if (!Objects.equals(directives, other.getDirectives()))
			return false;
		return Objects.equals(resource, other.getResource());
	}

	private boolean equalsNative(CapReq other) {
		if (mode != other.mode)
			return false;
		if (!Objects.equals(namespace, other.getNamespace()))
			return false;
		if (!Objects.equals(attributes, other.getAttributes()))
			return false;
		if (!Objects.equals(directives, other.getDirectives()))
			return false;
		return Objects.equals(resource, other.getResource());
	}

	private boolean equalsReq(Requirement other) {
		if (!Objects.equals(namespace, other.getNamespace()))
			return false;
		if (!Objects.equals(attributes, other.getAttributes()))
			return false;
		if (!Objects.equals(directives, other.getDirectives()))
			return false;
		return Objects.equals(resource, other.getResource());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (mode == MODE.Capability) {
			Object value = attributes.get(namespace);
			builder.append(namespace)
				.append('=')
				.append(value);
		} else {
			String filter = directives.get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
			builder.append(filter);
			if (Namespace.RESOLUTION_OPTIONAL.equals(directives.get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE))) {
				builder.append("%OPT");
			}
		}
		return builder.toString();
	}

	protected void toString(StringBuilder sb) {
		sb.append("[")
			.append(namespace)
			.append("]");
		sb.append(attributes);
		sb.append(directives);
	}

}

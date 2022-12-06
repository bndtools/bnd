package aQute.bnd.osgi.resource;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Objects;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import aQute.bnd.unmodifiable.Maps;

abstract class CapReq {
	private final String				namespace;
	private final Resource				resource;
	private final Map<String, String>	directives;
	private final Map<String, Object>	attributes;
	private transient int				hashCode	= 0;

	CapReq(String namespace, Resource resource, Map<String, String> directives, Map<String, Object> attributes) {
		this.namespace = requireNonNull(namespace);
		this.resource = resource;
		this.directives = Maps.copyOf(directives);
		this.attributes = new DeferredValueMap<>(Maps.copyOf(attributes));
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
		return hashCode = Objects.hash(attributes, directives, Boolean.valueOf(this instanceof Capability), namespace,
			resource);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (this instanceof Capability) {
			if (!(obj instanceof Capability other))
				return false;
			return Objects.equals(namespace, other.getNamespace()) && Objects.equals(attributes, other.getAttributes())
				&& Objects.equals(directives, other.getDirectives()) && Objects.equals(resource, other.getResource());
		} else {
			if (!(obj instanceof Requirement other))
				return false;
			return Objects.equals(namespace, other.getNamespace()) && Objects.equals(attributes, other.getAttributes())
				&& Objects.equals(directives, other.getDirectives()) && Objects.equals(resource, other.getResource());
		}
	}

	protected void toString(StringBuilder sb) {
		sb.append('[')
			.append(namespace)
			.append(']')
			.append(attributes)
			.append(directives);
	}
}

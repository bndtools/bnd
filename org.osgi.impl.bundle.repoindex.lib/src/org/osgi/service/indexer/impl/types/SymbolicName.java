package org.osgi.service.indexer.impl.types;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SymbolicName {

	private final String name;
	private final Map<String, String> attributes;

	public SymbolicName(String name, Map<String, String> attributes) {
		this.name = name;
		this.attributes = new HashMap<String, String>(attributes);
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		SymbolicName other = (SymbolicName) obj;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BSN [name=" + name + ", attributes=" + attributes + "]";
	}

}

package org.osgi.service.indexer;

import java.util.LinkedHashMap;
import java.util.Map;

public class Builder {

	private String namespace = null;
	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();
	private final Map<String, String> directives = new LinkedHashMap<String, String>();

	public Builder setNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public Builder addAttribute(String name, Object value) {
		attributes.put(name, value);
		return this;
	}

	public Builder addDirective(String name, String value) {
		directives.put(name, value);
		return this;
	}

	public Capability buildCapability() throws IllegalStateException {
		if (namespace == null)
			throw new IllegalStateException("Namespace not set");

		return new Capability(namespace, new LinkedHashMap<String, Object>(attributes), new LinkedHashMap<String, String>(directives));
	}

	public Requirement buildRequirement() throws IllegalStateException {
		if (namespace == null)
			throw new IllegalStateException("Namespace not set");

		return new Requirement(namespace, new LinkedHashMap<String, Object>(attributes), new LinkedHashMap<String, String>(directives));
	}

}

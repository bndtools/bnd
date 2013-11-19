package org.osgi.service.indexer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A container for attributes and directives under a certain namespace. Can
 * generate a capability and/or a requirement from the contained information.
 */
public final class Builder {
	/** the namespace */
	private String namespace = null;

	/** the attributes */
	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

	/** the directives */
	private final Map<String, String> directives = new LinkedHashMap<String, String>();

	/**
	 * @param namespace
	 *            the namespace to set
	 * @return this
	 */
	public Builder setNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	/**
	 * Add an attribute
	 * 
	 * @param name
	 *            attribute name
	 * @param value
	 *            attribute value
	 * @return this
	 */
	public Builder addAttribute(String name, Object value) {
		attributes.put(name, value);
		return this;
	}

	/**
	 * Add a directive
	 * 
	 * @param name
	 *            directive name
	 * @param value
	 *            directive value
	 * @return this
	 */
	public Builder addDirective(String name, String value) {
		directives.put(name, value);
		return this;
	}

	/**
	 * @return a new capability, constructed from the namespace, attributes and
	 *         directives
	 * @throws IllegalStateException
	 *             when the namespace isn't set
	 */
	public Capability buildCapability() throws IllegalStateException {
		if (namespace == null)
			throw new IllegalStateException("Namespace not set");

		return new Capability(namespace, new LinkedHashMap<String, Object>(attributes), new LinkedHashMap<String, String>(directives));
	}

	/**
	 * @return a new requirement, constructed from the namespace, attributes and
	 *         directives
	 * @throws IllegalStateException
	 *             when the namespace isn't set
	 */
	public Requirement buildRequirement() throws IllegalStateException {
		if (namespace == null)
			throw new IllegalStateException("Namespace not set");

		return new Requirement(namespace, new LinkedHashMap<String, Object>(attributes), new LinkedHashMap<String, String>(directives));
	}
}

package org.osgi.service.indexer;

import java.util.Collections;
import java.util.Map;

/**
 * A requirement
 */
public final class Requirement {
	/** the namespace */
	private final String namespace;

	/** the attributes */
	private final Map<String, Object> attributes;

	/** the directives */
	private final Map<String, String> directives;

	/**
	 * Constructor
	 * 
	 * @param namespace
	 *            the namespace
	 * @param attributes
	 *            the attributes
	 * @param directives
	 *            the directives
	 */
	Requirement(String namespace, Map<String, Object> attributes, Map<String, String> directives) {
		this.namespace = namespace;
		this.attributes = attributes;
		this.directives = directives;
	}

	/**
	 * @return the namespace
	 */
	public String getNamespace() {
		return namespace;
	}

	/**
	 * @return the attributes
	 */
	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	/**
	 * @return the directives
	 */
	public Map<String, String> getDirectives() {
		return Collections.unmodifiableMap(directives);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Requirement [namespace=").append(namespace).append(", attributes=").append(attributes).append(", directives=").append(directives).append("]");
		return builder.toString();
	}
}

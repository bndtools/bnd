package org.osgi.service.bindex;

import java.util.Collections;
import java.util.Map;

public class Requirement {

	private final String namespace;
	private final Map<String, Object> attributes;
	private final Map<String, String> directives;

	Requirement(String namespace, Map<String, Object> attributes, Map<String, String> directives) {
		this.namespace = namespace;
		this.attributes = attributes;
		this.directives = directives;
	}

	public String getNamespace() {
		return namespace;
	}

	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	public Map<String, String> getDirectives() {
		return Collections.unmodifiableMap(directives);
	}

}

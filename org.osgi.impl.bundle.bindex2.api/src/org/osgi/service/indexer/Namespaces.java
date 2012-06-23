package org.osgi.service.indexer;

public final class Namespaces {

	// Basic namespaces
	public static final String NS_IDENTITY = "osgi.identity";
	public static final String NS_CONTENT = "osgi.content";
	
	// Wiring namespaces
	public static final String NS_WIRING_PACKAGE = "osgi.wiring.package";
	public static final String NS_WIRING_BUNDLE = "osgi.wiring.bundle";
	public static final String NS_WIRING_HOST = "osgi.wiring.host";
	public static final String NS_WIRING_SERVICE = "osgi.wiring.service";
	public static final String NS_WIRING_EE = "osgi.wiring.ee";
	
	// Generic attributes
	public static final String ATTR_VERSION = "version";
	
	// Identity attributes
	public static final String ATTR_IDENTITY_TYPE = "type";
	
	// Resource types
	public static final String RESOURCE_TYPE_BUNDLE = "osgi.bundle";
	public static final String RESOURCE_TYPE_FRAGMENT = "osgi.fragment";
	
	// Content attributes
	public static final String ATTR_CONTENT_URL = "url";
	public static final String ATTR_CONTENT_SIZE = "size";
	public static final String ATTR_CONTENT_MIME = "mime";
	
	// Common directives
	public static final String DIRECTIVE_FILTER = "filter";
	public static final String DIRECTIVE_SINGLETON = "singleton";
	public static final String DIRECTIVE_EFFECTIVE = "effective";
	public static final String DIRECTIVE_MANDATORY = "mandatory";
	public static final String DIRECTIVE_USES = "uses";

	public static final String EFFECTIVE_RESOLVE = "resolve";
	public static final String EFFECTIVE_ACTIVE = "active";

}

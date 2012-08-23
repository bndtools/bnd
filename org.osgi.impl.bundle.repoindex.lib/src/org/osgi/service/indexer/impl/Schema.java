package org.osgi.service.indexer.impl;

public final class Schema {

	public static final Object XML_PROCESSING_INSTRUCTION = "<?xml version='1.0' encoding='utf-8'?>";
	public static final String NAMESPACE = "http://www.osgi.org/xmlns/repository/v1.0.0";

	public static final String ELEM_REPOSITORY = "repository";
	public static final String ELEM_RESOURCE = "resource";
	public static final String ELEM_CAPABILITY = "capability";
	public static final String ELEM_REQUIREMENT = "requirement";
	public static final String ELEM_ATTRIBUTE = "attribute";
	public static final String ELEM_DIRECTIVE = "directive";

	public static final String ATTR_XML_NAMESPACE = "xmlns";
	public static final String ATTR_NAME = "name";
	public static final String ATTR_INCREMENT = "increment";
	public static final String ATTR_NAMESPACE = "namespace";
	public static final String ATTR_TYPE = "type";
	public static final String ATTR_VALUE = "value";

	public static final String TYPE_VERSION = "Version";

}

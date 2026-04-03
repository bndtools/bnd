package org.bndtools.core.ui.wizards.service;

public interface ServiceTemplateConstants {
	String	TEMPLATE_SERVICE_API_TYPE			= "service-api";
	String	TEMPLATE_SERVICE_IMPL_TYPE		= "service-impl";
	String	TEMPLATE_SERVICE_CONSUMER_TYPE	= "service-consumer";

	String	DEFAULT_IMPL_SUFFIX				= ".impl";
	String	DEFAULT_CONSUMER_SUFFIX			= ".consumer";

	String	DEFAULT_API_CLASS_NAME_SUFFIX	= "Service";
	String	DEFAULT_IMPL_CLASS_NAME_SUFFIX		= "Impl";
	String	DEFAULT_CONSUMER_CLASS_NAME_SUFFIX	= "Consumer";

	static String getApiClassName(String serviceName) {
		return serviceName + DEFAULT_API_CLASS_NAME_SUFFIX;
	}

	static String getImplClassName(String serviceName) {
		return serviceName + DEFAULT_IMPL_CLASS_NAME_SUFFIX;
	}

	static String getConsumerClassName(String serviceName) {
		return serviceName + DEFAULT_CONSUMER_CLASS_NAME_SUFFIX;
	}

}

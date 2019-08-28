package bndtools.editor.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class ServiceComponentAttribs {
	// v1.0.0 attributes
	public final static String					COMPONENT_NAME					= "name:";
	public final static String					COMPONENT_FACTORY				= "factory:";
	public final static String					COMPONENT_SERVICEFACTORY		= "servicefactory:";
	public final static String					COMPONENT_IMMEDIATE				= "immediate:";
	public final static String					COMPONENT_ENABLED				= "enabled:";

	public final static String					COMPONENT_DYNAMIC				= "dynamic:";
	public final static String					COMPONENT_MULTIPLE				= "multiple:";
	public final static String					COMPONENT_PROVIDE				= "provide:";
	public final static String					COMPONENT_OPTIONAL				= "optional:";
	public final static String					COMPONENT_PROPERTIES			= "properties:";
	public final static String					COMPONENT_IMPLEMENTATION		= "implementation:";

	// v1.1.0 attributes
	public final static String					COMPONENT_VERSION				= "version:";
	public final static String					COMPONENT_CONFIGURATION_POLICY	= "configuration-policy:";
	public final static String					COMPONENT_MODIFIED				= "modified:";
	public final static String					COMPONENT_ACTIVATE				= "activate:";
	public final static String					COMPONENT_DEACTIVATE			= "deactivate:";

	private String								name							= null;
	private String								factory							= null;
	private Boolean								serviceFactory					= null;
	private Boolean								immediate						= null;
	private Boolean								enabled							= null;
	private List<String>						provide							= null;
	private String								implementation					= null;
	private ServiceComponentConfigurationPolicy	configurationPolicy				= null;
	private String								activate						= null;
	private String								deactivate						= null;
	private String								modified						= null;

	static ServiceComponentAttribs loadFrom(Map<String, String> map) {
		ServiceComponentAttribs result = new ServiceComponentAttribs();

		result.name = map.get(COMPONENT_NAME);
		result.factory = map.get(COMPONENT_FACTORY);
		result.serviceFactory = parseNullableBoolean(map.get(COMPONENT_SERVICEFACTORY));
		result.immediate = parseNullableBoolean(map.get(COMPONENT_IMMEDIATE));
		result.enabled = parseNullableBoolean(map.get(COMPONENT_ENABLED));
		result.provide = parseList(map.get(COMPONENT_PROVIDE));
		result.implementation = map.get(COMPONENT_IMPLEMENTATION);
		try {
			String configPolicyStr = map.get(COMPONENT_CONFIGURATION_POLICY);
			ServiceComponentConfigurationPolicy configPolicy = configPolicyStr != null
				? ServiceComponentConfigurationPolicy.valueOf(configPolicyStr)
				: null;
			result.configurationPolicy = configPolicy;
		} catch (IllegalArgumentException e) {
			result.configurationPolicy = null;
		}
		result.activate = map.get(COMPONENT_ACTIVATE);
		result.deactivate = map.get(COMPONENT_DEACTIVATE);
		result.modified = map.get(COMPONENT_MODIFIED);

		return result;
	}

	private static Boolean parseNullableBoolean(String string) {
		return string != null ? Boolean.parseBoolean(string) : null;
	}

	private static List<String> parseList(String string) {
		if (string == null)
			return null;

		List<String> result = new ArrayList<>();
		StringTokenizer tokenizer = new StringTokenizer(string, ",");
		while (tokenizer.hasMoreTokens()) {
			result.add(tokenizer.nextToken()
				.trim());
		}

		return result;
	}

	// Getters and Setters
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFactory() {
		return factory;
	}

	public void setFactory(String factory) {
		this.factory = factory;
	}

	public Boolean getServiceFactory() {
		return serviceFactory;
	}

	public void setServiceFactory(Boolean serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

	public Boolean getImmediate() {
		return immediate;
	}

	public void setImmediate(Boolean immediate) {
		this.immediate = immediate;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public List<String> getProvide() {
		return provide;
	}

	public void setProvide(List<String> provide) {
		this.provide = provide;
	}

	public String getImplementation() {
		return implementation;
	}

	public void setImplementation(String implementation) {
		this.implementation = implementation;
	}

	public ServiceComponentConfigurationPolicy getConfigurationPolicy() {
		return configurationPolicy;
	}

	public void setConfigurationPolicy(ServiceComponentConfigurationPolicy configurationPolicy) {
		this.configurationPolicy = configurationPolicy;
	}

	public String getActivate() {
		return activate;
	}

	public void setActivate(String activate) {
		this.activate = activate;
	}

	public String getDeactivate() {
		return deactivate;
	}

	public void setDeactivate(String deactivate) {
		this.deactivate = deactivate;
	}

	public String getModified() {
		return modified;
	}

	public void setModified(String modified) {
		this.modified = modified;
	}
}

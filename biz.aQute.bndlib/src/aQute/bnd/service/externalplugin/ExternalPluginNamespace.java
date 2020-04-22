package aQute.bnd.service.externalplugin;

import org.osgi.framework.Constants;

/**
 * External Plugins are executable code that the Workspace can execute. An
 * external plugin must define a Capability in the
 * {@link ExternalPluginNamespace}. This namespace defines the attributes:
 *
 * <pre>
 * bnd.external.plugin    name of the plugin
 * objectClass            the service type of the plugin
 * implementation         the implementation class
 * </pre>
 *
 * There is an annotation {@link ExternalPlugin} that can be applied to a
 * plugin.
 */
public interface ExternalPluginNamespace {

	/**
	 * Namespace name for external plugin capabilities and requirements.
	 */
	String	EXTERNAL_PLUGIN_NAMESPACE			= "bnd.external.plugin";

	/**
	 * The name of the external plugin
	 */
	String	CAPABILITY_NAME_ATTRIBUTE			= EXTERNAL_PLUGIN_NAMESPACE;

	/**
	 * The type of the interface under which it will be used
	 */
	String	CAPABILITY_OBJECTCLASS_ATTRIBUTE	= Constants.OBJECTCLASS;

	/**
	 * The type of the interface under which it will be used
	 */
	String	CAPABILITY_IMPLEMENTATION_ATTRIBUTE	= "implementation";

	/**
	 * An optional generic parameter to the objectClass type
	 */
	String	CAPABILITY_SUBTYPE_ATTRIBUTE		= "subtype";

	static String filter(String name, Class<?> type) {
		StringBuilder sb = new StringBuilder();
		sb.append("(&(")
			.append(NAME_A)
			.append("=")
			.append(name)
			.append(")(")
			.append(OBJECTCLASS_A)
			.append("=")
			.append(type.getName())
			.append(")(")
			.append(IMPLEMENTATION_A)
			.append("=*))");
		return sb.toString();
	}

	String	NAME				= EXTERNAL_PLUGIN_NAMESPACE;
	String	NAME_A				= CAPABILITY_NAME_ATTRIBUTE;
	String	IMPLEMENTATION_A	= CAPABILITY_IMPLEMENTATION_ATTRIBUTE;
	String	OBJECTCLASS_A		= CAPABILITY_OBJECTCLASS_ATTRIBUTE;


}

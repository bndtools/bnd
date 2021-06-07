package aQute.bnd.annotation.plugin;

/**
 * External Plugins are executable code that the Workspace can execute. An
 * external plugin must define a Capability in the
 * {@link InternalPluginNamespace}. This namespace defines the attributes:
 *
 * <pre>
 * bnd.external.plugin    name of the plugin
 * objectClass            the service type of the plugin
 * implementation         the implementation class
 * </pre>
 *
 * There is an annotation {@link BndPlugin} that can be applied to a plugin.
 */
public interface InternalPluginNamespace {

	/**
	 * Namespace name for external plugin capabilities and requirements.
	 */
	String	NAMESPACE			= "bnd.external.plugin";
	/**
	 * The name of the external plugin
	 */
	String	NAME_A				= NAMESPACE;
	/**
	 * The implementation class
	 */
	String	IMPLEMENTATION_A	= "implementation";
	/**
	 * The configuration interface
	 */
	String	PARAMETERS_A		= "parameters";
	/**
	 * Do not show as option in UI (normally for plugins added by the software
	 */

	String	HIDE_A				= "hide";

	static String filter(String name) {
		StringBuilder sb = new StringBuilder();
		sb.append("(&(")
			.append(NAME_A)
			.append("=")
			.append(name)
			.append(")(")
			.append(IMPLEMENTATION_A)
			.append("=*))");
		return sb.toString();
	}

}

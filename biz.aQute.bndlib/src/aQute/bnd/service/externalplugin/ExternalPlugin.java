package aQute.bnd.service.externalplugin;

import org.osgi.annotation.bundle.Attribute;
import org.osgi.annotation.bundle.Capability;

/**
 * Define a capability header for an External Plugin class.
 */
@Capability(namespace = ExternalPluginNamespace.EXTERNAL_PLUGIN_NAMESPACE, attribute = {
	ExternalPluginNamespace.CAPABILITY_IMPLEMENTATION_ATTRIBUTE + "=${@class}"
})
public @interface ExternalPlugin {
	/**
	 * The name of this plugin
	 */
	@Attribute(ExternalPluginNamespace.CAPABILITY_NAME_ATTRIBUTE)
	String name();

	/**
	 * The service/plugin type of the plugin
	 */
	@Attribute(ExternalPluginNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE)
	Class<?> objectClass();

	/**
	 * The version
	 *
	 * @return the version
	 */
	@Attribute(ExternalPluginNamespace.VERSION_ATTRIBUTE + ":Version")
	String version() default "0.0.0";

}

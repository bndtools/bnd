package aQute.bnd.service.externalplugin;

import org.osgi.annotation.bundle.Attribute;
import org.osgi.annotation.bundle.Capability;

@Capability(namespace = ExternalPluginNamespace.EXTERNAL_PLUGIN_NAMESPACE, attribute = {
	ExternalPluginNamespace.CAPABILITY_IMPLEMENTATION_ATTRIBUTE + "=${@class}"
})
public @interface ExternalPlugin {
	@Attribute(ExternalPluginNamespace.CAPABILITY_NAME_ATTRIBUTE)
	String name();

	@Attribute(ExternalPluginNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE)
	Class<?> objectClass();

}

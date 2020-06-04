package aQute.bnd.annotation.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Attribute;
import org.osgi.annotation.bundle.Capability;

@Capability(namespace = InternalPluginNamespace.NAMESPACE, attribute = {
	InternalPluginNamespace.IMPLEMENTATION_A + "=${@class}"
})
@Target({
	ElementType.TYPE
})
public @interface BndPlugin {

	@Attribute(InternalPluginNamespace.NAME_A)
	String name();

	@Attribute(InternalPluginNamespace.PARAMETERS_A)
	Class<?> parameters() default Object.class;

	@Attribute(InternalPluginNamespace.HIDE_A)
	boolean hide() default false;
}

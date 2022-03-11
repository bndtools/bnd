package aQute.bnd.annotation.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Capability;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Namespace;

/**
 * Adds a ProvideCapability for a service. This is useful to the resolver, when
 * a service is registered manually. It can annotate a class or a package. If it
 * annotates a class without specifying a value, the class will be named as the
 * registered service. Uses constraints will be calculated automatically.
 */
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.TYPE, ElementType.PACKAGE
})
@Capability(namespace = ServiceNamespace.SERVICE_NAMESPACE, //
	attribute = {
		ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE + ":List<String>=\"${uniq;${#value}}\"", //
		Namespace.CAPABILITY_USES_DIRECTIVE + ":=\"${uniq;${replace;${#value};(.*)\\.[^.]+;$1}}\""
	})
public @interface ServiceCapability {
	Class<?>[] value() default Target.class;
}
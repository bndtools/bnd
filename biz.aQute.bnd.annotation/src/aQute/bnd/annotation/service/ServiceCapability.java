package aQute.bnd.annotation.service;

import static aQute.bnd.annotation.spi.Constants.ATTRIBUTE_MACRO;
import static aQute.bnd.annotation.spi.Constants.SERVICE_MACRO;
import static org.osgi.resource.Namespace.EFFECTIVE_ACTIVE;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
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
@Repeatable(ServiceCapabilities.class)
@Capability(namespace = ServiceNamespace.SERVICE_NAMESPACE, //
	attribute = {
		SERVICE_MACRO, //
		Namespace.CAPABILITY_USES_DIRECTIVE
			+ ":=\"${if;${size;${#uses}};${#uses};${uniq;${replace;${#value};(.*)\\.[^.]+;$1}}}\"",
		ATTRIBUTE_MACRO
	}, effective = EFFECTIVE_ACTIVE)
public @interface ServiceCapability {

	/**
	 * The service <em>type</em>.
	 *
	 * @return the service type
	 */
	Class<?> value() default Target.class;

	/**
	 * A list of classes whose packages are inspected to calculate the
	 * {@code uses} directive for this capability.
	 * <p>
	 * If not specified, the {@code uses} directive is omitted from the
	 * capability clause.
	 */
	Class<?>[] uses() default {};

	/**
	 * A list of attribute or directive names and values.
	 * <p>
	 * Each string should be specified in the form:
	 * <ul>
	 * <li>{@code "name=value"} for attributes.</li>
	 * <li>{@code "name:type=value"} for typed attributes.</li>
	 * <li>{@code "name:=value"} for directives.</li>
	 * </ul>
	 * These are added, separated by semicolons, to the {@code osgi.service}
	 * capability. Non-standard {@code osgi.service} attributes will be included
	 * as service properties to the published service.
	 */
	String[] attribute() default {};
}
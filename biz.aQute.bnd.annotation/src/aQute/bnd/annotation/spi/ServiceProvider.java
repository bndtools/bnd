package aQute.bnd.annotation.spi;

import static aQute.bnd.annotation.Constants.EFFECTIVE_MACRO;
import static aQute.bnd.annotation.Constants.RESOLUTION_MACRO;
import static aQute.bnd.annotation.Constants.USES_MACRO;
import static aQute.bnd.annotation.spi.Constants.ATTRIBUTE_MACRO;
import static aQute.bnd.annotation.spi.Constants.REGISTER_MACRO;
import static aQute.bnd.annotation.spi.Constants.SERVICELOADER_REGISTRAR;
import static aQute.bnd.annotation.spi.Constants.SERVICELOADER_VERSION;
import static aQute.bnd.annotation.spi.Constants.SERVICE_MACRO;
import static aQute.bnd.annotation.spi.Constants.VALUE_MACRO;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;
import static org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;
import static org.osgi.resource.Namespace.EFFECTIVE_ACTIVE;
import static org.osgi.service.serviceloader.ServiceLoaderNamespace.SERVICELOADER_NAMESPACE;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Requirement;

import aQute.bnd.annotation.Resolution;

/**
 * Annotation used to generate requirements and capabilities necessary for
 * supporting the provider side of the <em>Service Loader Mediator</em>
 * specification.
 * <p>
 * Also results in the automatic generation of service descriptor files (a.k.a.
 * {@code META-INF/services}).
 *
 * @see <a href=
 *      "https://osgi.org/specification/osgi.cmpn/7.0.0/service.loader.html">Service
 *      Loader Mediator</a>
 */
@Retention(CLASS)
@Target({
	PACKAGE, TYPE
})
@Repeatable(ServiceProviders.class)
@Capability(name = VALUE_MACRO, namespace = SERVICELOADER_NAMESPACE, attribute = {
	REGISTER_MACRO, USES_MACRO, ATTRIBUTE_MACRO
})
@Capability(namespace = SERVICE_NAMESPACE, attribute = {
	SERVICE_MACRO, USES_MACRO, ATTRIBUTE_MACRO
}, effective = EFFECTIVE_ACTIVE)
@Requirement(name = SERVICELOADER_REGISTRAR, namespace = EXTENDER_NAMESPACE, version = SERVICELOADER_VERSION, attribute = {
	EFFECTIVE_MACRO, RESOLUTION_MACRO
})
public @interface ServiceProvider {
	/**
	 * The service <em>type</em>.
	 *
	 * @return the service type
	 */
	Class<?> value();

	/**
	 * The effective time of the {@code osgi.extender} requirement.
	 * <p>
	 * Specifies the time the {@code osgi.extender} requirements are available.
	 * The OSGi framework resolver only considers requirements without an
	 * effective directive or {@code effective:=resolve}. Requirements with
	 * other values for the effective directive can be considered by an external
	 * agent.
	 * <p>
	 * If not specified, the {@code effective} directive is omitted from the
	 * requirement clause.
	 */
	String effective() default "";

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
	 * These are added, separated by semicolons, to the
	 * {@code osgi.serviceloader} capability. Non-standard
	 * {@code osgi.serviceloader} attributes will be included as service
	 * properties to the published service.
	 */
	String[] attribute() default {};

	/**
	 * The resolution policy of the {@code osgi.extender} requirement.
	 * <p>
	 * A mandatory requirement forbids the bundle to resolve when this
	 * requirement is not satisfied; an optional requirement allows a bundle to
	 * resolve even if this requirement is not satisfied.
	 * <p>
	 * If not specified, the {@code resolution} directive is omitted from the
	 * requirement clause.
	 */
	Resolution resolution() default Resolution.DEFAULT;

	/**
	 * The type to register as the provider.
	 * <p>
	 * If the annotation used on a package, then {@link #register()} must be
	 * set. It is optional when used on a type using the type as the value.
	 * <p>
	 * The {@code register} directive is omitted from the requirement clause.
	 */
	Class<?> register() default Target.class;

}

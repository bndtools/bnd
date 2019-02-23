package aQute.bnd.annotation.spi;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

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
@Target(TYPE)
@Repeatable(ServiceProviders.class)
public @interface ServiceProvider {
	/**
	 * The service <em>type</em>.
	 *
	 * @return the service type
	 */
	Class<?> value();

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
}

package aQute.bnd.annotation.headers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define a Provide Capability clause in the manifest.
 * <p>
 * Since this annotation can only be applied once, it is possible to create an
 * annotation that models a specific capability. For example:
 *
 * <pre>
 * interface Webserver {
 * 	&#64;ProvideCapability(ns = "osgi.extender", name = "aQute.webserver", version = "${&#64;version}")
 * 	&#64;interface Provide {}
 *
 * 	&#64;RequireCapability(ns = "osgi.extender", filter = "(&amp;(osgi.extender=aQute.webserver)${frange;${&#64;version}})")
 * 	&#64;interface Require {}
 * }
 *
 * &#64;Webserver.Provide
 * public class MyWebserver {}
 * </pre>
 *
 * @deprecated see {@code org.osgi.annotation.bundle.Capability}
 */
@Deprecated
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.ANNOTATION_TYPE, ElementType.TYPE
})
public @interface ProvideCapability {
	/**
	 * Appended at the end of the clause (after a ';'). Can be used to add
	 * additional attributes and directives.
	 */
	String value() default "";

	/**
	 * The capability namespace. For example: {@code osgi.contract}.
	 */
	String ns();

	/**
	 * The name of the capability. If this is set, a property will be added as
	 * {ns}={name}. This is the custom pattern for OSGi namespaces. Leaving this
	 * unfilled, requires the {@link #value()} to be used to specify the name of
	 * the capability, if needed. For example {@code aQute.sse}.
	 */
	String name() default "";

	/**
	 * The version of the capability. This must be a valid OSGi version.
	 */
	String version() default "";

	/**
	 * Effective time. Specifies the time a capability is available, either
	 * resolve (default) or another name. The OSGi framework resolver only
	 * considers Capabilities without an effective directive or
	 * effective:=resolve. Capabilities with other values for the effective
	 * directive can be considered by an external agent.
	 */
	String effective() default "resolve";

	/**
	 * The uses directive lists package names that are used by this Capability.
	 * This information is intended to be used for <em>uses constraints</em>,
	 */
	String[] uses() default {};

	/**
	 * Mandatory attributes. Forces the resolver to only satisfy filters that
	 * refer to all listed attributes.
	 */
	String[] mandatory() default {};
}

package aQute.bnd.annotation.headers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * The Bundle’s Require-Capability header
 * </p>
 * <p>
 * Typically used as a meta-annotation, i.e. an annotation placed on another
 * annotation, which we will call the <em>user-defined annotation</em>. When the
 * user-defined annotation is found on a class within the bundle, an entry in
 * the <code>Require-Capability</code> header is added. The filter expression of
 * the requirement may be parameterised with values from the user-defined
 * annotation. For example, given the following declarations:
 * </p>
 *
 * <pre>
 * &#64;RequireCapability( ns = "com.acme.engine", effective = "active", filter = "(com.acme.engine=${type})")
 * public &#64;interface Engine { String type(); }
 * &#64;Engine(type = "wankel") public class Vehicle { ... }
 * </pre>
 * <p>
 * ... the following header will be generated in MANIFEST.MF:
 * </p>
 *
 * <pre>
 * Require-Capability:\ com.acme.engine; \ effective:=active; \
 *  filter:="(com.acme.engine=wankel)",\ ...
 * </pre>
 *
 * @deprecated see {@code org.osgi.annotation.bundle.Requirement}
 */
@Deprecated
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.ANNOTATION_TYPE, ElementType.TYPE
})
public @interface RequireCapability {

	String value() default "";

	String extra() default "";

	/**
	 * The capability namespace. For example: {@code osgi.contract}.
	 */
	String ns();

	/**
	 * Specifies the time a Requirement is considered, either 'resolve'
	 * (default) or another name. The OSGi framework resolver only considers
	 * Requirements without an effective directive or effective:=resolve. Other
	 * Requirements can be considered by an external agent. Additional names for
	 * the effective directive should be registered with the OSGi Alliance. See
	 * <a href="http://www.osgi.org/Specifications/Reference">OSGi Reference
	 * Page</a>
	 */
	String effective() default "resolve";

	/**
	 * A filter expression that is asserted on the Capabilities belonging to the
	 * given namespace. The matching of the filter against the Capability is
	 * done on one Capability at a time. A filter like {@code (&(a=1)(b=2))}
	 * matches only a Capability that specifies both attributes at the required
	 * value, not two capabilties that each specify one of the attributes
	 * correctly. A filter is optional, if no filter directive is specified the
	 * Requirement always matches.
	 */
	String filter() default "";

	/**
	 * A mandatory Requirement forbids the bundle to resolve when the
	 * Requirement is not satisfied; an optional Requirement allows a bundle to
	 * resolve even if the Requirement is not satisfied. No wirings are created
	 * when this Requirement cannot be resolved, this can result in Class Not
	 * Found Exceptions when the bundle attempts to use a package that was not
	 * resolved because it was optional.
	 *
	 * @deprecated
	 */
	aQute.bnd.annotation.headers.Resolution resolution() default aQute.bnd.annotation.headers.Resolution.mandatory;

}

package aQute.bnd.annotation.headers;

import java.lang.annotation.*;

/**
 * The Bundle’s Require-Capability header
 * 
 * {@link About}
 */
@Retention(RetentionPolicy.CLASS)
@Target({
		ElementType.ANNOTATION_TYPE, ElementType.TYPE
})
public @interface RequireCapability {
	String value() default "";

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
	String filter();

	/**
	 * A mandatory Requirement forbids the bundle to resolve when the
	 * Requirement is not satisfied; an optional Requirement allows a bundle to
	 * resolve even if the Requirement is not satisfied. No wirings are created
	 * when this Requirement cannot be resolved, this can result in Class Not
	 * Found Exceptions when the bundle attempts to use a package that was not
	 * resolved because it was optional.
	 */
	Resolution resolution() default Resolution.mandatory;

}

package aQute.bnd.annotation.cdi;

import static aQute.bnd.annotation.Constants.EFFECTIVE_DEFAULT;
import static aQute.bnd.annotation.Constants.EFFECTIVE_MACRO;
import static aQute.bnd.annotation.Constants.RESOLUTION_MACRO;
import static aQute.bnd.annotation.cdi.Constants.NAME_MACRO;
import static aQute.bnd.annotation.cdi.Constants.SERVICE_FILTER;
import static aQute.bnd.annotation.cdi.Constants.VERSION_MACRO;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;
import static org.osgi.annotation.bundle.Requirement.Resolution.OPTIONAL;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;
import static org.osgi.resource.Namespace.EFFECTIVE_ACTIVE;
import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Requirement;

import aQute.bnd.annotation.Resolution;

/**
 * Bundle annotation used to generate the requirements for a CDI Extension.
 */
@Retention(CLASS)
@Target({
	PACKAGE, TYPE
})
@Requirement(attribute = {
	EFFECTIVE_MACRO, RESOLUTION_MACRO
}, name = NAME_MACRO, namespace = CDI_EXTENSION_PROPERTY, version = VERSION_MACRO)
@Requirement(filter = SERVICE_FILTER, namespace = SERVICE_NAMESPACE, effective = EFFECTIVE_ACTIVE)
public @interface RequireCDIExtension {

	/**
	 * @return the extension name.
	 */
	String name();

	/**
	 * @return the extension version.
	 */
	String version() default "";

	/**
	 * The effective time of the {@code osgi.cdi.extension} and
	 * {@code osgi.extender} requirements.
	 * <p>
	 * Specifies the time the requirements are available. The OSGi framework
	 * resolver only considers requirements without an effective directive or
	 * {@code effective:=resolve}. Requirements with other values for the
	 * effective directive can be considered by an external agent.
	 * <p>
	 * If not specified, the {@code effective} directive is omitted from the
	 * requirement clause.
	 */
	String effective() default EFFECTIVE_DEFAULT;

	/**
	 * The resolution policy of the {@code osgi.cdi.extension} and
	 * {@code osgi.extender} requirements.
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
	 * Bundle annotation used to generate the requirements for a CDI Extension.
	 * <p>
	 * Requirements are defined in such a way that allows the library to resolve
	 * when the CDI Extension is not present at runtime while allowing
	 * deployment resolution. This offers the widest range of compatibility.
	 * <p>
	 * Generates the following requirements:
	 * <ul>
	 * <li>an {@code osgi.cdi.extension} <strong>requirement</strong> with
	 * {@link #name} and {@link #version} and preset effectiveness of
	 * {@code active}</li>
	 * <li>an {@code osgi.cdi.extension} <strong>requirement</strong> with
	 * {@link #name} and {@link #version} and preset resolution of
	 * {@code optional}</li>
	 * <li>an {@code osgi.service} <strong>requirement</strong> for type
	 * {@code javax.enterprise.inject.spi.Extension} with a preset effectiveness
	 * of {@code active}</li>
	 * </ul>
	 */
	@Retention(CLASS)
	@Target({
		PACKAGE, TYPE
	})
	@Requirement(name = NAME_MACRO, namespace = CDI_EXTENSION_PROPERTY, version = VERSION_MACRO, effective = EFFECTIVE_ACTIVE)
	@Requirement(name = NAME_MACRO, namespace = CDI_EXTENSION_PROPERTY, version = VERSION_MACRO, resolution = OPTIONAL)
	@Requirement(filter = SERVICE_FILTER, namespace = SERVICE_NAMESPACE, effective = EFFECTIVE_ACTIVE)
	static @interface Weak {

		/**
		 * @return the required extension name.
		 */
		String name();

		/**
		 * @return the required extension version.
		 */
		String version() default "";

	}

}

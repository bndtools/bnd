package aQute.bnd.annotation.cdi;

import static aQute.bnd.annotation.Constants.EFFECTIVE_DEFAULT;
import static aQute.bnd.annotation.Constants.EFFECTIVE_MACRO;
import static aQute.bnd.annotation.Constants.RESOLUTION_MACRO;
import static aQute.bnd.annotation.cdi.Constants.CDI_EXTENSION_PROPERTY_MACRO;
import static aQute.bnd.annotation.cdi.Constants.CDI_EXTENSION_TYPE;
import static aQute.bnd.annotation.cdi.Constants.NAME_MACRO;
import static aQute.bnd.annotation.cdi.Constants.SERVICE_ATTRIBUTE;
import static aQute.bnd.annotation.cdi.Constants.VERSION_MACRO;
import static aQute.bnd.annotation.spi.Constants.ATTRIBUTE_MACRO;
import static aQute.bnd.annotation.spi.Constants.REGISTER_MACRO;
import static aQute.bnd.annotation.spi.Constants.SERVICELOADER_REGISTRAR;
import static aQute.bnd.annotation.spi.Constants.SERVICELOADER_VERSION;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;
import static org.osgi.annotation.bundle.Requirement.Resolution.OPTIONAL;
import static org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE;
import static org.osgi.namespace.implementation.ImplementationNamespace.IMPLEMENTATION_NAMESPACE;
import static org.osgi.namespace.service.ServiceNamespace.SERVICE_NAMESPACE;
import static org.osgi.resource.Namespace.EFFECTIVE_ACTIVE;
import static org.osgi.service.cdi.CDIConstants.CDI_CAPABILITY_NAME;
import static org.osgi.service.cdi.CDIConstants.CDI_EXTENSION_PROPERTY;
import static org.osgi.service.cdi.CDIConstants.CDI_SPECIFICATION_VERSION;
import static org.osgi.service.serviceloader.ServiceLoaderNamespace.SERVICELOADER_NAMESPACE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Requirement;

import aQute.bnd.annotation.Resolution;

/**
 * Bundle annotation used to generate the requirements and capabilities
 * necessary when providing a CDI Extension.
 * <p>
 * Generates a capability for the {@code osgi.cdi.extension} namespace with the
 * value {@link #name} and a requirement for the
 * {@code osgi.implementation=osgi.cdi} capability.
 */
@Retention(CLASS)
@Target({
	PACKAGE, TYPE
})
@Capability(attribute = ATTRIBUTE_MACRO, name = NAME_MACRO, namespace = CDI_EXTENSION_PROPERTY, version = VERSION_MACRO)
@Requirement(attribute = {
	EFFECTIVE_MACRO, RESOLUTION_MACRO
}, name = CDI_CAPABILITY_NAME, namespace = IMPLEMENTATION_NAMESPACE, version = CDI_SPECIFICATION_VERSION)
public @interface ProvideCDIExtension {

	/**
	 * @return the extension name.
	 */
	String name();

	/**
	 * @return the extension version.
	 */
	String version();

	/**
	 * The effective time of the {@code osgi.implementation} requirement.
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
	 * The resolution policy of the {@code osgi.implementation} requirement.
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
	 * A list of attribute or directive names and values.
	 * <p>
	 * Each string should be specified in the form:
	 * <ul>
	 * <li>{@code "name=value"} for attributes.</li>
	 * <li>{@code "name:type=value"} for typed attributes.</li>
	 * <li>{@code "name:=value"} for directives.</li>
	 * </ul>
	 * These are added, separated by semicolons, to the
	 * {@code osgi.cdi.extension} capability.
	 */
	String[] attribute() default {};

	/**
	 * Bundle annotation used to generate the requirements and capabilities
	 * necessary when providing a CDI Extension.
	 * <p>
	 * Requirements are defined in such a way that allows the library to resolve
	 * when OSGi CDI implementation is not present at runtime while allowing
	 * deployment resolution to resolve both. This offers the widest range of
	 * compatibility.
	 * <p>
	 * Generates the following requirements and capabilities:
	 * <ul>
	 * <li>a {@code osgi.cdi.extension} <strong>capability</strong> with
	 * {@link #name} and {@link #version}</li>
	 * <li>an {@code osgi.implementation} <strong>requirement</strong> for
	 * {@code osgi.cdi} with preset effectiveness of {@code active}</li>
	 * <li>an {@code osgi.implementation} <strong>requirement</strong> for
	 * {@code osgi.cdi} with preset resolution of {@code optional}</li>
	 * </ul>
	 */
	@Retention(CLASS)
	@Target({
		PACKAGE, TYPE
	})
	@Capability(attribute = ATTRIBUTE_MACRO, name = NAME_MACRO, namespace = CDI_EXTENSION_PROPERTY, version = VERSION_MACRO)
	@Requirement(name = CDI_CAPABILITY_NAME, namespace = IMPLEMENTATION_NAMESPACE, version = CDI_SPECIFICATION_VERSION, effective = EFFECTIVE_ACTIVE)
	@Requirement(name = CDI_CAPABILITY_NAME, namespace = IMPLEMENTATION_NAMESPACE, version = CDI_SPECIFICATION_VERSION, resolution = OPTIONAL)
	static @interface Weak {

		/**
		 * @return the required extension name.
		 */
		String name();

		/**
		 * @return the required extension version.
		 */
		String version();

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
		 * {@code osgi.cdi.extension} capability.
		 */
		String[] attribute() default {};

	}

	/**
	 * Bundle annotation used to generate the requirements and capabilities
	 * necessary when providing a CDI Extension using Java's SPI mechanism. OSGi
	 * integration is provided by Service Loader Mediator.
	 * <p>
	 * Generates the following requirements and capabilities:
	 * <ul>
	 * <li>an {@code osgi.service} <strong>capability</strong> for type
	 * {@code javax.enterprise.inject.spi.Extension}</li>
	 * <li>a {@code osgi.cdi.extension} <strong>capability</strong> with
	 * {@link #name} and {@link #version}</li>
	 * <li>an {@code osgi.serviceloader} <strong>capability</strong> for type
	 * {@code javax.enterprise.inject.spi.Extension} with attribute
	 * {@code osgi.cdi.extension=}{@link #name}</li>
	 * <li>an {@code osgi.extender} <strong>requirement</strong> for
	 * {@code osgi.serviceloader.registrar}</li>
	 * <li>an {@code osgi.implementation} <strong>requirement</strong> for
	 * {@code osgi.cdi}</li>
	 * </ul>
	 */
	@Retention(CLASS)
	@Target({
		PACKAGE, TYPE
	})
	@Capability(attribute = SERVICE_ATTRIBUTE, namespace = SERVICE_NAMESPACE)
	@Capability(attribute = ATTRIBUTE_MACRO, name = NAME_MACRO, namespace = CDI_EXTENSION_PROPERTY, version = VERSION_MACRO)
	@Capability(attribute = {
		CDI_EXTENSION_PROPERTY_MACRO, REGISTER_MACRO, ATTRIBUTE_MACRO
	}, name = CDI_EXTENSION_TYPE, namespace = SERVICELOADER_NAMESPACE)
	@Requirement(attribute = {
		EFFECTIVE_MACRO, RESOLUTION_MACRO
	}, name = SERVICELOADER_REGISTRAR, namespace = EXTENDER_NAMESPACE, version = SERVICELOADER_VERSION)
	@Requirement(attribute = {
		EFFECTIVE_MACRO, RESOLUTION_MACRO
	}, name = CDI_CAPABILITY_NAME, namespace = IMPLEMENTATION_NAMESPACE, version = CDI_SPECIFICATION_VERSION)
	static @interface SPI {

		/**
		 * @return the required extension name.
		 */
		String name();

		/**
		 * @return the required extension version.
		 */
		String version();

		/**
		 * The effective time of the {@code osgi.extender} and
		 * {@code osgi.implementation} requirements.
		 * <p>
		 * Specifies the time the {@code osgi.extender} and
		 * {@code osgi.implementation} requirements are available. The OSGi
		 * framework resolver only considers requirements without an effective
		 * directive or {@code effective:=resolve}. Requirements with other
		 * values for the effective directive can be considered by an external
		 * agent.
		 * <p>
		 * If not specified, the {@code effective} directive is omitted from the
		 * requirement clause.
		 */
		String effective() default EFFECTIVE_DEFAULT;

		/**
		 * The resolution policy of the {@code osgi.extender} and
		 * {@code osgi.implementation} requirements.
		 * <p>
		 * A mandatory requirement forbids the bundle to resolve when this
		 * requirement is not satisfied; an optional requirement allows a bundle
		 * to resolve even if this requirement is not satisfied.
		 * <p>
		 * If not specified, the {@code resolution} directive is omitted from
		 * the requirement clause.
		 */
		Resolution resolution() default Resolution.DEFAULT;

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
		 * {@code osgi.cdi.extension} and {@code osgi.serviceloader}
		 * capabilities. Non-standard {@code osgi.serviceloader} attributes will
		 * be included as service properties to the published service.
		 */
		String[] attribute() default {};

	}

	/**
	 * Bundle annotation used to generate the requirements and capabilities
	 * necessary when providing a CDI Extension using Java's SPI mechanism. OSGi
	 * integration is provided by Service Loader Mediator.
	 * <p>
	 * Requirements are defined in such a way that allows the library to resolve
	 * when Service Loader Mediator and OSGi CDI implementations are not present
	 * at runtime while allowing deployment resolution to resolve both. This
	 * offers the widest range of compatibility.
	 * <p>
	 * Generates the following requirements and capabilities:
	 * <ul>
	 * <li>an {@code osgi.service} <strong>capability</strong> for type
	 * {@code javax.enterprise.inject.spi.Extension}</li>
	 * <li>a {@code osgi.cdi.extension} <strong>capability</strong> with
	 * {@link #name} and {@link #version}</li>
	 * <li>an {@code osgi.serviceloader} <strong>capability</strong> for type
	 * {@code javax.enterprise.inject.spi.Extension} with attribute
	 * {@code osgi.cdi.extension=}{@link #name}</li>
	 * <li>an {@code osgi.extender} <strong>requirement</strong> for
	 * {@code osgi.serviceloader.registrar} with preset effectiveness of
	 * {@code active}</li>
	 * <li>an {@code osgi.extender} <strong>requirement</strong> for
	 * {@code osgi.serviceloader.registrar} with preset resolution of
	 * {@code optional}</li>
	 * <li>an {@code osgi.implementation} <strong>requirement</strong> for
	 * {@code osgi.cdi} with preset effectiveness of {@code active}</li>
	 * <li>an {@code osgi.implementation} <strong>requirement</strong> for
	 * {@code osgi.cdi} with preset resolution of {@code optional}</li>
	 * </ul>
	 */
	@Retention(CLASS)
	@Target({
		PACKAGE, TYPE
	})
	@Capability(attribute = SERVICE_ATTRIBUTE, namespace = SERVICE_NAMESPACE)
	@Capability(attribute = ATTRIBUTE_MACRO, name = NAME_MACRO, namespace = CDI_EXTENSION_PROPERTY, version = VERSION_MACRO)
	@Capability(attribute = {
		CDI_EXTENSION_PROPERTY_MACRO, REGISTER_MACRO, ATTRIBUTE_MACRO
	}, name = CDI_EXTENSION_TYPE, namespace = SERVICELOADER_NAMESPACE)
	@Requirement(name = SERVICELOADER_REGISTRAR, namespace = EXTENDER_NAMESPACE, version = SERVICELOADER_VERSION, effective = EFFECTIVE_ACTIVE)
	@Requirement(name = SERVICELOADER_REGISTRAR, namespace = EXTENDER_NAMESPACE, version = SERVICELOADER_VERSION, resolution = OPTIONAL)
	@Requirement(name = CDI_CAPABILITY_NAME, namespace = IMPLEMENTATION_NAMESPACE, version = CDI_SPECIFICATION_VERSION, effective = EFFECTIVE_ACTIVE)
	@Requirement(name = CDI_CAPABILITY_NAME, namespace = IMPLEMENTATION_NAMESPACE, version = CDI_SPECIFICATION_VERSION, resolution = OPTIONAL)
	static @interface SPIWeak {

		/**
		 * @return the required extension name.
		 */
		String name();

		/**
		 * @return the required extension version.
		 */
		String version();

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
		 * {@code osgi.cdi.extension} and {@code osgi.serviceloader}
		 * capabilities. Non-standard {@code osgi.serviceloader} attributes will
		 * be included as service properties to the published service.
		 */
		String[] attribute() default {};

	}

}

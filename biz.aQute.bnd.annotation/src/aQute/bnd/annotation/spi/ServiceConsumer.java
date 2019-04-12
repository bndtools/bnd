package aQute.bnd.annotation.spi;

import static aQute.bnd.annotation.Constants.CARDINALITY_MACRO;
import static aQute.bnd.annotation.Constants.RESOLUTION_MACRO;
import static aQute.bnd.annotation.spi.Constants.SERVICELOADER_PROCESSOR;
import static aQute.bnd.annotation.spi.Constants.SERVICELOADER_VERSION;
import static aQute.bnd.annotation.spi.Constants.VALUE_MACRO;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;
import static org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE;
import static org.osgi.service.serviceloader.ServiceLoaderNamespace.SERVICELOADER_NAMESPACE;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Directive;
import org.osgi.annotation.bundle.Requirement;

import aQute.bnd.annotation.Cardinality;
import aQute.bnd.annotation.Resolution;

/**
 * Annotation used to generate requirements necessary for supporting the
 * consumer side of the <em>Service Loader Mediator</em> specification.
 *
 * @see <a href=
 *      "https://osgi.org/specification/osgi.cmpn/7.0.0/service.loader.html">Service
 *      Loader Mediator</a>
 */
@Repeatable(ServiceConsumers.class)
@Retention(CLASS)
@Target({
	PACKAGE, TYPE
})
@Requirement(name = VALUE_MACRO, namespace = SERVICELOADER_NAMESPACE, attribute = {
	SERVICELOADER_NAMESPACE + "=" + VALUE_MACRO, CARDINALITY_MACRO, RESOLUTION_MACRO
})
@Requirement(name = SERVICELOADER_PROCESSOR, namespace = EXTENDER_NAMESPACE, version = SERVICELOADER_VERSION, attribute = {
	RESOLUTION_MACRO
})
public @interface ServiceConsumer {
	/**
	 * The service <em>type</em>.
	 *
	 * @return the service type
	 */
	Class<?> value();

	/**
	 * The effective time of the {@code osgi.serviceloader} and
	 * {@code osgi.extender} requirements.
	 * <p>
	 * Specifies the time the service loader requirements are available. The
	 * OSGi framework resolver only considers requirements without an effective
	 * directive or {@code effective:=resolve}. Requirements with other values
	 * for the effective directive can be considered by an external agent.
	 * <p>
	 * If not specified, the {@code effective} directive is omitted from the
	 * requirement clause.
	 */
	@Directive
	String effective() default "";

	/**
	 * The cardinality of this requirement.
	 * <p>
	 * Indicates if this requirement can be wired a single time or multiple
	 * times.
	 * <p>
	 * If not specified, the {@code cardinality} directive is omitted from the
	 * requirement clause.
	 */
	Cardinality cardinality() default Cardinality.DEFAULT;

	/**
	 * The resolution policy of the {@code osgi.serviceloader} and
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

}

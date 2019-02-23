package aQute.bnd.annotation.spi;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

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
public @interface ServiceConsumer {
	/**
	 * The service <em>type</em>.
	 *
	 * @return the service type
	 */
	Class<?> value();

	/**
	 * The effective time of the {@code serviceloader} requirements.
	 * <p>
	 * Specifies the time the service loader requirements are available. The
	 * OSGi framework resolver only considers requirements without an effective
	 * directive or effective:=resolve. Requirements with other values for the
	 * effective directive can be considered by an external agent.
	 * <p>
	 * If not specified, the {@code effective} directive is omitted from the
	 * requirement clause.
	 */
	String effective() default "resolve"; // Namespace.EFFECTIVE_RESOLVE

	/**
	 * The cardinality of this requirement.
	 * <p>
	 * Indicates if this requirement can be wired a single time or multiple
	 * times.
	 * <p>
	 * If not specified, the {@code cardinality} directive is omitted from the
	 * requirement clause.
	 */
	Cardinality cardinality() default Cardinality.SINGLE;

	/**
	 * Cardinality for this requirement.
	 */
	public enum Cardinality {
		/**
		 * Indicates if the requirement can only be wired a single time.
		 */
		SINGLE("single"), // Namespace.CARDINALITY_SINGLE

		/**
		 * Indicates if the requirement can be wired multiple times.
		 */
		MULTIPLE("multiple"); // Namespace.CARDINALITY_MULTIPLE

		private final String value;

		Cardinality(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	/**
	 * The resolution policy of this requirement.
	 * <p>
	 * A mandatory requirement forbids the bundle to resolve when this
	 * requirement is not satisfied; an optional requirement allows a bundle to
	 * resolve even if this requirement is not satisfied.
	 * <p>
	 * If not specified, the {@code resolution} directive is omitted from the
	 * requirement clause.
	 */
	Resolution resolution() default Resolution.MANDATORY;

	/**
	 * Resolution for this requirement.
	 */
	public enum Resolution {
		/**
		 * A mandatory requirement forbids the bundle to resolve when the
		 * requirement is not satisfied.
		 */
		MANDATORY("mandatory"), // Namespace.RESOLUTION_MANDATORY

		/**
		 * An optional requirement allows a bundle to resolve even if the
		 * requirement is not satisfied.
		 */
		OPTIONAL("optional"); // Namespace.RESOLUTION_OPTIONAL

		private final String value;

		Resolution(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}
}

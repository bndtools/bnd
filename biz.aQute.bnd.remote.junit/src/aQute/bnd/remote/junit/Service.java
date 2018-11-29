package aQute.bnd.remote.junit;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({
	FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR
})
public @interface Service {

	/**
	 * Timeout in milliseconds
	 */
	long timeout() default 0;

	/**
	 * Target filter
	 */
	String filter() default "";

	/**
	 * The service type
	 */
	Class<?> type() default Object.class;
}

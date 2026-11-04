package aQute.bnd.mcp;

import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Documents a component of a tool input record. The text becomes the JSON
 * schema description an agent sees.
 */
@Retention(RUNTIME)
@Target(RECORD_COMPONENT)
public @interface Param {
	String value();

	boolean required() default false;
}

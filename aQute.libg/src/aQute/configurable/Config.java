package aQute.configurable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Config {
	String NULL = "<<NULL>>";

	boolean required() default false;

	String description() default "";

	String deflt() default NULL;

	String id() default NULL;
}

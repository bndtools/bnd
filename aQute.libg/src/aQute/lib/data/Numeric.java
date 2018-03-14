package aQute.lib.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = {
	ElementType.FIELD
})
public @interface Numeric {
	long min() default Long.MIN_VALUE;

	long max() default Long.MAX_VALUE;

	String reason() default "";
}

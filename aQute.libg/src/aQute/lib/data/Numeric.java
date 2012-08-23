package aQute.lib.data;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = {
	ElementType.FIELD
})
public @interface Numeric {
	long min() default Long.MIN_VALUE;

	long max() default Long.MAX_VALUE;

	String reason() default "";
}

package aQute.lib.data;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = {
	ElementType.FIELD
})
public @interface Validator {
	String value();

	String reason() default "";
}

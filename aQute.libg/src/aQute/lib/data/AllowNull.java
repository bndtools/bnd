package aQute.lib.data;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = {
	ElementType.FIELD
})
public @interface AllowNull {
	String reason() default "";
}

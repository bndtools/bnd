package aQute.bnd.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PACKAGE)
public @interface Export {
	String	MANDATORY	= "mandatory";
	String	OPTIONAL	= "optional";
	String	USES		= "uses";
	String	EXCLUDE		= "exclude";
	String	INCLUDE		= "include";

	String[] mandatory() default "";

	String[] optional() default "";

	Class<?>[] exclude() default Object.class;

	Class<?>[] include() default Object.class;
}

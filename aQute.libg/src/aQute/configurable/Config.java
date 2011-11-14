package aQute.configurable;

import java.lang.annotation.*;

@Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME) 
public @interface Config {
	String NULL = "<<NULL>>";
	
	boolean required() default false;
	String description() default "";
	String deflt() default NULL;
	String id() default NULL;
}

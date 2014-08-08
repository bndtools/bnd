package aQute.bnd.annotation.plugin;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({
		ElementType.TYPE
})
public @interface BndPlugin {
	
	String name();
	Class<?> parameters() default Object.class; 
}

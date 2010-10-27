package aQute.bnd.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.PACKAGE})
public @interface Version {
	String value();
}

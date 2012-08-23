package aQute.bnd.annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.PACKAGE
})
public @interface Version {
	String value();
}

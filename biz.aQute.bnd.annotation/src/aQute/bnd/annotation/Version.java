package aQute.bnd.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Deprecated because made superfluous by OSGi annotations
 */
@Deprecated
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.PACKAGE
})
public @interface Version {
	String value();
}

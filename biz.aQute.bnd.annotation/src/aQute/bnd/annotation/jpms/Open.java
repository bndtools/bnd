package aQute.bnd.annotation.jpms;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation used on a package to declare it is open in terms of JPMS
 * reflective access. This information is added to the
 * {@code module-info.class}.
 */
@Documented
@Retention(CLASS)
@Target(PACKAGE)
public @interface Open {

	/*
	 * The set of modules to which the package is open. The default is an empty
	 * array which means the package is open to every module.
	 */
	String[] value() default {};

}

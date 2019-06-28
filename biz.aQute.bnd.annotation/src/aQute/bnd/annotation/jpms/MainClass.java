package aQute.bnd.annotation.jpms;

import static aQute.bnd.annotation.jpms.Constants.CLASS_MACRO;
import static aQute.bnd.annotation.jpms.Constants.MAIN_CLASS;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Header;

/**
 * Annotation used on a type to indicate that it is the {@code Main-Class} of
 * the application resulting in the manifest header. This information is added
 * to the {@code module-info.class}.
 */
@Documented
@Retention(CLASS)
@Target(TYPE)
@Header(name = MAIN_CLASS, value = CLASS_MACRO)
public @interface MainClass {}

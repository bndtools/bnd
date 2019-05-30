package aQute.bnd.annotation.jpms;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Export;

/**
 * Annotation used on a package, in conjunction with the {@link Export}
 * annotation, to express the JPMS modules to which this package is exported.
 * This information is added to the {@code module-info.class}.
 * <p>
 * The exclusion of this annotation means the package is exported to all
 * modules.
 */
@Documented
@Retention(CLASS)
@Target(PACKAGE)
public @interface ExportTo {

	/*
	 * The set of modules to which the package is exported.
	 */
	String[] value();

}

package aQute.bnd.annotation.licenses;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import aQute.bnd.annotation.headers.BundleLicense;

/**
 * An annotation to indicate that the type depends on the Simplified BSD or
 * FreeBSD License. Applying this annotation will add a Bundle-License clause.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.PACKAGE, ElementType.TYPE
})
@BundleLicense(name = "BSD-2-Clause", link = "https://opensource.org/licenses/BSD-2-Clause", description = "BSD 2-Clause \"Simplified\" License")
public @interface BSD_2_Clause {}

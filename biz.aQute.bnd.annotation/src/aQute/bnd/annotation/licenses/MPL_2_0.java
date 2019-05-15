package aQute.bnd.annotation.licenses;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import aQute.bnd.annotation.headers.BundleLicense;

/**
 * An annotation to indicate that the type depends on the Mozilla Public License
 * 2.0. Applying this annotation will add a Bundle-License clause.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.PACKAGE, ElementType.TYPE
})
@BundleLicense(name = "MPL-2.0", link = "https://opensource.org/licenses/MPL-2.0", description = "Mozilla Public License 2.0")
public @interface MPL_2_0 {}

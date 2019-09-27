package aQute.bnd.annotation.licenses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import aQute.bnd.annotation.headers.BundleLicense;

/**
 * An annotation to indicate that the type depends on the GNU Lesser Public
 * License v2.1. Applying this annotation will add a Bundle-License clause.
 *
 * @deprecated Replaced by {@link LGPL_2_1_only} or {@link LGPL_2_1_or_later}.
 */
@Deprecated
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.PACKAGE, ElementType.TYPE
})
@BundleLicense(name = "LGPL-2.1", link = "https://opensource.org/licenses/LGPL-2.1", description = "GNU Lesser General Public License v2.1")
public @interface LGPL_2_1 {}

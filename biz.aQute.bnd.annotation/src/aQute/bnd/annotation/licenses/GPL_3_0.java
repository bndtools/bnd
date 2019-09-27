package aQute.bnd.annotation.licenses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import aQute.bnd.annotation.headers.BundleLicense;

/**
 * An annotation to indicate that the type depends on the GNU General Public
 * License v3.0. Applying this annotation will add a Bundle-License clause.
 *
 * @deprecated Replaced by {@link GPL_3_0_only} or {@link GPL_3_0_or_later}.
 */
@Deprecated
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.PACKAGE, ElementType.TYPE
})
@BundleLicense(name = "GPL-3.0", link = "https://opensource.org/licenses/GPL-3.0", description = "GNU General Public License v3.0")
public @interface GPL_3_0 {}

package aQute.bnd.annotation.licenses;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import aQute.bnd.annotation.headers.BundleLicense;

/**
 * An annotation to indicate that the type depends on the GNU General Public
 * License v2.0 or later. Applying this annotation will add a Bundle-License
 * clause.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.PACKAGE, ElementType.TYPE
})
@BundleLicense(name = "GPL-2.0-or-later", link = "https://opensource.org/licenses/GPL-2.0", description = "GNU General Public License v2.0 or later")
public @interface GPL_2_0_or_later {}

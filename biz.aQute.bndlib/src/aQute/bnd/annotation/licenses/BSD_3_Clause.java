package aQute.bnd.annotation.licenses;

import aQute.bnd.annotation.headers.*;

/**
 * An annotation to indicate that the type depends on the Revised BSD License.
 * Applying this annotation will add a Bundle-License clause.
 */

@BundleLicense(name = "http://opensource.org/licenses/BSD-3-Clause", link = "http://en.wikipedia.org/wiki/BSD_licenses", description = "Revised BSD License")
public @interface BSD_3_Clause {}

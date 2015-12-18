package aQute.bnd.annotation.licenses;

import aQute.bnd.annotation.headers.BundleLicense;

/**
 * An annotation to indicate that the type depends on the MIT License. Applying
 * this annotation will add a Bundle-License clause.
 */
@BundleLicense(name = "http://opensource.org/licenses/MIT", link = "http://en.wikipedia.org/wiki/MIT_License", description = "MIT License")
public @interface MIT_1_0 {}

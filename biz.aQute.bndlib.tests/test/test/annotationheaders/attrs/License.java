package test.annotationheaders.attrs;

import aQute.bnd.annotation.headers.BundleLicense;

@BundleLicense(name = "license")
public @interface License {
	String foo();
}

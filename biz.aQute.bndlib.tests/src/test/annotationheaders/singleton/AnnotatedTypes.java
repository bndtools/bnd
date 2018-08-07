package test.annotationheaders.singleton;

import aQute.bnd.annotation.headers.BundleHeader;

public class AnnotatedTypes {

	@BundleHeader(name = "Singleton-Bundle-Header", singleton = true)
	public static @interface SingletonBundleHeader {}

	@SingletonBundleHeader
	public static class A {}

	@SingletonBundleHeader
	public static class B {}

}

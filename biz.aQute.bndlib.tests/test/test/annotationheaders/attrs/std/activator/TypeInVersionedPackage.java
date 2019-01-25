package test.annotationheaders.attrs.std.activator;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class TypeInVersionedPackage implements BundleActivator {
	@Override
	public void start(BundleContext context) throws Exception {}

	@Override
	public void stop(BundleContext context) throws Exception {}
}

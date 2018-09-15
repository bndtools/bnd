package test.activator;

import org.osgi.framework.BundleContext;

public class MissingNoArgsConstructorActivator extends AbstractActivator {

	public MissingNoArgsConstructorActivator(String s) {}

	@Override
	public void start(BundleContext context) throws Exception {}

	@Override
	public void stop(BundleContext context) throws Exception {}

}

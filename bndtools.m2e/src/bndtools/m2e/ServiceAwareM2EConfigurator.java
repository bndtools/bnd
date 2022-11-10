package bndtools.m2e;

import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public abstract class ServiceAwareM2EConfigurator extends AbstractProjectConfigurator {

	protected IMaven getMaven() {
		IMaven service = getService(IMaven.class);
		this.maven = service;
		return service;
	}

	protected IMavenProjectRegistry getRegistry() {
		return getService(IMavenProjectRegistry.class);
	}

	private <T> T getService(Class<T> clz) {
		BundleContext ctx = FrameworkUtil.getBundle(IndexConfigurator.class)
			.getBundleContext();
		if (ctx == null) {
			throw new IllegalStateException("Bndtools M2E integration is not started");
		}
		ServiceReference<T> ref = ctx.getServiceReference(clz);
		if (ref == null) {
			throw new IllegalStateException(String.format("M2E service %s is missing", clz.getName()));
		}
		T service = ctx.getService(ref);
		if (service == null) {
			throw new IllegalStateException(String.format("M2E service %s is missing", clz.getName()));
		}
		return service;
	}

}

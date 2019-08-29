package bndtools.utils;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ServiceUtils {
	public static final <R, S, E extends Throwable> R usingService(BundleContext context, Class<S> clazz,
		ServiceOperation<R, S, E> operation) throws E {
		ServiceReference<S> reference = context.getServiceReference(clazz);
		if (reference != null) {
			S service = context.getService(reference);
			if (service != null) {
				try {
					return operation.execute(service);
				} finally {
					context.ungetService(reference);
				}
			}
		}
		return null;
	}
}

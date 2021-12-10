package org.bndtools.facade;

import static org.bndtools.facade.ExtensionFacade.consoleLog;
import static org.bndtools.facade.ExtensionFacade.uiLog;

import java.lang.ref.WeakReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

class ExtensionServiceTracker<T> extends ServiceTracker<Object, T> {
	private final Class<?>							downstreamClass;
	private final WeakReference<ExtensionFacade<T>>	parent;

	ExtensionServiceTracker(ExtensionFacade<T> parent, Class<?> downstreamClass, BundleContext context, Filter filter) {
		super(context, filter, null);
		// Reference back to the parent facade needs to be weak
		// so as not to prevent it being garbage collected once it
		// becomes unreachable.
		this.parent = new WeakReference<>(parent);
		this.downstreamClass = downstreamClass;
	}

	@Override
	public T addingService(ServiceReference<Object> reference) {
		ExtensionFacade<T> parent = parent();
		if (parent == null) {
			return null;
		}
		consoleLog.debug("{} addingService: {}", parent, reference);

		ServiceObjects<Object> objs = context.getServiceObjects(reference);
		final Object service = objs.getService();

		// if (service instanceof IExecutableExtension) {
		// IExecutableExtension ee = (IExecutableExtension) service;
		// try {
		// log("Initializing the ExecutableExtension");
		// ee.setInitializationData(config, propertyName, data);
		// } catch (CoreException e) {
		// e.printStackTrace();
		// return null;
		// }
		// }
		// if (service instanceof IExecutableExtensionFactory) {
		// IExecutableExtensionFactory factory =
		// (IExecutableExtensionFactory) service;
		// try {
		// log("Running factory.create()");
		// @SuppressWarnings("unchecked")
		// final T retval = (T) factory.create();
		// onNewService.forEach(callback -> {
		// log("notifying " + callback);
		// callback.accept(reference, retval);
		// });
		// return retval;
		// } catch (CoreException e) {
		// e.printStackTrace();
		// return null;
		// }
		// }
		if (downstreamClass != null && !downstreamClass.isAssignableFrom(service.getClass())) {
			String msg = String.format("%s downstreamClass is not an instance of %s, was %s", parent,
				downstreamClass.getCanonicalName(), service.getClass());
			consoleLog.error(msg);
			uiLog.logError(msg, null);
			objs.ungetService(service);
			return null;
		}
		consoleLog.debug("{} Returning non-factory extension", parent);
		@SuppressWarnings("unchecked")
		final T retval = (T) service;
		parent.onNewService.forEach(callback -> {
			consoleLog.debug("{} notifying callback of new service: {}", parent, callback);
			callback.accept(reference, retval);
		});
		return retval;
	}

	@Override
	public void modifiedService(ServiceReference<Object> reference, T service) {
		parent();
	}

	@Override
	public void removedService(ServiceReference<Object> reference, T service) {
		ExtensionFacade<T> parent = parent();
		if (parent != null) {
			consoleLog.debug("{} notifying service removal", parent);
			parent.onClosedService.forEach(callback -> {
				consoleLog.debug("{} notifying callback of service removal: {}", parent, callback);
				callback.accept(reference, service);
			});
		}
		try {
			ServiceObjects<Object> objs = context.getServiceObjects(reference);
			objs.ungetService(service);
		} catch (IllegalStateException | IllegalArgumentException e) {
			// When the context has been stopped or service already ungotten
		}
	}

	private ExtensionFacade<T> parent() {
		ExtensionFacade<T> parent = this.parent.get();
		if (parent == null) {
			consoleLog.debug("closing tracker {}", filter);
			close();
		}
		return parent;
	}
}

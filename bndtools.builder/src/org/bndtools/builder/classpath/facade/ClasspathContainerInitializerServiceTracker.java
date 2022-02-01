package org.bndtools.builder.classpath.facade;

import static org.bndtools.builder.classpath.facade.ClasspathContainerInitializerFacade.consoleLog;
import static org.bndtools.builder.classpath.facade.ClasspathContainerInitializerFacade.uiLog;

import java.lang.ref.WeakReference;

import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

class ClasspathContainerInitializerServiceTracker extends ServiceTracker<Object, ClasspathContainerInitializer> {
	private final WeakReference<ClasspathContainerInitializerFacade> parent;

	ClasspathContainerInitializerServiceTracker(ClasspathContainerInitializerFacade parent, BundleContext context,
		Filter filter) {
		super(context, filter, null);
		// Reference back to the parent facade needs to be weak
		// so as not to prevent it being garbage collected once it
		// becomes unreachable.
		this.parent = new WeakReference<>(parent);
	}

	@Override
	public ClasspathContainerInitializer addingService(ServiceReference<Object> reference) {
		ClasspathContainerInitializerFacade parent = parent();
		if (parent == null) {
			return null;
		}
		consoleLog.debug("{} addingService: {}", parent, reference);

		ServiceObjects<Object> objs = context.getServiceObjects(reference);
		final Object service = objs.getService();

		if (!(service instanceof ClasspathContainerInitializer)) {
			String msg = String.format("%s downstreamClass is not an instance of %s, was %s", parent,
				ClasspathContainerInitializer.class.getCanonicalName(), service == null ? null : service.getClass());
			consoleLog.error(msg);
			uiLog.logError(msg, null);
			if (service != null) {
				objs.ungetService(service);
			}
			return null;
		}
		consoleLog.debug("{} Returning non-factory extension", parent);
		final ClasspathContainerInitializer retval = (ClasspathContainerInitializer) service;
		parent.onNewService.forEach(callback -> {
			consoleLog.debug("{} notifying callback of new service: {}", parent, callback);
			callback.accept(reference, retval);
		});
		return retval;
	}

	@Override
	public void modifiedService(ServiceReference<Object> reference, ClasspathContainerInitializer service) {
		parent();
	}

	@Override
	public void removedService(ServiceReference<Object> reference, ClasspathContainerInitializer service) {
		ClasspathContainerInitializerFacade parent = parent();
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

	private ClasspathContainerInitializerFacade parent() {
		ClasspathContainerInitializerFacade parent = this.parent.get();
		if (parent == null) {
			consoleLog.debug("closing tracker {}", filter);
			close();
		}
		return parent;
	}
}

package org.bndtools.builder.facade;

import static org.bndtools.builder.facade.BuilderFacade.consoleLog;
import static org.bndtools.builder.facade.BuilderFacade.uiLog;

import java.lang.ref.WeakReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

class BuilderServiceTracker extends ServiceTracker<Object, ProjectBuilderDelegate> {
	private final WeakReference<BuilderFacade> parent;

	BuilderServiceTracker(BuilderFacade parent, BundleContext context, Filter filter) {
		super(context, filter, null);
		// Reference back to the parent facade needs to be weak
		// so as not to prevent it being garbage collected once it
		// becomes unreachable.
		this.parent = new WeakReference<>(parent);
	}

	@Override
	public ProjectBuilderDelegate addingService(ServiceReference<Object> reference) {
		BuilderFacade parent = parent();
		if (parent == null) {
			return null;
		}
		consoleLog.debug("{} addingService: {}", parent, reference);

		ServiceObjects<Object> objs = context.getServiceObjects(reference);
		final Object service = objs.getService();

		if (!(service instanceof ProjectBuilderDelegate)) {
			String msg = String.format("%s downstreamClass is not an instance of %s, was %s", parent,
				ProjectBuilderDelegate.class.getCanonicalName(), service == null ? null : service.getClass());
			consoleLog.error(msg);
			uiLog.logError(msg, null);
			if (service != null) {
				objs.ungetService(service);
			}
			return null;
		}
		consoleLog.debug("{} Returning non-factory extension", parent);
		final ProjectBuilderDelegate retval = (ProjectBuilderDelegate) service;
		retval.setSuper(parent);
		parent.onNewService.forEach(callback -> {
			consoleLog.debug("{} notifying callback of new service: {}", parent, callback);
			callback.accept(reference, retval);
		});
		// Now that the new backing service has started the build rules may
		// have changed and the parent's state should be cleared to reflect
		// this.
		parent.forgetLastBuiltState();
		return retval;
	}

	@Override
	public void modifiedService(ServiceReference<Object> reference, ProjectBuilderDelegate service) {
		parent();
	}

	@Override
	public void removedService(ServiceReference<Object> reference, ProjectBuilderDelegate service) {
		BuilderFacade parent = parent();
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

	private BuilderFacade parent() {
		BuilderFacade parent = this.parent.get();
		if (parent == null) {
			consoleLog.debug("closing tracker {}", filter);
			close();
		}
		return parent;
	}
}

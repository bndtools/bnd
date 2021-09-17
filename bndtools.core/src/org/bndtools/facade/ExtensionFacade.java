package org.bndtools.facade;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import aQute.bnd.exceptions.Exceptions;

public class ExtensionFacade<T> implements IExecutableExtension, IExecutableExtensionFactory, InvocationHandler {

	static org.slf4j.Logger		consoleLog	= org.slf4j.LoggerFactory.getLogger(ExtensionFacade.class);
	static ILogger				uiLog		= Logger.getLogger(ExtensionFacade.class);

	ServiceTracker<Object, T>	tracker;
	String						id;
	IConfigurationElement		config;
	String						propertyName;
	Class<T>					downstreamClass;
	Object						data;
	static final BundleContext	bc	= Optional.ofNullable(FrameworkUtil.getBundle(ExtensionFacade.class))
		.map(Bundle::getBundleContext)
		.orElseGet(null);

	@Override
	public Object create() throws CoreException {
		if (downstreamClass == null) {
			return getRequiredService();
		} else {
			consoleLog.debug("{} Attempting to create downstream object of type: {}", this, downstreamClass);
			return Proxy.newProxyInstance(downstreamClass.getClassLoader(), new Class<?>[] {
				downstreamClass
			}, this);
		}
	}

	static class Customizer<T> implements ServiceTrackerCustomizer<Object, T> {

		final Class<?>							downstreamClass;
		final WeakReference<ExtensionFacade<T>>	parent;

		Customizer(ExtensionFacade<T> parent, Class<?> downstreamClass) {
			// Reference back to the parent facade needs to be weak
			// so as not to prevent it being garbage collected once it
			// becomes unreachable.
			this.parent = new WeakReference<>(parent);
			this.downstreamClass = downstreamClass;
		}

		@Override
		public T addingService(ServiceReference<Object> reference) {
			consoleLog.debug("{} addingService: {}", parent.get(), reference);

			ServiceObjects<?> objs = bc.getServiceObjects(reference);
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
				String msg = String.format("%s downstreamClass is not an instance of %s, was %s", parent.get(),
					downstreamClass.getCanonicalName(), service.getClass());
				consoleLog.error(msg);
				uiLog.logError(msg, null);
				return null;
			}
			consoleLog.debug("{} Returning non-factory extension", parent.get());
			@SuppressWarnings("unchecked")
			final T retval = (T) service;
			ExtensionFacade<T> parent = this.parent.get();
			if (parent != null) {
				parent.onNewService.forEach(callback -> {
					consoleLog.debug("{} notifying callback of new service: {}", parent, callback);
					callback.accept(reference, retval);
				});
			}
			return retval;
		}

		@Override
		public void modifiedService(ServiceReference<Object> reference, T service) {}

		@Override
		public void removedService(ServiceReference<Object> reference, T service) {
			consoleLog.debug("{} notifying service removal", parent.get());
			ExtensionFacade<T> parent = this.parent.get();
			if (parent != null) {
				parent.onClosedService.forEach(callback -> {
					consoleLog.debug("{} notifying callback of service removal: {}", parent, callback);
					callback.accept(reference, service);
				});
			}
			ServiceObjects<Object> objs = bc.getServiceObjects(reference);
			objs.ungetService(service);
		}

	}

	List<BiConsumer<ServiceReference<Object>, T>>	onNewService	= new ArrayList<>();
	List<BiConsumer<ServiceReference<Object>, T>>	onClosedService	= new ArrayList<>();

	public void onNewService(BiConsumer<ServiceReference<Object>, T> callback) {
		onNewService.add(callback);
	}

	public void onClosedService(BiConsumer<ServiceReference<Object>, T> callback) {
		onClosedService.add(callback);
	}

	public boolean isEmpty() {
		return tracker.isEmpty();
	}

	public int size() {
		return tracker.size();
	}

	public Optional<T> getService() {
		return Optional.ofNullable(tracker.getService());
	}

	public T getRequiredService() {
		consoleLog.debug("{} Attempting to get service {}", this, id);
		return getService().orElseThrow(() -> {
			final String className = downstreamClass == null ? "<null>" : downstreamClass.getCanonicalName();
			uiLog.logWarning(MessageFormat.format("Service {0} ({1}) not found.", id, className), null);
			consoleLog.warn("{} Service {} ({}) not found", this, id, className);
			return new RuntimeException("Service " + id + " (" + className + ") not found");
		});
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
		throws CoreException {
		this.config = config;
		this.propertyName = propertyName;
		this.data = data;
		this.id = config.getAttribute("id");

		consoleLog.debug("{} Initializing facade, propName: \"{}\", data: \"{}\"", this, propertyName, data);

		if (data != null && !data.toString()
			.isEmpty()) {
			try {
				String epId = config.getDeclaringExtension()
					.getExtensionPointUniqueIdentifier();

				IExtensionPoint ep = Platform.getExtensionRegistry()
					.getExtensionPoint(epId);
				String bp = ep.getContributor()
					.getName();

				Optional<Bundle> b = Stream.of(bc.getBundles())
					.filter(x -> bp.equals(x.getSymbolicName()))
					.findFirst();

				if (b.isPresent()) {
					consoleLog.debug("{} Attempting to load \"{}\" from bundle: {}", this, data, b.get());
					@SuppressWarnings("unchecked")
					final Class<T> clazz = (Class<T>) b.get()
						.loadClass(data.toString());
					downstreamClass = clazz;
				} else {
					consoleLog.debug("Using our classloader");
					@SuppressWarnings("unchecked")
					final Class<T> clazz = (Class<T>) Class.forName(data.toString());
					downstreamClass = clazz;
				}
			} catch (ClassNotFoundException e) {
				consoleLog.error("{} exception:", this, e);
				throw new CoreException(
					new Status(IStatus.ERROR, getClass(), 0, "Downstream interface for " + id + " not found", e));
			}
		}
		try {
			initializeTracker(id);
		} catch (Exception e) {
			consoleLog.error("{} uncaught exception", this, e);
			throw Exceptions.duck(e);
		}
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		consoleLog.debug("{} Proxying method call: {}()", this, method);
		ClassLoader current = Thread.currentThread()
			.getContextClassLoader();
		Object retval;
		try {
			Object service = getRequiredService();
			Thread.currentThread()
				.setContextClassLoader(service.getClass()
					.getClassLoader());
			retval = method.invoke(service, args);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		} finally {
			Thread.currentThread()
				.setContextClassLoader(current);
		}

		return retval;
	}

	public void close() {
		consoleLog.debug("{} close()", this);
		tracker.close();
	}

	/**
	 * Invoked by the Eclipse UI. Initialization is deferred until
	 * {@link #setInitializationData} is called.
	 */
	public ExtensionFacade() {}

	/**
	 * Constructor for programmatic instantiation.
	 *
	 * @param id
	 */
	public ExtensionFacade(String id, Class<T> downstreamType) {
		downstreamClass = downstreamType;
		initializeTracker(id);
	}

	private void initializeTracker(String id) {
		consoleLog.debug("{} Initializing tracker", this);
		Filter filter = null;
		try {
			filter = bc.createFilter("(component.name=" + id + ")");
			consoleLog.debug("{} Tracking services with filter: {}", this, filter);
			Customizer<T> customizer = new Customizer<T>(this, downstreamClass);
			tracker = new ServiceTracker<Object, T>(bc, filter, customizer);
			tracker.open();
		} catch (InvalidSyntaxException e) {
			consoleLog.error("{} couldn't build filter for {}", this, filter, e);
			throw Exceptions.duck(e);
		}
	}

	@Override
	public String toString() {
		return "[" + id + ":" + System.identityHashCode(this) + "]";
	}

	@Override
	public void finalize() {
		if (tracker != null) {
			consoleLog.debug("{} finalize(): closing tracker", this);
			tracker.close();
		}
	}
}

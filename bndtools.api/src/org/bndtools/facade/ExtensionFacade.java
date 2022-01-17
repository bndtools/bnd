package org.bndtools.facade;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.memoize.Memoize;

public class ExtensionFacade<T> implements IExecutableExtension, IExecutableExtensionFactory, InvocationHandler {

	static org.slf4j.Logger				consoleLog	= org.slf4j.LoggerFactory.getLogger(ExtensionFacade.class);
	static ILogger						uiLog		= Logger.getLogger(ExtensionFacade.class);
	// The memoize defers initialization until later, after class
	// initialization, because sometimes (due to lazy bundle activation) the
	// bundle context can come back null when call getBundleContext() during
	// class initialization.
	final static Memoize<BundleContext>	bc			= Memoize
		.predicateSupplier(() -> Optional.ofNullable(FrameworkUtil.getBundle(ExtensionFacade.class))
			.map(Bundle::getBundleContext)
			.orElse(null), Objects::nonNull);

	ServiceTracker<Object, T>			tracker;
	String								id;
	IConfigurationElement				config;
	String								propertyName;
	Class<T>							downstreamClass;
	Object								data;

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

		if (data != null) {
			final String dataString = data.toString();

			final int index = dataString.indexOf(':');

			String className;
			if (index < 0) {
				className = dataString;
			} else {
				className = dataString.substring(0, index);
				if (index < dataString.length() - 1) {
					this.id = dataString.substring(index + 1);
				}
			}

			if (!className.isEmpty()) {
				try {
					String epId = config.getDeclaringExtension()
						.getExtensionPointUniqueIdentifier();

					IExtensionPoint ep = Platform.getExtensionRegistry()
						.getExtensionPoint(epId);
					String bp = ep.getContributor()
						.getName();

					Optional<Bundle> b = Stream.of(bc.get()
						.getBundles())
						.filter(x -> bp.equals(x.getSymbolicName()))
						.findFirst();

					if (b.isPresent()) {
						consoleLog.debug("{} Attempting to load \"{}\" from bundle: {}", this, data, b.get());
						@SuppressWarnings("unchecked")
						final Class<T> clazz = (Class<T>) b.get()
							.loadClass(className);
						downstreamClass = clazz;
					} else {
						consoleLog.debug("Using our classloader");
						@SuppressWarnings("unchecked")
						final Class<T> clazz = (Class<T>) Class.forName(className);
						downstreamClass = clazz;
					}
				} catch (ClassNotFoundException e) {
					consoleLog.error("{} exception:", this, e);
					throw new CoreException(
						new Status(IStatus.ERROR, getClass(), 0, "Downstream interface for " + id + " not found", e));
				}
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
		consoleLog.debug("{} Initializing tracker {} for bundle context: {}", this, id, bc);
		Filter filter = null;
		try {
			filter = bc.get()
				.createFilter("(component.name=" + id + ")");
			consoleLog.debug("{} Tracking services with filter: {}", this, filter);
			tracker = new ExtensionServiceTracker<T>(this, downstreamClass, bc.get(), filter);
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
}

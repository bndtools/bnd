package aQute.bnd.remote.junit;

import static aQute.bnd.remote.junit.JUnitFrameworkBuilder.projectDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.launch.Framework;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.osgi.Builder;
import aQute.lib.converter.Converter;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.inject.Injector;

/**
 * This class provides an OSGi framework that is configured with the current bnd
 * workspace. A project directory is used to find the workspace. This makes all
 * repositories in the workspace available to the framework. To be able to test
 * JUnit code against/in this framework it is necessary that all packages on the
 * buildpath and testpath are actually exported in the framework. This class
 * will ensure that. Once the framework is up and running it will be possible to
 * add bundles to it. There are a number of ways that this can be achieved:
 * <ul>
 * <li>Build a bundle – A bnd {@link Builder} is provided to create a bundle and
 * install it. This makes it possible to add classes from the src or test
 * directories or resources. See {@link #bundle()}. Convenience methods are
 * added to get services, see {@link #getService(Class)} et. al. Notice that
 * this framework starts in the same process as that the JUnit code runs. This
 * is normally a separately started VM.
 */
public class JUnitFramework implements AutoCloseable {

	private static final long			SERVICE_DEFAULT_TIMEOUT	= 5000L;
	static final AtomicInteger			n						= new AtomicInteger();
	final Framework						framework;
	final List<ServiceTracker<?, ?>>	trackers				= new ArrayList<>();

	final JUnitFrameworkBuilder			builder;
	final List<FrameworkEvent>			frameworkEvents			= new CopyOnWriteArrayList<FrameworkEvent>();
	final Injector<Service>				injector;

	Bundle								testbundle;

	JUnitFramework(JUnitFrameworkBuilder jUnitFrameworkBuilder, Framework framework) {
		try {
			this.builder = jUnitFrameworkBuilder;
			this.framework = framework;
			this.framework.init();
			this.injector = new Injector<>(makeConverter(), this::getService, Service.class);

			framework.getBundleContext()
				.addFrameworkListener(frameworkEvents::add);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Add a file as a bundle to the framework. This bundle will not be started.
	 * 
	 * @param f the file to install
	 * @return the bundle object
	 */
	public Bundle bundle(File f) {
		try {
			return framework.getBundleContext()
				.installBundle(toInstallURI(f));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Install a number of bundles based on their bundle specification. A bundle
	 * specification is the format used in for example -runbundles.
	 * 
	 * @param specification the bundle specifications
	 * @return a list of bundles
	 */
	public List<Bundle> bundles(String specification) {
		try {
			return JUnitFrameworkBuilder.workspace.getLatestBundles(projectDir.getAbsolutePath(), specification)
				.stream()
				.map(File::new)
				.map(this::bundle)
				.collect(Collectors.toList());
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Install a number of bundles
	 * 
	 * @param runbundles the list of bundles
	 * @return a list of bundle objects
	 */
	public List<Bundle> bundles(File... runbundles) {
		if (runbundles == null || runbundles.length == 0)
			return Collections.emptyList();

		try {
			List<Bundle> bundles = new ArrayList<>();
			for (File f : runbundles) {
				Bundle bundle = framework.getBundleContext()
					.installBundle(toInstallURI(f));
				bundles.add(bundle);
			}
			return bundles;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Start a bundle
	 * 
	 * @param b the bundle object
	 */
	public void start(Bundle b) {
		try {
			if (!isFragment(b))
				b.start();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Start all bundles
	 * 
	 * @param bs a collection of bundles
	 */
	public void start(Collection<Bundle> bs) {
		bs.forEach(this::start);
	}

	/**
	 * Close this framework
	 */
	@Override
	public void close() throws Exception {
		framework.stop();
		framework.waitForStop(builder.closeTimeout);
	}

	/**
	 * Get the Bundle Context. If a test bundle was installed then this is the
	 * context of the test bundle otherwise it is the context of the framework.
	 * To be able to proxy services it is necessary to have a test bundle
	 * installed.
	 * 
	 * @return the bundle context of the test bundle or the framework
	 */
	public BundleContext getBundleContext() {
		if (testbundle != null)
			return testbundle.getBundleContext();

		return framework.getBundleContext();
	}

	/**
	 * Get a service registered under class1. If multiple services or none are
	 * registered then this method will throw an exception when asserts are
	 * enabled.
	 * 
	 * @param class1 the name of the service
	 * @return a service
	 */
	public <T> T getService(Class<T> class1) {
		try {
			List<T> services = getServices(class1);
			assert 1 == services.size();
			return services.get(0);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Get a list of services of a given name
	 * 
	 * @param class1 the service name
	 * @return a list of services
	 */
	public <T> List<T> getServices(Class<T> class1) {
		try {
			Collection<ServiceReference<T>> refs = getBundleContext().getServiceReferences(class1, null);
			List<T> result = new ArrayList<>();
			for (ServiceReference<T> ref : refs) {
				T service = getBundleContext().getService(ref);
				if (service != null)
					result.add(service);
			}
			return result;
		} catch (InvalidSyntaxException e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Add the standard Gogo bundles
	 */
	public JUnitFramework gogo() {
		try {
			bundles("org.apache.felix.gogo.runtime,org.apache.felix.gogo.command,org.apache.felix.gogo.shell");
			return this;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Inject an object with services and other OSGi specific values.
	 * 
	 * @param object the object to inject
	 */

	public JUnitFramework inject(Object object) {
		try {
			injector.inject(object);
			return this;
		} catch (TimeoutException e) {
			// reportComponents();
			throw Exceptions.duck(e);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Install a bundle from a file
	 * 
	 * @param file the file to install
	 * @return a bundle
	 */
	public Bundle install(File file) {
		try {
			return framework.getBundleContext()
				.installBundle(toInstallURI(file));
		} catch (BundleException e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Create a new synthetic bundle.
	 * 
	 * @return the bundle builder
	 */
	public BundleBuilder bundle() {
		return new BundleBuilder(this);
	}

	/**
	 * Create a new object and inject it.
	 * 
	 * @param type the type of object
	 * @return a new object injected and all
	 */
	public <T> T newInstance(Class<T> type) {
		try {
			return injector.newInstance(type);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Show the information of how the framework is setup and is running
	 */
	public void report() throws InvalidSyntaxException {
		reportBundles(System.out);
		reportServices(System.out);
		reportEvents(System.out);
	}

	/**
	 * Show the installed bundles
	 */
	public void reportBundles(Appendable out) {
		try (Formatter f = new Formatter(out)) {
			Stream.of(framework.getBundleContext()
				.getBundles())
				.forEach(bb -> {
					f.format("%4s %s\n", bundleStateToString(bb.getState()), bb);
				});
		}
	}

	/**
	 * Show the registered service
	 */
	public void reportServices(Appendable out) throws InvalidSyntaxException {
		try (Formatter f = new Formatter(out)) {
			Stream.of(framework.getBundleContext()
				.getAllServiceReferences(null, null))
				.forEach(sref -> {
					f.format("%s\n", sref);
				});
			f.flush();
		}
	}

	/**
	 * Wait for a Service Reference to be registered
	 * 
	 * @param class1 the name of the service
	 * @param timeoutInMs the time to wait
	 * @return a service reference
	 * @throws InterruptedException
	 */
	public <T> Optional<ServiceReference<T>> waitForServiceReference(Class<T> class1, long timeoutInMs)
		throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutInMs;
		while (deadline > System.currentTimeMillis()) {

			ServiceReference<T> serviceReference = getBundleContext().getServiceReference(class1);
			if (serviceReference != null) {
				return Optional.of(serviceReference);
			}
			Thread.sleep(50);
		}
		return Optional.empty();
	}

	/**
	 * Wait for service to be registered
	 * 
	 * @param class1 name of the service
	 * @param timeoutInMs timeout in ms
	 * @return a service
	 * @throws InterruptedException
	 */
	public <T> Optional<T> waitForService(Class<T> class1, long timeoutInMs) throws InterruptedException {
		try {
			Optional<ServiceReference<T>> ref = waitForServiceReference(class1, timeoutInMs);
			BundleContext context = getBundleContext();
			return ref.map(context::getService);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	Object getService(Injector.Target<Service> param) {

		try {
			if (param.type == BundleContext.class) {
				return getBundleContext();
			}
			if (param.type == Bundle.class)
				return testbundle;

			if (param.type == Framework.class) {
				return framework;
			}
			if (param.type == Bundle[].class) {
				return framework.getBundleContext()
					.getBundles();
			}

			Service service = param.annotation;
			String target = service.target()
				.isEmpty() ? null : service.target();

			Class<?> serviceClass = service.service();

			if (serviceClass == Object.class)
				serviceClass = getServiceType(param.type);

			if (serviceClass == null)
				serviceClass = getServiceType(param.primaryType);

			if (serviceClass == null)
				throw new IllegalArgumentException("Cannot define service class for " + param);

			boolean multiple = isMultiple(param.type);

			List<? extends ServiceReference<?>> serviceReferences = getReferences(serviceClass, target);

			if (multiple) {
				return serviceReferences;
			} else {
				if (serviceReferences.isEmpty()) {

					long timeout = service.timeout();
					if (timeout <= 0)
						timeout = SERVICE_DEFAULT_TIMEOUT;
					Optional<?> waitForServiceReference = waitForServiceReference(serviceClass, timeout);
					return waitForServiceReference
						.orElseThrow(() -> new TimeoutException("Cannot find service " + param));
				} else
					return serviceReferences.get(0);
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	void reportEvents(Appendable out) {
		try (Formatter f = new Formatter(out)) {
			frameworkEvents.forEach(fe -> {
				f.format("%s\n", fe);
			});
			f.flush();
		}
	}

	private String bundleStateToString(int state) {
		switch (state) {
			case Bundle.UNINSTALLED :
				return "UNIN";
			case Bundle.INSTALLED :
				return "INST";
			case Bundle.RESOLVED :
				return "RSLV";
			case Bundle.STARTING :
				return "STAR";
			case Bundle.ACTIVE :
				return "ACTV";
			case Bundle.STOPPING :
				return "STOP";
			default :
				return "UNKN";
		}
	}

	private List<? extends ServiceReference<?>> getReferences(Class<?> serviceClass, String target)
		throws InvalidSyntaxException {
		List<? extends ServiceReference<?>> serviceReferences = new ArrayList<>(
			getBundleContext().getServiceReferences(serviceClass, target));
		Collections.sort(serviceReferences);
		return serviceReferences;
	}

	private Class<?> getServiceType(Type type) {
		if (type instanceof Class)
			return (Class<?>) type;

		if (type instanceof ParameterizedType) {
			Type rawType = ((ParameterizedType) type).getRawType();
			if (rawType instanceof Class) {
				Class<?> rawClass = (Class<?>) rawType;
				if (Iterable.class.isAssignableFrom(rawClass)) {
					return getServiceType(((ParameterizedType) type).getActualTypeArguments()[0]);
				}
				if (Optional.class.isAssignableFrom(rawClass)) {
					return getServiceType(((ParameterizedType) type).getActualTypeArguments()[0]);
				}
				if (ServiceReference.class.isAssignableFrom(rawClass)) {
					return getServiceType(((ParameterizedType) type).getActualTypeArguments()[0]);
				}
			}
		}
		return null;
	}

	private boolean isMultiple(Type type) {
		if (type instanceof Class) {
			return ((Class<?>) type).isArray();
		}
		if (type instanceof ParameterizedType) {

			Type rawType = ((ParameterizedType) type).getRawType();
			if (rawType instanceof Class) {
				Class<?> clazz = (Class<?>) rawType;
				if (Iterable.class.isAssignableFrom(clazz))
					return true;
			}

		}
		return false;
	}

	private boolean isParameterizedType(Type to, Class<?> clazz) {
		if (to instanceof ParameterizedType) {
			if (((ParameterizedType) to).getRawType() == clazz)
				return true;
		}
		return false;
	}

	private Converter makeConverter() {
		Converter converter = new Converter();
		converter.hook(null, (to, from) -> {
			try {
				if (!(from instanceof ServiceReference))
					return null;

				ServiceReference<?> reference = (ServiceReference<?>) from;

				if (isParameterizedType(to, ServiceReference.class))
					return reference;

				if (isParameterizedType(to, Map.class))
					return converter.convert(to, toMap(reference));

				Object service = getBundleContext().getService(reference);

				if (isParameterizedType(to, Optional.class))
					return Optional.ofNullable(service);

				return service;
			} catch (Exception e) {
				throw e;
			}
		});
		return converter;
	}

	private String toInstallURI(File c) {
		return "reference:" + c.toURI();
	}

	private Map<String, Object> toMap(ServiceReference<?> reference) {
		Map<String, Object> map = new HashMap<>();

		for (String key : reference.getPropertyKeys()) {
			map.put(key, reference.getProperty(key));
		}

		return map;
	}

	/**
	 * Get a bundle by symbolic name
	 */
	public Optional<Bundle> getBundle(String bsn) {
		return Stream.of(getBundleContext().getBundles())
			.filter(b -> bsn.equals(b.getSymbolicName()))
			.findFirst();
	}

	/**
	 * Broadcast a message to many services
	 */

	@SuppressWarnings("unchecked")
	public <T> int broadcast(Class<T> type, Consumer<T> consumer) {
		ServiceTracker<T, T> tracker = new ServiceTracker<>(getBundleContext(), type, null);
		int n = 0;
		try {
			for (T instance : (T[]) tracker.getServices()) {
				consumer.accept(instance);
				n++;
			}
		} finally {
			tracker.close();
		}
		return n;
	}

	/**
	 * Hide a service by registering a hook. This should in general be done
	 * before you let others look. In general, the JUnit Framework should be
	 * started in {@link JUnitFrameworkBuilder#nostart()} mode. This initializes
	 * the OSGi framework making it possible to register a service before
	 */

	public Closeable hide(Class<?> type) {
		ServiceRegistration<EventListenerHook> eventReg = getBundleContext().registerService(EventListenerHook.class,
			(event, listeners) -> {
				if (isOneOfType(event.getServiceReference(), type))
					listeners.entrySet()
						.removeIf(e -> e.getKey()
							.getBundle() != testbundle);
			}, null);

		ServiceRegistration<FindHook> findReg = getBundleContext().registerService(FindHook.class, new FindHook() {

			@Override
			public void find(BundleContext context, String name, String filter, boolean allServices,
				Collection<ServiceReference<?>> references) {
				if (name == null || name.equals(type.getName()))
					references.removeIf(r -> r.getBundle() != testbundle);
			}
		}, null);

		return () -> {
			eventReg.unregister();
			findReg.unregister();
		};
	}

	public boolean isOneOfType(ServiceReference<?> serviceReference, Class<?>... types) {
		String[] objectClasses = (String[]) serviceReference.getProperty(Constants.OBJECTCLASS);
		for (Class<?> type : types) {
			String name = type.getName();

			for (String objectClass : objectClasses) {
				if (objectClass.equals(name))
					return true;
			}
		}
		return false;
	}

	/**
	 * Start the framework if not yet started
	 */

	public void start() {
		try {
			framework.start();
			List<Bundle> toBeStarted = new ArrayList<>();
			for (String path : builder.local.runbundles) {
				File file = new File(path);
				if (!file.isFile())
					throw new IllegalArgumentException("-runbundle " + file + " does not exist or is not a file");

				Bundle b = install(file);
				if (!isFragment(b))
					toBeStarted.add(b);
			}

			toBeStarted.forEach(this::start);

			if (builder.testbundle)
				testbundle();

			toBeStarted.forEach(this::start);

		} catch (BundleException e) {
			throw Exceptions.duck(e);
		}
	}

	private boolean isFragment(Bundle b) {
		return b.getHeaders()
			.get(Constants.FRAGMENT_HOST) != null;
	}

	/**
	 * Stop the framework if not yet stopped
	 */

	public void stop() {
		try {
			framework.stop();
		} catch (BundleException e) {
			throw Exceptions.duck(e);
		}
	}

	public void testbundle() {
		if (testbundle != null) {
			throw new IllegalArgumentException("Test bundle already exists");
		}
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			Manifest man = new Manifest();
			man.getMainAttributes()
				.putValue("Manifest-Version", "1");
			String name = projectDir.getName()
				.toUpperCase();
			man.getMainAttributes()
				.putValue(Constants.BUNDLE_SYMBOLICNAME, name);
			man.getMainAttributes()
				.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
			JarOutputStream jout = new JarOutputStream(bout, man);
			jout.close();
			ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
			this.testbundle = framework.getBundleContext()
				.installBundle(name, bin);
			this.testbundle.start();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public <T> ServiceRegistration<T> register(Class<T> type, T instance) {
		return getBundleContext().registerService(type, instance, null);
	}

	public Framework getFramework() {
		return framework;
	}
}

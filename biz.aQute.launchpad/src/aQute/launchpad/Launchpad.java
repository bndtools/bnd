package aQute.launchpad;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
//import org.osgi.service.component.runtime.ServiceComponentRuntime;
//import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.service.specifications.RunSpecification;
import aQute.launchpad.internal.ProbeImpl;
import aQute.lib.converter.Converter;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.inject.Injector;
import aQute.lib.io.IO;
import aQute.lib.startlevel.StartLevelRuntimeHandler;
import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import aQute.libg.parameters.ParameterMap;

/**
 * This class provides an OSGi framework that is configured with the current bnd
 * workspace. A project directory is used to find the workspace. This makes all
 * repositories in the workspace available to the framework. To be able to test
 * JUnit code against/in this framework it is necessary that all packages on the
 * buildpath and testpath are actually exported in the framework. This class
 * will ensure that. Once the framework is up and running it will be possible to
 * add bundles to it. There are a number of ways that this can be achieved:
 * <ul>
 * <li>Build a bundle – A bnd Builder is provided to create a bundle and install
 * it. This makes it possible to add classes from the src or test directories or
 * resources. See {@link #bundle()}. Convenience methods are added to get
 * services, see {@link #getService(Class)} et. al. Notice that this framework
 * starts in the same process as that the JUnit code runs. This is normally a
 * separately started VM.
 */
@ProviderType
public class Launchpad implements AutoCloseable {

	public static final String					BUNDLE_PRIORITY			= "Bundle-Priority";
	private static final long					SERVICE_DEFAULT_TIMEOUT	= 60000L;
	final AtomicInteger							counter					= new AtomicInteger();
	final File									projectDir;

	final Framework								framework;
	final List<ServiceTracker<?, ?>>			trackers				= new ArrayList<>();
	final List<FrameworkEvent>					frameworkEvents			= new CopyOnWriteArrayList<>();
	final Injector<Service>						injector;
	final Map<Class<?>, ServiceTracker<?, ?>>	injectedDoNotClose		= new HashMap<>();
	final Set<String>							frameworkExports;
	final List<String>							errors					= new ArrayList<>();
	final String								name;
	final String								className;
	final RunSpecification						runspec;
	final boolean								hasTestBundle;
	final StartLevelRuntimeHandler				startlevels;

	Bundle										testbundle;
	boolean										debug;
	final boolean								byReference;

	PrintStream									out						= System.err;
	ServiceTracker<FindHook, FindHook>			hooks;
	private long								closeTimeout;
	private Bundle								proxyBundle;
	private Probe								probe					= new ProbeImpl();

	Launchpad(Framework framework, String name, String className, RunSpecification runspec, long closeTimeout,
		boolean debug, boolean hasTestBundle, boolean byReference) {
		this.runspec = runspec;
		this.closeTimeout = closeTimeout;
		this.hasTestBundle = hasTestBundle;
		this.byReference = byReference;
		this.proxyBundle = framework;
		this.startlevels = StartLevelRuntimeHandler.create(this::report, this.runspec.properties);

		try {
			this.className = className;
			this.name = name;
			this.projectDir = IO.work;
			this.debug = debug;
			this.framework = framework;
			this.framework.init();
			this.injector = new Injector<>(makeConverter(), this::getService, Service.class);
			this.frameworkExports = getExports(framework).keySet();

			report("Initialized framework %s", this.framework);
			report("Classpath %s", System.getProperty("java.class.path")
				.replace(File.pathSeparatorChar, '\n'));

			framework.getBundleContext()
				.addFrameworkListener(frameworkEvents::add);

			hooks = new ServiceTracker<>(framework.getBundleContext(), FindHook.class, null);
			hooks.open();

			startlevels.beforeStart(framework);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public void report(String format, Object... args) {
		if (!debug)
			return;

		out.printf(format + "%n", args);
	}

	/**
	 * Generate an error so that the test case can check if we found anything
	 * wrong. This is easy to check with {@link #check(String...)}
	 *
	 * @param format the format string used in
	 *            {@link String#format(String, Object...)}
	 * @param args the arguments to be formatted
	 */
	public void error(String format, Object... args) {
		report(format, args);
		String msg = String.format(format, args);
		errors.add(msg);
	}

	/**
	 * Check the errors found, filtering out any unwanted with globbing patters.
	 * Each error is filtered against all the patterns. This method return true
	 * if there are no unfiltered errors, otherwise false.
	 *
	 * @param patterns globbing patterns
	 * @return true if no errors after filtering,otherwise false
	 */
	public boolean check(String... patterns) {
		Glob[] globs = Stream.of(patterns)
			.map(Glob::new)
			.toArray(Glob[]::new);
		boolean[] used = new boolean[globs.length];

		String[] unmatched = errors.stream()
			.filter(msg -> {
				for (int i = 0; i < globs.length; i++) {
					if (globs[i].finds(msg) >= 0) {
						used[i] = true;
						return false;
					}
				}
				return true;
			})
			.toArray(String[]::new);

		if (unmatched.length == 0) {

			List<Glob> report = new ArrayList<>();
			for (int i = 0; i < used.length; i++) {
				if (!used[i]) {
					report.add(globs[i]);
				}
			}

			if (report.isEmpty())
				return true;

			out.println("Missing patterns");
			out.println(Strings.join("\n", globs));
			return false;
		}

		out.println("Errors");
		out.println(Strings.join("\n", unmatched));
		return false;
	}

	/**
	 * Add a file as a bundle to the framework. This bundle will not be started.
	 *
	 * @param f the file to install
	 * @return the bundle object
	 */
	public Bundle bundle(File f) {
		try {
			report("Installing %s", f);
			return framework.getBundleContext()
				.installBundle(toInstallURI(f));
		} catch (Exception e) {
			report("Failed to installing %s : %s", f, e);
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Set the debug flag
	 */
	public Launchpad debug() {
		this.debug = true;
		return this;
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
			return LaunchpadBuilder.workspace.getLatestBundles(projectDir.getAbsolutePath(), specification)
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
				Bundle b = bundle(f);
				bundles.add(b);
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
			if (!isFragment(b)) {
				report("Starting %s", b);
				b.start();

				Set<String> exports = getExports(b).keySet();
				Set<String> imports = getImports(b).keySet();
				exports.removeAll(imports);
				exports.retainAll(frameworkExports);

				if (!exports.isEmpty()) {
					error(
						"bundle %s is exporting but NOT importing package(s) %s that are/is also exported by the framework.\n"
							+ "This means that the test code and the bundle cannot share classes of these package.",
						b, exports);
				}

			} else {
				report("Not starting fragment %s", b);
			}
		} catch (Exception e) {
			report("Failed to start %s : %s", b, e);
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
		startlevels.close();
		report("Stop the framework");
		framework.stop();
		report("Stopped the framework");
		framework.waitForStop(closeTimeout);
		report("Framework fully stopped");
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
	 * Get a service registered under class. If multiple services are registered
	 * it will return the first
	 *
	 * @param serviceInterface the name of the service
	 * @return a service
	 */
	public <T> Optional<T> getService(Class<T> serviceInterface) {
		return getService(serviceInterface, null);
	}

	public <T> Optional<T> getService(Class<T> serviceInterface, @Nullable String target) {
		return getServices(serviceInterface, target, 0, 0, false).stream()
			.map(this::getService)
			.findFirst();
	}

	/**
	 * Get a list of services of a given name
	 *
	 * @param serviceClass the service name
	 * @return a list of services
	 */
	public <T> List<T> getServices(Class<T> serviceClass) {
		return getServices(serviceClass, null);
	}

	/**
	 * Get a list of services in the current registry
	 *
	 * @param serviceClass the type of the service
	 * @param target the target, may be null
	 * @return a list of found services currently in the registry
	 */
	public <T> List<T> getServices(Class<T> serviceClass, @Nullable String target) {
		return getServices(serviceClass, target, 0, 0, false).stream()
			.map(this::getService)
			.collect(Collectors.toList());
	}

	/**
	 * Get a service from a reference. If the service is null, then throw an
	 * exception.
	 *
	 * @param ref the reference
	 * @return the service, never null
	 */
	public <T> T getService(ServiceReference<T> ref) {
		try {
			T service = getBundleContext().getService(ref);
			if (service == null) {
				if (ref.getBundle() == null) {
					throw new ServiceException(
						"getService(" + ref + ") returns null, the service is no longer registered");
				}
				throw new ServiceException("getService(" + ref + ") returns null, this probbaly means the \n"
					+ "component failed to activate. The cause can \n" + "generally be found in the log.\n" + "");
			}
			return service;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Add the standard Gogo bundles
	 */
	public Launchpad gogo() {
		try {
			bundles("org.apache.felix.gogo.runtime,org.apache.felix.gogo.command,org.apache.felix.gogo.shell");
			return this;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Add the standard Gogo bundles
	 */
	public Launchpad snapshot() {
		try {
			bundles("biz.aQute.bnd.runtime.snapshot");
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

	public Launchpad inject(Object object) {
		try {
			injector.inject(object);
			return this;
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
			if (byReference) {
				String installURI = toInstallURI(file);
				report("Installing %s", installURI);
				return framework.getBundleContext()
					.installBundle(installURI);
			}
			try (FileInputStream fin = new FileInputStream(file)) {
				return framework.getBundleContext()
					.installBundle("-> " + file, fin);
			} catch (FileNotFoundException e) {
				report("Failed to install %s  because file could not be found", file);
				throw Exceptions.duck(e);
			} catch (IOException e) {
				report("Failed to install %s  because %s", file, e.getMessage());
				throw Exceptions.duck(e);
			}
		} catch (BundleException e) {
			report("Failed to install %s : %s", file, e);
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
			report("Failed to create and instance for %s : %s", type, e);
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Show the information of how the framework is setup and is running
	 */
	public Launchpad report() throws InvalidSyntaxException {
		boolean old = debug;
		debug = true;
		reportBundles();
		reportServices();
		reportEvents();
		debug = old;
		return this;
	}

	/**
	 * Show the installed bundles
	 */
	public void reportBundles() {
		Stream.of(framework.getBundleContext()
			.getBundles())
			.forEach(bb -> {
				report("%4s %4s %s", bundleStateToString(bb.getState()), startlevels.getBundleStartLevel(bb), bb);
			});
	}

	/**
	 * Show the registered service
	 */
	public void reportServices() throws InvalidSyntaxException {
		Stream.of(framework.getBundleContext()
			.getAllServiceReferences(null, null))
			.forEach(sref -> {
				report("%s", sref);
			});
	}

	/**
	 * Wait for a Service Reference to be registered
	 *
	 * @param class1 the name of the service
	 * @param timeoutInMs the time to wait
	 * @return a service reference
	 */
	public <T> Optional<ServiceReference<T>> waitForServiceReference(Class<T> class1, long timeoutInMs) {

		return getServices(class1, null, 1, timeoutInMs, false).stream()
			.findFirst();

	}

	/**
	 * Wait for a Service Reference to be registered
	 *
	 * @param class1 the name of the service
	 * @param timeoutInMs the time to wait
	 * @return a service reference
	 */
	public <T> Optional<ServiceReference<T>> waitForServiceReference(Class<T> class1, long timeoutInMs, String target) {

		return getServices(class1, target, 1, timeoutInMs, false).stream()
			.findFirst();

	}

	/**
	 * Wait for service to be registered
	 *
	 * @param class1 name of the service
	 * @param timeoutInMs timeout in ms
	 * @return a service
	 */
	public <T> Optional<T> waitForService(Class<T> class1, long timeoutInMs) {
		return this.waitForService(class1, timeoutInMs, null);
	}

	/**
	 * Wait for service to be registered
	 *
	 * @param class1 name of the service
	 * @param timeoutInMs timeout in ms
	 * @param target filter, may be null
	 * @return a service
	 */
	public <T> Optional<T> waitForService(Class<T> class1, long timeoutInMs, String target) {
		try {
			return getServices(class1, target, 1, timeoutInMs, false).stream()
				.findFirst()
				.map(getBundleContext()::getService);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Turn a service reference's properties into a Map
	 *
	 * @param reference the reference
	 * @return a Map with all the properties of the reference
	 */
	public Map<String, Object> toMap(ServiceReference<?> reference) {
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
	 * Broadcast a message to many services at once
	 */

	@SuppressWarnings("unchecked")
	public <T> int broadcast(Class<T> type, Consumer<T> consumer) {
		ServiceTracker<T, T> tracker = new ServiceTracker<>(getBundleContext(), type, null);
		tracker.open();
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
	 * before you let others look. In general, the Launchpad should be started
	 * in {@link LaunchpadBuilder#nostart()} mode. This initializes the OSGi
	 * framework making it possible to register a service before
	 */

	public Closeable hide(Class<?> type) {
		return hide(type, "hide");
	}

	/**
	 * Hide a service. This will register a FindHook and an EventHook for the
	 * type. This will remove the visibility of all services with that type for
	 * all bundles _except_ the testbundle. Notice that bundles that already
	 * obtained a references are not affected. If you use this facility it is
	 * best to not start the framework before you hide a service. You can
	 * indicate this to the build with {@link LaunchpadBuilder#nostart()}. The
	 * framework can be started after creation with {@link #start()}. Notice
	 * that services through the testbundle remain visible for this hide.
	 *
	 * @param type the type to hide
	 * @param reason the reason why it is hidden
	 * @return a Closeable, when closed it will remove the hooks
	 */
	public Closeable hide(Class<?> type, String reason) {
		ServiceRegistration<EventListenerHook> eventReg = framework.getBundleContext()
			.registerService(EventListenerHook.class, new EventListenerHook() {
				@Override
				public void event(ServiceEvent event, Map<BundleContext, Collection<ListenerInfo>> listeners) {

					ServiceReference<?> ref = event.getServiceReference();
					if (selectForHiding(type, ref))
						listeners.clear();
				}

				@Override
				public String toString() {
					return "Launchpad[" + reason + "]";
				}
			}, null);

		ServiceRegistration<FindHook> findReg = framework.getBundleContext()
			.registerService(FindHook.class, new FindHook() {

				@Override
				public void find(BundleContext context, String name, String filter, boolean allServices,
					Collection<ServiceReference<?>> references) {
					if (name == null || name.equals(type.getName())) {
						references.removeIf(ref -> selectForHiding(type, ref));
					}
				}

				@Override
				public String toString() {
					return "Launchpad[" + reason + "]";
				}

			}, null);

		return () -> {
			eventReg.unregister();
			findReg.unregister();
		};
	}

	/**
	 * Check of a service reference has one of the given types in its object
	 * class
	 *
	 * @param serviceReference the service reference to check
	 * @param types the set of types
	 * @return true if one of the types name is in the service reference's
	 *         objectClass property
	 */
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

	private boolean selectForHiding(Class<?> type, ServiceReference<?> ref) {

		//
		// We never hide services registered by the testbundle
		//

		if (ref.getBundle() == testbundle)
			return false;

		// only hide references when one of their
		// service interfaces is of the hidden type

		return isOneOfType(ref, type);
	}

	/**
	 * Start the framework if not yet started
	 */

	public void start() {
		try {
			framework.start();
			List<Bundle> toBeStarted = new ArrayList<>();
			for (String path : runspec.runbundles) {
				File file = new File(path);
				if (!file.isFile())
					throw new IllegalArgumentException("-runbundle " + file + " does not exist or is not a file");

				Bundle b = install(file);
				if (!isFragment(b)) {
					toBeStarted.add(b);
				}
			}

			FrameworkWiring fw = framework.adapt(FrameworkWiring.class);
			fw.resolveBundles(toBeStarted);

			Collections.sort(toBeStarted, this::startorder);

			if (hasTestBundle)
				testbundle();

			toBeStarted.forEach(this::start);

			startlevels.afterStart();
		} catch (BundleException e) {
			throw Exceptions.duck(e);
		}
	}

	// reverse ordering. I.e. highest priority is first
	int startorder(Bundle a, Bundle b) {

		return Integer.compare(getPriority(b), getPriority(a));
	}

	private int getPriority(Bundle b) {
		try {
			String h = b.getHeaders()
				.get(BUNDLE_PRIORITY);
			if (h != null)
				return Integer.parseInt(h);
		} catch (Exception e) {
			// ignore
		}
		return 0;
	}

	/**
	 * Stop the framework if not yet stopped
	 */

	public void stop() {
		try {
			report("Stopping the framework");
			framework.stop();
		} catch (BundleException e) {
			report("Could not stop the framework : %s", e);
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Set the test bundle
	 */
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
			report("Creating test bundle %s", name);
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
			report("Failed to create test bundle");
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Register a service. You can specify the type and the instance as well as
	 * the properties. The properties are specified as varargs. That means you
	 * can define a property by specifying the key (which must be a String) and
	 * the value consecutively. The value can be any of the types allowed by the
	 * service properties.
	 *
	 * <pre>
	 * fw.register(Foo.class, instance, "foo", 10, "bar", new long[] {
	 * 	1, 2, 3
	 * });
	 * </pre>
	 *
	 * @param type the service type
	 * @param instance the service object
	 * @param props the service properties specified as a seq of "key", value
	 * @return the service registration
	 */
	public <T> ServiceRegistration<T> register(Class<T> type, T instance, Object... props) {
		report("Registering service %s %s", type, instance, Arrays.toString(props));
		Hashtable<String, Object> ht = new Hashtable<>();
		for (int i = 0; i < props.length; i += 2) {
			String key = (String) props[i];
			Object value = null;
			if (i + 1 < props.length) {
				value = props[i + 1];
			}
			ht.put(key, value);
		}
		return getBundleContext().registerService(type, instance, ht);
	}

	/**
	 * Return the framework object
	 *
	 * @return the framework object
	 */
	public Framework getFramework() {
		return framework;
	}

	/**
	 * Add a component class. This creates a little bundle that holds the
	 * component class so that bnd adds the DS XML. However, it also imports the
	 * package of the component class so that in runtime DS will load it from
	 * the classpath.
	 */

	public <T> Bundle component(Class<T> type) {
		return bundle().addResource(type)
			.start();
	}

	/**
	 * Runs the given code within the context of a synthetic bundle. Creates a
	 * synthetic bundle and adds the supplied class to it using
	 * {@link BundleBuilder#addResourceWithCopy}. It then loads the class using
	 * the synthetic bundle's class loader and instantiates it using the public,
	 * no-parameter constructor.
	 *
	 * @param clazz the class to instantiate within the context of the
	 *            framework.
	 * @return The instantiated object.
	 * @see BundleBuilder#addResourceWithCopy(Class)
	 */
	public <T> T instantiateInFramework(Class<? extends T> clazz) {
		try {
			clazz.getConstructor();
		} catch (NoSuchMethodException e) {
			Exceptions.duck(e);
		}
		Bundle b = bundle().addResourceWithCopy(clazz)
			.start();
		try {
			@SuppressWarnings("unchecked")
			Class<? extends T> insideClass = (Class<? extends T>) b.loadClass(clazz.getName());
			return insideClass.getConstructor()
				.newInstance();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Check if a bundle is a fragement
	 *
	 * @param b the bundle to check
	 */
	public boolean isFragment(Bundle b) {
		return b.getHeaders()
			.get(Constants.FRAGMENT_HOST) != null;
	}

	/**
	 * Check if a bundle is in the ACTIVE state
	 *
	 * @param b the bundle to check
	 */
	public boolean isActive(Bundle b) {
		return b.getState() == Bundle.ACTIVE;
	}

	/**
	 * Check if a bundle is in the RESOLVED state
	 *
	 * @param b the bundle to check
	 */
	public boolean isResolved(Bundle b) {
		return b.getState() == Bundle.RESOLVED;
	}

	/**
	 * Check if a bundle is in the INSTALLED state
	 *
	 * @param b the bundle to check
	 */
	public boolean isInstalled(Bundle b) {
		return b.getState() == Bundle.INSTALLED;
	}

	/**
	 * Check if a bundle is in the UNINSTALLED state
	 *
	 * @param b the bundle to check
	 */
	public boolean isUninstalled(Bundle b) {
		return b.getState() == Bundle.UNINSTALLED;
	}

	/**
	 * Check if a bundle is in the STARTING state
	 *
	 * @param b the bundle to check
	 */
	public boolean isStarting(Bundle b) {
		return b.getState() == Bundle.STARTING;
	}

	/**
	 * Check if a bundle is in the STOPPING state
	 *
	 * @param b the bundle to check
	 */
	public boolean isStopping(Bundle b) {
		return b.getState() == Bundle.STOPPING;
	}

	/**
	 * Check if a bundle is in the ACTIVE or STARTING state
	 *
	 * @param b the bundle to check
	 */
	public boolean isRunning(Bundle b) {
		return isActive(b) || isStarting(b);
	}

	/**
	 * Check if a bundle is in the RESOLVED or ACTIVE or STARTING state
	 *
	 * @param b the bundle to check
	 */
	public boolean isReady(Bundle b) {
		return isResolved(b) || isActive(b) || isStarting(b);
	}

	private ParameterMap getExports(Bundle b) {
		return new ParameterMap(b.getHeaders()
			.get(Constants.EXPORT_PACKAGE));
	}

	private ParameterMap getImports(Bundle b) {
		return new ParameterMap(b.getHeaders()
			.get(Constants.IMPORT_PACKAGE));
	}

	private String toInstallURI(File c) {
		if (byReference)
			return "reference:" + c.toURI();
		return c.toURI()
			.toString();
	}

	Object getService(Injector.Target<Service> param) {

		try {
			if (param.type == Launchpad.class) {
				return this;
			}
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

			long timeout = service.timeout();
			if (timeout <= 0)
				timeout = SERVICE_DEFAULT_TIMEOUT;

			boolean multiple = isMultiple(param.type);
			int cardinality = multiple ? service.minimum() : 1;

			List<? extends ServiceReference<?>> matchedReferences = getServices(serviceClass, target, cardinality,
				timeout, true);

			if (multiple)
				return matchedReferences;
			else
				return matchedReferences.get(0);

		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@SuppressWarnings({
		"rawtypes", "unchecked"
	})
	public <T> List<ServiceReference<T>> getServices(Class<T> serviceClass, @Nullable String target, int cardinality,
		long timeout, boolean exception) {
		try {

			String className = serviceClass.getName();

			ServiceTracker<?, ?> tracker = injectedDoNotClose.computeIfAbsent(serviceClass, (c) -> {
				ServiceTracker<?, ?> t = new ServiceTracker<>(framework.getBundleContext(), className, null);
				t.open(true);
				return t;
			});

			long deadline = System.currentTimeMillis() + timeout;

			while (true) {

				// we get the ALL services regardless of class space or hidden
				// by hooks or filters.

				@SuppressWarnings("unchecked")
				List<ServiceReference<T>> allReferences = (List<ServiceReference<T>>) getReferences(tracker,
					serviceClass);

				List<ServiceReference<T>> visibleReferences = allReferences.stream()
					.filter(ref -> ref.isAssignableTo(proxyBundle, className))
					.collect(Collectors.toList());

				List<ServiceReference<T>> unhiddenReferences = new ArrayList<>(visibleReferences);
				Map<ServiceReference<T>, FindHook> hookMap = new HashMap<>();

				for (FindHook hook : this.hooks.getServices(new FindHook[0])) {
					List<ServiceReference<T>> original = new ArrayList<>(unhiddenReferences);
					hook.find(testbundle.getBundleContext(), className, target, true, (Collection) unhiddenReferences);
					original.removeAll(unhiddenReferences);
					for (ServiceReference<T> ref : original) {
						hookMap.put(ref, hook);
					}
				}

				List<ServiceReference<T>> matchedReferences;

				if (target == null) {
					matchedReferences = new ArrayList<>(unhiddenReferences);
				} else {
					Filter filter = framework.getBundleContext()
						.createFilter(target);
					matchedReferences = visibleReferences.stream()
						.filter(filter::match)
						.collect(Collectors.toList());
				}

				if (cardinality <= matchedReferences.size()) {
					return matchedReferences;
				}

				if (deadline < System.currentTimeMillis()) {
					String error = "Injection of service " + className;
					if (target != null)
						error += " with target " + target;

					error += " failed.";

					if (allReferences.size() > visibleReferences.size()) {
						List<ServiceReference<?>> invisibleReferences = new ArrayList<>(allReferences);
						invisibleReferences.removeAll(visibleReferences);
						for (ServiceReference<?> r : invisibleReferences) {
							error += "\nInvisible reference " + r + "[" + r.getProperty(Constants.SERVICE_ID)
								+ "] from bundle " + r.getBundle();

							String[] objectClass = (String[]) r.getProperty(Constants.OBJECTCLASS);
							for (String clazz : objectClass) {
								error += "\n  " + clazz + "\n     registrar: "
									+ getSource(clazz, r.getBundle()).orElse("null") + "\n     proxybundle: "
									+ getSource(clazz, proxyBundle).orElse("null");
							}
						}
					}

					if (visibleReferences.size() > unhiddenReferences.size()) {
						List<ServiceReference<?>> hiddenReferences = new ArrayList<>(visibleReferences);
						hiddenReferences.removeAll(unhiddenReferences);
						for (ServiceReference<?> r : hiddenReferences) {
							error += "\nHidden (FindHook) Reference " + r + " from bundle " + r.getBundle() + " hook "
								+ hookMap.get(r);
						}
					}

					if (unhiddenReferences.size() > matchedReferences.size()) {
						List<ServiceReference<?>> untargetReferences = new ArrayList<>(unhiddenReferences);
						untargetReferences.removeAll(matchedReferences);
						error += "\nReference not matched by the target filter " + target;
						for (ServiceReference<?> ref : untargetReferences) {
							error += "\n  " + ref + " : " + getProperties(ref);
						}
					}

					if (exception && timeout > 1)
						throw new TimeoutException(error);

					return Collections.emptyList();
				}
				Thread.sleep(100);
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private Map<String, String> getProperties(ServiceReference<?> ref) {
		Map<String, String> map = new HashMap<>();
		for (String k : ref.getPropertyKeys()) {
			Object property = ref.getProperty(k);
			String s;
			if (property != null && property.getClass()
				.isArray()) {
				s = Arrays.deepToString((Object[]) property);
			} else
				s = property + "";

			map.put(k, s);
		}
		return map;
	}

	private Optional<String> getSource(String className, Bundle from) {
		try {
			Class<?> loadClass = from.loadClass(className);
			Bundle bundle = FrameworkUtil.getBundle(loadClass);

			if (bundle == null)
				return Optional.of("from class path");
			else {
				BundleWiring wiring = bundle.adapt(BundleWiring.class);
				String exported = "PRIVATE! ";
				List<BundleCapability> capabilities = wiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
				String packageName = loadClass.getPackage()
					.getName();

				for (BundleCapability c : capabilities) {
					if (packageName.equals(c.getAttributes()
						.get(PackageNamespace.PACKAGE_NAMESPACE))) {
						exported = "Exported from ";
					}
				}

				return Optional.of(exported + " " + bundle.toString());
			}
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	void reportEvents() {
		frameworkEvents.forEach(fe -> {
			report("%s", fe);
		});
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

	private List<? extends ServiceReference<?>> getReferences(ServiceTracker<?, ?> tracker, Class<?> serviceClass) {
		ServiceReference<?>[] references = tracker.getServiceReferences();
		if (references == null) {
			return Collections.emptyList();
		}
		Arrays.sort(references);
		return Arrays.asList(references);
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

				Object service = getService(reference);

				if (isParameterizedType(to, Optional.class))
					return Optional.ofNullable(service);

				return service;
			} catch (Exception e) {
				throw e;
			}
		});
		return converter;
	}

	public String getName() {
		return name;
	}

	public String getClassName() {
		return className;
	}

	public Closeable enable(Class<?> componentClass) {
		return probe.enable(componentClass);
	}

	public void setProxyBundle(Bundle tb) {
		this.proxyBundle = tb;
	}

	public Launchpad setProbe(Probe probe) {
		try {
			inject(probe);
		} catch (NoClassDefFoundError e) {
			// ignore
			return this;
		} catch (Exception e) {
			// ignore
			return this;
		}
		this.probe = probe;
		return this;
	}

	public void sync() {
		startlevels.sync();
	}
}

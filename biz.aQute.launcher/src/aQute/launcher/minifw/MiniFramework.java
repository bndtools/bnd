package aQute.launcher.minifw;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;

public class MiniFramework implements Framework, Bundle, BundleContext {
	final ClassLoader				loader;
	private final Properties		properties;
	private Map<Long, Bundle>		bundles	= new HashMap<>();
	private final AtomicInteger		ID		= new AtomicInteger(0);
	int								state	= Bundle.RESOLVED;
	ClassLoader						last;
	final FrameworkStartLevelImpl	frameworkStartLevel;
	private Tracing					tracing	= Tracing.noop;
	private final Manifest			manifest;
	private long					lastModified;

	public MiniFramework(Map<Object, Object> properties) {
		this.properties = new Properties(System.getProperties());
		this.properties.putAll(properties);

		bundles.put(Long.valueOf(0), getBundle());
		last = loader = getClass().getClassLoader();
		frameworkStartLevel = new FrameworkStartLevelImpl(this);
		manifest = new Manifest();
		manifest.getMainAttributes()
			.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.getMainAttributes()
			.putValue(Constants.BUNDLE_SYMBOLICNAME, "Mini framework");
		manifest.getMainAttributes()
			.putValue(Constants.BUNDLE_VERSION, "1.8.0");
		lastModified = System.currentTimeMillis();
	}

	public MiniFramework setTracing(Tracing tracing) {
		this.tracing = tracing;
		return this;
	}

	void trace(String msg, Object... objects) {
		tracing.trace(msg, objects);
	}

	@Override
	public void init() throws BundleException {
		state = Bundle.STARTING;
	}

	@Override
	public void init(FrameworkListener... listeners) throws BundleException {
		init();
	}

	@Override
	public FrameworkEvent waitForStop(long timeout) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeout;

		while (state != Bundle.UNINSTALLED) {
			if (timeout != 0) {
				long wait = deadline - System.currentTimeMillis();
				if (wait <= 0)
					return new FrameworkEvent(FrameworkEvent.WAIT_TIMEDOUT, getBundle(), null);
			}
			Thread.sleep(100);
		}
		return new FrameworkEvent(FrameworkEvent.STOPPED, getBundle(), null);
	}

	@Override
	public BundleContext getBundleContext() {
		return this;
	}

	@Override
	public long getBundleId() {
		return 0;
	}

	@Override
	public URL getEntry(String path) {
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		return loader.getResource(path);
	}

	@Override
	public Enumeration<String> getEntryPaths(String path) {
		return null;
	}

	@Override
	public Dictionary<String, String> getHeaders() {
		return new Headers(manifest);
	}

	@Override
	public Dictionary<String, String> getHeaders(String locale) {
		return new Headers(manifest);
	}

	@Override
	public long getLastModified() {
		return lastModified;
	}

	@Override
	public String getLocation() {
		return "System Bundle";
	}

	@Override
	public URL getResource(String name) {
		return loader.getResource(name);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		return loader.getResources(name);
	}

	@Override
	public int getState() {
		return state;
	}

	@Override
	public String getSymbolicName() {
		return "system.bundle";
	}

	@Override
	public Version getVersion() {
		return new Version("1.0");
	}

	@Override
	public boolean hasPermission(Object permission) {
		return true;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return loader.loadClass(name);
	}

	@Override
	public void start() {
		start(0);
	}

	@Override
	public void start(int options) {
		state = Bundle.ACTIVE;
	}

	@Override
	public void stop() {
		stop(0);
	}

	@Override
	public synchronized void stop(int options) {
		state = Bundle.RESOLVED;
		notifyAll();
	}

	@Override
	public Bundle getBundle() {
		return this;
	}

	@Override
	public Bundle getBundle(long id) {
		Long l = Long.valueOf(id);
		Bundle b = bundles.get(l);
		return b;
	}

	@Override
	public Bundle[] getBundles() {
		return bundles.values()
			.toArray(new Bundle[0]);
	}

	@Override
	public File getDataFile(String filename) {
		return null;
	}

	@Override
	public String getProperty(String key) {
		return properties.getProperty(key);
	}

	@Override
	public Bundle installBundle(String location, InputStream in) throws BundleException {
		try {
			if (in != null) {
				in.close();
			}
			if (location.startsWith("reference:")) {
				location = location.substring("reference:".length());
			}
			if (location.startsWith("file:")) {
				location = new File(new URL(location).toURI()).getAbsolutePath();
			} else {
				try {
					@SuppressWarnings("unused")
					URL url = new URL(location);
				} catch (MalformedURLException e) {
					throw new BundleException(
						"For the mini framework, the location must be a proper URL even though this is not required by the specification "
							+ location,
						e);
				}
			}

			while (location.startsWith("//")) {
				location = location.substring(1);
			}

			MiniBundle c = new MiniBundle(this, last, ID.incrementAndGet(), location);
			bundles.put(Long.valueOf(c.getBundleId()), c);
			last = c.getClassLoader();
			lastModified = c.getLastModified();
			return c;
		} catch (Exception e) {
			throw new BundleException("Failed to install", e);
		}
	}

	@Override
	public Bundle installBundle(String location) throws BundleException {
		return installBundle(location, null);
	}

	@Override
	public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
		return null;
	}

	@Override
	public ServiceReference<?>[] getRegisteredServices() {
		return null;
	}

	@Override
	public ServiceReference<?>[] getServicesInUse() {
		return null;
	}

	@Override
	public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
		return new HashMap<>();
	}

	@Override
	public void uninstall() throws BundleException {
		throw new BundleException("Cannot uninstall framework", BundleException.UNSUPPORTED_OPERATION);
	}

	@Override
	public void update() throws BundleException {
		throw new BundleException("Cannot update mini framework", BundleException.UNSUPPORTED_OPERATION);
	}

	@Override
	public void update(InputStream in) throws BundleException {
		throw new BundleException("Cannot update mini framework", BundleException.UNSUPPORTED_OPERATION);
	}

	@Override
	public void addBundleListener(BundleListener listener) {
		// no services so cannot do any harm
	}

	@Override
	public void addFrameworkListener(FrameworkListener listener) {
		// no services so cannot do any harm
	}

	@Override
	public void addServiceListener(ServiceListener listener) {
		// no services so cannot do any harm
	}

	@Override
	public void addServiceListener(ServiceListener listener, String filter) {
		// no services so cannot do any harm
	}

	@Override
	public Filter createFilter(String filter) throws InvalidSyntaxException {
		return FrameworkUtil.createFilter(filter);
	}

	@Override
	public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		return null;
	}

	@Override
	public ServiceReference<?> getServiceReference(String clazz) {
		return null;
	}

	@Override
	public ServiceReference<?>[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		return null;
	}

	@Override
	public void removeBundleListener(BundleListener listener) {
		// ok
	}

	@Override
	public void removeFrameworkListener(FrameworkListener listener) {
		// ok
	}

	@Override
	public void removeServiceListener(ServiceListener listener) {
		// ok
	}

	@Override
	public String toString() {
		return "0 Mini framework";
	}

	@Override
	public int compareTo(Bundle other) {
		return Long.signum(getBundleId() - other.getBundleId());
	}

	@Override
	public ServiceRegistration<?> registerService(String[] clazzes, Object service, Dictionary<String, ?> properties) {
		return null;
	}

	@Override
	public ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties) {
		return null;
	}

	@Override
	public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
		return null;
	}

	@Override
	public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
		return null;
	}

	@Override
	public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter)
		throws InvalidSyntaxException {
		return null;
	}

	@Override
	public <S> S getService(ServiceReference<S> reference) {
		return null;
	}

	@Override
	public boolean ungetService(ServiceReference<?> reference) {
		return false;
	}

	@Override
	public Bundle getBundle(String location) {
		return null;
	}

	@Override
	public <A> A adapt(Class<A> type) {
		if (BundleRevision.class.equals(type)) {
			return type.cast(new BundleRevisionImpl(getBundle()));
		}
		if (BundleWiring.class.equals(type)) {
			return type.cast(new BundleWiringImpl(getBundle(), loader));
		}
		if (BundleStartLevel.class.equals(type)) {
			return type.cast(new BundleStartLevelImpl(getBundle(), frameworkStartLevel));
		}
		if (FrameworkStartLevel.class.equals(type)) {
			return type.cast(frameworkStartLevel);
		}
		if (FrameworkWiring.class.equals(type)) {
			return type.cast(new FrameworkWiringImpl(this));
		}
		trace("### No adaptation for adapt(%s) %s", type, this);
		return null;
	}

	@Override
	public <S> ServiceRegistration<S> registerService(Class<S> clazz, ServiceFactory<S> factory,
		Dictionary<String, ?> properties) {
		return null;
	}

	@Override
	public <S> ServiceObjects<S> getServiceObjects(ServiceReference<S> reference) {
		return null;
	}

}

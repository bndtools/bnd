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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
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

public class MiniFramework implements Framework, Bundle, BundleContext {
	ClassLoader			loader;
	Properties			properties;
	Map<Long, Bundle>	bundles	= new HashMap<>();
	int					ID		= 0;
	int					state	= Bundle.INSTALLED;
	ClassLoader			last;

	public MiniFramework(Map<Object, Object> properties) {
		this.properties = new Properties(System.getProperties());
		this.properties.putAll(properties);

		bundles.put(Long.valueOf(0), this);
		last = loader = getClass().getClassLoader();
	}

	@Override
	public void init() throws BundleException {
		state = Bundle.ACTIVE;
	}

	@Override
	public FrameworkEvent waitForStop(long timeout) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeout;

		while (state != Bundle.UNINSTALLED) {
			if (timeout != 0) {
				long wait = deadline - System.currentTimeMillis();
				if (wait <= 0)
					return new FrameworkEvent(FrameworkEvent.WAIT_TIMEDOUT, this, null);
			}
			Thread.sleep(100);
		}
		return new FrameworkEvent(FrameworkEvent.STOPPED, this, null);
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
		if (path.startsWith("/"))
			path = path.substring(1);
		return loader.getResource(path);
	}

	@Override
	public Enumeration<String> getEntryPaths(String path) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Dictionary<String, String> getHeaders() {
		return new Hashtable<>();
	}

	@Override
	public Dictionary<String, String> getHeaders(String locale) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long getLastModified() {
		return 0;
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
		return Bundle.ACTIVE;
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
	public void start() {}

	@Override
	public void start(int options) {}

	@Override
	public synchronized void stop() {
		state = Bundle.UNINSTALLED;
		notifyAll();
	}

	@Override
	public void stop(int options) throws BundleException {}

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
	public Bundle installBundle(String location) throws BundleException {
		try {
			if (location.startsWith("reference:"))
				location = new File(new URL(location.substring("reference:".length())).toURI()).getPath();
			else if (location.startsWith("file:"))
				location = new File(location.substring("file:".length())).getPath();

			while (location.startsWith("//"))
				location = location.substring(1);

			Context c = new Context(this, last, ++ID, location);
			bundles.put(Long.valueOf(c.id), c);
			last = c;
			return c;
		} catch (Exception e) {
			throw new BundleException("Failed to install", e);
		}
	}

	@Override
	public Bundle installBundle(String location, InputStream in) throws BundleException {
		Context c;
		try {
			in.close();
			try {
				@SuppressWarnings("unused")
				URL url = new URL(location);
			} catch (MalformedURLException e) {
				throw new BundleException(
					"For the mini framework, the location must be a proper URL even though this is not required by the specification "
						+ location,
					e);
			}
			c = new Context(this, last, ++ID, location);
			bundles.put(Long.valueOf(c.id), c);
			last = c;
			return c;
		} catch (Exception e) {
			throw new BundleException("Can't install " + location, e);
		}
	}

	@Override
	public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ServiceReference<?>[] getRegisteredServices() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ServiceReference<?>[] getServicesInUse() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void uninstall() throws BundleException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void update() throws BundleException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void update(InputStream in) throws BundleException {
		throw new UnsupportedOperationException();
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
		throw new UnsupportedOperationException();
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
		return "Mini framework";
	}

	class Loader extends ClassLoader {
		@Override
		public Class<?> findClass(String name) throws ClassNotFoundException {
			for (Bundle b : bundles.values()) {
				try {
					return b.loadClass(name);
				} catch (ClassNotFoundException e) {
					// Ignore, try next
				}
			}
			throw new ClassNotFoundException(name);
		}
	}

	public int compareTo(Bundle var0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ServiceRegistration<?> registerService(String[] clazzes, Object service, Dictionary<String, ?> properties) {
		return null;
	}

	@Override
	public ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties) {
		return null;
	}

	public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
		return null;
	}

	public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
		return null;
	}

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

	public Bundle getBundle(String location) {
		return null;
	}

	public <A> A adapt(Class<A> type) {
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

	@Override
	public void init(FrameworkListener... listeners) throws BundleException {

	}
}

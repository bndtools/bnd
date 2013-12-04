package aQute.launcher.minifw;

import java.io.*;
import java.net.*;
import java.security.cert.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.framework.launch.*;

public class MiniFramework implements Framework, Bundle, BundleContext {
	ClassLoader			loader;
	Properties			properties;
	Map<Long,Bundle>	bundles	= new HashMap<Long,Bundle>();
	int					ID		= 1;
	int					state	= Bundle.INSTALLED;
	ClassLoader			last;

	public MiniFramework(Map<Object,Object> properties) {
		this.properties = new Properties(System.getProperties());
		this.properties.putAll(properties);

		bundles.put(new Long(0), this);
		last = loader = getClass().getClassLoader();
	}

	public void init() throws BundleException {
		state = Bundle.ACTIVE;
	}

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

	public BundleContext getBundleContext() {
		return this;
	}

	public long getBundleId() {
		return 0;
	}

	public URL getEntry(String path) {
		if (path.startsWith("/"))
			path = path.substring(1);
		return loader.getResource(path);
	}

	public Enumeration< String > getEntryPaths(String path) {
		throw new UnsupportedOperationException();
	}

	public Dictionary<String,String> getHeaders() {
		return new Hashtable<String,String>();
	}

	public Dictionary< String , String > getHeaders(String locale) {
		throw new UnsupportedOperationException();
	}

	public long getLastModified() {
		return 0;
	}

	public String getLocation() {
		return "System Bundle";
	}

	public URL getResource(String name) {
		return loader.getResource(name);
	}

	public Enumeration<URL> getResources(String name) throws IOException {
		return loader.getResources(name);
	}

	public int getState() {
		return Bundle.ACTIVE;
	}

	public String getSymbolicName() {
		return "system.bundle";
	}

	public Version getVersion() {
		return new Version("1.0");
	}

	public boolean hasPermission(Object permission) {
		return true;
	}

	public Class< ? > loadClass(String name) throws ClassNotFoundException {
		return loader.loadClass(name);
	}

	public void start() {}

	public void start(int options) {}

	public synchronized void stop() {
		state = Bundle.UNINSTALLED;
		notifyAll();
	}

	public void stop(int options) throws BundleException {}

	public Bundle getBundle() {
		return this;
	}

	public Bundle getBundle(long id) {
		Long l = new Long(id);
		Bundle b = bundles.get(l);
		return b;
	}

	public Bundle[] getBundles() {
		Bundle[] bs = new Bundle[bundles.size()];
		return bundles.values().toArray(bs);
	}

	public File getDataFile(String filename) {
		return null;
	}

	public String getProperty(String key) {
		return properties.getProperty(key);
	}

	public Bundle installBundle(String location) throws BundleException {
		try {
			if (location.startsWith("reference:"))
				location = new File(new URL(location.substring("reference:".length())).toURI()).getPath();
			else if (location.startsWith("file:"))
				location = new File(location.substring("file:".length())).getPath();

			while (location.startsWith("//"))
				location = location.substring(1);

			Context c = new Context(this, last, ++ID, location);
			bundles.put(new Long(c.id), c);
			last = c;
			return c;
		}
		catch (Exception e) {
			throw new BundleException("Failed to install", e);
		}
	}

	public Bundle installBundle(String location, InputStream in) throws BundleException {
		Context c;
		try {
			in.close();
			try {
				@SuppressWarnings("unused")
				URL url = new URL(location);
			}
			catch (MalformedURLException e) {
				throw new BundleException(
						"For the mini framework, the location must be a proper URL even though this is not required by the specification "
								+ location, e);
			}
			c = new Context(this, last, ++ID, location);
			bundles.put(new Long(c.id), c);
			last = c;
			return c;
		}
		catch (Exception e) {
			throw new BundleException("Can't install " + location, e);
		}
	}

	public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
		throw new UnsupportedOperationException();
	}

	public ServiceReference[] getRegisteredServices() {
		throw new UnsupportedOperationException();
	}

	public ServiceReference[] getServicesInUse() {
		throw new UnsupportedOperationException();
	}

	public Map<X509Certificate,List<X509Certificate>> getSignerCertificates(int signersType) {
		throw new UnsupportedOperationException();
	}

	public void uninstall() throws BundleException {
		throw new UnsupportedOperationException();
	}

	public void update() throws BundleException {
		throw new UnsupportedOperationException();
	}

	public void update(InputStream in) throws BundleException {
		throw new UnsupportedOperationException();
	}

	public void addBundleListener(BundleListener listener) {
		// no services so cannot do any harm
	}

	public void addFrameworkListener(FrameworkListener listener) {
		// no services so cannot do any harm
	}

	public void addServiceListener(ServiceListener listener) {
		// no services so cannot do any harm
	}

	public void addServiceListener(ServiceListener listener, String filter) {
		// no services so cannot do any harm
	}

	public Filter createFilter(String filter) throws InvalidSyntaxException {
		return FrameworkUtil.createFilter(filter);
	}

	public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		throw new UnsupportedOperationException();
	}


	public ServiceReference getServiceReference(String clazz) {
		return null;
	}

	public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		return null;
	}


	public void removeBundleListener(BundleListener listener) {
		// ok
	}

	public void removeFrameworkListener(FrameworkListener listener) {
		// ok
	}

	public void removeServiceListener(ServiceListener listener) {
		// ok
	}

	@Override
	public String toString() {
		return "Mini framework";
	}

	class Loader extends ClassLoader {
		@Override
		public Class< ? > findClass(String name) throws ClassNotFoundException {
			for (Bundle b : bundles.values()) {
				try {
					return b.loadClass(name);
				}
				catch (ClassNotFoundException e) {
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

	public ServiceRegistration< ? > registerService(String[] clazzes, Object service, Dictionary<String, ? > properties) {
		// TODO Auto-generated method stub
		return null;
	}

	public ServiceRegistration< ? > registerService(String clazz, Object service, Dictionary<String, ? > properties) {
		// TODO Auto-generated method stub
		return null;
	}

	public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ? > properties) {
		// TODO Auto-generated method stub
		return null;
	}

	public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter)
			throws InvalidSyntaxException {
		// TODO Auto-generated method stub
		return null;
	}

	public <S> S getService(ServiceReference<S> reference) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean ungetService(ServiceReference< ? > reference) {
		// TODO Auto-generated method stub
		return false;
	}

	public Bundle getBundle(String location) {
		// TODO Auto-generated method stub
		return null;
	}

	public <A> A adapt(Class<A> type) {
		// TODO Auto-generated method stub
		return null;
	}
}

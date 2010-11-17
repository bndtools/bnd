package aQute.launcher.minifw;

import java.io.*;
import java.net.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.framework.launch.*;

public class MiniFramework implements Framework, Bundle, BundleContext {
	ClassLoader	loader;
	Properties	properties;
	Map			bundles	= new HashMap();
	int			ID		= 1;
	int			state	= Bundle.INSTALLED;
	ClassLoader	last;

	public MiniFramework(Map properties) {
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
			long wait = deadline - System.currentTimeMillis();
			if (wait <= 0)
				return new FrameworkEvent(FrameworkEvent.WAIT_TIMEDOUT, this, null);
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
    	if ( path.startsWith("/"))
    		path = path.substring(1);
		return loader.getResource(path);
	}

	public Enumeration getEntryPaths(String path) {
		throw new UnsupportedOperationException();
	}

	public Dictionary getHeaders() {
		return new Hashtable();
	}

	public Dictionary getHeaders(String locale) {
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

	public Enumeration getResources(String name) throws IOException {
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

	public Class loadClass(String name) throws ClassNotFoundException {
		return loader.loadClass(name);
	}

	public void start() {
	}

	public void start(int options) {
	}

	public synchronized void stop() {
		state = Bundle.UNINSTALLED;
		notifyAll();
	}

	public void stop(int options) throws BundleException {
	}

	public Bundle getBundle() {
		return this;
	}

	public Bundle getBundle(long id) {
		Long l = new Long(id);
		Bundle b = (Bundle) bundles.get(l);
		return b;
	}

	public Bundle[] getBundles() {
		Bundle[] bs = new Bundle[bundles.size()];
		return (Bundle[]) bundles.values().toArray(bs);
	}

	public File getDataFile(String filename) {
		return null;
	}

	public String getProperty(String key) {
		return properties.getProperty(key);
	}

	public Bundle installBundle(String location) throws BundleException {
		if (location.startsWith("reference:")) 
			location = location.substring("reference:".length()).trim();

		if (location.startsWith("file:")) 
			location = location.substring("file:".length()).trim();

		while ( location.startsWith("//"))
			location = location.substring(1);
		
		try {
			Context c = new Context(this, last, ++ID, location);
			bundles.put(new Long(c.id), c);
			last = c;
			return c;
		} catch (Exception e) {
			throw new BundleException("Failed to install", e);
		}
	}

	public Bundle installBundle(String location, InputStream in) throws BundleException {
		Context c;
		try {
			in.close();
			try {
				URL url = new URL(location);
			} catch (MalformedURLException e) {
				throw new BundleException("For the mini framework, the location must be a proper URL even though this is not required by the specification " + location, e);
			}
			c = new Context(this, last, ++ID, location);
			bundles.put(new Long(c.id), c);
			last = c;
			return c;
		} catch (Exception e) {
			throw new BundleException("Can't install " + location, e);
		}
	}

	public Enumeration findEntries(String path, String filePattern, boolean recurse) {
		throw new UnsupportedOperationException();
	}

	public ServiceReference[] getRegisteredServices() {
		throw new UnsupportedOperationException();
	}

	public ServiceReference[] getServicesInUse() {
		throw new UnsupportedOperationException();
	}

	public Map getSignerCertificates(int signersType) {
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

	public ServiceReference[] getAllServiceReferences(String clazz, String filter)
			throws InvalidSyntaxException {
		throw new UnsupportedOperationException();
	}

	public Object getService(ServiceReference reference) {
		return null;
	}

	public ServiceReference getServiceReference(String clazz) {
		return null;
	}

	public ServiceReference[] getServiceReferences(String clazz, String filter)
			throws InvalidSyntaxException {
		return null;
	}

	public ServiceRegistration registerService(String[] clazzes, Object service,
			Dictionary properties) {
		throw new UnsupportedOperationException();
	}

	public ServiceRegistration registerService(String clazz, Object service, Dictionary properties) {
		throw new UnsupportedOperationException();
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

	public boolean ungetService(ServiceReference reference) {
		throw new UnsupportedOperationException();
	}

	public String toString() {
		return "Mini framework";
	}

	class Loader extends ClassLoader {
		public Class findClass(String name) throws ClassNotFoundException {
			for (Iterator i = bundles.values().iterator(); i.hasNext();) {
				Bundle b = (Bundle) i;
				try {
					return b.loadClass(name);
				} catch (ClassNotFoundException e) {
					// Ignore, try next
				}
			}
			throw new ClassNotFoundException(name);
		}
	}
}

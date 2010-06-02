package aQute.miniframework;

import java.io.*;
import java.net.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.framework.launch.*;

public class MiniFramework implements Framework, Bundle {
	ClassLoader								loader;
	Map<String, String>						properties;
	Map<Long, Bundle>						bundles		= new HashMap<Long, Bundle>();
	int										ID			= 1;
	ClassLoader								last;
	final List<LocalServiceRegistration>	registry	= new ArrayList<LocalServiceRegistration>();
	final Context							systemBundle;
	int										serviceId	= 0;

	public MiniFramework(Map properties) throws IOException {
		this.properties = properties;
		bundles.put(new Long(0), this);
		last = loader = getClass().getClassLoader();
		systemBundle = new Context(this, loader, 0, "System Bundle");
	}

	public void init() throws BundleException {

	}

	public FrameworkEvent waitForStop(long timeout) throws InterruptedException {
		return null;
	}

	public BundleContext getBundleContext() {
		return systemBundle;
	}

	public long getBundleId() {
		return 0;
	}

	public URL getEntry(String path) {
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

	public void stop() {
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
		return null;
	}

	public Bundle installBundle(String location) throws BundleException {
		throw new UnsupportedOperationException();
	}

	public Bundle installBundle(String location, InputStream in) throws BundleException {
		Context c;
		try {
			c = new Context(this, last, ++ID, location);
			bundles.put(new Long(c.id), c);
			last = c;
			return c;
		} catch (IOException e) {
			throw new BundleException("Can't install " + location, e);
		}
	}

	public Enumeration findEntries(String path, String filePattern, boolean recurse) {
		throw new UnsupportedOperationException();
	}

	public ServiceReference[] getRegisteredServices() {
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
		throw new UnsupportedOperationException();
	}

	public void addFrameworkListener(FrameworkListener listener) {
		throw new UnsupportedOperationException();
	}

	public void addServiceListener(ServiceListener listener) {
		throw new UnsupportedOperationException();
	}

	public void addServiceListener(ServiceListener listener, String filter) {
		throw new UnsupportedOperationException();
	}

	public Filter createFilter(String filter) throws InvalidSyntaxException {
		throw new UnsupportedOperationException();
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
		throw new UnsupportedOperationException();
	}

	public void removeFrameworkListener(FrameworkListener listener) {
		throw new UnsupportedOperationException();
	}

	public void removeServiceListener(ServiceListener listener) {
		throw new UnsupportedOperationException();
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

	public void removeRegistration(LocalServiceRegistration localServiceRegistration) {
		// TODO Auto-generated method stub

	}

	public synchronized int nextId() {
		return ++serviceId;
	}
}

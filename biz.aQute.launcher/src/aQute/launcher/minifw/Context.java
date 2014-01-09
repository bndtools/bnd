package aQute.launcher.minifw;

import java.io.*;
import java.net.*;
import java.security.cert.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

import org.osgi.framework.*;

public class Context extends URLClassLoader implements Bundle, BundleContext, BundleReference {
	long					id;
	MiniFramework			fw;
	String					location;
	int						state	= Bundle.INSTALLED;
	JarFile					jar;
	Manifest				manifest;
	private TreeSet<String>	paths;
	private File			jarFile;

	class Dict extends Dictionary<String,String> {

		@Override
		public Enumeration<String> elements() {
			Enumeration<?> enumeration = Collections.enumeration(manifest.getMainAttributes().values());
			return (Enumeration<String>) enumeration;
		}

		@Override
		public String get(Object key) {
			String o = manifest.getMainAttributes().getValue((String) key);
			return o;
		}

		@Override
		public boolean isEmpty() {
			return manifest.getMainAttributes().isEmpty();
		}

		@Override
		public Enumeration<String> keys() {
			Vector<String> v = new Vector<String>();
			for (Iterator<Object> i = manifest.getMainAttributes().keySet().iterator(); i.hasNext();) {
				Attributes.Name name = (Attributes.Name) i.next();
				v.add(name.toString());
			}
			return v.elements();
		}

		@Override
		public String put(String key, String value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String remove(Object key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int size() {
			return manifest.getMainAttributes().size();
		}

	}

	public Context(MiniFramework fw, ClassLoader parent, int id, String location) throws Exception {
		super(new URL[] {
			new File(location).toURI().toURL()
		}, parent);
		this.fw = fw;
		this.id = id;
		this.location = location;

		jar = new JarFile(jarFile = new File(location));
		// Enumeration<JarEntry> entries = jar.entries();
		// while ( entries.hasMoreElements())
		// System.err.println(entries.nextElement().getName());

		manifest = jar.getManifest();
		jar.close();
	}

	public BundleContext getBundleContext() {
		return this;
	}

	public long getBundleId() {
		return id;
	}

	public URL getEntry(String path) {
		if (path.startsWith("/"))
			path = path.substring(1);
		return getResource(path);
	}

	public Enumeration<String> getEntryPaths(String path) {
		throw new UnsupportedOperationException();
	}

	public Dictionary<String,String> getHeaders() {
		return new Dict();
	}

	public Dictionary<String,String> getHeaders(String locale) {
		return new Dict();
	}

	public long getLastModified() {
		return jarFile.lastModified();
	}

	public String getLocation() {
		return location;
	}

	public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {

		try {
			if (path.startsWith("/"))
				path = path.substring(1);
			if (!path.endsWith("/"))
				path += "/";

			Vector<URL> paths = new Vector<URL>();
			for (Iterator<String> i = getPaths().iterator(); i.hasNext();) {
				String entry = i.next();
				if (entry.startsWith(path)) {
					if (recurse || entry.indexOf('/', path.length()) < 0) {
						if (filePattern == null || matches(entry, filePattern)) {
							URL url = getResource(entry);
							if (url == null) {
								System.err.println("Cannot load resource that should be there: " + entry);
							} else
								paths.add(url);
						}
					}
				}
			}
			return paths.elements();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean matches(String path, String filePattern) {
		do {
			int part = filePattern.indexOf('*');
			if (part < 0) {
				return path.indexOf(filePattern) >= 0;
			}
			String match = filePattern.substring(0, part);
			int m = path.indexOf(match);
			if (m < 0)
				return false;

			path = path.substring(m + match.length());
			filePattern = filePattern.substring(part + 1);
		} while (true);
	}

	private Collection<String> getPaths() throws Exception {
		if (paths != null)
			return paths;

		paths = new TreeSet<String>();
		JarFile jar = new JarFile(new File(location));
		try {
			for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
				ZipEntry entry = e.nextElement();
				paths.add(entry.getName());
			}
		}
		finally {
			jar.close();
		}
		return paths;
	}

	public ServiceReference[] getRegisteredServices() {
		return null;
	}

	public ServiceReference[] getServicesInUse() {
		return null;
	}

	public Map<X509Certificate,List<X509Certificate>> getSignerCertificates(int signersType) {
		throw new UnsupportedOperationException();
	}

	public int getState() {
		return state;
	}

	public String getSymbolicName() {
		return ((String) getHeaders().get("Bundle-SymbolicName")).trim();
	}

	public Version getVersion() {
		String v = ((String) getHeaders().get("Bundle-Version")).trim();
		if (v == null)
			return new Version("0");
		return new Version(v);
	}

	public boolean hasPermission(Object permission) {
		return true;
	}

	public void start() throws BundleException {
		state = Bundle.ACTIVE;
	}

	public void start(int options) throws BundleException {
		state = Bundle.ACTIVE;
	}

	public void stop() throws BundleException {
		state = Bundle.RESOLVED;
	}

	public void stop(int options) throws BundleException {
		state = Bundle.RESOLVED;
	}

	public void uninstall() throws BundleException {
		state = Bundle.UNINSTALLED;
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

	public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		throw new UnsupportedOperationException();
	}

	public Bundle getBundle() {
		return this;
	}

	public Bundle getBundle(long id) {
		return fw.getBundle(id);
	}

	public Bundle[] getBundles() {
		return fw.getBundles();
	}

	public File getDataFile(String filename) {
		return null;
	}

	public String getProperty(String key) {
		return fw.getProperty(key);
	}


	public ServiceReference<?> getServiceReference(String clazz) {
		return null;
	}

	public ServiceReference<?>[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		return null;
	}

	public Bundle installBundle(String location) throws BundleException {
		return fw.installBundle(location);
	}

	public Bundle installBundle(String location, InputStream input) throws BundleException {
		return fw.installBundle(location, input);
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

	@Override
	public String toString() {
		return id + " " + location;
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

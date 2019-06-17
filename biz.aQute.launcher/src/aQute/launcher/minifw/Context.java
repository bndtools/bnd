package aQute.launcher.minifw;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

public class Context extends URLClassLoader implements Bundle, BundleContext, BundleReference {
	long					id;
	MiniFramework			fw;
	String					location;
	int						state	= Bundle.INSTALLED;
	JarFile					jar;
	Manifest				manifest;
	private TreeSet<String>	paths;
	private File			jarFile;

	class Dict extends Dictionary<String, String> {

		@Override
		public Enumeration<String> elements() {
			@SuppressWarnings({
				"unchecked", "rawtypes"
			})
			Enumeration<String> enumeration = (Enumeration) Collections.enumeration(manifest.getMainAttributes()
				.values());
			return enumeration;
		}

		@Override
		public String get(Object key) {
			String o = manifest.getMainAttributes()
				.getValue((String) key);
			return o;
		}

		@Override
		public boolean isEmpty() {
			return manifest.getMainAttributes()
				.isEmpty();
		}

		@Override
		public Enumeration<String> keys() {
			Vector<String> v = new Vector<>();
			for (Iterator<Object> i = manifest.getMainAttributes()
				.keySet()
				.iterator(); i.hasNext();) {
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
			return manifest.getMainAttributes()
				.size();
		}

	}

	public Context(MiniFramework fw, ClassLoader parent, int id, String location) throws Exception {
		super(new URL[] {
			new File(location).toURI()
				.toURL()
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

	@Override
	public BundleContext getBundleContext() {
		return this;
	}

	@Override
	public long getBundleId() {
		return id;
	}

	@Override
	public URL getEntry(String path) {
		if (path.startsWith("/"))
			path = path.substring(1);
		return getResource(path);
	}

	@Override
	public Enumeration<String> getEntryPaths(String path) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Dictionary<String, String> getHeaders() {
		return new Dict();
	}

	@Override
	public Dictionary<String, String> getHeaders(String locale) {
		return new Dict();
	}

	@Override
	public long getLastModified() {
		return jarFile.lastModified();
	}

	@Override
	public String getLocation() {
		return location;
	}

	@Override
	public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {

		try {
			if (path.startsWith("/"))
				path = path.substring(1);
			if (!path.endsWith("/"))
				path += "/";

			Vector<URL> paths = new Vector<>();
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
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean matches(String path, String filePattern) {
		do {
			int part = filePattern.indexOf('*');
			if (part < 0) {
				return path.contains(filePattern);
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

		paths = new TreeSet<>();
		try (JarFile jar = new JarFile(new File(location))) {
			for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
				ZipEntry entry = e.nextElement();
				paths.add(entry.getName());
			}
		}
		return paths;
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
		throw new UnsupportedOperationException();
	}

	@Override
	public int getState() {
		return state;
	}

	@Override
	public String getSymbolicName() {
		return getHeaders().get(aQute.bnd.osgi.Constants.BUNDLE_SYMBOLICNAME)
			.trim();
	}

	@Override
	public Version getVersion() {
		String v = getHeaders().get(aQute.bnd.osgi.Constants.BUNDLE_VERSION)
			.trim();
		if (v == null)
			return new Version("0");
		return new Version(v);
	}

	@Override
	public boolean hasPermission(Object permission) {
		return true;
	}

	@Override
	public void start() throws BundleException {
		state = Bundle.ACTIVE;
	}

	@Override
	public void start(int options) throws BundleException {
		state = Bundle.ACTIVE;
	}

	@Override
	public void stop() throws BundleException {
		state = Bundle.RESOLVED;
	}

	@Override
	public void stop(int options) throws BundleException {
		state = Bundle.RESOLVED;
	}

	@Override
	public void uninstall() throws BundleException {
		state = Bundle.UNINSTALLED;
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
		throw new UnsupportedOperationException();
	}

	@Override
	public void addFrameworkListener(FrameworkListener listener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addServiceListener(ServiceListener listener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addServiceListener(ServiceListener listener, String filter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Filter createFilter(String filter) throws InvalidSyntaxException {
		throw new UnsupportedOperationException();
	}

	@Override
	public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Bundle getBundle() {
		return this;
	}

	@Override
	public Bundle getBundle(long id) {
		return fw.getBundle(id);
	}

	@Override
	public Bundle[] getBundles() {
		return fw.getBundles();
	}

	@Override
	public File getDataFile(String filename) {
		return null;
	}

	@Override
	public String getProperty(String key) {
		return fw.getProperty(key);
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
	public Bundle installBundle(String location) throws BundleException {
		return fw.installBundle(location);
	}

	@Override
	public Bundle installBundle(String location, InputStream input) throws BundleException {
		return fw.installBundle(location, input);
	}

	@Override
	public void removeBundleListener(BundleListener listener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeFrameworkListener(FrameworkListener listener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeServiceListener(ServiceListener listener) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return id + " " + location;
	}

	public int compareTo(Bundle var0) {
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

	public <T> ServiceRegistration<T> registerService(Class<T> clazz, T service, Dictionary<String, ?> properties) {
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

}

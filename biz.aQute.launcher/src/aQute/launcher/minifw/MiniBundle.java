package aQute.launcher.minifw;

import static aQute.launcher.minifw.Enumerations.enumeration;
import static java.util.stream.Collectors.toMap;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

public class MiniBundle implements Bundle, BundleContext, Closeable {
	private final long						id;
	private final long						lastModified;
	private final MiniFramework				fw;
	private final String					location;
	private volatile int					state;
	private final JarFile					jar;
	private final Headers					headers;
	private final TreeMap<String, JarEntry>	entries;
	private final File						jarFile;
	private final URLClassLoader			loader;

	public MiniBundle(MiniFramework fw, ClassLoader parent, long id, String location) throws BundleException {
		super();
		this.fw = fw;
		this.id = id;
		this.location = location;
		state = Bundle.INSTALLED;
		lastModified = System.currentTimeMillis();

		try {
			jarFile = new File(location);
			loader = new BundleClassLoader(jarFile, parent, getBundle());
			jar = new JarFile(jarFile);
			headers = new Headers(jar.getManifest());
			entries = Enumerations.stream(jar.entries())
				.filter(entry -> !entry.isDirectory())
				// .peek(entry -> fw.trace("entry %s",entry.getName()))
				.collect(toMap(JarEntry::getName, Function.identity(), (u, v) -> {
					throw new IllegalStateException(String.format("Duplicate jar entry %s", u));
				}, TreeMap::new));
		} catch (IOException e) {
			throw new BundleException("Failure to create MiniBundle", BundleException.READ_ERROR, e);
		}
	}

	@Override
	public void close() throws IOException {
		loader.close();
		jar.close();
	}

	ClassLoader getClassLoader() {
		return loader;
	}

	private Map<String, JarEntry> getEntries() {
		return entries;
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
		fw.trace("### getEntry(%s) %s", path, this);
		if (path.startsWith("/")) {
			path = path.substring(1);
		}

		URL entry = Optional.ofNullable(getEntries().get(path))
			.map(this::createURL)
			.orElse(null);
		return entry;
	}

	@Override
	public Enumeration<String> getEntryPaths(String path) {
		fw.trace("### getEntryPaths(%s) %s", path, this);
		Stream<String> paths = findEntryPaths(path, null, true);
		return enumeration(paths);
	}

	@Override
	public Dictionary<String, String> getHeaders() {
		return headers;
	}

	@Override
	public Dictionary<String, String> getHeaders(String locale) {
		return getHeaders();
	}

	@Override
	public long getLastModified() {
		return lastModified;
	}

	@Override
	public String getLocation() {
		return location;
	}

	@Override
	public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
		fw.trace("### findEntries(%s, %s, %s) %s", path, filePattern, recurse, this);
		Stream<URL> entries = findEntryPaths(path, filePattern, recurse).map(name -> getEntries().get(name))
			.map(this::createURL);
		return enumeration(entries);
	}

	private Stream<String> findEntryPaths(String path, String filePattern, boolean recurse) {
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		if (!path.endsWith("/")) {
			path += "/";
		}

		String finalPath = path;
		int finalPathLength = finalPath.length();
		Stream<String> paths = getEntries().keySet()
			.stream()
			.filter(name -> name.startsWith(finalPath) && (recurse || name.indexOf('/', finalPathLength) < 0)
				&& (filePattern == null || matches(name, filePattern)));
		return paths;
	}

	private static boolean matches(String path, String filePattern) {
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

	private URL createURL(JarEntry jarEntry) {
		try {
			return new URL("bundlentry", null, 0, "/".concat(jarEntry.getName()), new URLStreamHandler() {
				@Override
				protected URLConnection openConnection(URL u) throws IOException {
					return new URLConnection(u) {
						@Override
						public void connect() throws IOException {}

						@Override
						public InputStream getInputStream() throws IOException {
							return jar.getInputStream(jarEntry);
						}

						@Override
						public int getContentLength() {
							return (int) getContentLengthLong();
						}

						@Override
						public long getContentLengthLong() {
							return jarEntry.getSize();
						}

						@Override
						public String getContentType() {
							return "application/octet-stream";
						}

						@Override
						public long getLastModified() {
							return jarEntry.getTime();
						}
					};
				}
			});
		} catch (MalformedURLException e) {
			throw new UncheckedIOException(e); // wont happen
		}
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
	public int getState() {
		return state;
	}

	@Override
	public String getSymbolicName() {
		return Optional.ofNullable(getHeaders().get(Constants.BUNDLE_SYMBOLICNAME))
			.map(bsn -> {
				int n = bsn.indexOf(';');
				return (n > 0) ? bsn.substring(0, n) : bsn;
			})
			.map(String::trim)
			.orElse(null);
	}

	@Override
	public Version getVersion() {
		return Version.parseVersion(getHeaders().get(Constants.BUNDLE_VERSION));
	}

	@Override
	public boolean hasPermission(Object permission) {
		return true;
	}

	@Override
	public void start() throws BundleException {
		start(0);
	}

	@Override
	public void start(int options) throws BundleException {
		state = Bundle.ACTIVE;
	}

	@Override
	public void stop() throws BundleException {
		stop(0);
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
		update(null);
	}

	@Override
	public void update(InputStream in) throws BundleException {
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				throw new BundleException("Cannot update bundle in mini framework",
					BundleException.UNSUPPORTED_OPERATION, e);
			}
		}
		throw new BundleException("Cannot update bundle in mini framework", BundleException.UNSUPPORTED_OPERATION);
	}

	@Override
	public void addBundleListener(BundleListener listener) {
		fw.addBundleListener(listener);
	}

	@Override
	public void addFrameworkListener(FrameworkListener listener) {
		fw.addFrameworkListener(listener);
	}

	@Override
	public void addServiceListener(ServiceListener listener) {
		fw.addServiceListener(listener);
	}

	@Override
	public void addServiceListener(ServiceListener listener, String filter) {
		fw.addServiceListener(listener, filter);
	}

	@Override
	public Filter createFilter(String filter) throws InvalidSyntaxException {
		return fw.createFilter(filter);
	}

	@Override
	public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		return null;
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
		fw.removeBundleListener(listener);
	}

	@Override
	public void removeFrameworkListener(FrameworkListener listener) {
		fw.removeFrameworkListener(listener);
	}

	@Override
	public void removeServiceListener(ServiceListener listener) {
		fw.removeServiceListener(listener);
	}

	@Override
	public String toString() {
		return id + " " + location;
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
	public <T> ServiceRegistration<T> registerService(Class<T> clazz, T service, Dictionary<String, ?> properties) {
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
		return fw.getBundle(location);
	}

	@Override
	public <A> A adapt(Class<A> type) {
		if (BundleRevision.class.equals(type)) {
			return type.cast(new BundleRevisionImpl(getBundle()));
		}
		if (BundleWiring.class.equals(type)) {
			return type.cast(new BundleWiringImpl(getBundle(), getClassLoader()));
		}
		if (BundleStartLevel.class.equals(type)) {
			return type.cast(new BundleStartLevelImpl(getBundle(), fw.adapt(FrameworkStartLevel.class)));
		}
		fw.trace("### No adaptation for adapt(%s) %s", type, this);
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
	public URL getResource(String name) {
		fw.trace("### getResource(%s) %s", name, this);
		return loader.getResource(name);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		fw.trace("### loadClass(%s) %s", name, this);
		return loader.loadClass(name);
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		fw.trace("### getResources(%s) %s", name, this);
		return loader.getResources(name);
	}
}

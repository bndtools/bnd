package aQute.bnd.osgi;

import static aQute.lib.collections.Enumerations.enumeration;
import static java.util.stream.Collectors.toList;

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
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.io.ByteBufferInputStream;
import aQute.lib.io.IO;

/**
 * This class loader can load classes from JAR files.
 */
class ActivelyClosingClassLoader extends URLClassLoader implements Closeable {
	static {
		ClassLoader.registerAsParallelCapable();
	}

	final AtomicReference<Map<File, Wrapper>>	wrappers	= new AtomicReference<>(new LinkedHashMap<>());
	final AtomicBoolean							open		= new AtomicBoolean(true);
	final Processor								processor;
	ScheduledFuture<?>							schedule;

	class Wrapper {
		Jar				jarFile;
		volatile long	lastAccess;
		final File		file;

		Wrapper(File file) {
			this.file = file;
		}

		synchronized void close() {
			IO.close(jarFile);
			jarFile = null;
		}

		synchronized byte[] getData(String name) {
			if (!open.get()) {
				return null;
			}
			try {
				if (jarFile == null) {
					if (!file.exists()) {
						return null;
					}
					jarFile = new Jar(file);
				}
				Resource resource = jarFile.getResource(name);
				if (resource == null) {
					return null;
				}
				lastAccess = System.nanoTime();
				return IO.read(resource.openInputStream());
			} catch (Exception e) {
				processor.exception(e, "while loading resource %s from %s: %s", name, file, e.getMessage());
				return null;
			}
		}
	}

	ActivelyClosingClassLoader(Processor processor, ClassLoader parent) {
		super(new URL[0], parent);
		this.processor = processor;
	}

	void add(File file) {
		if (!open.get()) {
			throw new IllegalStateException("Already closed");
		}
		wrappers.updateAndGet(map -> {
			Map<File, Wrapper> copy = new LinkedHashMap<>(map);
			copy.computeIfAbsent(file, Wrapper::new);
			return copy;
		});
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		return dataStream(name.replace('.', '/') + ".class").findFirst()
			.map(data -> defineClass(name, data, 0, data.length))
			.orElseThrow(() -> new ClassNotFoundException(name + " not found in " + this));
	}

	@Override
	public URL findResource(String name) {
		return dataStream(name).findFirst()
			.map(data -> createURL(name, data))
			.orElse(null);
	}

	private Stream<byte[]> dataStream(String name) {
		return wrappers.get()
			.values()
			.stream()
			.map(wrapper -> wrapper.getData(name))
			.filter(Objects::nonNull);
	}

	private URL createURL(String name, byte[] data) {
		try {
			return new URL("bndloader", null, 0, name, new URLStreamHandler() {

				@Override
				protected URLConnection openConnection(URL u) throws IOException {
					return new URLConnection(u) {

						@Override
						public void connect() throws IOException {}

						@Override
						public InputStream getInputStream() throws IOException {
							return new ByteBufferInputStream(data);
						}

						@Override
						public int getContentLength() {
							return data.length;
						}

						@Override
						public long getContentLengthLong() {
							return data.length;
						}

						@Override
						public String getContentType() {
							return "application/octet-stream";
						}
					};
				}
			});
		} catch (MalformedURLException e) {
			throw new UncheckedIOException(e); // wont happen
		}
	}

	@Override
	public Enumeration<URL> findResources(String name) {
		return enumeration(dataStream(name).spliterator(), data -> createURL(name, data));
	}

	/**
	 * This method will close any open files that have not been accessed within
	 * the specified delta cutoff time.
	 *
	 * @param purgeTime The delta cutoff time in nanoseconds.
	 */
	void purge(long purgeTime) {
		long now = System.nanoTime();
		wrappers.get()
			.values()
			.stream()
			.filter(w -> now - w.lastAccess >= purgeTime)
			.forEach(Wrapper::close);
	}

	@Override
	public void close() {
		if (open.getAndSet(false)) {
			if (schedule != null) {
				schedule.cancel(true);
			}
			wrappers.getAndSet(new LinkedHashMap<>())
				.values()
				.forEach(Wrapper::close);
		}
	}

	List<File> getFiles() {
		return wrappers.get()
			.values()
			.stream()
			.map(w -> w.file)
			.collect(toList());
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		try {
			try {
				return super.loadClass(name);
			} catch (ClassNotFoundException nfe) {

				//
				// Best effort search in the application classpath
				// if we're running on OSGi
				//

				Bundle bundle = FrameworkUtil.getBundle(ActivelyClosingClassLoader.class);
				if (bundle == null) {
					// not running inside OSGi...
					throw nfe;
				}
				Optional<Class<?>> bundleClass = tryLoadFromBundle(name, bundle, nfe).or(() -> {
					BundleContext bundleContext = bundle.getBundleContext();
					if (bundleContext == null) {
						// not an active bundle, nothing more we can do here...
						return Optional.empty();
					}
					return tryLoadFromBundle(name, bundleContext.getBundle(0), nfe);
				});
				return bundleClass.orElseThrow(() -> nfe);
			}
		} catch (Throwable t) {

			StringBuilder sb = new StringBuilder();
			sb.append(name);
			sb.append(" not found, parent: ");
			sb.append(getParent());
			sb.append(" urls:");
			sb.append(getFiles());
			sb.append(" exception:");
			sb.append(Exceptions.toString(t));
			throw new ClassNotFoundException(sb.toString(), t);
		}
	}

	/**
	 * This method will schedule closing any open files that have not been
	 * accessed within the specified time interval.
	 *
	 * @param freshPeriod The time interval in nanoseconds.
	 */
	void autopurge(long freshPeriod) {
		schedule = Processor.getScheduledExecutor()
			.scheduleWithFixedDelay(() -> purge(freshPeriod), freshPeriod, freshPeriod, TimeUnit.NANOSECONDS);
	}

	/**
	 * Try to load a named class from a bundle
	 *
	 * @param name the name of the class to load
	 * @param bundle the bundle to query
	 * @param original an (optional) original exception that should be used to
	 *            report a failure loading this class from the bundle as a
	 *            suppressed exception
	 * @return an {@link Optional} describing the loaded class, or an empty
	 *         {@link Optional#empty()} if the class can not be loaded.
	 */
	private static Optional<Class<?>> tryLoadFromBundle(String name, Bundle bundle, Throwable original) {
		try {
			return Optional.of(bundle.loadClass(name));
		} catch (ClassNotFoundException e) {
			if (original != null) {
				original.addSuppressed(e);
			}
		}
		return Optional.empty();
	}

}

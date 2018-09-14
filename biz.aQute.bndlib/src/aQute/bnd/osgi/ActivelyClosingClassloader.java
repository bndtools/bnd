package aQute.bnd.osgi;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;

/**
 * This class loader can load classes from JAR files.
 */
class ActivelyClosingClassloader extends URLClassLoader implements Closeable {
	final Map<File, Wrapper>	wrappers	= new ConcurrentHashMap<>();
	final AtomicBoolean			open		= new AtomicBoolean(true);
	final Processor				processor;
	ScheduledFuture<?>			schedule;

	class Wrapper {
		Set<String>		names;
		Jar				jarFile;
		volatile long	lastAccess;
		File			file;

		Wrapper(File file) {
			this.file = file;
		}

		synchronized void close() {
			IO.close(jarFile);
			jarFile = null;
		}

		synchronized byte[] getData(String name) {
			if (!open.get())
				return null;

			if (names != null && !names.contains(name))
				return null;

			try {
				init();

				Resource resource = jarFile.getResource(name);
				if (resource == null)
					return null;

				lastAccess = System.currentTimeMillis();
				try (InputStream in = resource.openInputStream()) {
					return IO.read(in);
				}
			} catch (Exception e) {
				processor.error("while loading class bytes %s from %s: %s", name, file, e.getMessage());
				return null;
			}
		}

		synchronized URL getResource(String name) {
			try {
				if (!open.get())
					return null;

				if (names != null && !names.contains(name))
					return null;

				init();

				if (!names.contains(name))
					return null;

				lastAccess = System.currentTimeMillis();
				return new URL("bndloader", null, 0, name, new URLStreamHandler() {

					@Override
					protected URLConnection openConnection(URL u) throws IOException {
						return new URLConnection(u) {

							@Override
							public void connect() throws IOException {

							}

							@Override
							public InputStream getInputStream() throws IOException {
								byte[] data = getData(name);
								return new ByteArrayInputStream(data);
							}
						};
					}
					
				});
			} catch (IOException e) {
				processor.error("while loading resource %s from %s: %s", name, file, e.getMessage());
				return null;
			}
		}

		private void init() throws IOException {
			if (jarFile == null) {
				jarFile = new Jar(file);
			}
			if (names == null) {
				names = new HashSet<>(jarFile.getResources()
					.keySet());
			}
		}
	}

	ActivelyClosingClassloader(Processor processor, ClassLoader parent) {
		super(new URL[0], parent);
		this.processor = processor;
		registerAsParallelCapable();
	}

	public void add(File file) {
		if (!open.get())
			throw new IllegalStateException("Already closed");
		wrappers.computeIfAbsent(file, Wrapper::new);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		byte[] data = getData(name.replace('.', '/') + ".class");
		if (data == null)
			throw new ClassNotFoundException(name + " not found in " + this);

		return defineClass(name, data, 0, data.length);
	}

	private byte[] getData(String name) {
		for (Wrapper wrapper : wrappers.values()) {
			byte[] data = wrapper.getData(name);
			if (data != null)
				return data;
		}
		return null;
	}

	@Override
	public URL findResource(String name) {
		return wrappers.values()
			.stream()
			.map(wrapper -> wrapper.getResource(name))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	@Override
	public Enumeration<URL> findResources(String name) {
		List<URL> resources = wrappers.values()
			.stream()
			.map(w -> w.getResource(name))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		return Collections.enumeration(resources);
	}

	/**
	 * This method will close any open files that have not been accessed since
	 * purgeTime
	 * 
	 * @param purgeTime the absolute cutoff time
	 */

	public void purge(long purgeTime) {
		wrappers.values()
			.stream()
			.filter(w -> w.lastAccess < purgeTime)
			.forEach(Wrapper::close);
	}

	@Override
	public void close() {
		if (open.getAndSet(false)) {
			if (schedule != null) {
				schedule.cancel(true);
			}
			wrappers.values()
				.removeIf(w -> {
					w.close();
					return true;
				});
		}
	}

	public List<File> getFiles() {
		return wrappers.values()
			.stream()
			.map(w -> w.file)
			.collect(Collectors.toList());
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

				Bundle bundle = FrameworkUtil.getBundle(ActivelyClosingClassloader.class);
				if (bundle == null)
					throw nfe;
				Bundle system = bundle.getBundleContext()
					.getBundle(0);
				return system.loadClass(name);
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

	public void autopurge(long freshPeriod) {
		schedule = Processor.getScheduledExecutor()
			.scheduleWithFixedDelay(() -> {
				purge(System.currentTimeMillis() - freshPeriod);
			}, freshPeriod, freshPeriod, TimeUnit.MILLISECONDS);
	}

}

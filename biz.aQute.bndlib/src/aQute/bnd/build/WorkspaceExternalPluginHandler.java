package aQute.bnd.build;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.resource.Capability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.exceptions.FunctionWithException;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.memoize.Memoize;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Processor.CL;
import aQute.bnd.osgi.resource.MainClassNamespace;
import aQute.bnd.result.Result;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.RegistryPlugin;
import aQute.bnd.service.externalplugin.ExternalPluginNamespace;
import aQute.bnd.service.progress.ProgressPlugin.Task;
import aQute.bnd.service.progress.TaskManager;
import aQute.bnd.version.MavenVersion;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.command.Command;

public class WorkspaceExternalPluginHandler implements AutoCloseable {
	final static Logger			logger	= LoggerFactory.getLogger("aQute.bnd.build");
	final static Method			close	= getMethod(AutoCloseable.class, "close");
	final Workspace				workspace;
	final Map<Capability, CL>	loaders	= new HashMap<>();

	WorkspaceExternalPluginHandler(Workspace workspace) {
		this.workspace = workspace;
	}

	public <T, R> Result<R> call(String pluginName, Class<T> c, FunctionWithException<T, Result<R>> f) {
		return call(pluginName, null, c, f);
	}

	public <T, R> Result<R> call(String pluginName, VersionRange range, Class<T> c,
		FunctionWithException<T, Result<R>> f) {
		try {

			String filter = ExternalPluginNamespace.filter(pluginName, c);

			List<Capability> caps = workspace.findProviders(ExternalPluginNamespace.EXTERNAL_PLUGIN_NAMESPACE, filter)
				.sorted(this::sort)
				.collect(Collectors.toList());

			if (caps.isEmpty())
				return Result.err("no such plugin %s for type %s", pluginName, c.getName());

			Capability cap = caps.get(0);

			Result<File> bundle = workspace.getBundle(cap.getResource());
			if (bundle.isErr())
				return bundle.asError();

			String className = ExternalPluginNamespace.getImplementation(cap);
			if (className == null)
				return Result.err("no proper class attribute in plugin capability %s is %s", pluginName, cap);

			URL url = bundle.unwrap()
				.toURI()
				.toURL();

			try (URLClassLoader cl = new URLClassLoader(new URL[] {
				url
			}, WorkspaceExternalPluginHandler.class.getClassLoader())) {
				@SuppressWarnings("unchecked")
				Class<?> impl = cl.loadClass(className);
				T instance = c.cast(impl.getConstructor()
					.newInstance());
				try {
					return f.apply(instance);
				} catch (Exception e) {
					return Result.err("external plugin '%s' failed with: %s", pluginName, Exceptions.causes(e));
				}
			}
		} catch (ClassNotFoundException e) {
			return Result.err("no such class %s in %s for plugin %s", c.getName(), e.getMessage(), pluginName);
		} catch (Exception e) {
			return Result.err("could not instantiate class %s in %s for plugin %s: %s", c.getName(), e.getMessage(),
				pluginName, Exceptions.causes(e));
		}
	}

	public Result<Integer> call(String mainClass, VersionRange range, Processor context, Map<String, String> attrs,
		List<String> args, InputStream stdin, OutputStream stdout, OutputStream stderr) {
		List<File> cp = new ArrayList<>();
		try {

			Parameters cpp = new Parameters(attrs.get("classpath"));

			for (Map.Entry<String, Attrs> e : cpp.entrySet()) {
				String v = e.getValue()
					.getVersion();
				MavenVersion mv = MavenVersion.parseMavenString(v);

				Result<File> result = workspace.getBundle(e.getKey(), mv.getOSGiVersion(), null);
				if (result.isErr())
					return result.asError();

				cp.add(result.unwrap());
			}

			String filter = MainClassNamespace.filter(mainClass, range);

			List<Capability> caps = workspace.findProviders(MainClassNamespace.MAINCLASS_NAMESPACE, filter)
				.sorted(this::sort)
				.collect(Collectors.toList());

			if (caps.isEmpty())
				return Result.err("no bundle found with main class %s", mainClass);

			Capability cap = caps.get(0);

			Result<File> bundle = workspace.getBundle(cap.getResource());
			if (bundle.isErr())
				return bundle.asError();

			cp.add(bundle.unwrap());

			Command c = new Command();

			c.setTrace();

			File cwd = context.getBase();
			String workingdir = attrs.get("workingdir");
			if (workingdir != null) {
				cwd = context.getFile(workingdir);
				cwd.mkdirs();
				if (!cwd.isDirectory()) {
					return Result.err("Working dir set to %s but cannot make it a directory", cwd);
				}
			}
			c.setCwd(cwd);
			c.setTimeout(1, TimeUnit.MINUTES);

			c.add(context.getProperty("java", IO.getJavaExecutablePath("java")));
			c.add("-cp");

			String classpath = Strings.join(File.pathSeparator, cp);
			c.add(classpath);

			c.add(mainClass);

			for (String arg : args) {
				c.add(arg);
			}

			int exitCode = TaskManager.with(getTask(c), () -> {

				PrintWriter lstdout = IO.writer(stdout == null ? System.out : stdout);
				PrintWriter lstderr = IO.writer(stderr == null ? System.err : stderr);
				try {
					return c.execute(stdin, lstdout, lstderr);
				} finally {
					lstdout.flush();
					lstderr.flush();
				}
			});

			return Result.ok(exitCode);

		} catch (Exception e) {
			return Result.err("Failed with: %s", Exceptions.causes(e));
		}
	}

	private Task getTask(Command c) {
		return new Task() {

			private boolean canceled;

			@Override
			public void worked(int units) {}

			@Override
			public void done(String message, Throwable e) {}

			@Override
			public boolean isCanceled() {
				return canceled;
			}

			@Override
			public void abort() {
				this.canceled = true;
				c.cancel();
			}
		};
	}

	@Override
	public void close() {
		loaders.values()
			.forEach(IO::close);
	}

	/**
	 * Returns list of external plugin proxies that implement the given
	 * interface. The proxies will load the actual plugin on demand when used.
	 * That is, the plugins will be quite cheap unless used.
	 *
	 * @param interf the interface listed in `-plugin`.
	 * @param attrs the attributes from the that interface, the name specifies
	 *            the name of the plugin, wildcards allowed
	 * @return a list of plugins loaded from the external plugin set
	 */
	public <T> Result<List<T>> getImplementations(Class<T> interf, Attrs attrs) {
		assert interf.isInterface();

		try {
			String v = attrs.getVersion();
			VersionRange r = null;
			if (v != null) {
				r = VersionRange.valueOf(v);
			}

			String filter = ExternalPluginNamespace.filter(attrs.getOrDefault("name", "*"), interf, r);
			List<Capability> externalCapabilities = workspace
				.findProviders(ExternalPluginNamespace.EXTERNAL_PLUGIN_NAMESPACE, filter)
				.sorted(this::sort)
				.collect(Collectors.toList());

			Class<?>[] interfaces = new Class[] {
				interf, AutoCloseable.class, Plugin.class, RegistryPlugin.class
			};
			ClassLoader loader = new ProxyClassLoader(interf.getClassLoader(), interfaces);
			List<T> plugins = new ArrayList<>();
			for (Capability c : externalCapabilities) {
				Memoize<Object> delegate = Memoize.supplier(() -> load(c, attrs).unwrap());
				@SuppressWarnings("unchecked")
				T proxy = (T) Proxy.newProxyInstance(loader, interfaces, new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if (method.getDeclaringClass() == Object.class) {
							return method.invoke(this, args);
						}

						if (method.getDeclaringClass() == AutoCloseable.class) {
							if (delegate.isPresent()) {
								Object object = delegate.get();
								if (object instanceof AutoCloseable)
									return method.invoke(object, args);
							}
							return null;
						}

						Object object = delegate.get();
						if (object == null)
							throw new IllegalStateException("Could not load external plugin for capability " + c);

						if ((method.getDeclaringClass() == Plugin.class) && !(object instanceof Plugin)) {
							return null;
						}
						if ((method.getDeclaringClass() == RegistryPlugin.class)
							&& !(object instanceof RegistryPlugin)) {
							return null;
						}

						return method.invoke(object, args);
					}

					@Override
					public String toString() {
						return "Proxy plugin: " + c;
					}
				});
				plugins.add(proxy);
			}
			return Result.ok(plugins);
		} catch (Exception e) {
			return Result.err("failed to load external plugins %s (%s): %s", interf, attrs, e);
		}
	}

	private Result<Object> load(Capability cap, Attrs attrs) {
		String implementation = ExternalPluginNamespace.getImplementation(cap);
		if (implementation == null) {
			return Result.err("No implementation class %s", cap);
		}
		try {
			Result<? extends ClassLoader> loader = getLoader(cap);
			if (loader.isErr())
				return loader.asError();

			Class<?> loadedClass = loader.unwrap()
				.loadClass(implementation);

			Object plugin = loadedClass.getConstructor()
				.newInstance();
			return Result.ok(plugin);
		} catch (Exception e) {
			Workspace.logger.info("failed to load class %s for external plugin load for %s", implementation, cap, e);
			return Result.err("failed to load class %s for external plugin load for %s: %s", implementation, cap, e);
		}
	}

	private synchronized Result<CL> getLoader(Capability cap) {
		CL urlClassLoader = loaders.get(cap);
		if (urlClassLoader == null) {
			try {
				Result<File> bundle = workspace.getBundle(cap.getResource());
				if (bundle.isErr())
					return bundle.asError();

				File file = bundle.unwrap();
				urlClassLoader = new CL(workspace);
				urlClassLoader.add(file);
				loaders.put(cap, urlClassLoader);
			} catch (Exception e) {
				return Result.err("failed to create class loader for %s: %s", cap, e);
			}
		}
		return Result.ok(urlClassLoader);
	}

	private static Method getMethod(Class<?> class1, String name, Class<?>... args) {
		try {
			return class1.getMethod(name, args);
		} catch (NoSuchMethodException | SecurityException e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Class loader that can be used to create a proxy in cases where the
	 * interface classes are not visible from the class loader of the first
	 * interface class.
	 */
	static class ProxyClassLoader extends ClassLoader {
		static {
			ClassLoader.registerAsParallelCapable();
		}

		private final Class<?>[]	classes;
		private final ClassLoader[]	loaders;

		public ProxyClassLoader(ClassLoader parent, Class<?>[] classes) {
			super(parent);
			this.classes = requireNonNull(classes);
			this.loaders = Arrays.stream(classes)
				.map(c -> {
					ClassLoader loader = c.getClassLoader();
					return (loader == null) ? getSystemClassLoader() : loader;
				})
				.distinct()
				.toArray(ClassLoader[]::new);
		}

		@Override
		public Class<?> findClass(String name) throws ClassNotFoundException {
			for (Class<?> c : classes) {
				if (name.equals(c.getName())) {
					return c;
				}
			}
			for (ClassLoader loader : loaders) {
				try {
					return loader.loadClass(name);
				} catch (ClassNotFoundException | NoClassDefFoundError cnfe) {
					// Try next
				}
			}
			throw new ClassNotFoundException(name);
		}

		@Override
		public URL findResource(String name) {
			for (ClassLoader loader : loaders) {
				URL url = loader.getResource(name);
				if (url != null) {
					return url;
				}
			}
			return null;
		}
	}

	private int sort(Capability a, Capability b) {
		String av = a.getAttributes()
			.getOrDefault("version", "0.0.0")
			.toString();
		String bv = b.getAttributes()
			.getOrDefault("version", "0.0.0")
			.toString();
		try {
			Version avv = new Version(av);
			Version bvv = new Version(bv);
			return bvv.compareTo(avv);
		} catch (Exception e) {
			return bv.compareTo(av);
		}
	}
}

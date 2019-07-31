package aQute.launchpad;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import aQute.bnd.remoteworkspace.client.RemoteWorkspaceClientFactory;
import aQute.bnd.service.remoteworkspace.RemoteWorkspace;
import aQute.bnd.service.remoteworkspace.RemoteWorkspaceClient;
import aQute.bnd.service.specifications.BuilderSpecification;
import aQute.bnd.service.specifications.RunSpecification;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import aQute.libg.parameters.ParameterMap;

/**
 * This class is a builder for frameworks that can be used in JUnit testing.
 */
public class LaunchpadBuilder implements AutoCloseable {

	private static final String		LAUNCHPAD_NAME		= "launchpad.name";
	private static final String		LAUNCHPAD_CLASSNAME	= "launchpad.classname";

	private static final String		EXCLUDEEXPORTS		= "-excludeexports";
	final static ExecutorService	executor			= Executors.newCachedThreadPool();
	final static File				projectDir			= IO.work;
	final static RemoteWorkspace	workspace			= RemoteWorkspaceClientFactory.create(projectDir,
		new RemoteWorkspaceClient() {});
	final static RunSpecification	projectTestSetup;
	final static AtomicInteger		counter				= new AtomicInteger();

	static {
		projectTestSetup = workspace.analyzeTestSetup(IO.work.getAbsolutePath());

		//
		// We only want the raw setup and not the run spec since this
		// makes it impossible to start a clean framework. Make them
		// immutable so that they can't be modified accidentally.
		//

		projectTestSetup.runbundles = Collections.emptyList();
		projectTestSetup.runpath = Collections.emptyList();
		projectTestSetup.properties = Collections.emptyMap();
		projectTestSetup.runfw = Collections.emptyList();

		projectTestSetup.extraSystemPackages = Collections.unmodifiableMap(projectTestSetup.extraSystemPackages);
		projectTestSetup.extraSystemCapabilities = Collections
			.unmodifiableMap(projectTestSetup.extraSystemCapabilities);

		Runtime.getRuntime()
			.addShutdownHook(new Thread(() -> {
				try {
					workspace.close();
				} catch (IOException e) {
					// ignore
				}
			}));
	}

	RunSpecification				local;
	boolean							start			= true;
	boolean							testbundle		= true;
	boolean							byReference		= true;
	long							closeTimeout	= 60000;
	boolean							debug;
	final Set<Class<?>>				hide			= new HashSet<>();
	final List<Predicate<String>>	excludeExports	= new ArrayList<>();
	final List<String>				exports			= new ArrayList<>();
	ClassLoader						myClassLoader;
	String							parentLoader	= Constants.FRAMEWORK_BUNDLE_PARENT_BOOT;

	/**
	 * Start a framework assuming the current working directory is the project
	 * directory.
	 */
	public LaunchpadBuilder() {
		// This ensures a deep clone.
		local = new RunSpecification();
		local.mergeWith(projectTestSetup);
	}

	public LaunchpadBuilder bndrun(File file) {
		RunSpecification setup = workspace.getRun(file.getAbsolutePath());
		if (!setup.errors.isEmpty()) {
			throw new IllegalArgumentException(
				"Errors while get bndrun file " + file.getAbsolutePath() + "\n" + Strings.join("\n", setup.errors));

		}
		setup.target = null;
		setup.bin = null;
		setup.bin_test = null;
		local.mergeWith(setup);
		return this;
	}

	public LaunchpadBuilder bndrun(String path) {
		return bndrun(IO.getFile(projectDir, path));
	}

	public LaunchpadBuilder project() {
		return bndrun(projectDir);
	}

	public LaunchpadBuilder gogo() {
		bundles("org.apache.felix.gogo.runtime,org.apache.felix.gogo.command,org.apache.felix.gogo.shell");
		return this;
	}

	public LaunchpadBuilder bundles(String specification) {
		List<String> config = workspace.getLatestBundles(projectDir.getAbsolutePath(), specification);
		config.forEach(local.runbundles::add);
		return this;
	}

	public LaunchpadBuilder runpath(String specification) {
		List<String> config = workspace.getLatestBundles(projectDir.getAbsolutePath(), specification);
		config.forEach(local.runpath::add);
		return this;
	}

	public LaunchpadBuilder bundles(File... files) {
		Stream.of(files)
			.map(File::getAbsolutePath)
			.forEach(local.runpath::add);

		return this;
	}

	public LaunchpadBuilder runpath(File... files) {
		Stream.of(files)
			.map(File::getAbsolutePath)
			.forEach(local.runpath::add);

		return this;
	}

	public LaunchpadBuilder nostart() {
		this.start = false;
		return this;
	}

	public LaunchpadBuilder hide(Class<?> hide) {
		this.hide.add(hide);
		return this;
	}

	public LaunchpadBuilder notestbundle() {
		this.testbundle = false;
		return this;
	}

	public LaunchpadBuilder runfw(File file) {
		local.runfw.clear();
		local.runfw.add(file.getAbsolutePath());

		return this;
	}

	public LaunchpadBuilder runfw(String specification) {
		local.runfw = workspace.getLatestBundles(projectDir.getAbsolutePath(), specification);
		return this;
	}

	public LaunchpadBuilder set(String key, String value) {
		local.properties.put(key, value);
		return this;
	}

	public LaunchpadBuilder closeTimeout(long ms) {
		this.closeTimeout = ms;
		return this;
	}

	public LaunchpadBuilder usingClassLoader(ClassLoader loader) {
		this.myClassLoader = loader;
		return this;
	}

	public LaunchpadBuilder debug() {
		this.debug = true;
		return this;
	}

	/**
	 * Exclude the exports that are matched by any of the given globs
	 *
	 * @param globs the globs to match against the system framework exports
	 * @return this
	 */
	public LaunchpadBuilder excludeExport(String... globs) {
		Stream.of(globs)
			.flatMap((x) -> Strings.splitAsStream(x))
			.map(this::toPredicate)
			.forEach(excludeExports::add);
		return this;
	}

	/**
	 * Exclude the exports that are matched by any of the given predicates
	 *
	 * @param predicate the predicates to match against the system framework
	 *            exports
	 * @return this
	 */
	public LaunchpadBuilder excludeExport(Predicate<String> predicate) {
		excludeExports.add(predicate);
		return this;
	}

	public Launchpad create() {
		StackTraceElement element = new Exception().getStackTrace()[1];
		return create(element.getMethodName(), element.getClassName());
	}

	public Launchpad create(String name) {
		StackTraceElement element = new Exception().getStackTrace()[1];
		return create(name, element.getClassName());
	}

	public Launchpad create(String name, String className) {
		try {
			File storage = IO.getFile(new File(local.target), "launchpad/launchpad-" + counter.incrementAndGet());
			IO.delete(storage);

			List<Predicate<String>> localExcludeExports = new ArrayList<>(excludeExports);
			new ParameterMap(local.instructions.get(EXCLUDEEXPORTS)).keySet()
				.stream()
				.map(this::toPredicate)
				.forEach(localExcludeExports::add);

			ParameterMap restrictedExports = restrict(new ParameterMap(local.extraSystemPackages), localExcludeExports);

			String extraPackages = restrictedExports.toString();

			String extraCapabilities = new ParameterMap(local.extraSystemCapabilities).toString();

			RunSpecification runspec = new RunSpecification();
			runspec.mergeWith(local);

			runspec.properties.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, extraPackages);
			runspec.properties.put(Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA, extraCapabilities);
			runspec.properties.put(Constants.FRAMEWORK_STORAGE, storage.getAbsolutePath());
			runspec.properties.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
			runspec.properties.put(Constants.FRAMEWORK_BUNDLE_PARENT, parentLoader);
			runspec.properties.put(LAUNCHPAD_NAME, name);
			runspec.properties.put(LAUNCHPAD_CLASSNAME, className);

			Framework framework = getFramework(runspec);

			@SuppressWarnings("resource")
			Launchpad launchpad = new Launchpad(framework, name, className, runspec, closeTimeout, debug, testbundle,
				byReference);

			launchpad.report("ALL extra system packages\n     %s", toLines(local.extraSystemPackages.keySet()));
			launchpad.report("Filtered extra system packages\n     %s", toLines(restrictedExports.keySet()));
			launchpad.report("ALL extra system capabilities\n     %s", toLines(local.extraSystemCapabilities.keySet()));

			launchpad.report("Storage %s", storage.getAbsolutePath());
			launchpad.report("Runpath %s", local.runpath);

			hide.forEach(launchpad::hide);
			if (start) {
				launchpad.start();
			}

			return launchpad;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private String toLines(Collection<String> set) {
		return set.stream()
			.sorted()
			.collect(Collectors.joining("\n     "));
	}

	// private Map<String, Map<String, String>> addUses(Map<String, Map<String,
	// String>> extraSystemPackages) {
	// String uses = extraSystemPackages.keySet()
	// .stream()
	// .map(s -> {
	// while (s.endsWith("~"))
	// s = s.substring(0, s.length() - 1);
	// return s;
	// })
	// .filter(s -> !s.startsWith("java"))
	// .distinct()
	// .collect(Strings.joining());
	//
	// extraSystemPackages.entrySet()
	// .forEach(e -> {
	// e.getValue()
	// .put("uses:", uses);
	// });
	// return extraSystemPackages;
	// }

	/**
	 * We're not closing the framework and reuse the static variables. That
	 * means we never really shut down the remote connection and let the process
	 * exit kill it. This is for efficiency reasons.
	 */
	@Override
	public void close() throws Exception {}

	Framework getFramework(RunSpecification runspec) {
		try {
			ClassLoader loader;

			if (runspec.runpath.isEmpty() && local.runfw.isEmpty()) {
				loader = getMyClassLoader();
			} else {
				List<String> runpath = new ArrayList<>(local.runfw);
				runpath.addAll(runspec.runpath);
				URL[] urls = runpath.stream()
					.map(File::new)
					.map(this::toURL)
					.toArray(URL[]::new);

				loader = new URLClassLoader(urls, getMyClassLoader());
			}

			FrameworkFactory factory = getFactory(loader);
			if (factory == null) {
				throw new IllegalArgumentException("Could not find an OSGi Framework on the runpath " + local.runpath);
			}
			return factory.newFramework(runspec.properties);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private FrameworkFactory getFactory(ClassLoader loader) throws Exception {
		ServiceLoader<FrameworkFactory> sl = ServiceLoader.load(FrameworkFactory.class, loader);
		FrameworkFactory result = null;

		for (FrameworkFactory ff : sl) {

			if (result != null) {

				if (ff.getClass()
					.getClassLoader() == loader)
					return ff;

				return result;
			} else {
				result = ff;
			}
		}
		if (result == null)
			throw new FileNotFoundException("No Framework found on classpath");
		else
			return result;
	}

	private ClassLoader getMyClassLoader() {
		return myClassLoader == null ? LaunchpadBuilder.class.getClassLoader() : myClassLoader;
	}

	private URL toURL(File file) {
		try {
			return file.toURI()
				.toURL();
		} catch (MalformedURLException e) {
			throw Exceptions.duck(e);
		}
	}

	public LaunchpadBuilder snapshot() {
		this.bundles("biz.aQute.bnd.runtime.snapshot");
		return this;
	}

	private ParameterMap restrict(ParameterMap map, Collection<Predicate<String>> globs) {
		if (!exports.isEmpty()) {
			return map.restrict(exports);
		}

		if (globs == null || globs.isEmpty())
			return map;

		ParameterMap parameters = new ParameterMap();
		local.extraSystemPackages.entrySet()
			.stream()
			.filter(e -> {
				for (Predicate<String> p : excludeExports) {
					if (p.test(e.getKey()))
						return false;
				}
				return true;
			})
			.forEach(e -> parameters.put(e.getKey(), e.getValue()));
		return parameters;
	}

	Predicate<String> toPredicate(String specification) {
		Glob g = new Glob(specification);
		return (test) -> g.matches(test);
	}

	public LaunchpadBuilder copyInstall() {
		byReference = false;
		return this;
	}

	public LaunchpadBuilder addCapability(String namespace, String... keyVal) {
		Map<String, String> attrs = new HashMap<>();
		for (int i = 0; i < keyVal.length - 1; i += 2) {
			attrs.put(keyVal[i], keyVal[i + 1]);
		}
		while (local.extraSystemCapabilities.containsKey(namespace))
			namespace += "~";

		local.extraSystemCapabilities.put(namespace, attrs);
		return this;
	}

	public RunSpecification getLocal() {
		return local;
	}

	public byte[] build(String path, BuilderSpecification spec) {
		return workspace.build(path, spec);
	}

	public LaunchpadBuilder export(String spec) {
		exports.add(spec);
		return this;
	}

	public boolean isDebug() {
		return debug;
	}

	public LaunchpadBuilder applicationLoaderAsParent() {
		this.parentLoader = Constants.FRAMEWORK_BUNDLE_PARENT_APP;
		return this;
	}
}

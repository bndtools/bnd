package aQute.launchpad;

import static aQute.lib.exceptions.Exceptions.unchecked;
import static java.util.UUID.randomUUID;
import static java.util.stream.Stream.of;
import static org.osgi.framework.Constants.FRAMEWORK_BUNDLE_PARENT;
import static org.osgi.framework.Constants.FRAMEWORK_BUNDLE_PARENT_BOOT;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT;
import static org.osgi.framework.Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA;
import static org.osgi.framework.Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import aQute.bnd.service.remoteworkspace.RemoteWorkspace;
import aQute.bnd.service.specifications.BuilderSpecification;
import aQute.bnd.service.specifications.RunSpecification;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.libg.glob.Glob;
import aQute.libg.parameters.ParameterMap;

public abstract class AbstractLaunchpadBuilder<T extends AbstractLaunchpadBuilder<T>> implements AutoCloseable {

	static final String				EXCLUDEEXPORTS		= "-excludeexports";
	static final String				LAUNCHPAD_NAME		= "launchpad.name";
	static final String				LAUNCHPAD_CLASSNAME	= "launchpad.classname";

	final RunSpecification			local;
	final RemoteWorkspace			remoteWorkspace;
	final File						projectDir;
	boolean							start			= true;
	boolean							testbundle		= true;
	boolean							byReference		= true;
	long							closeTimeout	= 60000;
	boolean							debug;
	final Set<Class<?>>				hide			= new HashSet<>();
	final List<Predicate<String>>	excludeExports	= new ArrayList<>();
	final List<String>				exports			= new ArrayList<>();
	ClassLoader						myClassLoader;
	String							parentLoader		= FRAMEWORK_BUNDLE_PARENT_BOOT;

	public AbstractLaunchpadBuilder(File projectDir, RemoteWorkspace remoteWorkspace) {
		this.projectDir = projectDir;
		local = new RunSpecification();
		this.remoteWorkspace = remoteWorkspace;
	}

	public T bndrun(File file) {
		RunSpecification setup = remoteWorkspace.getRun(file.getAbsolutePath());
		if (!setup.errors.isEmpty()) {
			throw new IllegalArgumentException(
				"Errors while get bndrun file " + file.getAbsolutePath() + "\n" + Strings.join("\n", setup.errors));
		}

		setup.target = null;
		setup.bin = null;
		setup.bin_test = null;
		local.mergeWith(setup);
		return cast();
	}

	public T bndrun(String path) {
		return bndrun(IO.getFile(projectDir, path));
	}

	public T project() {
		return bndrun(projectDir);
	}

	public T gogo() {
		bundles("org.apache.felix.gogo.runtime,org.apache.felix.gogo.command,org.apache.felix.gogo.shell");
		return cast();
	}

	public T bundles(String specification) {
		List<String> config = remoteWorkspace.getLatestBundles(projectDir.getAbsolutePath(), specification);
		config.forEach(local.runbundles::add);
		return cast();
	}

	public T runpath(String specification) {
		List<String> config = remoteWorkspace.getLatestBundles(projectDir.getAbsolutePath(), specification);
		config.forEach(local.runpath::add);
		return cast();
	}

	public T bundles(File... files) {
		of(files).map(File::getAbsolutePath)
			.forEach(local.runpath::add);

		return cast();
	}

	public T runpath(File... files) {
		of(files).map(File::getAbsolutePath)
			.forEach(local.runpath::add);

		return cast();
	}

	public T nostart() {
		this.start = false;
		return cast();
	}

	public T hide(Class<?> hide) {
		this.hide.add(hide);
		return cast();
	}

	public T notestbundle() {
		this.testbundle = false;
		return cast();
	}

	public T runfw(File file) {
		local.runfw.clear();
		local.runfw.add(file.getAbsolutePath());

		return cast();
	}

	public T runfw(String specification) {
		local.runfw = remoteWorkspace.getLatestBundles(projectDir.getAbsolutePath(), specification);
		return cast();
	}

	public T set(String key, String value) {
		local.properties.put(key, value);
		return cast();
	}

	public T closeTimeout(long ms) {
		this.closeTimeout = ms;
		return cast();
	}

	public T usingClassLoader(ClassLoader loader) {
		this.myClassLoader = loader;
		return cast();
	}

	public T debug() {
		this.debug = true;
		return cast();
	}

	/**
	 * Exclude the exports that are matched by any of the given globs
	 *
	 * @param globs the globs to match against the system framework exports
	 * @return this
	 */
	public T excludeExport(String... globs) {
		Stream.of(globs)
			.flatMap((x) -> Strings.splitAsStream(x))
			.map(this::toPredicate)
			.forEach(excludeExports::add);
		return cast();
	}

	/**
	 * Exclude the exports that are matched by any of the given predicates
	 *
	 * @param predicate the predicates to match against the system framework
	 *            exports
	 * @return this
	 */
	public T excludeExport(Predicate<String> predicate) {
		excludeExports.add(predicate);
		return cast();
	}

	public Launchpad create() {
		StackTraceElement element = Thread.currentThread()
			.getStackTrace()[2];
		return create(element.getMethodName(), element.getClassName());
	}

	public Launchpad create(String name) {
		StackTraceElement element = Thread.currentThread()
			.getStackTrace()[2];
		return create(name, element.getClassName());
	}

	public Launchpad create(String name, String className) {
		return unchecked(() -> {
			File storage = IO.getFile(new File(local.target), "launchpad/launchpad-" + randomUUID());
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

			runspec.properties.put(FRAMEWORK_SYSTEMPACKAGES_EXTRA, extraPackages);
			runspec.properties.put(FRAMEWORK_SYSTEMCAPABILITIES_EXTRA, extraCapabilities);
			runspec.properties.put(FRAMEWORK_STORAGE, storage.getAbsolutePath());
			runspec.properties.put(FRAMEWORK_STORAGE_CLEAN, FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
			runspec.properties.put(FRAMEWORK_BUNDLE_PARENT, parentLoader);
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
		});
	}

	private String toLines(Collection<String> set) {
		return set.stream()
			.sorted()
			.collect(Collectors.joining("\n     "));
	}

	/**
	 * We're not closing the framework and reuse the static variables. That
	 * means we never really shut down the remote connection and let the process
	 * exit kill it. This is for efficiency reasons.
	 */
	@Override
	public void close() throws Exception {}

	Framework getFramework(RunSpecification runspec) {
		return unchecked(() -> {
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
		});
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
		return myClassLoader == null ? AbstractLaunchpadBuilder.class.getClassLoader() : myClassLoader;
	}

	private URL toURL(File file) {
		return unchecked(() -> file.toURI()
			.toURL());
	}

	public T snapshot() {
		this.bundles("biz.aQute.bnd.runtime.snapshot");
		return cast();
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

	public T copyInstall() {
		byReference = false;
		return cast();
	}

	public T addCapability(String namespace, String... keyVal) {
		Map<String, String> attrs = new HashMap<>();
		for (int i = 0; i < keyVal.length - 1; i += 2) {
			attrs.put(keyVal[i], keyVal[i + 1]);
		}
		while (local.extraSystemCapabilities.containsKey(namespace))
			namespace += "~";

		local.extraSystemCapabilities.put(namespace, attrs);
		return cast();
	}

	public RunSpecification getLocal() {
		return local;
	}

	public byte[] build(String path, BuilderSpecification spec) {
		return remoteWorkspace.build(path, spec);
	}

	public T export(String spec) {
		exports.add(spec);
		return cast();
	}

	public boolean isDebug() {
		return debug;
	}

	public T applicationLoaderAsParent() {
		this.parentLoader = Constants.FRAMEWORK_BUNDLE_PARENT_APP;
		return cast();
	}

	@SuppressWarnings("unchecked")
	private T cast() {
		return (T) this;
	}

}

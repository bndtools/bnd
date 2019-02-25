package aQute.launchpad;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import aQute.bnd.remoteworkspace.client.RemoteWorkspaceClientFactory;
import aQute.bnd.service.remoteworkspace.RemoteWorkspace;
import aQute.bnd.service.remoteworkspace.RemoteWorkspaceClient;
import aQute.bnd.service.specifications.RunSpecification;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

/**
 * This class is a builder for frameworks that can be used in JUnit testing.
 */
public class LaunchpadBuilder implements AutoCloseable {

	final static ExecutorService	executor	= Executors.newCachedThreadPool();
	final static File				projectDir	= IO.work;
	final static RemoteWorkspace	workspace	= RemoteWorkspaceClientFactory.create(projectDir,
		new RemoteWorkspaceClient() {});
	final static RunSpecification	projectTestSetup;
	final static AtomicInteger		counter		= new AtomicInteger();

	static {
		projectTestSetup = workspace.analyzeTestSetup(IO.work.getAbsolutePath());

		//
		// We only want the raw setup and not the run spec since this
		// makes it impossible to start a clean framework
		//

		projectTestSetup.runbundles.clear();
		projectTestSetup.runpath.clear();
		projectTestSetup.properties.clear();
		projectTestSetup.runfw.clear();

		Runtime.getRuntime()
			.addShutdownHook(new Thread(() -> {
				try {
					workspace.close();
				} catch (IOException e) {
					// ignore
				}
			}));
	}

	RunSpecification	local;
	boolean				start		= true;
	boolean				testbundle	= true;
	long				closeTimeout	= 60000;
	boolean				debug;

	/**
	 * Start a framework assuming the current working directory is the project
	 * directory.
	 */
	public LaunchpadBuilder() {
		local = projectTestSetup.clone();
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
		projectTestSetup.properties.put(key, value);
		return this;
	}

	public LaunchpadBuilder closeTimeout(long ms) {
		this.closeTimeout = ms;
		return this;
	}

	public LaunchpadBuilder debug() {
		this.debug = true;
		return this;
	}

	public Launchpad create() {
		try {
			File storage = IO.getFile(new File(local.target), "launchpad-" + counter.incrementAndGet());
			IO.delete(storage);

			String extraPackages = toString(local.extraSystemPackages);
			String extraCapabilities = toString(local.extraSystemCapabilities);

			local.properties.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, extraPackages);
			local.properties.put(Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA, extraCapabilities);
			local.properties.put(Constants.FRAMEWORK_STORAGE, storage.getAbsolutePath());
			local.properties.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
			local.properties.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK);

			Framework framework = getFramework();

			@SuppressWarnings("resource")
			Launchpad launchpad = new Launchpad(this, framework);

			launchpad.report("Extra system packages %s", local.extraSystemPackages.keySet()
				.stream()
				.collect(Collectors.joining("\n")));
			launchpad.report("Extra system capabilities %s", local.extraSystemCapabilities.keySet()
				.stream()
				.collect(Collectors.joining("\n")));
			launchpad.report("Storage %s", storage.getAbsolutePath());
			launchpad.report("Runpath %s", local.runpath);

			if (start) {
				launchpad.start();
			}

			return launchpad;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
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
	// .collect(Collectors.joining(","));
	//
	// extraSystemPackages.entrySet()
	// .forEach(e -> {
	// e.getValue()
	// .put("uses:", uses);
	// });
	// return extraSystemPackages;
	// }

	private String toString(Map<String, Map<String, String>> map) {
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (Map.Entry<String, Map<String, String>> e : map.entrySet()) {

			String key = e.getKey();
			while (key.endsWith("~"))
				key = key.substring(0, key.length() - 1);

			sb.append(del);
			sb.append(key);
			e.getValue()
				.entrySet()
				.forEach(ee -> {
					sb.append(";");
					sb.append(ee.getKey());
					sb.append("=");
					sb.append("\"");
					for (int i = 0; i < ee.getValue()
						.length(); i++) {
						char c = ee.getValue()
							.charAt(i);
						switch (c) {
							case '\n' :
							case '"' :
								sb.append('\\');
								break;
						}
						sb.append(c);
					}
					sb.append("\"");
				});
			del = ", ";
		}
		return sb.toString();
	}

	/**
	 * We're not closing the framework and reuse the static variables. That
	 * means we never really shut down the remote connection and let the process
	 * exit kill it. This is for efficiency reasons.
	 */
	@Override
	public void close() throws Exception {}

	Framework getFramework() {
		try {
			ClassLoader loader;

			if (local.runpath.isEmpty() && local.runfw.isEmpty()) {
				loader = getMyClassLoader();
			} else {
				List<String> runpath = new ArrayList<>(local.runfw);
				runpath.addAll(local.runpath);
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
			return factory.newFramework(local.properties);
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
		return LaunchpadBuilder.class.getClassLoader();
	}

	private URL toURL(File file) {
		try {
			return file.toURI()
				.toURL();
		} catch (MalformedURLException e) {
			throw Exceptions.duck(e);
		}
	}

}

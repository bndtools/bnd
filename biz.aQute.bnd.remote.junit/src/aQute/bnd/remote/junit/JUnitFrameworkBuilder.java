package aQute.bnd.remote.junit;

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

/**
 * This class is a builder for frameworks that can be used in JUnit testing.
 */
public class JUnitFrameworkBuilder implements AutoCloseable {

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

	/**
	 * Start a framework assuming the current working directory is the project
	 * directory.
	 */
	public JUnitFrameworkBuilder() {
		local = projectTestSetup.clone();
	}

	public JUnitFrameworkBuilder bndrun(File file) {
		RunSpecification setup = workspace.getRun(file.getAbsolutePath());
		local.mergeWith(setup);
		return this;
	}

	public JUnitFrameworkBuilder bndrun(String path) {
		return bndrun(IO.getFile(projectDir, path));
	}

	public JUnitFrameworkBuilder project() {
		return bndrun(projectDir);
	}

	public JUnitFrameworkBuilder gogo() {
		bundles("org.apache.felix.gogo.runtime,org.apache.felix.gogo.command,org.apache.felix.gogo.shell");
		return this;
	}

	public JUnitFrameworkBuilder bundles(String specification) {
		List<String> config = workspace.getLatestBundles(projectDir.getAbsolutePath(), specification);
		config.forEach(local.runbundles::add);
		return this;
	}

	public JUnitFrameworkBuilder runpath(String specification) {
		List<String> config = workspace.getLatestBundles(projectDir.getAbsolutePath(), specification);
		config.forEach(local.runpath::add);
		return this;
	}

	public JUnitFrameworkBuilder bundles(File... files) {
		Stream.of(files)
			.map(File::getAbsolutePath)
			.forEach(local.runpath::add);

		return this;
	}

	public JUnitFrameworkBuilder runpath(File... files) {
		Stream.of(files)
			.map(File::getAbsolutePath)
			.forEach(local.runpath::add);

		return this;
	}

	public JUnitFrameworkBuilder nostart() {
		this.start = false;
		return this;
	}

	public JUnitFrameworkBuilder notestbundle() {
		this.testbundle = false;
		return this;
	}

	public JUnitFrameworkBuilder runfw(File file) {
		local.runfw.clear();
		local.runfw.add(file.getAbsolutePath());

		return this;
	}

	public JUnitFrameworkBuilder runfw(String specification) {
		local.runfw = workspace.getLatestBundles(projectDir.getAbsolutePath(), specification);
		return this;
	}

	public JUnitFrameworkBuilder set(String key, String value) {
		projectTestSetup.properties.put(key, value);
		return this;
	}

	public JUnitFramework create() {
		try {
			File storage = IO.getFile(new File(local.target), "junit-fw-" + counter.incrementAndGet());
			IO.delete(storage);
			local.properties.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, toString(local.extraSystemPackages));
			local.properties.put(Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA, toString(local.extraSystemCapabilities));
			local.properties.put(Constants.FRAMEWORK_STORAGE, storage.getAbsolutePath());
			local.properties.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
			local.properties.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK);

			Framework framework = getFramework();

			@SuppressWarnings("resource")
			JUnitFramework jUnitFramework = new JUnitFramework(this, framework);
			if (start) {
				jUnitFramework.start();
			}

			return jUnitFramework;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private String toString(Map<String, Map<String, String>> map) {
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (Map.Entry<String, Map<String, String>> e : map.entrySet()) {
			sb.append(del);
			sb.append(e.getKey());
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
		System.out.println(sb.toString());
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
				System.out.println("-runpath " + runpath);
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
		return JUnitFrameworkBuilder.class.getClassLoader();
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

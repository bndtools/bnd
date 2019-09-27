package aQute.bnd.junit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.Strategy;
import aQute.bnd.version.VersionRange;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.io.IO;

/**
 * This class provides an OSGi framework that is configured with the current bnd
 * workspace. A project directory is used to find the workspace. This makes all
 * repositories in the workspace available to the framework. To be able to test
 * JUnit code against/in this framework it is necessary that all packages on the
 * buildpath and testpath are actually exported in the framework. This class
 * will ensure that. Once the framework is up and running it will be possible to
 * add bundles to it. There are a number of ways that this can be achieved:
 * <ul>
 * <li>Build a bundle – A bnd {@link Builder} is provided to create a bundle and
 * install it. This makes it possible to add classes from the src or test
 * directories or resources. See {@link #bundle()}.
 * <li>Add a bundle using a bnd spec – Using the bnd specs (e.g.
 * 'org.apache.felix.configadmin;version=3'). See {@link #addBundle(String)} et.
 * al.
 * <li>Add a bndrun file – A file bndrun file can be added. All properties in
 * this file that can be applied after a framework is started will be applied.
 * See {@link #addBundles(File)} et. al.
 * </ul>
 * Convenience methods are added to get services, see {@link #getService(Class)}
 * et. al. Notice that this framework starts in the same process as that the
 * JUnit code runs. This is normally a separately started VM.
 */
@Deprecated // see biz.aQute.bnd.remote.junit
public class JUnitFramework implements AutoCloseable {
	final ExecutorService					executor		= Executors.newCachedThreadPool();
	final PromiseFactory					promiseFactory	= new PromiseFactory(executor);
	public final List<ServiceTracker<?, ?>>	trackers		= new ArrayList<>();
	public final Jar						bin_test;
	public final Framework					framework;
	public final BundleContext				context;
	public final File						projectDir;
	public Workspace						workspace;
	public Project							project;

	/**
	 * Start a framework assuming the current working directory is the project
	 * directory.
	 */
	public JUnitFramework() {
		this(IO.work);
	}

	/**
	 * Start a framework while providing a project directory.
	 *
	 * @param projectDir
	 */
	public JUnitFramework(File projectDir) {
		this.projectDir = projectDir.getAbsoluteFile();

		try {
			Project p = getProject();
			File bin_test = p.getTestOutput();
			this.bin_test = new Jar(bin_test);

			String extra = getExtra();

			Map<String, String> props = new HashMap<>();
			props.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, extra);

			File storage = IO.getFile(p.getTarget(), "fw");
			IO.delete(storage);

			props.put(Constants.FRAMEWORK_STORAGE, IO.absolutePath(storage));
			props.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);

			FrameworkFactory factory = getFactory();

			framework = factory.newFramework(props);
			framework.init();
			framework.start();
			this.context = framework.getBundleContext();
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/*
	 * Calculate the extra packages by calculating the import of the test
	 * classes.
	 */
	private String getExtra() throws Exception {
		try (Analyzer a = new Analyzer()) {

			for (Container c : getProject().getBuildpath()) {
				assert c.getError() == null;
				a.addClasspath(c.getFile());
			}
			for (Container c : getProject().getTestpath()) {
				assert c.getError() == null;
				a.addClasspath(c.getFile());
			}

			a.setJar(bin_test);
			a.removeClose(bin_test);

			a.calcManifest();
			StringBuilder extra = new StringBuilder();
			String del = "";
			for (Entry<PackageRef, Attrs> e : a.getImports()
				.entrySet()) {
				extra.append(del);
				extra.append(e.getKey()
					.getFQN());
				String v = e.getValue()
					.getVersion();
				if (v != null) {
					VersionRange vr = VersionRange.parseOSGiVersionRange(v);
					extra.append(";version=")
						.append(vr.getLow());
				}
				del = ",";
			}
			for (Entry<PackageRef, Attrs> e : a.getContained()
				.entrySet()) {
				extra.append(del);
				extra.append(e.getKey()
					.getFQN());
				String v = e.getValue()
					.getVersion();
				if (v != null) {
					VersionRange vr = VersionRange.parseOSGiVersionRange(v);
					extra.append(";version=")
						.append(vr.getLow());
				}
				del = ",";
			}
			return extra.toString();
		}
	}

	/**
	 * Close this framework
	 */
	@Override
	public void close() throws Exception {
		for (ServiceTracker<?, ?> st : trackers) {
			st.close();
		}
		framework.stop();
		framework.waitForStop(10000);
		executor.shutdownNow();
		bin_test.close();
	}

	public BundleContext getBundleContext() {
		return context;
	}

	public <T> List<T> getServices(Class<T> class1) throws InvalidSyntaxException {
		Collection<ServiceReference<T>> refs = context.getServiceReferences(class1, null);
		List<T> result = new ArrayList<>();
		for (ServiceReference<T> ref : refs) {
			T service = context.getService(ref);
			if (service != null)
				result.add(service);
		}
		return result;
	}

	public <T> T getService(Class<T> class1) throws Exception {
		List<T> services = getServices(class1);
		assert 1 == services.size();
		return services.get(0);
	}

	public <T> Promise<T> waitForService(final Class<T> class1, final long timeoutInMs) throws Exception {
		return promiseFactory.submit(() -> {
			ServiceTracker<T, T> tracker = new ServiceTracker<>(context, class1, null);
			tracker.open();
			T s = tracker.waitForService(timeoutInMs);
			if (s == null) {
				tracker.close();
				throw new Exception("No service object " + class1);
			}
			return s;
		});
	}

	static AtomicInteger n = new AtomicInteger();

	public class BundleBuilder extends Builder {
		Map<String, Resource> additionalResources = new HashMap<>();

		BundleBuilder() {
			setBundleSymbolicName("test-" + n.incrementAndGet());
		}

		public BundleBuilder addResource(String path, URL url) throws IOException {
			return addResource(path, Resource.fromURL(url, getPlugin(HttpClient.class)));
		}

		public BundleBuilder addResource(String path, Resource resource) {
			additionalResources.put(path, resource);
			return this;
		}

		public Bundle install() throws Exception {
			try {
				Jar jar = new Jar("x");
				for (Entry<String, Resource> e : additionalResources.entrySet()) {
					jar.putResource(e.getKey(), e.getValue());
				}
				setJar(jar);
				jar = build();

				try (JarResource j = new JarResource(jar);) {
					return context.installBundle("generated " + jar.getBsn(), j.openInputStream());
				}
			} finally {
				close();
			}
		}

		@Override
		public void close() throws IOException {
			getClasspath().remove(bin_test);
			super.close();
		}

		public BundleBuilder addResource(Class<?> class1) throws IOException {
			String name = class1.getName();
			name = name.replace('.', '/') + ".class";
			addResource(name, class1.getResource("/" + name));
			return this;
		}

	}

	public BundleBuilder bundle() throws IOException {
		BundleBuilder bundleBuilder = new BundleBuilder();
		bundleBuilder.addClasspath(bin_test);
		bundleBuilder.removeClose(bin_test);
		return bundleBuilder;
	}

	public void addBundles(String bndrun) throws Exception {
		addBundles(IO.getFile(bndrun));
	}

	public void addBundles(File bndrun) throws Exception {
		Run run = Run.createRun(getWorkspace(), bndrun);
		List<Bundle> bundles = new ArrayList<>();
		for (Container c : run.getRunbundles()) {
			assert c.getError() == null;
			Bundle bundle = context.installBundle(c.getFile()
				.toURI()
				.toString());
			bundles.add(bundle);
		}
		startAll(bundles);
	}

	public Workspace getWorkspace() throws Exception {
		if (workspace == null) {
			workspace = Workspace.getWorkspace(projectDir.getParentFile());
			// workspace.setOffline(true);
			// TODO fix the loading error
			// assertTrue(workspace.check());
		}
		return workspace;
	}

	public Project getProject() throws Exception {
		if (project == null) {
			project = getWorkspace().getProjectFromFile(projectDir);
			assert project.check();
		}
		return project;
	}

	public void startAll(List<Bundle> bundles) throws BundleException {
		for (Bundle b : bundles) {
			b.start();
		}
	}

	public List<Bundle> addBundle(String spec) throws Exception {
		Parameters p = new Parameters(spec);
		List<Bundle> bundles = new ArrayList<>();
		for (Map.Entry<String, Attrs> e : p.entrySet()) {
			Container c = getProject().getBundle(e.getKey(), e.getValue()
				.get("version"), Strategy.HIGHEST, e.getValue());
			if (c.getError() != null) {
				throw new RuntimeException(c.getError());
			}
			Bundle bundle = context.installBundle(c.getFile()
				.toURI()
				.toString());
			bundles.add(bundle);
		}
		startAll(bundles);
		return bundles;
	}

	private FrameworkFactory getFactory() throws Exception {
		ServiceLoader<FrameworkFactory> sl = ServiceLoader.load(FrameworkFactory.class);
		for (FrameworkFactory ff : sl) {
			return ff;
		}
		throw new FileNotFoundException("No Framework found on classpath");
	}
}

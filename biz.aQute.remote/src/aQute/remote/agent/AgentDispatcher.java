package aQute.remote.agent;

import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.invoke.MethodType.methodType;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import aQute.lib.startlevel.StartLevelRuntimeHandler;
import aQute.remote.api.Agent;
import aQute.remote.api.Supervisor;
import aQute.remote.util.Link;

/**
 * This class collaborates with the Envoy part of this design. After the envoy
 * has installed the -runpath it will reflectively call this class to create a
 * framework and run an {@link AgentServer}.
 */
public class AgentDispatcher {

	//
	// We keep a descriptor for each created framework by its name.
	//
	static List<Descriptor> descriptors = new CopyOnWriteArrayList<>();

	// public because of testing
	public static class Descriptor implements Closeable {
		public AtomicBoolean			closed		= new AtomicBoolean(false);
		public List<AgentServer>		servers		= new CopyOnWriteArrayList<>();
		public Framework				framework;
		public Map<String, Object>		configuration;
		public File						storage;
		public File						shaCache;
		public String					name;
		public List<BundleActivator>	activators	= new ArrayList<>();
		public StartLevelRuntimeHandler	startlevels;

		@Override
		public void close() throws IOException {
			if (closed.getAndSet(true))
				return;

			for (AgentServer as : servers) {
				try {
					as.close();
				} catch (Exception e) {
					// ignore
				}
			}
			for (BundleActivator ba : activators)
				try {
					ba.stop(framework.getBundleContext());
				} catch (Exception e) {
					// ignore
				}
			try {
				framework.stop();
			} catch (BundleException e) {
				// ignore
			}
		}
	}

	/**
	 * Create a new framework. This is reflectively called from the Envoy
	 */
	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	public static Descriptor createFramework(String name, Map<String, Object> configuration, final File storage,
		final File shacache) throws Exception {

		//
		// Use the service loader for loading a framework
		//
		ClassLoader loader = AgentServer.class.getClassLoader();
		ServiceLoader<FrameworkFactory> sl = ServiceLoader.load(FrameworkFactory.class, loader);
		FrameworkFactory ff = null;
		for (FrameworkFactory fff : sl) {
			ff = fff;
			// break;
		}

		if (ff == null)
			throw new IllegalArgumentException("No framework on runpath");

		Descriptor d = new Descriptor();
		//
		// create a new descriptor. This is returned
		// to the envoy side as an Object and we will
		// get this back later in toAgent. The envoy
		// maintains a list of name -> framework
		//

		d.shaCache = shacache;
		d.storage = storage;
		d.configuration = configuration;
		d.name = name;
		boolean isTrace = configuration.get("launch.trace") != null;
		d.startlevels = StartLevelRuntimeHandler.create(
			!isTrace ? AgentDispatcher::dummy : AgentDispatcher::log,
			(Map) configuration);

		//
		// Create the framework
		//

		@SuppressWarnings({
			"unchecked", "rawtypes"
		})
		Framework framework = ff.newFramework((Map) configuration);
		framework.init();
		framework.getBundleContext()
			.addFrameworkListener(event -> {
				// System.err.println("FW Event " + event);
			});
		d.framework = framework;

		d.startlevels.beforeStart(d.framework);
		framework.start();


		String embedded = (String) configuration.get("biz.aQute.remote.embedded");

		if (embedded != null && !(embedded = embedded.trim()).isEmpty()) {
			String activators[] = embedded.trim()
				.split("\\s*,\\s*");
			for (String activator : activators)
				try {
					Class<?> activatorClass = loader.loadClass(activator);
					if (BundleActivator.class.isAssignableFrom(activatorClass)) {

						// TODO check immediate

						BundleActivator ba = (BundleActivator) newInstance(activatorClass);
						ba.start(framework.getBundleContext());
						d.activators.add(ba);
					}
				} catch (Exception e) {
					// TODO
					System.out.println("IGNORED");
					e.printStackTrace();
				}
		}

		return d;
	}

	/**
	 * Create a new agent on an existing framework.
	 */

	public static void toAgent(final Descriptor descriptor, DataInputStream in, DataOutputStream out) {

		//
		// Check if the framework is active
		if (descriptor.framework.getState() != Bundle.ACTIVE) {
			throw new IllegalStateException("Framework " + descriptor.name + " is not active. (Stopped?)");
		}

		//
		// Get the bundle context
		//

		AgentServer as = new AgentServer(descriptor) {
			//
			// Override the close se we can remote it from the list
			//
			@Override
			public void close() throws IOException {
				descriptor.servers.remove(this);
				super.close();
			}
		};

		//
		// Link up
		//
		Link<Agent, Supervisor> link = new Link<>(Supervisor.class, as, in, out);
		as.setLink(link);
		link.open();
	}

	/**
	 * Close
	 */

	public static void close() throws IOException {
		for (Descriptor descriptor : descriptors) {
			descriptor.close();
		}
		for (Descriptor descriptor : descriptors) {
			try {
				descriptor.framework.waitForStop(2000);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}

	private static final MethodType defaultConstructor = methodType(void.class);

	private static <T> T newInstance(Class<T> rawClass) throws Exception {
		try {
			return (T) publicLookup().findConstructor(rawClass, defaultConstructor)
				.invoke();
		} catch (Error | Exception e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private static void dummy(String msg, Object... args) {}

	private static void log( String msg, Object ... args) {
		try {
			System.out.println(String.format(msg, args));
		} catch (Exception e) {
			System.out.println(msg + ":" + Arrays.toString(args));
		}
	}
}

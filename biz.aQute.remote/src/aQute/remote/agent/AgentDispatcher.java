package aQute.remote.agent;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.osgi.framework.*;
import org.osgi.framework.launch.*;

import aQute.remote.api.*;
import aQute.remote.util.*;

/**
 * This class collaborates with the Envoy part of this design. After the envoy
 * has installed the -runpath it will reflectively call this class to create a
 * framework and run an {@link AgentServer}.
 */
public class AgentDispatcher {

	//
	// We keep a descriptor for each created framework by its name.
	//
	static List<Descriptor>	descriptors	= new CopyOnWriteArrayList<Descriptor>();

	static class Descriptor implements Closeable {
		AtomicBoolean		closed	= new AtomicBoolean(false);
		List<AgentServer>	servers	= new CopyOnWriteArrayList<AgentServer>();
		Framework			framework;
		Map<String,Object>	configuration;
		File				storage;
		File				shaCache;
		String				name;

		@Override
		public void close() throws IOException {
			if (closed.getAndSet(true))
				return;

			for (AgentServer as : servers) {
				try {
					as.close();
				}
				catch (Exception e) {
					// ignore
				}
			}
			try {
				framework.stop();
			}
			catch (BundleException e) {
				// ignore
			}
		}
	}

	/**
	 * Create a new framework. This is reflectively called from the Envoy
	 */
	public static Descriptor createFramework(String name, Map<String,Object> configuration, final File storage,
			final File shacache) throws Exception {

		//
		// Use the service loader for loading a framework
		//

		ServiceLoader<FrameworkFactory> sl = ServiceLoader.load(FrameworkFactory.class,
				AgentServer.class.getClassLoader());
		FrameworkFactory ff = null;
		for (FrameworkFactory fff : sl) {
			ff = fff;
			// break;
		}

		if (ff == null)
			throw new IllegalArgumentException("No framework on runpath");

		//
		// Create the framework
		//

		@SuppressWarnings({
				"unchecked", "rawtypes"
		})
		Framework framework = ff.newFramework((Map) configuration);
		framework.init();
		framework.getBundleContext().addFrameworkListener(new FrameworkListener() {

			@Override
			public void frameworkEvent(FrameworkEvent event) {
				// System.err.println("FW Event " + event);
			}
		});

		framework.start();

		//
		// create a new descriptor. This is returned
		// to the envoy side as an Object and we will
		// get this back later in toAgent. The envoy
		// maintains a list of name -> framework
		//

		Descriptor d = new Descriptor();
		d.framework = framework;
		d.shaCache = shacache;
		d.storage = storage;
		d.configuration = configuration;
		d.name = name;

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

		BundleContext context = descriptor.framework.getBundleContext();
		AgentServer as = new AgentServer(descriptor.name, context, descriptor.shaCache) {
			//
			// Override the close se we can remote it from the list
			//
			public void close() throws IOException {
				descriptor.servers.remove(this);
				super.close();
			}
		};

		//
		// Link up
		//
		Link<Agent,Supervisor> link = new Link<Agent,Supervisor>(Supervisor.class, as, in, out);
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
			}
			catch (InterruptedException e) {
				// ignore
			}
		}
	}
}

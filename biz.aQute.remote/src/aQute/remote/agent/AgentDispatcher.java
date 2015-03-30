package aQute.remote.agent;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import aQute.remote.api.Agent;
import aQute.remote.api.Supervisor;
import aQute.remote.util.Link;

public class AgentDispatcher {
	static List<Descriptor>	descriptors = new CopyOnWriteArrayList<Descriptor>();
	
	static class Descriptor implements Closeable {
		AtomicBoolean closed = new AtomicBoolean(false);
		List<AgentServer>	servers = new CopyOnWriteArrayList<AgentServer>();
		Framework framework;
		Map<String, Object> configuration;
		File storage;
		File shaCache;
		String name;
		
		@Override
		public void close() throws IOException {
			if ( closed.getAndSet(true))
				return;

			for ( AgentServer as : servers) {
				as.close();
			}
			try {
				framework.stop();
			} catch (BundleException e) {
				// ignore
			}
		}
	}

	/**
	 * Create a new framework. This is reflectively called from the 
	 * EnvoyDispatcher 
	 */
	public static Descriptor createFramework(String name,
			Map<String, Object> configuration, final File storage,
			final File shacache) throws Exception {

		ServiceLoader<FrameworkFactory> sl = ServiceLoader.load(
				FrameworkFactory.class, AgentServer.class.getClassLoader());
		FrameworkFactory ff = null;
		for (FrameworkFactory fff : sl) {
			ff = fff;
			// break;
		}

		if (ff == null)
			throw new IllegalArgumentException("No framework on runpath");

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Framework framework = ff.newFramework((Map) configuration);
		framework.init();
		framework.getBundleContext().addFrameworkListener(
				new FrameworkListener() {

					@Override
					public void frameworkEvent(FrameworkEvent event) {
						System.err.println("FW Event " + event);
					}
				});

		framework.start();
		
		Descriptor d = new Descriptor();
		d.framework = framework;
		d.shaCache =shacache;
		d.storage=storage;
		d.configuration=configuration;
		d.name = name;
		
		return d;
	}

	/**
	 * Create a new agent on an existing framework.
	 */
	
	
	public static void toAgent(final Descriptor descriptor, DataInputStream in, DataOutputStream out) {
		assert descriptor.framework.getState() == Bundle.ACTIVE;
		
		BundleContext context= descriptor.framework.getBundleContext();
		AgentServer as = new AgentServer(descriptor.name, context, descriptor.shaCache) {
			public void close() throws IOException {
				descriptor.servers.remove(this);
				super.close();
			}
		};
		Link<Agent,Supervisor> link = new Link<Agent, Supervisor>(Supervisor.class, as, in,out);
		as.setLink(link);
		link.open();
	}
	
	/**
	 * Close
	 * @throws IOException 
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
}

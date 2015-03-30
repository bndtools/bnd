package aQute.remote.main;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Constants;

import aQute.libg.shacache.ShaCache;
import aQute.libg.shacache.ShaSource;
import aQute.remote.util.Link;
import aQute.service.reporter.Reporter;

/**
 * Creates a framework and through that framework's class loader it will create
 * an AgentServer.
 */
public class EnvoyDispatcher implements Closeable {
	private ShaCache cache;
	private ShaSource source;
	private Reporter main;
	private File storage;
	private String network;
	private int port;
	private Map<String, DispatcherInfo> frameworks = new HashMap<String, EnvoyDispatcher.DispatcherInfo>();
	private Set<EnvoyImpl> envoys = new HashSet<EnvoyImpl>();

	class DispatcherInfo {
		String name;
		int port;
		URLClassLoader cl;
		Class<?> dispatcher;
		Map<String, Object> properties;
		Collection<String> runpath;
		Closeable framework;
		File storage;

		void close() {
			try {
				main.trace("closing framework for %s", this);
				framework.close();
				frameworks.remove(name);
			} catch (Exception e) {
				main.exception(e, "Closing framework for %s", this);
			}
		}
		
		public String toString() {
			return name + "(" + framework + ") ["+port+"]";
		}
	}

	class EnvoyImpl implements Envoy {

		private Link<Envoy, EnvoySupervisor> link;

		EnvoyImpl(Socket socket) throws IOException {
			envoys.add(this);
			this.link = new Link<Envoy, EnvoySupervisor>(EnvoySupervisor.class,
					this, socket.getInputStream(), socket.getOutputStream());
			setRemote(this.link.getRemote());
		}
		
		void open() {
			link.open();
		}

		@Override
		public boolean createFramework(String name, Collection<String> runpath,
				Map<String, Object> properties) throws Exception {
			main.trace("create framework %s - %s --- %s", name, runpath,
					properties);

			if (!name.matches("[a-zA-Z0-9_.$-]+"))
				throw new IllegalArgumentException(
						"Name must match symbolic name");

			try {
				DispatcherInfo existing = frameworks.get(name);
				if (existing != null) {
					if (existing.runpath.equals(runpath)
							&& existing.properties.equals(properties)) {
						createAgent(existing);
						return false;
					} else {
						existing.close();
						frameworks.remove(name);
					}
				}

				DispatcherInfo info = create(name,runpath,properties);
				frameworks.put(name,info);
				createAgent(info);

				return true;
			} catch (Exception e) {
				main.trace("creating framework %s", e);
				main.exception(e, "creating framework");
				throw e;
			}
		}

		private void createAgent(DispatcherInfo info) throws Exception {
			main.trace("Adding an agent for %s (%s)", info);
			link.transfer();
			Method newAgent = info.dispatcher.getMethod("createAgent",
					DataInputStream.class, DataOutputStream.class);
			
			newAgent.invoke(info.framework, link.getInput(), link.getOutput());
			close();
		}

		private DispatcherInfo create(String name, Collection<String> runpath,
				Map<String, Object> properties) throws Exception {
			List<URL> files = new ArrayList<URL>();

			for (String sha : runpath) {
				files.add(cache.getFile(sha, source).toURI().toURL());
			}

			main.trace("runpath %s", files);

			DispatcherInfo info = new DispatcherInfo();
			info.name = name;
			info.cl = new URLClassLoader(
					files.toArray(new URL[files.size()]));
			info.properties = properties;
			info.runpath = runpath;
			info.storage = new File(storage, name);
			info.dispatcher = info.cl
					.loadClass("aQute.remote.agent.AgentDispatcher");

			File storage = new File(EnvoyDispatcher.this.storage, name);
			storage.mkdirs();
			if (!storage.isDirectory())
				throw new IllegalArgumentException(
						"Cannot create framework storage " + storage);

			properties.put(Constants.FRAMEWORK_STORAGE,
					info.storage.getAbsolutePath());

			Method newFw = info.dispatcher.getMethod("createFramework",
					String.class, Map.class, File.class, File.class);

			info.framework = (Closeable) newFw.invoke(null, name, properties, storage,
					cache.getRoot());
			
			return info;
		}

		@Override
		public boolean isEnvoy() {
			return true;
		}

		public void close() throws IOException {
			if (envoys.remove(this) && link!=null)
				link.close();
			link = null;
		}

	}

	public EnvoyDispatcher(Reporter main, File cache, File storage,
			String network, int port) {
		this.main = main;
		this.cache = new ShaCache(cache);
		this.storage = storage;
		this.network = network;
		this.port = port;
	}

	public void setRemote(final EnvoySupervisor remote) {
		this.source = new ShaSource() {

			@Override
			public boolean isFast() {
				return false;
			}

			@Override
			public InputStream get(String sha) throws Exception {
				byte[] data = remote.getFile(sha);
				if (data == null)
					return null;

				return new ByteArrayInputStream(data);
			}

		};
	}

	@Override
	public void close() throws IOException {
		for (EnvoyImpl envoy : envoys)
			envoy.close();

		for (DispatcherInfo di : frameworks.values()) {
			di.close();
		}
	}

	public void run() {
		while (!Thread.currentThread().isInterrupted())
			try {
				ServerSocket server = network.equals("*") ? new ServerSocket(
						port) : new ServerSocket(port, 3,
						InetAddress.getByName(network));
				main.trace("Will wait  for %s:%s to finish",
						InetAddress.getLocalHost(), port);
				Socket socket = server.accept();
				main.trace("Got a request on %s", socket);
				EnvoyImpl envoyImpl = new EnvoyImpl(socket);
				envoyImpl.open();
			} catch (Exception e) {
				main.exception(e,
						"while listening for incoming requests on %s:%s",
						network, port);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
				}
			}
	}
}

package aQute.remote.main;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Constants;

import aQute.libg.shacache.ShaCache;
import aQute.libg.shacache.ShaSource;
import aQute.remote.util.Linkable;
import aQute.service.reporter.Reporter;

/**
 * Creates a framework and through that framework's class loader it will create
 * an AgentServer.
 */
public class EnvoyImpl implements Envoy, Linkable<Envoy, EnvoySupervisor> {
	private ShaCache cache;
	private ShaSource source;
	private Reporter main;
	private File storage;

	public EnvoyImpl(Reporter main, ShaCache cache, File storage) {
		this.main = main;
		this.cache = cache;
		this.storage=storage;
	}

	@Override
	public int createFramework(String name, Collection<String> runpath,
			Map<String, Object> properties) throws Exception {
		main.trace("create framework %s - %s --- %s", name, runpath, properties);

		if ( !name.matches("[a-zA-Z0-9_.$-]+"))
			throw new IllegalArgumentException("Name must match symbolic name");
		
		List<URL> files = new ArrayList<URL>();

		for (String sha : runpath) {
			files.add(cache.getFile(sha, source).toURI().toURL());
		}

//		String[] jcp = System.getProperty("java.class.path").split(
//				Pattern.quote(File.pathSeparator));
//		for (String path : jcp) {
//			File f = new File(path);
//			files.add(f.toURI().toURL());
//		}
		main.trace("runpath %s", files);

		try {
			
			URLClassLoader cl = new URLClassLoader(files.toArray(new URL[files.size()]));
			Class<?> c = cl.loadClass("aQute.remote.agent.AgentServer");
			
			File storage = new File(this.storage, name);
			storage.mkdirs();
			if ( !storage.isDirectory())
				throw new IllegalArgumentException("Cannot create framework storage " + storage);
			
			properties.put(Constants.FRAMEWORK_STORAGE, storage.getAbsolutePath());
			
			Method newFw = c
					.getMethod("createFramework", String.class, Map.class, File.class, File.class);
			return (Integer) newFw.invoke(null, name, properties, storage, cache.getRoot());
		} catch (Exception e) {
			main.trace("creating framework %s", e);
			main.exception(e, "creating framework");
			throw e;
		}
	}

	@Override
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
	}

	@Override
	public boolean isEnvoy() {
		return true;
	}

	@Override
	public Envoy get() {
		return this;
	}
}

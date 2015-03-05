package aQute.remote.agent.provider;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;

import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;
import aQute.libg.shacache.ShaCache;
import aQute.remote.api.Envoy;
import aQute.remote.api.Supervisor;
import aQute.service.reporter.Reporter;

/**
 * Creates a framework and through that framework's class loader it will create
 * an AgentServer.
 */
public class EnvoyImpl implements Envoy, Linkable<Envoy, Supervisor> {

	private Supervisor remote;
	private ShaCache cache;
	private SupervisorSource source;
	private Reporter main;
	private File storage;

	public EnvoyImpl(Reporter main, ShaCache cache, File storage) {
		this.main = main;
		this.cache = cache;
		this.storage=storage;
	}

	@Override
	public int createFramework(String name, List<String> runpath,
			Map<String, Object> properties) throws Exception {
		main.trace("create framework %s - %s --- %s", name, runpath, properties);

		if ( !name.matches("[a-zA-Z0-9_.$-]+"))
			throw new IllegalArgumentException("Name must match symbolic name");
		
		List<URL> files = new ArrayList<URL>();

		for (String sha : runpath) {
			files.add(cache.getFile(sha, source).toURI().toURL());
		}

		String[] jcp = System.getProperty("java.class.path").split(
				Pattern.quote(File.pathSeparator));
		for (String path : jcp) {
			File f = new File(path);
			files.add(f.toURI().toURL());
		}
		main.trace("runpath %s", files);

		try {
			URL[] classpath = files.toArray(new URL[files.size()]);
			@SuppressWarnings("resource")
			URLClassLoader cl = new URLClassLoader(classpath);
			Class<?> c = cl.loadClass(AgentServer.class.getName());
			
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
	public Map<String, String> getSystemProperties() throws Exception {
		main.trace("get system properties");
		return Converter.cnv(new TypeReference<Map<String, String>>() {
		}, System.getProperties());
	}

	@Override
	public Envoy get() {
		return this;
	}

	@Override
	public void setRemote(Supervisor remote) {
		this.remote = remote;
		this.source = new SupervisorSource(this.remote);
	}

	@Override
	public void close() throws IOException {
	}

}

package aQute.remote.main;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.osgi.framework.Constants;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;

import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;
import aQute.libg.shacache.ShaCache;
import aQute.remote.api.Agent;
import aQute.remote.api.Linkable;
import aQute.remote.api.Supervisor;
import aQute.remote.util.SupervisorSource;
import aQute.service.reporter.Reporter;

/**
 * Creates a framework and through that framework's class loader it will create
 * an AgentServer.
 */
public class EnvoyImpl implements Agent, Linkable<Agent, Supervisor> {

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
	public int createFramework(String name, Collection<String> runpath,
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
	public Map<String, String> getSystemProperties() throws Exception {
		main.trace("get system properties");
		return Converter.cnv(new TypeReference<Map<String, String>>() {
		}, System.getProperties());
	}

	@Override
	public void setRemote(Supervisor remote) {
		this.remote = remote;
		this.source = new SupervisorSource(this.remote);
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public AgentType getType() {
		return AgentType.envoy;
	}

	@Override
	public Agent get() {
		return this;
	}


	@Override
	public FrameworkDTO getFramework() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BundleDTO install(String location, String sha) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String start(long... id) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String stop(long... id) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String uninstall(long... id) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String update(Map<String, String> bundles) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void redirect(boolean on) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stdin(String s) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String shell(String cmd) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean abort() {
		// TODO Auto-generated method stub
		return false;
	}

}

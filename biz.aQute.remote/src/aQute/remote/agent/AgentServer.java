package aQute.remote.agent;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.osgi.framework.*;
import org.osgi.framework.dto.*;

import aQute.lib.converter.*;
import aQute.libg.shacache.*;
import aQute.remote.api.*;
import aQute.remote.api.Event.Type;
import aQute.remote.util.*;

/**
 * Implementation of the Agent. This implementation implements the Agent
 * interfaces and communicates with a Supervisor interfaces.
 */
public class AgentServer implements Agent, Closeable, FrameworkListener {

	//
	// Constant so we do not have to repeat it
	//

	private static final TypeReference<Map<String,String>>	MAP_STRING_STRING_T	= new TypeReference<Map<String,String>>() {};

	private static final long[]								EMPTY				= new long[0];

	//
	// Known keys in the framework properties since we cannot
	// iterate over framework properties
	//

	@SuppressWarnings("deprecation")
	static String											keys[]				= {
			Constants.FRAMEWORK_BEGINNING_STARTLEVEL, Constants.FRAMEWORK_BOOTDELEGATION,
			Constants.FRAMEWORK_BSNVERSION, Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_TRUST_REPOSITORIES,
			Constants.FRAMEWORK_COMMAND_ABSPATH, Constants.FRAMEWORK_EXECPERMISSION,
			Constants.FRAMEWORK_EXECUTIONENVIRONMENT, Constants.FRAMEWORK_LANGUAGE,
			Constants.FRAMEWORK_LIBRARY_EXTENSIONS, Constants.FRAMEWORK_OS_NAME, Constants.FRAMEWORK_OS_VERSION,
			Constants.FRAMEWORK_PROCESSOR, Constants.FRAMEWORK_SECURITY, Constants.FRAMEWORK_STORAGE,
			Constants.FRAMEWORK_SYSTEMCAPABILITIES, Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA,
			Constants.FRAMEWORK_SYSTEMPACKAGES, Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, Constants.FRAMEWORK_UUID,
			Constants.FRAMEWORK_VENDOR, Constants.FRAMEWORK_VERSION, Constants.FRAMEWORK_WINDOWSYSTEM,
																				};

	private Supervisor										remote;
	private BundleContext									context;
	private final ShaCache									cache;
	private ShaSource										source;
	private final Map<String,String>						installed			= new HashMap<String,String>();
	volatile boolean										quit;
	private static Map<String,AgentDispatcher>				instances			= new HashMap<String,AgentDispatcher>();
	private Redirector										redirector			= new NullRedirector();
	private Link<Agent,Supervisor>							link;

	/**
	 * An agent server is based on a context and takes a name and cache
	 * directory
	 * 
	 * @param name
	 *            the name of the agent's framework
	 * @param context
	 *            a bundle context of the framework
	 * @param cache
	 *            the directory for caching
	 */
	public AgentServer(String name, BundleContext context, File cache) {
		this.context = context;
		if (this.context != null)
			this.context.addFrameworkListener(this);

		this.cache = new ShaCache(cache);
	}

	/**
	 * Get the framework's DTO
	 */
	@Override
	public FrameworkDTO getFramework() throws Exception {
		FrameworkDTO fw = new FrameworkDTO();
		fw.bundles = getBundles();
		fw.properties = getProperties();
		fw.services = getServiceReferences();
		return fw;
	}

	@Override
	public BundleDTO install(String location, String sha) throws Exception {
		InputStream in = cache.getStream(sha, source);
		if (in == null)
			return null;

		Bundle b = context.installBundle(location, in);
		installed.put(b.getLocation(), sha);
		return toDTO(b);
	}

	@Override
	public String start(long... ids) {
		StringBuilder sb = new StringBuilder();

		for (long id : ids) {
			Bundle bundle = context.getBundle(id);
			try {
				bundle.start();
			}
			catch (BundleException e) {
				sb.append(e.getMessage()).append("\n");
			}
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	@Override
	public String stop(long... ids) {
		StringBuilder sb = new StringBuilder();

		for (long id : ids) {
			Bundle bundle = context.getBundle(id);
			try {
				bundle.stop();
			}
			catch (BundleException e) {
				sb.append(e.getMessage()).append("\n");
			}
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	@Override
	public String uninstall(long... ids) {
		StringBuilder sb = new StringBuilder();

		for (long id : ids) {
			Bundle bundle = context.getBundle(id);
			try {
				bundle.uninstall();
				installed.remove(bundle.getBundleId());
			}
			catch (BundleException e) {
				sb.append(e.getMessage()).append("\n");
			}
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	@Override
	public String update(Map<String,String> bundles) {

		Formatter out = new Formatter();
		if (bundles == null) {
			bundles = Collections.emptyMap();
		}

		Set<String> toBeDeleted = new HashSet<String>(installed.keySet());
		toBeDeleted.removeAll(bundles.keySet());

		Set<String> toBeInstalled = new HashSet<String>(bundles.keySet());
		toBeInstalled.removeAll(installed.keySet());

		Map<String,String> changed = new HashMap<String,String>(bundles);
		changed.values().removeAll(installed.values());
		changed.keySet().removeAll(toBeInstalled);

		Set<String> affected = new HashSet<String>(toBeDeleted);
		affected.addAll(changed.keySet());

		Set<Bundle> toBeStarted = new HashSet<Bundle>();

		for (String location : affected) {
			Bundle b = getBundle(location);
			if (b == null) {
				out.format("Could not location bundle %s to stop it", location);
				continue;
			}

			try {
				if (isActive(b))
					toBeStarted.add(b);

				b.stop();
			}
			catch (Exception e) {
				printStack(e);
				out.format("Trying to stop bundle %s : %s", b, e);
			}

		}

		for (String location : toBeDeleted) {
			Bundle b = getBundle(location);
			if (b == null) {
				out.format("Could not find bundle %s to uninstall it", location);
				continue;
			}

			try {
				b.uninstall();
				installed.remove(location);
				toBeStarted.remove(b);
			}
			catch (Exception e) {
				printStack(e);
				out.format("Trying to uninstall %s: %s", location, e);
			}
		}

		for (String location : toBeInstalled) {
			String sha = bundles.get(location);

			try {
				InputStream in = cache.getStream(sha, source);
				if (in == null) {
					out.format("Could not find file with sha %s for bundle %s", sha, location);
					continue;
				}

				Bundle b = context.installBundle(location, in);
				installed.put(location, sha);
				toBeStarted.add(b);

			}
			catch (Exception e) {
				printStack(e);
				out.format("Trying to install %s: %s", location, e);
			}
		}

		for (Entry<String,String> e : changed.entrySet()) {
			String location = e.getKey();
			String sha = e.getValue();

			try {
				InputStream in = cache.getStream(sha, source);
				if (in == null) {
					out.format("Cannot find file for sha %s to update %s", sha, location);
					continue;
				}

				Bundle bundle = getBundle(location);
				if (bundle == null) {
					out.format("No such bundle for location %s while trying to update it", location);
					continue;
				}

				if (bundle.getState() == Bundle.UNINSTALLED)
					context.installBundle(location, in);
				else
					bundle.update(in);

			}
			catch (Exception e1) {
				printStack(e1);
				out.format("Trying to update %s: %s", location, e);
			}
		}

		for (Bundle b : toBeStarted) {
			try {
				b.start();
			}
			catch (Exception e1) {
				printStack(e1);
				out.format("Trying to start %s: %s", b, e1);
			}
		}

		String result = out.toString();
		out.close();
		if (result.length() == 0)
			return null;

		return result;
	}

	private Bundle getBundle(String location) {
		try {
			Bundle bundle = context.getBundle(location);
			return bundle;
		}
		catch (Exception e) {
			printStack(e);
		}
		return null;
	}

	private boolean isActive(Bundle b) {
		return b.getState() == Bundle.ACTIVE || b.getState() == Bundle.STARTING;
	}

	@Override
	public boolean redirect(int port) throws Exception {
		if (redirector != null) {
			if (redirector.getPort() == port)
				return false;

			redirector.close();
			redirector = new NullRedirector();
		}

		if (port == Agent.NONE)
			return true;

		if (port <= Agent.COMMAND_SESSION) {
			try {
				redirector = new GogoRedirector(this, context);
			}
			catch (Exception e) {
				throw new IllegalStateException("Gogo is not present in this framework", e);
			}
			return true;
		}

		if (port == Agent.CONSOLE) {
			redirector = new ConsoleRedirector(this);
			return true;
		}

		redirector = new SocketRedirector(this, port);
		return true;
	}

	@Override
	public boolean stdin(String s) throws Exception {
		if (redirector != null) {
			redirector.stdin(s);
			return true;
		}
		return false;
	}

	@Override
	public String shell(String cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setSupervisor(Supervisor remote) {
		setRemote(remote);
	}

	private List<ServiceReferenceDTO> getServiceReferences() throws Exception {
		ServiceReference< ? >[] refs = context.getAllServiceReferences(null, null);
		if (refs == null)
			return Collections.emptyList();

		ArrayList<ServiceReferenceDTO> list = new ArrayList<ServiceReferenceDTO>(refs.length);
		for (ServiceReference< ? > r : refs) {
			ServiceReferenceDTO ref = new ServiceReferenceDTO();
			ref.bundle = r.getBundle().getBundleId();
			ref.id = (Long) r.getProperty(Constants.SERVICE_ID);
			ref.properties = getProperties(r);
			Bundle[] usingBundles = r.getUsingBundles();
			if (usingBundles == null)
				ref.usingBundles = EMPTY;
			else {
				ref.usingBundles = new long[usingBundles.length];
				for (int i = 0; i < usingBundles.length; i++) {
					ref.usingBundles[i] = usingBundles[i].getBundleId();
				}
			}
			list.add(ref);
		}
		return list;
	}

	private Map<String,Object> getProperties(ServiceReference< ? > ref) {
		Map<String,Object> map = new HashMap<String,Object>();
		for (String key : ref.getPropertyKeys())
			map.put(key, ref.getProperty(key));
		return map;
	}

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	private Map<String,Object> getProperties() {
		Map map = new HashMap();
		map.putAll(System.getenv());
		map.putAll(System.getProperties());
		for (String key : keys) {
			Object value = context.getProperty(key);
			if (value != null)
				map.put(key, value);
		}
		return map;
	}

	private List<BundleDTO> getBundles() {
		Bundle[] bundles = context.getBundles();
		ArrayList<BundleDTO> list = new ArrayList<BundleDTO>(bundles.length);
		for (Bundle b : bundles) {
			list.add(toDTO(b));
		}
		return list;
	}

	private BundleDTO toDTO(Bundle b) {
		BundleDTO bd = new BundleDTO();
		bd.id = b.getBundleId();
		bd.lastModified = b.getLastModified();
		bd.state = b.getState();
		bd.symbolicName = b.getSymbolicName();
		bd.version = b.getVersion() == null ? "0" : b.getVersion().toString();
		return bd;
	}

	void cleanup(int event) throws Exception {
		if (quit)
			return;

		instances.remove(this);
		quit = true;
		update(null);
		redirect(0);
		sendEvent(event);
		link.close();
	}

	@Override
	public void close() throws IOException {
		try {
			cleanup(-2);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void abort() throws Exception {
		cleanup(-3);
	}

	private void sendEvent(int code) {
		Event e = new Event();
		e.type = Event.Type.exit;
		e.code = code;
		try {
			remote.event(e);
		}
		catch (Exception e1) {
			printStack(e1);
		}
	}

	@Override
	public void frameworkEvent(FrameworkEvent event) {
		try {
			Event e = new Event();
			e.type = Type.framework;
			e.code = event.getType();
			remote.event(e);
		}
		catch (Exception e1) {
			printStack(e1);
		}
	}

	private void printStack(Exception e1) {
		try {
			e1.printStackTrace(redirector.getOut());
		}
		catch (Exception e) {
			//
		}
	}

	public void setRemote(Supervisor supervisor) {
		this.remote = supervisor;
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
	public boolean isEnvoy() {
		return false;
	}

	@Override
	public Map<String,String> getSystemProperties() throws Exception {
		return Converter.cnv(MAP_STRING_STRING_T, System.getProperties());
	}

	@Override
	public boolean createFramework(String name, Collection<String> runpath, Map<String,Object> properties)
			throws Exception {
		throw new UnsupportedOperationException("This is an agent, we can't create new frameworks (for now)");
	}

	public Supervisor getSupervisor() {
		return remote;
	}

	public void setLink(Link<Agent,Supervisor> link) {
		setRemote(link.getRemote());
		this.link = link;
	}

	public boolean ping() {
		return true;
	}

	public BundleContext getContext() {
		return context;
	}
}

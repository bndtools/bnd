package aQute.remote.agent;

import static aQute.remote.api.XResultDTO.SKIPPED;
import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.dto.CapabilityDTO;
import org.osgi.resource.dto.RequirementDTO;
import org.osgi.util.tracker.ServiceTracker;

import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;
import aQute.lib.io.ByteBufferInputStream;
import aQute.lib.startlevel.StartLevelRuntimeHandler;
import aQute.libg.shacache.ShaCache;
import aQute.libg.shacache.ShaSource;
import aQute.remote.agent.AgentDispatcher.Descriptor;
import aQute.remote.api.Agent;
import aQute.remote.api.AgentExtension;
import aQute.remote.api.Event;
import aQute.remote.api.Event.Type;
import aQute.remote.api.Supervisor;
import aQute.remote.api.XBundleDTO;
import aQute.remote.api.XComponentDTO;
import aQute.remote.api.XConfigurationDTO;
import aQute.remote.api.XPropertyDTO;
import aQute.remote.api.XResultDTO;
import aQute.remote.api.XServiceDTO;
import aQute.remote.api.XThreadDTO;
import aQute.remote.util.Link;

/**
 * Implementation of the Agent. This implementation implements the Agent
 * interfaces and communicates with a Supervisor interfaces.
 */
public class AgentServer implements Agent, Closeable, FrameworkListener {
	private static final Pattern							BSN_P				= Pattern.compile("\\s*([^;\\s]+).*");
	private static final AtomicInteger						sequence			= new AtomicInteger(1000);

	//
	// Constant so we do not have to repeat it
	//

	private static final TypeReference<Map<String, String>>	MAP_STRING_STRING_T	= new TypeReference<Map<String, String>>() {};

	private static final long[]								EMPTY				= new long[0];

	//
	// Known keys in the framework properties since we cannot
	// iterate over framework properties
	//

	@SuppressWarnings("deprecation")
	static final String										keys[]				= {
		Constants.FRAMEWORK_BEGINNING_STARTLEVEL, Constants.FRAMEWORK_BOOTDELEGATION, Constants.FRAMEWORK_BSNVERSION,
		Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_TRUST_REPOSITORIES, Constants.FRAMEWORK_COMMAND_ABSPATH,
		Constants.FRAMEWORK_EXECPERMISSION, Constants.FRAMEWORK_EXECUTIONENVIRONMENT, Constants.FRAMEWORK_LANGUAGE,
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
	private final Map<String, String>						installed			= new HashMap<>();
	volatile boolean										quit;
	private Redirector										redirector			= new NullRedirector();
	private Link<Agent, Supervisor>							link;
	private CountDownLatch									refresh				= new CountDownLatch(0);
	private final StartLevelRuntimeHandler					startlevels;
	private final int										startOptions;

	private final ServiceTracker<Object, Object>					scrTracker;
	private final ServiceTracker<Object, Object>					metatypeTracker;
	private final ServiceTracker<Object, Object>					configAdminTracker;
	private final ServiceTracker<Object, Object>					gogoCommandsTracker;
	private final ServiceTracker<AgentExtension, AgentExtension>	agentExtensionTracker;

	private final Set<String>										gogoCommands		= new CopyOnWriteArraySet<>();
	private final Map<String, AgentExtension>						agentExtensions		= new ConcurrentHashMap<>();

	/**
	 * An agent server is based on a context and takes a name and cache
	 * directory
	 *
	 * @param name the name of the agent's framework
	 * @param context a bundle context of the framework
	 * @param cache the directory for caching
	 */

	public AgentServer(String name, BundleContext context, File cache) throws Exception {
		this(name, context, cache, StartLevelRuntimeHandler.absent());
	}

	public AgentServer(String name, BundleContext context, File cache, StartLevelRuntimeHandler startlevels)
		throws Exception {
		requireNonNull(context, "Bundle context cannot be null");
		this.context = context;

		boolean eager = context.getProperty(aQute.bnd.osgi.Constants.LAUNCH_ACTIVATION_EAGER) != null;
		startOptions = eager ? 0 : Bundle.START_ACTIVATION_POLICY;

		this.cache = new ShaCache(cache);
		this.startlevels = startlevels;
		this.context.addFrameworkListener(this);

		final Filter gogoCommandFilter = context.createFilter("(osgi.command.scope=*)");

		metatypeTracker = new ServiceTracker<>(context, "org.osgi.service.metatype.MetaTypeService", null);
		configAdminTracker = new ServiceTracker<>(context, "org.osgi.service.cm.ConfigurationAdmin", null);
		scrTracker = new ServiceTracker<>(context, "org.osgi.service.component.runtime.ServiceComponentRuntime", null);
		agentExtensionTracker = new ServiceTracker<AgentExtension, AgentExtension>(context, AgentExtension.class,
			null) {

			@Override
			public AgentExtension addingService(ServiceReference<AgentExtension> reference) {
				Object name = reference.getProperty(AgentExtension.PROPERTY_KEY);
				if (name == null) {
					return null;
				}
				AgentExtension tracked = super.addingService(reference);
				agentExtensions.put(name.toString(), tracked);
				return tracked;
			}

			@Override
			public void modifiedService(ServiceReference<AgentExtension> reference, AgentExtension service) {
				removedService(reference, service);
				addingService(reference);
			}

			@Override
			public void removedService(ServiceReference<AgentExtension> reference, AgentExtension service) {
				Object name = reference.getProperty(AgentExtension.PROPERTY_KEY);
				if (name == null) {
					return;
				}
				agentExtensions.remove(name);
			}
		};
		gogoCommandsTracker = new ServiceTracker<Object, Object>(context, gogoCommandFilter, null) {
			@Override
			public Object addingService(final ServiceReference<Object> reference) {
				final String scope = String.valueOf(reference.getProperty("osgi.command.scope"));
				final String[] functions = adapt(reference.getProperty("osgi.command.function"));
				addCommand(scope, functions);
				return super.addingService(reference);
			}

			@Override
			public void removedService(final ServiceReference<Object> reference, final Object service) {
				final String scope = String.valueOf(reference.getProperty("osgi.command.scope"));
				final String[] functions = adapt(reference.getProperty("osgi.command.function"));
				removeCommand(scope, functions);
			}

			private String[] adapt(final Object value) {
				if (value instanceof String[]) {
					return (String[]) value;
				}
				return new String[] {
					value.toString()
				};
			}

			private void addCommand(final String scope, final String... commands) {
				Stream.of(commands)
					.forEach(cmd -> gogoCommands.add(scope + ":" + cmd));
			}

			private void removeCommand(final String scope, final String... commands) {
				Stream.of(commands)
					.forEach(cmd -> gogoCommands.remove(scope + ":" + cmd));
			}
		};
		scrTracker.open();
		metatypeTracker.open();
		configAdminTracker.open();
		gogoCommandsTracker.open();
		agentExtensionTracker.open();
	}

	AgentServer(Descriptor d) throws Exception {
		this(d.name, d.framework.getBundleContext(), d.shaCache, d.startlevels);
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
	public BundleDTO installWithData(String location, byte[] data) throws Exception {
		requireNonNull(data);

		Bundle installedBundle;

		if (location == null) {
			location = getLocation(data);
		}

		try (InputStream stream = new ByteBufferInputStream(data)) {
			installedBundle = context.getBundle(location);
			if (installedBundle == null) {
				installedBundle = context.installBundle(location, stream);
			} else {
				installedBundle.update(stream);
				refresh(true);
			}
		}
		return toDTO(installedBundle);
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
	public BundleDTO installFromURL(String location, String url) throws Exception {
		InputStream is = new URL(url).openStream();
		Bundle b = context.installBundle(location, is);
		installed.put(b.getLocation(), url);
		return toDTO(b);
	}

	@Override
	public String start(long... ids) {
		StringBuilder sb = new StringBuilder();

		for (long id : ids) {
			Bundle bundle = context.getBundle(id);
			try {
				bundle.start(startOptions);
			} catch (BundleException e) {
				sb.append(e.getMessage())
					.append("\n");
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
			} catch (BundleException e) {
				sb.append(e.getMessage())
					.append("\n");
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
				installed.remove(bundle.getLocation());
			} catch (BundleException e) {
				sb.append(e.getMessage())
					.append("\n");
			}
		}
		return sb.length() == 0 ? null : sb.toString();
	}

	@Override
	public String update(Map<String, String> bundles) throws InterruptedException {

		refresh.await();

		Formatter out = new Formatter();
		if (bundles == null) {
			bundles = Collections.emptyMap();
		}

		Set<String> toBeDeleted = new HashSet<>(installed.keySet());
		toBeDeleted.removeAll(bundles.keySet());

		LinkedHashSet<String> toBeInstalled = new LinkedHashSet<>(bundles.keySet());
		toBeInstalled.removeAll(installed.keySet());

		Map<String, String> changed = new HashMap<>(bundles);
		changed.values()
			.removeAll(installed.values());
		changed.keySet()
			.removeAll(toBeInstalled);

		Set<String> affected = new HashSet<>(toBeDeleted);
		affected.addAll(changed.keySet());

		LinkedHashSet<Bundle> toBeStarted = new LinkedHashSet<>();

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
			} catch (Exception e) {
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
			} catch (Exception e) {
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

			} catch (Exception e) {
				printStack(e);
				out.format("Trying to install %s: %s", location, e);
			}
		}

		for (Entry<String, String> e : changed.entrySet()) {
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

			} catch (Exception e1) {
				printStack(e1);
				out.format("Trying to update %s: %s", location, e);
			}
		}

		for (Bundle b : toBeStarted) {
			try {
				b.start(startOptions);
			} catch (Exception e1) {
				printStack(e1);
				out.format("Trying to start %s: %s", b, e1);
			}
		}

		String result = out.toString();
		out.close();
		startlevels.afterStart();
		if (result.length() == 0) {
			refresh(true);
			return null;
		}
		return result;
	}

	@Override
	public String update(long id, String sha) throws Exception {
		InputStream in = cache.getStream(sha, source);
		if (in == null)
			return null;

		StringBuilder sb = new StringBuilder();

		try {
			Bundle bundle = context.getBundle(id);
			bundle.update(in);
			refresh(true);
		} catch (Exception e) {
			sb.append(e.getMessage())
				.append("\n");
		}

		return sb.length() == 0 ? null : sb.toString();
	}

	@Override
	public String updateFromURL(long id, String url) throws Exception {
		StringBuilder sb = new StringBuilder();
		InputStream is = new URL(url).openStream();

		try {
			Bundle bundle = context.getBundle(id);
			bundle.update(is);
			refresh(true);
		} catch (Exception e) {
			sb.append(e.getMessage())
				.append("\n");
		}

		return sb.length() == 0 ? null : sb.toString();
	}

	private Bundle getBundle(String location) {
		try {
			Bundle bundle = context.getBundle(location);
			return bundle;
		} catch (Exception e) {
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
			} catch (Exception e) {
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
	public String shell(String cmd) throws Exception {
		redirect(Agent.COMMAND_SESSION);
		stdin(cmd);
		PrintStream ps = redirector.getOut();
		if (ps instanceof RedirectOutput) {
			RedirectOutput rout = (RedirectOutput) ps;
			return rout.getLastOutput();
		}

		return null;
	}

	public void setSupervisor(Supervisor remote) {
		setRemote(remote);
	}

	private List<ServiceReferenceDTO> getServiceReferences() throws Exception {
		ServiceReference<?>[] refs = context.getAllServiceReferences(null, null);
		if (refs == null)
			return Collections.emptyList();

		ArrayList<ServiceReferenceDTO> list = new ArrayList<>(refs.length);
		for (ServiceReference<?> r : refs) {
			ServiceReferenceDTO ref = new ServiceReferenceDTO();
			ref.bundle = r.getBundle()
				.getBundleId();
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

	private Map<String, Object> getProperties(ServiceReference<?> ref) {
		Map<String, Object> map = new HashMap<>();
		for (String key : ref.getPropertyKeys())
			map.put(key, ref.getProperty(key));
		return map;
	}

	@SuppressWarnings({
		"unchecked", "rawtypes"
	})
	private Map<String, Object> getProperties() {
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
		ArrayList<BundleDTO> list = new ArrayList<>(bundles.length);
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
		bd.version = b.getVersion() == null ? "0"
			: b.getVersion()
				.toString();
		return bd;
	}

	void cleanup(int event) throws Exception {
		if (quit)
			return;

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
			startlevels.close();

			scrTracker.close();
			metatypeTracker.close();
			configAdminTracker.close();
			gogoCommandsTracker.close();
			agentExtensionTracker.close();
		} catch (Exception e) {
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
		} catch (Exception e1) {
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
		} catch (Exception e1) {
			printStack(e1);
		}
	}

	private void printStack(Exception e1) {
		try {
			e1.printStackTrace(redirector.getOut());
		} catch (Exception e) {
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
	public Map<String, String> getSystemProperties() throws Exception {
		return Converter.cnv(MAP_STRING_STRING_T, System.getProperties());
	}

	@Override
	public boolean createFramework(String name, Collection<String> runpath, Map<String, Object> properties)
		throws Exception {
		throw new UnsupportedOperationException("This is an agent, we can't create new frameworks (for now)");
	}

	public Supervisor getSupervisor() {
		return remote;
	}

	public void setLink(Link<Agent, Supervisor> link) {
		setRemote(link.getRemote());
		this.link = link;
	}

	@Override
	public boolean ping() {
		return true;
	}

	public BundleContext getContext() {
		return context;
	}

	@SuppressWarnings("deprecation")
	public void refresh(boolean async) throws InterruptedException {
		try {
			FrameworkWiring f = context.getBundle(0)
				.adapt(FrameworkWiring.class);
			if (f != null) {
				refresh = new CountDownLatch(1);
				f.refreshBundles(null, event -> refresh.countDown());

				if (async)
					return;

				refresh.await();
				return;
			}
		} catch (Exception | NoSuchMethodError e) {
			@SuppressWarnings("unchecked")
			ServiceReference<org.osgi.service.packageadmin.PackageAdmin> ref = (ServiceReference<org.osgi.service.packageadmin.PackageAdmin>) context
				.getServiceReference(org.osgi.service.packageadmin.PackageAdmin.class.getName());
			if (ref != null) {
				org.osgi.service.packageadmin.PackageAdmin padmin = context.getService(ref);
				padmin.refreshPackages(null);
				return;
			}
		}
		throw new IllegalStateException("Cannot refresh");
	}

	@Override
	public List<BundleDTO> getBundles(long... bundleId) throws Exception {

		Bundle[] bundles;
		if (bundleId.length == 0) {
			bundles = context.getBundles();
		} else {
			bundles = new Bundle[bundleId.length];
			for (int i = 0; i < bundleId.length; i++) {
				bundles[i] = context.getBundle(bundleId[i]);
				if (bundles[i] == null)
					throw new IllegalArgumentException("Bundle " + bundleId[i] + " not installed");
			}
		}

		List<BundleDTO> bundleDTOs = new ArrayList<>(bundles.length);

		for (Bundle b : bundles) {
			BundleDTO dto = toDTO(b);
			bundleDTOs.add(dto);
		}

		return bundleDTOs;
	}

	/**
	 * Return the bundle revisions
	 */
	@Override
	public List<BundleRevisionDTO> getBundleRevisons(long... bundleId) throws Exception {

		Bundle[] bundles;
		if (bundleId.length == 0) {
			bundles = context.getBundles();
		} else {
			bundles = new Bundle[bundleId.length];
			for (int i = 0; i < bundleId.length; i++) {
				bundles[i] = context.getBundle(bundleId[i]);
				if (bundles[i] == null) {
					throw new IllegalArgumentException("Bundle " + bundleId[i] + " does not exist");
				}
			}
		}

		List<BundleRevisionDTO> revisions = new ArrayList<>(bundles.length);

		for (Bundle b : bundles) {
			BundleRevision resource = b.adapt(BundleRevision.class);
			BundleRevisionDTO bwd = toDTO(resource);
			revisions.add(bwd);
		}

		return revisions;
	}

	/*
	 * Turn a bundle in a Bundle Revision dto. On a r6 framework we could do
	 * this with adapt but on earlier frameworks we're on our own
	 */

	private BundleRevisionDTO toDTO(BundleRevision resource) {
		BundleRevisionDTO brd = new BundleRevisionDTO();
		brd.bundle = resource.getBundle()
			.getBundleId();
		brd.id = sequence.getAndIncrement();
		brd.symbolicName = resource.getSymbolicName();
		brd.type = resource.getTypes();
		brd.version = resource.getVersion()
			.toString();

		brd.requirements = new ArrayList<>();

		for (Requirement r : resource.getRequirements(null)) {
			brd.requirements.add(toDTO(brd.id, r));
		}

		brd.capabilities = new ArrayList<>();
		for (Capability c : resource.getCapabilities(null)) {
			brd.capabilities.add(toDTO(brd.id, c));
		}

		return brd;
	}

	private RequirementDTO toDTO(int resource, Requirement r) {
		RequirementDTO rd = new RequirementDTO();
		rd.id = sequence.getAndIncrement();
		rd.resource = resource;
		rd.namespace = r.getNamespace();
		rd.directives = r.getDirectives();
		rd.attributes = r.getAttributes();
		return rd;
	}

	private CapabilityDTO toDTO(int resource, Capability r) {
		CapabilityDTO rd = new CapabilityDTO();
		rd.id = sequence.getAndIncrement();
		rd.resource = resource;
		rd.namespace = r.getNamespace();
		rd.directives = r.getDirectives();
		rd.attributes = r.getAttributes();
		return rd;
	}

	private Entry<String, Version> getIdentity(byte[] data) throws IOException {
		try (JarInputStream jin = new JarInputStream(new ByteArrayInputStream(data))) {
			Manifest manifest = jin.getManifest();
			if (manifest == null) {
				throw new IllegalArgumentException("No manifest in bundle");
			}
			Attributes mainAttributes = manifest.getMainAttributes();
			String value = mainAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
			Matcher matcher = BSN_P.matcher(value);

			if (!matcher.matches()) {
				throw new IllegalArgumentException("No proper Bundle-SymbolicName in bundle: " + value);
			}

			String bsn = matcher.group(1);

			String versionString = mainAttributes.getValue(Constants.BUNDLE_VERSION);
			if (versionString == null)
				versionString = "0";

			Version version = Version.parseVersion(versionString);

			return new AbstractMap.SimpleEntry<>(bsn, version);
		}
	}

	private Set<Bundle> findBundles(String bsn, Version version) {

		return Stream.of(context.getBundles())
			.filter(b -> bsn.equals(b.getSymbolicName()))
			.filter(b -> version == null || version.equals(b.getVersion()))
			.collect(Collectors.toSet());
	}

	private String getLocation(byte[] data) throws IOException {
		Map.Entry<String, Version> entry = getIdentity(data);
		Set<Bundle> bundles = findBundles(entry.getKey(), null);
		switch (bundles.size()) {
			case 0 :
				return "manual:" + entry.getKey();

			case 1 :
				return bundles.iterator()
					.next()
					.getLocation();

			default :
				throw new IllegalArgumentException(
					"No location specified but there are multiple bundles with the same bsn " + entry.getKey() + ": "
						+ bundles);
		}
	}

	@Override
	public List<XBundleDTO> getAllBundles() {
		return XBundleAdmin.get(context);
	}

	@Override
	public List<XComponentDTO> getAllComponents() {
		boolean isScrAvailable = PackageWirings.isScrWired(context);
		if (isScrAvailable) {
			XComponentAdmin scrAdmin = new XComponentAdmin(scrTracker.getService());
			return scrAdmin.getComponents();
		}
		return Collections.emptyList();
	}

	@Override
	public List<XConfigurationDTO> getAllConfigurations() {
		boolean isConfigAdminAvailable = PackageWirings.isConfigAdminWired(context);
		boolean isMetatypeAvailable = PackageWirings.isMetatypeWired(context);

		List<XConfigurationDTO> configs = new ArrayList<>();
		if (isConfigAdminAvailable) {
			XConfigurationAdmin configAdmin = new XConfigurationAdmin(context, configAdminTracker.getService(),
				metatypeTracker.getService());
			configs.addAll(configAdmin.getConfigurations());
		}
		if (isMetatypeAvailable) {
			XMetaTypeAdmin metatypeAdmin = new XMetaTypeAdmin(context, configAdminTracker.getService(),
				metatypeTracker.getService());
			configs.addAll(metatypeAdmin.getConfigurations());
		}
		return configs;
	}

	@Override
	public List<XPropertyDTO> getAllProperties() {
		return XPropertyAdmin.get(context);
	}

	@Override
	public List<XServiceDTO> getAllServices() {
		return XServiceAdmin.get(context);
	}

	@Override
	public List<XThreadDTO> getAllThreads() {
		return XThreadAdmin.get();
	}

	@Override
	public XResultDTO enableComponent(long id) {
		boolean isScrAvailable = PackageWirings.isScrWired(context);
		if (isScrAvailable) {
			XComponentAdmin scrAdmin = new XComponentAdmin(scrTracker.getService());
			return scrAdmin.enableComponent(id);
		}
		return createResult(SKIPPED, "SCR bundle is not installed to process this request");
	}

	@Override
	public XResultDTO enableComponent(String name) {
		requireNonNull(name, "Component name cannot be null");

		boolean isScrAvailable = PackageWirings.isScrWired(context);
		if (isScrAvailable) {
			XComponentAdmin scrAdmin = new XComponentAdmin(scrTracker.getService());
			return scrAdmin.enableComponent(name);
		}
		return createResult(SKIPPED, "SCR bundle is not installed to process this request");
	}

	@Override
	public XResultDTO disableComponent(long id) {
		boolean isScrAvailable = PackageWirings.isScrWired(getContext());
		if (isScrAvailable) {
			XComponentAdmin scrAdmin = new XComponentAdmin(scrTracker.getService());
			return scrAdmin.disableComponent(id);
		}
		return createResult(SKIPPED, "SCR bundle is not installed to process this request");
	}

	@Override
	public XResultDTO disableComponent(String name) {
		requireNonNull(name, "Component name cannot be null");

		boolean isScrAvailable = PackageWirings.isScrWired(getContext());
		if (isScrAvailable) {
			XComponentAdmin scrAdmin = new XComponentAdmin(scrTracker.getService());
			return scrAdmin.disableComponent(name);
		}
		return createResult(SKIPPED, "SCR bundle is not installed to process this request");
	}

	@Override
	public XResultDTO createOrUpdateConfiguration(String pid, Map<String, Object> newProperties) {
		requireNonNull(pid, "Configuration PID cannot be null");
		requireNonNull(newProperties, "Configuration properties cannot be null");

		boolean isConfigAdminAvailable = PackageWirings.isConfigAdminWired(context);
		if (isConfigAdminAvailable) {
			XConfigurationAdmin configAdmin = new XConfigurationAdmin(context, configAdminTracker.getService(),
				metatypeTracker.getService());
			return configAdmin.createOrUpdateConfiguration(pid, newProperties);
		}
		return createResult(SKIPPED, "ConfigAdmin bundle is not installed to process this request");
	}

	@Override
	public XResultDTO deleteConfiguration(String pid) {
		requireNonNull(pid, "Configuration PID cannot be null");

		boolean isConfigAdminAvailable = PackageWirings.isConfigAdminWired(getContext());
		if (isConfigAdminAvailable) {
			XConfigurationAdmin configAdmin = new XConfigurationAdmin(context, configAdminTracker.getService(),
				metatypeTracker.getService());
			return configAdmin.deleteConfiguration(pid);
		}
		return createResult(SKIPPED, "ConfigAdmin bundle is not installed to process this request");
	}

	@Override
	public XResultDTO createFactoryConfiguration(String factoryPid, Map<String, Object> newProperties) {
		requireNonNull(factoryPid, "Configuration factory PID cannot be null");
		requireNonNull(newProperties, "Configuration properties cannot be null");

		boolean isConfigAdminAvailable = PackageWirings.isConfigAdminWired(context);

		if (isConfigAdminAvailable) {
			XConfigurationAdmin configAdmin = new XConfigurationAdmin(context, configAdminTracker.getService(),
				metatypeTracker.getService());
			return configAdmin.createFactoryConfiguration(factoryPid, newProperties);
		}
		return createResult(SKIPPED, "ConfigAdmin bundle is not installed to process this request");
	}

	@Override
	public Map<String, String> getRuntimeInfo() {
		final Map<String, String> runtime = new HashMap<>();
		final Bundle systemBundle = getContext().getBundle(0);

		runtime.put("Framework", systemBundle.getSymbolicName());
		runtime.put("Framework Version", systemBundle.getVersion()
			.toString());
		runtime.put("Memory Total", String.valueOf(Runtime.getRuntime()
			.totalMemory()));
		runtime.put("Memory Free", String.valueOf(Runtime.getRuntime()
			.freeMemory()));
		runtime.put("OS Name", System.getProperty("os.name"));
		runtime.put("OS Version", System.getProperty("os.version"));
		runtime.put("OS Architecture", System.getProperty("os.arch"));
		runtime.put("Uptime", String.valueOf(getSystemUptime()));

		return runtime;
	}

	@Override
	public Set<String> getGogoCommands() {
		return new HashSet<>(gogoCommands);
	}

	@Override
	public Object executeExtension(String name, Map<String, Object> context) {
		if (!agentExtensions.containsKey(name)) {
			return "Agent extension with name '" + name + "' doesn't exist";
		}
		return agentExtensions.get(name)
			.execute(context);
	}

	private static long getSystemUptime() {
		final RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
		return rb.getUptime();
	}

	public static <K, V> Map<K, V> valueOf(final Dictionary<K, V> dictionary) {
		if (dictionary == null) {
			return null;
		}
		final Map<K, V> map = new HashMap<>(dictionary.size());
		final Enumeration<K> keys = dictionary.keys();
		while (keys.hasMoreElements()) {
			final K key = keys.nextElement();
			map.put(key, dictionary.get(key));
		}
		return map;
	}

	public static XResultDTO createResult(int result, String response) {
		XResultDTO dto = new XResultDTO();

		dto.result = result;
		dto.response = response;

		return dto;
	}

}

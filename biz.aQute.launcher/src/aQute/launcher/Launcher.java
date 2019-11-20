package aQute.launcher;

import static aQute.launcher.constants.LauncherConstants.CANCELED;
import static aQute.launcher.constants.LauncherConstants.DEFAULT_LAUNCHER_PROPERTIES;
import static aQute.launcher.constants.LauncherConstants.ERROR;
import static aQute.launcher.constants.LauncherConstants.UPDATE_NEEDED;
import static aQute.launcher.constants.LauncherConstants.WARNING;
import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.invoke.MethodType.methodType;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IllegalFormatException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.permissionadmin.PermissionInfo;

import aQute.launcher.agent.LauncherAgent;
import aQute.launcher.constants.LauncherConstants;
import aQute.launcher.minifw.MiniFramework;
import aQute.launcher.pre.EmbeddedLauncher;
import aQute.lib.io.ByteBufferDataOutput;
import aQute.lib.io.IO;
import aQute.lib.startlevel.StartLevelRuntimeHandler;
import aQute.lib.strings.Strings;
import aQute.libg.uri.URIUtil;

/**
 * This is the primary bnd launcher. It implements a launcher that runs on Java
 * 1.8.
 */
public class Launcher implements ServiceListener {

	private static final String				BND_LAUNCHER		= ".bnd.launcher";

	private PrintStream						out;
	private LauncherConstants				parms;
	private Framework						systemBundle;
	private FrameworkWiring					frameworkWiring;
	private final Properties				properties;
	private final Properties				initialSystemProperties	= System.getProperties();
	private boolean							security;
	private SimplePermissionPolicy			policy;
	private Callable<Integer>				mainThread;
	private final Map<Bundle, Throwable>	errors				= new HashMap<>();
	private final Map<File, Bundle>			installedBundles	= new LinkedHashMap<>();
	private File							home				= new File(System.getProperty("user.home"));
	private File							bnd					= new File(home, "bnd");
	private List<Bundle>					wantsToBeStarted	= new ArrayList<>();
	private AtomicBoolean					active				= new AtomicBoolean();
	private AtomicReference<DatagramSocket>	commsSocket			= new AtomicReference<>();
	private StartLevelRuntimeHandler		startLevelhandler;

	enum EmbeddedActivatorPhase {

		AFTER_FRAMEWORK_INIT,
		BEFORE_BUNDLES_START,
		AFTER_BUNDLES_START,
		UNKNOWN;

		static EmbeddedActivatorPhase valueOf( Object o) {
			if ( AFTER_FRAMEWORK_INIT.name().equals(o))
				return AFTER_FRAMEWORK_INIT;

			if (Boolean.TRUE.equals(o) || BEFORE_BUNDLES_START.name()
				.equals(o))
				return BEFORE_BUNDLES_START;

			if (Boolean.FALSE.equals(o) || AFTER_BUNDLES_START.name()
				.equals(o))
				return AFTER_BUNDLES_START;

			return UNKNOWN;
		}
	}
	/**
	 * Called from the commandline (and via EmbeddedLauncher in the embedded
	 * mode.)
	 */
	public static void main(String[] args) {
		try {
			int exitcode = 0;
			try {

				exitcode = Launcher.run(args);
			} catch (Throwable t) {
				exitcode = 127;
				// Last resort ... errors should be handled lower
				t.printStackTrace(System.err);
			}

			// We exit, even if there are non-daemon threads active
			// though we've reported those
			if (exitcode != LauncherConstants.RETURN_INSTEAD_OF_EXIT)
				System.exit(exitcode);
		} finally {
			System.out.println("gone");
		}
	}

	/*
	 * Without exit and properties are read. Called reflectively from embedded
	 */
	public static int run(String args[]) throws Throwable {
		Launcher target = new Launcher();
		target.loadPropertiesFromFileOrEmbedded();
		return target.launch(args);
	}

	/**
	 * Called from Eclipse launcher? passes the properties
	 */
	public static int main(String[] args, Properties p) throws Throwable {
		Launcher target = new Launcher(p);
		return target.launch(args);
	}

	private static String getVersion() {
		try {
			Enumeration<URL> manifests = Launcher.class.getClassLoader()
				.getResources("META-INF/MANIFEST.MF");
			StringBuilder sb = new StringBuilder();
			String del = "";
			for (Enumeration<URL> u = manifests; u.hasMoreElements();) {
				URL url = u.nextElement();
				try (InputStream in = url.openStream()) {
					Manifest m = new Manifest(in);
					String bsn = m.getMainAttributes()
						.getValue(Constants.BUNDLE_SYMBOLICNAME);
					String version = m.getMainAttributes()
						.getValue(Constants.BUNDLE_VERSION);
					if (bsn != null && version != null) {
						sb.append(del)
							.append(bsn)
							.append(";version=")
							.append(version);
						del = ", ";
					}
				}
			}
			return sb.toString();
		} catch (Exception e) {
			return "Cannot read manifest: " + e;
		}
	}

	private static void printUsage() {
		System.out.println("Usage: java -Dlauncher.properties=<launcher.properties> -jar <launcher.jar>");
	}

	private static void errorAndExit(String message, Object... args) {
		System.err.println(Strings.format(message, args));
		System.exit(ERROR);
	}

	public Launcher() throws Exception {
		this(new Properties());
	}

	public Launcher(Properties p) {
		this.properties = p;
		out = System.err;
	}

	private void loadPropertiesFromFileOrEmbedded() throws Exception {
		final InputStream in;
		String path = getPropertiesPath();
		if (path != null) {
			File propertiesFile = new File(path).getAbsoluteFile();
			if (!propertiesFile.isFile())
				errorAndExit("Specified launch file `%s' was not found - absolutePath='%s'", path,
					propertiesFile.getAbsolutePath());
			in = IO.stream(propertiesFile);
		} else {
			in = Launcher.class.getClassLoader()
				.getResourceAsStream(DEFAULT_LAUNCHER_PROPERTIES);
			if (in == null) {
				printUsage();
				errorAndExit("Launch file not specified, and no embedded properties found.");
				return;
			}
		}
		loadProperties(in);
	}

	public String getPropertiesPath() {
		String path = System.getProperty(LauncherConstants.LAUNCHER_PROPERTIES);
		if (path != null) {
			int l = path.length() - 1;
			if (l > 1) {
				char q = path.charAt(0);
				if (((q == '\'') || (q == '"')) && (q == path.charAt(l))) {
					path = path.substring(1, l);
				}
			}
		}
		return path;
	}

	private void loadProperties(final InputStream in) throws IOException {
		this.properties.clear();

		try (Reader ir = IO.reader(in, UTF_8)) {
			this.properties.load(ir);
		}

		for (String key : LauncherConstants.LAUNCHER_PROPERTY_KEYS) {
			String value = initialSystemProperties.getProperty(key);
			if (value == null)
				continue;

			properties.put(key, value);
		}

		for (Object key : properties.keySet()) {
			String s = (String) key;
			String v = initialSystemProperties.getProperty(s);
			if (v != null)
				properties.put(key, v);
		}
	}

	private void setSystemProperties() {
		Properties sysProps = new LauncherSystemProperties(initialSystemProperties, properties);
		System.setProperties(sysProps);
	}

	/*
	 * If we have a properties file specified watch it and update
	 */
	private void watch() {
		String path = getPropertiesPath();
		if (path == null) {
			return;
		}
		File propertiesFile = new File(path).getAbsoluteFile();
		if (propertiesFile.isFile() && parms.embedded == false) {
			ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
			AtomicLong lastModified = new AtomicLong(propertiesFile.lastModified());
			scheduledExecutor.scheduleAtFixedRate(() -> {
				long now = propertiesFile.lastModified();
				if (lastModified.get() < now) {
					try {
						loadProperties(IO.stream(propertiesFile));
						setSystemProperties();
						parms = new LauncherConstants(properties);
						List<Bundle> tobestarted = update(now);
						startBundles(tobestarted);
					} catch (Exception e) {
						error("Error in updating the framework from the properties: %s", e);
					}
					lastModified.set(now);
				}
			}, 5000, 1000, TimeUnit.MILLISECONDS);
		}
	}

	private void setupComms() {
		DatagramSocket oldSocket;
		if (parms.notificationPort == -1) {
			oldSocket = commsSocket.getAndSet(null);
		} else {
			oldSocket = commsSocket.get();
			if (oldSocket != null && oldSocket.getPort() == parms.notificationPort) {
				oldSocket = null;
			} else {
				DatagramSocket newSocket;
				try {
					newSocket = new DatagramSocket(new InetSocketAddress(InetAddress.getByName(null), 0));
					newSocket.connect(new InetSocketAddress(InetAddress.getByName(null), parms.notificationPort));

				} catch (IOException ioe) {
					// TODO what now?
					newSocket = null;
				}
				commsSocket.compareAndSet(oldSocket, newSocket);
			}
		}

		if (oldSocket != null) {
			oldSocket.close();
		}
	}

	private int launch(String args[]) throws Throwable {
		try {
			setSystemProperties();
			this.parms = new LauncherConstants(properties);
			setupComms();

			this.startLevelhandler = StartLevelRuntimeHandler.create(this::trace, properties);

			watch();

			trace("inited runbundles=%s activators=%s timeout=%s", parms.runbundles, parms.activators, parms.timeout);
			trace("version %s", getVersion());

			int status = activate();
			if (status != 0) {
				report(out);
				System.exit(status);
			}


			trace("framework=%s", systemBundle);

			// Register the command line with ourselves as the
			// service.
			if (parms.services) { // Does not work for our dummy framework

				try {
					if (LauncherAgent.instrumentation != null) {
						Hashtable<String, Object> argprops = new Hashtable<>();
						if (LauncherAgent.agentArgs != null)
							argprops.put("agent.arguments", LauncherAgent.agentArgs);
						systemBundle.getBundleContext()
							.registerService(Instrumentation.class.getName(), LauncherAgent.instrumentation, argprops);
					}
				} catch (NoClassDefFoundError e) {
					// Must be running on a profile which does not support
					// java.lang.instrument
				}

				Hashtable<String, Object> argprops = new Hashtable<>();
				argprops.put(LauncherConstants.LAUNCHER_ARGUMENTS, args);
				argprops.put(LauncherConstants.LAUNCHER_READY, "true");
				argprops.put(Constants.SERVICE_RANKING, -1000);
				systemBundle.getBundleContext()
					.registerService(new String[] {
						Object.class.getName(), Launcher.class.getName()
					}, this, argprops);
				trace("registered launcher with arguments for syncing");
			} else {
				trace("requested to register no services");
			}


			// Wait until a Runnable is registered with main.thread=true.
			// not that this will never happen when we're running on the mini fw
			// but the test case normally exits.
			synchronized (this) {
				while (mainThread == null) {
					trace("will wait for a registered Runnable");
					wait();
				}
			}
			trace("will call main");
			// report(System.err);
			Integer exitCode = mainThread.call();
			trace("main return, code %s", exitCode);
			return exitCode == null ? 0 : exitCode;
		} catch (Throwable e) {
			error("Unexpected error in the run body: %s", e);
			throw e;
		} finally {
			deactivate();
			trace("stopped system bundle due to leaving run body");
			System.setProperties(initialSystemProperties);
			// TODO should we wait here?
		}
	}

	private List<String> split(String value, String separator) {
		List<String> list = new ArrayList<>();
		if (value == null)
			return list;

		StringTokenizer st = new StringTokenizer(value, separator);
		while (st.hasMoreTokens())
			list.add(st.nextToken());

		return list;
	}

	private int activate() throws Exception {
		Policy.setPolicy(new AllPolicy());

		systemBundle = createFramework();
		if (systemBundle == null)
			return LauncherConstants.ERROR;

		startLevelhandler.beforeStart(systemBundle);

		frameworkWiring = systemBundle.adapt(FrameworkWiring.class);

		active.set(true);

		doTimeoutHandler();

		doSecurity();


		int result = LauncherConstants.OK;

		BundleContext systemContext = systemBundle.getBundleContext();

		systemContext.addServiceListener(this, "(&(|(objectclass=" + Runnable.class.getName() + ")(objectclass="
			+ Callable.class.getName() + "))(main.thread=true))");

		List<BundleActivator> startBeforeBundleStart = new LinkedList<>();
		List<BundleActivator> startAfterBundleStart = new LinkedList<>();

		// Start embedded activators
		trace("start embedded activators");
		if (parms.activators != null) {
			ClassLoader loader = getClass().getClassLoader();
			for (Object token : parms.activators) {
				try {
					Class<?> clazz = loader.loadClass((String) token);
					BundleActivator activator = (BundleActivator) newInstance(clazz);
					Object immediateFieldValue = getImmediateFieldValue(activator);
					EmbeddedActivatorPhase phase = EmbeddedActivatorPhase.valueOf(immediateFieldValue);
					switch (phase) {
						case AFTER_FRAMEWORK_INIT :
							trace("AFTER_FRAMEWORK_INIT %s", activator);
							result = start(systemContext, result, activator);
							break;

						case BEFORE_BUNDLES_START :
							trace("BEFORE_BUNDLES_START %s", activator);
							startBeforeBundleStart.add(activator);
							break;

						case UNKNOWN :
							trace(
								"!the value of IMMEDIATE is not recognized as one of %s, it is %s",
								EmbeddedActivatorPhase.values(),
								immediateFieldValue);

							// FALL THROUGH

						default :
						case AFTER_BUNDLES_START :
							trace("AFTER_BUNDLES_START %s", activator);
							startAfterBundleStart.add(activator);
							break;
					}
				} catch (Exception e) {
					throw new IllegalArgumentException("Embedded Bundle Activator incorrect: " + token, e);
				}
			}
		}
		List<Bundle> tobestarted = update(System.currentTimeMillis() + 100);

		systemBundle.start();

		trace("system bundle started ok");

		for (BundleActivator activator : startBeforeBundleStart) {
			result = start(systemContext, result, activator);
		}

		startBundles(tobestarted);

		for (BundleActivator activator : startAfterBundleStart) {
			result = start(systemContext, result, activator);
		}

		startLevelhandler.afterStart();

		if (parms.trace) {
			report(out);
		}

		return result;
	}

	private Object getImmediateFieldValue(BundleActivator activator) {
		try {
			Field f = activator.getClass()
				.getField("IMMEDIATE");
			return f.get(activator);
		} catch (Exception e) {}
		return Boolean.FALSE;
	}

	private int start(BundleContext systemContext, int result, BundleActivator activator) {
		try {
			trace("starting activator %s", activator);
			activator.start(systemContext);
		} catch (Exception e) {
			error("Starting activator %s : %s", activator, e);
			result = LauncherConstants.ERROR;
		}
		return result;
	}

	/**
	 * Ensure that all the bundles in the parameters are actually started. We
	 * can start in embedded mode (bundles are inside our main jar) or in file
	 * system mode.
	 *
	 * @param begin
	 */
	private List<Bundle> update(long before) throws Exception {

		trace("Updating framework with %s", parms.runbundles);
		List<Bundle> tobestarted = new ArrayList<>();
		if (parms.embedded)
			installEmbedded(tobestarted);
		else
			synchronizeFiles(tobestarted, before);

		return tobestarted;
	}

	private void startBundles(List<Bundle> tobestarted) throws Exception {
		refresh();

		trace("bundles administered %s", installedBundles.keySet());

		// From now on, the bundles are on their own. They have
		// by default AllPermission, but if they install bundles
		// they will not automatically get AllPermission anymore
		if (security)
			policy.setDefaultPermissions(null);

		// Get the resolved status
		if (frameworkWiring.resolveBundles(null) == false) {
			List<String> failed = new ArrayList<>();

			for (Bundle b : installedBundles.values()) {
				try {
					if ((b.getState() & Bundle.INSTALLED) != 0) {
						start(b);
					}
				} catch (Exception e) {
					failed.add(b.getSymbolicName() + "-" + b.getVersion() + " " + e + "\n");
				}
			}
			if (!failed.isEmpty()) {
				error("could not resolve the bundles: %s", failed);
			}
			// return LauncherConstants.RESOLVE_ERROR;
		}

		// Now start all the installed bundles in the same order
		// (unless they're a fragment)

		trace("Will start bundles: %s", tobestarted);
		List<Bundle> all = new ArrayList<>(tobestarted);
		// Add all bundles that we've tried to start but failed
		all.addAll(wantsToBeStarted);


		for (Bundle b : tobestarted) {
			try {
				trace("starting %s", b.getSymbolicName());
				start(b);
				trace("started  %s", b.getSymbolicName());
			} catch (BundleException e) {
				wantsToBeStarted.add(b);
				error("Failed to start bundle %s-%s, exception %s", b.getSymbolicName(), b.getVersion(), e);
			}
		}

	}

	void start(Bundle b) throws BundleException {
		if (isFragment(b))
			return;

		int startOptions = Bundle.START_ACTIVATION_POLICY;

		// when activationEager remove start activation policy
		if (parms.activationEager) {
			startOptions &= ~Bundle.START_ACTIVATION_POLICY;
		}
		b.start(startOptions);
	}

	void stop(Bundle b) throws BundleException {
		if (isFragment(b))
			return;
		b.stop();
	}

	private void refresh() throws InterruptedException {
		Semaphore semaphore = new Semaphore(0);

		frameworkWiring.refreshBundles(null, (e) -> {
			if (e.getType() == FrameworkEvent.PACKAGES_REFRESHED)
				semaphore.release();
		});

		trace("Waiting for refresh to finish");

		if (semaphore.tryAcquire(10, TimeUnit.MINUTES)) {
			trace("Refresh is finished");
			return;
		}

		trace("Could not refresh the framework in 5 minutes");
	}

	/**
	 * @param tobestarted
	 */
	private void synchronizeFiles(List<Bundle> tobestarted, long before) {
		// Turn the bundle location paths into files
		Map<File, Integer> desired = new LinkedHashMap<>();

		int n = 0;
		for (Object o : parms.runbundles) {
			String s = (String) o;
			s = toNativePath(s);
			File file = new File(s).getAbsoluteFile();
			desired.put(file, n);
			n++;
		}

		// deleted = old - new
		List<File> tobedeleted = new ArrayList<>(installedBundles.keySet());

		tobedeleted.removeAll(desired.keySet());

		// updated = old /\ new
		List<File> tobeupdated = new ArrayList<>(installedBundles.keySet());
		tobeupdated.retainAll(desired.keySet());

		// install = new - old
		List<File> tobeinstalled = new ArrayList<>(desired.keySet());
		tobeinstalled.removeAll(installedBundles.keySet());

		for (File f : tobedeleted)
			try {
				trace("uninstalling %s", f);
				Bundle bundle = installedBundles.get(f);
				bundle.uninstall();
				installedBundles.remove(f);
			} catch (Exception e) {
				error("Failed to uninstall bundle %s, exception %s", f, e);
			}

		for (File f : tobeinstalled)
			try {
				int index = desired.get(f);
				trace("installing %s", f);
				if (f.exists()) {
					Bundle b = install(f);
					installedBundles.put(f, b);
					tobestarted.add(b);
				} else
					error("should installing %s but file does not exist", f);
			} catch (Exception e) {
				error("Failed to uninstall bundle %s, exception %s", f, e);
			}

		for (File f : tobeupdated)
			try {
				Integer index = desired.get(f);
				if (f.exists()) {
					Bundle b = installedBundles.get(f);

					//
					// Ensure we only update bundles that
					// we're modified before the properties file was modified.
					// Otherwise we might update bundles that are still being
					// written by bnd
					//
					if (f.lastModified() <= before) {
						if (b.getLastModified() < f.lastModified()) {
							trace("updating %s", f);
							if ((b.getState() & (Bundle.ACTIVE | Bundle.STARTING)) != 0) {
								tobestarted.add(b);
								stop(b);
							}
							b.update();
						} else
							trace("bundle is still current according to timestamp %s", f);
					}
				} else
					error("should update %s but file does not exist", f);
			} catch (Exception e) {
				error("Failed to update bundle %s, exception %s", f, e);
			}
	}


	/**
	 * Convert a path to native when it contains a macro. This is needed for the
	 * jpm option since it stores the paths with a macro in the JAR through the
	 * packager. This path is platform independent and must therefore be
	 * translated to the executing platform. if no macro is present, we assume
	 * the path is already native.
	 *
	 * @param s
	 */
	private String toNativePath(String s) {
		if (!s.contains("${"))
			return s;

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '/' :
					sb.append(File.separatorChar);
					break;

				case '$' :
					if (s.length() - 3 > i) {
						char rover = s.charAt(++i);
						if (rover == '{') {
							rover = s.charAt(++i);
							StringBuilder var = new StringBuilder();
							while (i < s.length() - 1 && rover != '}') {
								var.append(rover);
								rover = s.charAt(++i);
							}
							String key = var.toString();
							String value = System.getProperty(key);
							if (value == null)
								value = System.getenv(key);
							if (value != null)
								sb.append(value);
							else
								sb.append("${")
									.append(key)
									.append("}");
						} else
							sb.append('$')
								.append(rover);
					} else
						sb.append('$');
					break;

				case '\\' :
					if (s.length() - 1 > i)
						sb.append(s.charAt(++i));
					break;

				default :
					sb.append(c);
			}
		}
		return sb.toString();
	}

	/*
	 * Get the digest for a given path
	 */
	static String[] DIGESTS = {
		"SHA-Digest", "SHA1-Digest", "SHA-1-Digest", "SHA-256-Digest", "SHA-224-Digest", "SHA-384-Digest",
		"SHA-512-Digest", "MD5-Digest"
	};

	private String getDigest(String path) {
		Manifest m = EmbeddedLauncher.MANIFEST;
		if (m != null) {
			for (String name : DIGESTS) {
				Attributes attributes = m.getAttributes(path);
				if (attributes != null) {
					String digest = attributes.getValue(name);
					if (digest != null) {
						return digest;
					}
				}
			}
		}
		return null;
	}

	/*
	 * Install/Update the bundles from the current jar.
	 */
	private void installEmbedded(List<Bundle> tobestarted) throws Exception {
		trace("starting in embedded mode");
		BundleContext context = systemBundle.getBundleContext();
		int n = 0;
		for (Object o : parms.runbundles) {

			String path = (String) o;
			String digest = getDigest(path);

			URL resource = getClass().getClassLoader()
				.getResource(path);
			Bundle bundle;
			if (useReferences() && resource.getProtocol()
				.equalsIgnoreCase("file")) {
				trace("installing %s by reference", path);

				//
				// Install by reference
				//

				File file = new File(resource.toURI());
				bundle = context.installBundle(getReferenceUrl(file));
				updateDigest(digest, bundle);
				tobestarted.add(bundle);

			} else {

				//
				// Install by copying since the URL we got
				// is not a file url.
				//

				try (InputStream in = resource.openStream()) {
					bundle = getBundleByLocation(path);
					if (bundle == null) {
						trace("installing %s", path);
						bundle = context.installBundle(path, in);
						updateDigest(digest, bundle);
					} else {
						if (mustUpdate(digest, bundle)) {
							trace("updating %s, digest=%s", path, digest);
							stop(bundle);
							bundle.update(in);
							updateDigest(digest, bundle);
						} else {
							trace("not updating %s because identical digest=%s", path, digest);
						}
					}
					tobestarted.add(bundle);
				}
			}
			n++;
		}
	}

	/*
	 * Check if we have a digest from the manifest and it it was for this the
	 * bundle.
	 */
	private boolean mustUpdate(String digest, Bundle bundle) {
		if (digest == null)
			return true;

		File digestFile = digestFile(bundle);

		if (digestFile == null || !digestFile.isFile() || !digestFile.canRead())
			return true;

		try {
			String storedDigest = IO.collect(digestFile);
			if (storedDigest == null || !storedDigest.equals(digest))
				return true;
		} catch (IOException e) {
			return true;
		}

		return false;
	}

	private File digestFile(Bundle bundle) {
		BundleContext context = systemBundle.getBundleContext();
		File bndlauncher = context.getDataFile(BND_LAUNCHER);
		bndlauncher.mkdirs();
		File digestFile = new File(bndlauncher, bundle.getBundleId() + "");
		return digestFile;
	}

	private void updateDigest(String digest, Bundle bundle) {
		if (digest == null)
			return;

		File digestFile = digestFile(bundle);
		if (digestFile == null || !digestFile.getParentFile()
			.isDirectory() && digestFile.getParentFile()
				.canWrite())
			return;

		try {
			IO.store(digest, digestFile);
		} catch (Exception e) {
			error("Could not (over) write digest %s", e);
		}
	}

	private Bundle install(File f) throws Exception {
		BundleContext context = systemBundle.getBundleContext();
		try {
			String location;
			if (!useReferences()) {
				trace("no reference: url %s", parms.noreferences);
				location = f.toURI()
					.toURL()
					.toExternalForm();
			} else
				location = getReferenceUrl(f);

			Bundle b = context.installBundle(location);
			if (b.getLastModified() < f.lastModified()) {
				b.update();
			}

			return b;
		} catch (BundleException e) {
			trace("failed reference, will try to install %s with input stream", f.getAbsolutePath());
			String reference = f.toURI()
				.toURL()
				.toExternalForm();
			try (InputStream in = IO.stream(f)) {
				return context.installBundle(reference, in);
			}
		}
	}

	private boolean useReferences() {
		return !IO.isWindows() && !parms.noreferences;
	}

	private String getReferenceUrl(File f) throws MalformedURLException {
		return "reference:" + f.toURI()
			.toURL()
			.toExternalForm();
	}

	private void doTimeoutHandler() {
		// Ensure we properly close in a separate thread so that
		// we can leverage the main thread, which is required for macs
		Thread wait = new Thread("FrameworkWaiter") {
			@Override
			public void run() {
				try {
					FrameworkEvent result = systemBundle.waitForStop(parms.timeout);
					if (!active.get()) {
						trace(
							"ignoring timeout handler because framework is already no longer active, shutdown is orderly handled");
						return;
					}

					trace("framework event %s %s", result, result.getType());
					switch (result.getType()) {
						case FrameworkEvent.STOPPED :
							trace("framework event stopped");
							System.exit(LauncherConstants.STOPPED);
							break;

						case FrameworkEvent.STARTLEVEL_CHANGED :
							trace("start level changed");
							break;

						case FrameworkEvent.WAIT_TIMEDOUT :
							trace("framework event timedout");
							System.exit(LauncherConstants.TIMEDOUT);
							break;

						case FrameworkEvent.ERROR :
							System.exit(ERROR);
							break;

						case FrameworkEvent.WARNING :
							System.exit(WARNING);
							break;

						case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED :
						case FrameworkEvent.STOPPED_UPDATE :
							trace("framework event update");
							System.exit(UPDATE_NEEDED);
							break;
					}
				} catch (InterruptedException e) {
					System.exit(CANCELED);
				}
			}
		};
		wait.start();
	}

	private void doSecurity() {
		try {
			PermissionInfo allPermissions[] = new PermissionInfo[] {
				new PermissionInfo(AllPermission.class.getName(), null, null)
			};
			policy = new SimplePermissionPolicy(this, systemBundle.getBundleContext());

			// All bundles installed from the script are getting AllPermission
			// for now.
			policy.setDefaultPermissions(allPermissions);
			security = true;
		} catch (Throwable t) {
			// This can throw a linkage error when the framework
			// does not carry the PermissionAdmin class
			security = false;
		}
	}

	private boolean isFragment(Bundle b) {
		return (b.adapt(BundleRevision.class)
			.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
	}

	private void deactivate() throws Exception {
		if (active.getAndSet(false)) {
			systemBundle.stop();
			systemBundle.waitForStop(parms.timeout);

			ThreadGroup group = Thread.currentThread()
				.getThreadGroup();
			Thread[] threads = new Thread[group.activeCount() + 100];
			group.enumerate(threads);
			{
				for (Thread t : threads) {
					if (t != null && !t.isDaemon() && t.isAlive()) {
						trace("alive thread %s", t);
					}
				}
			}
			startLevelhandler.close();
		}
	}

	public void addSystemPackage(String packageName) {
		parms.systemPackages = concat(parms.systemPackages, packageName);
	}

	private String concat(String a, String b) {
		if (a == null)
			return b;
		else if (b == null)
			return a;

		return a + "," + b;
	}

	private Framework createFramework() throws Exception {
		Properties p = new Properties();
		p.putAll(properties);
		File workingdir = null;
		if (parms.storageDir != null)
			workingdir = parms.storageDir;
		else if (parms.keep && parms.name != null) {
			workingdir = new File(bnd, parms.name);
		}

		if (workingdir == null) {
			workingdir = File.createTempFile("osgi.", ".fw");
			final File wd = workingdir;
			Runtime.getRuntime()
				.addShutdownHook(new Thread("launcher::delete temp working dir") {
					@Override
					public void run() {
						deleteFiles(wd);
					}
				});
		}

		trace("using working dir: %s with keeping=%s", workingdir, parms.keep);

		if (!parms.keep && workingdir.exists()) {
			trace("deleting working dir %s because not kept", workingdir);
			delete(workingdir);
			p.setProperty(Constants.FRAMEWORK_STORAGE_CLEAN, "true");
		}

		IO.mkdirs(workingdir);
		if (!workingdir.isDirectory())
			throw new IllegalArgumentException("Cannot create a working dir: " + workingdir);

		if (System.getProperty(Constants.FRAMEWORK_STORAGE) == null)
			p.setProperty(Constants.FRAMEWORK_STORAGE, workingdir.getAbsolutePath());
		else
			p.setProperty(Constants.FRAMEWORK_STORAGE, System.getProperty(Constants.FRAMEWORK_STORAGE));

		if (parms.systemPackages != null) {
			p.setProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, parms.systemPackages);
			trace("system packages used: %s", parms.systemPackages);
		}

		if (parms.systemCapabilities != null) {
			p.setProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA, parms.systemCapabilities);
			trace("system capabilities used: %s", parms.systemCapabilities);
		}

		Framework systemBundle;

		if (parms.services) {
			trace("using META-INF/services");
			// 3) framework = null, lookup in META-INF/services

			ClassLoader loader = getClass().getClassLoader();

			// 3) Lookup in META-INF/services
			List<String> implementations = getMetaInfServices(loader, FrameworkFactory.class.getName());

			if (implementations.isEmpty())
				error("Found no fw implementation");
			if (implementations.size() > 1)
				error("Found more than one framework implementations: %s", implementations);

			String implementation = implementations.get(0);

			Class<?> clazz = loader.loadClass(implementation);
			FrameworkFactory factory = (FrameworkFactory) newInstance(clazz);
			trace("Framework factory %s", factory);
			@SuppressWarnings({
				"unchecked", "rawtypes"
			})
			Map<String, String> configuration = (Map) p;
			systemBundle = factory.newFramework(configuration);
			trace("framework instance %s", systemBundle);
		} else {
			trace("using embedded mini framework because we were told not to use META-INF/services");
			// we have to use our own dummy framework
			systemBundle = new MiniFramework(p).setTracing(this::trace);
		}
		systemBundle.init();

		try {
			systemBundle.getBundleContext()
				.addFrameworkListener(event -> {
					switch (event.getType()) {
						case FrameworkEvent.ERROR :
							error(event.toString(), event.getThrowable());
							break;
						case FrameworkEvent.WAIT_TIMEDOUT :
							trace("Refresh will end due to error or timeout %s", event.toString());

					}
				});
		} catch (Exception e) {
			trace("could not register a framework listener: %s", e);
		}

		trace("inited system bundle %s", systemBundle);
		return systemBundle;
	}

	protected void deleteFiles(File wd) {
		IO.delete(wd);
	}

	/**
	 * Try to get the stupid service interface ...
	 *
	 * @param loader
	 * @param string
	 * @throws IOException
	 */
	private List<String> getMetaInfServices(ClassLoader loader, String factory) throws IOException {
		if (loader == null)
			loader = getClass().getClassLoader();

		Enumeration<URL> e = loader.getResources("META-INF/services/" + factory);
		List<String> factories = new ArrayList<>();

		while (e.hasMoreElements()) {
			URL url = e.nextElement();
			trace("found META-INF/services in %s", url);

			try (BufferedReader rdr = IO.reader(url.openStream(), UTF_8)) {
				String line;
				while ((line = rdr.readLine()) != null) {
					trace("%s", line);
					line = line.trim();
					if (!line.startsWith("#") && line.length() > 0) {
						factories.add(line);
					}
				}
			}
		}
		return factories;
	}

	public void addBundle(File resource) {
		parms.runbundles.add(resource.getAbsolutePath());
	}

	private void delete(File f) {
		IO.delete(f);
	}

	public void report(PrintStream out) {
		startLevelhandler.sync();
		try {
			out.println("------------------------------- REPORT --------------------------");
			out.println();
			row(out, "Framework", systemBundle == null ? "<>" : systemBundle.getClass());
			row(out, "Framework type", parms.services ? "META-INF/services" : "mini framework");
			row(out, "Storage", parms.storageDir);
			row(out, "Keep", parms.keep);
			row(out, "Security", security);
			row(out, "Has StartLevels", startLevelhandler.hasStartLevels());
			row(out, "Startlevel", startLevelhandler.getFrameworkStartLevel(systemBundle));
			list(out, fill("Run bundles", 40), parms.runbundles);
			row(out, "Java Home", System.getProperty("java.home"));
			list(out, fill("Classpath", 40), split(System.getProperty("java.class.path"), File.pathSeparator));
			list(out, fill("System Packages", 40), split(parms.systemPackages, ","));
			list(out, fill("System Capabilities", 40), split(parms.systemCapabilities, ","));
			row(out, "Properties");
			for (Entry<Object, Object> entry : properties.entrySet()) {
				String key = (String) entry.getKey();
				String value = (String) entry.getValue();
				row(out, key, value);
			}
			if (systemBundle != null) {
				BundleContext context = systemBundle.getBundleContext();
				if (context != null) {
					Bundle bundles[] = context.getBundles();
					out.println();
					out.println("Id    Levl State Modified      Location");

					for (int i = 0; i < bundles.length; i++) {
						String lastModified = "<>";
						String loc = bundles[i].getLocation();
						Optional<Path> p = URIUtil.pathFromURI(loc);
						if (p.isPresent() && Files.exists(p.get())) {
							lastModified = FORMAT.format(Files.getLastModifiedTime(p.get())
								.toInstant());
							loc = p.get()
								.toString();
						}
						out.print(fill(Long.toString(bundles[i].getBundleId()), 6));
						out.print(fill(startLevelhandler.getBundleStartLevel(bundles[i]) + "", 4));
						out.print(fill(toState(bundles[i].getState()), 6));
						out.print(fill(lastModified, 14));

						if (errors.containsKey(bundles[i])) {
							out.print(fill(loc, 50));
							out.print(errors.get(bundles[i])
								.toString());
						} else
							out.print(bundles[i].getLocation());

						out.println();
					}
				}
			}
		} catch (Throwable t) {
			error("Sorry, can't print framework: %s", t);
		}
	}

	private void row(PrintStream out, Object... parms) {
		boolean fill = true;
		for (Object p : parms) {
			if (fill)
				out.print(fill(p + "", 40));
			else
				out.print(p);
			fill = false;
		}
		out.println();
	}

	static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("YYYYMMddHHmm")
		.withZone(ZoneOffset.UTC);

	private String fill(String s, int width) {
		return fill(s, width, ' ', -1);
	}

	private String fill(String s, int width, char filler, int dir) {
		StringBuilder sb = new StringBuilder();
		if (s.length() > width) {
			int half = (width - 1) / 2;
			return s.substring(0, half) + ".." + s.substring(s.length() - half);
		}
		width -= s.length();
		int before = (dir == 0) ? width / 2 : (dir < 0) ? 0 : width;
		int after = width - before;

		while (before-- > 0)
			sb.append(filler);

		sb.append(s);

		while (after-- > 0)
			sb.append(filler);

		return sb.toString();
	}

	private String toState(int state) {
		switch (state) {
			case Bundle.INSTALLED :
				return "INSTL";
			case Bundle.RESOLVED :
				return "RSLVD";
			case Bundle.STARTING :
				return "STRTD";
			case Bundle.STOPPING :
				return "STPPD";
			case Bundle.ACTIVE :
				return "ACTIV";
			case Bundle.UNINSTALLED :
				return "UNNST";
		}
		return "? " + state;
	}

	private void list(PrintStream out, String del, List<?> l) {
		for (Object o : l) {
			String s = o.toString();
			out.print(del);
			out.println(s);
			del = fill(" ", 40);
		}
	}

	public int translateToError(BundleException e) {
		switch (e.getType()) {
			case BundleException.ACTIVATOR_ERROR :
				return LauncherConstants.ACTIVATOR_ERROR;

			case BundleException.DUPLICATE_BUNDLE_ERROR :
				return LauncherConstants.DUPLICATE_BUNDLE;

			case BundleException.RESOLVE_ERROR :
				return LauncherConstants.RESOLVE_ERROR;

			case BundleException.INVALID_OPERATION :
			case BundleException.MANIFEST_ERROR :
			case BundleException.NATIVECODE_ERROR :
			case BundleException.STATECHANGE_ERROR :
			case BundleException.UNSUPPORTED_OPERATION :
			case BundleException.UNSPECIFIED :
			default :
				return ERROR;
		}
	}

	public String translateToMessage(BundleException e) {
		switch (e.getType()) {
			case BundleException.ACTIVATOR_ERROR :
				Throwable t = e.getCause();
				StackTraceElement[] stackTrace = t.getStackTrace();
				if (stackTrace == null || stackTrace.length == 0)
					return "activator error " + t;
				StackTraceElement top = stackTrace[0];
				return "activator error " + t.getMessage() + " from: " + top.getClassName() + ":" + top.getMethodName()
					+ "#" + top.getLineNumber();

			case BundleException.DUPLICATE_BUNDLE_ERROR :
			case BundleException.RESOLVE_ERROR :
			case BundleException.INVALID_OPERATION :
			case BundleException.MANIFEST_ERROR :
			case BundleException.NATIVECODE_ERROR :
			case BundleException.STATECHANGE_ERROR :
			case BundleException.UNSUPPORTED_OPERATION :
			case BundleException.UNSPECIFIED :
			default :
				return e.getMessage();
		}
	}

	static final PermissionCollection all = new AllPermissionCollection();

	class AllPolicy extends Policy {

		@Override
		public PermissionCollection getPermissions(CodeSource codesource) {
			if (codesource == null)
				trace("Granting AllPermission to a bundle without codesource!");
			else
				trace("Granting AllPermission to %s", codesource.getLocation());
			return all;
		}

		@Override
		public void refresh() {
			trace("Policy refresh");
		}
	}

	static class AllPermissionCollection extends PermissionCollection {
		private static final long			serialVersionUID	= 1L;
		private static final List<Permission>	PERMISSIONS			= Collections.singletonList(new AllPermission());

		AllPermissionCollection() {
			setReadOnly();
		}

		@Override
		public void add(Permission permission) {}

		@Override
		public Enumeration<Permission> elements() {
			return Collections.enumeration(PERMISSIONS);
		}

		@Override
		public boolean implies(Permission permission) {
			return true;
		}
	}

	/**
	 * Monitor the services. If a service is registered with the {@code
	 * main.thread} property then check if it is a {@code Runnable} (priority
	 * for backward compatibility) or a {@code Callable<Integer>}. If so, we set
	 * it as the main thread runner and call it once the initialization is all
	 * done.
	 */

	@Override
	@SuppressWarnings("unchecked")
	public synchronized void serviceChanged(ServiceEvent event) {
		if (event.getType() == ServiceEvent.REGISTERED) {
			trace("service event %s", event);
			if (mainThread != null) {
				return;
			}
			try {
				final Object service = systemBundle.getBundleContext()
					.getService(event.getServiceReference());
				String[] objectclasses = (String[]) event.getServiceReference()
					.getProperty(Constants.OBJECTCLASS);

				// This looks a bit more complicated than necessary but for
				// backward compatibility reasons we require the Callable or
				// Runnable to be registered as such. Under that
				// condition, we prefer the Callable.
				for (String objectclass : objectclasses) {
					if (Callable.class.getName()
						.equals(objectclass)) {
						try {
							Method m = service.getClass()
								.getMethod("call");
							if (m.getReturnType() != Integer.class)
								throw new IllegalArgumentException("Found a main thread service which is Callable<"
									+ m.getReturnType()
										.getName()
									+ "> which should be Callable<Integer> " + event.getServiceReference());
							mainThread = (Callable<Integer>) service;
							return;
						} catch (NoSuchMethodException e) {
							assert false;
						}
					}
				}

				mainThread = Executors.callable((Runnable) service, 0);
			} finally {
				trace("selected main thread %s", event);
				notifyAll();
			}
		}
	}

	public void trace(String msg, Object... objects) {
		if (parms.trace) {
			message("# ", msg, objects);
		}
	}

	private void message(String prefix, String string, Object[] objects) {
		Throwable e = null;
		for (int n = 0; n < objects.length; n++) {
			Object o = objects[n];
			if (o instanceof Throwable) {
				Throwable t = e = (Throwable) o;
				if (t instanceof BundleException) {
					objects[n] = translateToMessage((BundleException) t);
				} else {
					for (Throwable cause; (t instanceof InvocationTargetException)
						&& ((cause = t.getCause()) != null);) {
						t = cause; // unwrap exception
					}
					objects[n] = t.getMessage();
				}
			}
		}

		StringBuilder sb = new StringBuilder(prefix);
		try (Formatter f = new Formatter(sb)) {
			f.format(string, objects);
		} catch (IllegalFormatException fe) {
			sb.append(fe);
		}

		String message = sb.toString();
		out.println(message);
		if (e != null)
			e.printStackTrace(out);
		out.flush();

		DatagramSocket socket = commsSocket.get();

		if (socket != null) {
			int severity;
			if (message.startsWith("! ")) {
				severity = 0; // NotificationType.ERROR.ordinal();
			} else if (message.startsWith("# ") && parms.trace) {
				severity = 2; // NotificationType.INFO.ordinal();
			} else {
				return;
			}

			try {
				ByteBufferDataOutput dos = new ByteBufferDataOutput();
				dos.writeInt(severity);
				dos.writeUTF(message.substring(2));
				if (e != null) {
					dos.writeUTF(toString(e));
				}
				ByteBuffer bb = dos.toByteBuffer();
				socket.send(new DatagramPacket(bb.array(), bb.arrayOffset(), bb.remaining()));
			} catch (IOException ioe) {
				out.println("! Unable to send notification to " + socket.getRemoteSocketAddress());
				if (parms.trace)
					ioe.printStackTrace(out);
				out.flush();
			}
		}
	}

	void error(String msg, Object... objects) {
		message("! ", msg, objects);
	}

	/**
	 * Find a bundle by its location.
	 *
	 * @param path the location to find
	 */
	private Bundle getBundleByLocation(String path) {
		BundleContext context = systemBundle.getBundleContext();
		for (Bundle b : context.getBundles()) {
			if (b.getLocation()
				.equals(path))
				return b;
		}
		return null;
	}

	private static String toString(Throwable t) {
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		return sw.toString();
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

}

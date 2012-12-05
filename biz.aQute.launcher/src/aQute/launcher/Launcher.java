package aQute.launcher;

import static aQute.launcher.constants.LauncherConstants.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.Map.Entry;

import org.osgi.framework.*;
import org.osgi.framework.launch.*;
import org.osgi.service.packageadmin.*;
import org.osgi.service.permissionadmin.*;

import aQute.launcher.constants.*;
import aQute.launcher.minifw.*;

/**
 * This is the primary bnd launcher. It implements a launcher that runs on Java
 * 1.4.
 */
public class Launcher implements ServiceListener {
	private PrintStream					out;
	LauncherConstants					parms;
	Framework							systemBundle;
	volatile boolean					inrefresh;
	private final Properties			properties;
	private boolean						security;
	private SimplePermissionPolicy		policy;
	private Runnable					mainThread;
	private PackageAdmin				padmin;
	private final List<BundleActivator>	embedded			= new ArrayList<BundleActivator>();
	private final Map<Bundle,Throwable>	errors				= new HashMap<Bundle,Throwable>();
	private final Map<File,Bundle>		installedBundles	= new LinkedHashMap<File,Bundle>();
	private File						home				= new File(System.getProperty("user.home"));
	private File						bnd					= new File(home, "bnd");
	private List<Bundle>				wantsToBeStarted	= new ArrayList<Bundle>();

	public static void main(String[] args) {
		try {
			final InputStream in;
			final File propertiesFile;

			String path = System.getProperty(LauncherConstants.LAUNCHER_PROPERTIES);
			if (path != null) {
				propertiesFile = new File(path).getAbsoluteFile();
				if (!propertiesFile.isFile())
					errorAndExit("Specified launch file `%s' was not found - absolutePath='%s'", path, propertiesFile.getAbsolutePath());
				in = new FileInputStream(propertiesFile);
			} else {
				propertiesFile = null;
				in = Launcher.class.getClassLoader().getResourceAsStream(DEFAULT_LAUNCHER_PROPERTIES);
				if (in == null) {
					printUsage();
					errorAndExit("Launch file not specified, and no embedded properties found.");
				}
			}

			Properties properties = new Properties();
			try {
				properties.load(in);
			}
			finally {
				if (in != null)
					in.close();
			}
			Launcher target = new Launcher(properties, propertiesFile);
			target.run(args);
		}
		catch (Throwable t) {
			// Last resort ... errors should be handled lower
			t.printStackTrace(System.err);
		}
	}

	private static void printUsage() {
		System.out.println("Usage: java -Dlauncher.properties=<launcher.properties> -jar <launcher.jar>");
	}

	private static void errorAndExit(String message, Object... args) {
		System.err.println(String.format(message, args));
		System.exit(1);
	}

	public Launcher(Properties properties, final File propertiesFile) throws Exception {
		this.properties = properties;

		// Allow the system to override any properties with -Dkey=value

		for (Object key : properties.keySet()) {
			String s = (String) key;
			String v = System.getProperty(s);
			if (v != null)
				properties.put(key, v);
		}

		System.getProperties().putAll(properties);
		this.parms = new LauncherConstants(properties);

		out = System.err;
		trace("inited runbundles=%s activators=%s timeout=%s", parms.runbundles, parms.activators, parms.timeout);
		if (propertiesFile != null && parms.embedded == false) {
			TimerTask watchdog = new TimerTask() {
				long	begin	= propertiesFile.lastModified();

				public void run() {
					if (begin < propertiesFile.lastModified()) {
						try {
							FileInputStream in = new FileInputStream(propertiesFile);
							Properties properties = new Properties();
							try {
								properties.load(in);
							}
							finally {
								in.close();
							}
							parms = new LauncherConstants(properties);
							update();
						}
						catch (Exception e) {
							error("Error in updating the framework from the properties: %s", e);
						}
						begin = propertiesFile.lastModified();
					}
				}
			};
			new Timer().scheduleAtFixedRate(watchdog, 5000, 1000);
		}
	}

	private void run(String args[]) throws Throwable {
		try {
			int status = activate();
			if (status != 0) {
				report(out);
				System.exit(status);
			}

			trace("framework=" + systemBundle);

			// Register the command line with ourselves as the
			// service.
			if (parms.services) { // Does not work for our dummy framework
				Hashtable<String,Object> argprops = new Hashtable<String,Object>();
				argprops.put(LauncherConstants.LAUNCHER_ARGUMENTS, args);
				argprops.put(LauncherConstants.LAUNCHER_READY, "true");
				argprops.put(Constants.SERVICE_RANKING, -1000);
				systemBundle.getBundleContext().registerService(new String[] {
						Object.class.getName(), Launcher.class.getName()
				}, this, argprops);
				trace("registered launcher with arguments for syncing");
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
			trace("Will run %s as main thread", mainThread);
			mainThread.run();
		}
		catch (Throwable e) {
			error("Unexpected error in the run body: %s", e);
			throw e;
		}
		finally {
			systemBundle.stop();
			trace("stopped system bundle due to leaving run body");
		}
	}

	private List<String> split(String value, String separator) {
		List<String> list = new ArrayList<String>();
		if (value == null)
			return list;

		StringTokenizer st = new StringTokenizer(value, separator);
		while (st.hasMoreTokens())
			list.add(st.nextToken());

		return list;
	}

	public int activate() throws Exception {
		Policy.setPolicy(new AllPolicy());

		systemBundle = createFramework();
		if (systemBundle == null)
			return LauncherConstants.ERROR;

		doTimeoutHandler();

		doSecurity();

		// Initialize this framework so it becomes STARTING
		systemBundle.start();
		trace("system bundle started ok");

		BundleContext systemContext = systemBundle.getBundleContext();
		ServiceReference ref = systemContext.getServiceReference(PackageAdmin.class.getName());
		if (ref != null) {
			padmin = (PackageAdmin) systemContext.getService(ref);
		} else
			trace("could not get package admin");

		systemContext.addServiceListener(this, "(&(objectclass=java.lang.Runnable)(main.thread=true))");

		// Start embedded activators
		trace("start embedded activators");
		if (parms.activators != null) {
			ClassLoader loader = getClass().getClassLoader();
			for (Object token : parms.activators) {
				try {
					Class< ? > clazz = loader.loadClass((String) token);
					BundleActivator activator = (BundleActivator) clazz.newInstance();
					embedded.add(activator);
					trace("adding activator %s", activator);
				}
				catch (Exception e) {
					throw new IllegalArgumentException("Embedded Bundle Activator incorrect: " + token + ", " + e);
				}
			}
		}

		update();

		if (parms.trace) {
			report(out);
		}

		int result = LauncherConstants.OK;
		for (BundleActivator activator : embedded)
			try {
				trace("starting activator %s", activator);
				activator.start(systemContext);
			}
			catch (Exception e) {
				error("Starting activator %s : %s", activator, e);
				result = LauncherConstants.ERROR;
			}

		return result;
	}

	/**
	 * Ensure that all the bundles in the parameters are actually started. We
	 * can start in embedded mode (bundles are inside our main jar) or in file
	 * system mode.
	 */
	void update() throws Exception {
		trace("Updating framework with %s", parms.runbundles);

		List<Bundle> tobestarted = new ArrayList<Bundle>();
		if (parms.embedded)
			installEmbedded(tobestarted);
		else
			synchronizeFiles(tobestarted);

		if (padmin != null) {
			inrefresh = true;
			padmin.refreshPackages(null);
			trace("Waiting for refresh to finish");

			// Will be reset by the Framework listener we added
			// when we created the framework.
			while (inrefresh)
				Thread.sleep(100);
		} else
			trace("cannot refresh the bundles because there is no Package Admin");

		trace("bundles administered %s", installedBundles.keySet());

		// From now on, the bundles are on their own. They have
		// by default AllPermission, but if they install bundles
		// they will not automatically get AllPermission anymore
		if (security)
			policy.setDefaultPermissions(null);

		// Get the resolved status
		if (padmin != null && padmin.resolveBundles(null) == false) {
			error("could not resolve the bundles");
			// return LauncherConstants.RESOLVE_ERROR;
		}

		// Now start all the installed bundles in the same order
		// (unless they're a fragment)

		trace("Will start bundles: %s", tobestarted);
		List<Bundle> all = new ArrayList<Bundle>(tobestarted);
		// Add all bundles that we've tried to start but failed
		all.addAll(wantsToBeStarted);
		
		for (Bundle b : tobestarted) {
			try {
				trace("starting %s", b.getSymbolicName());
				if (!isFragment(b))
					b.start(Bundle.START_ACTIVATION_POLICY);
				trace("started  %s", b.getSymbolicName());
			}
			catch (BundleException e) {
				wantsToBeStarted.add(b);
				error("Failed to start bundle %s-%s, exception %s", b.getSymbolicName(), b.getVersion(), e);
			}
		}

	}

	/**
	 * @param tobestarted
	 */
	void synchronizeFiles(List<Bundle> tobestarted) {
		// Turn the bundle location paths into files
		List<File> desired = new ArrayList<File>();
		for (Object o : parms.runbundles) {
			File file = new File((String) o).getAbsoluteFile();
			if (!file.exists())
				error("Bundle files does not exist: " + file);
			else
				desired.add(file);
		}

		// deleted = old - new
		List<File> tobedeleted = new ArrayList<File>(installedBundles.keySet());
		tobedeleted.removeAll(desired);

		// updated = old /\ new
		List<File> tobeupdated = new ArrayList<File>(installedBundles.keySet());
		tobeupdated.retainAll(desired);

		// install = new - old
		List<File> tobeinstalled = new ArrayList<File>(desired);
		tobeinstalled.removeAll(installedBundles.keySet());

		for (File f : tobedeleted)
			try {
				trace("uninstalling %s", f);
				installedBundles.get(f).uninstall();
				installedBundles.remove(f);
			}
			catch (Exception e) {
				error("Failed to uninstall bundle %s, exception %s", f, e);
			}

		for (File f : tobeinstalled)
			try {
				trace("installing %s", f);
				Bundle b = install(f);
				installedBundles.put(f, b);
				tobestarted.add(b);
			}
			catch (Exception e) {
				error("Failed to uninstall bundle %s, exception %s", f, e);
			}

		for (File f : tobeupdated)
			try {
				Bundle b = installedBundles.get(f);
				if (b.getLastModified() < f.lastModified()) {
					trace("updating %s", f);
					if (b.getState() == Bundle.ACTIVE) {
						tobestarted.add(b);
						b.stop();
					}
					b.update();
				} else
					trace("bundle is still current according to timestamp %s", f);
			}
			catch (Exception e) {
				error("Failed to update bundle %s, exception %s", f, e);
			}
	}

	/**
	 * Install the bundles from the current jar.
	 * 
	 * @param tobestarted
	 * @throws BundleException
	 * @throws IOException
	 */
	void installEmbedded(List<Bundle> tobestarted) throws BundleException, IOException {
		trace("starting in embedded mode");
		BundleContext context = systemBundle.getBundleContext();
		for (Object o : parms.runbundles) {
			String path = (String) o;
			trace("installing %s", path);
			InputStream in = getClass().getClassLoader().getResourceAsStream(path);
			try {
				Bundle bundle = context.installBundle(path, in);
				tobestarted.add(bundle);
			}
			finally {
				in.close();
			}
		}
	}

	Bundle install(File f) throws Exception {
		BundleContext context = systemBundle.getBundleContext();
		try {
			trace("will install %s with reference", f.getAbsolutePath());
			String reference = "reference:" + f.toURI().toURL().toExternalForm();
			Bundle b = context.installBundle(reference);
			if (b.getLastModified() < f.lastModified()) {
				b.update();
			}
			return b;
		}
		catch (BundleException e) {
			trace("failed reference, will try to install %s with input stream", f.getAbsolutePath());
			String reference = f.toURI().toURL().toExternalForm();
			InputStream in = new FileInputStream(f);
			try {
				return context.installBundle(reference, in);
			}
			finally {
				in.close();
			}
		}
	}

	private void doTimeoutHandler() {
		// Ensure we properly close in a separate thread so that
		// we can leverage the main thread, which is required for macs
		Thread wait = new Thread("FrameworkWaiter") {
			@Override
			public void run() {
				try {
					FrameworkEvent result = systemBundle.waitForStop(parms.timeout);
					trace("framework event " + result + " " + result.getType());
					Thread.sleep(1000);
					switch (result.getType()) {
						case FrameworkEvent.STOPPED :
							System.exit(LauncherConstants.OK);
							break;
						case FrameworkEvent.WAIT_TIMEDOUT :
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
							System.exit(UPDATE_NEEDED);
							break;
					}
				}
				catch (InterruptedException e) {
					System.exit(CANCELED);
				}
				finally {
					System.err.println("System exiting due to timeout of " + parms.timeout);
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
		}
		catch (Throwable t) {
			// This can throw a linkage error when the framework
			// does not carry the PermissionAdmin class
			security = false;
		}
	}

	private boolean isFragment(Bundle b) {
		return padmin != null && padmin.getBundleType(b) == PackageAdmin.BUNDLE_TYPE_FRAGMENT;
	}

	public void deactivate() throws Exception {
		if (systemBundle != null) {
			systemBundle.stop();
			systemBundle.waitForStop(parms.timeout);
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

		if (workingdir == null)
			workingdir = File.createTempFile("osgi.", ".fw");

		trace("using working dir: %s", workingdir);

		if (!parms.keep && workingdir.exists()) {
			trace("deleting working dir %s because not kept", workingdir);
			delete(workingdir);
			p.setProperty(Constants.FRAMEWORK_STORAGE_CLEAN, "true");
		}

		if (!workingdir.exists() && !workingdir.mkdirs()) {
			throw new IOException("Could not create directory " + workingdir);
		}
		if (!workingdir.isDirectory())
			throw new IllegalArgumentException("Cannot create a working dir: " + workingdir);

		p.setProperty(Constants.FRAMEWORK_STORAGE, workingdir.getAbsolutePath());

		if (parms.systemPackages != null) {
			p.setProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, parms.systemPackages);
			trace("system packages used: %s", parms.systemPackages);
		}

		Framework systemBundle;

		if (parms.services) {
			trace("using META-INF/services");
			// 3) framework = null, lookup in META-INF/services

			ClassLoader loader = getClass().getClassLoader();

			// 3) Lookup in META-INF/services
			List<String> implementations = getMetaInfServices(loader, FrameworkFactory.class.getName());

			if (implementations.size() == 0)
				error("Found no fw implementation");
			if (implementations.size() > 1)
				error("Found more than one framework implementations: %s", implementations);

			String implementation = implementations.get(0);

			Class< ? > clazz = loader.loadClass(implementation);
			FrameworkFactory factory = (FrameworkFactory) clazz.newInstance();
			trace("Framework factory %s", factory);
			systemBundle = factory.newFramework(p);
			trace("framework instance %s", systemBundle);
		} else {
			trace("using embedded mini framework because we were told not to use META-INF/services");
			// we have to use our own dummy framework
			systemBundle = new MiniFramework(p);
		}
		systemBundle.init();

		try {
			systemBundle.getBundleContext().addFrameworkListener(new FrameworkListener() {

				public void frameworkEvent(FrameworkEvent event) {
					switch (event.getType()) {
						case FrameworkEvent.ERROR :
						case FrameworkEvent.WAIT_TIMEDOUT :
							trace("Refresh will end due to error or timeout %s", event.toString());
							
						case FrameworkEvent.PACKAGES_REFRESHED :
							inrefresh = false;
							trace("refresh ended");
							break;
					}
				}
			});
		}
		catch (Exception e) {
			trace("could not register a framework listener: %s", e);
		}
		trace("inited system bundle %s", systemBundle);
		return systemBundle;
	}

	/**
	 * Try to get the stupid service interface ...
	 * 
	 * @param loader
	 * @param string
	 * @return
	 * @throws IOException
	 */
	private List<String> getMetaInfServices(ClassLoader loader, String factory) throws IOException {
		if (loader == null)
			loader = getClass().getClassLoader();

		Enumeration<URL> e = loader.getResources("META-INF/services/" + factory);
		List<String> factories = new ArrayList<String>();

		while (e.hasMoreElements()) {
			URL url = e.nextElement();
			trace("found META-INF/services in %s", url);

			InputStream in = null;
			BufferedReader rdr = null;
			String line;
			try {
				in = url.openStream();
				rdr = new BufferedReader(new InputStreamReader(in, "UTF-8"));
				while ((line = rdr.readLine()) != null) {
					trace(line);
					line = line.trim();
					if (!line.startsWith("#") && line.length() > 0) {
						factories.add(line);
					}
				}
			}
			finally {
				if (rdr != null) {
					rdr.close();
				}
				if (in != null) {
					in.close();
				}
			}
		}
		return factories;
	}

	public void addBundle(File resource) {
		parms.runbundles.add(resource.getAbsolutePath());
	}

	private void delete(File f) {
		String path = f.getAbsolutePath();
		char first = path.charAt(0);
		if (path.equals("/") || (first >= 'A' && first <= 'Z' && path.substring(1).equals(":\\")))
			throw new IllegalArgumentException("You can not make the root the storage area because it will be deleted");
		if (f.isDirectory()) {
			File fs[] = f.listFiles();
			for (int i = 0; i < fs.length; i++)
				delete(fs[i]);
		}
		f.delete();
	}

	public void report(PrintStream out) {
		try {
			out.println("------------------------------- REPORT --------------------------");
			out.println();
			row(out, "Framework", systemBundle == null ? "<>" : systemBundle.getClass());
			row(out, "Framework type", parms.services ? "META-INF/services" : "mini framework");
			row(out, "Storage", parms.storageDir);
			row(out, "Keep", parms.keep);
			row(out, "Security", security);
			list(out, fill("Run bundles", 40), parms.runbundles);
			list(out, fill("Classpath", 40), split(System.getProperty("java.class.path"), File.pathSeparator));
			list(out, fill("System Packages", 40), split(parms.systemPackages, ","));
			row(out, "Properties");
			for (Entry<Object,Object> entry : properties.entrySet()) {
				String key = (String) entry.getKey();
				String value = (String) entry.getValue();
				row(out, key, value);
			}
			if (systemBundle != null) {
				BundleContext context = systemBundle.getBundleContext();
				if (context != null) {
					Bundle bundles[] = context.getBundles();
					out.println();
					out.println("Id    State Modified      Location");

					for (int i = 0; i < bundles.length; i++) {
						String loc = bundles[i].getLocation();
						loc = loc.replaceAll("\\w+:", "");
						File f = new File(loc);
						out.print(fill(Long.toString(bundles[i].getBundleId()), 6));
						out.print(fill(toState(bundles[i].getState()), 6));
						if (f.exists())
							out.print(fill(toDate(f.lastModified()), 14));
						else
							out.print(fill("<>", 14));

						if (errors.containsKey(bundles[i])) {
							out.print(fill(loc, 50));
							out.print(errors.get(bundles[i]).getMessage());
						} else
							out.print(bundles[i].getLocation());

						out.println();
					}
				}
			}
		}
		catch (Throwable t) {
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

	String toDate(long t) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(t);
		return fill(c.get(Calendar.YEAR), 4) + fill(c.get(Calendar.MONTH), 2) + fill(c.get(Calendar.DAY_OF_MONTH), 2)
				+ fill(c.get(Calendar.HOUR_OF_DAY), 2) + fill(c.get(Calendar.MINUTE), 2);
	}

	private String fill(int n, int width) {
		return fill(Integer.toString(n), width, '0', -1);
	}

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

	private void list(PrintStream out, String del, List< ? > l) {
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
					return "activator error " + t.getMessage();
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

	static PermissionCollection	all	= new AllPermissionCollection();

	class AllPolicy extends Policy {

		@Override
		public PermissionCollection getPermissions(CodeSource codesource) {
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
		private static Vector<Permission>	list				= new Vector<Permission>();

		static {
			list.add(new AllPermission());
		}

		{
			setReadOnly();
		}

		@Override
		public void add(Permission permission) {}

		@Override
		public Enumeration<Permission> elements() {
			return list.elements();
		}

		@Override
		public boolean implies(Permission permission) {
			return true;
		}
	}

	public synchronized void serviceChanged(ServiceEvent event) {
		if (event.getType() == ServiceEvent.REGISTERED) {
			mainThread = (Runnable) systemBundle.getBundleContext().getService(event.getServiceReference());
			notifyAll();
		}
	}

	public void trace(String msg, Object... objects) {
		if (parms.trace) {
			message("# ", msg, objects);
		}
	}

	private void message(String prefix, String string, Object[] objects) {
		Throwable e = null;

		StringBuilder sb = new StringBuilder();
		int n = 0;
		sb.append(prefix);
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c == '%') {
				c = string.charAt(++i);
				switch (c) {
					case 's' :
						if (n < objects.length) {
							Object o = objects[n++];
							if (o instanceof Throwable) {
								e = (Throwable) o;
								if (o instanceof BundleException) {
									sb.append(translateToMessage((BundleException) o));
								} else if (o instanceof InvocationTargetException) {
									Throwable t = (InvocationTargetException) o;
									sb.append(t.getMessage());
									e = t;
								} else
									sb.append(e.getMessage());
							} else {
								sb.append(o);
							}
						} else
							sb.append("<no more arguments>");
						break;

					default :
						sb.append(c);
				}
			} else {
				sb.append(c);
			}
		}
		out.println(sb);
		if (e != null && parms.trace)
			e.printStackTrace(out);
		out.flush();
	}

	public void error(String msg, Object... objects) {
		message("! ", msg, objects);
	}

}

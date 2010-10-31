package aQute.launcher;

import static aQute.launcher.constants.LauncherConstants.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.Map.*;

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
	private PrintStream						out;
	private LauncherConstants				parms;
	private Framework						systemBundle;
	private final Properties				properties;
	private boolean							security;
	private SimplePermissionPolicy			policy;
	private Runnable						mainThread;
	private PackageAdmin					padmin;
	private static File						propertiesFile;
	private final Timer						timer		= new Timer();
	private final List<BundleActivator>		embedded	= new ArrayList<BundleActivator>();
	private TimerTask						watchdog	= null;
	private final Map<Bundle, Throwable>	errors		= new HashMap<Bundle, Throwable>();

	public static void main(String[] args) {
		try {
			String path = System.getProperty(LauncherConstants.LAUNCHER_PROPERTIES);
			assert path != null;
			propertiesFile = new File(path).getAbsoluteFile();
			FileInputStream in = new FileInputStream(propertiesFile);
			Properties properties = new Properties();
			try {
				properties.load(in);
			} finally {
				in.close();
			}
			Launcher target = new Launcher(properties);
			target.run(args);
		} catch (Throwable t) {
			// Last resort ... errors should be handled lower
			t.printStackTrace(System.err);
		}
	}

	public Launcher(Properties properties) throws Exception {
		this.properties = properties;
		System.getProperties().putAll(properties);
		this.parms = new LauncherConstants(properties);

		out = System.err;
		trace("inited runbundles=%s timeout=%s", parms.runbundles, parms.activators, parms.timeout);
		watchdog = new TimerTask() {
			long	begin	= propertiesFile.lastModified();

			public void run() {
				if (begin < propertiesFile.lastModified()) {
					update();
					begin = propertiesFile.lastModified();
				}
			}
		};
		timer.scheduleAtFixedRate(watchdog, 5000, 1000);
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
			if (parms.services) { //  Does not work for our dummy framework
				Hashtable<String, Object> argprops = new Hashtable<String, Object>();
				argprops.put(LauncherConstants.LAUNCHER_ARGUMENTS, args);
				argprops.put(LauncherConstants.LAUNCHER_READY, "true");
				systemBundle.getBundleContext().registerService(Launcher.class.getName(), this,
						argprops);
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
		} catch (Throwable e) {
			error("Unexpected error in the run body: %s", e);
			throw e;
		} finally {
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

		if (parms.timeout != 0)
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

		systemContext.addServiceListener(this,
				"(&(objectclass=java.lang.Runnable)(main.thread=true))");

		// Install the set of bundles
		List<Bundle> installed = new ArrayList<Bundle>();

		for (Object i : parms.runbundles) {
			File path = new File((String) i).getAbsoluteFile();

			try {
				trace("will install %s with reference", path.getAbsolutePath());
				String reference = "reference:" + path.toURL().toExternalForm();
				Bundle b = systemContext.installBundle(reference);
				if (b.getLastModified() < path.lastModified()) {
					b.update();
				}
				installed.add(b);
			} catch (BundleException e) {
				trace("failed reference, will try to install %s with input stream", path.getAbsolutePath());
				String reference = path.toURL().toExternalForm();
				InputStream in = new FileInputStream(path);
				try {
					Bundle b = findBundleByLocation(reference);
					if (b != null) {
						trace("already installed");
						if (b.getLastModified() < path.lastModified()) {
							b.update(in);
							trace("updated");
						}
					} else {
						b = systemContext.installBundle(reference, in);
						installed.add(b);
						trace("installed");
					}
				} catch (BundleException ee) {
					error("Install: %s error: %s", path, ee);
					return translateToError(ee);
				} finally {
					in.close();
				}
			}
		}
		trace("all bundles installed %s", parms.runbundles);

		// From now on, the bundles are on their own. They have
		// by default AllPermission, but if they install bundles
		// they will not automatically get AllPermission anymore
		if (security)
			policy.setDefaultPermissions(null);

		// Get the resolved status
		if (padmin != null && padmin.resolveBundles(null) == false) {
			error("could not resolve the bundles");
			//return LauncherConstants.RESOLVE_ERROR;
		}

		// Now start all the installed bundles in the same order
		// (unless they're a fragment)

		for (Iterator<Bundle> i = installed.iterator(); i.hasNext();) {
			Bundle b = (Bundle) i.next();
			try {
				trace("starting %s", b.getSymbolicName());
				if (!isFragment(b))
					b.start();
				trace("started  %s", b.getSymbolicName());
			} catch (BundleException e) {
				errors.put(b, e);
				error("Start: %s, cause: %s", b.getBundleId(), e);
				return translateToError(e);
			} catch (RuntimeException e) {
				errors.put(b, e);
				throw e;
			}
		}

		if (parms.trace) {
			report(out);
		}

		// Start embedded activators
		trace("start embedded activators");
		if (parms.activators != null) {
			ClassLoader loader = getClass().getClassLoader();
			for (Object token : parms.activators) {
				try {
					Class<?> clazz = loader.loadClass((String) token);
					BundleActivator activator = (BundleActivator) clazz.newInstance();
					embedded.add(activator);
					trace("adding activator %s", activator);
				} catch (Exception e) {
					throw new IllegalArgumentException("Embedded Bundle Activator incorrect: "
							+ token + ", " + e);
				}
			}
		}
		for (BundleActivator activator : embedded)
			try {
				trace("starting activator %s", activator);
				activator.start(systemContext);
			} catch (Exception e) {
				error("Starting activator %s : %s", activator, e);
				e.printStackTrace();
				return LauncherConstants.ERROR;
			}

		return LauncherConstants.OK;
	}

	private Bundle findBundleByLocation(String location) {
		Bundle bs[] = systemBundle.getBundleContext().getBundles();
		for (Bundle b : bs) {
			if (location.equals(b.getLocation()))
				return b;
		}
		return null;
	}

	private void doTimeoutHandler() {
		// Ensure we properly close in a separate thread so that
		// we can leverage the main thread, which is required for macs
		Thread wait = new Thread("FrameworkWaiter") {
			public void run() {
				try {
					FrameworkEvent result = systemBundle.waitForStop(parms.timeout);
					switch (result.getType()) {
					case FrameworkEvent.STOPPED:
						System.exit(LauncherConstants.OK);
						break;
					case FrameworkEvent.WAIT_TIMEDOUT:
						System.exit(LauncherConstants.TIMEDOUT);
						break;

					case FrameworkEvent.ERROR:
						System.exit(ERROR);
						break;

					case FrameworkEvent.WARNING:
						System.exit(WARNING);
						break;

					case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
					case FrameworkEvent.STOPPED_UPDATE:
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

	void update() {
		Bundle[] bundles = systemBundle.getBundleContext().getBundles();
		List<Bundle> tobeupdated = new ArrayList<Bundle>();

		for (Bundle b : bundles) {
			String location = b.getLocation();
			File f = new File(location);
			if (parms.runbundles.contains(f)) {
				if (b.getLastModified() < f.lastModified()) {
					tobeupdated.add(b);
					try {
						b.stop();
					} catch (BundleException e) {
						// Ignore for now
					}
				}
			}
		}

		for (Bundle b : tobeupdated) {
			try {
				b.update();
			} catch (BundleException e) {
				out.println("Failed to update " + b.getLocation());
			}
		}
		if (padmin != null)
			padmin.refreshPackages(null);

		for (Bundle b : tobeupdated) {
			try {
				b.start();
			} catch (BundleException e) {
				out.println("Failed to start " + b.getLocation());
			}
		}

	}

	private void doSecurity() {
		try {
			PermissionInfo allPermissions[] = new PermissionInfo[] { new PermissionInfo(
					AllPermission.class.getName(), null, null) };
			policy = new SimplePermissionPolicy(systemBundle.getBundleContext());

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
		File workingdir = new File("tmp").getAbsoluteFile();
		if (parms.storageDir != null)
			workingdir = parms.storageDir;

		trace("using working dir: %s", parms.storageDir);

		if (!parms.keep) {
			trace("deleting working dir %s because not kept", workingdir);
			delete(workingdir);
			p.setProperty(Constants.FRAMEWORK_STORAGE_CLEAN, "true");
		}

		workingdir.mkdirs();
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
			List<String> implementations = getMetaInfServices(loader, FrameworkFactory.class
					.getName());

			if (implementations.size() == 0)
				error("Found no fw implementation");
			if (implementations.size() > 1)
				error("Found more than one framework implementations: %s", implementations);

			String implementation = (String) implementations.get(0);

			Class<?> clazz = loader.loadClass(implementation);
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
			URL url = (URL) e.nextElement();
			trace("found META-INF/services in %s", url);

			InputStream in = url.openStream();
			try {
				BufferedReader rdr = new BufferedReader(new InputStreamReader(in));
				String line;
				while ((line = rdr.readLine()) != null) {
					trace(line);
					line = line.trim();
					if (!line.startsWith("#") && line.length() > 0) {
						factories.add(line);
					}
				}
			} finally {
				in.close();
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
			throw new IllegalArgumentException(
					"You can not make the root the storage area because it will be deleted");
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
			list(out, fill("Classpath", 40), split(System.getProperty("java.class.path"),
					File.pathSeparator));
			list(out, fill("System Packages", 40), split(parms.systemPackages, ","));
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
					out.println("Id    State Modified      Location");

					for (int i = 0; i < bundles.length; i++) {
						File f = new File(bundles[i].getLocation());
						out.print(fill(Long.toString(bundles[i].getBundleId()), 6));
						out.print(fill(toState(bundles[i].getState()), 6));
						if (f.exists())
							out.print(fill(toDate(f.lastModified()), 14));
						else
							out.print(fill("<>", 14));
						if (errors.containsKey(bundles[i])) {
							out.print(fill(bundles[i].getLocation(), 50));
							out.print(errors.get(bundles[i]).getMessage());
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
				out.print(fill(p.toString(), 40));
			else
				out.print(p.toString());
			fill = false;
		}
		out.println();
	}

	String toDate(long t) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(t);
		return fill(c.get(Calendar.YEAR), 4) + fill(c.get(Calendar.MONTH), 2)
				+ fill(c.get(Calendar.DAY_OF_MONTH), 2) + fill(c.get(Calendar.HOUR_OF_DAY), 2)
				+ fill(c.get(Calendar.MINUTE), 2);
	}

	private String fill(int n, int width) {
		return fill(Integer.toString(n), width, '0', -1);
	}

	private String fill(String s, int width) {
		return fill(s, width, ' ', -1);
	}

	private String fill(String s, int width, char filler, int dir) {
		StringBuffer sb = new StringBuffer();
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
		case Bundle.INSTALLED:
			return "INSTL";
		case Bundle.RESOLVED:
			return "RSLVD";
		case Bundle.STARTING:
			return "STRTD";
		case Bundle.STOPPING:
			return "STPPD";
		case Bundle.ACTIVE:
			return "ACTIV";
		case Bundle.UNINSTALLED:
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
		case BundleException.ACTIVATOR_ERROR:
			return LauncherConstants.ACTIVATOR_ERROR;

		case BundleException.DUPLICATE_BUNDLE_ERROR:
			return LauncherConstants.DUPLICATE_BUNDLE;

		case BundleException.RESOLVE_ERROR:
			return LauncherConstants.RESOLVE_ERROR;

		case BundleException.INVALID_OPERATION:
		case BundleException.MANIFEST_ERROR:
		case BundleException.NATIVECODE_ERROR:
		case BundleException.STATECHANGE_ERROR:
		case BundleException.UNSUPPORTED_OPERATION:
		case BundleException.UNSPECIFIED:
		default:
			return ERROR;
		}
	}

	public String translateToMessage(BundleException e) {
		switch (e.getType()) {
		case BundleException.ACTIVATOR_ERROR:
			Throwable t = e.getCause();
			StackTraceElement[] stackTrace = t.getStackTrace();
			if (stackTrace == null || stackTrace.length == 0)
				return "activator error " + t.getMessage();
			StackTraceElement top = stackTrace[0];
			return "activator error " + t.getMessage() + " from: " + top.getClassName() + ":"
					+ top.getMethodName() + "#" + top.getLineNumber();

		case BundleException.DUPLICATE_BUNDLE_ERROR:
		case BundleException.RESOLVE_ERROR:
		case BundleException.INVALID_OPERATION:
		case BundleException.MANIFEST_ERROR:
		case BundleException.NATIVECODE_ERROR:
		case BundleException.STATECHANGE_ERROR:
		case BundleException.UNSUPPORTED_OPERATION:
		case BundleException.UNSPECIFIED:
		default:
			return e.getMessage();
		}
	}

	static class AllPolicy extends Policy {
		static PermissionCollection	all	= new AllPermissionCollection();

		public PermissionCollection getPermissions(CodeSource codesource) {
			return all;
		}

		public void refresh() {
		}
	}

	static class AllPermissionCollection extends PermissionCollection {
		private static final long			serialVersionUID	= 1L;
		private static Vector<Permission>	list				= new Vector<Permission>();

		{
			setReadOnly();
		}

		public void add(Permission permission) {
		}

		public Enumeration<Permission> elements() {
			return list.elements();
		}

		public boolean implies(Permission permission) {
			return true;
		}
	}

	public synchronized void serviceChanged(ServiceEvent event) {
		if (event.getType() == ServiceEvent.REGISTERED) {
			mainThread = (Runnable) systemBundle.getBundleContext().getService(
					event.getServiceReference());
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

		StringBuffer sb = new StringBuffer();
		int n = 0;
		sb.append(prefix);
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			if (c == '%') {
				c = string.charAt(++i);
				switch (c) {
				case 's':
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

				default:
					sb.append(c);
				}
			} else {
				sb.append(c);
			}
		}
		out.println(sb);
		if (e != null && parms.trace)
			e.printStackTrace(out);
	}

	public void error(String msg, Object... objects) {
		message("! ", msg, objects);
	}

}

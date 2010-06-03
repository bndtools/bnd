package aQute.launcher;

import java.io.*;
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
public class Launcher implements LauncherConstants, ServiceListener {
	private PrintStream				out;
	private final List<String>		runbundles	= new ArrayList<String>();
	private String					systemPackages;

	private Framework				systemBundle;
	private String					storage;
	private boolean					keep;
	private final Properties		properties;
	private boolean					security;
	private SimplePermissionPolicy	policy;
	private boolean					report;
	private long					timeout		= 0;
	private Runnable				mainThread;
	private PackageAdmin			padmin;
	private File					propertiesFile;
	private Timer					timer		= new Timer();
	private List<BundleActivator>	embedded	= new ArrayList<BundleActivator>();

	private TimerTask				watchdog	= null;
	private boolean					services	= true;

	public static void main(String[] args) throws Throwable {
		String path = System.getProperty(LAUNCH_PROPERTIES);
		assert path != null;
		Launcher target = new Launcher(path);
		target.run(args);
	}

	public Launcher(Properties properties) {
		this.properties = properties;
		init();
	}

	public Launcher(String path) throws Exception {
		out = System.err;
		propertiesFile = new File(path).getAbsoluteFile();
		FileInputStream in = new FileInputStream(propertiesFile);
		properties = new Properties();
		try {
			properties.load(in);
		} finally {
			in.close();
		}
		System.getProperties().putAll(properties);
		init();
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

	void init() {
		storage = properties.getProperty(LAUNCH_STORAGE_DIR, "");
		keep = isSet(LAUNCH_KEEP);
		report = isSet(LAUNCH_REPORT);
		runbundles.addAll(split(properties.getProperty(LAUNCH_RUNBUNDLES)));
		systemPackages = properties.getProperty(LAUNCH_SYSTEMPACKAGES);
		timeout = Long.parseLong(properties.getProperty(LAUNCH_TIMEOUT, "0"));

		String fw = properties.getProperty(LAUNCH_FRAMEWORK);
		if ("none".equalsIgnoreCase(fw))
			services = false;
		else
			services = true;
	}

	private void run(String args[]) throws Throwable {
		try {
			int status = activate();
			if (status != 0) {
				report(out);
				System.exit(status);
			}

			// Register the command line with ourselves as the
			// service.
			if (services) {
				Hashtable<String, Object> argprops = new Hashtable<String, Object>();
				argprops.put(LAUNCHER_ARGUMENTS, args);
				argprops.put(LAUNCHER_READY, "true");
				systemBundle.getBundleContext().registerService(Launcher.class.getName(), this,
						argprops);
			}
			
			// Wait until a Runnable is registered with main.thread=true.
			// not that this will never happen when we're running on the mini fw
			// but the test case normally exits.
			synchronized (this) {
				while (mainThread == null)
					wait();
			}
			mainThread.run();
		} catch (Throwable e) {
			report(out);
			throw e;
		} finally {
			systemBundle.stop();
		}
	}

	private List<String> split(String value) {
		return split(value, " ,");
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

	private boolean isSet(String key) {
		return properties.containsKey(key) && "true".equalsIgnoreCase(properties.getProperty(key));
	}

	public int activate() throws Exception {
		Policy.setPolicy(new AllPolicy());

		systemBundle = createFramework();
		if (systemBundle == null)
			return ERROR;

		if (timeout != 0)
			doTimeoutHandler();

		doSecurity();

		// Initialize this framework so it becomes STARTING
		systemBundle.start();

		BundleContext systemContext = systemBundle.getBundleContext();
		ServiceReference ref = systemContext.getServiceReference(PackageAdmin.class.getName());
		if (ref != null) {
			padmin = (PackageAdmin) systemContext.getService(ref);
		}

		systemContext.addServiceListener(this,
				"(&(objectclass=java.lang.Runnable)(main.thread=true))");

		// Install the set of bundles
		List<Bundle> installed = new ArrayList<Bundle>();

		for (String i : runbundles) {
			File path = new File(i).getAbsoluteFile();

			InputStream in = new FileInputStream(path);
			try {
				Bundle b = findBundleByLocation(path.toString());
				if (b != null) {
					if (b.getLastModified() < path.lastModified())
						b.update(in);
				} else {
					b = systemContext.installBundle(path.toString(), in);
					installed.add(b);
				}
			} catch (BundleException e) {
				out.print("Install: " + path + ": ");
				return report(e, out);
			} finally {
				in.close();
			}
		}

		// From now on, the bundles are on their own. They have
		// by default AllPermission, but if they install bundles
		// they will not automatically get AllPermission anymore

		// Get the resolved status
		if (padmin != null && padmin.resolveBundles(null) == false)
			return RESOLVE_ERROR;

		if (security)
			policy.setDefaultPermissions(null);

		if (report) {
			report(System.out);
			System.out.flush();
		}

		// Now start all the installed bundles in the same order
		// (unless they're a fragment)

		for (Iterator<Bundle> i = installed.iterator(); i.hasNext();) {
			Bundle b = (Bundle) i.next();
			try {
				if (!isFragment(b))
					b.start();
			} catch (BundleException e) {
				out.print("Start: " + b.getBundleId() + ": ");
				return report(e, out);
			}
		}

		// Start embedded activators
		String activators = systemContext.getProperty(LauncherConstants.LAUNCH_ACTIVATORS);
		if (activators != null) {
			StringTokenizer st = new StringTokenizer(activators, " ,");
			ClassLoader loader = getClass().getClassLoader();
			while (st.hasMoreElements()) {
				String token = st.nextToken();
				try {
					Class<?> clazz = loader.loadClass(token);
					BundleActivator activator = (BundleActivator) clazz.newInstance();
					embedded.add(activator);
				} catch (Exception e) {
					throw new IllegalArgumentException("Embedded Bundle Activator incorrect: "
							+ token + ", " + e);
				}
			}
		}
		for (BundleActivator activator : embedded)
			try {
				activator.start(systemContext);
			} catch (Exception e) {
				e.printStackTrace();
				return ERROR;
			}

		return OK;
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
					FrameworkEvent result = systemBundle.waitForStop(timeout);
					switch (result.getType()) {
					case FrameworkEvent.STOPPED:
						System.exit(OK);
						break;
					case FrameworkEvent.WAIT_TIMEDOUT:
						System.exit(TIMEDOUT);
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
			if (runbundles.contains(f)) {
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
			systemBundle.waitForStop(timeout);
		}
	}

	public void addSystemPackage(String packageName) {
		systemPackages = concat(systemPackages, packageName);
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
		if (storage != null)
			workingdir = new File(storage).getAbsoluteFile();

		if (!keep) {
			delete(workingdir);
			p.setProperty(Constants.FRAMEWORK_STORAGE_CLEAN, "true");
		}

		workingdir.mkdirs();
		if (!workingdir.isDirectory())
			throw new IllegalArgumentException("Cannot create a working dir: " + workingdir);

		p.setProperty(Constants.FRAMEWORK_STORAGE, workingdir.getAbsolutePath());

		if (systemPackages != null)
			p.setProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, systemPackages);

		Framework systemBundle;

		if (services) {

			// 3) framework = null, lookup in META-INF/services

			ClassLoader loader = getClass().getClassLoader();

			// 3) Lookup in META-INF/services
			List<String> implementations = getMetaInfServices(loader, FrameworkFactory.class
					.getName());

			if (implementations.size() == 0)
				out.println("Found no fw implementation");
			if (implementations.size() > 1)
				out.println("Found more than one framework implementations: " + implementations);

			String implementation = (String) implementations.get(0);

			Class<?> clazz = loader.loadClass(implementation);
			FrameworkFactory factory = (FrameworkFactory) clazz.newInstance();
			systemBundle = factory.newFramework(p);
			systemBundle.init();
		} else {
			// we have to use our own dummy framework
			systemBundle = new MiniFramework(p);
			systemBundle.init();
		}
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
			InputStream in = url.openStream();
			try {
				BufferedReader rdr = new BufferedReader(new InputStreamReader(in));
				String line;
				while ((line = rdr.readLine()) != null) {
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
		runbundles.add(resource.getAbsolutePath());
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
			row(out, "Storage", storage);
			row(out, "Keep", keep);
			row(out, "Report", report);
			row(out, "Security", security);
			row(out, "System Packages", systemPackages);
			list(out, "Classpath", split(System.getProperty("java.class.path"), File.pathSeparator));
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
						out.println(bundles[i].getLocation());
					}
				}
			}
		} catch (Throwable t) {
			out.println("Sorry, can't print framework: " + t);
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

	public int report(BundleException e, PrintStream out) {
		switch (e.getType()) {
		case BundleException.ACTIVATOR_ERROR:
			out.println("Caused by activator: ");
			e.getCause().printStackTrace(out);
			return LauncherConstants.ACTIVATOR_ERROR;

		case BundleException.DUPLICATE_BUNDLE_ERROR:
			out.print("Duplicate bundles: " + e);
			return LauncherConstants.DUPLICATE_BUNDLE;

		case BundleException.RESOLVE_ERROR:
			out.print("Resolve error: " + e);
			return LauncherConstants.RESOLVE_ERROR;

		case BundleException.INVALID_OPERATION:
		case BundleException.MANIFEST_ERROR:
		case BundleException.NATIVECODE_ERROR:
		case BundleException.STATECHANGE_ERROR:
		case BundleException.UNSUPPORTED_OPERATION:
		case BundleException.UNSPECIFIED:
		default:
			out.println(e);
			return ERROR;
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
}

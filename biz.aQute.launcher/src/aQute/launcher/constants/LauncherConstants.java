package aQute.launcher.constants;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import aQute.bnd.osgi.Constants;

public class LauncherConstants {

	public final static String		LAUNCHER_PROPERTIES			= "launcher.properties";
	public final static String		DEFAULT_LAUNCHER_PROPERTIES	= "launcher.properties";
	public final static String		LAUNCHER_ARGUMENTS			= "launcher.arguments";
	public final static String		LAUNCHER_READY				= "launcher.ready";

	// MUST BE ALIGNED WITH ProjectLauncher! Do not want to create coupling
	// so cannot refer.
	public final static int			OK							= 0;
	public final static int			WARNING						= 126 - 1;
	public final static int			ERROR						= 126 - 2;
	public final static int			TIMEDOUT					= 126 - 3;
	public final static int			UPDATE_NEEDED				= 126 - 4;
	public final static int			CANCELED					= 126 - 5;
	public final static int			DUPLICATE_BUNDLE			= 126 - 6;
	public final static int			RESOLVE_ERROR				= 126 - 7;
	public final static int			ACTIVATOR_ERROR				= 126 - 8;
	public static final int			STOPPED						= 126 - 9;
	public static final int			RETURN_INSTEAD_OF_EXIT		= 197;

	// Local names
	final static String				LAUNCH_SERVICES				= "launch.services";
	final static String				LAUNCH_STORAGE_DIR			= "launch.storage.dir";
	final static String				LAUNCH_KEEP					= "launch.keep";
	final static String				LAUNCH_RUNBUNDLES			= "launch.bundles";
	final static String				LAUNCH_SYSTEMPACKAGES		= "launch.system.packages";
	final static String				LAUNCH_SYSTEMCAPABILITIES	= "launch.system.capabilities";
	final static String				LAUNCH_TIMEOUT				= "launch.timeout";
	final static String				LAUNCH_EMBEDDED				= "launch.embedded";
	final static String				LAUNCH_NAME					= "launch.name";
	final static String				LAUNCH_NOREFERENCES			= "launch.noreferences";
	final static String				LAUNCH_NOTIFICATION_PORT	= "launch.notificationPort";

	public final static String[]	LAUNCHER_PROPERTY_KEYS		= {
		LAUNCH_SERVICES, LAUNCH_STORAGE_DIR, LAUNCH_KEEP, LAUNCH_NOREFERENCES, LAUNCH_RUNBUNDLES, LAUNCH_SYSTEMPACKAGES,
		LAUNCH_SYSTEMCAPABILITIES, LAUNCH_SYSTEMPACKAGES, Constants.LAUNCH_TRACE, LAUNCH_TIMEOUT,
		Constants.LAUNCH_ACTIVATORS,
		LAUNCH_EMBEDDED, LAUNCH_NAME, LAUNCH_NOREFERENCES, LAUNCH_NOTIFICATION_PORT, Constants.LAUNCH_ACTIVATION_EAGER
	};
	/**
	 * The command line arguments of the launcher. Launcher are not supposed to
	 * eat any arguments, they should use -D VM arguments so that applications
	 * can leverage the command line. The launcher must register itself as a
	 * service under its impl. class with this property set to a String[].
	 */

	public boolean					services;
	public boolean					noreferences;
	public File						storageDir;
	public boolean					keep;
	public final List<String>		runbundles					= new ArrayList<>();
	public String					systemPackages;
	public String					systemCapabilities;
	public boolean					trace;
	public long						timeout;
	public final List<String>		activators					= new ArrayList<>();
	public Map<String, String>		runProperties				= new HashMap<>();
	public boolean					embedded					= false;
	public String					name;
	public int						notificationPort			= -1;
	public boolean					activationEager				= false;

	/**
	 * Translate a constants to properties.
	 */
	public <P extends Properties> P getProperties(P p) {
		p.setProperty(LAUNCH_NOREFERENCES, noreferences + "");
		p.setProperty(LAUNCH_SERVICES, services + "");
		if (storageDir != null)
			p.setProperty(LAUNCH_STORAGE_DIR, storageDir.getAbsolutePath());
		p.setProperty(LAUNCH_KEEP, keep + "");

		p.setProperty(LAUNCH_RUNBUNDLES, join(runbundles, ","));

		if (systemPackages != null)
			p.setProperty(LAUNCH_SYSTEMPACKAGES, systemPackages + "");
		if (systemCapabilities != null)
			p.setProperty(LAUNCH_SYSTEMCAPABILITIES, systemCapabilities + "");
		p.setProperty(Constants.LAUNCH_TRACE, trace + "");
		p.setProperty(LAUNCH_TIMEOUT, timeout + "");
		p.setProperty(Constants.LAUNCH_ACTIVATORS, join(activators, ","));
		p.setProperty(LAUNCH_EMBEDDED, embedded + "");

		if (name != null)
			p.setProperty(LAUNCH_NAME, name);

		p.setProperty(LAUNCH_NOTIFICATION_PORT, String.valueOf(notificationPort));
		p.setProperty(Constants.LAUNCH_ACTIVATION_EAGER, activationEager + "");

		for (Map.Entry<String, String> entry : runProperties.entrySet()) {
			if (entry.getValue() == null) {
				if (entry.getKey() != null)
					p.remove(entry.getKey());
			} else {
				p.setProperty(entry.getKey(), entry.getValue());
			}

		}
		return p;
	}

	/**
	 * Empty constructor for the plugin
	 */

	public LauncherConstants() {}

	/**
	 * Create a constants from properties.
	 *
	 * @param p
	 */
	public LauncherConstants(Properties p) {
		services = Boolean.valueOf(p.getProperty(LAUNCH_SERVICES));
		if (p.getProperty(LAUNCH_STORAGE_DIR) != null)
			storageDir = new File(p.getProperty(LAUNCH_STORAGE_DIR));
		noreferences = Boolean.valueOf(p.getProperty(LAUNCH_NOREFERENCES));
		keep = Boolean.valueOf(p.getProperty(LAUNCH_KEEP));
		runbundles.addAll(split(p.getProperty(LAUNCH_RUNBUNDLES), ","));

		systemPackages = p.getProperty(LAUNCH_SYSTEMPACKAGES);
		systemCapabilities = p.getProperty(LAUNCH_SYSTEMCAPABILITIES);
		trace = Boolean.valueOf(p.getProperty(Constants.LAUNCH_TRACE));
		timeout = Long.parseLong(p.getProperty(LAUNCH_TIMEOUT));
		activators.addAll(split(p.getProperty(Constants.LAUNCH_ACTIVATORS), " ,"));
		String s = p.getProperty(LAUNCH_EMBEDDED);
		embedded = s != null && Boolean.parseBoolean(s);
		name = p.getProperty(LAUNCH_NAME);
		notificationPort = Integer.valueOf(p.getProperty(LAUNCH_NOTIFICATION_PORT, "-1"));
		activationEager = Boolean.valueOf(p.getProperty(Constants.LAUNCH_ACTIVATION_EAGER));
		@SuppressWarnings({
			"unchecked", "rawtypes"
		})
		Map<String, String> map = (Map) p;
		runProperties.putAll(map);
	}

	private Collection<String> split(String property, String string) {
		List<String> result = new ArrayList<>();
		StringTokenizer st = new StringTokenizer(property, string);
		while (st.hasMoreTokens()) {
			result.add(st.nextToken());
		}

		return result;
	}

	private static String join(List<?> runbundles2, String string) {
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (Object r : runbundles2) {
			sb.append(del);
			sb.append(r);
			del = string;
		}
		return sb.toString();
	}
}

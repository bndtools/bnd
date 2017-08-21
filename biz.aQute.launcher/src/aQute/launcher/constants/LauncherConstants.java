package aQute.launcher.constants;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

public class LauncherConstants {

	public final static String		LAUNCHER_PROPERTIES			= "launcher.properties";
	public final static String		DEFAULT_LAUNCHER_PROPERTIES	= "launcher.properties";
	public final static String		LAUNCHER_ARGUMENTS			= "launcher.arguments";
	public final static String		LAUNCHER_READY				= "launcher.ready";

	// MUST BE ALIGNED WITH ProjectLauncher! Donot want to create coupling
	// so cannot refer.
	public final static int			OK							= 0;
	public final static int			ERROR						= -2;
	public final static int			WARNING						= -1;
	public final static int			TIMEDOUT					= -3;
	public final static int			UPDATE_NEEDED				= -4;
	public final static int			CANCELED					= -5;
	public final static int			DUPLICATE_BUNDLE			= -6;
	public final static int			RESOLVE_ERROR				= -7;
	public final static int			ACTIVATOR_ERROR				= -8;
	public static final int			STOPPED						= -9;
	public static final int			RETURN_INSTEAD_OF_EXIT		= 197;

	// Start custom errors from here
	public final static int			CUSTOM_LAUNCHER				= -128;

	// Local names
	final static String				LAUNCH_SERVICES				= "launch.services";
	final static String				LAUNCH_STORAGE_DIR			= "launch.storage.dir";
	final static String				LAUNCH_KEEP					= "launch.keep";
	final static String				LAUNCH_RUNBUNDLES			= "launch.bundles";
	final static String				LAUNCH_SYSTEMPACKAGES		= "launch.system.packages";
	final static String				LAUNCH_SYSTEMCAPABILITIES	= "launch.system.capabilities";
	final static String				LAUNCH_TRACE				= "launch.trace";
	final static String				LAUNCH_TIMEOUT				= "launch.timeout";
	final static String				LAUNCH_ACTIVATORS			= "launch.activators";
	final static String				LAUNCH_EMBEDDED				= "launch.embedded";
	final static String				LAUNCH_NAME					= "launch.name";
	final static String				LAUNCH_NOREFERENCES			= "launch.noreferences";
	final static String				LAUNCH_NOTIFICATION_PORT	= "launch.notificationPort";

	public final static String[]	LAUNCHER_PROPERTY_KEYS		= {
			LAUNCH_SERVICES, LAUNCH_STORAGE_DIR, LAUNCH_KEEP, LAUNCH_NOREFERENCES, LAUNCH_RUNBUNDLES,
			LAUNCH_SYSTEMPACKAGES, LAUNCH_SYSTEMCAPABILITIES, LAUNCH_SYSTEMPACKAGES, LAUNCH_TRACE, LAUNCH_TIMEOUT,
			LAUNCH_ACTIVATORS, LAUNCH_EMBEDDED, LAUNCH_NAME, LAUNCH_NOREFERENCES, LAUNCH_NOTIFICATION_PORT
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
	public final List<String>		runbundles					= new ArrayList<String>();
	public String					systemPackages;
	public String					systemCapabilities;
	public boolean					trace;
	public long						timeout;
	public final List<String>		activators					= new ArrayList<String>();
	public Map<String,String>		runProperties				= new HashMap<String,String>();
	public boolean					embedded					= false;
	public String					name;
	public int						notificationPort			= -1;

	/**
	 * Translate a constants to properties.
	 */
	public Properties getProperties(Properties p) {
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
		p.setProperty(LAUNCH_TRACE, trace + "");
		p.setProperty(LAUNCH_TIMEOUT, timeout + "");
		p.setProperty(LAUNCH_ACTIVATORS, join(activators, ","));
		p.setProperty(LAUNCH_EMBEDDED, embedded + "");
		if (name != null)
			p.setProperty(LAUNCH_NAME, name);

		p.setProperty(LAUNCH_NOTIFICATION_PORT, String.valueOf(notificationPort));

		for (Map.Entry<String,String> entry : runProperties.entrySet()) {
			if (entry.getValue() == null) {
				if (entry.getKey() != null)
					p.remove(entry.getKey());
			} else {
				p.put(entry.getKey(), entry.getValue());
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
		trace = Boolean.valueOf(p.getProperty(LAUNCH_TRACE));
		timeout = Long.parseLong(p.getProperty(LAUNCH_TIMEOUT));
		activators.addAll(split(p.getProperty(LAUNCH_ACTIVATORS), " ,"));
		String s = p.getProperty(LAUNCH_EMBEDDED);
		embedded = s != null && Boolean.parseBoolean(s);
		name = p.getProperty(LAUNCH_NAME);
		notificationPort = Integer.valueOf(p.getProperty(LAUNCH_NOTIFICATION_PORT, "-1"));
		@SuppressWarnings({
				"unchecked", "rawtypes"
		})
		Map<String,String> map = (Map) p;
		runProperties.putAll(map);
	}

	private Collection< ? extends String> split(String property, String string) {
		List<String> result = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(property, string);
		while (st.hasMoreTokens()) {
			result.add(st.nextToken());
		}

		return result;
	}

	private static String join(List< ? > runbundles2, String string) {
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

package aQute.launcher.constants;

import java.io.*;
import java.util.*;

public class LauncherConstants {

	public final static String	LAUNCHER_PROPERTIES		= "launcher.properties";
	public final static String	LAUNCHER_ARGUMENTS		= "launcher.arguments";
	public final static String	LAUNCHER_READY			= "launcher.ready";

	// MUST BE ALIGNED WITH ProjectLauncher! Donot want to create coupling
	// so cannot refer.
	public final static int		OK						= 0;
	public final static int		ERROR					= -2;
	public final static int		WARNING					= -1;
	public final static int		TIMEDOUT				= -3;
	public final static int		UPDATE_NEEDED			= -4;
	public final static int		CANCELED				= -5;
	public final static int		DUPLICATE_BUNDLE		= -6;
	public final static int		RESOLVE_ERROR			= -7;
	public final static int		ACTIVATOR_ERROR			= -8;
	// Start custom errors from here
	public final static int		CUSTOM_LAUNCHER			= -128;

	// Local names
	final static String			LAUNCH_SERVICES			= "launch.services";
	final static String			LAUNCH_STORAGE_DIR		= "launch.storage.dir";
	final static String			LAUNCH_KEEP				= "launch.keep";
	final static String			LAUNCH_RUNBUNDLES		= "launch.bundles";
	final static String			LAUNCH_SYSTEMPACKAGES	= "launch.system.packages";
	final static String			LAUNCH_TRACE			= "launch.trace";
	final static String			LAUNCH_TIMEOUT			= "launch.timeout";
	final static String			LAUNCH_ACTIVATORS		= "launch.activators";

	/**
	 * The command line arguments of the launcher. Launcher are not supposed to
	 * eat any arguments, they should use -D VM arguments so that applications
	 * can leverage the command line. The launcher must register itself as a
	 * service under its impl. class with this property set to a String[].
	 */

	public boolean				services;
	public File					storageDir				= new File("");
	public boolean				keep;
	public final List<String>	runbundles				= new ArrayList<String>();
	public String				systemPackages;
	public boolean				trace;
	public long					timeout;
	public final List<String>	activators				= new ArrayList<String>();
	public Map<String, String>	runProperties			= new HashMap<String, String>();

	/**
	 * Translate a constants to properties.
	 * 
	 * @return
	 */
	public Properties getProperties() {
		Properties p = new Properties();
		p.setProperty(LAUNCH_SERVICES, services + "");
		p.setProperty(LAUNCH_STORAGE_DIR, storageDir.getAbsolutePath());
		p.setProperty(LAUNCH_KEEP, keep + "");
		p.setProperty(LAUNCH_RUNBUNDLES, join(runbundles, ","));
		if (systemPackages != null)
			p.setProperty(LAUNCH_SYSTEMPACKAGES, systemPackages + "");
		p.setProperty(LAUNCH_TRACE, trace + "");
		p.setProperty(LAUNCH_TIMEOUT, timeout + "");
		p.setProperty(LAUNCH_ACTIVATORS, join(activators, ","));

		for (Map.Entry<String, String> entry : runProperties.entrySet()) {
			if (entry.getValue() == null) {
				if (entry.getKey() != null) p.remove(entry.getKey());
			} else {
				p.put(entry.getKey(), entry.getValue());
			}

		}
		return p;
	}

	/**
	 * Empty constructor for the plugin
	 */

	public LauncherConstants() {
	}

	/**
	 * Create a constants from properties.
	 * 
	 * @param p
	 */
	public LauncherConstants(Properties p) {
		services = Boolean.valueOf(p.getProperty(LAUNCH_SERVICES));
		storageDir = new File(p.getProperty(LAUNCH_STORAGE_DIR));
		keep = Boolean.valueOf(p.getProperty(LAUNCH_KEEP));
		runbundles.addAll(split(p.getProperty(LAUNCH_RUNBUNDLES), ","));
		systemPackages = p.getProperty(LAUNCH_SYSTEMPACKAGES);
		trace = Boolean.valueOf(p.getProperty(LAUNCH_TRACE));
		timeout = Long.parseLong(p.getProperty(LAUNCH_TIMEOUT));
		activators.addAll(split(p.getProperty(LAUNCH_ACTIVATORS), " ,"));
		Map<String, String> map = (Map) p;
		runProperties.putAll(map);
	}

	private Collection<? extends String> split(String property, String string) {
		List<String> result = new ArrayList<String>();
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

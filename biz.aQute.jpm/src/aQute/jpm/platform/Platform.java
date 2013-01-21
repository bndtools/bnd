package aQute.jpm.platform;

import java.io.*;
import java.util.*;

import aQute.jpm.lib.*;
import aQute.service.reporter.*;

public abstract class Platform {
	static Platform	platform;
	static Runtime	runtime	= Runtime.getRuntime();
	Reporter		reporter;
	
	/**
	 * Get the current platform manager.
	 * 
	 * @param reporter
	 * @param jpmx
	 * @return
	 */
	public static Platform getPlatform(Reporter reporter) {

		if (platform == null) {

			String osName = System.getProperty("os.name").toLowerCase();
			reporter.trace("os.name=%s", osName);
			if (osName.startsWith("windows"))
				platform = new Windows();
			else if (osName.startsWith("mac") || osName.startsWith("darwin"))
				platform = new MacOS();
			else
				platform = new Linux();
			platform.reporter = reporter;
			reporter.trace("platform=%s", platform.reporter);
		}
		return platform;
	}

	public abstract File getGlobal();

	public abstract File getLocal();

	abstract public void shell(String initial) throws Exception;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Formatter formatter = new Formatter(sb);
		formatter.format("Name                %s%n", getName());
		formatter.format("Local               %s%n", getLocal());
		formatter.format("Global              %s%n", getGlobal());
		return sb.toString();
	}

	abstract public String getName();

	abstract public void uninstall() throws IOException;

	public int run(String args) throws Exception {
		return runtime.exec(args).waitFor();
	}

	public abstract String createCommand(CommandData data, String ... deps) throws Exception;

	public abstract String createService(ServiceData data) throws Exception;

	public abstract String remove(CommandData data) throws Exception;

	public abstract String remove(ServiceData data) throws Exception;

	public abstract int launchService(ServiceData data) throws Exception;
	
	public abstract void installDaemon(boolean user) throws Exception;
	public abstract void uninstallDaemon(boolean user) throws Exception;
	public abstract void chown(String user, boolean recursive, File file) throws Exception;
	public abstract String user() throws Exception;

	public abstract  void deleteCommand(CommandData cmd) throws Exception ;

	public String defaultCacertsPassword() { return "changeme"; }
}

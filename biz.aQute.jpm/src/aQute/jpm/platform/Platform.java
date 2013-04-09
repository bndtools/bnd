package aQute.jpm.platform;

import static aQute.lib.io.IO.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import aQute.jpm.lib.*;
import aQute.lib.collections.*;
import aQute.libg.sed.*;
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

	/**
	 * Global homeDir
	 * @return
	 */
	public abstract File getGlobal();

	/**
	 * Local homeDir
	 * @return
	 */
	public abstract File getLocal();

	abstract public void shell(String initial) throws Exception;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Formatter formatter = new Formatter(sb);
		try {
			formatter.format("Name                %s%n", getName());
			formatter.format("Local               %s%n", getLocal());
			formatter.format("Global              %s%n", getGlobal());
			return formatter.toString();
		}
		finally {
			formatter.close();
		}
	}

	abstract public String getName();

	abstract public void uninstall() throws IOException;

	public int run(String args) throws Exception {
		return runtime.exec(args).waitFor();
	}

	public abstract String createCommand(CommandData data, Map<String,String> map, String... deps) throws Exception;

	public abstract String createService(ServiceData data, Map<String,String> map, String... deps) throws Exception;

	public abstract String remove(CommandData data) throws Exception;

	public abstract String remove(ServiceData data) throws Exception;

	public abstract int launchService(ServiceData data) throws Exception;

	public abstract void installDaemon(boolean user) throws Exception;

	public abstract void uninstallDaemon(boolean user) throws Exception;

	public abstract void chown(String user, boolean recursive, File file) throws Exception;

	public abstract String user() throws Exception;

	public abstract void deleteCommand(CommandData cmd) throws Exception;

	public String defaultCacertsPassword() {
		return "changeme";
	}

	protected void process(String resource, CommandData data, String path, Map<String,String> map, String... extra)
			throws Exception {
		File file = new File(path);
		copy(getClass().getResourceAsStream(resource), file);
		Sed sed = new Sed(file);
		sed.setBackup(false);
		if (data.title == null || data.title.trim().length() == 0)
			data.title = data.name;

		for (Field key : data.getClass().getFields()) {
			Object value = key.get(data);
			if (value == null) {
				value = "";
			}

			//pl: this is fishy -> epiplog -> epilog + this seems to correspond to a service, not a command
			
			// We want to enclose the prolog and epilog so they are
			// executed as one command and thus logged as one command
			if ("epiplog".equals(key.getName()) || "prolog".equals(key.getName())) {
				String s = (String) value;
				if (s != null && s.trim().length() > 0) {
					value = "(" + s + ")";
				}
			}
			String v = "" + value;
			v = v.replace("\\", "\\\\");
			sed.replace("%" + key.getName() + "%", v);
		}
		ExtList<String> deps = new ExtList<String>(data.dependencies);
		for (String x : extra) {
			deps.add(x);
		}
		String classpath = deps.join(File.pathSeparator);
		sed.replace("%classpath%", classpath.replace("\\", "\\\\"));

		if (map != null) {
			StringBuilder sb = new StringBuilder();
			String del = "-D";
			for (Map.Entry<String,String> e : map.entrySet()) {
				reporter.trace("define %s=%s", e.getKey(), e.getValue());
				sed.replace("%" + e.getKey() + "%", e.getValue());
				sb.append(del).append(e.getKey()).append("=\"").append(e.getValue()).append("\"");
				del = " -D";
			}
			sed.replace("%defines%", sb.toString());
		} else {
			sed.replace("%defines%", "");
		}
		sed.doIt();
	}

	protected String[] add(String[] extra, String... more) {
		if (extra == null || extra.length == 0)
			return more;

		if (more == null || more.length == 0)
			return extra;

		String[] result = new String[extra.length + more.length];
		System.arraycopy(extra, 0, result, 0, extra.length);
		System.arraycopy(more, 0, result, extra.length, more.length);
		return result;
	}

	public abstract void report(Formatter out) throws IOException, Exception;

}
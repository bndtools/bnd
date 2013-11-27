package aQute.jpm.platform;

import static aQute.lib.io.IO.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import aQute.jpm.lib.*;
import aQute.lib.collections.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.libg.reporter.*;
import aQute.libg.sed.*;
import aQute.service.reporter.*;

public abstract class Platform {
	public enum Type {
		UNKNOWN, WINDOWS, LINUX, MACOS
	};

	static Platform				platform;
	static Runtime				runtime	= Runtime.getRuntime();
	Reporter					reporter;
	JustAnotherPackageManager	jpm;

	/**
	 * Get the current platform manager.
	 * 
	 * @param reporter
	 * @param jpmx
	 * @return
	 */
	public static Platform getPlatform(Reporter reporter, Type type) {
		if (platform == null) {
			if (type == null)
				type = getPlatformType();

			switch (type) {
				case LINUX :
					platform = new Linux();
					break;
				case MACOS :
					platform = new MacOS();
					break;
				case WINDOWS :
					platform = new Windows();
					break;
				default :
					return null;
			}
		}
		platform.reporter = reporter;
		return platform;
	}

	public static Type getPlatformType() {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.startsWith("windows"))
			return Type.WINDOWS;
		else if (osName.startsWith("mac") || osName.startsWith("darwin")) {
			return Type.MACOS;
		} else if (osName.contains("linux"))
			return Type.LINUX;

		return null;
	}

	/**
	 * Return the place where we place the jpm home directory for global access.
	 * E.g. /var/jpm
	 * 
	 * @return
	 * @throws Exception
	 */
	public abstract File getGlobal();

	/**
	 * Return the place where we place the jpm home directory for user/local
	 * access. E.g. ~/.jpm
	 * 
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

	public abstract String createCommand(CommandData data, Map<String,String> map, boolean force, String... deps)
			throws Exception;

	public abstract String createService(ServiceData data, Map<String,String> map, boolean force, String... deps)
			throws Exception;

	public abstract String deleteService(ServiceData data) throws Exception;

	public abstract int launchService(ServiceData data) throws Exception;

	public abstract void installDaemon(boolean user) throws Exception;

	public abstract void uninstallDaemon(boolean user) throws Exception;

	public abstract void chown(String user, boolean recursive, File file) throws Exception;

	public abstract String user() throws Exception;

	public abstract void deleteCommand(CommandData cmd) throws Exception;

	/**
	 * Return the directory on this platform were normally executables are
	 * installed.
	 */
	public abstract File getGlobalBinDir();

	public File getInitd(ServiceData data) {
		return null;
	}

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

		//
		// Allow commands to be done in java or javaw
		//
		sed.replace("%java%", data.windows ? "javaw" : "java");

		for (Field key : data.getClass().getFields()) {
			Object value = key.get(data);
			if (value == null) {
				value = "";
			}

			// We want to enclose the prolog and epilog so they are
			// executed as one command and thus logged as one command
			if ("epilog".equals(key.getName()) || "prolog".equals(key.getName())) {
				String s = (String) value;
				if (s != null && s.trim().length() > 0) {
					value = "(" + s + ")";
				}
			}
			String v = "" + value;
			v = v.replace("\\", "\\\\");
			sed.replace("%" + key.getName() + "%", v);
		}

		ExtList<String> deps = new ExtList<String>();
		for (byte[] dependency : data.dependencies) {
			ArtifactData d = jpm.get(dependency);
			deps.add(d.file);
		}
		for (String x : extra) {
			deps.add(x);
		}
		String classpath = deps.join(File.pathSeparator);
		sed.replace("%classpath%", classpath.replace("\\", "\\\\"));

		if (map != null) {
			StringBuilder sb = new StringBuilder();
			String del = "-D";
			for (Map.Entry<String,String> e : map.entrySet()) {
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

	public String installCompletion(Object target) throws Exception {
		return "No completion available for this platform";
	}

	public String getConfigFile() throws Exception {
		return "~/.jpm/settings.json";
	}

	public void parseCompletion(Object target, File f) throws Exception {
		IO.copy(getClass().getResource("unix/jpm-completion.bash"), f);

		Sed sed = new Sed(f);
		sed.setBackup(false);

		Reporter r = new ReporterAdapter();
		CommandLine c = new CommandLine(r);
		Map<String,Method> commands = c.getCommands(target);
		StringBuilder sb = new StringBuilder();
		for (String commandName : commands.keySet()) {
			sb.append(" " + commandName);
		}
		sb.append(" help");

		sed.replace("%listJpmCommands%", sb.toString().substring(1));
		sed.doIt();
	}

	public void parseCompletion(Object target, PrintStream out) throws Exception {
		File tmp = File.createTempFile("jpm-completion", ".tmp");
		tmp.deleteOnExit();

		try {
			parseCompletion(target, tmp);
			IO.copy(tmp, out);
		}
		finally {
			tmp.delete();
		}
	}

	/**
	 * Is called to initialize the platform if necessary.
	 * 
	 * @throws IOException
	 * @throws Exception
	 */
	public void init() throws Exception {
		// can be overridden by the subclasses
	}

	public void setJpm(JustAnotherPackageManager jpm) {
		this.jpm = jpm;
	}

	public boolean hasPost() {
		return false;
	}

	public void doPostInstall() {
		// do nothing
	}

}
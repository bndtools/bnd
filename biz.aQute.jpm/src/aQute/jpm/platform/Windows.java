package aQute.jpm.platform;

/**
 * http://support.microsoft.com/kb/814596
 */
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.boris.winrun4j.*;

import aQute.jpm.lib.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.lib.strings.*;

public class Windows extends Platform {
	static boolean	IS64	= System.getProperty("os.arch").contains("64");

	static File		javahome;
	private File	misc;

	@Override
	public File getGlobal() {
		String sysdrive = System.getenv("SYSTEMDRIVE");
		if (sysdrive == null)
			sysdrive = "c:";

		return IO.getFile(sysdrive + "\\JPM4J");
	}

	@Override
	public File getLocal() {
		return IO.getFile("~/.jpm/windows");
	}

	@Override
	public File getGlobalBinDir() {
		return new File(getGlobal() + "\\bin");
	}

	@Override
	public void shell(String initial) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		return "Windows";
	}

	/**
	 * The uninstaller should be used
	 */
	@Override
	public void uninstall() throws IOException {}

	@Override
	public String createCommand(CommandData data, Map<String,String> map, boolean force, String... extra)
			throws Exception {
		data.bin = getExecutable(data);
		File f = new File(data.bin);

		if (!force && f.exists())
			return "Command already exists " + data.bin + ", try to use --force";

		if (data.windows)
			IO.copy(new File(getMisc(), "winrun4j.exe"), f);
		else
			IO.copy(new File(getMisc(), "winrun4jc.exe"), f);

		File ini = new File(f.getAbsolutePath().replaceAll("\\.exe$", ".ini"));
		PrintWriter pw = new PrintWriter(ini);
		try {
			pw.printf("main.class=%s%n", data.main);
			pw.printf("log.level=error%n");
			String del = "classpath.1=";

			for (byte[] dependency : data.dependencies) {
				ArtifactData d = jpm.get(dependency);
				pw.printf("%s%s", del, d.file);
				del = ",";
			}
			if (data.jvmArgs != null && data.jvmArgs.length() != 0) {
				String parts[] = data.jvmArgs.split("\\s+");
				for (int i = 0; i < parts.length; i++)
					pw.printf("vmarg.%d=%s%n", i, data.jvmArgs);
			}
		}
		finally {
			pw.close();
		}
		reporter.trace("Ini content %s", IO.collect(ini));
		return null;
	}

	private File getMisc() {
		if (misc == null) {
			misc = new File(jpm.getHomeDir(), "misc");
		}
		return misc;
	}

	protected String getExecutable(CommandData data) {
		return new File(jpm.getBinDir(), data.name + ".exe").getAbsolutePath();
	}

	@Override
	public String createService(ServiceData data, Map<String,String> map, boolean force, String... extra)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String remove(CommandData data) throws Exception {
		File f = new File(data.bin);
		if (f.isFile() && !f.delete())
			return "Could not delete " + data.bin;
		return null;
	}

	@Override
	public String remove(ServiceData data) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int launchService(ServiceData data) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void installDaemon(boolean user) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void uninstallDaemon(boolean user) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void chown(String user, boolean recursive, File file) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public String user() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteCommand(CommandData cmd) throws Exception {
		String executable = getExecutable(cmd);
		File f = new File(executable);
		File fj = new File(executable + ".ini");
		if (cmd.name.equals("jpm")) {
			reporter.trace("leaving jpm behind");
			return;
		} else {
			IO.deleteWithException(f);
			IO.deleteWithException(fj);
		}
	}

	@Override
	public String toString() {
		try {
			return "Windows";
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Provide as much detail about the jpm environment as possible.
	 * 
	 * @throws IOException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */

	public void report(Formatter f) throws Exception {}

	/**
	 * Initialize the directories for windows.
	 * 
	 * @throws IOException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */

	public void init() throws Exception {
		if (!getMisc().isDirectory() && !getMisc().mkdirs())
			throw new IOException("Cannot create directory " + getMisc());
		if (IS64) {
			IO.copy(getClass().getResourceAsStream("windows/winrun4jc64.exe"), new File(getMisc(), "winrun4jc.exe"));
			IO.copy(getClass().getResourceAsStream("windows/winrun4j64.exe"), new File(getMisc(), "winrun4j.exe"));
			// IO.copy(getClass().getResourceAsStream("windows/sjpm64.exe"), new
			// File(getMisc(), "sjpm.exe"));
		} else {
			IO.copy(getClass().getResourceAsStream("windows/winrun4j.exe"), new File(getMisc(), "winrun4j.exe"));
			IO.copy(getClass().getResourceAsStream("windows/winrun4jc.exe"), new File(getMisc(), "winrun4jc.exe"));
			// IO.copy(getClass().getResourceAsStream("windows/winrun4j.exe"),
			// new File(getMisc(), "sjpm.exe"));
		}
	}

	@Override
	public boolean hasPost() {
		return true;
	}

	@Override
	public void doPostInstall() {
		System.out.println("In post install");
	}

	/**
	 * Add the current bindir to the environment
	 */

	@Arguments(arg = {})
	interface PathOptions extends Options {
		boolean remove();

		List<String> delete();

		boolean add();

		List<String> extra();
	}

	public void _path(PathOptions options) {
		RegistryKey env = RegistryKey.HKEY_CURRENT_USER.getSubKey("Environment");
		if (env == null) {
			reporter.error("Cannot find key for environment HKEY_CURRENT_USER/Environment");
			return;
		}

		String path = env.getString("Path");
		String parts[] = path == null ? new String[0] : path.split(File.pathSeparator);
		List<String> paths = new ArrayList<String>(Arrays.asList(parts));
		boolean save = false;
		if (options.extra() != null) {
			paths.addAll(options.extra());
			save = true;
		}

		for (int i = 0; i < parts.length; i++) {
			System.out.printf("%2d:%s %s %s%n", i, parts[i].toLowerCase().contains("jpm") ? "*" : " ", new File(
					parts[i]).isDirectory() ? " " : "!", parts[i]);
		}

		if (options.remove()) {
			if (!paths.remove(jpm.getBinDir().getAbsolutePath())) {
				reporter.error("Could not find %s", jpm.getBinDir());
			}
			save = true;
		}
		if (options.delete() != null) {
			save |= paths.removeAll(options.delete());
		}
		if (options.add()) {
			paths.remove(jpm.getBinDir().getAbsolutePath());
			paths.add(jpm.getBinDir().getAbsolutePath());
			save = true;
		}
		if (save) {
			String p = Strings.join(File.pathSeparator, paths);
			env.setString("Path", p);
		}
	}

}

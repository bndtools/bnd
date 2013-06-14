package aQute.jpm.platform;

/**
 * http://support.microsoft.com/kb/814596
 */
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import aQute.jpm.lib.*;
import aQute.jpm.platform.windows.*;
import aQute.lib.io.*;

public class Windows extends Platform {

	final static File	home;
	final static File	bin;
	final static File	misc;

	static File			javahome;

	static {
		try {
			File homex = readkey("Home");
			if (homex == null) {
				homex = IO.getFile("~/.jpm");
			}
			home = homex;

			misc = new File(home, "misc");
			misc.mkdirs();
			bin = new File(home, "Bin");
			bin.mkdirs();
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	
	@Override
	public File getBinDir() {
		return bin;
	}

	@Override
	public File getGlobal() {
		return home;
	}

	private static File readkey(String key) throws Exception {
		String h = WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\JPM4j", key);
		if (h == null)
			throw new IllegalArgumentException("jpm4j is not installed. Missing registry key HKLM/Software/JPM4j/"
					+ key);
		File file = new File(h);
		file.mkdirs();
		return file.getAbsoluteFile();
	}

	@Override
	public File getLocal() {
		return IO.getFile("~/.jpm");
	}

	@Override
	public void shell(String initial) throws Exception {
		// TODO Auto-generated method stub

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
	public String createCommand(CommandData data, Map<String,String> map, boolean force, String... extra) throws Exception {
		if (data.bin == null)
			data.bin = getExecutable(data);

		File f = new File(data.bin);
		if (f.isDirectory()) {
			f = new File(data.bin, data.name + ".exe");
			data.bin = f.getAbsolutePath();
		}

		if (!force && f.exists())
			return "Command already exists " + data.bin + ", try to use --force";

		if (data.name.equals("jpm")) {
			File exe = new File(misc, "sjpm.exe");
			File fs = new File(f.getParentFile(), "sjpm.exe");
			reporter.trace("jpm! %s -> %s", exe, fs);
			IO.copy(exe, fs);
		}
		IO.copy(new File(misc, "runner.exe"), f);

		data.java = getJavaExe();
		process("windows/command.sh", data, f.getAbsolutePath() + ".jpm", map, extra);
		return null;
	}

	protected String getExecutable(CommandData data) {
		return new File(bin, data.name + ".exe").getAbsolutePath();
	}

	@Override
	public String createService(ServiceData data, Map<String,String> map, boolean force, String... extra) throws Exception {
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
		File fj = new File(executable + ".jpm");
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
			return "Window\nJava Home " + getJavaHome() + "\nJava Exe  " + getJavaExe() + "\nJPM Home  "
					+ home.getAbsolutePath();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String getJavaExe() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		return getJavaHome() + "\\bin\\java.exe";
	}

	public String getJavaHome() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (javahome == null) {
			String version = WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE,
					"Software\\Javasoft\\Java Runtime Environment", "CurrentVersion");
			if (version == null)
				throw new IllegalStateException("No Java installed? Coulnd find version of installed Java");

			String home = WinRegistry.readString(WinRegistry.HKEY_LOCAL_MACHINE,
					"Software\\Javasoft\\Java Runtime Environment\\" + version, "JavaHome");

			if (home == null)
				throw new IllegalStateException("No Java installed? Could not find JavaHome for version " + version);

			javahome = new File(home);
			if (!javahome.isDirectory())
				throw new IllegalStateException("No Java installed? Java Home could not be found: " + javahome);
		}
		return javahome.getAbsolutePath();
	}

	/**
	 * Provide as much detail about the jpm environment as possible.
	 * 
	 * @throws IOException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */

	public void report(Formatter f) throws Exception {
		f.format("Home \t0:%s\n", home);
		f.format("Java Home \t0:%s\n", getJavaHome());
		f.format("Java Exe  \t0:%s\n", getJavaExe());
	}
}

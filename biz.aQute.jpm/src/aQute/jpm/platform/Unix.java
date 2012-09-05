package aQute.jpm.platform;

import static aQute.lib.io.IO.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import aQute.jpm.lib.*;
import aQute.lib.collections.*;
import aQute.lib.io.*;
import aQute.libg.command.*;
import aQute.libg.sed.*;

public abstract class Unix extends Platform {
	
	public static String BINARIES = "/usr/bin";
	public static String JPM_GLOBAL = "/var/jpm";

	@Override
	public File getGlobal() {
		return new File("/var/jpm");
	}

	@Override
	public File getLocal() {
		File home = new File(System.getProperty("user.home"));
		return new File(home, ".jpm");
	}

	@Override
	public String createCommand(CommandData data) throws Exception {
		File executable = getExecutable(data);
		if (!data.force && executable.exists())
			return "Command already exists " + executable;

		process("unix/command.sh", data, executable);
		return null;
	}

	@Override
	public void deleteCommand(CommandData data) throws Exception {
		File executable = getExecutable(data);
		IO.deleteWithException(executable);
	}

	@Override
	public String createService(ServiceData data) throws Exception {
		File initd = getInitd(data);
		File launch = getLaunch(data);

		if (!data.force && (initd.exists() || launch.exists()))
			return "Service already exists " + initd + " & " + launch;

		process("unix/launch.sh", data, launch, data.serviceLib);
		process("unix/initd.sh", data, initd, data.serviceLib);
		return null;
	}

	private File getInitd(ServiceData data) {
		return new File("/etc/init.d/" + data.name);
	}

	protected File getLaunch(ServiceData data) {
		return new File(data.sdir, "launch.sh");
	}

	private File getExecutable(CommandData data) {
		return new File(BINARIES + "/" + data.name);
	}

	@Override
	public String remove(ServiceData data) {
		if (!getInitd(data).delete())
			return "Cannot delete " + getInitd(data);

		if (!getExecutable(data).delete())
			return "Cannot delete " + getExecutable(data);

		return null;
	}

	@Override
	public String remove(CommandData data) throws Exception {
		if (!getExecutable(data).delete())
			return "Cannot delete " + getExecutable(data);

		return null;
	}

	@Override
	public int launchService(ServiceData data) throws Exception {
		File launch = getLaunch(data);
		Process p = Runtime.getRuntime().exec(launch.getAbsolutePath(), null, new File(data.work));
		return p.waitFor();
	}

	protected void process(String resource, CommandData data, File file, String... extra) throws Exception {
		copy(getClass().getResourceAsStream(resource), file);
		Sed sed = new Sed(file);
		sed.setBackup(false);
		for (Field key : data.getClass().getFields()) {
			Object value = key.get(data);
			if (value == null) {
				value = "";
			}
			
			// We want to enclose the prolog and epilog so they are
			// executed as one command and thus logged as one command
			if ( "epiplog".equals(key.getName()) || "prolog".equals(key.getName())) {
				String s = (String) value;
				if ( s != null && s.trim().length()> 0) {
					value = "(" + s + ")";
				}
			}
			sed.replace("%" + key.getName() + "%", "" + value);
		}
		ExtList<String> deps = new ExtList<String>(data.dependencies);
		for (String x : extra) {
			deps.add(x);
		}
		String classpath = deps.join(File.pathSeparator);
		sed.replace("%classpath%", classpath);

		sed.doIt();
		run("chmod a+x " + file.getAbsolutePath());
	}

	static String	DAEMON	= "\n### JPM BEGIN ###\n" + BINARIES + "/jpm daemon >"+JPM_GLOBAL+"/daemon.log 2>>"+JPM_GLOBAL+"/daemon.log &\n### JPM END ###\n";
	static Pattern	DAEMON_PATTERN	= Pattern.compile("\n### JPM BEGIN ###\n.*\n### JPM END ###\n", Pattern.MULTILINE);

	@Override
	public void installDaemon(boolean user) throws Exception {
		if (user)
			throw new IllegalArgumentException("This Unix platform does not support user based agents");

		File rclocal = new File("/etc/rc.d/rc.local");
		if (!rclocal.isFile())
			rclocal = new File("/etc/rc.local");

		if (!rclocal.isFile())
			throw new IllegalArgumentException("Cannot find rc.local in either /etc or /etc/rc.d. Unknown unix");

		String s = IO.collect(rclocal);
		if (s.contains(DAEMON))
			return;

		s += DAEMON;
		IO.store(s, rclocal);

	}

	@Override
	public void uninstallDaemon(boolean user) throws Exception {
		if (user)
			return;

		File rclocal = new File("/etc/rc.d/rc.local");
		if (!rclocal.isFile())
			rclocal = new File("/etc/rc.local");

		if (!rclocal.isFile())
			return;

		String s = IO.collect(rclocal);
		
		Matcher m = DAEMON_PATTERN.matcher(s);
		s = m.replaceAll("");
		s += DAEMON;
		IO.store(s, rclocal);
	}

	
	@Override
	public void chown(String user, boolean recursive, File file) throws Exception {
		String cmd = "chown " + (recursive? " -R " : "") + user + " " + file.getAbsolutePath();
		if ( "root".equals(user))
			return;
		
		if ( "0".equals(user))
			return;
		
		Command chown = new Command(cmd);
		StringBuilder sb = new StringBuilder();
		
		int n = chown.execute(sb,sb);
		if ( n != 0) 
			throw new IllegalArgumentException("Changing ownership for " + file + " fails: " + n + " : " + sb);
	}
	
	@Override
	public String user() throws Exception {
		ProcessBuilder pb = new ProcessBuilder();
		Map<String,String> environment = pb.environment();
		String user = environment.get("USER");
		System.out.println(user);
		return user;
//		Command id = new Command("id -nu");
//		StringBuilder sb = new StringBuilder();
//		
//		int n = id.execute(sb,sb);
//		if ( n != 0) 
//			throw new IllegalArgumentException("Getting user id fails: " + n + " : " + sb);
//		
//		return sb.toString().trim();
	}

}

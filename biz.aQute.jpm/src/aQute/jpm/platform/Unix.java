package aQute.jpm.platform;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import aQute.jpm.lib.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.lib.strings.*;
import aQute.libg.command.*;
import aQute.libg.glob.*;
import aQute.libg.sed.*;

public abstract class Unix extends Platform {

	public static String	JPM_GLOBAL	= "/var/jpm";

	@Override
	public File getGlobal() {
		return new File("/var/jpm");
	}

	@Override
	public File getGlobalBinDir() {
		return new File("/usr/local/bin");
	}

	@Override
	public File getLocal() {
		File home = new File(System.getenv("HOME"));
		return new File(home, "jpm");
	}

	@Override
	public String createCommand(CommandData data, Map<String,String> map, boolean force, String... extra)
			throws Exception {
		data.bin = getExecutable(data);

		File f = new File(data.bin);
		if (f.isDirectory()) {
			f = new File(data.bin, data.name);
			data.bin = f.getAbsolutePath();
		}

		if (!force && f.exists())
			return "Command already exists " + data.bin;

		process("unix/command.sh", data, data.bin, map, extra);
		return null;
	}

	@Override
	public void deleteCommand(CommandData data) throws Exception {
		File executable = new File(getExecutable(data));
		IO.deleteWithException(executable);
	}

	@Override
	public String createService(ServiceData data, Map<String,String> map, boolean force, String... extra)
			throws Exception {
		File initd = getInitd(data);
		File launch = getLaunch(data);

		if (!force) {
			if (initd.exists())
				return "Service already exists in " + initd + ", use --force to override";

			if (launch.exists())
				return "Service launch file already exists in " + launch + ", use --force to override";
		}

		process("unix/launch.sh", data, launch.getAbsolutePath(), map, add(extra, data.serviceLib));
		process("unix/initd.sh", data, initd.getAbsolutePath(), map, add(extra, data.serviceLib));
		return null;
	}

	public File getInitd(ServiceData data) {
		return new File("/etc/init.d/" + data.name);
	}

	protected File getLaunch(ServiceData data) {
		return new File(data.sdir, "launch.sh");
	}

	protected String getExecutable(CommandData data) {
		return new File(jpm.getBinDir(), data.name).getAbsolutePath();
	}

	@Override
	public String deleteService(ServiceData data) {
		if (!getInitd(data).delete())
			return "Cannot delete " + getInitd(data);

		File f = new File(getExecutable(data));
		if (!f.delete())
			return "Cannot delete " + getExecutable(data);

		System.out.println("Removed service data ");

		return null;
	}

	@Override
	public int launchService(ServiceData data) throws Exception {
		File launch = getLaunch(data);
		Process p = Runtime.getRuntime().exec(launch.getAbsolutePath(), null, new File(data.work));
		return p.waitFor();
	}

	String			DAEMON			= "\n### JPM BEGIN ###\n" + "jpm daemon >" + JPM_GLOBAL + "/daemon.log 2>>"
											+ JPM_GLOBAL + "/daemon.log &\n### JPM END ###\n";
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
		String cmd = "chown " + (recursive ? " -R " : "") + user + " " + file.getAbsolutePath();
		if ("root".equals(user))
			return;

		if ("0".equals(user))
			return;

		Command chown = new Command(cmd);
		StringBuilder sb = new StringBuilder();

		int n = chown.execute(sb, sb);
		if (n != 0)
			throw new IllegalArgumentException("Changing ownership for " + file + " fails: " + n + " : " + sb);
	}

	@Override
	public String user() throws Exception {
		ProcessBuilder pb = new ProcessBuilder();
		Map<String,String> environment = pb.environment();
		String user = environment.get("USER");
		return user;
		// Command id = new Command("id -nu");
		// StringBuilder sb = new StringBuilder();
		//
		// int n = id.execute(sb,sb);
		// if ( n != 0)
		// throw new IllegalArgumentException("Getting user id fails: " + n +
		// " : " + sb);
		//
		// return sb.toString().trim();
	}

	protected void process(String resource, CommandData data, String file, Map<String,String> map, String... extra)
			throws Exception {
		super.process(resource, data, file, map, extra);
		run("chmod a+x " + file);
	}

	@Override
	public void report(Formatter out) throws IOException, Exception {
		out.format("Name     \t%s\n", getName());
		out.format("Global   \t%s\n", getGlobal());
		out.format("Local    \t%s\n", getLocal());
	}

	/**
	 * Add the bindir to PATH env. variable in .profile
	 */

	@Arguments(arg = {})
	@Description("Add the bin directory for this jpm to your PATH in the user's environment variables in the ~/.profile file.")
	interface PathOptions extends Options {
		@Description("Remove the bindir from the user's environment variables.")
		boolean remove();

		@Description("Delete a path from the PATH environment variable")
		List<String> delete();

		@Description("Add the current binary dir to the PATH environment variable")
		boolean add();

		@Description("Add additional paths to the PATH environment variable")
		List<String> extra();

		@Description("Override the default .profile and use this file. This file is relative to the home directory if not absolute.")
		String profile();

		@Description("If the profile file does not exist, create")
		boolean force();

	}

	static Pattern	PATH_P	= Pattern.compile("(export|set)\\s+PATH\\s*=(.*):\\$PATH\\s*$");

	@Description("Add the bin directory for this jpm to your PATH in the user's environment variables")
	public void _path(PathOptions options) throws IOException {
		File file = options.profile() == null ? IO.getFile("~/.profile") : IO.getFile(IO.home, options.profile());

		if (!file.isFile()) {
			if (!options.force()) {
				reporter.error("No such file %s", file);
				return;
			}
			IO.store("# created by jpm\n", file);
		}

		String parts[] = System.getenv("PATH").split(":");
		List<String> paths = new ArrayList<String>(Arrays.asList(parts));

		for (int i = 0; i < parts.length; i++) {
			System.out.printf("%2d:%s %s %s%n", i, parts[i].toLowerCase().contains("jpm") ? "*" : " ", new File(
					parts[i]).isDirectory() ? " " : "!", parts[i]);
		}

		String bd = jpm.getBinDir().getAbsolutePath();
		String s = IO.collect(file);
		
		//
		// Remove the current binary path. If it is add, we add it later
		//
		if (options.remove() || options.add()) {
			if (!bd.equals(getGlobalBinDir())) {
				s = s.replaceAll("(PATH\\s*=)"+bd+ ":(.*\\$PATH)", "$1$2");
				reporter.trace("removed %s", bd );
			}
		}

		if (options.delete() != null) {
			for (String delete : options.delete()) {
				s = s.replaceAll("(PATH\\s*=)"+Glob.toPattern(delete) + ":(.*\\$PATH)", "$1$2");
			}
		}

		//
		// Remove any orphans
		//
		s = s.replaceAll("\\s*(set|export)\\s+PATH\\s*=\\$PATH\\s*", "");

		List<String> additions = new ArrayList<String>();
		if (options.add() ) {
			if ( !bd.equals( getGlobalBinDir())) {
				additions.add(bd);
			}
		}
		if (options.extra() != null)
			for (String add : options.extra()) {
				File f = IO.getFile(add);
				if (!f.isDirectory()) {
					reporter.error("%s is not a directory, not added", f.getAbsolutePath());
				} else {
					if (!paths.contains(f.getAbsolutePath()))
						additions.add(f.getAbsolutePath());
				}
			}

		if (!additions.isEmpty()) {
			s += "\nexport PATH=" + Strings.join(":", additions) + ":$PATH\n";
			reporter.trace("s %s", s );
		}
		IO.store(s,file);
	}
}

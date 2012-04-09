package aQute.jpm;

import static aQute.lib.io.IO.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;

import javax.swing.*;

import aQute.jpm.platform.*;
import aQute.lib.base64.*;
import aQute.lib.collections.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.libg.filerepo.*;
import aQute.libg.reporter.*;
import aQute.libg.version.*;

/**
 * JPM is the Java package manager. It manages a local repository in the user
 * home directory and/or a global directory. This class is the main entry point
 * for the command line.
 */
@Description("Just Another Package Manager (for java™)\nMaintains a local repository of Java jars (apps or libs). Can automatically link these jars to an OS command or OS service.") public class JustAnotherPackageManager
		extends ReporterAdapter {
	/**
	 * Main options
	 */

	@Description("Options valid for all commands. Must be given before sub command") interface jpmOptions
			extends Options {

		@Description("Use a local jpm directory.") String local();

		@Description("Print exception stack traces when they occur.") boolean exceptions();

		@Description("Trace on.") boolean trace();

		@Description("Be pedantic about all details.") boolean pedantic();

		@Description("Specify a new base directory (default working directory).") String base();

		@Description("Do not return error status for error that match this given regular expression.") String[] failok();
	}

	/**
	 * Install a jar options
	 */
	@Arguments(arg = { "url|file", "..." }) @Description("Install a jar into the repository. If the jar defines a number of headers it can also be installed as a command and/or a service. ") public interface installOptions
			extends Options {
		@Description("Overrides the command name. If the name is '-' then no command is installed. The JAR must have a Main-Class header.") String command();

		@Description("Verify digests in the JAR, provide algorithms. Default is MD5 and SHA1. A '-' ignores the digests.") String[] verify();

		@Description("Install the jar as a service. The JAR must have a Main-Class header.") String service();
	}

	/**
	 * Show installed binaries
	 */

	@Arguments(arg = {}) @Description("List the repository information. Contains bsns, versions, and services.") public interface listOptions
			extends Options {
		String filter();
	}

	/**
	 * Show platform
	 */
	@Arguments(arg = {}) @Description("Show platform specific information.") public interface platformOptions
			extends Options {
	}

	/**
	 * Uninstall a binary.
	 * 
	 */
	@Description("Uninstall a jar by bsn.") @Arguments(arg = { "bsn", "..." }) public interface uninstallOptions
			extends Options {
		@Description("Version range that must be matched, if not specified all versions are removed.") VersionRange range();
	}

	/**
	 * Uninstall a binary.
	 * 
	 */
	public interface deinitOptions extends Options {
		boolean force();
	}

	File			base				= new File(System.getProperty("user.dir"));
	String			repository			= "http://repo.libsync.org";
	File			home;
	File			commands;
	File			services;
	File			working;
	Platform		platform			= Platform.getPlatform(this);
	FileRepo		repo;
	PrintStream		err					= System.err;
	PrintStream		out					= System.out;
	File			sm;

	static Pattern	VERSION_PATTERN		= Pattern
												.compile("[0-9]+(\\.[0-9]+(\\.[0-9]+(\\.[0-9A-Za-z_-]+)?)?)?");
	static Pattern	BSN_PATTERN			= Pattern.compile("[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*");

	static Pattern	COMMAND_PATTERN		= Pattern.compile("[\\w\\d]*");

	static Pattern	URL_PATTERN			= Pattern.compile("[a-zA-Z][0-9A-Za-z]{1,8}:.+");

	static Pattern	DIGEST_PATTERN		= Pattern.compile("([\\w\\d]+)-Digest",
												Pattern.CASE_INSENSITIVE);

	static Pattern	MAINCLASS_PATTERN	= Pattern
												.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");

	/**
	 * Main entry
	 * 
	 * @throws Exception
	 * 
	 */
	public static void main(String args[]) throws Exception {
		JustAnotherPackageManager jpm = new JustAnotherPackageManager();
		jpm.run(args);
	}

	/**
	 * Default constructor
	 * 
	 */

	public JustAnotherPackageManager() {
		super(System.err);
	}

	/**
	 * The command line command for install.
	 * 
	 * @param opts
	 *            The options for installing
	 */
	public void _install(installOptions opts) throws Exception {
		if (!home.canWrite()) {
			error("No write acces, might require administrator or root privileges (sudo in *nix)");
			return;
		}

		for (String target : opts._()) {
			try {
				if (URL_PATTERN.matcher(target).matches()) {
					try {
						// Download
						URL url = new URL(target);
						File tmp = File.createTempFile("jpm", ".jar");
						try {
							copy(url, tmp);
							if (tmp.isFile()) {
								install(tmp, opts);

								// next!

								continue;
							}
						} finally {
							tmp.delete();
						}
					} catch (MalformedURLException mfue) {
						// Ignore, try as file name
					}
				}

				// Try as file/directory name

				File file = IO.getFile(base, target);
				if (file.isFile())
					install(file, opts);
				else if (file.isDirectory()) {
					for (File sub : file.listFiles()) {
						if (sub.getName().endsWith(".jar")) {
							install(sub, opts);
						}
					}
				}

			} catch (IOException e) {
				exception(e, "Could not install %s because %s", target, e);
			}
		}
	}
	
	/**
	 * List the repository, services, and commands
	 * @param opts
	 * @throws Exception
	 */

	public void _list(listOptions opts) throws Exception {
		for (String bsn : repo.list(opts.filter())) {
			Set<Version> versions = new TreeSet<Version>(repo.versions(bsn));
			System.out.printf("%-40s %s\n", bsn, versions);
		}
		for (File cmd : commands.listFiles()) {
			System.out.printf("%-40s %s\n", cmd.getName(), collect(cmd));
		}
		for (File service : services.listFiles()) {
			File start = new File(service, "start");
			System.out.printf("%-40s %s\n", service.getName(), collect(start));
		}
	}

	/**
	 * Uninstall a bsn and any dependent commands
	 * 
	 * @param opts
	 * @throws Exception
	 */
	public void _uninstall(uninstallOptions opts) throws Exception {
		if (!home.canWrite()) {
			error("No write acces, might require administrator or root privileges (sudo in *nix)");
			return;
		}

		if (opts._().isEmpty()) {
			error("Uninstall requires at least one bsn");
			return;
		}
		VersionRange range = opts.range();

		for (String bsn : opts._()) {
			File[] files = repo.get(bsn, range);
			if (files == null) {
				error("No package found for %s:%s", bsn, range);
			} else {
				for (File file : files) {
					uninstall(file);
				}
			}
		}
	}

	/**
	 * Remove all traces.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	public void _deinit(deinitOptions opts) throws Exception {
		
		if (opts._().size() != 0) {
			error("Deinit requires no other parameters: %s", opts._());
			return;
		}

		if ( !checkWrite() )
			return;
		
		for (String cmd : commands.list()) {
			trace("unlinking %s", cmd);
			platform.deleteCommand(cmd);
		}
		
		if (platform.getGlobal().exists()) {
			trace("deleting %s", platform.getGlobal());
			delete(platform.getGlobal());
		}
		return;
	}

	private boolean checkWrite() {
		return home.isDirectory() && home.canWrite();
	}

	/**
	 * Really uninstall.
	 * 
	 * @param file
	 * @throws IOException
	 */
	private void uninstall(File file) throws Exception {
		JarFile jar = new JarFile(file);
		try {
			Manifest manifest = jar.getManifest();
			Attributes main = manifest.getMainAttributes();
			String command = main.getValue("System-Command");
			if (command != null) {
				command = command.trim();
				platform.deleteCommand(command);
			}
		} finally {
			jar.close();
		}
		file.delete();
	}

	/**
	 * Initialize the repository and other global vars.
	 * 
	 * @param opts
	 *            the options
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void _jpm(jpmOptions opts) {
		home = platform.getGlobal();
		home.mkdirs();

		if (opts.local() != null) {
			File local = new File(opts.local());
			if (local.isAbsolute())
				home = local;
			else
				home = new File(base, opts.local());
		}

		if (!home.isDirectory()) {
			error("No access to the home directory %s", home);
			return;
		}

		File r = IO.getFile(home, "repo");

		r.mkdirs();

		repo = new FileRepo(r);

		commands = new File(home, "commands");
		services = new File(home, "services");
		commands.mkdir();
		services.mkdir();

		try {
			setExceptions(opts.exceptions());
			setTrace(opts.trace());
			setPedantic(opts.pedantic());

			if (opts.base() != null)
				base = IO.getFile(base, opts.base());

			CommandLine handler = opts._command();
			List<String> arguments = opts._();

			if (arguments.isEmpty()) {
				Formatter f = new Formatter(err);
				handler.help(f, this);
				f.flush();
			} else {
				String cmd = arguments.remove(0);
				String help = handler.execute(this, cmd, arguments);
				if (help != null) {
					err.println(help);
				}
			}
		} catch (Throwable t) {
			exception(t, "Failed %s", t.getMessage());
		}

		report(err);
		if (!check(opts.failok())) {
			System.exit(getErrors().size());
		}
	}

	/**
	 * Install the source file in the repository.
	 * 
	 * @param target
	 *            The source file
	 * @param opts
	 * @throws IOException
	 */

	private void install(File source, installOptions opts) throws Exception {
		JarFile jar = new JarFile(source);
		Manifest m = jar.getManifest();
		Attributes main = m.getMainAttributes();
		String bsn = main.getValue("Bundle-SymbolicName");
		String version = main.getValue("Bundle-Version");
		String command = main.getValue("JPM-Command");
		String service = main.getValue("JPM-Service");
		String embedded = main.getValue("JPM-Embedded");
		String mainClass = main.getValue("Main-Class");

		List<File> install = new ArrayList<File>();
		if (embedded != null) {
			for (String e : embedded.trim().split("\\s*,\\s*")) {
				trace("embedded %s from %s", e, source);
				File tmp = File.createTempFile("jpm", ".jar");
				InputStream in = getClass().getClassLoader().getResourceAsStream(e);
				if (in != null) {
					copy(in, tmp);
					install.add(tmp);
				} else
					warning("%s contains embedded that is not present: %s", source, e);
			}
		}
		trace("embedded %s", install);

		if (opts != null && opts.command() != null)
			command = opts.command();

		if (opts != null && opts.service() != null)
			service = opts.service();

		try {
			String msg = verify(jar, opts.verify());
			if (msg != null) {
				error("The JAR %s fails to verify, %s", source, msg);
				return;
			}

			bsn = verify(BSN_PATTERN, bsn);
			version = verify(VERSION_PATTERN, version);
			if (command != null)
				command = verify(COMMAND_PATTERN, command);
			if (service != null)
				service = verify(COMMAND_PATTERN, service);
			if (mainClass != null)
				mainClass = verify(MAINCLASS_PATTERN, mainClass);

			if (command != null && mainClass == null)
				error("JPM command is specified but JAR contains no Main-Class attribute for %s",
						source);

			if (service != null && mainClass == null)
				error("JPM service is specified but JAR contains no Main-Class attribute for %s",
						source);

			if (!isOk())
				return;
		} finally {
			jar.close();
		}

		Version v = new Version(version);
		File target = repo.put(bsn, v);
		if (!target.getParentFile().canWrite()) {
			error("Cannot write repo file, try using sudo, %s", target);
			return;
		}

		try {
			for (File e : install)
				install(e, opts);

			copy(source, target);
			trace("copied %s to %s (%s)", source, target, target.isFile());

			if (command != null && !command.equals("-")) {
				platform.createCommand(command, target);

				store(target.getAbsolutePath(), new File(commands, command));

				trace("created command %s to %s", command, target);
			}

			if (service != null && !service.equals("-")) {
				Service s = getService(service);
				s.createService(target, mainClass);
				trace("created services %s to %s", service, target);
			}

		} catch (Exception t) {
			target.delete();
			throw t;
		}
	}

	private Service getService(String service) throws Exception {
		File base = new File(services, service);
		return new Service(this, base, service);
	}

	/**
	 * Main entry for the command line
	 * 
	 * @param args
	 * @throws Exception
	 */
	private void run(String[] args) throws Exception {
		try {
			Method m =System.class.getMethod("console");
			Object o = m.invoke(null);
			if ( o == null) {
				Icon icon = new ImageIcon(
						JustAnotherPackageManager.class.getResource("/images/jpm.png"), "JPM");
				int answer = JOptionPane.showOptionDialog(null,
						"This is a command line application. Setup?", "Package Manager for Java™",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, icon, null, null);
				if (answer == JOptionPane.OK_OPTION) {

					_init(null);
					if (!isPerfect()) {
						StringBuilder sb = new StringBuilder();
						report(sb);
						JOptionPane.showMessageDialog(null, sb.toString());
					} else
						JOptionPane.showMessageDialog(null, "Initialized");
				}
				return;
			}
		} catch (Throwable t) {
			// Ignore, happens in certain circumstances, we fallback to the
			// command line
		}
		CommandLine cl = new CommandLine(this);
		String help = cl.execute(this, "jpm", new ExtList<String>(args));
		check();
		if (help != null)
			err.println(help);

	}

	/**
	 * Setup jpm to run on this system.
	 * 
	 * @throws Exception
	 */
	public void _init(Options opts) throws Exception {
		String s = System.getProperty("java.class.path");
		if (s == null || s.indexOf(File.pathSeparator) > 0) {
			error("Cannot initialize because not clear what the command jar is from java.class.path: %s",
					s);
			return;
		}

		try {
			File f = new File(s).getAbsoluteFile();
			if (f.exists()) {
				CommandLine cl = new CommandLine(this);
				cl.execute(this, "install", Arrays.asList(f.getAbsolutePath()));
			} else
				error("Cannot find the jpm jar from %s", f);
		} catch (InvocationTargetException e) {
			exception(e, "Could not install jpm, %s", e.getCause().getMessage());
		}
	}

	/**
	 * Verify that the jar file is correct. This also verifies ok when there are
	 * no checksums or.
	 * 
	 * @throws IOException
	 */
	String verify(JarFile jar, String[] algorithms) throws IOException {
		if (algorithms == null)
			algorithms = new String[] { "MD5", "SHA1" };
		else if (algorithms.length == 1 && algorithms[0].equals("-"))
			return null;

		try {
			Manifest m = jar.getManifest();
			if (m.getEntries().isEmpty())
				return "No name sections";

			for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
				JarEntry je = e.nextElement();
				if (je.getName().equals("META-INF/MANIFEST.MF") || je.getName().endsWith(".SF")
						|| je.getName().endsWith("/"))
					continue;

				Attributes nameSection = m.getAttributes(je.getName());
				if (nameSection == null)
					return "No name section for " + je.getName();

				for (String algorithm : algorithms) {
					try {
						MessageDigest md = MessageDigest.getInstance(algorithm);
						String expected = nameSection.getValue(algorithm + "-Digest");
						byte digest[] = Base64.decodeBase64(expected);
						copy(jar.getInputStream(je), md);
						if (!Arrays.equals(digest, md.digest()))
							return "Invalid digest for " + je.getName() + ", " + expected + " != "
									+ Base64.encodeBase64(md.digest());

					} catch (NoSuchAlgorithmException nsae) {
						return "Missing digest algorithm " + algorithm;
					}
				}
			}
		} catch (Exception e) {
			return "Failed to verify due to exception: " + e.getMessage();
		}
		return null;
	}

	/*
	 * Help to verify a pattern against a name. Also returns the trimmed value.
	 * Returns null of not verified but also reports an error with the details.
	 */
	private String verify(Pattern pattern, String value) {
		value = value.trim();
		Matcher m = pattern.matcher(value);
		if (m.matches()) {
			return value;
		}
		error("Cannot match %s to %s", pattern.pattern(), value);
		return null;
	}

	public void _platform(platformOptions opts) {
		out.println(platform);
	}

	/**
	 * Start a service.
	 * 
	 * @param options
	 * @throws Exception
	 */
	public void _start(Options options) throws Exception {
		for (String s : options._()) {
			Service service = getService(s);
			if (service == null)
				error("Non existent service %s", s);
			else {
				try {
					if (!service.start())
						progress("Service %s was already started", s);
				} catch (Exception e) {
					exception(e, "Could not start service %s due to %s", s, e.getMessage());
				}
			}
		}
	}

	/**
	 * Stop a service.
	 * 
	 * @param options
	 * @throws Exception
	 */
	public void _stop(Options options) throws Exception {
		for (String s : options._()) {
			Service service = getService(s);
			if (service == null)
				error("Non existent service %s", s);
			else {
				try {
					if (!service.stop())
						progress("Service %s was not running", s);
				} catch (Exception e) {
					exception(e, "Could not stop service %s due to %s", s, e.getMessage());
				}
			}
		}
	}

	/**
	 * Status a service.
	 * 
	 * @param options
	 * @throws Exception
	 */
	public void _status(Options options) {
		for (String s : options._()) {
			String runs = "false";
			String status = "no service";
			try {
				Service service = getService(s);
				if (service != null) {
					runs = service.isRunning() + "";
					status = service.status();
				}
			} catch (Exception e) {
				status = e.getMessage();
				exception(e, "could not fetch status information from service %s, due to %s", s,
						e.getMessage());
			}
			out.printf("%-40s %8s %s\n", s, runs, status);
		}
	}
}

package aQute.jpm.main;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.*;
import java.util.regex.*;

import aQute.bnd.osgi.*;
import aQute.bnd.version.*;
import aQute.jpm.lib.*;
import aQute.jpm.lib.Service;
import aQute.jpm.platform.*;
import aQute.jpm.platform.windows.*;
import aQute.lib.base64.*;
import aQute.lib.collections.*;
import aQute.lib.data.*;
import aQute.lib.getopt.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.justif.*;
import aQute.lib.settings.*;
import aQute.libg.reporter.*;
import aQute.service.library.*;
import aQute.service.library.Library.Program;
import aQute.service.library.Library.Revision;

/**
 * The command line interface to JPM
 */
@Description("Just Another Package Manager (for Java). Maintains a local repository of Java jars (apps or libs). Can automatically link these jars to an OS command or OS service. For more information see https://www.jpm4j.org/#/md/jpm")
public class Main extends ReporterAdapter {
	static Pattern				ASSIGNMENT		= Pattern.compile("\\s*([-\\w\\d_.]+)\\s*(?:=\\s*([^\\s]+)\\s*)?");
	public final static Pattern	URL_PATTERN		= Pattern.compile("[a-zA-Z][0-9A-Za-z]{1,8}:.+");
	public final static Pattern	BSNID_PATTERN	= Pattern.compile("([-A-Z0-9_.]+?)(-\\d+\\.\\d+.\\d+)?",
														Pattern.CASE_INSENSITIVE);
	File						base			= new File(System.getProperty("user.dir"));
	Settings					settings;

	/**
	 * Show installed binaries
	 */

	public interface ModifyService extends ModifyCommand {
		@Description("Provide arguments to the service when started")
		String args();

		@Description("Set the log path. Log output will go to this file, otherwise in a reserved directory")
		String log();

		@Description("Set the log path")
		String work();

		@Description("Set the user id for the service when running")
		String user();

		@Description("If set, will be started at boot time after the given services have been started. Specify boot if there are no other dependencies.")
		List<String> after();

		@Description("Commands executed after the service exits, will run under root.")
		String epilog();

		@Description("Commands executed just before the service starts while still root.")
		String prolog();
	}

	public interface ModifyCommand {
		@Description("Provide or override the JVM arguments")
		String jvmargs();

		@Description("Provide the name of the main class used to launch this command or service in fully qualified form, e.g. aQute.main.Main")
		String main();

		@Description("Provide the name of the command or service")
		String name();

		@Description("Provide the title of the command or service")
		String title();

		@Description("Collect permission requests and print them at the end of a run. This can provide detailed information about what resources the command is using.")
		boolean trace();

	}

	/**
	 * Services
	 */
	@Arguments(arg = {
		"[name]"
	})
	@Description("Manage the JPM services. Without arguments and options, this will show all the current services. Careful, if --remove is used all services are removed without any parameters.")
	public interface ServiceOptions extends Options, ModifyService {

		@Description("Create a new service on an existing artifact")
		String create();

		@Description("Remove the given service")
		boolean remove();

		@Description("Consider staged versions. Normally only masters are considered.")
		boolean staged();

		@Description("Update the service with the latest master/staged version (see --staged)")
		boolean update();

		@Description("Specify the coordinates of the service, identifies the main binary")
		String coordinates();
	}

	/**
	 * Commands
	 */
	@Arguments(arg = "command")
	@Description("Manage the commands that have been installed so far")
	public interface CommandOptions extends Options, ModifyCommand {
		String create();

		boolean remove();

	}

	/**
	 * Uninstall a binary.
	 */
	@Description("Uninstall a jar by bsn.")
	@Arguments(arg = {
			"bsn", "..."
	})
	public interface uninstallOptions_unused extends Options { //pl: not used ...
		@Description("Version range that must be matched, if not specified all versions are removed.")
		Version version();
	}

	/**
	 * Uninstall a binary.
	 */
	public interface deinitOptions extends Options {
		boolean force();
	}

	/**
	 * garbage collect commands and service
	 */
	@Description("Garbage collect any orphan service and commands")
	@Arguments(arg = {})
	public interface GCOptions extends Options {}

	JustAnotherPackageManager	jpm;
	final PrintStream			err;
	final PrintStream			out;
	File						sm;
	private String				url;
	static String				encoding	= System.getProperty("file.encoding");

	static {
		if (encoding == null)
			encoding = Charset.defaultCharset().name();
	}

	/**
	 * Default constructor
	 * 
	 * @throws UnsupportedEncodingException
	 */

	public Main() throws UnsupportedEncodingException {
		super(new PrintStream(System.err, true, encoding));
		err = new PrintStream(System.err, true, encoding);
		out = new PrintStream(System.out, true, encoding);
	}

	/**
	 * Main entry
	 * 
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		Main jpm = new Main();
		try {
			jpm.run(args);
		}
		finally {
			jpm.err.flush();
			jpm.out.flush();
		}
	}

	/**
	 * Main options
	 */

	@Arguments(arg = "cmd ...")
	@Description("Options valid for all commands. Must be given before sub command")
	interface JpmOptions extends Options {

		@Description("Print exception stack traces when they occur.")
		boolean exceptions();

		@Description("Trace on.")
		boolean trace();

		@Description("Be pedantic about all details.")
		boolean pedantic();

		@Description("Specify a new base directory (default working directory).")
		String base();
		
		@Description("Do not return error status for error that match this given regular expression.")
		String[] failok();

		@Description("Remote library url (can also be permanently set with 'jpm settings library.url=...'")
		String library();

		@Description("Cache directory (one-shot)")
		String cache();

		@Description("Wait for a key press, might be useful when you want to see the result before it is overwritten by a next command")
		boolean key();

		@Description("Show the release notes")
		boolean release();
		
		@Description("Run jpm with local configurations (repository -> cache.local setting or platform default, binaries -> bin.local or platform default)")
		boolean user();
		
		@Description("Run jpm with global configurations (repository -> cache.global setting or platform default, binaries -> bin.global or platform default)")
		boolean global();
		
		@Description("Change settings file (one-shot)")
		String settings();
		
		@Description("Specify executables directory (one-shot)")
		String bindir();
	}

	/**
	 * Initialize the repository and other global vars.
	 * 
	 * @param opts
	 *            the options
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Description("Just Another Package Manager for Java")
	public void _jpm(JpmOptions opts) throws IOException {
		try {
			setExceptions(opts.exceptions());
			setTrace(opts.trace());
			setPedantic(opts.pedantic());
			trace("set the options");

			if(opts.settings() != null) {
				trace("Using settings file: %s", opts.settings());
				settings = new Settings(opts.settings());
			} else {
				settings = new Settings("~/.jpm");
			}
			
			if (opts.base() != null)
				base = IO.getFile(base, opts.base());

			url = opts.library();
			if (url == null)
				url = settings.get("library.url");

			if (url != null)
				jpm.setLibrary(new URI(url));

			File homeDir;
			if (opts.cache() != null) {
				trace("Using dir specified in CLI");
				homeDir = IO.getFile(base, opts.cache());
			} else if (opts.user()) {
				homeDir = setLocal();
			} else if (opts.global()) {
				homeDir = setGlobal();
			} else if (settings.containsKey("jpm.runconfig")) {
				if(settings.get("jpm.runconfig").equalsIgnoreCase("local")) {
					homeDir = setLocal();
				} else {
					homeDir = setGlobal();
				}
			} else {
				homeDir = setGlobal();
			}
			jpm.setHomeDir(homeDir);
			if(opts.bindir() != null) {
				jpm.setBinDir(IO.getFile(opts.bindir()));
			}
			
			CommandLine handler = opts._command();
			List<String> arguments = opts._();

			if (arguments.isEmpty()) {
				Justif j = new Justif();
				Formatter f = j.formatter();
				handler.help(f, this);
				err.println(j.wrap());
			} else {
				String cmd = arguments.remove(0);
				String help = handler.execute(this, cmd, arguments);
				if (help != null) {
					err.println(help);
				}
			}
		}

		catch (InvocationTargetException t) {
			Throwable tt = t;
			while (tt instanceof InvocationTargetException)
				tt = ((InvocationTargetException) tt).getTargetException();

			exception(tt, "%s", tt.getMessage());
		}
		catch (Throwable t) {
			exception(t, "Failed %s", t);
		}
		finally {
			// Check if we need to wait for it to finish
			if (opts.key()) {
				System.out.println("Hit a key to continue ...");
				System.in.read();
			}
		}

		if (!check(opts.failok())) {
			System.exit(getErrors().size());
		}
	}

	private File setLocal() throws Exception {
		if(settings.containsKey("jpm.bin.local")) {
			jpm.setBinDir(IO.getFile(settings.get("jpm.bin.local")).getAbsoluteFile());
		}
		
		if (settings.containsKey("jpm.cache.local")) {
			trace("Using local dir (from configuration)");
			return IO.getFile(base, settings.get("jpm.cache.local"));
		} else {
			trace("Using local dir (platform default)");
			return Platform.getPlatform(this).getLocal();
		}
	}
	
	private File setGlobal() throws Exception {
		if(settings.containsKey("jpm.bin.global")) {
			jpm.setBinDir(IO.getFile(settings.get("jpm.bin.global")).getAbsoluteFile());
		}
		
		if (settings.containsKey("jpm.cache.global")) {
			trace("Using global dir (from configuration)");
			return IO.getFile(base, settings.get("jpm.cache.global"));
		} else {
			trace("Using global dir (platform default)");
			return Platform.getPlatform(this).getGlobal();
		}
	}
	
	/**
	 * Install a jar options
	 */
	@Arguments(arg = {
		"command|service"
	})
	@Description("Install a jar into the repository. If the jar defines a number of headers it can also be installed as a command and/or a service. ")
	public interface installOptions extends ModifyCommand, Options {
		@Description("Ignore command and service information")
		boolean ignore(); // pl: not used

		@Description("Force overwrite of existing command")
		boolean force();

		@Description("Require a master version even when version is specified")
		boolean master(); // pl: not used

		@Description("Include staged revisions in the search")
		boolean staged();

		@Description("Ignore digest")
		boolean xdigest(); // pl: not used

		@Description("Run service (if present) under the given user name, default is the name of the service")
		String user(); // pl: not used

		/**
		 * If specified, will install a revision with the given name and version
		 * and then add any command/service to the system.
		 */
		String bsn(); // pl: not used

		/**
		 * Specify a version range for the artifact.
		 * 
		 * @return
		 */
		Version version(); // pl: not used

		/**
		 * Install a file and extra commands
		 */

		String local();

		@Description("The path to the log file")
		String path(); // pl: not used

		@Description("Specify a command name, overrides the JPM-Command header")
		String command(); // pl: not used

	}

	/**
	 * The command line command for install.
	 * 
	 * @param opts
	 *            The options for installing
	 */
	@Description("Install an artifact from a url, file, or www.jpm4j.org")
	public void _install(installOptions opts) throws Exception {
		if (!jpm.hasAccess()) {
			error("No write acces, might require administrator or root privileges (sudo in *nix)");
			return;
		}

		ArtifactData target = null;
		boolean staged = false;

		if (opts.local() != null) {

			File f = IO.getFile(base, opts.local());
			if (f.isFile()) {
				trace("Found file %s", f);
				target = jpm.put(f.toURI());
				target.coordinates = f.toURI().toString();
			} else {
				try {
					trace("Found url %s", f);
					target = jpm.put(new URI(opts.local()));
					target.coordinates = opts.local();
				}
				catch (Exception e) {
					e.printStackTrace();
					// ignore
				}
			}
			if (target == null) {
				error("Could not install from file/url: %s", opts.local());
				return;
			}
		} else {
			if (opts._().isEmpty()) {
				error("You need to specify a command name or artifact id");
				return;
			}
			String key = opts._().get(0);
			if (isSha(key) && (target = jpm.get(Hex.toByteArray(key))) != null) {

				// we've got to check for locally installed files
				trace("locally installed file found");
				target.coordinates = "sha:" + key.trim();
			} else {
				staged = isSha(key) || opts.staged() || key.indexOf('@') > 0;
				target = jpm.getCandidate(key, staged);
				if (target == null) {
					if (!staged) {
						target = jpm.getCandidate(key, true);
						if (target != null) {
							error("No candidate %s found, but found staged version(s): %s", key, target);
							return;
						}
					}
					error("No such candidate %s", key);
					return;
				}

				target.sync();
				if (target.error != null) {
					error("Error in getting target %s", target.error);
					return;
				}
				target.coordinates = key;
			}
		}
		trace("Target from %s", Hex.toHexString(target.sha));

		if (target.command != null) {
			target.command.force = opts.force();
			target.command.coordinates = target.coordinates;
			update(target.command, opts);
			target.command.dependencies.add(0, target.file);
			if (opts.force() && jpm.getCommand(target.command.name) != null)
				jpm.deleteCommand(target.command.name);

			String result = jpm.createCommand(target.command);
			if (result != null)
				error("Command creation failed: %s", result);
		} else if (opts.name() != null) {
			CommandData data = new CommandData();
			data.description = "Installed from command line";
			data.coordinates = target.coordinates;
			data.force = opts.force();
			data.main = target.mainClass;
			data.name = opts.name();
			data.sha = target.sha;
			data.time = System.currentTimeMillis();
			data.dependencies.add(target.file);
			update(data, opts);
			if (data.main == null) {
				error("No main class set");
				return;
			}

			if (opts.force() && jpm.getCommand(data.name) != null)
				jpm.deleteCommand(data.name);
			String result = jpm.createCommand(data);
			if (result != null)
				error("Command creation failed: %s", result);
		} else {
			if (staged)
				warning("No command found in %s", target.coordinates);
			else
				warning("No command found in %s. You could try with --staged to find a staged version?",
						target.coordinates);
		}

		if (target.service != null) {
			target.service.force = opts.force();
			target.service.coordinates = target.coordinates;
			target.service.dependencies.add(0, target.file);
			Service s = jpm.getService(target.service.name);
			trace("existing service %s", s);
			if (opts.force() && s != null) {
				trace("will remove %s", s.getServiceData().bin);
				s.remove();
			}
			String result = jpm.createService(target.service);
			if (result != null)
				error("Service creation failed: %s", result);
		}
	}
	
	

	/**
	 * Check if a key is a sha
	 * 
	 * @param key
	 * @return
	 */
	private boolean isSha(String key) {
		return key != null && key.length() == 40 && key.matches("[\\da-fA-F]{40,40}");
	}

	@Description("Manage the jpm4j services")
	public void _service(ServiceOptions opts) throws Exception {
		if (opts._().isEmpty()) {
			for (ServiceData sd : jpm.getServices())
				print(sd);
			return;
		}

		List<String> cmdline = opts._();
		String name = cmdline.remove(0);

		Service s = jpm.getService(name);

		if (opts.remove()) {
			if (!jpm.hasAccess()) {
				error("No write access to remove service %s", name);
				return;
			}
			if (s == null) {
				error("No such service %s to remove", name);
				return;
			}
			s.stop();
			s.remove();
			return;
		}

		if (opts.create() != null) {
			if (s != null) {
				error("Service already exists, cannot be created: %s. Update or remove it first", name);
				return;
			}

			ArtifactData target = jpm.getCandidate(opts.create(), opts.staged());
			if (target == null)
				return;

			ServiceData data = target.service;
			data.coordinates = opts.create();
			update(data, opts);
			String result = jpm.createService(data);
			if (result != null)
				error("Create service failed: %s", result);
			return;
		}

		if (s == null) {
			error("No such service: %s", name);
			return;
		}

		ServiceData data = s.getServiceData();
		if (update(data, opts) || opts.coordinates() != null || opts.update()) {
			if (!jpm.hasAccess()) {
				error("No write access to update service %s", name);
				return;
			}

			//
			// Check if we have to update the underlying artifact
			// This is triggered by --coordinates, which provides
			// the new coordinates or just --update which reuses the
			// old coordinates without version
			//

			if (opts.coordinates() != null || opts.update()) {
				String coordinates = opts.coordinates();
				if (coordinates == null || coordinates.equals(".")) {
					coordinates = data.coordinates;
				}
				if (coordinates == null) {
					error("No coordinates found in old service record");
					return;
				}

				int n = coordinates.indexOf('@');
				if (n > 0)
					coordinates = coordinates.substring(0, n);

				trace("Updating from coordinates: %s", coordinates);
				ArtifactData target = jpm.getCandidate(coordinates, opts.staged());
				if (target == null) {
					error("No candidates found for %s (%s)", coordinates, opts.staged() ? "staged" : "only masters");
					return;
				}

				data.dependencies.clear();
				data.dependencies.add(target.file);
				data.dependencies.addAll(target.dependencies);
				data.coordinates = coordinates;
			}
			data.force = true;
			String result = jpm.createService(data);
			if (result != null)
				error("Update service failed: %s", result);
			else if (s.isRunning())
				warning("Changes will not affect the currently running process");
		}
		Data.details(data, out);
	}

	private boolean update(ServiceData data, ModifyService opts) {
		boolean update = false;
		if (opts.args() != null) {
			data.args = opts.args();
			update = true;
		}
		if (opts.prolog() != null) {
			data.prolog = opts.prolog();
			update = true;
		}
		if (opts.epilog() != null) {
			data.epilog = opts.epilog();
			update = true;
		}
		if (opts.log() != null) {
			data.log = IO.getFile(base, opts.log()).getAbsolutePath();
			update = true;
		}
		if (opts.work() != null) {
			data.work = IO.getFile(base, opts.work()).getAbsolutePath();
			update = true;
		}

		if (opts.after() != null) {
			data.after = opts.after();
			update = true;
		}
		if (opts.user() != null) {
			data.user = opts.user();
			update = true;
		}

		return update((CommandData) data, opts) || update;
	}

	private boolean update(CommandData data, ModifyCommand opts) {
		boolean update = false;
		if (opts.main() != null) {
			data.main = opts.main();
			update = true;
		}
		if (opts.jvmargs() != null) {
			data.jvmArgs = opts.jvmargs();
			update = true;
		}
		if (opts.name() != null) {
			data.name = opts.name();
			update = true;
		}
		if (opts.title() != null) {
			data.title = opts.title();
			update = true;
		}
		if (opts.trace() != data.trace) {
			data.trace = opts.trace();
			update = true;
		}
		return update;
	}

	private void print(ServiceData sd) throws Exception {
		Service s = jpm.getService(sd.name);
		out.printf("%-40s (%s) %s%n", sd.name, s.isRunning() ? "runs   " : "stopped", sd.args);
	}

	@Description("Manage the jpm4j commands")
	public void _command(CommandOptions opts) throws Exception {

		if (opts.remove()) {
			Instructions instrs = new Instructions(opts._());
			for (CommandData cmd : jpm.getCommands()) {
				if (instrs.matches(cmd.name)) {
					jpm.deleteCommand(cmd.name);
				}
			}
			return;
		}

		if (opts._().isEmpty()) {
			for (CommandData sd : jpm.getCommands())
				out.printf("%-40s %s%n", sd.name, sd.description == null ? "" : sd.description);
			return;
		}

		String cmd = opts._().get(0);

		CommandData data = jpm.getCommand(cmd);
		if (data == null) {
			error("Not found: %s", cmd);
		} else {
			if (update(data, opts)) {
				jpm.deleteCommand(data.name);
				String result = jpm.createCommand(data);
				if (result != null)
					error("Failed to update command %s: %s", cmd, result);
			}

			out.printf("%-40s %s%n", data.name, data.description == null ? "" : data.description);
			out.printf("  %-38s %s%n", "Classpath", data.dependencies);
			out.printf("  %-38s %s%n", "JVM Args", data.jvmArgs);
			out.printf("  %-38s %s%n", "Main class", data.main);
			out.printf("  %-38s %s%n", "Time", new Date(data.time));
		}
	}

	@Description("Clean up any stale data, including any downloaded files not used in any commands")
	public void _gc(GCOptions opts) throws Exception {
		jpm.gc();
	}

	/**
	 * Remove all traces.
	 * 
	 * @param opts
	 * @throws Exception
	 */
	@Description("Remove jpm from the system by deleting all artifacts and metadata")
	public void _deinit(deinitOptions opts) throws Exception {

		if (opts._().size() != 0)
			error("Deinit requires no other parameters: %s", opts._());
		if (!jpm.hasAccess())
			error("Requires write access to jmp area (sudo?)");

		if (!isOk())
			return;

		String result = jpm.deinit(opts.force());
		if (result == null)
			return;

		error("Cannot doinit due to %s", result);
	}

	/**
	 * Main entry for the command line
	 * 
	 * @param args
	 * @throws Exception
	 */
	public void run(String[] args) throws Exception {
		if(jpm == null) { 
			jpm = new JustAnotherPackageManager(this);
		}
		
		try {
			if (args.length > 0 && args[0].equals("daemon"))
				jpm.daemon();
			else {
				CommandLine cl = new CommandLine(this);
				ExtList<String> list = new ExtList<String>(args);
				String help = cl.execute(this, "jpm", list);
				check();
				if (help != null)
					err.println(help);
			}
		}
		finally {
			jpm.close();
		}
	}

	/**
	 * Setup jpm to run on this system.
	 * 
	 * @throws Exception
	 */
	interface InitOptions extends Options {
		
		@Description("Specify cache for jpm install")
		String cache();
		
		@Description("Specify the directory for the jpm executable file")
		String bindir();
	}

	@Description("Install jpm on the current system")
	public void _init(InitOptions opts) throws Exception {
		// Reset config, or respect init flags
		jpm.setHomeDir(opts.cache() == null ? null : IO.getFile(opts.cache()));
		jpm.setBinDir(opts.bindir() == null ? null : IO.getFile(opts.bindir()));
		
		try {
			String s = System.getProperty("java.class.path");
			if (s == null || s.indexOf(File.pathSeparator) > 0) {
				error("Cannot initialize because not clear what the command jar is from java.class.path: %s", s);
				return;
			}
			try {
				File f = new File(s).getAbsoluteFile();
				if (f.exists()) {
					jpm.init();
					CommandLine cl = new CommandLine(this);
					String help = cl.execute(this, "install", Arrays.asList("-fl", f.getAbsolutePath()));
					if (help != null)
						error(help);

				} else
					error("Cannot find the jpm jar from %s", f);
			}
			catch (InvocationTargetException e) {
				exception(e.getTargetException(), "Could not install jpm, %s", e.getTargetException().getMessage());
			}
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * Show platform
	 */
	@Arguments(arg = {})
	@Description("Show the name of the platform, or more specific information")
	public interface PlatformOptions extends Options {
		@Description("Show detailed information")
		boolean verbose();
	}

	/**
	 * Show the platform info.
	 * 
	 * @param opts
	 * @throws IOException
	 * @throws Exception
	 */
	@Description("Show platform information")
	public void _platform(PlatformOptions opts) throws IOException, Exception {
		if (opts.verbose()) {
			Justif j = new Justif(80, 30, 40, 50, 60);
			jpm.getPlatform().report(j.formatter());
			out.append(j.wrap());
		} else
			out.println(jpm.getPlatform().getName());
	}

	/**
	 * Start a service.
	 * 
	 * @param options
	 * @throws Exception
	 */
	@Arguments(arg = {"service"})
	interface startOptions extends Options {
		boolean clean();
	}

	@Description("Start a service")
	public void _start(startOptions options) throws Exception {
		if (!jpm.hasAccess()) {
			error("No write acces, might require administrator or root privileges (sudo in *nix)");
			return;
		}
		for (String s : options._()) {
			Service service = jpm.getService(s);
			if (service == null)
				error("Non existent service %s", s);
			else {
				if (!service.isRunning()) {
					try {
						ServiceData d = service.getServiceData();
						trace("starting %s as user %s, lock=%s, log=%s", d.name, d.user, d.lock, d.log);
						if (options.clean())
							service.clear();
						String result = service.start();
						if (result != null)
							error("Failed to start: %s", result);
					}
					catch (Exception e) {
						exception(e, "Could not start service %s due to %s", s, e.getMessage());
					}
				} else
					warning("Service %s already running", s);
			}
		}
	}

	/**
	 * Restart a service.
	 * 
	 * @param options
	 * @throws Exception
	 */
	@Description("Restart a service")
	public void _restart(Options options) throws Exception {
		for (String s : options._()) {
			Service service = jpm.getService(s);
			if (service == null)
				error("Non existent service %s", s);
			else {
				try {
					if (service.isRunning())
						service.stop();
					String result = service.start();
					if (result != null)
						error("Failed to start: %s", result);
				}
				catch (Exception e) {
					exception(e, "Could not start service %s due to %s", s, e.getMessage());
				}
			}
		}
	}

	/**
	 * Trace a service.
	 * 
	 * @param options
	 * @throws Exception
	 */
	@Arguments(arg = {
			"service", "[on|off]"
	})
	public interface traceOptions extends Options {
		boolean continuous();
	}

	@Description("Trace a service")
	public void _trace(traceOptions options) throws Exception {
		List<String> args = options._();
		String s = args.remove(0);
		boolean on = args.isEmpty() || !"off".equalsIgnoreCase(args.remove(0));

		Service service = jpm.getService(s);
		if (service == null)
			error("Non existent service %s", s);
		else {
			try {
				if (!service.isRunning())
					error("First start the service to trace it");
				else {
					String result = service.trace(on);
					if (result != null)
						error("Failed to trace: %s", result);
				}
			}
			catch (Exception e) {
				exception(e, "Could not trace service %s due to %s", s, e.getMessage());
			}
		}
	}

	/**
	 * Stop a service.
	 * 
	 * @param options
	 * @throws Exception
	 */
	@Description("Stop a service")
	public void _stop(Options options) throws Exception {
		for (String s : options._()) {
			Service service = jpm.getService(s);
			if (service == null)
				error("Non existent service %s", s);
			else {
				if (service.isRunning()) {
					try {
						String result = service.stop();
						if (result != null)
							error("Failed to stop: %s", result);
					}
					catch (Exception e) {
						exception(e, "Could not stop service %s due to %s", s, e.getMessage());
					}
				} else
					warning("Service %s not running", s);
			}
		}
	}

	/**
	 * Status a service.
	 * 
	 * @param options
	 * @throws Exception
	 */
	interface statusOptions extends Options {
		boolean continuous();
	}

	@Description("Status of a service")
	public void _status(statusOptions options) throws InterruptedException {
		while (true) {
			for (String s : options._()) {
				String runs = "false";
				String status = "no service";
				try {
					Service service = jpm.getService(s);
					if (service != null) {
						runs = service.isRunning() + "";
						status = service.status();
					}
				}
				catch (Exception e) {
					status = e.getMessage();
					exception(e, "could not fetch status information from service %s, due to %s", s, e.getMessage());
				}
				out.printf("%-40s %8s %s\r", s, runs, status);
			}
			if (!options.continuous()) {
				out.println();
				return;
			}
			Thread.sleep(1000);
		}
	}

	/**
	 * Show the current version
	 * 
	 * @throws IOException
	 */

	@Arguments(arg = {})
	@Description("Show the current version. The qualifier represents the build date.")
	interface VersionOptions extends Options {

	}

	@Description("Show the current version of jpm")
	public void _version(VersionOptions options) throws IOException {
		Enumeration<URL> urls = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			trace("found manifest %s", url);
			Manifest m = new Manifest(url.openStream());
			String name = m.getMainAttributes().getValue("Bundle-SymbolicName");
			if (name != null && name.trim().equals("biz.aQute.jpm.run")) {
				out.println(m.getMainAttributes().getValue("Bundle-Version"));
				return;
			}
		}
		error("No version found in jar");
	}

	// /**
	// * Manage the interface to the library
	// *
	// * @throws Exception
	// */
	//
	// public void _lib(LibraryCommandOptions options) throws Exception {
	// LibraryCommand library = new LibraryCommand(options, base, out,
	// settings);
	//
	// CommandLine cline = options._command();
	// List<String> _ = options._();
	// if (_.isEmpty())
	// cline.execute(library, "info", _);
	// else
	// cline.execute(library, _.remove(0), _);
	//
	// }

	/**
	 * @throws Exception
	 */

	interface KeysOptions extends Options {
		boolean secret();

		boolean pem();

		boolean hex();

		boolean extended();
	}

	@Description("Show the jpm machine keys")
	public void _keys(KeysOptions opts) throws Exception {
		boolean any = opts.pem() || opts.extended() || opts.hex();

		if (opts.extended()) {
			PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(settings.getPrivateKey());
			X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(settings.getPublicKey());
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
			PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
			privateKey.getAlgorithm();
			if (opts.secret())
				out.format("private %s", privateKey);
			out.format("public  %s", publicKey);
		}
		if (opts.hex()) {
			if (opts.secret())
				out.format("private %s", Hex.toHexString(settings.getPrivateKey()));
			out.format("public  %s", Hex.toHexString(settings.getPublicKey()));
		}
		if (opts.pem() || !any) {
			formatKey(settings.getPublicKey(), "PUBLIC");
			if (opts.secret())
				formatKey(settings.getPrivateKey(), "PRIVATE");
		}
	}

	private void formatKey(byte[] data, String type) throws UnknownHostException {
		String email = settings.getEmail();
		if (email == null)
			email = "<no email set>";
		email += " " + InetAddress.getLocalHost().getHostName();

		StringBuilder sb = new StringBuilder(Base64.encodeBase64(data));
		int r = 60;
		while (r < sb.length()) {
			sb.insert(r, "\n");
			r += 60;
		}
		out.format("-----BEGIN %s %s KEY-----%n", email, type);
		out.append(sb.toString()).append("\n");
		out.format("-----END %s %s KEY-----%n", email, type);
	}

	/**
	 * Show the tail of the log output.
	 */

	@Arguments(arg = "service")
	interface logOptions extends Options {

		boolean tail();

		boolean clear();

	}

	@Description("Show the service log")
	public void _log(logOptions opts) throws Exception {

		String s = opts._().isEmpty() ? null : opts._().get(0);
		if (s == null) {
			error("No such service %s", s);
			return;
		}
		Service service = jpm.getService(s);
		if (service == null) {
			error("No such service %s", s);
			return;
		}

		ServiceData data = service.getServiceData();
		File logFile = new File(data.log);
		if (!logFile.isFile()) {
			error("Log file %s for service %s is not a file", logFile, s);
			return;
		}

		if (opts.clear()) {
			logFile.delete();
			logFile.createNewFile();
		}

		RandomAccessFile raf = new RandomAccessFile(logFile, "r");
		try {
			long start = Math.max(logFile.length() - 2000, 0);
			while (true) {
				long l = raf.length();
				byte[] buffer = new byte[(int) (l - start)];
				raf.seek(start);
				raf.read(buffer);
				out.write(buffer);
				start = l;
				if (!service.isRunning() || !opts.tail())
					return;

				if (l == raf.length())
					Thread.sleep(100);
			}
		}
		finally {
			raf.close();
		}
	}

	/**
	 * Install JPM as a platform daemon that will start the services marked with
	 * after xxx (where boot is the canonical start).
	 */

	@Arguments(arg = {})
	interface registerOptions extends Options {
		@Description("Register for user login only")
		boolean user();
	}

	@Description("Install JPM as a platform daemon")
	public void _register(registerOptions opts) throws Exception {
		jpm.register(opts.user());
	}

	/**
	 * Handle the global settings
	 */
	interface settingOptions extends Options {
		boolean clear();

		boolean publicKey();

		boolean secretKey();

		boolean id();

		boolean mac();

		boolean hex();
	}

	@Description("Manage user settings of jpm (in ~/.jpm)")
	public void _settings(settingOptions opts) throws Exception {
		try {
			trace("settings %s", opts.clear());
			List<String> rest = opts._();

			if (opts.clear()) {
				settings.clear();
				trace("clear %s", settings.entrySet());
			}

			if (opts.publicKey()) {
				out.println(tos(opts.hex(), settings.getPublicKey()));
				return;
			}
			if (opts.secretKey()) {
				out.println(tos(opts.hex(), settings.getPrivateKey()));
				return;
			}
			if (opts.id()) {
				out.printf("%s\n", tos(opts.hex(), settings.getPublicKey()));
			}

			if (opts.mac()) {
				for (String s : rest) {
					byte[] data = s.getBytes("UTF-8");
					byte[] signature = settings.sign(data);
					out.printf("%s\n", tos(opts.hex(), signature));
				}
				return;
			}

			if (rest.isEmpty()) {
				list(null, settings);
			} else {
				boolean set = false;
				for (String s : rest) {
					Matcher m = ASSIGNMENT.matcher(s);
					trace("try %s", s);
					if (m.matches()) {
						trace("matches %s %s %s", s, m.group(1), m.group(2));
						String key = m.group(1);
						Instructions instr = new Instructions(key);
						Collection<String> select = instr.select(settings.keySet(), true);

						String value = m.group(2);
						if (value == null) {
							trace("list wildcard " + instr + " " + select + " " + settings.keySet());
							list(select, settings);
						} else {
							trace("assignment 	");
							settings.put(key, value);
							set = true;
						}
					} else {
						err.printf("Cannot assign %s\n", s);

					}
				}
				if (set) {
					trace("saving");
					settings.save();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String tos(boolean hex, byte[] data) {
		return hex ? Hex.toHexString(data) : Base64.encodeBase64(data);
	}

	private void list(Collection<String> keys, Map<String,String> map) {
		for (Entry<String,String> e : map.entrySet()) {
			if (keys == null || keys.contains(e.getKey()))
				out.printf("%-40s = %s\n", e.getKey(), e.getValue());
		}
	}

	/**
	 * Turned out that StartSSL HTTPS certificates are not recognized by the Java
	 * certificate store. So we have a command to get the certificate chain
	 */

	@Arguments(arg = "host")
	@Description("Provides a way to let Java trust a host for HTTPS access. "
			+ "The certificate command makes it possible to import an HTTPS certificate from a "
			+ "remote host that runs HTTPS. It will contact the host over the given host and port and "
			+ "then add the certificate chain's top certificate to the local keystore of the running VM. This command"
			+ "requires running as an administrator/root. By default this command will only show what it"
			+ "will do, specify -i/--install to really install it. Note that running this command is global for the Java VM and is persistent.")
	interface CertificateOptions extends Options {
		@Description("Only install the certificate when this option is specified")
		boolean install();

		@Description("Override the default HTTPS port 443")
		int port();

		@Description("Password for the keystore. Only necessary when the password has been changed before (the default is 'changeit' for MacOS and 'changeme' for others)")
		String secret();

		@Description("Override the default $JAVA_HOME/lib/security/(jsse)cacerts file location.")
		File cacerts();
	}

	@Description("Install a certificate that is trusted by this VM (persistent for the JVM!)")
	public void _certificate(CertificateOptions opts) throws Exception {
		if (!this.jpm.hasAccess())
			error("Must be administrator");

		InstallCert.installCert(this, opts._().get(0), opts.port() == 0 ? 443 : opts.port(),
				opts.secret() == null ? Platform.getPlatform(this).defaultCacertsPassword() : opts.secret(),
				opts.cacerts(), opts.install());
	}

	/**
	 * List candidates
	 * 
	 * @throws Exception
	 */

	@Description("Show the candidate revisions for a given program coordinate")
	public void _candidates(Options opts) throws Exception {
		for (String key : opts._()) {
			List<Revision> candidates = jpm.getCandidates(key);
			if (candidates == null) {
				error("No candidates found for %s", key);
			} else
				print(candidates);
		}
	}

	void print(Iterable<Revision> revisions) {
		for (Revision r : revisions) {
			out.printf("%-40s %s %s\n", jpm.getCoordinates(r), Hex.toHexString(r._id), (r.description == null ? ""
					: r.description));
		}
	}

	void printPrograms(Iterable< ? extends Program> programs) {
		Justif j = new Justif(120, 40, 42, 100);
		StringBuilder sb = new StringBuilder();
		Formatter f = new Formatter(sb);
		try {
			for (Program p : programs) {
				if (p.groupId.equals(Library.OSGI_GROUP) || p.groupId.equals(Library.SHA_GROUP))
					f.format("%s", p.artifactId);
				else
					f.format("%s:%s", p.groupId, p.artifactId);

				f.format("\t0-\t1");

				if (p.wiki != null && p.wiki.text != null)
					sb.append(p.wiki.text.replace('\n', '\f'));
				else if (p.last != null) {
					if (p.last.description != null)
						sb.append(p.last.description.replace('\n', '\f'));
				}
				f.format("%n");
			}
			j.wrap(sb);
			out.println(sb);
		}
		finally {
			f.close();
		}
	}

	interface findOptions extends Options {

	}

	@Description("Find programs from a query")
	public void _find(findOptions opts) throws Exception {
		String q = new ExtList<String>(opts._()).join(" ");
		Iterable< ? extends Program> programs = jpm.find(q);
		printPrograms(programs);
	}

	/**
	 * Some window specific commands
	 */

	enum Key {
		HKEY_LOCAL_MACHINE(WinRegistry.HKEY_LOCAL_MACHINE), HKEY_CURRENT_USER(WinRegistry.HKEY_CURRENT_USER);

		int	n;

		Key(int n) {
			this.n = n;
		}

		public int value() {
			return n;
		}
	}

	@Arguments(arg = {
			"key", "[property]"
	})
	interface winregOptions extends Options {
		boolean localMachine();
	}

	@Description("Windows specific access to the registry")
	public void _winreg(winregOptions opts) throws Exception {
		List<String> _ = opts._();
		String key = _.remove(0);
		String property = _.isEmpty() ? null : _.remove(0);
		int n = opts.localMachine() ? WinRegistry.HKEY_LOCAL_MACHINE : WinRegistry.HKEY_CURRENT_USER;

		List<String> keys = WinRegistry.readStringSubKeys(n, key);
		if (property == null) {
			Map<String,String> map = WinRegistry.readStringValues(n, key);
			out.println(map);
		} else {
			WinRegistry.readString(n, key, property);
		}
	}

	/**
	 * Make the setup local
	 */
	@Arguments(arg = {
		"user|global"
	})
	interface setupOptions extends Options {}

	@Description("Make jpm local/global")
	public void _setup(setupOptions opts) {

		
		String type = opts._().remove(0);
		if (type.equalsIgnoreCase("user")) {
			trace("Making jpm local");
			settings.put("jpm.runconfig", "local");
		} else {
			trace("Making jpm global");
			settings.put("jpm.runconfig", "global");
		}
		settings.save();
	}

	/**
	 * Deposit a file in JPM and scan it.
	 */

	@Arguments(arg = "file")
	@Description("Deposit a file in a private depository")
	interface DepositOptions extends Options {
		@Description("If this file should be scanned")
		boolean scan();

		@Description("Import message for scanning")
		String message();

		@Description("Repository path")
		String path();

		@Description("Override email")
		String email();
		
	}

	@Description("Deposit a file in a private depository")
	public void _deposit(DepositOptions options) {
		File f = IO.getFile(base, options._().get(0));
		if ( f.isFile()) {
			error("No such file %s", f);
		}

	}
	
	/**
	 * Alternative for command -r {@code <commandName>}
	 */
	@Arguments(arg = "command|service")
	@Description("Remove the specified command or service from the system (equivalent to jpm <commmand|service> -r <command|service>)")
	interface UninstallOptions extends Options {}
	
	@Description("Remove a command or a service from the system")
	public void _remove(UninstallOptions opts) throws Exception {
		if(!jpm.hasAccess()) {
			error("No write acces, might require administrator or root privileges (sudo in *nix)");
			return;
		}
		
		if (opts._().isEmpty()) {
			error("Syntax: jpm remove <command|service>");
			return;
		}

		String name = opts._().get(0);
		Service s = null;
		
		if(jpm.getCommand(name) != null) { // Try command first
			trace("Corresponding command found, removing");
			jpm.deleteCommand(name);
			
		} else if((s = jpm.getService(name)) != null) { // No command matching, try service
			trace("Corresponding service found, removing");
			s.remove();
		
		} else { // No match amongst commands & services
			error("No matching command or service found");
		}
		
	}
	
	@Description("Generate markdown documentation for jpm") 
	interface MarkdownOptions extends Options {}
	
	@Description("Generate markdown documentation for jpm") 
	public void _gmd(MarkdownOptions opts) {
		CommandLine cl = new CommandLine(this);	
		cl.generateDocumentation(this);
	}
	
	/**
	 * Constructor for testing purposes
	 */
	public Main(JustAnotherPackageManager jpm) throws UnsupportedEncodingException {
		this();
		this.jpm = jpm;
	}
}

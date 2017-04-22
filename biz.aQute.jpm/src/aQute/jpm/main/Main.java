package aQute.jpm.main;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Instructions;
import aQute.jpm.lib.ArtifactData;
import aQute.jpm.lib.CommandData;
import aQute.jpm.lib.JVM;
import aQute.jpm.lib.JustAnotherPackageManager;
import aQute.jpm.lib.JustAnotherPackageManager.UpdateMemo;
import aQute.jpm.lib.Service;
import aQute.jpm.lib.ServiceData;
import aQute.jpm.platform.Platform;
import aQute.lib.base64.Base64;
import aQute.lib.collections.ExtList;
import aQute.lib.data.Data;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.CommandLine;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.hex.Hex;
import aQute.lib.io.IO;
import aQute.lib.justif.Justif;
import aQute.lib.settings.Settings;
import aQute.lib.strings.Strings;
import aQute.libg.command.Command;
import aQute.libg.glob.Glob;
import aQute.libg.reporter.ReporterAdapter;
import aQute.service.library.Coordinate;
import aQute.service.library.Library;
import aQute.service.library.Library.Program;
import aQute.service.library.Library.Revision;
import aQute.struct.struct.Error;
/**
 * The command line interface to JPM
 */
@Description("Just Another Package Manager (for Java)\nMaintains a local repository of Java jars (apps or libs). Can automatically link these jars to an OS command or OS service. For more information see https://www.jpm4j.org/#!/md/jpm")
public class Main extends ReporterAdapter {
	private final static Logger	logger			= LoggerFactory.getLogger(Main.class);
	private static final String	JPM_CONFIG_BIN	= "jpm.config.bin";
	private static final String	JPM_CONFIG_HOME	= "jpm.config.home";
	static Pattern				ASSIGNMENT		= Pattern.compile("\\s*([-\\w\\d_.]+)\\s*(?:=\\s*([^\\s]+)\\s*)?");
	public final static Pattern	URL_PATTERN		= Pattern.compile("[a-zA-Z][0-9A-Za-z]{1,8}:.+");
	public final static Pattern	BSNID_PATTERN	= Pattern.compile("([-A-Z0-9_.]+?)(-\\d+\\.\\d+.\\d+)?",
			Pattern.CASE_INSENSITIVE);
	File						base			= new File(System.getProperty("user.dir"));
	Settings					settings;
	boolean						userMode		= false;

	JustAnotherPackageManager	jpm;
	final PrintStream			err;
	final PrintStream			out;
	File						sm;
	private String				url;
	private JpmOptions			options;
	static String				encoding		= System.getProperty("file.encoding");
	int							width			= 120;																// characters
	int							tabs[]			= {
														40, 48, 56, 64, 72, 80, 88, 96, 104, 112
													};

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
		} finally {
			jpm.err.flush();
			jpm.out.flush();
		}
	}

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

		@Description("Java is default started in console mode, you can specify to start it in windows mode (or javaw)")
		boolean windows();

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

		@Description("Specify the coordinate of the service, identifies the main binary")
		String coordinates();
	}

	/**
	 * Commands
	 */
	@Arguments(arg = "[command]")
	@Description("Manage the commands that have been installed so far")
	public interface CommandOptions extends Options, ModifyCommand {
		String create();

		@Description("Remove the given service")
		boolean remove();

	}

	@Description("Remove jpm and all created data from the system (including commands and services). "
			+ "Without the --force flag only list the elements that would be deleted.")
	public interface deinitOptions extends Options {

		@Description("Actually remove jpm from the system")
		boolean force();
	}

	/**
	 * garbage collect commands and service
	 */
	@Arguments(arg = {})
	@Description("Garbage collect the cache (remove useless dependencies)")
	public interface GCOptions extends Options {}

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

		@Description("Specify the home directory of jpm. (can also be permanently set with 'jpm settings jpm.home=...'")
		String home();

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

		@Description("Specify the platform (this is mainly for testing purposes). Is either WINDOWS, MACOS, or LINUX")
		Platform.Type os();

		boolean xtesting();

		int width();
	}

	/**
	 * Initialize the repository and other global vars.
	 * 
	 * @param opts the options
	 * @throws IOException
	 */
	@Description("Just Another Package Manager for Java (\"jpm help jpm\" to see a list of global options)")
	public void _jpm(JpmOptions opts) throws IOException {

		try {
			setExceptions(opts.exceptions());
			setTrace(opts.trace());
			setPedantic(opts.pedantic());
			Platform platform = Platform.getPlatform(this, opts.os());

			if (opts.base() != null)
				base = IO.getFile(base, opts.base());

			if (opts.settings() != null) {
				settings = new Settings(opts.settings());
				logger.debug("Using settings file: {}", opts.settings());
			} else {
				settings = new Settings(platform.getConfigFile());
				logger.debug("Using settings file: {}", platform.getConfigFile());
			}

			File homeDir;
			File binDir;
			String home = settings.get(JPM_CONFIG_HOME);
			String bin = settings.get(JPM_CONFIG_BIN);

			if (opts.home() != null) {
				logger.debug("home set");
				homeDir = IO.getFile(base, opts.home());
				binDir = new File(homeDir, "bin");
			} else if (opts.user()) {
				logger.debug("user set");
				homeDir = platform.getLocal();
				binDir = new File(homeDir, "bin");
			} else if (!opts.global() && home != null) {
				logger.debug("global or in settings");
				homeDir = new File(home);
				binDir = new File(bin);
			} else {
				logger.debug("default");
				homeDir = platform.getGlobal();
				binDir = platform.getGlobalBinDir();
			}

			logger.debug("home={}, bin={}", homeDir, binDir);
			if (opts.bindir() != null) {
				logger.debug("bindir set");
				binDir = new File(opts.bindir());
				if (!binDir.isAbsolute())
					binDir = new File(base, opts.bindir());
				binDir = binDir.getAbsoluteFile();
			} else if (bin != null && !opts.user() && !opts.global()) {
				binDir = new File(bin);
			}

			logger.debug("home={}, bin={}", homeDir, binDir);

			url = opts.library();
			if (url == null)
				url = settings.get("library.url");

			jpm = new JustAnotherPackageManager(this, platform, homeDir, binDir);
			platform.setJpm(jpm);
			jpm.setLibrary(url == null ? null : new URI(url));

			try {
				this.options = opts;
				if (opts.xtesting())
					jpm.setUnderTest();

				CommandLine handler = opts._command();
				List<String> arguments = opts._arguments();

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

				if (options.width() > 0)
					this.width = options.width();
			} finally {
				jpm.close();
			}
		}

		catch (InvocationTargetException t) {
			Throwable tt = t;
			while (tt instanceof InvocationTargetException)
				tt = ((InvocationTargetException) tt).getTargetException();

			exception(tt, "%s", tt);
		} catch (Throwable t) {
			exception(t, "Failed %s", t);
		} finally {
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

	/**
	 * Install a jar options
	 */
	@Arguments(arg = {
			"command|service"
	})
	@Description("Install a jar into the repository. If the jar defines a number of headers it can also be installed as a command and/or a service. "
			+ "If not, additional information such as the name of the command and/or the main class must be specified with the appropriate flags.")
	public interface installOptions extends ModifyCommand, Options {
		// @Description("Ignore command and service information")
		// boolean ignore(); // pl: not used

		@Description("Force overwrite of existing command")
		boolean force();

		// @Description("Require a master version even when version is
		// specified")
		// boolean master(); // pl: not used

		// @Description("Ignore digest")
		// boolean xdigest(); // pl: not used

		// @Description("Run service (if present) under the given user name,
		// default is the name of the service")
		// String user(); // pl: not used

		// /**
		// * If specified, will install a revision with the given name and
		// version
		// * and then add any command/service to the system.
		// */
		// String bsn(); // pl: not used

		// /**
		// * Specify a version range for the artifact.
		// *
		// */
		// Version version(); // pl: not used

		/**
		 * Install a file and extra commands
		 */
		@Description("Install jar without resolving dependencies with http://www.jpm4j.org")
		boolean local();

		/**
		 * Do not look for command
		 */
		@Description("Install jar but do not look for a command in the jar")
		boolean ignore();
	}

	/**
	 * A better way to install
	 */

	@Description("Install an artifact from a url, file, or http://www.jpm4j.org")
	public void _install(installOptions opts) throws Exception {

		if (!this.options.user() && !jpm.hasAccess()) {
			error("No write acces, might require administrator or root privileges (sudo in *nix)");
			return;
		}

		for (String coordinate : opts._arguments()) {
			logger.debug("install {}", coordinate);
			File file = IO.getFile(base, coordinate);
			if (file.isFile()) {
				coordinate = file.toURI().toString();
				logger.debug("is existing file: {}", coordinate);
			}

			ArtifactData artifact = jpm.getCandidate(coordinate);
			logger.debug("candidate {}", artifact);
			if (artifact == null) {
				if (jpm.isWildcard(coordinate))
					error("no candidate found for %s", coordinate);
				else
					error("no candidate found for %s, you could try %s@* to also see staged and withdrawn revisions",
							coordinate, coordinate);
			} else {

				if (!opts.ignore()) {
					CommandData cmd = jpm.parseCommandData(artifact);
					updateCommandData(cmd, opts);
					logger.debug("main={}, name={}", cmd.main, cmd.name);
					if (cmd.main != null) {
						if (cmd.name == null && !artifact.local) {
							cmd.name = artifact.coordinate.getArtifactId();
						}
						List<Error> errors = cmd.validate();
						if (!errors.isEmpty()) {
							error("Command not valid");
							for (Error error : errors) {
								error("[%s] %s %s %s %s", error.code, error.description, error.path, error.failure,
										error.value);
							}
						} else {
							String result = jpm.createCommand(cmd, opts.force());
							if (result != null) {
								error("[%s] %s", coordinate, result);
							}
						}
					} else
						error("No main class found. Please specify");
				}
			}
		}
	}

	@Description("Manage the jpm4j services")
	public void _service(ServiceOptions opts) throws Exception {
		if (opts._arguments().isEmpty()) {
			for (ServiceData sd : jpm.getServices())
				print(sd);
			return;
		}

		List<String> cmdline = opts._arguments();
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
			logger.debug("create service");
			if (s != null) {
				error("Service already exists, cannot be created: %s. Update or remove it first", name);
				return;
			}

			ArtifactData target = jpm.getCandidate(opts.create());
			if (target == null) {
				error("Cannot find candidate for coordinates", opts.create());
				return;
			}

			ServiceData data = new ServiceData();
			CommandData cmd = jpm.parseCommandData(target);
			for (Field f : cmd.getClass().getFields()) {
				f.set(data, f.get(cmd));
			}

			logger.debug("service data {}", cmd);

			data.name = name;

			updateServiceData(data, opts);

			logger.debug("update service data");
			String result = jpm.createService(data, false);
			if (result != null)
				error("Create service failed: %s", result);
			return;
		}

		if (s == null) {
			error("No such service: %s", name);
			return;
		}

		ServiceData data = s.getServiceData();
		if (updateServiceData(data, opts) || opts.coordinates() != null || opts.update()) {
			if (!jpm.hasAccess()) {
				error("No write access to update service %s", name);
				return;
			}

			//
			// Check if we have to update the underlying artifact
			// This is triggered by --coordinate, which provides
			// the new coordinate or just --update which reuses the
			// old coordinate without version
			//

			if (opts.coordinates() != null || opts.update()) {
				String coordinates = opts.coordinates();
				if (coordinates == null) {
					error("No coordinate found in old service record");
					return;
				}

				int n = coordinates.indexOf('@');
				if (n > 0)
					coordinates = coordinates.substring(0, n);

				logger.debug("Updating from coordinate: {}", coordinates);
				ArtifactData target = jpm.getCandidate(coordinates);
				if (target == null) {
					error("No candidates found for %s (%s)", coordinates, opts.staged() ? "staged" : "only masters");
					return;
				}

				CommandData cmd = jpm.parseCommandData(target);
				for (Field f : cmd.getClass().getFields()) {
					f.set(data, f.get(cmd));
				}
				data.name = name;
			}

			String result = jpm.createService(data, true);
			if (result != null)
				error("Update service failed: %s", result);
			else if (s.isRunning())
				warning("Changes will not affect the currently running process");
		}
		Data.details(data, out);
	}

	private boolean updateServiceData(ServiceData data, ModifyService opts) {
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

		return updateCommandData(data, opts) || update;
	}

	private boolean updateCommandData(CommandData data, ModifyCommand opts) {
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
		if (opts.windows() != data.windows) {
			data.windows = opts.windows();
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
			Instructions instrs = new Instructions(opts._arguments());
			for (CommandData cmd : jpm.getCommands()) {
				if (instrs.matches(cmd.name)) {
					jpm.deleteCommand(cmd.name);
				}
			}
			return;
		}

		if (opts._arguments().isEmpty()) {
			print(jpm.getCommands());
			return;
		}

		String cmd = opts._arguments().get(0);

		CommandData data = jpm.getCommand(cmd);
		if (data == null) {
			error("Not found: %s", cmd);
		} else {
			CommandData newer = new CommandData();
			JustAnotherPackageManager.xcopy(data, newer);

			if (updateCommandData(newer, opts)) {
				jpm.deleteCommand(data.name);
				String result = jpm.createCommand(newer, true);
				if (result != null)
					error("Failed to update command %s: %s", cmd, result);
			}
			print(newer);
		}
	}

	private void print(CommandData command) throws Exception {
		Justif j = new Justif(width, tabs);
		Formatter f = j.formatter();
		f.format("%n[%s]%n", command.name);
		f.format("%s\n\n", Strings.display(command.description, command.title));
		f.format("SHA-1\t1%s%n", Hex.toHexString(command.sha));
		f.format("Bundle Symbolic Name\t1%s%n", Strings.display(command.bsn, "<no bsn>"));
		f.format("Version\t1%s%n", Strings.display(command.version, "<no version>"));
		f.format("JVMArgs\t1%s%n", "JVM Args", command.jvmArgs);
		f.format("Main class\t1%s%n", command.main);
		f.format("Install time\t1%s%n", new Date(command.time));
		f.format("Path\t1%s%n", command.bin);
		f.format("Installed\t1%s%n", command.installed);
		f.format("JRE\t1%s%n", Strings.display(command.java, "<default>"));
		f.format("Trace\t1%s%n", command.trace ? "On" : "Off");
		list(f, "Dependencies", jpm.toString(command.dependencies));
		list(f, "Runbundles", jpm.toString(command.runbundles));

		out.append(j.wrap());
	}

	private void list(Formatter f, String title, List< ? > elements) {
		if (elements == null || elements.isEmpty())
			return;

		f.format("[%s]\t1", title);
		String del = "";
		for (Object element : elements) {
			f.format("%s%s", del, element);
			del = "\f";
		}
		f.format("%n");
	}

	private void print(List<CommandData> commands) {
		Justif j = new Justif(width, tabs);
		Formatter f = j.formatter();
		for (CommandData command : commands) {
			f.format("%s\t1%s%n", command.name, Strings.display(command.description, command.title));
		}
		out.append(j.wrap());
	}

	@Description("Clean up any stale data, including any downloaded files not used in any commands")
	public void _gc(GCOptions opts) throws Exception {
		if (!jpm.hasAccess()) {
			error("No write acces, might require administrator or root privileges (sudo in *nix)");
			return;
		}
		error("Not supported right now");

		// jpm.gc();
	}

	@Description("Remove jpm from the system by deleting all artifacts and metadata")
	public void _deinit(deinitOptions opts) throws Exception {
		jpm.deinit(out, opts.force());
	}

	/**
	 * Main entry for the command line
	 * 
	 * @param args
	 * @throws Exception
	 */
	public void run(String[] args) throws Exception {
		CommandLine cl = new CommandLine(this);
		ExtList<String> list = new ExtList<String>(args);
		String help = cl.execute(this, "jpm", list);
		check();
		if (help != null)
			err.println(help);
	}

	/**
	 * Setup jpm to run on this system.
	 * 
	 */
	@Description("Install jpm on the current system")
	interface InitOptions extends Options {}

	@Description("Install jpm on the current system")
	public void _init(InitOptions opts) throws Exception {

		if (!jpm.hasAccess()) {
			error("No write acces, might require administrator or root privileges (sudo in *nix)");
			return;
		}

		jpm.getPlatform().init();

		try {
			String s = System.getProperty("java.class.path");
			if (s == null) {
				error("Cannot initialize because not clear what the command jar is from java.class.path: %s", s);
				return;
			}
			String parts[] = s.split(File.pathSeparator);
			s = parts[0];
			try {
				File f = new File(s).getAbsoluteFile();
				if (f.exists()) {
					CommandLine cl = new CommandLine(this);
					String help = cl.execute(this, "install", Arrays.asList("-fl", f.getAbsolutePath()));
					if (help != null) {
						error(help);
						return;
					}

					String completionInstallResult = jpm.getPlatform().installCompletion(this);
					if (completionInstallResult != null)
						logger.debug("{}", completionInstallResult);

					settings.put(JPM_CONFIG_BIN, jpm.getBinDir().getAbsolutePath());
					settings.put(JPM_CONFIG_HOME, jpm.getHomeDir().getAbsolutePath());
					settings.save();

					if (jpm.getPlatform().hasPost()) {
						Command cmd = new Command();
						cmd.add(new File(jpm.getBinDir(), "jpm").getAbsolutePath());
						cmd.add("_postinstall");
						cmd.setTimeout(5, TimeUnit.SECONDS);

						StringBuffer stdout = new StringBuffer();
						StringBuffer stderr = new StringBuffer();
						System.out.println("post : " + cmd);
						int result = cmd.execute(stdout, stderr);
						if (result != 0) {
							Justif j = new Justif(80, 5, 10, 20);
							if (stdout.length() != 0)
								j.formatter().format("stdout: $-%n", stdout);
							if (stderr.length() != 0)
								j.formatter().format("stderr: $-%n", stderr);
							error("Failed to run platform init, exit code %s.%n%s", result, j.wrap());
						} else
							out.append(stdout);

					}
					out.println("Home dir      " + jpm.getHomeDir());
					out.println("Bin  dir      " + jpm.getBinDir());
				} else
					error("Cannot find the jpm jar from %s", f);
			} catch (InvocationTargetException e) {
				exception(e.getTargetException(), "Could not install jpm, %s", e.getTargetException());
				if (isExceptions())
					e.printStackTrace();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * Show platform
	 */
	@Arguments(arg = {
			"[cmd]", "..."
	})
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
		CommandLine cli = opts._command();
		List<String> cmds = opts._arguments();
		if (cmds.isEmpty()) {
			if (opts.verbose()) {
				Justif j = new Justif(80, 30, 40, 50, 60);
				jpm.getPlatform().report(j.formatter());
				out.append(j.wrap());
			} else
				out.println(jpm.getPlatform().getName());
		} else {
			String execute = cli.execute(jpm.getPlatform(), cmds.remove(0), cmds);
			if (execute != null) {
				out.append(execute);
			}
		}
	}

	/**
	 * Show all the installed VMs
	 */
	@Description("Manage installed VMs ")
	interface VMOptions extends Options {
		String add();
	}

	public void _vm(VMOptions opts) throws Exception {
		if (opts.add() != null) {
			File f = IO.getFile(base, opts.add()).getCanonicalFile();

			if (!f.isDirectory()) {
				error("No such directory %s to add a JVM", f);
			} else {
				jpm.addVm(f);
			}
		}

		SortedSet<JVM> vms = jpm.getVMs();

		for (JVM jvm : vms) {
			out.printf("%-30s %-5s %-20s %-30s %s\n", jvm.name, jvm.platformVersion, jvm.version, jvm.vendor, jvm.path);
		}

	}

	@Arguments(arg = {
			"service"
	})
	@Description("Start a service")
	interface startOptions extends Options {
		boolean clean();
	}

	/**
	 * Start a service.
	 * 
	 * @param options
	 * @throws Exception
	 */
	@Description("Start a service")
	public void _start(startOptions options) throws Exception {
		if (!jpm.hasAccess()) {
			error("No write acces, might require administrator or root privileges (sudo in *nix)");
			return;
		}
		for (String s : options._arguments()) {
			Service service = jpm.getService(s);
			if (service == null)
				error("Non existent service %s", s);
			else {
				if (!service.isRunning()) {
					try {
						ServiceData d = service.getServiceData();
						logger.debug("starting {} as user {}, lock={}, log={}", d.name, d.user, d.lock, d.log);
						if (options.clean())
							service.clear();
						String result = service.start();
						if (result != null)
							error("Failed to start: %s", result);
					} catch (Exception e) {
						exception(e, "Could not start service %s due to %s", s, e);
					}
				} else
					warning("Service %s already running", s);
			}
		}
	}

	@Arguments(arg = "service")
	@Description("Restart a service")
	public interface RestartOptions extends Options {}

	/**
	 * Restart a service.
	 * 
	 * @param options
	 * @throws Exception
	 */
	@Description("Restart a service")
	public void _restart(RestartOptions options) throws Exception {
		for (String s : options._arguments()) {
			Service service = jpm.getService(s);
			if (service == null)
				error("Non existent service %s", s);
			else {
				try {
					if (service.isRunning()) {
						service.stop();
					}

					String result = service.start();
					if (result != null)
						error("Failed to start: %s", result);
				} catch (Exception e) {
					exception(e, "Could not start service %s due to %s", s, e);
				}
			}
		}
	}

	@Arguments(arg = {
			"service", "[on|off]"
	})
	public interface traceOptions extends Options {
		boolean continuous();
	}

	/**
	 * Trace a service.
	 * 
	 * @param options
	 * @throws Exception
	 */
	@Description("Trace a service")
	public void _trace(traceOptions options) throws Exception {
		List<String> args = options._arguments();
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
			} catch (Exception e) {
				exception(e, "Could not trace service %s due to %s", s, e);
			}
		}
	}

	@Description("Stop a service")
	public interface StopOptions extends Options {}

	/**
	 * Stop a service.
	 * 
	 * @param options
	 * @throws Exception
	 */
	@Description("Stop a service")
	public void _stop(StopOptions options) throws Exception {
		for (String s : options._arguments()) {
			Service service = jpm.getService(s);
			if (service == null)
				error("Non existent service %s", s);
			else {
				if (service.isRunning()) {
					try {
						String result = service.stop();
						if (result != null)
							error("Failed to stop: %s", result);
					} catch (Exception e) {
						exception(e, "Could not stop service %s due to %s", s, e);
					}
				} else
					warning("Service %s not running", s);
			}
		}
	}

	@Description("Status of a service")
	@Arguments(arg = {
			"service", "[service]", "..."
	})
	interface statusOptions extends Options {
		@Description("Prints status for the service(s) every second")
		boolean continuous();
	}

	/**
	 * Status a service.
	 * 
	 * @param options
	 * @throws InterruptedException
	 */
	@Description("Status of a service/services")
	public void _status(statusOptions options) throws InterruptedException {
		while (true) {
			for (String s : options._arguments()) {
				String runs = "false";
				String status = "no service";
				try {
					Service service = jpm.getService(s);
					if (service != null) {
						runs = service.isRunning() + "";
						status = service.status();
					}
				} catch (Exception e) {
					status = e.toString();
					exception(e, "could not fetch status information from service %s, due to %s", s, status);
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

	@Arguments(arg = {})
	@Description("Show the current version. The qualifier represents the build date.")
	interface VersionOptions extends Options {

	}

	/**
	 * Show the current version
	 * 
	 * @throws IOException
	 */
	@Description("Show the current version of jpm")
	public void _version(VersionOptions options) throws IOException {
		Enumeration<URL> urls = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			logger.debug("found manifest {}", url);
			Manifest m = new Manifest(url.openStream());
			String name = m.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
			if (name != null && name.trim().equals("biz.aQute.jpm.run")) {
				out.println(m.getMainAttributes().getValue(Constants.BUNDLE_VERSION));
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
	@Description("Show the service log")
	interface logOptions extends Options {
		@Description("Shows new lines in the service log as they are written")
		boolean tail();

		@Description("Reset the log file for the service")
		boolean clear();

	}

	@Description("Show the service log")
	public void _log(logOptions opts) throws Exception {

		String s = opts._arguments().isEmpty() ? null : opts._arguments().get(0);
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
			IO.delete(logFile);
			logFile.createNewFile();
		}

		try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
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
	@Description("Manage user settings of jpm (in ~/.jpm). Without argument, print the current settings. "
			+ "Can alse be used to create change a settings with \"jpm settings <key>=<value>\"")
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
			logger.debug("settings {}", opts.clear());
			List<String> rest = opts._arguments();

			if (opts.clear()) {
				settings.clear();
				logger.debug("clear {}", settings.entrySet());
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
					byte[] data = s.getBytes(UTF_8);
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
					logger.debug("try {}", s);
					if (m.matches()) {
						logger.debug("matches {} {} {}", s, m.group(1), m.group(2));
						String key = m.group(1);
						Instructions instr = new Instructions(key);
						Collection<String> select = instr.select(settings.keySet(), true);

						String value = m.group(2);
						if (value == null) {
							logger.debug("list wildcard {} {} {}", instr, select, settings.keySet());
							list(select, settings);
						} else {
							logger.debug("assignment 	");
							settings.put(key, value);
							set = true;
						}
					} else {
						err.printf("Cannot assign %s\n", s);

					}
				}
				if (set) {
					logger.debug("saving");
					settings.save();
				}
			}
		} catch (Exception e) {
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
	 * Turned out that StartSSL HTTPS certificates are not recognized by the
	 * Java certificate store. So we have a command to get the certificate chain
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

		@Description("Delete the given name")
		boolean delete();
	}

	@Description("Install a certificate that is trusted by this VM (persistent for the JVM!)")
	public void _certificate(CertificateOptions opts) throws Exception {
		if (!this.jpm.hasAccess())
			error("Must be administrator");

		if (opts.delete())
			InstallCert.deleteCert(opts._arguments().get(0),
					opts.secret() == null ? jpm.getPlatform().defaultCacertsPassword() : opts.secret(), opts.cacerts());
		else
			InstallCert.installCert(this, opts._arguments().get(0), opts.port() == 0 ? 443 : opts.port(),
					opts.secret() == null ? jpm.getPlatform().defaultCacertsPassword() : opts.secret(), opts.cacerts(),
					opts.install());
	}

	// void print(Iterable<RevisionRef> revisions) {
	// for (RevisionRef r : revisions) {
	// out.printf("%-40s %s %s\n", jpm.getCoordinates(r),
	// Hex.toHexString(r._id), (r.description == null ? ""
	// : r.description));
	// }
	// }

	void printPrograms(Iterable< ? extends Program> programs) {
		Justif j = new Justif(120, 40, 42, 100);
		StringBuilder sb = new StringBuilder();
		try (Formatter f = new Formatter(sb)) {
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
	}

	interface RevisionPrintOptions {
		@Description("Just show the coordinate only")
		boolean coordinate();

		@Description("Include the description field")
		boolean description();
	}

	void printRevisions(Iterable< ? extends Revision> revisions, RevisionPrintOptions po) {
		if (po.coordinate()) {
			for (Revision r : revisions) {
				out.println(new Coordinate(r));
			}
			return;
		}
		Justif j = new Justif(140, 40, 70, 82, 100, 120);
		try (Formatter f = j.formatter()) {
			for (Revision r : revisions) {
				f.format("[%s] ", r.phase.getIdentifier());
				if (r.groupId.equals(Library.OSGI_GROUP) || r.groupId.equals(Library.SHA_GROUP))
					f.format("%s ", r.artifactId);
				else
					f.format("%s:%s ", r.groupId, r.artifactId);

				f.format("\t0%s\t1%tF\t2%s", r.version, new Date(r.modified), Hex.toHexString(r._id));
				if (po.description() && r.description != null) {
					f.format("\n \t1%s", r.description);
				}

				f.format("\n");
			}
			out.println(j.wrap());
		}
	}

	@Description("Find programs and libraries corresponding to the given query")
	interface findOptions extends Options {

		/**
		 * Number of search items to skip
		 *
		 */
		@Description("Number of programs to skip")
		int skip();

		@Description("Maximum number of programs per listing")
		int limit();

	}

	@Description("Find programs and libraries corresponding to the given query")
	public void _find(findOptions opts) throws Exception {
		int skip = 0;
		int limit = 0;
		if (opts.skip() > 0)
			skip = opts.skip();
		if (opts.limit() > 0)
			limit = opts.limit();

		String q = new ExtList<String>(opts._arguments()).join(" ");
		List<Program> programs = jpm.find(q, skip, limit);
		printPrograms(programs);
	}

	/**
	 * Some window specific commands
	 */

	// enum Key {
	// // HKEY_LOCAL_MACHINE(WinRegistry.HKEY_LOCAL_MACHINE),
	// HKEY_CURRENT_USER(WinRegistry.HKEY_CURRENT_USER);
	//
	// int n;
	//
	// Key(int n) {
	// this.n = n;
	// }
	//
	// public int value() {
	// return n;
	// }
	// }
	//
	// @Arguments(arg = {
	// "key", "[property]"
	// })
	// interface winregOptions extends Options {
	// boolean localMachine();
	// boolean thirtytwo();
	// }
	//
	// @Description("Windows specific access to the registry")
	// public void _winreg(winregOptions opts) throws Exception {
	// List<String> _ = opts._();
	// String key = _.remove(0);
	// key = key.replace('/', '\\');
	// String property = _.isEmpty() ? null : _.remove(0);
	//
	// RegistryKey environment =
	// RegistryKey.HKEY_CURRENT_USER.getSubKey("Environment");
	// System.out.println(environment.exists());
	// System.out.println(environment.getString("Path"));
	//
	// //
	// //
	// // int n = opts.localMachine() ? WinRegistry.HKEY_LOCAL_MACHINE :
	// WinRegistry.HKEY_CURRENT_USER;
	// // //int wow = opts.thirtytwo() ? WinRegistry.KEY_WOW64_32KEY :
	// WinRegistry.KEY_WOW64_64KEY;
	// //
	// // List<String> keys = WinRegistry.readStringSubKeys(n, key);
	// // if (property == null) {
	// // Map<String,String> map = WinRegistry.readStringValues(n, key);
	// // out.println(map);
	// // } else {
	// // WinRegistry.readString(n, key, property);
	// // }
	// }

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
		File f = IO.getFile(base, options._arguments().get(0));
		if (f.isFile()) {
			error("No such file %s", f);
		}

	}

	/**
	 * Alternative for command -r {@code <commandName>}
	 */
	@Arguments(arg = {
			"command|service", "..."
	})
	@Description("Remove the specified command(s) or service(s) from the system")
	interface UninstallOptions extends Options {}

	@Description("Remove a command or a service from the system")
	public void _remove(UninstallOptions opts) throws Exception {
		if (!jpm.hasAccess()) {
			error("No write acces, might require administrator or root privileges (sudo in *nix)");
			return;
		}

		ArrayList<String> toDelete = new ArrayList<String>();

		ArrayList<String> names = new ArrayList<String>();
		List<CommandData> commands = jpm.getCommands();
		for (CommandData command : commands) {
			names.add(command.name);
		}
		List<ServiceData> services = jpm.getServices();
		for (ServiceData service : services) {
			names.add(service.name);
		}

		for (String pattern : opts._arguments()) {
			Glob glob = new Glob(pattern);
			for (String name : names) {
				if (glob.matcher(name).matches()) {
					toDelete.add(name);
				}
			}
		}

		int ccount = 0, scount = 0;

		for (String name : toDelete) {
			Service s = null;
			if (jpm.getCommand(name) != null) { // Try command first
				logger.debug("Corresponding command found, removing");
				jpm.deleteCommand(name);
				ccount++;

			} else if ((s = jpm.getService(name)) != null) { // No command
																// matching, try
																// service
				logger.debug("Corresponding service found, removing");
				s.remove();
				scount++;

			} else { // No match amongst commands & services
				error("No matching command or service found for: %s", name);
			}
		}
		out.format("%d command(s) removed and %d service(s) removed%n", ccount, scount);
	}

	@Arguments(arg = "markdown|bash-completion")
	@Description("Print additional files for jpm (markdown documentation or bash completion file) to the standard output.")
	interface GenerateOptions extends Options {}

	@Description("Generate additional files for jpm")
	public void _generate(GenerateOptions opts) throws Exception {

		if (opts._arguments().isEmpty()) {
			error("Syntax: jpm generate <markdown|bash-completion>");
			return;
		}

		String genType = opts._arguments().get(0);

		if (genType.equalsIgnoreCase("markdown")) {
			IO.copy(this.getClass().getResourceAsStream("/static/jpm_prefix.md"), out);

			CommandLine cl = new CommandLine(this);
			cl.generateDocumentation(this, out);
		} else if (genType.equalsIgnoreCase("bash-completion")) {
			jpm.getPlatform().parseCompletion(this, out);
			// IO.copy(this.getClass().getResourceAsStream("/aQute/jpm/platform/unix/jpm-completion.bash"),
			// out);
		} else {
			error("Syntax: jpm generate <markdown|bash-completion>");
			return;
		}

	}

	/**
	 * Constructor for testing purposes
	 */
	public Main(JustAnotherPackageManager jpm) throws UnsupportedEncodingException {
		this();
		this.jpm = jpm;
	}

	/**
	 * Get an artifact
	 */
	@Arguments(arg = {
			"coordinate"
	})
	@Description("Get an artifact")
	interface GetOptions extends Options {
		@Description("Specify an output file")
		String output();
	}

	public void _get(GetOptions options) throws Exception {
		String coord = options._arguments().get(0);
		String f = options.output();
		if (f == null)
			IO.copy(new URL(coord).openStream(), System.out);
		else {
			IO.copy(new URL(coord).openStream(), IO.getFile(f));
		}
	}

	@Arguments(arg = {
			"jar file | url", "..."
	})
	@Description("Provide information about a jar file")
	interface WhatOptions extends Options {
		@Description("Force one liner description for one jar")
		boolean shortinfo();

		@Description("Force long information for multiple jars")
		boolean longinfo();
	}

	@Description("Provide information about a jar file")
	public void _what(WhatOptions opts) throws Exception {
		if (opts._arguments().size() == 0) {
			error("Syntax: jpm what <jar file | url>");
			return;
		}

		String res;
		if (opts._arguments().size() == 1) {
			res = jpm.what(opts._arguments().get(0), opts.shortinfo());
			if (res != null) {
				out.println(res);
			} else {
				out.println("No information found for this file");
			}
		} else {
			ArrayList<String> fails = new ArrayList<String>();
			for (String key : opts._arguments()) {
				res = jpm.what(key, !opts.longinfo());
				if (res != null) {
					out.println(res);
				} else {
					fails.add(key);
				}
			}

			if (fails.size() > 0) {
				out.println("No information found for:");
				for (String fail : fails) {
					out.println(" - " + fail);
				}
			}
		}
	}

	@Description("Perform updates for installed commands and services. "
			+ "Without argument (and without the --all flag), list possible updates for all installed commands and services. "
			+ "With arguments, apply possible updates for the specified command(s) and/or service(s).")
	@Arguments(arg = {
			"[command|service]", "..."
	})
	interface UpdateOptions extends Options {
		@Description("Apply all possible updates")
		boolean all();

		@Description("Include staging versions for updates")
		boolean staged();
	}

	@Description("Perform updates for installed commands and services")
	public void _update(UpdateOptions opts) throws Exception {
		if (!jpm.hasAccess()) {
			error("No write acces, might require administrator or root privileges (sudo in *nix)");
			return;
		}

		ArrayList<String> refs = new ArrayList<String>();
		for (CommandData data : jpm.getCommands()) {
			refs.add(data.name);
		}
		for (ServiceData data : jpm.getServices()) {
			refs.add(data.name);
		}

		ArrayList<UpdateMemo> notFound = new ArrayList<JustAnotherPackageManager.UpdateMemo>();
		ArrayList<UpdateMemo> upToDate = new ArrayList<JustAnotherPackageManager.UpdateMemo>();
		ArrayList<UpdateMemo> toUpdate = new ArrayList<JustAnotherPackageManager.UpdateMemo>();

		ArrayList<CommandData> datas = new ArrayList<CommandData>();
		if (opts._arguments().size() == 0) {
			datas.addAll(jpm.getCommands());
			datas.addAll(jpm.getServices());
		} else {
			for (String pattern : opts._arguments()) {
				Glob glob = new Glob(pattern);
				for (String name : refs) {
					if (glob.matcher(name).matches()) {
						CommandData data = jpm.getCommand(name);
						if (data == null) {
							Service service = jpm.getService(name);
							if (service != null) {
								data = service.getServiceData();
							}
						}
						if (data != null) {
							datas.add(data);
						}
					}
				}

			}
		}

		for (CommandData data : datas) {
			jpm.listUpdates(notFound, upToDate, toUpdate, data, opts.staged());
		}

		if (opts.all() || opts._arguments().size() > 0) {
			for (UpdateMemo memo : toUpdate) {
				jpm.update(memo);
			}
			out.format("%d command(s) updated%n", toUpdate.size());
		} else {
			Justif justif = new Justif(100, 20, 50);
			StringBuilder sb = new StringBuilder();
			Formatter f = new Formatter(sb);

			if (upToDate.size() > 0) {
				f.format("Up to date:%n");
				for (UpdateMemo memo : upToDate) {
					if (memo.current instanceof ServiceData) {
						f.format(" - %s (service) \t0- %s%n", memo.current.name, memo.current.version);
					} else {
						f.format(" - %s \t0- %s%n", memo.current.name, memo.current.version);
					}
				}
				f.format("%n");
			}

			if (toUpdate.size() > 0) {
				f.format("Update available:%n");
				for (UpdateMemo memo : toUpdate) {
					if (memo.current instanceof ServiceData) {
						f.format(" - %s (service) \t0- %s \t1-> %s%n", memo.current.name, memo.current.version,
								memo.best.version);
					} else {
						f.format(" - %s \t0- %s \t1-> %s%n", memo.current.name, memo.current.version,
								memo.best.version);
					}
				}
				f.format("%n");
			}

			if (notFound.size() > 0) {
				if (opts.staged()) {
					f.format("Information not found (local install ?):%n");
				} else {
					f.format("Information not found (try including staging versions with the --staged (-s) flag)%n");
				}
				for (UpdateMemo memo : notFound) {
					if (memo.current instanceof ServiceData) {
						f.format(" - %s (service)%n", memo.current.name);
					} else {
						f.format(" - %s%n", memo.current.name);
					}

				}
			}

			if (toUpdate.size() > 0) {
				f.format(
						"%nIn order to apply all possible updates, run jpm update again with the --all (or -a) flag.%n");
			}
			f.flush();
			justif.wrap(sb);

			out.println(sb.toString());
			f.close();
		}
	}

	/**
	 * Show a list of candidates from a coordinate
	 */
	@Arguments(arg = "coordinate")
	@Description("Print out the candidates from a coordinate specification. A coordinate is:\n\n"
			+ "    coordinate \t0:\t1[groupId ':'] artifactId \n\t1[ '@' [ version ] ( '*' | '=' | '~' | '!')]\n"
			+ "    '*'        \t0:\t1Version, if specified, is treated as required prefix of the actual version. Sees MASTER | STAGING | LOCKED\n"
			+ "    '='        \t0:\t1Version, if specified, must match exactly. Sees MASTER\n"
			+ "    '~'        \t0:\t1Version, if specified, is treated as required prefix of the actual version. Sees all phases\n"
			+ "    '!'        \t0:\t1Version, if specified, is treated as required prefix of the actual version. Sees normally invisible phases")
	interface CandidateOptions extends Options, RevisionPrintOptions {

	}

	@Description("List the candidates for a coordinate")
	public void _candidates(CandidateOptions options) throws Exception {
		String c = options._arguments().get(0);

		if (!Coordinate.COORDINATE_P.matcher(c).matches()) {
			error("Not a proper coordinate %s", c);
			return;
		}

		printRevisions(jpm.getCandidates(new Coordinate(c)), options);
	}

	/**
	 * Start jpm as daemon
	 * 
	 * @throws Exception
	 */

	public void _daemon(Options opts) throws Exception {
		jpm.daemon();
	}

	@Arguments(arg = {})
	interface UseOptions extends Options {
		@Description("Forget the current settings.")
		boolean forget();

		@Description("Save settings.")
		boolean save();
	}

	@Description("Manage the current setting of the home directory and the bin directory. These settings can be set with the initial options -g/--global, -u/--user, and -h/--home")
	public void _use(UseOptions o) {
		out.println("Home dir      " + jpm.getHomeDir());
		out.println("Bin  dir      " + jpm.getBinDir());
		if (o.forget()) {
			settings.remove(JPM_CONFIG_BIN);
			settings.remove(JPM_CONFIG_HOME);
			settings.save();
		} else if (o.save()) {
			if (!jpm.getHomeDir().isDirectory()) {
				error("The current home directory is not a JPM directory, init to initialize it, this will save the permanent settings to use that directory by default");
				return;
			}
			settings.put(JPM_CONFIG_BIN, jpm.getBinDir().getAbsolutePath());
			settings.put(JPM_CONFIG_HOME, jpm.getHomeDir().getAbsolutePath());
			settings.save();
		}
	}

	/**
	 * Run the platform init
	 */

	public void __postinstall(Options opts) {
		jpm.doPostInstall();
	}

}

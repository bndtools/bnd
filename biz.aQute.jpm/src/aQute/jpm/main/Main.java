package aQute.jpm.main;

import static aQute.lib.io.IO.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.*;
import java.util.regex.*;

import javax.swing.*;

import aQute.bnd.header.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.service.RepositoryPlugin.PutResult;
import aQute.bnd.version.*;
import aQute.jpm.lib.*;
import aQute.jpm.lib.Service;
import aQute.jpm.platform.*;
import aQute.lib.base64.*;
import aQute.lib.collections.*;
import aQute.lib.data.*;
import aQute.lib.getopt.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.settings.*;
import aQute.libg.cryptography.*;
import aQute.libg.reporter.*;
import aQute.library.remote.*;
import aQute.service.library.*;
import aQute.service.library.Library.Find;
import aQute.service.library.Library.Revision;

@Description("Just Another Package Manager (for java™)\nMaintains a local repository of Java jars (apps or libs). Can automatically link these jars to an OS command or OS service.")
public class Main extends ReporterAdapter {
	static Pattern				ASSIGNMENT		= Pattern.compile("\\s*([-\\w\\d_.]+)\\s*(?:=\\s*([^\\s]+)\\s*)?");
	public final static Pattern	URL_PATTERN		= Pattern.compile("[a-zA-Z][0-9A-Za-z]{1,8}:.+");
	public final static Pattern	BSNID_PATTERN	= Pattern.compile("([-A-Z0-9_.]+?)(-\\d+\\.\\d+.\\d+)?",
														Pattern.CASE_INSENSITIVE);
	File						base			= new File(System.getProperty("user.dir"));
	Settings					settings		= new Settings();
	RemoteLibrary				library;

	/**
	 * Show installed binaries
	 */

	@Arguments(arg = {})
	@Description("List the repository information. Contains bsns, versions, and service.")
	public interface artifactOptions extends Options {
		String filter();
	}

	/**
	 * Show platform
	 */
	@Arguments(arg = {})
	@Description("Show platform specific information.")
	public interface platformOptions extends Options {}

	/**
	 * Services
	 */
	@Arguments(arg = {
		"[name]"
	})
	@Description("Services")
	public interface serviceOptions extends Options {

		String create();

		String args();

		String jvmargs();

		Version version();

		boolean force();

		String main();

		String log();

		String work();

		String user();

		String epilog();

		String prolog();

		boolean remove();

		@Description("If set, will be started at boot time after the given services have been started. Specify boot if there are no other dependencies.")
		List<String> after();
	}

	/**
	 * Commands
	 */
	@Arguments(arg = {
		"..."
	})
	@Description("Commands")
	public interface commandOptions extends Options {
		boolean remove();

	}

	/**
	 * Uninstall a binary.
	 */
	@Description("Uninstall a jar by bsn.")
	@Arguments(arg = {
			"bsn", "..."
	})
	public interface uninstallOptions extends Options {
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
	final PrintStream			err	= System.err;
	final PrintStream			out	= System.out;
	File						sm;
	private String				url;

	/**
	 * Default constructor
	 */

	public Main() {
		super(System.err);
	}

	/**
	 * Main entry
	 * 
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {
		Main jpm = new Main();
		jpm.run(args);
	}

	/**
	 * Main options
	 */

	@Description("Options valid for all commands. Must be given before sub command")
	interface jpmOptions extends Options {

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

		@Description("Remote library url (can also be permanently set with 'jpm set library.url=...'")
		String library();

		@Description("Cache directory, can als be permanently set with 'jpm set library.cache=...'")
		String cache();
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
		try {
			setExceptions(opts.exceptions());
			setTrace(opts.trace());
			setPedantic(opts.pedantic());

			if (opts.base() != null)
				base = IO.getFile(base, opts.base());

			url = opts.library();
			if (url == null)
				url = settings.get("library.url");

			File cacheDir;
			if (opts.cache() != null) {
				cacheDir = IO.getFile(base, opts.cache());
			} else if (settings.containsKey("library.cache")) {
				cacheDir = IO.getFile(base, settings.get("library.cache"));
			} else
				cacheDir = null;

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
	@Description("Install a jar into the repository. If the jar defines a number of headers it can also be installed as a command and/or a service. ")
	public interface installOptions extends Options {
		@Description("Ignore command and service information")
		boolean ignore();

		@Description("Force overwrite of existing command")
		boolean force();

		@Description("Require a master version even when version is specified")
		boolean master();

		@Description("Include staged revisions in the search")
		boolean staged();

		@Description("Ignore digest")
		boolean xdigest();

		@Description("Run service (if present) under the given user name, default is the name of the service")
		String user();

		/**
		 * If specified, will install a revision with the given name and version
		 * and then add any command/service to the system.
		 */
		String bsn();

		/**
		 * Specify a version range for the artifact.
		 * 
		 * @return
		 */
		Version version();

		/**
		 * Install a file and extra commands
		 */

		String local();

		@Description("The path to the log file")
		String path();

		@Description("Specify a command name, overrides the JPM-Command header")
		String command();
		

	}

	/**
	 * The command line command for install.
	 * 
	 * @param opts
	 *            The options for installing
	 */
	public void _install(installOptions opts) throws Exception {
		if (!jpm.hasAccess()) {
			error("No write acces, might require administrator or root privileges (sudo in *nix)");
			return;
		}

		List<String> _ = opts._();
		List<ArtifactData> list = new ArrayList<ArtifactData>();

		if (opts.bsn() != null) {
			String bsn = opts.bsn();
			if (!Verifier.isBsn(bsn)) {
				error("Not a valid bsn: %s", bsn);
				return;
			}
			Version version = opts.version();

			if (!_.isEmpty())
				error("If --bsn is specified than no other arguments are allowed: %s", opts._());

			ArtifactData artifact = jpm.artifact(bsn, version, opts.xdigest());
			if (artifact == null) {
				error("Cannot find %s-%s", bsn, version);
				return;
			}

			if (!isOk())
				return;

			list.add(artifact);
		} else if (opts.local() != null) {
			boolean artificial = false;
			File file = getFile(opts.local());
			if (file == null || !file.isFile()) {
				error("%s is not a file", opts.local());
				return;
			}
			Jar jar = new Jar(file);
			try {
				Manifest m = jar.getManifest();
				if (m == null) {
					m = new Manifest();
					jar.setManifest(m);
				}

				String bsn = jar.getBsn();
				String version = jar.getVersion();
				if (bsn == null || !Verifier.isBsn(bsn)) {
					bsn = SHA1.digest(file).asHex();
					warning("No bsn in jar, uses SHA %s as bsn", bsn);
					version = "1.0.0"; // always new version

					m.getMainAttributes().putValue("Bundle-ManifestVersion", "2");
					m.getMainAttributes().putValue("Bundle-SymbolicName", bsn);
					m.getMainAttributes().putValue("Bundle-Version", version);
					if ( opts.command() != null)
						m.getMainAttributes().putValue("JPM-Command", opts.command());
						
					File tmp = File.createTempFile("jpm", ",jar");
					jar.setDigestAlgorithms(new String[] {
							"SHA1", "MD5"
					});
					jar.write(tmp);
					tmp.deleteOnExit();
					file = tmp;
				} else {
					if (version == null)
						error("No version in %s", file);
					else if (!Verifier.isVersion(version)) {
						error("Invalid version %s in %s", version, file);
					}
				}
				if (!isOk())
					return;

				trace("putting %s", file);
				PutResult result = jpm.put(IO.stream(file), null);
				Parameters embedded = OSGiHeader.parseHeader(m.getMainAttributes().getValue("JPM-Embedded"));

				for (Map.Entry<String,Attrs> e : embedded.entrySet()) {
					Resource r = jar.getResource(e.getKey());
					if (r == null)
						error("Specifies JPM-Embedded but not such entry %s", e.getKey());
					else {
						PutResult r2 = jpm.put(r.openInputStream(), null);
					}
				}

				ArtifactData artifact = jpm.artifact(bsn, new Version(version), opts.xdigest());

				if (artifact == null) {
					error("Cannot find %s-%s", bsn, version);
					return;
				}
				if (!isOk())
					return;

				list.add(artifact);
			}
			finally {
				jar.close();
			}
		} else {

			for (String target : _) {
				try {

					Find<Revision> where = getLibrary().findRevision();
					where.capability("x-jpm", "x-jpm", target);

					ExtList<Revision> sl = new ExtList<Revision>(where);
					if (sl.size() == 0)
						error("cannot find %s", target);
					else {
						trace("found %s revisions", sl.size());

						Version version = opts.version();
						boolean staged = opts.staged();
						SortedMap<Version,Revision> candidates = new TreeMap<Version,Library.Revision>();

						for (Revision r : sl) {
							Version v = new Version(r.version);

							if (version != null) {
								if (version.compareTo(v) <= 0) {
									candidates.put(v, r);
								} else
									trace("skipping %s because not in range %s", v, version);
							} else if (r.phase.isListable() || staged) {
								candidates.put(v, r);
							} else
								trace("skipping %s because staged", v, version, staged);

						}
						if (candidates.isEmpty()) {
							Revision r = sl.get(sl.size() - 1);
							error("Not master revision but there are unstaged revisions for command '%s'. You can specify --staged or use --bsn %s --version %s",
									target, r.bsn, r.baseline);
						} else {
							Revision r = candidates.get(candidates.lastKey()); // get
																				// best
																				// match
							trace("Installing %s-%s %s %s", r.bsn, r.version, r.url, Hex.toHexString(r._id));

							ArtifactData artifact = jpm.artifact(r.bsn, new Version(r.baseline), opts.xdigest());
							if (artifact == null) {
								error("Cannot find %s-%s", r.bsn, r.version);
								continue;
							}

							trace("Got artifact");
							if (artifact.reason != null)
								error("Revision %s-%s has has troubles downloading: %s, sha=%s", r.bsn, r.baseline,
										artifact.reason, r._id);

							if (artifact.verify != null && !opts.xdigest())
								error("Revision %s-%s has has digest troubles: %s, sha=%s", r.bsn, r.baseline,
										artifact.reason, r._id);

							if (artifact.command == null && artifact.service == null)
								error("Revision %s-%s has no JPM command nor service (%s) ", r.bsn, r.baseline, r._id);

							if (!isOk())
								continue;

							trace("Artifact verified");
							if (!opts.force()) {

								if (artifact.command != null && artifact.command.installed)
									error("Command %s already exists, specify -f,--force if you want it installed",
											target);
								if (artifact.service != null && artifact.service.installed)
									error("Service %s already exists, specify -f,--force if you want it installed",
											target);
							}
							list.add(artifact);
						}
					}

				}
				catch (IOException e) {
					exception(e, "Could not install %s because %s", target, e);
				}
			}
		}
		// Now do the real work ...
		trace("artifacts %s", list);
		for (ArtifactData artifact : list) {

			if (artifact.command != null) {
				artifact.command.force = opts.force();
				trace("creating command %s", artifact.command.bsn);
				String error = jpm.createCommand(artifact.command);
				if (error != null)
					error("Could not create command: %s", error);
			}
			if (artifact.service != null) {
				if (opts.user() != null)
					artifact.service.user = opts.user();

				if (opts.path() != null)
					artifact.service.log = IO.getFile(base, opts.path()).getAbsolutePath();

				artifact.service.force = opts.force();
				trace("creating service %s", artifact.service.bsn);
				String error = jpm.createService(artifact.service);
				if (error != null)
					error("Could not create service: %s", error);
			}
		}
	}

	/**
	 * Put a file in the repository:
	 * 
	 * <pre>
	 * put
	 * </pre>
	 */

	@Arguments(arg = {
			"file", "..."
	})
	interface putOptions extends Options {
		boolean install();
	}

	public void _put(putOptions opts) throws Exception {
		for (String target : opts._()) {
			File file = getFile(target);
			if (file == null) {
				error("Cannot find file %s", file);
			} else {
				FileInputStream in = new FileInputStream(file);
				try {
					PutResult result = jpm.put(in, null);
					trace("put succesfully %s %s", result.artifact, Hex.toHexString(result.digest));
				}
				finally {
					in.close();
				}
			}
		}
	}

	/**
	 * Publish an artifact by staging it to the registry
	 * 
	 * @param opts
	 */
	// @Description("Stage a file to the repository. This command requires credentials")
	// @Arguments(arg = "file...")
	// interface stageOptions extends Options {
	// @Description("Scan directories only for these extensions")
	// Set<String> extensions();
	//
	// @Description("Staging message")
	// String message();
	//
	// @Description("Email address to use for staging (default is in settings)")
	// String owner();
	//
	// @Description("Identify this machine")
	// String id();
	//
	// @Description("Force scanning the artifact, even if it already exists")
	// boolean force();
	// }
	//
	// public void _stage(stageOptions opts) throws NoSuchAlgorithmException,
	// Exception {
	// if (!credentials(opts.owner(), opts.id()))
	// return;
	//
	// List<File> files = getFiles(opts._(), opts.extensions());
	//
	// for (File f : files) {
	// Jar jar = new Jar(f);
	// try {
	// Verifier v = new Verifier(jar);
	// try {
	// v.verify();
	// getInfo(v, f.getName());
	// }
	// finally {
	// v.close();
	// }
	// }
	// finally {
	// jar.close();
	// }
	// }
	// if (!isOk())
	// return;
	//
	// for (File f : files) {
	// StageRequest sr = new StageRequest();
	// sr.file = f;
	// sr.force = opts.force();
	// sr.message = opts.message();
	// sr.owner = opts.owner() == null ? settings.getEmail() : opts.owner();
	// StageResponse stage = getLibrary().stage(sr);
	// addErrors(f.getName(), stage.errors);
	// addWarnings(f.getName(), stage.warnings);
	// }
	// }
	//
	// private List<File> getFiles(List<String> paths, Set<String> extensions) {
	// List<File> files = new ArrayList<File>();
	// for (String p : paths) {
	// traverse(files, IO.getFile(base, p), extensions);
	// }
	// return files;
	// }

	// private void traverse(List<File> files, File file, Set<String>
	// extensions) {
	// if (file.isFile()) {
	// files.add(file);
	// } else if (file.isDirectory()) {
	// for (File sub : file.listFiles()) {
	//
	// if (extensions != null) {
	// int n = sub.getName().lastIndexOf('.');
	// String ext = sub.getName().substring(n + 1);
	// if (!extensions.contains(ext))
	// continue;
	// } else if (!file.getName().endsWith(".jar"))
	// continue;
	//
	// traverse(files, sub, extensions);
	// }
	// }
	// }
	//
	// private boolean credentials(String email, String machine) throws
	// Exception {
	// if (email == null)
	// email = settings.getEmail();
	// if (machine == null)
	// machine = InetAddress.getLocalHost() + "";
	//
	// if (email == null) {
	// error("email not set, use 'jpm set email=...");
	// return false;
	// }
	// getLibrary().credentials(email, machine, settings.getPublicKey(),
	// settings.getPrivateKey());
	// return isOk();
	// }

	/**
	 * List the repository, service, and commands
	 * 
	 * @param opts
	 * @throws Exception
	 */

	public void _artifacts(artifactOptions opts) throws Exception {
		List<String> list = jpm.list(opts.filter());
		for (String bsn : list) {
			List<Version> l = new ArrayList<Version>(jpm.versions(bsn));
			Collections.reverse(l);
			out.printf("%-40s %s%n", bsn, l);
		}
	}

	public void _service(serviceOptions opts) throws Exception {
		if (opts._().isEmpty()) {
			for (ServiceData sd : jpm.getServices())
				print(sd);
			return;
		}

		List<String> cmdline = opts._();
		String name = cmdline.remove(0);

		Service s = jpm.getService(name);
		ServiceData data;
		boolean update = false;
		if (s != null) {
			data = s.getServiceData();
			if (opts.remove()) {
				if (!jpm.hasAccess()) {
					error("No write access to remove service %s", name);
					return;
				}
				s.remove();
				return;
			}
		} else {
			if (opts.create() != null) {
				if (!jpm.hasAccess()) {
					error("No write access to create service %s", name);
					return;
				}

				update = true;
				data = new ServiceData();
				data.name = name;
				data.bsn = opts.create();
				data.force = opts.force();

				if (opts.version() != null)
					data.version = opts.version();

				File artifact = jpm.get(data.bsn, data.version, null);
				if (artifact == null) {
					error("No such artifact %s:%s", data.bsn, data.version);
					return;
				}

			} else {
				error("No such service %s", name);
				return;
			}
		}

		if (opts.main() != null) {
			data.main = opts.main();
			update = true;
		}
		if (opts.args() != null) {
			data.args = opts.args();
			update = true;
		}
		if (opts.jvmargs() != null) {
			data.jvmArgs = opts.jvmargs();
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

		if (update) {
			if (!jpm.hasAccess()) {
				error("No write access to update service %s", name);
				return;
			}
			data.force = true;
			if (s == null) {
				String result = jpm.createService(data);
				if (result != null) {
					error("Failed to create service %s, due to %s", name, result);
					return;
				}
				jpm.getService(name);
			} else {
				if (s.isRunning())
					warning("Changes will not affect the currently running process");

				String result = s.update(data);
				if (result != null) {
					error("Failed to update service %s, due to %s", name, result);
					return;
				}
			}
		}
		Data.details(data, out);
	}

	private void print(ServiceData sd) throws Exception {
		out.printf("%-40s %s-%s (%s) %s%n", sd.name, sd.bsn, sd.version,
				jpm.getService(sd.name).isRunning() ? "runs   " : "stopped", sd.args);
	}

	public void _command(commandOptions opts) throws Exception {

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
		} else {
			for (String name : opts._()) {
				CommandData data = jpm.getCommand(name);
				if (data == null) {
					error("Not found: %s", name);
				} else {
					out.printf("%-40s %s%n", data.name, data.description == null ? "" : data.description);
					out.printf("  %-38s %s%n", "Classpath", data.dependencies);
					out.printf("  %-38s %s-%s%n", "Name", data.bsn, data.version);
					out.printf("  %-38s %s%n", "JVM Args", data.jvmArgs);
					out.printf("  %-38s %s%n", "Main class", data.main);
					out.printf("  %-38s %s%n", "Time", new Date(data.time));
				}
			}
		}
	}

	/**
	 * Uninstall a bsn and any dependent commands
	 * 
	 * @param opts
	 * @throws Exception
	 */
	public void _uninstall(uninstallOptions opts) throws Exception {
		if (opts._().isEmpty())
			error("Uninstall requires at least one bsn");

		if (!jpm.hasAccess())
			error("No write acces, might require administrator or root privileges (sudo in *nix)");

		if (!isOk())
			return;

		Version version = opts.version();

		for (String bsn : opts._()) {
			jpm.delete(bsn, version);
		}

		jpm.gc();
	}

	public void _gc(@SuppressWarnings("unused")
	GCOptions opts) throws Exception {
		jpm.gc();
	}

	/**
	 * Remove all traces.
	 * 
	 * @param opts
	 * @throws Exception
	 */
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
	private void run(String[] args) throws Exception {
		jpm = new JustAnotherPackageManager(this);
		if (args.length > 0 && args[0].equals("daemon"))
			jpm.daemon();
		else {
			try {
				Method m = System.class.getMethod("console");
				Object o = m.invoke(null);
				if (o == null) {
					Icon icon = new ImageIcon(Main.class.getResource("/images/jpm.png"), "JPM");
					int answer = JOptionPane.showOptionDialog(null, "This is a command line application. Setup?",
							"Package Manager for Java(r)", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
							icon, null, null);
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
			}
			catch (Throwable t) {
				// Ignore, happens in certain circumstances, we fallback to the
				// command line
			}
			CommandLine cl = new CommandLine(this);
			ExtList<String> list = new ExtList<String>(args);
			String help = cl.execute(this, "jpm", list);
			check();
			if (help != null)
				err.println(help);
		}

	}

	/**
	 * Setup jpm to run on this system.
	 * 
	 * @throws Exception
	 */
	public void _init(@SuppressWarnings("unused")
	Options opts) throws Exception {
		String s = System.getProperty("java.class.path");
		if (s == null || s.indexOf(File.pathSeparator) > 0) {
			error("Cannot initialize because not clear what the command jar is from java.class.path: %s", s);
			return;
		}
		try {
			File f = new File(s).getAbsoluteFile();
			if (f.exists()) {
				CommandLine cl = new CommandLine(this);
				cl.execute(this, "install", Arrays.asList("-fl", f.getAbsolutePath()));
			} else
				error("Cannot find the jpm jar from %s", f);
		}
		catch (InvocationTargetException e) {
			exception(e, "Could not install jpm, %s", e.getMessage());
		}
	}

	public void _platform(@SuppressWarnings("unused")
	platformOptions opts) {
		out.println(jpm);
	}

	/**
	 * Start a service.
	 * 
	 * @param options
	 * @throws Exception
	 */
	interface startOptions extends Options {
		boolean clean();
	}

	public void _start(startOptions options) throws Exception {
		if (!jpm.canWrite()) {
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

	public void _version(Options options) throws IOException {
		Manifest m = new Manifest(getClass().getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF"));
		out.println(m.getMainAttributes().getValue("Bundle-Version"));
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

	private RemoteLibrary getLibrary() {
		if (library == null) {
			trace("using url %s", url);
			library = new RemoteLibrary(url);
		}
		return library;
	}

	/**
	 * Show the tail of the log output.
	 */

	@Arguments(arg = "service")
	interface logOptions extends Options {

		boolean tail();

		boolean clear();

	}

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

	/**
	 * Install JPM as a platform daemon that will start the services marked with
	 * a before (where boot is the canonical before).
	 */

	@Arguments(arg = {})
	interface registerOptions extends Options {
		@Description("Register for user login only")
		boolean user();
	}

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
	 * Turned out that StartSSL HTTPS certifcates are not recognized by the Java
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

	public void _certificate(CertificateOptions opts) throws Exception {
		if (!this.jpm.canWrite())
			error("Must be administrator");

		InstallCert.installCert(this, opts._().get(0), opts.port() == 0 ? 443 : opts.port(),
				opts.secret() == null ? Platform.getPlatform(this).defaultCacertsPassword() : opts.secret(),
				opts.cacerts(), opts.install());
	}
}

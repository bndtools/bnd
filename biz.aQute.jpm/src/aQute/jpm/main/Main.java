package aQute.jpm.main;

import static aQute.lib.io.IO.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.regex.*;

import javax.swing.*;

import aQute.bnd.header.*;
import aQute.bnd.version.*;
import aQute.impl.library.cache.*;
import aQute.impl.library.remote.*;
import aQute.jpm.lib.*;
import aQute.lib.collections.*;
import aQute.lib.data.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.libg.reporter.*;

@Description("Just Another Package Manager (for java™)\nMaintains a local repository of Java jars (apps or libs). Can automatically link these jars to an OS command or OS service.")
public class Main extends ReporterAdapter {

	public final static Pattern	URL_PATTERN	= Pattern.compile("[a-zA-Z][0-9A-Za-z]{1,8}:.+");
	File						base		= new File(System.getProperty("user.dir"));
	RemoteLibrary				library;
	LibraryCache				cache;

	/**
	 * Show installed binaries
	 */

	@Arguments(arg = {})
	@Description("List the repository information. Contains bsns, versions, and services.")
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
	}

	/**
	 * Commands
	 */
	@Arguments(arg = {})
	@Description("Commands")
	public interface commandOptions extends Options {}

	/**
	 * Uninstall a binary.
	 */
	@Description("Uninstall a jar by bsn.")
	@Arguments(arg = {
			"bsn", "..."
	})
	public interface uninstallOptions extends Options {
		@Description("Version range that must be matched, if not specified all versions are removed.")
		VersionRange range();

		@Description("Garbage collect command and services that do not point to a valid repo file")
		boolean gc();
	}

	/**
	 * Uninstall a binary.
	 */
	public interface deinitOptions extends Options {
		boolean force();
	}

	/**
	 * garbage collect commands and services
	 */
	@Description("Garbage collect any orphan services and commands")
	@Arguments(arg = {})
	public interface GCOptions extends Options {}

	JustAnotherPackageManager	jpm;
	final PrintStream			err	= System.err;
	final PrintStream			out	= System.out;
	File						sm;

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

		@Description("Use a local jpm directory.")
		String local();

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
System.out.println("hello");
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
		}
		
		catch (InvocationTargetException t) {
			Throwable tt = t;
			while ( tt instanceof InvocationTargetException)
				tt = ((InvocationTargetException)tt).getTargetException();

			exception(tt, "%s", tt.getMessage());			
		}
		catch (Throwable t) {
			exception(t, "Failed %s", t);
		}

		report(err);
		if (!check(opts.failok())) {
			System.exit(getErrors().size());
		}
	}

	/**
	 * Install a jar options
	 */
	@Arguments(arg = {
			"url|file", "..."
	})
	@Description("Install a jar into the repository. If the jar defines a number of headers it can also be installed as a command and/or a service. ")
	public interface installOptions extends Options {
		@Description("Ignore command and service information")
		boolean ignore();

		@Description("Force overwrite of existing command")
		boolean force();

		@Description("Verify digests in the JAR, provide algorithms. Default is MD5 and SHA1. A '-' ignores the digests.")
		String[] verify();

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
						}
						finally {
							tmp.delete();
						}
					}
					catch (MalformedURLException mfue) {
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

			}
			catch (IOException e) {
				exception(e, "Could not install %s because %s", target, e);
			}
		}
	}

	/**
	 * List the repository, services, and commands
	 * 
	 * @param opts
	 * @throws Exception
	 */

	public void _artifacts(artifactOptions opts) throws Exception {
		List<ArtifactData> ads = jpm.getArtifacts();
		Collections.sort(ads, new Comparator<ArtifactData>() {

			@Override
			public int compare(ArtifactData a, ArtifactData b) {
				return a.bsn.compareTo(b.bsn);
			}
		});

		for (ArtifactData artifact : ads)
			out.printf("%-40s %s%n", artifact.bsn, artifact.version);
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
		if (s != null)
			data = s.getServiceData();
		else {
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

				File artifact = jpm.getArtifact(data.bsn, data.version);
				if (artifact == null) {
					error("No such artifact %s:%s", data.bsn, data.version);
					return;
				}

				data.repoFile = artifact;
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
		if (opts.log() != null) {
			data.log = IO.getFile(base, opts.log());
			update = true;
		}
		if (opts.work() != null) {
			data.work = IO.getFile(base, opts.work());
			update = true;
		}

		if (update) {
			if (!jpm.hasAccess()) {
				error("No write access to update service %s", name);
				return;
			}
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
		out.printf("%-40s %s-%s (%s) %s%n", sd.name, sd.bsn, sd.version, jpm.getService(sd.name).isRunning(), sd.args);
	}

	public void _command(commandOptions opts) throws Exception {
		if (opts._().isEmpty()) {
			for (CommandData sd : jpm.getCommands())
				out.printf("%-40s %s-%s (%s)%n", sd.name, sd.bsn, sd.version, sd.repoFile);
			return;
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

		VersionRange range = opts.range();

		for (String bsn : opts._()) {
			jpm.uninstall(bsn, range);
		}

		jpm.gc();
	}

	public void _gc(@SuppressWarnings("unused") GCOptions opts) throws Exception {
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
		if (bsn == null)
			error("The JAR does not have a name (Bundle-SymbolicName header)");

		String v = main.getValue("Bundle-Version");
		Version version = null;

		if (v == null)
			error("The JAR does not have a version (Bundle-Version header)");
		else if (!v.matches(JustAnotherPackageManager.VERSION_PATTERN))
			error("Not a valid version: %s", v);
		else
			version = new Version(v);

		String mainClass = main.getValue("Main-Class");

		trace("install %s %s %s", bsn, version, mainClass);
		List<File> install = new ArrayList<File>();
		List<ServiceData> services = new ArrayList<ServiceData>();
		List<CommandData> commands = new ArrayList<CommandData>();

		{
			Parameters embedded = OSGiHeader.parseHeader(main.getValue("JPM-Embedded"));
			if (embedded != null) {
				for (String e : embedded.keySet()) {
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
		}

		if (!opts.ignore()) {
			Parameters service = OSGiHeader.parseHeader(main.getValue("JPM-Service"));
			for (Map.Entry<String,Attrs> e : service.entrySet()) {
				Attrs attrs = e.getValue();
				ServiceData data = new ServiceData();
				data.name = e.getKey();
				data.bsn = bsn;
				data.version = version;
				if (attrs.containsKey("args"))
					data.args = attrs.get("args");
				if (attrs.containsKey("jvmargs"))
					data.jvmArgs = attrs.get("jvmargs");

				data.force = opts.force();
				data.main = mainClass;
				services.add(data);
			}
			trace("services %s", services);
		}

		if (!opts.ignore()) {
			Parameters command = OSGiHeader.parseHeader(main.getValue("JPM-Command"));
			for (Map.Entry<String,Attrs> e : command.entrySet()) {
				Attrs attrs = e.getValue();
				CommandData data = new CommandData();
				data.bsn = bsn;
				data.version = version;
				data.name = e.getKey();
				data.jvmArgs = attrs.get("vmargs");
				data.force = opts.force();
				data.main = mainClass;
				commands.add(data);
			}
			trace("commands %s", commands);
		}

		try {
			String msg = jpm.verify(jar, opts.verify());
			if (msg != null) {
				error("The JAR %s fails to verify, %s. Use -v - to ignore verification", source, msg);
				return;
			}

			if (!opts.ignore()) {
				if (commands.size() > 0 && mainClass == null)
					error("JPM command is specified but JAR contains no Main-Class attribute for %s", source);

				if (services.size() > 0 && mainClass == null)
					error("JPM service is specified but JAR contains no Main-Class attribute for %s", source);
			}

			if (!isOk())
				return;

		}
		finally {
			jar.close();
		}

		for (File e : install)
			install(e, opts);

		File repoFile = jpm.install(source, bsn, version);

		if (!opts.ignore()) {
			for (CommandData data : commands) {
				data.repoFile = repoFile;
				trace("create command %s", data);
				String s = jpm.createCommand(data);
				if (s != null)
					error("During create command %s, %s", data, s);
				else
					out.println("Installed command " + data);
			}

			for (ServiceData data : services) {
				data.repoFile = repoFile;
				trace("create service %s", data);
				String s = jpm.createService(data);
				if (s != null)
					error("During create service %s, %s", data, s);
				else
					out.println("Installed service " + data);
			}
		}
	}

	/**
	 * Main entry for the command line
	 * 
	 * @param args
	 * @throws Exception
	 */
	private void run(String[] args) throws Exception {
		jpm = new JustAnotherPackageManager(this);
		try {
			Method m = System.class.getMethod("console");
			Object o = m.invoke(null);
			if (o == null) {
				Icon icon = new ImageIcon(Main.class.getResource("/images/jpm.png"), "JPM");
				int answer = JOptionPane.showOptionDialog(null, "This is a command line application. Setup?",
						"Package Manager for Java(r)", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, icon,
						null, null);
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

	/**
	 * Setup jpm to run on this system.
	 * 
	 * @throws Exception
	 */
	public void _init(@SuppressWarnings("unused") Options opts) throws Exception {
		String s = System.getProperty("java.class.path");
		if (s == null || s.indexOf(File.pathSeparator) > 0) {
			error("Cannot initialize because not clear what the command jar is from java.class.path: %s", s);
			return;
		}
		try {
			File f = new File(s).getAbsoluteFile();
			if (f.exists()) {
				CommandLine cl = new CommandLine(this);
				cl.execute(this, "install", Arrays.asList("-f", f.getAbsolutePath()));
			} else
				error("Cannot find the jpm jar from %s", f);
		}
		catch (InvocationTargetException e) {
			exception(e, "Could not install jpm, %s", e.getMessage());
		}
	}

	public void _platform(@SuppressWarnings("unused") platformOptions opts) {
		out.println(jpm);
	}

	/**
	 * Start a service.
	 * 
	 * @param options
	 * @throws Exception
	 */
	public void _start(Options options) throws Exception {
		for (String s : options._()) {
			Service service = jpm.getService(s);
			if (service == null)
				error("Non existent service %s", s);
			else {
				if (!service.isRunning()) {
					try {
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
	public interface traceOptions extends Options {}

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
	public void _status(Options options) {
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
			out.printf("%-40s %8s %s%n", s, runs, status);
		}
	}

	/**
	 * Show the current version
	 * @throws IOException 
	 */
	
	public void _version(Options options) throws IOException {
		Manifest m = new Manifest(getClass().getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF"));
		out.println( m.getMainAttributes().getValue("Bundle-Version"));
	}
	
	/**
	 * Search files in repository
	 */
	
	interface RepoOptions extends Options {
		URI url();
	}
	public void _repo( RepoOptions opts ) throws Exception {
		initRepo( opts.url());
		
		List<String> cmds = opts._();
		CommandLine proc = opts._command();
		proc.execute(new RepoCommand(this), cmds.remove(0), cmds);
	}

	private void initRepo(URI url) throws Exception {
		library = new RemoteLibrary();
		if ( url != null)
			library.url(url.toString());
		cache = new LibraryCache(library, null, false);
		cache.open();
	}
	
	
}

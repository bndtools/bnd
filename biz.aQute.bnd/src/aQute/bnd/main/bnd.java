package aQute.bnd.main;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.ProjectLauncher.LiveCoding;
import aQute.bnd.build.ProjectTester;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.enroute.commands.EnrouteCommand;
import aQute.bnd.enroute.commands.EnrouteOptions;
import aQute.bnd.exporter.subsystem.SubsystemExporter;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.help.Syntax;
import aQute.bnd.main.BaselineCommands.baseLineOptions;
import aQute.bnd.main.BaselineCommands.schemaOptions;
import aQute.bnd.main.DiffCommand.diffOptions;
import aQute.bnd.main.RepoCommand.repoOptions;
import aQute.bnd.maven.MavenCommand;
import aQute.bnd.maven.PomFromManifest;
import aQute.bnd.osgi.About;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Clazz.Def;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Macro;
import aQute.bnd.osgi.Packages;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.osgi.eclipse.EclipseClasspath;
import aQute.bnd.repository.maven.provider.NexusCommand;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.action.Action;
import aQute.bnd.service.repository.InfoRepository;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.util.home.Home;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import aQute.configurable.Config;
import aQute.lib.base64.Base64;
import aQute.lib.collections.ExtList;
import aQute.lib.collections.MultiMap;
import aQute.lib.collections.SortedList;
import aQute.lib.exceptions.Exceptions;
import aQute.lib.filter.Filter;
import aQute.lib.getopt.Arguments;
import aQute.lib.getopt.CommandLine;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.hex.Hex;
import aQute.lib.io.FileTree;
import aQute.lib.io.IO;
import aQute.lib.justif.Justif;
import aQute.lib.settings.Settings;
import aQute.lib.strings.Strings;
import aQute.lib.tag.Tag;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.classdump.ClassDumper;
import aQute.libg.cryptography.MD5;
import aQute.libg.cryptography.SHA1;
import aQute.libg.cryptography.SHA256;
import aQute.libg.cryptography.SHA512;
import aQute.libg.forker.Forker;
import aQute.libg.generics.Create;
import aQute.libg.glob.Glob;
import aQute.libg.qtokens.QuotedTokenizer;
import aQute.libg.reporter.ReporterAdapter;
import aQute.libg.reporter.ReporterMessages;
import aQute.libg.sed.Sed;
import aQute.service.reporter.Reporter;

/**
 * Utility to make bundles. @version $Revision: 1.14 $
 */
public class bnd extends Processor {
	private static Logger						logger					= LoggerFactory.getLogger(bnd.class);
	private final static Pattern				ASSIGNMENT				= Pattern.compile(							//
		"([^=]+) (= ( ?: (\"|'|) (.+) \\3 )? ) ?", Pattern.COMMENTS);
	Settings									settings				= new Settings(
		Home.getUserHomeBnd() + "/settings.json");
	final PrintStream							err						= System.err;
	final public PrintStream					out						= System.out;
	Justif										justif					= new Justif(80, 40, 42, 70);
	BndMessages									messages				= ReporterMessages.base(this,
		BndMessages.class);
	private Workspace							ws;
	private char[]								password;
	private Workspace							workspace;

	private static final ThreadLocal<Boolean>	noExit					= new ThreadLocal<Boolean>() {
																			@Override
																			protected Boolean initialValue() {
																				return false;
																			};
																		};
	private static final String					DEFAULT_LOG_LEVEL_KEY	= "org.slf4j.simpleLogger.defaultLogLevel";

	private final static Pattern				JARCOMMANDS				= Pattern
		.compile("(cv?0?(m|M)?f?)|(uv?0?M?f?)|(xv?f?)|(tv?f?)|(i)");

	private final static Pattern				COMMAND					= Pattern.compile("\\w{2,}");
	static final String							BND_BND					= "**/bnd.bnd";
	static final String							BNDRUN_ALL				= "**/*.bndrun";

	interface verboseOptions extends Options {
		@Description("prints more processing information")
		boolean verbose();

	}

	/**
	 * Project command, executes actions.
	 */

	@Description("Execute a Project action, or if no parms given, show information about the project")
	interface projectOptions extends Options {
		@Description("Identify another project")
		String project();
	}

	interface workspaceOptions extends Options {

		@Description("Use the following workspace")
		String workspace();
	}

	interface excludeOptions extends Options {
		@Description("Exclude files by pattern")
		String[] exclude();
	}

	interface ProjectWorkspaceOptions extends workspaceOptions, projectOptions, verboseOptions, excludeOptions {

	}

	interface HandledProjectWorkspaceOptions {

		List<File> files();

		Workspace workspace();
	}

	@Description("OSGi Bundle Tool")
	interface bndOptions extends Options {
		@Description("Turns errors into warnings so command always succeeds")
		boolean failok();

		@Description("Report errors pedantically")
		boolean pedantic();

		@Description("Print out stack traces when there is an unexpected exception")
		boolean exceptions();

		@Description("Redirect output")
		File output();

		@Description("Use as base directory")
		String base();

		@Description("Trace progress")
		boolean trace();

		@Description("Show log debug output")
		boolean debug();

		@Description("Error/Warning ignore patterns")
		String[] ignore();

		@Description("Provide a settings password")
		char[] secret();

		@Description("Use the workspace related to the default directory as parent for bnd. If the current directory is not related to a workspace this option is silently ignored")
		boolean workspace();

	}

	public bnd(Workspace ws) {
		super(ws);
	}

	public bnd() {}

	public static void main(String args[]) throws Exception {
		Workspace.setDriver(Constants.BNDDRIVER_BND);
		Workspace.addGestalt(Constants.GESTALT_SHELL, null);

		try (bnd main = new bnd()) {
			main.start(args);
		}
		exitWithCode(0);
	}

	/**
	 * For testing
	 */
	static void mainNoExit(String args[], Path baseExecDir) throws Exception {
		noExit.set(true);// extra in test

		Workspace.setDriver(Constants.BNDDRIVER_BND);
		Workspace.addGestalt(Constants.GESTALT_SHELL, null);

		try (bnd main = new bnd()) {
			main.setBase(baseExecDir.toFile());
			main.start(args); // extra in test
		}
		exitWithCode(0);
	}

	public void start(String args[]) throws Exception {
		CommandLine cl = new CommandLine(this);
		String help = cl.execute(this, "bnd", new ExtList<>(args));
		check();
		if (help != null)
			err.println(help);
	}

	/**
	 * Rewrite the command line to mimic the jar command
	 *
	 * @param args
	 * @throws Exception
	 */
	private void rewrite(List<String> args) throws Exception {
		if (args.isEmpty())
			return;

		String arg = args.get(0);
		if (arg.equals("maven")) {
			// ensure that we do not much with options
			// because the maven command does not like
			// that
			args.add(0, "maven");
			return;
		}
		if (arg.equals("-version")) {
			args.set(0, "version");
			return;
		}
		Matcher m = JARCOMMANDS.matcher(arg);
		if (m.matches()) {
			rewriteJarCmd(args);
			return;
		}

		// Project project = getProject();
		// if (project != null) {
		// Action a = project.getActions().get(arg);
		// if (a != null) {
		// args.add(0, "project");
		// }
		// }

		m = COMMAND.matcher(args.get(0));
		if (!m.matches()) {
			args.add(0, "do");
		}

	}

	private void rewriteJarCmd(List<String> args) {
		String jarcmd = args.remove(0);

		char cmd = jarcmd.charAt(0);
		switch (cmd) {
			case 'c' :
				args.add(0, "create");
				break;

			case 'u' :
				args.add(0, "update");
				break;

			case 'x' :
				args.add(0, "extract");
				break;

			case 't' :
				args.add(0, "type");
				break;

			case 'i' :
				args.add(0, "index");
				break;
		}
		int start = 1;
		for (int i = 1; i < jarcmd.length(); i++) {
			switch (jarcmd.charAt(i)) {
				case 'v' :
					args.add(start++, "--verbose");
					break;

				case '0' :
					args.add(start++, "--nocompression");
					break;

				case 'm' :
					args.add(start++, "--manifest");
					start++; // make the manifest file the parameter
					break;

				case 'M' :
					args.add(start++, "--nomanifest");
					break;

				case 'f' :
					args.add(start++, "--file");
					break;
			}
		}
	}

	/**
	 * Main command. This has options the bnd base options and will then run
	 * another command.
	 *
	 * @param options
	 * @throws Exception
	 */
	@Description("The swiss army tool for OSGi")
	public void _bnd(bndOptions options) throws Exception {
		try {
			setProperty(FAIL_OK, options.failok() + "");
			setExceptions(options.exceptions());
			setTrace(options.trace());
			doLogging(options);

			workspace = Workspace.findWorkspace(IO.work);
			if (workspace != null) {
				logger.debug("Using workspace {}", workspace);
				workspace.use(this);
				if (options.workspace() && workspace != null) {
					this.setParent(workspace);
				}
			}

			setPedantic(options.pedantic());

			if (options.base() != null)
				setBase(getFile(getBase(), options.base()));

			// And the properties
			for (Entry<String, String> entry : options._properties()
				.entrySet()) {
				setProperty(entry.getKey(), entry.getValue());
			}

			CommandLine handler = options._command();
			List<String> arguments = options._arguments();

			// Rewrite command line to match jar commands and
			// handle commands that provide file names

			rewrite(arguments);

			logger.debug("rewritten {}", arguments);

			if (arguments.isEmpty()) {
				Justif f = new Justif(80, 20, 22, 72);
				handler.help(f.formatter(), this);
				err.append(f.wrap());
			} else {
				String cmd = arguments.remove(0);
				String help = handler.execute(this, cmd, arguments);
				if (help != null) {
					err.println(help);
				}
			}

			if (options.secret() != null) {
				password = options.secret();
				settings.load(password);
			}
		} catch (Throwable t) {
			t = Exceptions.unrollCause(t, InvocationTargetException.class);
			exception(t, "%s", t);
		}
		out.flush();
		err.flush();
		if (ws != null)
			getInfo(ws);
		if (workspace != null)
			getInfo(workspace);

		if (!check(options.ignore())) {
			err.flush();
			err.flush();
			Thread.sleep(1000);
			exitWithCode(getErrors().size());
		}
	}

	/**
	 * Setup SLF4J logging level.
	 *
	 * @param options
	 */
	private void doLogging(bndOptions options) {
		try {
			int level;
			if (options.debug()) {
				System.setProperty(DEFAULT_LOG_LEVEL_KEY, "debug");
				level = org.slf4j.spi.LocationAwareLogger.DEBUG_INT;
			} else {
				System.setProperty(DEFAULT_LOG_LEVEL_KEY, "warn");
				level = org.slf4j.spi.LocationAwareLogger.WARN_INT;
			}
			Field field = org.slf4j.impl.SimpleLogger.class.getDeclaredField("CONFIG_PARAMS");
			field.setAccessible(true);
			Object CONFIG_PARAMS = field.get(null);
			field = org.slf4j.impl.SimpleLoggerConfiguration.class.getDeclaredField("defaultLogLevel");
			field.setAccessible(true);
			field.set(CONFIG_PARAMS, level);

			field = org.slf4j.impl.SimpleLogger.class.getDeclaredField("currentLogLevel");
			field.setAccessible(true);
			field.set(logger, level);
		} catch (Exception e) {
			e.printStackTrace();
			// ignore
		}
		logger.debug("Setup logger");
	}

	/**
	 * Options for the jar create command.
	 */
	@Description("Equivalent jar command c[v0mf] command (supports the jar tool's syntax). Will wrap the bundle unless --wrapnot is specified")
	interface createOptions extends Options {
		@Description("Verbose (v option)")
		boolean verbose();

		@Description("No compression (0 option)")
		boolean nocompression();

		@Description("No manifest (M option)")
		boolean Manifest();

		@Description("Use manifest (m option)")
		String manifest();

		@Description("Jar file (f option)")
		String file();

		@Description("directory")
		String cdir();

		@Description("Wrap")
		boolean wrap();

		@Description("Properties for wrapping")
		String properties();

		@Description("Bundle Symbolic Name for wrap")
		String bsn();

		@Description("Bundle Version for wrap")
		Version version();

		@Description("Force write event if there are errors")
		boolean force();
	}

	/**
	 * Create jar file
	 *
	 * <pre>
	 *  jar c[v0M]f jarfile [-C dir] inputfiles [-Joption]
	 * jar c[v0]mf manifest jarfile [-C dir] inputfiles [-Joption] jar c[v0M]
	 * [-C dir] inputfiles [-Joption] jar c[v0]m manifest [-C dir] inputfiles
	 * [-Joption]
	 * </pre>
	 *
	 * @param options
	 * @throws Exception
	 */
	@Description("Create jar, used to support backward compatible java jar commands")
	public void _create(createOptions options) throws Exception {
		Jar jar = new Jar("dot");

		File dir = getBase().getAbsoluteFile();
		String sdir = options.cdir();
		if (sdir != null)
			dir = getFile(sdir);

		if (options._arguments()
			.isEmpty())
			add(jar, dir, ".", options.verbose());
		else
			for (String f : options._arguments()) {
				f = IO.normalizePath(f);
				add(jar, dir, f, options.verbose());
			}

		String manifest = options.manifest();
		if (manifest != null) {
			if (options.verbose())
				err.printf("Adding manifest from %s\n", manifest);

			jar.setManifest(getFile(manifest));
		}

		if (options.Manifest()) {
			jar.setManifest((Manifest) null);
		} else {
			if (options.wrap()) {
				Analyzer w = new Analyzer(this);
				addClose(w);
				w.setBase(getBase());
				w.use(this);
				w.setDefaults(options.bsn(), options.version());
				w.calcManifest();
				getInfo(w);
				w.setJar((Jar) null);
				w.close();
			}
		}

		if (options.nocompression())
			jar.setCompression(Jar.Compression.STORE);

		if (isOk()) {
			String jarFile = options.file();
			if (jarFile == null)
				jar.write(out);
			else
				jar.write(jarFile);
		}
		jar.close();

	}

	/**
	 * Helper for the jar create function, adds files to the jar
	 *
	 * @param jar
	 * @param base
	 * @param path
	 * @param report
	 * @throws IOException
	 */
	private void add(Jar jar, File base, String path, boolean report) throws IOException {
		if (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);

		File f;
		if (path.equals("."))
			f = base;
		else
			f = getFile(base, path);

		err.printf("Adding: %s\n", path);

		if (f.isFile()) {
			jar.putResource(path, new FileResource(f));
		} else if (f.isDirectory()) {
			if (path.equals("."))
				path = "";
			else
				path += "/";

			String[] subs = f.list();
			for (String sub : subs) {

				add(jar, base, path + sub, report);
			}
		}
	}

	/**
	 * Extract a file from the JAR
	 */

	@Description("Extract files from a JAR file, equivalent jar command x[vf] (syntax supported)")
	interface extractOptions extends Options {
		@Description("Verbose (v option)")
		boolean verbose();

		@Description("Jar file (f option)")
		String file();

		@Description("Directory where to store")
		String cdir();
	}

	@Description("Extract files from a JAR file, equivalent jar command x[vf] (syntax supported)")
	public void _extract(extractOptions opts) throws Exception {
		Jar jar;

		if (opts.file() != null) {
			File f = getFile(opts.file());
			if (!f.isFile()) {
				messages.NoSuchFile_(f);
				return;
			}
			jar = new Jar(f);
		} else {
			jar = new Jar("cin", System.in);
		}
		try {
			Instructions instructions = new Instructions(opts._arguments());
			Collection<String> selected = instructions.select(jar.getResources()
				.keySet(), true);
			File store = getBase();
			if (opts.cdir() != null)
				store = getFile(opts.cdir());

			IO.mkdirs(store);
			Jar.Compression compression = jar.hasCompression();
			for (String path : selected) {
				if (opts.verbose())
					err.printf("%8s: %s\n", compression.toString()
						.toLowerCase(), path);

				File f = getFile(store, path);
				File pf = f.getParentFile();
				IO.mkdirs(pf);
				Resource r = jar.getResource(path);
				IO.copy(r.openInputStream(), f);
			}
		} finally {
			jar.close();
		}
	}

	/**
	 * List the contents of the JAR
	 */

	@Description("List files int a JAR file, equivalent jar command t[vf] (syntax supported)")
	interface typeOptions extends Options {
		@Description("Verbose (v option)")
		boolean verbose();

		@Description("Jar file (f option)")
		String file();

	}

	@Description("List files int a JAR file, equivalent jar command t[vf] (syntax supported)")
	public void _type(typeOptions opts) throws Exception {
		Jar jar;

		if (opts.file() != null) {
			File f = getFile(opts.file());
			if (!f.isFile()) {
				messages.NoSuchFile_(f);
				return;
			}
			jar = new Jar(f);
		} else {
			jar = new Jar("cin", System.in);
		}

		try {
			Instructions instructions = new Instructions(opts._arguments());
			Collection<String> selected = instructions.select(jar.getResources()
				.keySet(), true);

			for (String path : selected) {
				if (opts.verbose()) {
					Resource r = jar.getResource(path);
					err.printf("%8s %-32s %s\n", r.size(), new Date(r.lastModified()), path);
				} else
					err.printf("%s\n", path);
			}
		} finally {
			jar.close();
		}
	}

	@Description("Execute a file based on its extension. Supported extensions are: bnd (build), bndrun (run), and jar (print)")
	interface dooptions extends Options {
		@Description("The output file")
		String output();

		@Description("Force even when there are errors")
		boolean force();
	}

	/**
	 * The do command interprets files and does a default action for each file
	 *
	 * @throws Exception
	 */
	@Description("Execute a file based on its extension. Supported extensions are: bnd (build), bndrun (run), and jar (print)")
	public void _do(dooptions options) throws Exception {
		for (String path : options._arguments()) {
			if (path.endsWith(Constants.DEFAULT_BND_EXTENSION)) {
				build(options.output(), options.force(), path);
			} else if (path.endsWith(Constants.DEFAULT_JAR_EXTENSION)
				|| path.endsWith(Constants.DEFAULT_BAR_EXTENSION)) {
				try (Jar jar = getJar(path)) {
					doPrint(jar, MANIFEST, null);
				}
			} else if (path.endsWith(Constants.DEFAULT_BNDRUN_EXTENSION)) {
				doRun(Arrays.asList(path), false, null);
			} else
				messages.UnrecognizedFileType_(path);
		}
	}

	public void build(String dest, boolean force, String path) throws IOException, Exception {
		try (Builder b = new Builder()) {

			File f = getFile(path);
			if (!f.isFile()) {
				error("No such file %s", f);
				return;
			}

			b.use(this);
			b.setProperties(f);

			List<Builder> subs = b.getSubBuilders();

			for (Builder bb : subs) {
				logger.debug("building {}", bb.getPropertiesFile());
				bb.build();
				File out = bb.getOutputFile(dest);
				getInfo(bb, bb.getBsn() + ": ");
				if (isOk()) {
					bb.save(out, force);
				}
				getInfo(bb, bb.getBsn() + ": "); // pickup any save errors
				if (!isOk()) {
					IO.delete(out);
				}
			}
		}
	}

	@Description("Execute a Project action, or if no parms given, show information about the project")
	public void _project(projectOptions options) throws Exception {
		Project project = getProject(options.project());
		if (project == null) {
			messages.NoProject();
			return;
		}

		List<String> l = new ArrayList<>(options._arguments());
		if (l.isEmpty()) {
			err.printf("Name         %s\n", project.getName());
			err.printf("Actions      %s\n", project.getActions()
				.keySet());
			err.printf("Directory    %s\n", project.getBase());
			err.printf("Depends on   %s\n", project.getDependson());
			try (ProjectBuilder pb = project.getBuilder(null)) {
				err.printf("Sub builders %s\n", pb.getSubBuilders());
			}
			return;
		}

		String cmd = null;
		String arg = null;

		if (!l.isEmpty())
			cmd = l.remove(0);
		if (!l.isEmpty())
			arg = l.remove(0);

		if (!l.isEmpty()) {
			messages.MoreArgumentsThanNeeded_(options._arguments());
			return;
		}

		if (cmd == null) {
			messages.NoCommandForProject(project);
			return;
		}

		Action a = project.getActions()
			.get(cmd);
		if (a != null) {
			a.execute(project, arg);
			getInfo(project);
			return;
		}
	}

	@Description("Bumps the version of a project. Will take the current version and then increment "
		+ "with a major, minor, or micro increment. The default bump is minor.")
	@Arguments(arg = "<major|minor|micro>")
	interface bumpoptions extends Options {
		@Description("Path to another project than the current project")
		String project();
	}

	/**
	 * Bump a version number
	 *
	 * @throws Exception
	 */
	@Description("Bumps the version of a project")
	public void _bump(bumpoptions options) throws Exception {
		Project project = getProject(options.project());

		if (project == null) {
			messages.NoProject();
			return;
		}

		String mask = null;
		if (!options._arguments()
			.isEmpty()) {
			mask = options._arguments()
				.get(0);
			if (mask.equalsIgnoreCase("major"))
				mask = "+00";
			else if (mask.equalsIgnoreCase("minor"))
				mask = "=+0";
			else if (mask.equalsIgnoreCase("micro"))
				mask = "==+";
			else if (!mask.matches("[+=0]{1,3}")) {
				messages.InvalidBumpMask_(mask);
				return;
			}
		}

		if (mask == null)
			project.bump();
		else
			project.bump(mask);

		getInfo(project);
		err.println(project.getProperty(BUNDLE_VERSION, "No version found"));
	}

	interface PerProject {
		void doit(Project p) throws Exception;
	}

	public void perProject(ProjectWorkspaceOptions opts, PerProject run) throws Exception {
		perProject(opts, run, true);
	}

	public void perProject(ProjectWorkspaceOptions opts, PerProject run, boolean manageDeps) throws Exception {
		List<Project> projects = getFilteredProjects(opts);

		final Set<Project> projectsWellDone = new HashSet<>();

		for (Project p : projects) {
			if (manageDeps) {
				final Collection<Project> projectDeps = p.getDependson(); // ordered
				if (opts.verbose()) {
					out.println("Project dependencies for: " + p.getName());
					projectDeps.forEach(pr -> out.println(
						" + " + pr.getName() + " " + (projectsWellDone.contains(pr) ? "<handled before>" : "")));
				}

				projectDeps.removeAll(projectsWellDone);

				for (Project dep : projectDeps) {
					run.doit(dep);
					projectsWellDone.add(dep);
				}
			}

			run.doit(p);

			getInfo(p, p + ": ");
		}
	}

	private List<Project> getFilteredProjects(ProjectWorkspaceOptions opts) throws Exception {
		List<Project> projects = new ArrayList<>();

		HandledProjectWorkspaceOptions hpw = handleOptions(opts, "**/bnd.bnd");

		Workspace ws = hpw.workspace();

		for (File file : hpw.files()) {

			Project p = null;
			if (Files.isDirectory(file.toPath())) {

				p = ws.getProjectFromFile(file);

			} else if (Project.BNDFILE.equals(file.getName())) {

				p = ws.getProjectFromFile(file.getParentFile());

			}
			if (p != null) {
				projects.add(p);
			}
		}
		return projects;
	}

	@Description("Build a project. This will create the jars defined in the bnd.bnd and sub-builders.")
	interface buildoptions extends ProjectWorkspaceOptions {

		@Description("Build for test")
		boolean test();

	}

	@Description("Build a project. This will create the jars defined in the bnd.bnd and sub-builders.")
	public void _build(final buildoptions opts) throws Exception {

		perProject(opts, p -> p.build(opts.test()));
	}

	interface CompileOptions extends ProjectWorkspaceOptions {

		@Description("Compile for test")
		boolean test();

	}

	@Description("Compile a project or the workspace")
	public void _compile(final CompileOptions opts) throws Exception {
		perProject(opts, p -> p.compile(opts.test()));
	}

	@Description("Test a project according to an OSGi test")
	@Arguments(arg = {
		"testclass[:method]..."
	})
	interface testOptions extends ProjectWorkspaceOptions {
		@Description("Verify all the dependencies before launching (runpath, runbundles, testpath)")
		boolean verify();

		@Description("Launch the test even if this bundle does not contain " + Constants.TESTCASES)
		boolean force();

		@Description("Set the -testcontinuous flag")
		boolean continuous();

		@Description("Set the -runtrace flag")
		boolean trace();
	}

	@Description("Test a project according to an OSGi test")
	public void _test(final testOptions opts) throws Exception {

		perProject(opts, project -> {
			List<String> testNames = opts._arguments();
			if (!testNames.isEmpty())
				project.setProperty(TESTCASES, "");

			if (project.is(NOJUNITOSGI) && !opts.force()) {
				warning("%s is set to true on this bundle. Use -f/--force to try this test anyway", NOJUNITOSGI);
				return;
			}

			if (project.getProperty(TESTCASES) == null)
				if (opts.force())
					project.setProperty(TESTCASES, "");
				else {
					warning(
						"No %s set on this bundle. Use -f/--force to try this test anyway (this works if another bundle provides the testcases)",
						TESTCASES);
					return;
				}

			if (opts.continuous())
				project.setProperty(TESTCONTINUOUS, "true");

			if (opts.trace() || isTrace())
				project.setProperty(RUNTRACE, "true");

			project.test(testNames);
		});

	}

	@Description("Test a project with plain JUnit")
	public void _junit(testOptions opts) throws Exception {

		perProject(opts, p -> p.junit());
	}

	private boolean verifyDependencies(Project project, boolean implies, boolean test) throws Exception {
		if (!implies) {
			return true;
		}

		project.verifyDependencies(test);
		getInfo(project);
		if (isOk())
			return true;

		return false;
	}

	@Description("Run a project in the OSGi launcher. If not bndrun is specified, the current project is used for the run specification")
	@Arguments(arg = "[bndrun]")
	interface runOptions extends Options {
		@Description("Path to another project than the current project. Only valid if no bndrun is specified")
		String project();

		@Description("Verify all the dependencies before launching (runpath, runbundles)")
		boolean verify();
	}

	@Description("Run a project in the OSGi launcher")
	public void _run(runOptions opts) throws Exception {
		doRun(opts._arguments(), opts.verify(), opts.project());
	}

	private void doRun(List<String> args, boolean verify, String project) throws Exception {
		Project run = null;

		if (args.isEmpty()) {
			run = getProject(project);
			if (run == null) {
				messages.NoProject();
				return;
			}
		} else {
			File f = getFile(args.get(0));

			File dir = f.getParentFile();
			File wsdir = dir.getParentFile();

			if (wsdir == null) {
				// We are in the filesystem root?? Create a standalone run.
				run = Run.createRun(null, f);
			} else {
				Workspace workspace = Workspace.getWorkspaceWithoutException(wsdir);
				run = Run.createRun(workspace, f);
			}
		}
		verifyDependencies(run, verify, false);
		ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		try (ProjectLauncher pl = run.getProjectLauncher();
			LiveCoding liveCoding = pl.liveCoding(ForkJoinPool.commonPool(), scheduledExecutor)) {
			pl.setTrace(run.isTrace() || run.isRunTrace());
			pl.launch();
		} catch (Exception e) {
			messages.Failed__(e, "Running " + run);
		} finally {
			scheduledExecutor.shutdownNow();
		}
		getInfo(run);
		getInfo(run.getWorkspace());
	}

	@Description("Clean a project")
	interface cleanOptions extends ProjectWorkspaceOptions {

	}

	@Description("Clean a project or workspace")
	public void _clean(cleanOptions opts) throws Exception {
		perProject(opts, p -> p.clean());
	}

	@Description("Access the internal bnd database of keywords and options")
	@Arguments(arg = {
		"header|instruction", "..."
	})
	interface syntaxOptions extends Options {
		@Description("The width of the printout")
		int width();
	}

	@Description("Access the internal bnd database of keywords and options")
	public void _syntax(syntaxOptions opts) throws Exception {
		int w = opts.width() < 80 ? 120 : opts.width();
		Justif justif = new Justif(w, opts.width(), 40, 42, w - 10);
		List<String> args = opts._arguments();
		StringBuilder sb = new StringBuilder();
		Formatter f = new Formatter(sb);
		for (String s : args) {
			f.format(" \n[%s]\n", s);
			Syntax sx = Syntax.HELP.get(s);
			if (s == null)
				f.format("Unknown");
			else {
				print(f, sx, "  ");
			}
		}
		f.flush();
		justif.wrap(sb);
		err.println(sb);
	}

	private void print(Formatter f, Syntax sx, String indent) {
		if (sx == null)
			return;

		f.format("%s%s\n\n", indent, sx.getLead());
		if (sx.getValues() != null)
			f.format("%sValues\t1:\t2%s\n", indent, sx.getValues());

		if (sx.getPattern() != null)
			f.format("%sPattern  \t1:\t2%s\n", indent, sx.getPattern());
		if (sx.getExample() != null)
			f.format("%sExample  \t1:\t2%s\n", indent, sx.getExample());
		if (sx.getChildren() != null) {

			for (Syntax child : sx.getChildren()) {
				f.format("\n%s[%s]\t1:\t2", indent, child.getHeader());
				print(f, child, indent + "  ");
			}
		}
	}

	@Description("Package a bnd or bndrun file into a single jar that executes with java -jar <>.jar. The JAR contains all dependencies, including the framework and the launcher. "
		+ "A profile can be specified which will be used to find properties. If a property is not found, a property with the name [<profile>]NAME will be looked up. This allows "
		+ "you to make different profiles for testing and runtime.")
	@Arguments(arg = {
		"<bnd|bndrun>", "[...]"
	})
	interface packageOptions extends Options {
		@Description("Where to store the resulting file. Default the name of the bnd file with a .jar extension.")
		String output();

		@Description("Profile name. Default no profile")
		String profile();
	}

	/**
	 * Package a bnd or bndrun file for packaging.
	 *
	 * @throws Exception
	 */
	@Description("Package a bnd or bndrun file into a single jar that executes with java -jar <>.jar")
	public void _package(packageOptions opts) throws Exception {
		List<String> cmdline = opts._arguments();
		File output = null;

		if (opts.output() != null) {
			output = getFile(opts.output());
		} else
			output = getBase();

		if (opts._arguments()
			.size() > 1) {
			IO.mkdirs(output);
		} else {
			File pf = output.getParentFile();
			IO.mkdirs(pf);
		}

		String profile = opts.profile() == null ? "exec" : opts.profile();

		if (cmdline.isEmpty())
			cmdline.add(Project.BNDFILE); // default project itself

		for (String path : cmdline) {
			Run run;

			File file = getFile(path);
			if (file.isDirectory())
				file = new File(file, Project.BNDFILE);

			if (!file.isFile()) {
				messages.NoSuchFile_(file);
				continue;
			}

			File dir = file.getParentFile();
			File workspaceDir = dir.getParentFile();
			if (workspaceDir == null) {
				// We are in the filesystem root?? Create a standalone run.
				run = Run.createRun(null, file);
			} else {
				Workspace ws = Workspace.getWorkspaceWithoutException(workspaceDir);
				run = Run.createRun(ws, file);
			}

			// Tricky because we can be run inside the context of a
			// project (in which case
			// we need to inherit from the project or outside.

			run.setProperty(PROFILE, profile);
			run.use(this);
			try {
				Jar jar = run.pack(profile);
				path = path.replaceAll(".bnd(run)?$", "") + ".jar";
				File out = output;
				if (output.isDirectory())
					out = new File(output, path);
				jar.write(out);
				jar.close();
			} catch (Exception e) {
				messages.ForProject_File_FailedToCreateExecutableException_(run, path, e);
			}
			getInfo(run);
		}
	}

	/**
	 * List all deliverables for this workspace.
	 */
	@Description("Show all deliverables from this workspace. with their current version and path.")
	@Arguments(arg = {})
	interface deliverableOptions extends Options {
		@Description("Path to project, default current directory")
		String project();

		@Description("Only provide deliverables of this project")
		boolean limit();
	}

	@Description("Show all deliverables from this workspace. with their current version and path.")
	public void _deliverables(deliverableOptions options) throws Exception {
		Project project = getProject(options.project());
		if (project == null) {
			messages.NoProject();
			return;
		}

		Collection<Project> projects;
		if (options.limit())
			projects = Arrays.asList(project);
		else
			projects = project.getWorkspace()
				.getAllProjects();

		List<Container> containers = new ArrayList<>();

		for (Project p : projects) {
			containers.addAll(p.getDeliverables());
		}

		for (Container c : containers) {
			Version v = new Version(c.getVersion());
			err.printf("%-40s %8s  %s\n", c.getBundleSymbolicName(), v.getWithoutQualifier(), c.getFile());
		}
		getInfo(project);
	}

	@Description("Show macro value. Macro can contain the ${ and } parentheses but it is also ok without. You can use the ':' instead of the ';' in a macro")
	@Arguments(arg = {
		"<macro>", "[...]"
	})
	interface macroOptions extends Options {
		@Description("Path to project, default current directory")
		String project();
	}

	/**
	 * Show the value of a macro
	 *
	 * @throws Exception
	 */
	@Description("Show macro value")
	public void _macro(macroOptions options) throws Exception {
		Processor project = getProject(options.project());

		if (project == null)
			project = ws;

		if (project == null) {
			messages.NoProject();
			return;
		}

		StringBuilder sb = new StringBuilder();
		Macro r = project.getReplacer();
		getInfo(project);

		String del = "";
		for (String s : options._arguments()) {
			if (!s.startsWith("${")) {
				s = "${" + s;
			}
			if (!s.endsWith("}")) {
				s += "}";
			}
			s = s.replace(':', ';');
			String p = r.process(s);
			sb.append(del);
			sb.append(p);
			del = " ";
		}
		getInfo(project);
		err.println(sb);
	}

	@Description("Release this project")
	interface releaseOptions extends Options {
		@Description("Path to project, default is current project")
		String project();

		@Description("Release with test build")
		boolean test();

		@Description("Set the release repository")
		String repo();

		@Description("Release all bundles in in the workspace")
		boolean workspace();

	}

	/**
	 * Release the project
	 *
	 * @throws Exception
	 */
	@Description("Release this project")
	public void _release(releaseOptions options) throws Exception {
		Set<Project> projects = new LinkedHashSet<>();

		Workspace ws = Workspace.findWorkspace(getBase());
		if (ws == null) {
			error("Workspace option was specified but cannot find a workspace from %s", getBase());
			return;
		}

		if (options.workspace()) {
			projects.addAll(ws.getAllProjects());
		}

		Project project = getProject(options.project());
		if (project != null) {
			projects.add(project);
		}

		if (projects.isEmpty()) {
			error("Cannot find any projects");
			return;
		}

		String repo = options.repo();
		if (repo != null) {
			RepositoryPlugin repository = ws.getRepository(repo);
			if (repository == null) {
				error("No such release repo %s%nFound:%n%s", repository, Strings.join("\n", ws.getRepositories()));
			}

		}
		for (Project p : projects) {
			if (repo != null) {
				p.setProperty(Constants.RELEASEREPO, repo);
			}
			p.release(options.test());
		}
		if (project != null) {
			getInfo(project);
		}
	}

	@Description("Show a cross references for all classes in a set of jars.")
	public void _xref(XRefCommand.xrefOptions options) throws IOException, Exception {
		XRefCommand cx = new XRefCommand(this);
		cx.xref(options);
	}

	@Description("Show info about the current directory's eclipse project")
	interface eclipseOptions extends workspaceOptions {
		@Description("Path to the project")
		String dir();
	}

	@Description("Eclipse")
	public void _eclipse(eclipseOptions options) throws Exception {

		List<String> arguments = options._arguments();

		File dir = getBase();
		if (options.dir() != null)
			dir = getFile(options.dir());

		if (!dir.isDirectory())
			error("Eclipse requires a path to a directory: %s", dir.getAbsolutePath());

		if (!arguments.isEmpty()) {
			try (EclipseCommand c = new EclipseCommand(this)) {
				String result = options._command()
					.subCmd(options, c);
				if (result != null) {
					out.println(result);
				}
				getInfo(c);
			}
			return;
		} else {

			if (!isOk())
				return;

			File cp = new File(dir, ".classpath");
			if (!cp.exists()) {
				error("Cannot find .classpath in project directory: %s", dir.getAbsolutePath());
			} else {
				EclipseClasspath eclipse = new EclipseClasspath(this, dir.getParentFile(), dir);
				err.println("Classpath    " + eclipse.getClasspath());
				err.println("Dependents   " + eclipse.getDependents());
				err.println("Sourcepath   " + eclipse.getSourcepath());
				err.println("Output       " + eclipse.getOutput());
				err.println();
			}
		}
	}

	/**
	 * Buildx
	 */
	final static int	BUILD_SOURCES	= 1;
	final static int	BUILD_POM		= 2;
	final static int	BUILD_FORCE		= 4;

	@Description("Build project, is deprecated but here for backward compatibility. If you use it, you should know how to use it so no more info is provided.")
	interface buildxOptions extends Options {
		String output();

		List<String> classpath();

		List<String> sourcepath();

		boolean eclipse();

		boolean noeclipse();

		boolean sources();

		boolean pom();

		boolean force();
	}

	@Description("Build project, is deprecated but here for backward compatibility")
	public void _buildx(buildxOptions options) throws Exception {

		// Create a build order

		List<Builder> builders = new ArrayList<>();
		List<String> order = new ArrayList<>();
		List<String> active = new ArrayList<>();

		for (String s : options._arguments()) {
			prebuild(active, order, builders, s);
		}

		for (Builder b : builders) {
			if (options.classpath() != null) {
				for (String f : options.classpath()) {
					b.addClasspath(getFile(f));
				}
			}

			if (options.sourcepath() != null) {
				for (String f : options.sourcepath()) {
					b.addSourcepath(getFile(f));
				}
			}

			if (options.sources())
				b.setSources(true);

			if (options.eclipse()) {
				EclipseClasspath ep = new EclipseClasspath(this, getBase().getParentFile(), getBase());

				b.addClasspath(ep.getClasspath());
				b.addClasspath(ep.getBootclasspath());
				b.addSourcepath(ep.getSourcepath());
			}

			Jar jar = b.build();

			File outputFile = b.getOutputFile(options.output());

			if (options.pom()) {
				Resource r = new PomFromManifest(jar.getManifest());
				jar.putResource("pom.xml", r);
				String path = outputFile.getName()
					.replaceAll("\\.jar$", ".pom");
				if (path.equals(outputFile.getName()))
					path = outputFile.getName() + ".pom";
				File pom = new File(outputFile.getParentFile(), path);
				try (OutputStream os = IO.outputStream(pom)) {
					r.write(os);
				}
			}

			getInfo(b, b.getPropertiesFile()
				.getName());
			if (isOk()) {
				b.save(outputFile, options.force());
			}
			b.close();
		}
	}

	// Find the build order
	// by recursively passing
	// through the builders.
	private void prebuild(List<String> set, List<String> order, List<Builder> builders, String s) throws IOException {
		if (order.contains(s)) // Already done
			return;

		if (set.contains(s))
			error("Cyclic -prebuild dependency %s from %s", s, set);

		Builder b = new Builder(this);
		b.setProperties(getFile(s));

		String prebuild = b.get("prebuild");
		if (prebuild != null) {
			set.add(s);
			try {
				Collection<String> parts = split(prebuild);
				for (String p : parts) {
					prebuild(set, order, builders, p);
				}
			} finally {
				set.remove(s);
			}
		}
		order.add(s);
		builders.add(b);
	}

	@Description("View a resource from a JAR file. Manifest will be pretty printed and class files are shown disassembled.")
	@Arguments(arg = {
		"<jar-file>", "<resource>", "[...]"
	})
	interface viewOptions extends Options {
		@Description("Character set to use for viewing")
		String charset();
	}

	/**
	 * View files from JARs We parse the commandline and print each file on it.
	 *
	 * @throws Exception
	 */
	@Description("View a resource from a JAR file.")
	public void _view(viewOptions options) throws Exception {
		Charset charset = UTF_8;
		if (options.charset() != null)
			charset = Charset.forName(options.charset());

		if (options._arguments()
			.isEmpty()) {
			error("Need a jarfile as source");
			return;
		}
		List<String> args = options._arguments();
		File file = getFile(args.remove(0));
		if (!file.isFile()) {
			error("File does not exist %s", file);
			return;
		}

		try (Jar jar = new Jar(file)) {
			if (args.isEmpty())
				args.add("*");

			Instructions instructions = new Instructions(args);
			Collection<String> selected = instructions.select(jar.getResources()
				.keySet(), true);
			for (String selection : selected) {
				Resource r = jar.getResource(selection);

				if (selection.endsWith(".MF")) {
					Manifest m = new Manifest(r.openInputStream());
					printManifest(m);
				} else if (selection.endsWith(".class")) {
					ClassDumper clsd = new ClassDumper(selection, r.openInputStream());
					clsd.dump(out);
				} else {
					InputStreamReader isr = new InputStreamReader(r.openInputStream(), charset);
					IO.copy(isr, out);
				}
			}
		}
	}

	@Description("Wrap a jar into a bundle. This is a poor man's facility to "
		+ "quickly turn a non-OSGi JAR into an OSGi bundle. "
		+ "It is usually better to write a bnd file and use the bnd <file>.bnd "
		+ "command because that has greater control. Even better is to wrap in bndtools.")
	@Arguments(arg = {
		"<jar-file>", "[...]"
	})
	interface wrapOptions extends Options {
		@Description("Path to the output, default the name of the input jar with the '.bar' extension. If this is a directory, the output is place there.")
		String output();

		@Description("A file with properties in bnd format.")
		String properties();

		@Description("A classpath specification")
		List<String> classpath();

		@Description("Allow override of an existing file")
		boolean force();

		@Description("Set the bundle symbolic name to use")
		String bsn();

		@Description("Set the version to use")
		Version version();
	}

	/**
	 * Wrap a jar to a bundle.
	 *
	 * @throws Exception
	 */
	@Description("Wrap a jar")
	public void _wrap(wrapOptions options) throws Exception {
		List<File> classpath = Create.list();
		File properties = getBase();

		if (options.properties() != null) {
			properties = getFile(options.properties());
		}

		if (options.classpath() != null)
			for (String cp : options.classpath()) {
				classpath.add(getFile(cp));
			}

		for (String j : options._arguments()) {
			File file = getFile(j);
			if (!file.isFile()) {
				error("File does not exist %s", file);
				continue;
			}

			try (Analyzer wrapper = new Analyzer(this)) {
				wrapper.use(this);

				for (File f : classpath)
					wrapper.addClasspath(f);

				wrapper.setJar(file);

				File outputFile = wrapper.getOutputFile(options.output());
				if (outputFile.getCanonicalFile()
					.equals(file.getCanonicalFile())) {
					// #267: CommandLine wrap deletes target even if file equals
					// source
					error("Output file %s and source file %s are the same file, they must be different", outputFile,
						file);
					return;
				}
				IO.delete(outputFile);

				String stem = file.getName();
				if (stem.endsWith(".jar"))
					stem = stem.substring(0, stem.length() - 4) + ".bnd";

				File p = getPropertiesFile(properties, file, stem);

				if (p == null) {
					wrapper.setImportPackage("*;resolution:=optional");
					wrapper.setExportPackage("*");
					warning("Using defaults for wrap, which means no export versions");

				} else if (p.isFile())
					wrapper.setProperties(p);
				else {
					error("No valid property file: %s", p);
				}

				if (options.bsn() != null)
					wrapper.setBundleSymbolicName(options.bsn());

				if (options.version() != null)
					wrapper.setBundleVersion(options.version());

				Manifest m = wrapper.calcManifest();

				if (wrapper.isOk()) {
					wrapper.getJar()
						.setManifest(m);
					wrapper.save(outputFile, options.force());
				}
				getInfo(wrapper, file.toString());
			}
		}
	}

	private File getPropertiesFile(File properties, File file, String stem) {
		if (properties.isFile())
			return properties;

		File p = getFile(file.getParentFile(), stem);
		if (p.isFile())
			return p;

		if (properties.isDirectory()) {
			p = getFile(properties, stem);
			if (p.isFile())
				return p;
		}

		return null;
	}

	@Description("Show a lot of info about the project you're in")
	interface debugOptions extends Options {
		@Description("Path to a project, default is current directory")
		String project();

		@Description("Show the flattened properties")
		boolean flattened();
	}

	/**
	 * Printout all the variables in scope.
	 *
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Description("Show a lot of info about the project you're in")
	public void _debug(debugOptions options) throws Exception {
		Project project = getProject(options.project());
		Justif justif = new Justif(120, 40, 50, 52, 80);

		logger.debug("using {}", project);
		Processor target = project;
		if (project != null) {
			Workspace ws = project.getWorkspace();

			ws.checkStructure();

			getInfo(project.getWorkspace());

			report(justif, "Workspace", project.getWorkspace());
			report(justif, "Project", project);

			try (ProjectBuilder pb = project.getBuilder(null)) {
				List<Builder> builders = pb.getSubBuilders();
				if (builders != null) {
					for (Builder sub : builders) {
						report(justif, "Sub-Builder", sub);
						getInfo(sub);
					}
				}
			}

			for (File file : project.getBase()
				.listFiles()) {
				if (file.getName()
					.endsWith(Constants.DEFAULT_BNDRUN_EXTENSION)) {
					Run run = Workspace.getRun(file);
					if (run == null) {
						error("No such run file %s", file);
					} else {
						report(justif, "bndrun", run);
						getInfo(run);
					}
				}
			}

			if (options.flattened()) {
				@SuppressWarnings("rawtypes")
				Map fp = project.getFlattenedProperties();
				Justif j = new Justif(140, 40, 44, 48, 100);
				j.table(fp, "-");
				out.println(j.wrap());
			}

			getInfo(project.getWorkspace());
			getInfo(project);

		} else
			err.println("No project");

	}

	private void report(Justif justif, String string, Processor processor) throws Exception {
		Map<String, Object> table = new LinkedHashMap<>();
		processor.report(table);
		Justif j = new Justif(140, 40, 44, 48, 100);
		j.formatter()
			.format("$-\n%s %s\n$-\n", string, processor);
		j.table(table, "-");
		out.println(j.wrap());
		out.println();
	}

	/**
	 * Manage the repo.
	 *
	 * <pre>
	 *  out.println(&quot; bnd repo [--repo|-r ('maven'|
	 * &lt;dir&gt;)]*&quot;); out.println(&quot; repos # list the
	 * repositories&quot;); out.println(&quot; list # list all content (not
	 * always possible)&quot;); out.println(&quot; get &lt;bsn&gt;
	 * &lt;version&gt; &lt;file&gt;? # get an artifact&quot;);
	 * out.println(&quot; put &lt;file&gt;+ # put in artifacts&quot;);
	 * out.println(&quot; help&quot;);
	 * </pre>
	 */

	@Description("Manage the repositories")
	public void _repo(repoOptions opts) throws Exception {
		new RepoCommand(this, opts);
	}

	/**
	 * Run enroute commands
	 */

	@Description("OSGi enRoute commands to maintain bnd workspaces (create workspace, add  project, etc)")
	public void _enroute(EnrouteOptions opts) throws Exception {
		new EnrouteCommand(this, opts);
	}

	/**
	 * Print out a JAR
	 */

	final static int	VERIFY			= 1;

	final static int	MANIFEST		= 2;

	final static int	LIST			= 4;

	final static int	IMPEXP			= 16;
	final static int	USES			= 32;
	final static int	USEDBY			= 64;
	final static int	COMPONENT		= 128;
	final static int	METATYPE		= 256;
	final static int	API				= 512;
	final static int	CAPABILITIES	= 1024;
	static final int	HEX				= 0;

	@Arguments(arg = "jar-file...")
	@Description("Provides detailed view of the bundle. It will analyze the bundle and then show its contents from different perspectives. If no options are specified, prints the manifest.")
	interface printOptions extends Options {
		@Description("Print the api usage. This shows the usage constraints on exported packages when only public API is used.")
		boolean api();

		@Description("Before printing, verify that the bundle is correct.")
		boolean verify();

		@Description("Print the manifest.")
		boolean manifest();

		@Description("List the resources")
		boolean list();

		@Description("List the imports exports, versions and ranges")
		boolean impexp();

		@Description("Show for each contained package, what other package it uses. Is either an private, exported, or imported package")
		boolean uses();

		@Description("Transposed uses. Will show for each known package who it is used by.")
		boolean by();

		@Description("Show components in detail")
		boolean component();

		@Description("Show any metatype data")
		boolean typemeta();

		@Description("Keep references to java in --api, --uses, and --usedby.")
		boolean java();

		@Description("Show all packages, not just exported, in the API view")
		boolean xport();

		@Description("Show the capabilities")
		boolean capabilities();
	}

	@Description("Printout the JAR")
	public void _print(printOptions options) throws Exception {
		for (String s : options._arguments()) {
			int opts = 0;
			if (options.verify())
				opts |= VERIFY;

			if (options.manifest())
				opts |= MANIFEST;

			if (options.api())
				opts |= API;

			if (options.list())
				opts |= LIST;

			if (options.impexp())
				opts |= IMPEXP;

			if (options.uses())
				opts |= USES;

			if (options.by())
				opts |= USEDBY;

			if (options.component())
				opts |= COMPONENT;

			if (options.typemeta())
				opts |= METATYPE;

			if (options.capabilities())
				opts |= CAPABILITIES;

			if (opts == 0)
				opts = MANIFEST | IMPEXP;

			try (Jar jar = getJar(s)) {
				doPrint(jar, opts, options);
			}
		}
	}

	private void doPrint(Jar jar, int options, printOptions po) throws ZipException, IOException, Exception {
		if ((options & VERIFY) != 0) {
			try (Verifier verifier = new Verifier(jar)) {
				verifier.setPedantic(isPedantic());
				verifier.verify();
				getInfo(verifier);
			}
		}
		if ((options & MANIFEST) != 0) {
			Manifest manifest = jar.getManifest();
			if (manifest == null)
				warning("JAR has no manifest %s", jar);
			else {
				err.println("[MANIFEST " + jar.getName() + "]");
				printManifest(manifest);
			}
			out.println();
		}
		if ((options & IMPEXP) != 0) {
			out.println("[IMPEXP]");
			Manifest m = jar.getManifest();

			if (m != null) {
				Domain domain = Domain.domain(m);
				Parameters imports = domain.getImportPackage();
				Parameters exports = domain.getExportPackage();
				for (String p : exports.keySet()) {
					if (imports.containsKey(p)) {
						Attrs attrs = imports.get(p);
						if (attrs.containsKey(VERSION_ATTRIBUTE)) {
							exports.get(p)
								.put("imported-as", attrs.get(VERSION_ATTRIBUTE));
						}
					}
				}
				print(Constants.IMPORT_PACKAGE, new TreeMap<>(imports));
				print(Constants.EXPORT_PACKAGE, new TreeMap<>(exports));
			} else
				warning("File has no manifest");
		}
		if ((options & CAPABILITIES) != 0) {
			out.println("[CAPABILITIES]");
			Manifest m = jar.getManifest();
			Domain domain = Domain.domain(m);

			if (m != null) {
				Parameters provide = domain.getProvideCapability();
				Parameters require = domain.getRequireCapability();
				print(Constants.PROVIDE_CAPABILITY, new TreeMap<>(provide));
				print(Constants.REQUIRE_CAPABILITY, new TreeMap<>(require));
			} else
				warning("File has no manifest");
		}
		if ((options & (USES | USEDBY | API)) != 0) {
			out.println();
			try (Analyzer analyzer = new Analyzer()) {
				analyzer.setPedantic(isPedantic());
				analyzer.setJar(jar);
				Manifest m = jar.getManifest();
				if (m != null) {
					String s = m.getMainAttributes()
						.getValue(Constants.EXPORT_PACKAGE);
					if (s != null)
						analyzer.setExportPackage(s);
				}
				analyzer.analyze();

				boolean java = po.java();

				Packages exports = analyzer.getExports();

				if ((options & API) != 0) {
					Map<PackageRef, List<PackageRef>> apiUses = analyzer.cleanupUses(analyzer.getAPIUses(), !po.java());
					if (!po.xport()) {
						if (exports.isEmpty())
							warning("Not filtering on exported only since exports are empty");
						else
							apiUses.keySet()
								.retainAll(analyzer.getExports()
									.keySet());
					}
					out.println("[API USES]");
					printMultiMap(apiUses);

					Set<PackageRef> privates = analyzer.getPrivates();
					for (PackageRef export : exports.keySet()) {
						Map<Def, List<TypeRef>> xRef = analyzer.getXRef(export, privates,
							Modifier.PROTECTED + Modifier.PUBLIC);
						if (!xRef.isEmpty()) {
							out.println();
							out.printf("%s refers to private Packages (not good)\n\n", export);
							for (Entry<Def, List<TypeRef>> e : xRef.entrySet()) {
								TreeSet<PackageRef> refs = new TreeSet<>();
								for (TypeRef ref : e.getValue())
									refs.add(ref.getPackageRef());

								refs.retainAll(privates);
								out.printf("%60s %-40s %s\n", e.getKey()
									.getOwnerType()
									.getFQN() //
									, e.getKey()
										.getName(),
									refs);
							}
							out.println();
						}
					}
					out.println();
				}

				Map<PackageRef, List<PackageRef>> uses = analyzer.cleanupUses(analyzer.getUses(), !po.java());
				if ((options & USES) != 0) {
					out.println("[USES]");
					printMultiMap(uses);
					out.println();
				}
				if ((options & USEDBY) != 0) {
					out.println("[USEDBY]");
					MultiMap<PackageRef, PackageRef> usedBy = new MultiMap<>(uses).transpose();
					printMultiMap(usedBy);
				}
			}
		}
		if ((options & COMPONENT) != 0) {
			printComponents(out, jar);
		}
		if ((options & METATYPE) != 0) {
			printMetatype(out, jar);
		}
		if ((options & LIST) != 0) {
			out.println("[LIST]");
			for (Map.Entry<String, Map<String, Resource>> entry : jar.getDirectories()
				.entrySet()) {
				String name = entry.getKey();
				Map<String, Resource> contents = entry.getValue();
				out.println(name);
				if (contents != null) {
					for (String element : contents.keySet()) {
						int n = element.lastIndexOf('/');
						if (n > 0)
							element = element.substring(n + 1);
						out.print("  ");
						out.print(element);
						String path = element;
						if (name.length() != 0)
							path = name + "/" + element;
						Resource r = contents.get(path);
						if (r != null) {
							String extra = r.getExtra();
							if (extra != null) {

								out.print(" extra='" + escapeUnicode(extra) + "'");
							}
						}
						out.println();
					}
				} else {
					out.println(name + " <no contents>");
				}
			}
			out.println();
		}
	}

	/**
	 * @param manifest
	 */
	void printManifest(Manifest manifest) {
		SortedSet<String> sorted = new TreeSet<>();
		for (Object element : manifest.getMainAttributes()
			.keySet()) {
			sorted.add(element.toString());
		}
		for (String key : sorted) {
			Object value = manifest.getMainAttributes()
				.getValue(key);
			out.printf("%-40s %-40s\n", key, value);
		}
	}

	private final char nibble(int i) {
		return "0123456789ABCDEF".charAt(i & 0xF);
	}

	private final String escapeUnicode(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c >= ' ' && c <= '~' && c != '\\')
				sb.append(c);
			else {
				sb.append("\\u");
				sb.append(nibble(c >> 12));
				sb.append(nibble(c >> 8));
				sb.append(nibble(c >> 4));
				sb.append(nibble(c));
			}
		}
		return sb.toString();
	}

	/**
	 * Print the components in this JAR.
	 *
	 * @param jar
	 */
	private void printComponents(PrintStream out, Jar jar) throws Exception {
		out.println("[COMPONENTS]");
		Manifest manifest = jar.getManifest();
		if (manifest == null) {
			out.println("No manifest");
			return;
		}

		String componentHeader = manifest.getMainAttributes()
			.getValue(Constants.SERVICE_COMPONENT);
		Parameters clauses = new Parameters(componentHeader, this);
		for (String path : clauses.keySet()) {
			out.println(path);

			Resource r = jar.getResource(path);
			if (r != null) {
				InputStreamReader ir = new InputStreamReader(r.openInputStream(), Constants.DEFAULT_CHARSET);
				OutputStreamWriter or = new OutputStreamWriter(out, Constants.DEFAULT_CHARSET);
				try {
					IO.copy(ir, or);
				} finally {
					or.flush();
					ir.close();
				}
			} else {
				out.println("  - no resource");
				warning("No Resource found for service component: %s", path);
			}
		}
		out.println();
	}

	/**
	 * Print the metatypes in this JAR.
	 *
	 * @param jar
	 */
	private void printMetatype(PrintStream out, Jar jar) throws Exception {
		out.println("[METATYPE]");
		Manifest manifest = jar.getManifest();
		if (manifest == null) {
			out.println("No manifest");
			return;
		}

		Map<String, Resource> map = jar.getDirectories()
			.get("OSGI-INF/metatype");
		if (map != null) {
			for (Map.Entry<String, Resource> entry : map.entrySet()) {
				out.println(entry.getKey());
				IO.copy(entry.getValue()
					.openInputStream(), out);
				out.println();
			}
			out.println();
		}
	}

	<T extends Comparable<? super T>> void printMultiMap(Map<T, ? extends Collection<?>> map) {
		SortedList<T> keys = new SortedList<>(map.keySet());
		for (Object key : keys) {
			String name = key.toString();

			SortedList<Object> values = new SortedList<>(map.get(key), null);
			String list = vertical(40, values);
			out.printf("%-40s %s\n", name, list);
		}
	}

	String vertical(int padding, Collection<?> used) {
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (Object s : used) {
			String name = s.toString();
			sb.append(del);
			sb.append(name);
			sb.append("\r\n");
			del = pad(padding);
		}
		if (sb.length() == 0)
			sb.append("\r\n");
		return sb.toString();
	}

	String pad(int i) {
		StringBuilder sb = new StringBuilder();
		while (i-- > 0)
			sb.append(' ');
		return sb.toString();
	}

	/**
	 * @param msg
	 * @param ports
	 */

	private void print(String msg, Map<?, ? extends Map<?, ?>> ports) {
		if (ports.isEmpty())
			return;
		out.println(msg);
		for (Entry<?, ? extends Map<?, ?>> entry : ports.entrySet()) {
			Object key = entry.getKey();
			Map<?, ?> clause = Create.copy((Map<?, ?>) entry.getValue());
			clause.remove("uses:");
			out.printf("  %-38s %s\n", key.toString()
				.trim(), clause.isEmpty() ? "" : clause.toString());
		}
	}

	/**
	 * Patch
	 */

	interface patchOptions extends Options {

	}

	public void patch(patchOptions opts) throws Exception {
		PatchCommand pcmd = new PatchCommand(this);
		List<String> args = opts._arguments();
		opts._command()
			.execute(pcmd, args.remove(0), args);
	}

	@Description("Run OSGi tests and create report")
	interface runtestsOptions extends workspaceOptions, verboseOptions, excludeOptions {
		@Description("Report directory")
		String reportdir();

		@Description("Title in the report")
		String title();

		@Description("Path to work directory")
		String dir();
	}

	/**
	 * Run the tests from a prepared bnd file.
	 *
	 * @throws Exception
	 */
	@Description("Run OSGi tests and create report")
	public void _runtests(runtestsOptions opts) throws Exception {
		boolean verbose = opts.verbose();
		int errors = 0;
		File cwd = getBase();
		if (opts.dir() != null) {
			cwd = getFile(opts.dir());
		}
		Workspace testws;
		if (opts.workspace() != null) {
			testws = new Workspace(getFile(cwd, opts.workspace()));
		} else {
			testws = new Workspace(cwd);
		}

		try {
			FileTree tree = new FileTree();
			tree.addIncludes(opts._arguments());
			tree.addExcludes(opts.exclude());
			List<File> matchedFiles = tree.getFiles(cwd, "*.bnd");

			File reportDir = getFile(cwd, "reports");
			if (opts.reportdir() != null) {
				reportDir = getFile(cwd, opts.reportdir());
			}
			IO.delete(reportDir);
			IO.mkdirs(reportDir);

			if (!reportDir.isDirectory())
				error("reportdir must be a directory %s (tried to create it ...)", reportDir);

			Tag summary = new Tag("summary");
			summary.addAttribute("date", new Date());
			summary.addAttribute("ws", testws.getBase());

			if (opts.title() != null)
				summary.addAttribute("title", opts.title());
			if (verbose) {
				out.println("workspace: " + testws);
				out.println("search in dir: " + cwd);
				out.println("reports in dir: " + reportDir);

				out.println("defaultIncludesPattern: ");
				out.println("- *.bnd");
				out.println("includePatterns: ");
				if (opts._arguments() != null) {
					for (String string : opts._arguments()) {
						out.println("- " + string);
					}
				}
				out.println("excludePatterns: ");
				if (opts.exclude() != null) {
					for (String string : opts.exclude()) {
						out.println("- " + string);
					}
				}
				out.println("matchedFiles: ");
				for (File string : matchedFiles) {
					out.println("- " + string);
				}
			}

			// TODO check all the arguments

			try {
				for (File f : matchedFiles) {
					if (verbose) {
						out.println();
						out.println("try to run file: " + f);
						out.println("results:");
					}
					int error = runtTest(f, testws, reportDir, summary);
					if (verbose) {
						out.println("Error: " + error);
					}
					errors += error;
				}

			} catch (Throwable e) {
				if (isExceptions()) {
					printExceptionSummary(e, out);
				}

				exception(e, "FAILURE IN RUNTESTS");
				errors++;
			}

			if (errors > 0)
				summary.addAttribute("errors", errors);

			for (String error : getErrors()) {
				Tag e = new Tag("error");
				e.addContent(error);
			}

			for (String warning : getWarnings()) {
				Tag e = new Tag("warning");
				e.addContent(warning);
			}

			File r = getFile(reportDir, "summary.xml");
			try (PrintWriter pw = IO.writer(r, UTF_8)) {
				summary.print(0, pw);
			}
			if (errors != 0)
				error("Errors found %s", errors);
		} finally {
			testws.close();
		}
	}

	/**
	 * Help function to run the tests
	 */
	private int runtTest(File testFile, Workspace testws, File reportDir, Tag summary) throws Exception {
		File tmpDir = new File(reportDir, "tmp");
		IO.mkdirs(tmpDir);
		tmpDir.deleteOnExit();

		Tag test = new Tag(summary, "test");
		test.addAttribute("path", testFile.getAbsolutePath());
		if (!testFile.isFile()) {
			error("No bnd file: %s", testFile);
			test.addAttribute("exception", "No bnd file found");
			error("No bnd file found for %s", testFile.getAbsolutePath());
			return 1;
		}

		Project project = new Project(testws, testFile.getAbsoluteFile()
			.getParentFile(), testFile.getAbsoluteFile());
		project.use(this);
		project.setProperty(NOBUNDLES, "true");

		ProjectTester tester = project.getProjectTester();

		if (!project.isOk()) {
			getInfo(project, project.toString() + ": " + testFile.getName() + ":");
			return 1; // Indicate failure but do not abort
		}

		tester.setContinuous(false);
		tester.setReportDir(tmpDir);
		test.addAttribute("title", project.toString());
		long start = System.currentTimeMillis();
		try {
			int errors = tester.test();

			Collection<File> reports = tester.getReports();
			for (File report : reports) {
				Tag bundle = new Tag(test, "bundle");
				File dest = new File(reportDir, report.getName());
				IO.rename(report, dest);
				bundle.addAttribute("file", dest.getAbsolutePath());
				doPerReport(bundle, dest);
			}

			switch (errors) {
				case ProjectLauncher.OK :
					return 0;

				case ProjectLauncher.CANCELED :
					test.addAttribute("failed", "canceled");
					return 1;

				case ProjectLauncher.DUPLICATE_BUNDLE :
					test.addAttribute("failed", "duplicate bundle");
					return 1;

				case ProjectLauncher.ERROR :
					test.addAttribute("failed", "unknown reason");
					return 1;

				case ProjectLauncher.RESOLVE_ERROR :
					test.addAttribute("failed", "resolve error");
					return 1;

				case ProjectLauncher.TIMEDOUT :
					test.addAttribute("failed", "timed out");
					return 1;
				case ProjectLauncher.WARNING :
					test.addAttribute("warning", "true");
					return 1;

				case ProjectLauncher.ACTIVATOR_ERROR :
					test.addAttribute("failed", "activator error");
					return 1;

				default :
					if (errors > 0) {
						test.addAttribute("errors", errors);
						return errors;
					}
					test.addAttribute("failed", "unknown reason");
					return errors;
			}
		} catch (Exception e) {
			test.addAttribute("failed", e);
			exception(e, "Exception in run %s", e);
			return 1;
		} finally {
			long duration = System.currentTimeMillis() - start;
			test.addAttribute("duration", (duration + 500) / 1000);
			getInfo(project, project.toString() + ": ");
		}
	}

	/**
	 * Calculate the coverage if there is coverage info in the test file.
	 */

	private void doPerReport(Tag report, File file) throws Exception {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true); // never forget this!
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(file);

			XPathFactory xpathFactory = XPathFactory.newInstance();
			XPath xpath = xpathFactory.newXPath();

			doCoverage(report, doc, xpath);
			doHtmlReport(report, file, doc, xpath);

		} catch (Exception e) {
			report.addAttribute("coverage-failed", e.toString());
		}
	}

	private void doCoverage(Tag report, Document doc, XPath xpath) throws XPathExpressionException {
		int bad = Integer.parseInt(xpath.evaluate("count(//method[count(ref)<2])", doc));
		int all = Integer.parseInt(xpath.evaluate("count(//method)", doc));
		report.addAttribute("coverage-bad", bad);
		report.addAttribute("coverage-all", all);
	}

	private void doHtmlReport(@SuppressWarnings("unused") Tag report, File file, Document doc,
		@SuppressWarnings("unused") XPath xpath) throws Exception {
		String path = file.getAbsolutePath();
		if (path.endsWith(".xml"))
			path = path.substring(0, path.length() - 4);
		path += ".html";
		File html = new File(path);
		logger.debug("Creating html report: {}", html);

		TransformerFactory fact = TransformerFactory.newInstance();

		try (InputStream in = getClass().getResourceAsStream("testreport.xsl")) {
			if (in == null) {
				warning("Resource not found: test-report.xsl, no html report");
			} else {
				try (Writer out = IO.writer(html, UTF_8)) {
					Transformer transformer = fact.newTransformer(new StreamSource(in));
					transformer.transform(new DOMSource(doc), new StreamResult(out));
					logger.debug("Transformed");
				}
			}
		}
	}

	@Description("Verify jars")
	@Arguments(arg = {
		"<jar path>", "[...]"
	})
	interface verifyOptions extends Options {}

	/**
	 * Verify jars.
	 *
	 * @throws Exception
	 */
	@Description("Verify jars")
	public void _verify(verifyOptions opts) throws Exception {
		for (String path : opts._arguments()) {
			File f = getFile(path);
			if (!f.isFile()) {
				error("No such file: %s", f);
			} else {
				try (Jar jar = new Jar(f)) {
					if (jar.getManifest() == null || jar.getBsn() == null)
						error("Not a bundle %s", f);
					else {
						try (Verifier v = new Verifier(jar)) {
							getInfo(v, f.getName());
						}
					}
				}
			}
		}
	}

	@Description("Merge a binary jar with its sources. It is possible to specify  source path")
	//
	@Arguments(arg = {
		"<jar path>", "<source path>"
	})
	//
	interface sourceOptions extends Options {
		@Description("The output file")
		String output();
	}

	/**
	 * Merge a bundle with its source.
	 *
	 * @throws Exception
	 */
	@Description("Merge a binary jar with its sources. It is possible to specify  source path")
	public void _source(sourceOptions opts) throws Exception {
		List<String> arguments = opts._arguments();
		File jarFile = getFile(arguments.remove(0));

		if (!jarFile.exists()) {
			error("File %s does not exist ", jarFile);
			return;
		}

		File sourceFile = getFile(arguments.remove(0));
		if (!sourceFile.exists()) {
			error("Source file %s does not exist ", sourceFile);
			return;
		}

		File output = jarFile;
		if (opts.output() != null)
			output = getFile(opts.output());

		File tmp = File.createTempFile("tmp", ".jar", jarFile.getParentFile());
		tmp.deleteOnExit();

		try (Jar bin = new Jar(jarFile); Jar src = new Jar(sourceFile)) {
			bin.setDoNotTouchManifest();
			for (String path : src.getResources()
				.keySet())
				bin.putResource("OSGI-OPT/src/" + path, src.getResource(path));
			bin.write(tmp);
		}
		IO.rename(tmp, output);
	}

	/**
	 * Diff two jar files
	 *
	 * @throws Exception
	 */
	@Description("Diff jars")
	public void _diff(diffOptions opts) throws Exception {
		DiffCommand diff = new DiffCommand(this);
		diff.diff(opts);
	}

	/**
	 * Baseline
	 *
	 * @throws Exception
	 */

	@Description("Compare a newer bundle to a baselined bundle and provide versioning advice")
	public void _baseline(baseLineOptions opts) throws Exception {
		BaselineCommands baseliner = new BaselineCommands(this);
		baseliner._baseline(opts);
	}

	/**
	 * Create a schema of package deltas and versions
	 *
	 * @throws Exception
	 */

	@Description("Highly specialized function to create an overview of package deltas in ees")
	public void _schema(schemaOptions opts) throws Exception {
		BaselineCommands baseliner = new BaselineCommands(this);
		baseliner._schema(opts);
	}

	public Project getProject() throws Exception {
		return getProject(null);
	}

	public Workspace getWorkspace(File workspaceDir) throws Exception {
		if (workspaceDir == null) {
			workspaceDir = getBase();
		}
		ws = Workspace.findWorkspace(workspaceDir);
		if (ws == null)
			return null;

		ws.use(this);
		return ws;
	}

	public Project getProject(String where) throws Exception {
		if (where == null || where.equals("."))
			where = Project.BNDFILE;

		File f = getFile(where);
		if (f.isDirectory()) {
			f = new File(f, Project.BNDFILE);
		}

		if (f.isFile()) {
			if (f.getName()
				.endsWith(Constants.DEFAULT_BNDRUN_EXTENSION)) {
				Workspace ws = Workspace.findWorkspace(f.getParentFile());
				Run run = Run.createRun(ws, f);
				return run;
			}

			File projectDir = f.getParentFile();
			File workspaceDir = projectDir.getParentFile();
			ws = Workspace.findWorkspace(workspaceDir);
			Project project = ws.getProject(projectDir.getName());
			if (project.isValid()) {
				project.use(this);
				return project;
			}
		}

		if (where.equals(Project.BNDFILE)) {
			return null;
		}
		error("Project not found: %s", f);

		return null;
	}

	public Workspace getWorkspace(String where) throws Exception {
		Workspace ws;
		if (where == null) {
			ws = Workspace.findWorkspace(IO.work);
			if (ws == null)
				ws = Workspace.createStandaloneWorkspace(new Processor(), IO.work.toURI());
		} else {
			File f = getFile(where);
			ws = Workspace.findWorkspace(f);
			if (f.isFile() && f.getName()
				.endsWith(Constants.DEFAULT_BNDRUN_EXTENSION)) {
				Run run = Run.createRun(ws, f);
				ws = run.getWorkspace();
			} else {
				ws = Workspace.findWorkspace(f);
			}
		}
		return ws;
	}

	/**
	 * Convert files
	 */
	@Description("Converter to different formats")
	@Arguments(arg = {
		"from", "to"
	})
	interface convertOptions extends Options {
		@Config(description = "Convert a manifest to a properties files")
		boolean m2p();

		@Config(description = "Save as xml")
		boolean xml();
	}

	@Description("Converter to different formats")
	public void _convert(convertOptions opts) throws IOException {
		File from = getFile(opts._arguments()
			.get(0));
		File to = getFile(opts._arguments()
			.get(1));
		if (opts.m2p()) {
			try (InputStream in = IO.stream(from)) {
				Properties p = new UTF8Properties();
				Manifest m = new Manifest(in);
				Attributes attrs = m.getMainAttributes();
				for (Map.Entry<Object, Object> i : attrs.entrySet()) {
					p.put(i.getKey()
						.toString(),
						i.getValue()
							.toString());
				}
				try (OutputStream fout = IO.outputStream(to)) {
					if (opts.xml())
						p.storeToXML(fout, "converted from " + from);
					else {
						try (Writer osw = IO.writer(fout, UTF_8)) {
							p.store(osw, "converted from " + from);
						}
					}
				}
			}
			return;
		}
		error("no conversion specified");
	}

	/**
	 * Create a list of file names that match manifest headers bnd select -h
	 * Bundle-SymbolicName --where (...) *
	 */
	@Description("Helps finding information in a set of JARs by filtering on manifest data and printing out selected information.")
	@Arguments(arg = {
		"<jar-path>", "[...]"
	})
	interface selectOptions extends Options {
		@Description("A simple assertion on a manifest header (e.g. " + Constants.BUNDLE_VERSION
			+ "=1.0.1) or an OSGi filter that is asserted on all manifest headers. Comparisons are case insensitive. The key 'resources' holds the pathnames of all resources and can also be asserted to check for the presence of a header.")
		String where();

		@Description("A manifest header to print or: path, name, size, length, modified for information about the file, wildcards are allowed to print multiple headers. ")
		Collection<String> header();

		@Description("Print the key before the value")
		boolean key();

		@Description("Print the file name before the value")
		boolean name();

		@Description("Print the file path before the value")
		boolean path();
	}

	@Description("Helps finding information in a set of JARs by filtering on manifest data and printing out selected information.")
	public void _select(selectOptions opts) throws Exception {
		PrintStream out = this.out;

		Filter filter = null;
		if (opts.where() != null) {
			String w = opts.where();
			if (!w.startsWith("("))
				w = "(" + w + ")";
			filter = new Filter(w);
		}

		Instructions instructions = new Instructions(opts.header());

		for (String s : opts._arguments()) {
			Jar jar = getJar(s);
			if (jar == null) {
				err.println("no file " + s);
				continue;
			}

			Domain domain = Domain.domain(jar.getManifest());
			Hashtable<String, Object> ht = new Hashtable<>();
			Iterator<String> i = domain.iterator();
			Set<String> realNames = new HashSet<>();

			while (i.hasNext()) {
				String key = i.next();
				String value = domain.get(key)
					.trim();
				ht.put(key.trim()
					.toLowerCase(), value);
				realNames.add(key);
			}
			ht.put("resources", jar.getResources()
				.keySet());
			realNames.add("resources");
			if (filter != null) {
				if (!filter.match(ht))
					continue;
			}

			Set<Instruction> unused = new HashSet<>();
			Collection<String> select = instructions.select(realNames, unused, true);
			for (String h : select) {
				if (opts.path()) {
					out.print(jar.getSource()
						.getAbsolutePath() + ":");
				}
				if (opts.name()) {
					out.print(jar.getSource()
						.getName() + ":");
				}
				if (opts.key()) {
					out.print(h + ":");
				}
				out.println(ht.get(h.toLowerCase()));
			}
			for (Instruction ins : unused) {
				String literal = ins.getLiteral();
				if (literal.equals("name"))
					out.println(jar.getSource()
						.getName());
				else if (literal.equals("path"))
					out.println(jar.getSource()
						.getAbsolutePath());
				else if (literal.equals("size") || literal.equals("length"))
					out.println(jar.getSource()
						.length());
				else if (literal.equals("modified"))
					out.println(new Date(jar.getSource()
						.lastModified()));
			}
		}
	}

	/**
	 * Central routine to get a JAR with error checking
	 *
	 * @param s
	 */
	Jar getJar(String s) {

		File f = getFile(s);
		if (f.isFile()) {
			try {
				return new Jar(f);
			} catch (ZipException e) {
				exception(e, "Not a jar/zip file: %s", f);
			} catch (Exception e) {
				exception(e, "Opening file: %s", f);
			}
			return null;
		}

		try {
			URL url = new URL(s);
			return new Jar(s, url.openStream());
		} catch (Exception e) {
			// Ignore
		}

		error("Not a file or proper url: %s", f);
		return null;
	}

	@Description("Show version information about bnd")
	@Arguments(arg = {})
	public interface versionOptions extends Options {
		@Description("Show licensing, copyright, sha, scm, etc")
		boolean xtra();
	}

	/**
	 * Show the version of this bnd
	 *
	 * @throws IOException
	 */
	@Description("Show version information about bnd")
	public void _version(versionOptions o) throws IOException {
		if (!o.xtra()) {
			Analyzer a = new Analyzer();
			out.println(a.getBndVersion());
			a.close();
			return;
		}
		Enumeration<URL> e = getClass().getClassLoader()
			.getResources("META-INF/MANIFEST.MF");
		while (e.hasMoreElements()) {
			URL u = e.nextElement();

			Manifest m = new Manifest(u.openStream());
			String bsn = m.getMainAttributes()
				.getValue(Constants.BUNDLE_SYMBOLICNAME);
			if (bsn != null && bsn.equals("biz.aQute.bnd")) {
				Attributes attrs = m.getMainAttributes();

				long lastModified = 0;
				try {
					lastModified = Long.parseLong(attrs.getValue(Constants.BND_LASTMODIFIED));
				} catch (Exception ee) {
					// Ignore
				}
				out.printf("%-40s %s\n", "Version", attrs.getValue(Constants.BUNDLE_VERSION));
				if (lastModified > 0)
					out.printf("%-40s %s\n", "From", new Date(lastModified));
				Parameters p = OSGiHeader.parseHeader(attrs.getValue(Constants.BUNDLE_LICENSE));
				for (String l : p.keySet())
					out.printf("%-40s %s\n", "License", p.get(l)
						.get("description"));
				out.printf("%-40s %s\n", "Copyright", attrs.getValue(Constants.BUNDLE_COPYRIGHT));
				out.printf("%-40s %s\n", "Git-SHA", attrs.getValue("Git-SHA"));
				out.printf("%-40s %s\n", "Git-Descriptor", attrs.getValue("Git-Descriptor"));
				out.printf("%-40s %s\n", "Sources", attrs.getValue(Constants.BUNDLE_SCM));
				return;
			}
		}
		error("Could not locate version");
	}

	/**
	 * Show some key info of the project
	 */
	@Arguments(arg = {})
	@Description("Show key project variables")
	interface infoOptions extends Options {
		boolean runbundles();

		boolean buildpath();

		boolean dependsOn();

		boolean sourcepath();

		boolean classpath();

		boolean vmpath();

		String project();
	}

	@Description("Show key project variables")
	public void _info(infoOptions options) throws Exception {
		Project p = getProject(options.project());
		if (p == null) {
			messages.NoProject();
			return;
		}
		boolean any = options.runbundles() || options.buildpath() || options.classpath() || options.dependsOn()
			|| options.vmpath();

		MultiMap<String, Object> table = new MultiMap<>();
		if (any || options.runbundles()) {
			table.addAll("Run", p.getRunbundles());
		}
		if (any || options.buildpath()) {
			table.addAll("Build", p.getBuildpath());
		}
		if (any || options.buildpath()) {
			table.addAll("Depends on", p.getDependson());
		}
		if (any || options.sourcepath()) {
			table.addAll("Source", p.getSourcePath());
		}
		if (any || options.classpath()) {
			table.addAll("Class path", p.getClasspath());
		}

		if (any || options.vmpath()) {
			table.addAll("Run path", p.getRunpath());
		}

		printMultiMap(table);
	}

	/**
	 * Grep in jars
	 */
	@Arguments(arg = {
		"pattern", "file..."
	})
	@Description("Grep the manifest of bundles/jar files. ")
	interface grepOptions extends Options {

		@Description("Search in exports")
		boolean exports();

		@Description("Search in imports")
		boolean imports();

		@Description("Search in bsn")
		boolean bsn();

		@Description("Set header(s) to search, can be wildcarded. The default is all headers (*).")
		Set<String> headers();

		@Description("Search path names of resources. No resources are included unless expressly specified.")
		Set<String> resources();

	}

	@Description("Grep the manifest of bundles/jar files. ")
	public void _grep(grepOptions opts) throws Exception {
		List<String> args = opts._arguments();
		String s = args.remove(0);
		Pattern pattern;
		try {
			pattern = Glob.toPattern(s);
		} catch (IllegalArgumentException e) {
			messages.InvalidGlobPattern_(s);
			return;
		}

		if (args.isEmpty()) {
			args = new ExtList<>(getBase().list((dir, name) -> name.endsWith(".jar")));
		}

		Set<String> headers = opts.headers();
		if (headers == null)
			headers = new TreeSet<>();

		if (opts.exports())
			headers.add(Constants.EXPORT_PACKAGE);
		if (opts.bsn())
			headers.add(Constants.BUNDLE_SYMBOLICNAME);
		if (opts.imports())
			headers.add(Constants.IMPORT_PACKAGE);

		Instructions instructions = new Instructions(headers);

		for (String fileName : args) {
			File file = getFile(fileName);
			if (!file.isFile()) {
				messages.NoSuchFile_(file);
				continue;
			}

			try (Jar in = new Jar(file.getName(), IO.stream(file))) {

				if (opts.resources() != null) {
					Instructions selection = new Instructions(opts.resources());
					Collection<String> selected = selection.select(in.getResources()
						.keySet(), null, false);
					selected.forEach(path -> {
						out.printf("%40s : %s\n", fileName, path);
					});
				}
				Manifest m = in.getManifest();
				for (Object header : m.getMainAttributes()
					.keySet()) {
					Attributes.Name name = (Name) header;
					if (instructions.isEmpty() || instructions.matches(name.toString())) {
						String h = m.getMainAttributes()
							.getValue(name);
						QuotedTokenizer qt = new QuotedTokenizer(h, ",;=");
						for (String value : qt.getTokenSet()) {
							Matcher matcher = pattern.matcher(value);
							while (matcher.find()) {
								int start = matcher.start() - 8;
								if (start < 0)
									start = 0;

								int end = matcher.end() + 8;
								if (end > value.length())
									end = value.length();

								out.printf("%40s : %20s ...%s[%s]%s...\n", fileName, name,
									value.substring(start, matcher.start()),
									value.substring(matcher.start(), matcher.end()),
									value.substring(matcher.end(), end));
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Handle the global settings
	 */
	@Description("Set bnd global variables. The key can be wildcard.")
	@Arguments(arg = {
		"<key>[=<value>]..."
	})
	interface settingOptions extends Options {
		@Description("Clear all the settings, including the public and private key")
		boolean clear();

		@Description("Show the public key")
		boolean publicKey();

		@Description("Show the private secret key")
		boolean secretKey();

		@Description("Sign the strings on the commandline")
		boolean mac();

		@Description("Show key in base64")
		boolean base64();

		@Description("Override the default \"~/.bnd/settings.json\" location")
		String location();

		@Description("Generate a new private/public key pair")
		boolean generate();

		@Description("Password for local file")
		char[] password();

	}

	@Description("Set bnd global variables")
	public void _settings(settingOptions opts) throws Exception {
		try {
			Settings settings = this.settings;
			char[] password = this.password;

			if (opts.location() != null) {

				password = opts.password();

				File f = getFile(opts.location());
				settings = new Settings(f.getAbsolutePath());
				settings.load(password);
				logger.debug("getting settings from {}", f);
			}

			if (opts.clear()) {
				settings.clear();
				logger.debug("clear {}", settings.entrySet());
			}

			if (opts.generate()) {
				logger.debug("Generating new key pair");
				settings.generate(password);
			}

			logger.debug("settings {}", opts.clear());
			List<String> rest = opts._arguments();

			if (opts.publicKey()) {
				out.println(tos(!opts.base64(), settings.getPublicKey()));
			}
			if (opts.secretKey()) {
				out.println(tos(!opts.base64(), settings.getPrivateKey()));
			}

			if (opts.mac()) {
				for (String s : rest) {
					byte[] data = s.getBytes(UTF_8);
					byte[] signature = settings.sign(data);
					out.printf("%s\n", tos(!opts.base64(), signature));
				}
			}

			if (rest.isEmpty()) {
				list(null, settings);
			} else {
				boolean set = false;
				for (String s : rest) {
					s = s.trim();
					Matcher m = ASSIGNMENT.matcher(s);
					logger.debug("try {}", s);
					if (m.matches()) {
						String key = m.group(1);
						Instructions instr = new Instructions(key);
						Collection<String> select = instr.select(settings.keySet(), true);

						// check if there is a value a='b'

						String value = m.group(4);
						if (value == null || value.trim()
							.length() == 0) {
							// no value
							// check '=' presence
							if (m.group(2) == null) {
								list(select, settings);
							} else {
								// we have 'a=', remove
								for (String k : select) {
									logger.debug("remove {}={}", k, settings.get(k));
									settings.remove(k);
									set = true;
								}
							}
						} else {
							logger.debug("assignment {}={}", key, value);
							settings.put(key, value);
							set = true;
						}
					} else {
						err.printf("Cannot assign %s\n", s);

					}
				}
				if (set) {
					logger.debug("saving");
					settings.save(password);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Set the private key in the settings for this machine
	 *
	 * @param hex
	 * @param data
	 * @throws Exception
	 */

	private String tos(boolean hex, byte[] data) {
		return data.length + " : " + (hex ? Hex.toHexString(data) : Base64.encodeBase64(data));
	}

	private void list(Collection<String> keys, Map<String, String> map) {
		for (Entry<String, String> e : map.entrySet()) {
			if (keys == null || keys.contains(e.getKey()))
				out.printf("%-40s = %s\n", e.getKey(), e.getValue());
		}
	}

	enum Alg {
		SHA1,
		MD5,
		SHA256,
		SHA512,
		TIMELESS
	}

	@Description("Digest a number of files")
	@Arguments(arg = "file...")
	interface hashOptions extends Options {

		@Description("Show hex output (default)")
		boolean hex();

		@Description("Show base64 output")
		boolean b64();

		@Description("Show process info")
		boolean process();

		@Description("Show the file name")
		boolean name();

		@Description("Specify the algorithms")
		List<Alg> algorithm();

	}

	/**
	 * hash a file
	 *
	 * @throws Exception
	 * @throws NoSuchAlgorithmException
	 */
	@Description("Digests a number of files")
	public void _digest(hashOptions o) throws NoSuchAlgorithmException, Exception {
		long start = System.currentTimeMillis();
		long total = 0;
		List<Alg> algs = o.algorithm();
		if (algs == null)
			algs = Arrays.asList(Alg.SHA1);

		for (String s : o._arguments()) {
			File f = getFile(s);
			if (f.isFile()) {

				outer: for (Alg alg : algs) {
					long now = System.currentTimeMillis();
					byte[] digest;

					switch (alg) {
						default :
							error("no such algorithm %s", alg);
							continue outer;

						case SHA1 :
							digest = SHA1.digest(f)
								.digest();
							break;
						case SHA256 :
							digest = SHA256.digest(f)
								.digest();
							break;
						case SHA512 :
							digest = SHA512.digest(f)
								.digest();
							break;
						case MD5 :
							digest = MD5.digest(f)
								.digest();
							break;

						case TIMELESS :
							Jar j = new Jar(f);
							digest = j.getTimelessDigest();
							break;
					}

					StringBuilder sb = new StringBuilder();
					String del = "";

					if (o.hex() || !o.b64()) {
						sb.append(del)
							.append(Hex.toHexString(digest));
						del = " ";
					}
					if (o.b64()) {
						sb.append(del)
							.append(Base64.encodeBase64(digest));
						del = " ";
					}
					if (o.name()) {
						sb.append(del)
							.append(f.getAbsolutePath());
						del = " ";
					}
					if (o.process()) {
						sb.append(del)
							.append(System.currentTimeMillis() - now)
							.append(" ms ")
							.append(f.length() / 1000)
							.append(" Kb");
						total += f.length();
					}
					out.println(sb);
				}
			} else
				error("file does not exist %s", f);
		}
		if (o.process()) {
			long time = (System.currentTimeMillis() - start);
			float mb = total / 1000000;
			out.format("Total %s Mb, %s ms, %s Mb/sec %s files\n", mb, time, (total / time) / 1024, o._arguments()
				.size());
		}
	}

	/**
	 * Maven command
	 *
	 * @throws Exception
	 */

	@Description("Maven bundle command")
	public void _maven(Options options) throws Exception {
		MavenCommand mc = new MavenCommand(this);
		mc.use(this);
		mc.run(options._arguments()
			.toArray(new String[0]), 1);
		getInfo(mc);
	}

	@Description("Generate autocompletion file for bash")
	public void _bash(Options options) throws Exception {
		File tmp = File.createTempFile("bnd-completion", ".tmp");
		tmp.deleteOnExit();

		try {
			IO.copy(getClass().getResource("bnd-completion.bash"), tmp);

			Sed sed = new Sed(tmp);
			sed.setBackup(false);

			Reporter r = new ReporterAdapter();
			CommandLine c = new CommandLine(r);
			Map<String, Method> commands = c.getCommands(this);
			StringBuilder sb = new StringBuilder();
			for (String commandName : commands.keySet()) {
				sb.append(" " + commandName);
			}
			sb.append(" help");

			sed.replace("%listCommands%", sb.toString()
				.substring(1));
			sed.doIt();
			IO.copy(tmp, out);
		} finally {
			IO.delete(tmp);
		}
	}

	/**
	 * List actions of the repositories if they implement Actionable and allow
	 * them to be executed
	 */

	@Description("Execute an action on a repo, or if no name is give, list the actions")
	interface ActionOptions extends projectOptions {
		Glob filter();

		boolean tooltip();
	}

	@Description("Execute an action on a repo, or if no name is give, list the actions")
	public void _action(ActionOptions opts) throws Exception {
		Project project = getProject(opts.project());
		if (project == null) {
			error("Not in a project directory");
			return;
		}

		Glob filter = opts.filter();
		if (filter == null)
			filter = new Glob("*");
		List<Actionable> actionables = project.getPlugins(Actionable.class);
		if (actionables.isEmpty()) {
			error("No actionables in [%s]", project.getPlugins());
			return;
		}
		for (Actionable o : actionables) {
			if (filter.matcher(o.title())
				.matches()) {
				logger.debug("actionable {} - {}", o, o.title());
				Map<String, Runnable> map = o.actions();
				if (map != null) {
					if (opts._arguments()
						.isEmpty()) {
						out.printf("# %s%n", o.title());
						if (opts.tooltip() && o.tooltip() != null) {
							out.printf("%s%n", o.tooltip());
						}
						out.printf("## actions%n");
						for (String entry : map.keySet()) {
							out.printf("  %s%n", entry);
						}
					} else {
						for (String entry : opts._arguments()) {
							Runnable r = map.get(entry);
							if (r != null) {
								r.run();
							}
						}
					}
				}
			}
		}
		getInfo(project);
	}

	/**
	 * Show the changes in the releases
	 */
	@Arguments(arg = {})
	@Description("Show the changes in this release of bnd")
	interface ChangesOptions extends Options {
		@Description("Print all releases")
		boolean all();
	}

	private final static Pattern	BUG_P			= Pattern.compile("#(\\d+)");
	private final static Pattern	BND_COMMAND_P	= Pattern.compile("\\[bnd\\s+(\\w+)\\s*\\]");

	public void _changes(ChangesOptions options) {
		boolean first = true;
		Justif j = new Justif(80, 10);
		Formatter f = j.formatter();

		for (Map.Entry<Version, String[]> e : About.CHANGES.entrySet()) {
			if (options.all() || first) {
				f.format("$-\nRelease %s\n$-\n", e.getKey());
				for (String s : e.getValue()) {
					f.format("- \t1%s", s.replace('\n', '\f'));
					Matcher m = BND_COMMAND_P.matcher(s);
					while (m.find()) {
						Formatter ff = new Formatter();
						ff.format("\n\n");
						CommandLine cli = options._command();
						cli.help(ff, this, m.group(1));
						j.indent(10, ff.out()
							.toString());
					}
					m = BUG_P.matcher(s);
					while (m.find()) {
						f.format("\f-> https://github.com/bndtools/bnd/issues/%s", m.group(1));
					}
					f.format("\n\n");
				}
			}
			first = false;
		}
		j.wrap();
		out.println(f.out());
	}

	/**
	 * Find a package in the current project or a set of jars
	 */

	@Arguments(arg = "[file]...")
	@Description("Go through the exports and/or imports and match the given "
		+ "exports/imports globs. If thet match print the file, package and version.")
	interface FindOptions extends Options {
		@Description("Glob expression on the imports.")
		Glob[] imports();

		@Description("Glob expression on the exports.")
		Glob[] exports();
	}

	public void _find(FindOptions options) throws Exception {
		List<File> files = new ArrayList<>();

		List<String> args = options._arguments();
		if (args.isEmpty()) {
			Project p = getProject();
			if (p == null) {
				error("This is not a project directory and you have specified no jar files ...");
				return;
			}
			File output = p.getOutput();
			if (output.exists()) {
				files.add(output);
			}
			for (Container c : p.getBuildpath()) {
				files.add(c.getFile());
			}
		} else {
			for (String f : args) {
				File file = getFile(f);
				files.add(file);
			}
		}
		for (File f : files) {
			logger.debug("find {}", f);
			try (Jar jar = new Jar(f)) {
				Manifest m = jar.getManifest();
				if (m != null) {
					Domain domain = Domain.domain(m);

					if (options.exports() != null) {
						Parameters ep = domain.getExportPackage();
						for (Glob g : options.exports()) {
							for (Entry<String, Attrs> exp : ep.entrySet()) {
								if (g.matcher(exp.getKey())
									.matches()) {
									String v = exp.getValue()
										.get(VERSION_ATTRIBUTE);
									if (v == null)
										v = "0";
									out.printf(">%s: %s-%s%n", f.getPath(), exp.getKey(), v);
								}
							}
						}
					}
					if (options.imports() != null) {
						Parameters ip = domain.getImportPackage();
						for (Glob g : options.imports()) {
							for (Entry<String, Attrs> imp : ip.entrySet()) {
								if (g.matcher(imp.getKey())
									.matches()) {
									String v = imp.getValue()
										.get(VERSION_ATTRIBUTE);
									if (v == null)
										v = "0";
									out.printf("<%s: %s-%s%n", f.getPath(), imp.getKey(), v);
								}
							}
						}
					}
				}
			}
		}

	}

	/**
	 * Merge n JARs into a new JAR
	 */

	@Arguments(arg = "jarfile...")
	interface MergeOptions extends Options {
		@Description("Specify the output file path. The default is output.jar in the current directory")
		String output();
	}

	@Description("Merge a number of jar files into a new jar file. The used manifest is that of the first"
		+ "given JAR file. The order of the JAR file is the class path order. I.e. earlier resources"
		+ "are preferred over later resources with the same name.")
	public void __merge(MergeOptions options) throws Exception {
		String name = options.output() == null ? "output.jar" : options.output();
		File out = getFile(name);
		if (!out.getParentFile()
			.isDirectory()) {
			error("Output file is not in a valid directory: %s", out.getParentFile());
		}
		List<String> list = options._arguments();
		Collections.reverse(list);

		try (Jar jar = new Jar(name)) {
			Jar last = null;
			for (String member : list) {
				File m = getFile(member);
				if (!m.isFile()) {
					error("%s is not a file", m.getAbsolutePath());
				} else {
					Jar jm = new Jar(m);
					last = jm;
					addClose(jm);
					jar.addAll(jm);
				}
			}
			if (last != null) {
				jar.setManifest(last.getManifest());
			}
			jar.write(out);
		}
	}

	@Arguments(arg = "<jar-file>...")
	@Description("Show the Execution Environments of a JAR")
	interface EEOptions extends Options {

	}

	/**
	 * Show the class versions used in a JAR
	 *
	 * @throws Exception
	 */
	@Description("Show the Execution Environments of a JAR")
	public void _ees(EEOptions options) throws Exception {
		for (String path : options._arguments()) {
			File f = getFile(path);
			if (!f.isFile()) {
				error("Not a file");
			} else {
				try (Analyzer a = new Analyzer(this)) {
					a.setJar(f);
					a.analyze();
					out.printf("%s %s%n", a.getJar()
						.getName(), a.getEEs());
				}
			}
		}
	}

	@Description("experimental - parallel build")
	interface ParallelBuildOptions extends buildoptions {

		long synctime();
	}

	/**
	 * Lets see if we can build in parallel
	 *
	 * @throws Exception
	 */
	public void __par(final ParallelBuildOptions options) throws Exception {
		ExecutorService pool = Executors.newCachedThreadPool();
		final AtomicBoolean quit = new AtomicBoolean();

		try {
			final Forker<Project> forker = new Forker<>(pool);

			List<Project> projects = getFilteredProjects(options);

			for (final Project proj : projects) {
				forker.doWhen(proj.getDependson(), proj, () -> {
					if (!quit.get()) {

						try {
							proj.compile(options.test());
							if (!quit.get()) {
								proj.build(options.test());
							}
							if (!proj.isOk()) {
								quit.set(true);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					getInfo(proj, proj + ": ");
				});
			}
			err.flush();

			long syncms = options.synctime() <= 0 ? 20000 : options.synctime();
			forker.start(syncms);
		} finally {
			pool.shutdownNow();
		}
	}

	/**
	 * Force a cache update of the workspace
	 *
	 * @throws Exception
	 */

	public void _sync(projectOptions options) throws Exception {
		Workspace ws = getWorkspace((File) null);

		if (ws == null) {
			error(
				"Cannot find workspace, either reside in a project directory, point to a project with --project, or reside in the workspace directory");
			return;
		}

		ws.syncCache();
	}

	/**
	 * From a set of bsns, create a list of urls
	 */

	interface Bsn2UrlOptions extends projectOptions {

	}

	private final static Pattern LINE_P = Pattern.compile("\\s*((\\S#|[^#])+)(\\s*#.*)?");

	public void _bsn2url(Bsn2UrlOptions opts) throws Exception {
		Project p = getProject(opts.project());

		if (p == null) {
			error("You need to be in a project or specify the project with -p/--project");
			return;
		}

		MultiMap<String, Version> revisions = new MultiMap<>();

		for (RepositoryPlugin repo : p.getPlugins(RepositoryPlugin.class)) {
			if (!(repo instanceof InfoRepository))
				continue;

			for (String bsn : repo.list(null)) {
				revisions.addAll(bsn, repo.versions(bsn));
			}
		}

		for (List<Version> versions : revisions.values()) {
			Collections.sort(versions, Collections.reverseOrder());
		}

		List<String> files = opts._arguments();

		for (String f : files) {
			try (BufferedReader r = IO.reader(getFile(f))) {
				String line;
				nextLine: while ((line = r.readLine()) != null) {
					Matcher matcher = LINE_P.matcher(line);
					if (!matcher.matches())
						continue nextLine;

					line = matcher.group(1);

					Parameters bundles = new Parameters(line, this);
					for (Map.Entry<String, Attrs> entry : bundles.entrySet()) {

						String bsn = entry.getKey();
						VersionRange range = new VersionRange(entry.getValue()
							.getVersion());

						List<Version> versions = revisions.get(bsn);
						if (versions == null) {
							error("No for versions for %s", bsn);
							break nextLine;
						}

						for (Version version : versions) {
							if (range.includes(version)) {

								for (RepositoryPlugin repo : p.getPlugins(RepositoryPlugin.class)) {

									if (!(repo instanceof InfoRepository))
										continue;

									InfoRepository rp = (InfoRepository) repo;
									ResourceDescriptor descriptor = rp.getDescriptor(bsn, version);
									if (descriptor == null) {
										error("Found bundle, but no descriptor %s;version=%s", bsn, version);
										return;
									}

									out.println(
										descriptor.url + " #" + descriptor.bsn + ";version=" + descriptor.version);
								}
							}
						}

					}

				}
			} catch (Exception e) {
				error("failed to create url list from file %s : %s", f, e);
			}
		}
	}

	/**
	 * Show the loaded workspace plugins
	 *
	 * @throws Exception
	 */

	public void _plugins(projectOptions opts) throws Exception {
		Workspace ws = getWorkspace(opts.project());

		if (ws == null) {
			error("Can't find a workspace");
			return;
		}

		int n = 0;
		for (Object o : ws.getPlugins()) {
			String s = o.toString();
			if (s.trim()
				.length() == 0)
				s = o.getClass()
					.getName();

			out.printf("%03d %s%n", n++, s);
		}
		getInfo(ws);

	}

	/**
	 * Show the dependencies of all projects
	 */

	@Description("Show the used workspace dependencies ")
	@Arguments(arg = "instruction...")
	interface DependencyOptions extends projectOptions {
		@Description("Show the number of projects using that dependency")
		boolean count();
	}

	@Description("Show the used workspace dependencies ")
	public void _dependencies(DependencyOptions opts) throws Exception {
		Workspace ws = getWorkspace(opts.project());

		if (ws == null) {
			error("Can't find a workspace");
			return;
		}
		Instructions instructions = new Instructions(opts._arguments());

		MultiMap<String, Attrs> dependencies = new MultiMap<>();
		int n = 0;
		for (Project p : ws.getAllProjects()) {
			if (instructions.matches(p.getName())) {
				Parameters parms = p.getParameters(Constants.BUILDPATH);
				for (Map.Entry<String, Attrs> e : parms.entrySet()) {
					dependencies.add(e.getKey(), e.getValue());
				}
			}
		}
		Justif justif = new Justif(80, new int[] {
			40, 48, 56
		});
		Formatter f = justif.formatter();

		for (Map.Entry<String, List<Attrs>> e : dependencies.entrySet()) {
			f.format("%s \t1%s\n", e.getKey(), e.getValue()
				.size());
		}

		out.println(justif.wrap());
		getInfo(ws);
	}

	/**
	 * start a local framework
	 */

	interface BootstrapOptions extends Options {

	}

	public void _bootstrap(BootstrapOptions options) throws Exception {
		Workspace ws = getWorkspace(getBase());
		File buildDir = ws.getBuildDir();
		File bndFile = IO.getFile(buildDir, "bnd.bnd");
		if (!bndFile.isFile()) {
			error("No bnd.bnd file found in cnf directory %s", bndFile);
			return;
		}

		Run run = new Run(ws, buildDir, bndFile);

		run.runLocal();

		getInfo(run);
	}

	/**
	 * Show all the defaults in bnd
	 */

	public void _defaults(Options o) {
		Processor defaults = Workspace.getDefaults();
		out.println(Strings.join("\n", defaults.getProperties()
			.entrySet()));
	}

	/*
	 * Cop a bundle, potentially stripping it
	 */

	@Arguments(arg = {
		"src...", "dest"
	})
	interface CopyOptions extends Options {

		@Description("Remove all metadata manifest")
		boolean strip();

		@Description("Remove OSGi metadata from the manifest")
		boolean specific();

		@Description("Remove OSGI-OPT")
		boolean optional();
	}

	public void _copy(CopyOptions options) throws Exception {
		List<String> files = options._arguments();
		if (files.size() < 2) {
			error("Need at least a source and a destination");
			return;
		}
		String output = files.remove(files.size() - 1);
		File dest = new File(getBase(), output);

		if (files.size() > 1) {
			if (dest.isFile()) {
				error("Multiple files require that the output is a directory");
				return;
			}
			IO.mkdirs(dest);
		}

		for (String f : files) {

			Jar jar = getJar(f);
			if (jar == null) {
				error("No such JAR %s", f);
				continue;
			}

			if (options.strip()) {
				Manifest m = new Manifest();
				jar.setManifest(m);
			}

			if (options.specific()) {
				Manifest m = new Manifest();
				if (jar.getManifest() != null) {
					for (Entry<Object, Object> e : jar.getManifest()
						.getMainAttributes()
						.entrySet()) {
						String header = e.getKey()
							.toString()
							.toLowerCase();
						if (header.startsWith("bundle-"))
							continue;

						if (!isIn(Constants.BUNDLE_SPECIFIC_HEADERS, header))
							m.getMainAttributes()
								.put(e.getKey(), e.getValue());
					}
				}
				jar.setManifest(m);
			}

			if (options.optional()) {
				jar.getDirectories()
					.remove("OSGI-OPT");
			}

			String name = getJarFileNameFrom(jar.getName());

			File out = dest.isDirectory() ? getFile(dest, name) : dest;
			File tmp = new File("tmp");
			jar.write(tmp);
			jar.close();

			IO.rename(tmp, out);
		}

	}

	private boolean isIn(String[] bundleSpecificHeaders, String key) {
		for (int i = 0; i < bundleSpecificHeaders.length; i++) {
			if (key.equalsIgnoreCase(bundleSpecificHeaders[i]))
				return true;
		}
		return false;
	}

	private String getJarFileNameFrom(String name) {
		String out = name;
		int n = out.lastIndexOf('/');
		if (n >= 0) {
			out = out.substring(n + 1);
		}
		if (out.endsWith(".jar"))
			return out;

		return out + ".jar";
	}

	/**
	 * Add a project, workspace, or plugin
	 */

	@Arguments(arg = {
		"what", "name..."
	})
	interface AddOptions extends Options {

	}

	@Description("Add a workspace, or a project or a plugin to the workspace")
	public void _add(AddOptions opts) throws Exception {
		List<String> args = opts._arguments();

		String what = args.remove(0);

		if ("project".equals(what)) {
			Workspace ws = Workspace.findWorkspace(getBase());
			if (ws == null) {
				error("No workspace found from %s", getBase());
				return;
			}

			for (String pname : args) {
				ws.createProject(pname);
			}

			getInfo(ws);
			return;
		}

		if ("workspace".equals(what)) {
			for (String pname : args) {
				File wsdir = getFile(pname);
				ws = Workspace.createWorkspace(wsdir);
				if (ws == null) {
					error("Could not create workspace");
				}
			}
			getInfo(ws);
			return;
		}

		if ("plugin".equals(what)) {
			Workspace ws = getWorkspace(getBase());
			if (ws == null) {
				error("No workspace found from %s", getBase());
				return;
			}

			CommandLine cl = new CommandLine(this);
			String help = cl.execute(new Plugins(this, ws), "add", new ExtList<>(args));
			if (help != null)
				out.println(help);
			getInfo(ws);
			return;
		}

	}

	@Arguments(arg = {
		"what", "name..."
	})
	interface RemoveOptions extends Options {}

	@Description("Remove a project or a plugin from the workspace")
	public void _remove(RemoveOptions opts) throws Exception {
		List<String> args = opts._arguments();

		String what = args.remove(0);

		Workspace ws = Workspace.findWorkspace(getBase());
		if (ws == null) {
			error("No workspace found from %s", getBase());
			return;
		}

		if ("project".equals(what)) {
			for (String pname : args) {
				Project project = ws.getProject(pname);
				if (project == null) {
					error("No such project %s", pname);
				} else
					project.remove();
			}

			getInfo(ws);
			return;
		}

		if ("workspace".equals(what)) {
			error("To delete a workspace, delete the directory");
			return;
		}

		if ("plugin".equals(what)) {
			CommandLine cl = new CommandLine(this);
			String help = cl.execute(new Plugins(this, ws), "remove", new ExtList<>(args));
			if (help != null)
				out.println(help);
			return;
		}
	}

	/**
	 * Profiles subcmd
	 */

	@Description("Profile management. A profile is a JAR that only contains packages and capabilities")
	@Arguments(arg = {
		"create", "..."
	})
	public interface ProfileOptions extends Options {

	}

	@Description("Profile management. A profile is a JAR that only contains packages and capabilities")
	public void _profile(ProfileOptions options) throws Exception {
		Profiles profiles = new Profiles(this, options);
		CommandLine cmd = options._command();
		cmd.subCmd(options, profiles);
		getInfo(profiles);
	}

	/**
	 * Resolve command
	 *
	 * @throws Exception
	 */

	public void _resolve(ResolveCommand.ResolveOptions options) throws Exception {
		ResolveCommand rc = new ResolveCommand(this);
		String help = options._command()
			.subCmd(options, rc);
		if (help != null)
			out.println(help);
		getInfo(rc);
		rc.close();
	}

	/**
	 * Remote command
	 *
	 * @throws Exception
	 */

	public void _remote(RemoteCommand.RemoteOptions options) throws Exception {
		RemoteCommand rc = new RemoteCommand(this, options);
		String help = options._command()
			.subCmd(options, rc);
		if (help != null)
			out.println(help);
		getInfo(rc);
		rc.close();
	}

	/**
	 * Nexus commands
	 *
	 * @throws Exception
	 */

	public void _nexus(NexusCommand.NexusOptions options) throws Exception {
		NexusCommand rc = new NexusCommand(this, options);
		String help = options._command()
			.subCmd(options, rc);
		if (help != null)
			out.println(help);
		getInfo(rc);
		rc.close();
	}

	/**
	 * Export a bndrun file
	 */
	interface ExportOptions extends ProjectWorkspaceOptions {

		@Override
		@Description("Use the following workspace")
		String workspace();

		List<String> exporter();

		String output();
	}

	public void _export(ExportOptions options) throws Exception {

		HandledProjectWorkspaceOptions ho = handleOptions(options, BNDRUN_ALL);

		for (File f : ho.files()) {
			if (options.verbose()) {
				out.println("Exporter: " + f);
			}
			// // temporary
			// project.getWorkspace()

			Run run = new Run(ho.workspace(), f);
			run.getSettings(this);
			run.addBasicPlugin(new SubsystemExporter());

			Parameters exports = new Parameters();

			List<String> types = options.exporter();
			if (types != null) {

				for (String type : types) {
					out.println("Types: " + type);
					exports.putAll(new Parameters(type, this));
				}
			} else {
				String exportTypes = run.getProperty(Constants.EXPORTTYPE);
				out.println("Exporttype: " + exportTypes);
				exports.putAll(new Parameters(exportTypes, this));
			}

			if (exports.entrySet()
				.isEmpty()) {
				err.println("no Exporters set");
			}
			for (Entry<String, Attrs> e : exports.entrySet()) {

				if (options.verbose()) {
					out.println("exporting " + run + " to " + e.getKey() + " with " + e.getValue());
				}
				logger.debug("exporting {} to {} with {}", run, e.getKey(), e.getValue());

				Map.Entry<String, Resource> result = run.export(e.getKey(), e.getValue());
				getInfo(run);

				if (result != null && isOk()) {

					String name = result.getKey();

					File output = new File(run.getTarget(), options.output() == null ? name : options.output());
					if (output.isDirectory())
						output = new File(output, name);
					IO.mkdirs(output.getParentFile());
					logger.debug("Got a result for {}, store in {}", e.getKey(), output);
					IO.copy(result.getValue()
						.openInputStream(), output);
				}
			}
		}
	}

	protected HandledProjectWorkspaceOptions handleOptions(ProjectWorkspaceOptions options, String... defaultIncludes)
		throws Exception {

		boolean verbose = options.verbose();
		Project project = getProject(options.project());
		final Workspace cws = calcWorkspace(options);

		File searchBaseDir = project != null ? project.getBase() : cws.getBase();

		List<String> includePatterns = options._arguments();
		String[] excludePatterns = options.exclude();

		if (verbose) {
			out.println("workspace: " + cws);
			out.println("search in dir: " + searchBaseDir);

			out.println("defaultIncludesPatterns: ");
			for (String string : defaultIncludes) {
				out.println("- " + string);
			}

			out.println("includePatterns: ");
			if (includePatterns != null) {
				for (String string : includePatterns) {
					out.println("- " + string);
				}
			}
			out.println("excludePatterns: ");
			if (excludePatterns != null) {
				for (String string : excludePatterns) {
					out.println("- " + string);
				}
			}
		}

		FileTree tree = new FileTree();
		tree.addIncludes(includePatterns);
		tree.addExcludes(excludePatterns);
		List<File> matchedFiles = tree.getFiles(searchBaseDir, defaultIncludes);

		if (verbose) {
			out.println("matchedFiles: ");
			for (File string : matchedFiles) {

				out.println("- " + string);
			}
		}
		return new HandledProjectWorkspaceOptions() {

			@Override
			public Workspace workspace() {

				return cws;
			}

			@Override
			public List<File> files() {

				return matchedFiles;
			}

		};

	}

	private Workspace calcWorkspace(ProjectWorkspaceOptions options) throws Exception {
		Project project = getProject(options.project());
		Workspace ws = null;

		// Set ws if defined as param
		if (options.workspace() != null) {
			ws = getWorkspace(options.workspace());
		} // Use ws of project
		else if (project != null) {
			ws = project.getWorkspace();
		} else {
			ws = getWorkspace(getBase());
		}

		if (ws == null) {
			warning("Using default workspace");
			ws = Workspace.createDefaultWorkspace();
		}

		return ws;
	}

	/**
	 * Flatten a jar
	 */

	@Arguments(arg = {
		"input", "output"
	})
	@Description("Flatten a bundle by expanding all entries on the Bundle-ClassPath")
	interface FlattenOptions extends Options {}

	@Description("Flatten a bundle by expanding all entries on the Bundle-ClassPath")
	public void _flatten(FlattenOptions opts) throws Exception {

		List<String> inputs = opts._arguments();

		String inputPath = inputs.remove(0);
		String outputPath = inputs.remove(0);

		File source = getFile(inputPath);
		if (!source.isFile()) {
			error("Not a source file %s", source);
			return;
		}

		File destination = getFile(outputPath);
		IO.mkdirs(destination.getParentFile());

		if (!destination.getParentFile()
			.isDirectory()) {
			error("Could not create directory for output file %s", outputPath);
		}

		Jar input = new Jar(source);
		addClose(input);

		Manifest manifest = input.getManifest();

		Domain domain = Domain.domain(manifest);
		List<String> bundleClassPath = new ArrayList<>(domain.getBundleClasspath()
			.keySet());

		if (bundleClassPath.isEmpty()) {
			warning("%s has no bundle class path", source);
			return;
		}

		Collections.reverse(bundleClassPath);

		Jar output = new Jar(source.getName());

		for (String path : bundleClassPath) {
			logger.debug("bcp entry {}", path);
			Resource r = input.getResource(path);
			if (r == null) {

				logger.debug("Is directory {}", path);

				if (path.equals(".")) {
					addAll(output, input, "", bundleClassPath);
				} else
					addAll(output, input, path, null);

			} else {

				logger.debug("Is jar {}", path);

				Jar sub = new Jar(path, r.openInputStream());
				addClose(sub);
				addAll(output, sub, "", null);
			}
		}

		domain.setBundleClasspath(".");
		output.setManifest(manifest);
		output.stripSignatures();
		output.write(destination);
	}

	@Description("Extract a set of classes/packages from a set of JARs")
	interface CollectOptions extends Options {
		@Description("A file with a list of class names to extract. Can be -- when the names are piped into this command")
		String classes();

	}

	@Description("Extract a set of resources from a set of JARs given a set of prefixes. "
		+ "All prefixes in any of the given input jars are added to the output jar")
	@Arguments(arg = {
		"<out path>", "<in path>", "[...]"
	})
	public void _collect(CollectOptions options) throws Exception {
		List<Jar> opened = new ArrayList<>();

		List<String> args = options._arguments();
		String outpath = args.remove(0);

		File outfile = getFile(outpath);
		outfile.getParentFile()
			.mkdirs();
		logger.debug("out %s", outfile);

		String classes = options.classes();
		if (classes == null) {
			classes = "--";
		}

		Jar store = new Jar("store");
		opened.add(store);

		for (String arg : args) {
			try {
				logger.debug("storing %s", arg);
				File file = getFile(arg);
				if (!file.isFile()) {
					error("Cannot open file %s", file);
					continue;
				}
				Jar jar = new Jar(file.getName(), file);
				store.addAll(jar);
				opened.add(jar);
			} catch (Exception e) {
				exception(e, "File %s", arg);
			}
		}

		Jar out = new Jar("out");
		opened.add(out);
		forEachLine(classes, line -> {
			String path = line.trim();
			if (path.isEmpty() || path.startsWith("#"))
				return;

			logger.info("line {}", path);
			boolean match = false;
			for (Map.Entry<String, Resource> e : store.getResources()
				.entrySet()) {
				if (e.getKey()
					.startsWith(path)) {
					match = true;
					logger.info("# found %s", path);
					out.putResource(e.getKey(), e.getValue());
				}
			}
			if (!match) {
				this.error("Not found %s", path);
			}
		});
		out.write(outfile);
		opened.forEach(IO::close);

	}

	@Description("Convert class names to resource paths from stdin to stdout")
	@Arguments(arg = {})
	public void _classtoresource(Options options) throws IOException {
		try (Analyzer a = new Analyzer()) {
			List<String> l = options._arguments()
				.isEmpty() ? Arrays.asList("--") : options._arguments();

			for (String f : l) {
				forEachLine(f, s -> {
					String trim = s.trim();
					TypeRef t = a.getTypeRefFromFQN(trim);
					while (t.isArray())
						t.getComponentTypeRef();

					out.println(t.getPath());
				});
			}
		}
	}

	@Description("Convert package names to resource paths from stdin to stdout")
	@Arguments(arg = {})
	public void _packagetoresource(Options options) throws IOException {
		try (Analyzer a = new Analyzer()) {
			List<String> l = options._arguments()
				.isEmpty() ? Arrays.asList("--") : options._arguments();

			for (String f : l) {
				forEachLine(f, s -> {
					String trim = s.trim();
					if (trim.isEmpty())
						return;

					String path = Descriptors.fqnToBinary(trim);
					if (!path.equals("/"))
						path = path + "/";
					out.println(path);
				});
			}
		}
	}

	private void forEachLine(String file, Consumer<String> c) throws IOException {
		InputStream in = System.in;
		boolean isConsole = file == null || !file.equals("--");
		if (isConsole) {
			File f = getFile(file);
			if (!f.isFile()) {
				error("No such file %s", f);
				return;
			}
			in = new FileInputStream(f);
		}

		try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
			String line;
			while ((line = br.readLine()) != null) {
				c.accept(line);
			}
		} finally {
			if (isConsole) {
				in.close();
			}
		}

	}

	private void addAll(Jar output, Jar sub, String prefix, List<String> bundleClassPath) {
		if (prefix.length() > 0 && !prefix.endsWith("/"))
			prefix += "/";

		for (Map.Entry<String, Resource> e : sub.getResources()
			.entrySet()) {
			String path = e.getKey();

			if (bundleClassPath != null && bundleClassPath.contains(path))
				continue;

			logger.debug("Add {}", path);

			if (path.equals("META-INF/MANIFEST.MF"))
				continue;

			Resource r = e.getValue();

			if (path.startsWith(prefix)) {
				logger.debug("Add {}", path);
				path = path.substring(prefix.length());
				output.putResource(path, r);
			} else
				logger.debug("Ignore {} because it does not start with prefix {}", path, prefix);

		}
	}

	private static void exitWithCode(int code) {
		if (!noExit.get()) {
			System.exit(code);
		}
	}

	/**
	 * Index command
	 *
	 * @throws Exception
	 */

	@Description("Index bundles from the local file system")
	public void _index(IndexCommand.indexOptions options) throws Exception {
		IndexCommand ic = new IndexCommand(this);
		ic.use(this);
		ic._index(options);
		ic.close();
	}

	@Description("Commands to verify the bnd communications setup")
	public void _com(CommunicationCommands.CommunicationOptions options) throws Exception {
		try (CommunicationCommands cc = new CommunicationCommands(this, options)) {
			if (cc.isOk() && isWorkspaceOk()) {
				String help = options._command()
					.subCmd(options, cc);
				if (help != null)
					out.println(help);
			}
			getInfo(cc);
		}
	}

	private boolean isWorkspaceOk() {
		return ws == null || ws.isOk();
	}

	@Description("Commands to inspect a dependency graph of a set of bundles")
	public void _graph(GraphCommand.GraphOptions options) throws Exception {
		try (GraphCommand cc = new GraphCommand(this, options)) {
			String help = options._command()
				.subCmd(options, cc);
			if (help != null)
				out.println(help);
		}
	}

	@Description("Start an interactive shell")
	public void _shell(Shell.ShellOptions options) throws Exception {
		try (Shell shell = new Shell(this, options)) {
			shell.loop();
		} finally {
			out.println("done");
		}
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	@Description("Show the project or the workspace properties")
	@Arguments(arg = {})
	interface PropertiesOptions extends projectOptions {
		@Description("Get the inherited properties")
		boolean local();

		@Description("Filter on key")
		Glob key(Glob deflt);

		@Description("Filter on value")
		Glob value(Glob deflt);
	}

	/**
	 * Print out all the properties
	 */

	@Description("Show the project or the workspace properties")
	public void _properties(PropertiesOptions options) throws Exception {

		Processor domain = getProject(options.project());
		if (domain == null) {
			domain = getWorkspace();
		}
		if (domain == null) {
			domain = this;
		}

		Glob key = options.key(Glob.ALL);
		Glob value = options.value(Glob.ALL);

		for (String s : domain.getPropertyKeys(options.local())) {
			if (key.matches(s)) {
				String property = domain.getProperty(s);
				if (value.matches(property)) {
					out.printf("%-40s %s%n", s, property);
				}
			}
		}

	}

	@Description("Generate and export reports of a workspace, a project or of a jar.")
	public void _exportreport(ExportReportCommand.ReporterOptions options) throws Exception {
		ExportReportCommand mc = new ExportReportCommand(this);
		mc.run(options);
		getInfo(mc);
	}

	@Description("Maintain Maven Bnd Repository GAV files")
	public void _mbr(MbrCommand.MrOptions options) throws Exception {
		MbrCommand c = new MbrCommand(this, options);
		CommandLine cl = new CommandLine(this);
		String s = cl.subCmd(options, c);
		if (s != null) {
			out.println(s);
		}
		getInfo(c);
	}

}

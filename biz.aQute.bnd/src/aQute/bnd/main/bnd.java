package aQute.bnd.main;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.jar.*;
import java.util.jar.Attributes.Name;
import java.util.regex.*;
import java.util.zip.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.xpath.*;

import org.w3c.dom.*;

import aQute.bnd.build.*;
import aQute.bnd.header.*;
import aQute.bnd.help.*;
import aQute.bnd.main.BaselineCommands.baseLineOptions;
import aQute.bnd.main.BaselineCommands.schemaOptions;
import aQute.bnd.main.DiffCommand.diffOptions;
import aQute.bnd.main.RepoCommand.repoOptions;
import aQute.bnd.maven.*;
import aQute.bnd.osgi.*;
import aQute.bnd.osgi.Clazz.Def;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.osgi.eclipse.*;
import aQute.bnd.service.*;
import aQute.bnd.service.action.*;
import aQute.bnd.service.repository.*;
import aQute.bnd.service.repository.SearchableRepository.ResourceDescriptor;
import aQute.bnd.version.*;
import aQute.configurable.*;
import aQute.lib.base64.*;
import aQute.lib.collections.*;
import aQute.lib.filter.*;
import aQute.lib.getopt.*;
import aQute.lib.hex.*;
import aQute.lib.io.*;
import aQute.lib.justif.*;
import aQute.lib.settings.*;
import aQute.lib.tag.*;
import aQute.libg.classdump.*;
import aQute.libg.cryptography.*;
import aQute.libg.forker.*;
import aQute.libg.generics.*;
import aQute.libg.glob.*;
import aQute.libg.qtokens.*;
import aQute.libg.reporter.*;
import aQute.libg.sed.*;
import aQute.service.reporter.*;

/**
 * Utility to make bundles. Should be areplace for jar and much more.
 * 
 * @version $Revision: 1.14 $
 */
public class bnd extends Processor {
	static Pattern				ASSIGNMENT	= Pattern.compile( //
													"([^=]+) (= ( ?: (\"|'|) (.+) \\3 )? ) ?", Pattern.COMMENTS);
	Settings					settings	= new Settings();
	final PrintStream			err			= System.err;
	final public PrintStream	out			= System.out;
	Justif						justif		= new Justif(80, 40, 42, 70);
	BndMessages					messages	= ReporterMessages.base(this, BndMessages.class);
	private Workspace			ws;

	static Pattern				JARCOMMANDS	= Pattern.compile("(cv?0?(m|M)?f?)|(uv?0?M?f?)|(xv?f?)|(tv?f?)|(i)");

	static Pattern				COMMAND		= Pattern.compile("\\w[\\w\\d]+");

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

		@Description("Error/Warning ignore patterns")
		String[] ignore();

	}

	public static void main(String args[]) throws Exception {
		bnd main = new bnd();
		try {
			main.start(args);
		}
		finally {
			main.close();
		}
	}

	public void start(String args[]) throws Exception {
		CommandLine cl = new CommandLine(this);
		String help = cl.execute(this, "bnd", new ExtList<String>(args));
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
			set(FAIL_OK, options.failok() + "");
			setExceptions(options.exceptions());
			setTrace(options.trace());
			setPedantic(options.pedantic());

			if (options.base() != null)
				setBase(getFile(getBase(), options.base()));

			// And the properties
			for (Entry<String,String> entry : options._properties().entrySet()) {
				setProperty(entry.getKey(), entry.getValue());
			}

			CommandLine handler = options._command();
			List<String> arguments = options._();

			// Rewrite command line to match jar commands and
			// handle commands that provide file names

			rewrite(arguments);

			trace("rewritten %s", arguments);

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
		}
		catch (Throwable t) {
			if (t instanceof InvocationTargetException)
				t = t.getCause();
			exception(t, "%s", t.getMessage());
		}
		out.flush();
		err.flush();
		if (ws != null)
			getInfo(ws);

		if (!check(options.ignore())) {
			System.err.flush();
			System.err.flush();
			Thread.sleep(1000);
			System.exit(getErrors().size());
		}
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

		@Description("Directory (-C option)")
		String Cdir();

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
	 *     jar c[v0M]f jarfile [-C dir] inputfiles [-Joption] 
	 *     jar c[v0]mf manifest jarfile [-C dir] inputfiles [-Joption] 
	 *     jar c[v0M] [-C dir] inputfiles [-Joption] 
	 *     jar c[v0]m manifest [-C dir] inputfiles [-Joption]
	 * </pre>
	 * 
	 * @param options
	 * @throws Exception
	 */
	@Description("Create jar, used to support backward compatible java jar commands")
	public void _create(createOptions options) throws Exception {
		Jar jar = new Jar("dot");

		File dir = getBase().getAbsoluteFile();
		String sdir = options.Cdir();
		if (sdir != null)
			dir = getFile(sdir);

		if (options._().isEmpty())
			add(jar, dir, ".", options.verbose());
		else
			for (String f : options._()) {
				f = f.replace(File.separatorChar, '/');
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
				jar.write(System.out);
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
	 */
	private void add(Jar jar, File base, String path, boolean report) {
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
		String CDir();
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
			Instructions instructions = new Instructions(opts._());
			Collection<String> selected = instructions.select(jar.getResources().keySet(), true);
			File store = getBase();
			if (opts.CDir() != null)
				store = getFile(opts.CDir());

			if (!store.exists() && !store.mkdirs()) {
				throw new IOException("Could not create directory " + store);
			}
			Jar.Compression compression = jar.hasCompression();
			for (String path : selected) {
				if (opts.verbose())
					System.err.printf("%8s: %s\n", compression.toString().toLowerCase(), path);

				File f = getFile(store, path);
				File pf = f.getParentFile();
				if (!pf.exists() && !pf.mkdirs()) {
					throw new IOException("Could not create directory " + pf);
				}
				Resource r = jar.getResource(path);
				IO.copy(r.openInputStream(), f);
			}
		}
		finally {
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
			Instructions instructions = new Instructions(opts._());
			Collection<String> selected = instructions.select(jar.getResources().keySet(), true);

			for (String path : selected) {
				if (opts.verbose()) {
					Resource r = jar.getResource(path);
					err.printf("%8s %-32s %s\n", r.size(), new Date(r.lastModified()), path);
				} else
					err.printf("%s\n", path);
			}
		}
		finally {
			jar.close();
		}
	}

	/**
	 * The do command interprets files and does a default action for each file
	 * 
	 * @param project
	 * @param args
	 * @param i
	 * @return
	 * @throws Exception
	 */

	@Description("Execute a file based on its extension. Supported extensions are: bnd (build), bndrun (run), and jar (print)")
	interface dooptions extends Options {
		@Description("The output file")
		String output();

		@Description("Force even when there are errors")
		boolean force();
	}

	@Description("Execute a file based on its extension. Supported extensions are: bnd (build), bndrun (run), and jar (print)")
	public void _do(dooptions options) throws Exception {
		for (String path : options._()) {
			if (path.endsWith(Constants.DEFAULT_BND_EXTENSION)) {
				Builder b = new Builder();
				b.setTrace(isTrace());
				b.setPedantic(isPedantic());

				File f = getFile(path);
				b.setProperties(f);
				b.build();

				File out = b.getOutputFile(options.output());
				getInfo(b, f.getName() + ": ");
				if (isOk()) {
					b.save(out, options.force());
				}
				getInfo(b, f.getName() + ": "); // pickup any save errors
				if (!isOk()) {
					out.delete();
				}
				b.close();
			} else if (path.endsWith(Constants.DEFAULT_JAR_EXTENSION) || path.endsWith(Constants.DEFAULT_BAR_EXTENSION)) {
				Jar jar = getJar(path);
				doPrint(jar, MANIFEST, null);
			} else if (path.endsWith(Constants.DEFAULT_BNDRUN_EXTENSION)) {
				doRun(Arrays.asList(path), false, null);
			} else
				messages.UnrecognizedFileType_(path);
		}
	}

	/**
	 * Project command, executes actions.
	 */

	@Description("Execute a Project action, or if no parms given, show information about the project")
	@Arguments(arg = {})
	interface projectOptions extends Options {
		@Description("Identify another project")
		String project();
	}

	@Description("Execute a Project action, or if no parms given, show information about the project")
	public void _project(projectOptions options) throws Exception {
		Project project = getProject(options.project());
		if (project == null) {
			messages.NoProject();
			return;
		}

		List<String> l = new ArrayList<String>(options._());
		if (l.isEmpty()) {
			err.printf("Name         %s\n", project.getName());
			err.printf("Actions      %s\n", project.getActions().keySet());
			err.printf("Directory    %s\n", project.getBase());
			err.printf("Depends on   %s\n", project.getDependson());
			err.printf("Sub builders %s\n", project.getSubBuilders());
			return;
		}

		String cmd = null;
		String arg = null;

		if (!l.isEmpty())
			cmd = l.remove(0);
		if (!l.isEmpty())
			arg = l.remove(0);

		if (!l.isEmpty()) {
			messages.MoreArgumentsThanNeeded_(options._());
			return;
		}

		if (cmd == null) {
			messages.NoCommandForProject(project);
			return;
		}

		Action a = project.getActions().get(cmd);
		if (a != null) {
			a.execute(project, arg);
			getInfo(project);
			return;
		}
	}

	/**
	 * Bump a version number
	 * 
	 * @param args
	 * @param i
	 * @throws Exception
	 */
	@Description("Bumps the version of a project. Will take the current version and then increment "
			+ "with a major, minor, or micro increment. The default bump is minor.")
	@Arguments(arg = "<major|minor|micro>")
	interface bumpoptions extends Options {
		@Description("Path to another project than the current project")
		String project();
	}

	@Description("Bumps the version of a project")
	public void _bump(bumpoptions options) throws Exception {
		Project project = getProject(options.project());

		if (project == null) {
			messages.NoProject();
			return;
		}

		String mask = null;
		if (!options._().isEmpty()) {
			mask = options._().get(0);
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

	@Description("Build a project. This will create the jars defined in the bnd.bnd and sub-builders.")
	@Arguments(arg = {})
	interface buildoptions extends projectOptions {

		@Description("Build for test")
		boolean test();

		@Description("Do full")
		boolean full();
	}

	@Description("Build a project. This will create the jars defined in the bnd.bnd and sub-builders.")
	public void _build(buildoptions opts) throws Exception {
		Project project = getProject(opts.project());
		if (project == null) {
			messages.NoProject();
			return;
		}
		project.build(opts.test());
		getInfo(project);
	}

	@Description("Test a project according to an OSGi test")
	@Arguments(arg = {
		"testclass[:method]..."
	})
	interface testOptions extends Options {
		@Description("Path to another project than the current project")
		String project();

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
	public void _test(testOptions opts) throws Exception {
		Project project = getProject(opts.project());
		if (project == null) {
			messages.NoProject();
			return;
		}

		// if (!verifyDependencies(project, opts.verify(), true))
		// return;
		//
		List<String> testNames = opts._();
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
				warning("No %s set on this bundle. Use -f/--force to try this test anyway (this works if another bundle provides the testcases)",
						TESTCASES);
				return;
			}

		if (opts.continuous())
			project.setProperty(TESTCONTINUOUS, "true");

		if (opts.trace() || isTrace())
			project.setProperty(RUNTRACE, "true");

		project.test(testNames);
		getInfo(project);
	}

	@Description("Test a project with plain JUnit")
	public void _junit(testOptions opts) throws Exception {
		Project project = getProject(opts.project());
		if (project == null) {
			messages.NoProject();
			return;
		}

		project.junit();
		getInfo(project);
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
		doRun(opts._(), opts.verify(), opts.project());
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
			run = Workspace.getRun(f);
			if (run == null) {
				messages.NoRunFile(f);
				return;
			}
		}
		verifyDependencies(run, verify, false);
		try {
			run.run();
		}
		catch (Exception e) {
			messages.Failed__(e, "Running " + run);
		}
		getInfo(run);
		getInfo(run.getWorkspace());
	}

	@Description("Clean a project")
	interface cleanOptions extends Options {
		@Description("Path to another project than the current project")
		String project();
	}

	@Description("Clean a project")
	public void _clean(cleanOptions opts) throws Exception {
		Project project = getProject(opts.project());
		if (project == null) {
			messages.NoProject();
			return;
		}
		project.clean();
		getInfo(project);
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
		List<String> args = opts._();
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

	/**
	 * Package a bnd or bndrun file for packaging.
	 * 
	 * @param path
	 * @throws Exception
	 */
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

		@Description("Use JPM to deliver the -runbundles and -runpath. This will include the SHAs of the jars in the manifest. When JPM installs such a "
				+ "JAR it will automatically fetchs these jars and place them in the proper place. The filepaths to these artifacts will contain a ${JPMREPO} "
				+ "macro that points at the directory where the sha based named files are stored.")
		boolean jpm();
	}

	@Description("Package a bnd or bndrun file into a single jar that executes with java -jar <>.jar")
	public void _package(packageOptions opts) throws Exception {
		Project project = getProject(); // default project
		if (project == null) {
			error("Packaging only works inside a project directory (needs bnd.bnd file)");
			return;
		}

		List<String> cmdline = opts._();
		File output = null;

		if (opts.output() != null) {
			output = getFile(opts.output());
		} else
			output = getBase();

		if (opts._().size() > 1) {
			if (!output.exists() && !output.mkdirs()) {
				throw new IOException("Could not create directory " + output);
			}
		} else {
			File pf = output.getParentFile();
			if (!pf.exists() && !pf.mkdirs()) {
				throw new IOException("Could not create directory " + pf);
			}
		}

		String profile = opts.profile() == null ? "exec" : opts.profile();
		if (opts.jpm())
			project.setProperty(Constants.PACKAGE, "jpm");

		// TODO Not sure if we need a project actually?
		project.build();

		if (cmdline.isEmpty())
			cmdline.add(Project.BNDFILE); // default project itself

		for (String path : cmdline) {

			File file = getFile(path);
			if (!file.isFile()) {
				messages.NoSuchFile_(file);
			} else {

				// Tricky because we can be run inside the context of a
				// project (in which case
				// we need to inherit from the project or outside.

				Workspace ws = project.getWorkspace();
				project = new Project(ws, getBase(), file);
				project.setProperty(PROFILE, profile);
				project.use(this);
				if (opts.jpm()) {
					project.setProperty(Constants.PACKAGE, Constants.PACKAGE_JPM);
				}

				try {
					Jar jar = project.pack(profile);
					path = path.replaceAll(".bnd(run)?$", "") + ".jar";
					File out = output;
					if (output.isDirectory())
						out = new File(output, path);
					jar.write(out);
					jar.close();
				}
				catch (Exception e) {
					messages.ForProject_File_FailedToCreateExecutableException_(project, path, e);
				}
				getInfo(project);
			}
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
			projects = project.getWorkspace().getAllProjects();

		List<Container> containers = new ArrayList<Container>();

		for (Project p : projects) {
			containers.addAll(p.getDeliverables());
		}

		for (Container c : containers) {
			Version v = new Version(c.getVersion());
			err.printf("%-40s %8s  %s\n", c.getBundleSymbolicName(), v.getWithoutQualifier(), c.getFile());
		}
		getInfo(project);
	}

	/**
	 * Show the value of a macro
	 * 
	 * @param args
	 * @param i
	 * @return
	 * @throws Exception
	 */
	@Description("Show macro value. Macro can contain the ${ and } parentheses but it is also ok without. You can use the ':' instead of the ';' in a macro")
	@Arguments(arg = {
			"<macro>", "[...]"
	})
	interface macroOptions extends Options {
		@Description("Path to project, default current directory")
		String project();
	}

	@Description("Show macro value")
	public void _macro(macroOptions options) throws Exception {
		Project project = getProject(options.project());

		if (project == null) {
			messages.NoProject();
			return;
		}

		StringBuilder sb = new StringBuilder();
		Macro r = project.getReplacer();
		getInfo(project);

		String del = "";
		for (String s : options._()) {
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

	/**
	 * Release the project
	 * 
	 * @param args
	 * @param i
	 * @throws Exception
	 */
	@Description("Release this project")
	interface releaseOptions extends Options {
		@Description("Path to project, default is current project")
		String project();

		@Description("Release with test build")
		boolean test();
	}

	@Description("Release this project")
	public void _release(releaseOptions options) throws Exception {
		Project project = getProject(options.project());
		if (project == null)
			return;

		project.release(options.test());
		getInfo(project);
	}

	/**
	 * Cross reference every class in the jar file to the files it references
	 * 
	 * @param args
	 * @param i
	 */
	@Description("Show a cross references for all classes in a set of jars.")
	@Arguments(arg = {
			"<jar path>", "[...]"
	})
	interface xrefOptions extends Options {
		@Description("Show classes instead of packages")
		boolean classes();

		@Description("Show references to other classes/packages (>)")
		boolean to();

		@Description("Show references from other classes/packages (<)")
		boolean from();

		@Description("Filter for class names, a globbing expression")
		List<String> match();

	}

	static public class All {
		public Map<TypeRef,List<TypeRef>>		classes		= new HashMap<Descriptors.TypeRef,List<TypeRef>>();
		public Map<PackageRef,List<PackageRef>>	packages	= new HashMap<Descriptors.PackageRef,List<PackageRef>>();
	}

	@Description("Show a cross references for all classes in a set of jars.")
	public void _xref(xrefOptions options) throws IOException, Exception {
		Analyzer analyzer = new Analyzer();
		final MultiMap<TypeRef,TypeRef> table = new MultiMap<TypeRef,TypeRef>();
		final MultiMap<PackageRef,PackageRef> packages = new MultiMap<PackageRef,PackageRef>();
		Set<TypeRef> set = Create.set();
		Instructions filter = new Instructions(options.match());
		for (String arg : options._()) {
			try {
				File file = new File(arg);
				Jar jar = new Jar(file.getName(), file);
				try {
					for (Map.Entry<String,Resource> entry : jar.getResources().entrySet()) {
						String key = entry.getKey();
						Resource r = entry.getValue();
						if (key.endsWith(".class")) {
							TypeRef ref = analyzer.getTypeRefFromPath(key);
							if (filter.matches(ref.toString())) {
								set.add(ref);

								InputStream in = r.openInputStream();
								Clazz clazz = new Clazz(analyzer, key, r);

								// TODO use the proper bcp instead
								// of using the default layout
								Set<TypeRef> s = clazz.parseClassFile();
								for (Iterator<TypeRef> t = s.iterator(); t.hasNext();) {
									TypeRef tr = t.next();
									if (tr.isJava() || tr.isPrimitive())
										t.remove();
									else
										packages.add(ref.getPackageRef(), tr.getPackageRef());
								}
								table.addAll(ref, s);
								set.addAll(s);
								in.close();
							}
						}
					}
				}
				finally {
					jar.close();
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		boolean to = options.to();
		boolean from = options.from();
		if (to == false && from == false)
			to = from = true;

		if (options.classes()) {
			if (to)
				printxref(table, ">");
			if (from)
				printxref(table.transpose(), "<");
		} else {
			if (to)
				printxref(packages, ">");
			if (from)
				printxref(packages.transpose(), "<");
		}
	}

	@SuppressWarnings("unchecked")
	private void printxref(MultiMap< ? , ? > map, String direction) {
		SortedList< ? > labels = new SortedList<Comparable< ? >>((Collection< ? extends Comparable< ? >>) map.keySet());
		for (Object element : labels) {
			List< ? > e = map.get(element);
			if (e == null) {
				// ignore
			} else {
				Set<Object> set = new LinkedHashSet<Object>(e);
				set.remove(element);
				Iterator< ? > row = set.iterator();
				String first = "";
				if (row.hasNext())
					first = row.next().toString();
				out.printf("%50s %s %s\n", element, direction, first);
				while (row.hasNext()) {
					out.printf("%50s   %s\n", "", row.next());
				}
			}
		}
	}

	@Description("Show info about the current directory's eclipse project")
	@Arguments(arg = {})
	interface eclipseOptions extends Options {
		@Description("Path to the project")
		String dir();
	}

	@Description("Show info about the current directory's eclipse project")
	public void _eclipse(eclipseOptions options) throws Exception {

		File dir = getBase();
		if (options.dir() != null)
			dir = getFile(options.dir());

		if (!dir.isDirectory())
			error("Eclipse requires a path to a directory: " + dir.getAbsolutePath());

		if (options._().size() != 0)
			error("Unnecessary arguments %s", options._());

		if (!isOk())
			return;

		File cp = new File(dir, ".classpath");
		if (!cp.exists()) {
			error("Cannot find .classpath in project directory: " + dir.getAbsolutePath());
		} else {
			EclipseClasspath eclipse = new EclipseClasspath(this, dir.getParentFile(), dir);
			err.println("Classpath    " + eclipse.getClasspath());
			err.println("Dependents   " + eclipse.getDependents());
			err.println("Sourcepath   " + eclipse.getSourcepath());
			err.println("Output       " + eclipse.getOutput());
			err.println();
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

		List<Builder> builders = new ArrayList<Builder>();
		List<String> order = new ArrayList<String>();
		List<String> active = new ArrayList<String>();

		for (String s : options._()) {
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
				String path = outputFile.getName().replaceAll("\\.jar$", ".pom");
				if (path.equals(outputFile.getName()))
					path = outputFile.getName() + ".pom";
				File pom = new File(outputFile.getParentFile(), path);
				OutputStream out = new FileOutputStream(pom);
				try {
					r.write(out);
				}
				finally {
					out.close();
				}
			}

			getInfo(b, b.getPropertiesFile().getName());
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
			}
			finally {
				set.remove(s);
			}
		}
		order.add(s);
		builders.add(b);
	}

	/**
	 * View files from JARs We parse the commandline and print each file on it.
	 * 
	 * @param args
	 * @param i
	 * @throws Exception
	 */
	@Description("View a resource from a JAR file. Manifest will be pretty printed and class files are shown disassembled.")
	@Arguments(arg = {
			"<jar-file>", "<resource>", "[...]"
	})
	interface viewOptions extends Options {
		@Description("Character set to use for viewing")
		String charset();
	}

	@Description("View a resource from a JAR file.")
	public void _view(viewOptions options) throws Exception {
		String charset = "UTF-8";
		if (options.charset() != null)
			charset = options.charset();

		if (options._().isEmpty()) {
			error("Need a jarfile as source");
			return;
		}
		List<String> args = options._();
		File file = getFile(args.remove(0));
		if (!file.isFile()) {
			error("File does not exist %s", file);
			return;
		}

		Jar jar = new Jar(file);
		try {
			if (args.isEmpty())
				args.add("*");

			Instructions instructions = new Instructions(args);
			Collection<String> selected = instructions.select(jar.getResources().keySet(), true);
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
		finally {
			jar.close();
		}
	}

	/**
	 * Wrap a jar to a bundle.
	 * 
	 * @param args
	 * @param i
	 * @throws Exception
	 */
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

		for (String j : options._()) {
			File file = getFile(j);
			if (!file.isFile()) {
				error("File does not exist %s", file);
				continue;
			}

			Analyzer wrapper = new Analyzer(this);
			try {
				wrapper.use(this);
				addClose(wrapper);

				for (File f : classpath)
					wrapper.addClasspath(f);

				wrapper.setJar(file);

				File outputFile = wrapper.getOutputFile(options.output());
				if (outputFile.getCanonicalFile().equals(file.getCanonicalFile())) {
					// #267: CommandLine wrap deletes target even if file equals
					// source
					error("Output file %s and source file %s are the same file, they must be different", outputFile,
							file);
					return;
				}
				outputFile.delete();

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
					wrapper.getJar().setManifest(m);
					wrapper.save(outputFile, options.force());
				}
				getInfo(wrapper, file.toString());
			}
			finally {
				wrapper.close();
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

	/**
	 * Printout all the variables in scope.
	 * 
	 * @param args
	 * @param i
	 * @throws Exception
	 */
	@Description("Show a lot of info about the project you're in")
	interface debugOptions extends Options {
		@Description("Path to a project, default is current directory")
		String project();

		@Description("Show the flattened properties")
		boolean flattened();
	}

	@SuppressWarnings("unchecked")
	@Description("Show a lot of info about the project you're in")
	public void _debug(debugOptions options) throws Exception {
		Project project = getProject(options.project());
		Justif justif = new Justif(120, 40, 50, 52, 80);

		trace("using %s", project);
		Processor target = project;
		if (project != null) {
			getInfo(project.getWorkspace());

			report(justif, "Workspace", project.getWorkspace());
			report(justif, "Project", project);

			if (project.getSubBuilders() != null)
				for (Builder sub : project.getSubBuilders()) {
					report(justif, "Sub-Builder", sub);
					getInfo(sub);
				}

			for (File file : project.getBase().listFiles()) {
				if (file.getName().endsWith(Constants.DEFAULT_BNDRUN_EXTENSION)) {
					Run run = Workspace.getRun(file);
					if (run == null) {
						error("No such run file", file);
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
		Map<String,Object> table = new LinkedHashMap<String,Object>();
		processor.report(table);
		Justif j = new Justif(140, 40, 44, 48, 100);
		j.formatter().format("$-\n%s %s\n$-\n", string, processor);
		j.table(table, "-");
		out.println(j.wrap());
		out.println();
	}

	/**
	 * Manage the repo.
	 * 
	 * <pre>
	 * out.println(&quot; bnd repo [--repo|-r ('maven'| &lt;dir&gt;)]*&quot;);
	 * out.println(&quot;        repos                          # list the repositories&quot;);
	 * out.println(&quot;        list                           # list all content (not always possible)&quot;);
	 * out.println(&quot;        get &lt;bsn&gt; &lt;version&gt; &lt;file&gt;?    # get an artifact&quot;);
	 * out.println(&quot;        put &lt;file&gt;+                    # put in artifacts&quot;);
	 * out.println(&quot;        help&quot;);
	 * </pre>
	 */

	@Description("Manage the repositories")
	public void _repo(repoOptions opts) throws Exception {
		new RepoCommand(this, opts);
	}

	/**
	 * Print out a JAR
	 */

	final static int	VERIFY		= 1;

	final static int	MANIFEST	= 2;

	final static int	LIST		= 4;

	final static int	IMPEXP		= 16;
	final static int	USES		= 32;
	final static int	USEDBY		= 64;
	final static int	COMPONENT	= 128;
	final static int	METATYPE	= 256;
	final static int	API			= 512;

	static final int	HEX			= 0;

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
	}

	@Description("Printout the JAR")
	public void _print(printOptions options) throws Exception {
		for (String s : options._()) {
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

			if (opts == 0)
				opts = MANIFEST | IMPEXP;

			Jar jar = getJar(s);
			try {
				doPrint(jar, opts, options);
			}
			finally {
				jar.close();
			}
		}
	}

	private void doPrint(Jar jar, int options, printOptions po) throws ZipException, IOException, Exception {

		Analyzer analyzer = new Analyzer();
		try {
			if ((options & VERIFY) != 0) {
				Verifier verifier = new Verifier(jar);
				verifier.setPedantic(isPedantic());
				verifier.verify();
				getInfo(verifier);
			}
			if ((options & MANIFEST) != 0) {
				Manifest manifest = jar.getManifest();
				if (manifest == null)
					warning("JAR has no manifest " + jar);
				else {
					err.println("[MANIFEST " + jar.getName() + "]");
					printManifest(manifest);
				}
				out.println();
			}
			if ((options & IMPEXP) != 0) {
				out.println("[IMPEXP]");
				Manifest m = jar.getManifest();
				Domain domain = Domain.domain(m);

				if (m != null) {
					Parameters imports = domain.getImportPackage();
					Parameters exports = domain.getExportPackage();
					for (String p : exports.keySet()) {
						if (imports.containsKey(p)) {
							Attrs attrs = imports.get(p);
							if (attrs.containsKey(VERSION_ATTRIBUTE)) {
								exports.get(p).put("imported-as", attrs.get(VERSION_ATTRIBUTE));
							}
						}
					}
					print(Constants.IMPORT_PACKAGE, new TreeMap<String,Attrs>(imports));
					print(Constants.EXPORT_PACKAGE, new TreeMap<String,Attrs>(exports));
				} else
					warning("File has no manifest");
			}

			if ((options & (USES | USEDBY | API)) != 0) {
				out.println();
				analyzer.setPedantic(isPedantic());
				analyzer.setJar(jar);
				Manifest m = jar.getManifest();
				if (m != null) {
					String s = m.getMainAttributes().getValue(Constants.EXPORT_PACKAGE);
					if (s != null)
						analyzer.setExportPackage(s);
				}
				analyzer.analyze();

				boolean java = po.java();

				Packages exports = analyzer.getExports();

				if ((options & API) != 0) {
					Map<PackageRef,List<PackageRef>> apiUses = analyzer.cleanupUses(analyzer.getAPIUses(), !po.java());
					if (!po.xport()) {
						if (exports.isEmpty())
							warning("Not filtering on exported only since exports are empty");
						else
							apiUses.keySet().retainAll(analyzer.getExports().keySet());
					}
					out.println("[API USES]");
					printMultiMap(apiUses);

					Set<PackageRef> privates = analyzer.getPrivates();
					for (PackageRef export : exports.keySet()) {
						Map<Def,List<TypeRef>> xRef = analyzer.getXRef(export, privates, Modifier.PROTECTED
								+ Modifier.PUBLIC);
						if (!xRef.isEmpty()) {
							out.println();
							out.printf("%s refers to private Packages (not good)\n\n", export);
							for (Entry<Def,List<TypeRef>> e : xRef.entrySet()) {
								TreeSet<PackageRef> refs = new TreeSet<Descriptors.PackageRef>();
								for (TypeRef ref : e.getValue())
									refs.add(ref.getPackageRef());

								refs.retainAll(privates);
								out.printf("%60s %-40s %s\n", e.getKey().getOwnerType().getFQN() //
										, e.getKey().getName(), refs);
							}
							out.println();
						}
					}
					out.println();
				}

				Map<PackageRef,List<PackageRef>> uses = analyzer.cleanupUses(analyzer.getUses(), !po.java());
				if ((options & USES) != 0) {
					out.println("[USES]");
					printMultiMap(uses);
					out.println();
				}
				if ((options & USEDBY) != 0) {
					out.println("[USEDBY]");
					MultiMap<PackageRef,PackageRef> usedBy = new MultiMap<Descriptors.PackageRef,Descriptors.PackageRef>(
							uses).transpose();
					printMultiMap(usedBy);
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
				for (Map.Entry<String,Map<String,Resource>> entry : jar.getDirectories().entrySet()) {
					String name = entry.getKey();
					Map<String,Resource> contents = entry.getValue();
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
		finally {
			jar.close();
			analyzer.close();
		}
	}

	/**
	 * @param manifest
	 */
	void printManifest(Manifest manifest) {
		SortedSet<String> sorted = new TreeSet<String>();
		for (Object element : manifest.getMainAttributes().keySet()) {
			sorted.add(element.toString());
		}
		for (String key : sorted) {
			Object value = manifest.getMainAttributes().getValue(key);
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

		String componentHeader = manifest.getMainAttributes().getValue(Constants.SERVICE_COMPONENT);
		Parameters clauses = new Parameters(componentHeader);
		for (String path : clauses.keySet()) {
			out.println(path);

			Resource r = jar.getResource(path);
			if (r != null) {
				InputStreamReader ir = new InputStreamReader(r.openInputStream(), Constants.DEFAULT_CHARSET);
				OutputStreamWriter or = new OutputStreamWriter(out, Constants.DEFAULT_CHARSET);
				try {
					IO.copy(ir, or);
				}
				finally {
					or.flush();
					ir.close();
				}
			} else {
				out.println("  - no resource");
				warning("No Resource found for service component: " + path);
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

		Map<String,Resource> map = jar.getDirectories().get("OSGI-INF/metatype");
		if (map != null) {
			for (Map.Entry<String,Resource> entry : map.entrySet()) {
				out.println(entry.getKey());
				IO.copy(entry.getValue().openInputStream(), out);
				out.println();
			}
			out.println();
		}
	}

	<T extends Comparable< ? >> void printMultiMap(Map<T, ? extends Collection< ? >> map) {
		SortedList<Object> keys = new SortedList<Object>(map.keySet());
		for (Object key : keys) {
			String name = key.toString();

			SortedList<Object> values = new SortedList<Object>(map.get(key));
			String list = vertical(40, values);
			out.printf("%-40s %s\n", name, list);
		}
	}

	String vertical(int padding, Collection< ? > used) {
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

	private void print(String msg, Map< ? , ? extends Map< ? , ? >> ports) {
		if (ports.isEmpty())
			return;
		out.println(msg);
		for (Entry< ? , ? extends Map< ? , ? >> entry : ports.entrySet()) {
			Object key = entry.getKey();
			Map< ? , ? > clause = Create.copy(entry.getValue());
			clause.remove("uses:");
			out.printf("  %-38s %s\n", key.toString().trim(), clause.isEmpty() ? "" : clause.toString());
		}
	}

	/**
	 * Patch
	 */

	interface patchOptions extends Options {

	}

	public void patch(patchOptions opts) throws Exception {
		PatchCommand pcmd = new PatchCommand(this);
		List<String> args = opts._();
		opts._command().execute(pcmd, args.remove(0), args);
	}

	/**
	 * Run the tests from a prepared bnd file.
	 * 
	 * @param args
	 * @param i
	 * @throws Exception
	 */

	@Description("Run OSGi tests and create report")
	interface runtestsOptions extends Options {
		@Description("Report directory")
		String reportdir();

		@Description("Title in the report")
		String title();

		@Description("Path to work directory")
		String dir();

		@Description("Path to workspace")
		String workspace();
	}

	@Description("Run OSGi tests and create report")
	public void _runtests(runtestsOptions opts) throws Exception {
		int errors = 0;
		File cwd = new File("").getAbsoluteFile();

		Workspace ws = new Workspace(cwd);
		try {
			File reportDir = getFile("reports");

			IO.delete(reportDir);

			Tag summary = new Tag("summary");
			summary.addAttribute("date", new Date());
			summary.addAttribute("ws", ws.getBase());

			if (opts.reportdir() != null) {
				reportDir = getFile(opts.reportdir());
			}
			if (!reportDir.exists() && !reportDir.mkdirs()) {
				throw new IOException("Could not create directory " + reportDir);
			}

			if (!reportDir.isDirectory())
				error("reportdir must be a directory %s (tried to create it ...)", reportDir);

			if (opts.title() != null)
				summary.addAttribute("title", opts.title());

			if (opts.dir() != null)
				cwd = getFile(opts.dir());

			if (opts.workspace() != null) {
				ws.close();
				ws = Workspace.getWorkspace(getFile(opts.workspace()));
			}

			// TODO check all the arguments

			boolean hadOne = false;
			try {
				for (String arg : opts._()) {
					trace("will run test %s", arg);
					File f = getFile(arg);
					errors += runtTest(f, ws, reportDir, summary);
					hadOne = true;
				}

				if (!hadOne) {
					// See if we had any, if so, just use all files in
					// the current directory
					File[] files = cwd.listFiles();
					for (File f : files) {
						if (f.getName().endsWith(".bnd")) {
							errors += runtTest(f, ws, reportDir, summary);
						}
					}
				}
			}
			catch (Throwable e) {
				if (isExceptions()) {
					printExceptionSummary(e, out);
				}

				error("FAILURE IN RUNTESTS", e);
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
			FileOutputStream out = new FileOutputStream(r);
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));

			try {
				summary.print(0, pw);
			}
			finally {
				pw.close();
				out.close();
			}
			if (errors != 0)
				error("Errors found %s", errors);
		}
		finally {
			ws.close();
		}
	}

	/**
	 * Help function to run the tests
	 */
	private int runtTest(File testFile, Workspace ws, File reportDir, Tag summary) throws Exception {
		File tmpDir = new File(reportDir, "tmp");
		if (!tmpDir.exists() && !tmpDir.mkdirs()) {
			throw new IOException("Could not create directory " + tmpDir);
		}
		tmpDir.deleteOnExit();

		Tag test = new Tag(summary, "test");
		test.addAttribute("path", testFile.getAbsolutePath());
		if (!testFile.isFile()) {
			error("No bnd file: %s", testFile);
			test.addAttribute("exception", "No bnd file found");
			error("No bnd file found for %s", testFile.getAbsolutePath());
			return 1;
		}

		Project project = new Project(ws, testFile.getAbsoluteFile().getParentFile(), testFile.getAbsoluteFile());
		project.setTrace(isTrace());
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
				report.renameTo(dest);
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
		}
		catch (Exception e) {
			test.addAttribute("failed", e);
			error("Exception in run %s", e);
			return 1;
		}
		finally {
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

		}
		catch (Exception e) {
			report.addAttribute("coverage-failed", e.getMessage());
		}
	}

	private void doCoverage(Tag report, Document doc, XPath xpath) throws XPathExpressionException {
		int bad = Integer.parseInt(xpath.evaluate("count(//method[count(ref)<2])", doc));
		int all = Integer.parseInt(xpath.evaluate("count(//method)", doc));
		report.addAttribute("coverage-bad", bad);
		report.addAttribute("coverage-all", all);
	}

	private void doHtmlReport(@SuppressWarnings("unused")
	Tag report, File file, Document doc, @SuppressWarnings("unused")
	XPath xpath) throws Exception {
		String path = file.getAbsolutePath();
		if (path.endsWith(".xml"))
			path = path.substring(0, path.length() - 4);
		path += ".html";
		File html = new File(path);
		trace("Creating html report: %s", html);

		TransformerFactory fact = TransformerFactory.newInstance();

		InputStream in = getClass().getResourceAsStream("testreport.xsl");
		if (in == null) {
			warning("Resource not found: test-report.xsl, no html report");
		} else {
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(html), "UTF-8");
			try {
				Transformer transformer = fact.newTransformer(new StreamSource(in));
				transformer.transform(new DOMSource(doc), new StreamResult(out));
				trace("Transformed");
			}
			finally {
				in.close();
				out.close();
			}
		}
	}

	/**
	 * Merge a bundle with its source.
	 * 
	 * @throws Exception
	 */

	@Description("Verify jars")
	@Arguments(arg = {
			"<jar path>", "[...]"
	})
	interface verifyOptions extends Options {}

	@Description("Verify jars")
	public void _verify(verifyOptions opts) throws Exception {
		for (String path : opts._()) {
			File f = getFile(path);
			if (!f.isFile()) {
				error("No such file: %ss", f);
			} else {
				Jar jar = new Jar(f);
				if (jar.getManifest() == null || jar.getBsn() == null)
					error("Not a bundle %s", f);
				else {
					Verifier v = new Verifier(jar);
					getInfo(v, f.getName());
					v.close();
				}
				jar.close();
			}
		}
	}

	/**
	 * Merge a bundle with its source.
	 * 
	 * @throws Exception
	 */

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

	@Description("Merge a binary jar with its sources. It is possible to specify  source path")
	public void _source(sourceOptions opts) throws Exception {
		List<String> arguments = opts._();
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

		Jar bin = new Jar(jarFile);
		File tmp = File.createTempFile("tmp", ".jar", jarFile.getParentFile());
		tmp.deleteOnExit();
		try {
			Jar src = new Jar(sourceFile);
			try {
				for (String path : src.getResources().keySet())
					bin.putResource("OSGI-OPT/src/" + path, src.getResource(path));
				bin.write(tmp);
			}
			finally {
				src.close();
			}
		}
		finally {
			bin.close();
		}
		tmp.renameTo(output);
	}

	/**
	 * Diff two jar files
	 * 
	 * @return
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
	 * @return
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
	 * @return
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
		ws = Workspace.getWorkspace(workspaceDir);
		ws.setTrace(isTrace());
		ws.setPedantic(isPedantic());
		ws.setExceptions(isExceptions());
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
			File projectDir = f.getParentFile();
			File workspaceDir = projectDir.getParentFile();
			ws = getWorkspace(workspaceDir);
			Project project = ws.getProject(projectDir.getName());
			if (project.isValid()) {
				project.use(this);
				return project;
			}
		}

		if (where.equals(Project.BNDFILE)) {
			return null;
		}
		error("Project not found: " + f);

		return null;
	}

	public Workspace getWorkspace(String where) throws Exception {
		Project p = getProject(where);
		if (p != null)
			return p.getWorkspace();

		File dir;
		if (where != null) {
			dir = getFile(where);
		} else
			dir = getBase();

		if (!dir.isDirectory())
			return null;

		File buildBnd = getFile(dir, "cnf/build.bnd");
		if (!buildBnd.isFile())
			return null;

		return getWorkspace(dir);
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
		File from = getFile(opts._().get(0));
		File to = getFile(opts._().get(1));
		if (opts.m2p()) {
			FileInputStream in = new FileInputStream(from);
			try {
				Properties p = new Properties();
				Manifest m = new Manifest(in);
				Attributes attrs = m.getMainAttributes();
				for (Map.Entry<Object,Object> i : attrs.entrySet()) {
					p.put(i.getKey().toString(), i.getValue().toString());
				}
				FileOutputStream fout = new FileOutputStream(to);
				try {
					if (opts.xml())
						p.storeToXML(fout, "converted from " + from);
					else
						p.store(fout, "converted from " + from);
				}
				finally {
					fout.close();
				}
			}
			finally {
				in.close();
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
		@Description("A simple assertion on a manifest header (e.g. "
				+ Constants.BUNDLE_VERSION
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

		for (String s : opts._()) {
			Jar jar = getJar(s);
			if (jar == null) {
				err.println("no file " + s);
				continue;
			}

			Domain domain = Domain.domain(jar.getManifest());
			Hashtable<String,Object> ht = new Hashtable<String,Object>();
			Iterator<String> i = domain.iterator();
			Set<String> realNames = new HashSet<String>();

			while (i.hasNext()) {
				String key = i.next();
				String value = domain.get(key).trim();
				ht.put(key.trim().toLowerCase(), value);
				realNames.add(key);
			}
			ht.put("resources", jar.getResources().keySet());
			realNames.add("resources");
			if (filter != null) {
				if (!filter.match(ht))
					continue;
			}

			Set<Instruction> unused = new HashSet<Instruction>();
			Collection<String> select = instructions.select(realNames, unused, true);
			for (String h : select) {
				if (opts.path()) {
					out.print(jar.getSource().getAbsolutePath() + ":");
				}
				if (opts.name()) {
					out.print(jar.getSource().getName() + ":");
				}
				if (opts.key()) {
					out.print(h + ":");
				}
				out.println(ht.get(h.toLowerCase()));
			}
			for (Instruction ins : unused) {
				String literal = ins.getLiteral();
				if (literal.equals("name"))
					out.println(jar.getSource().getName());
				else if (literal.equals("path"))
					out.println(jar.getSource().getAbsolutePath());
				else if (literal.equals("size") || literal.equals("length"))
					out.println(jar.getSource().length());
				else if (literal.equals("modified"))
					out.println(new Date(jar.getSource().lastModified()));
			}
		}
	}

	/**
	 * Central routine to get a JAR with error checking
	 * 
	 * @param s
	 * @return
	 */
	Jar getJar(String s) {

		File f = getFile(s);
		if (f.isFile()) {
			try {
				return new Jar(f);
			}
			catch (ZipException e) {
				error("Not a jar/zip file: %s", f);
			}
			catch (Exception e) {
				error("Opening file: %s", e, f);
			}
			return null;
		}

		try {
			URL url = new URL(s);
			return new Jar(s, url.openStream());
		}
		catch (Exception e) {
			// Ignore
		}

		error("Not a file or proper url: %s", f);
		return null;
	}

	/**
	 * Show the version of this bnd
	 * 
	 * @throws IOException
	 */

	@Description("Show version information about bnd")
	@Arguments(arg = {})
	public interface versionOptions extends Options {
		@Description("Show licensing, copyright, sha, scm, etc")
		boolean xtra();
	}

	@Description("Show version information about bnd")
	public void _version(versionOptions o) throws IOException {
		if (!o.xtra()) {
			Analyzer a = new Analyzer();
			out.println(a.getBndVersion());
			a.close();
			return;
		}
		Enumeration<URL> e = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
		while (e.hasMoreElements()) {
			URL u = e.nextElement();

			Manifest m = new Manifest(u.openStream());
			String bsn = m.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
			if (bsn != null && bsn.equals("biz.aQute.bnd")) {
				Attributes attrs = m.getMainAttributes();

				long lastModified = 0;
				try {
					lastModified = Long.parseLong(attrs.getValue(Constants.BND_LASTMODIFIED));
				}
				catch (Exception ee) {
					// Ignore
				}
				out.printf("%-40s %s\n", "Version", attrs.getValue(Constants.BUNDLE_VERSION));
				if (lastModified > 0)
					out.printf("%-40s %s\n", "From", new Date(lastModified));
				Parameters p = OSGiHeader.parseHeader(attrs.getValue(Constants.BUNDLE_LICENSE));
				for (String l : p.keySet())
					out.printf("%-40s %s\n", "License", p.get(l).get("description"));
				out.printf("%-40s %s\n", "Copyright", attrs.getValue(Constants.BUNDLE_COPYRIGHT));
				out.printf("%-40s %s\n", "Git-SHA", attrs.getValue("Git-SHA"));
				out.printf("%-40s %s\n", "Git-Descriptor", attrs.getValue("Git-Descriptor"));
				out.printf("%-40s %s\n", "Sources", attrs.getValue("Bundle-SCM"));
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

		MultiMap<String,Object> table = new MultiMap<String,Object>();
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

	}

	@Description("Grep the manifest of bundles/jar files. ")
	public void _grep(grepOptions opts) throws Exception {
		List<String> args = opts._();
		String s = args.remove(0);
		Pattern pattern = Glob.toPattern(s);
		if (pattern == null) {
			messages.InvalidGlobPattern_(s);
			return;
		}

		if (args.isEmpty()) {
			args = new ExtList<String>(getBase().list(new FilenameFilter() {

				public boolean accept(File dir, String name) {
					return name.endsWith(".jar");
				}
			}));
		}

		Set<String> headers = opts.headers();
		if (headers == null)
			headers = new TreeSet<String>();

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

			JarInputStream in = new JarInputStream(new FileInputStream(file));
			try {
				Manifest m = in.getManifest();
				for (Object header : m.getMainAttributes().keySet()) {
					Attributes.Name name = (Name) header;
					if (instructions.isEmpty() || instructions.matches(name.toString())) {
						String h = m.getMainAttributes().getValue(name);
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
			finally {
				in.close();
			}
		}
	}

	/**
	 * Handle the global settings
	 */
	@Description("Set bnd/jpm global variables. The key can be wildcard.")
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

		@Description("Show key in hex")
		boolean hex();
	}

	@Description("Set bnd/jpm global variables")
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
					s = s.trim();
					Matcher m = ASSIGNMENT.matcher(s);
					trace("try %s", s);
					if (m.matches()) {
						String key = m.group(1);
						Instructions instr = new Instructions(key);
						Collection<String> select = instr.select(settings.keySet(), true);

						// check if there is a value a='b'

						String value = m.group(4);
						if (value == null || value.trim().length() == 0) {
							// no value
							// check '=' presence
							if (m.group(2) == null) {
								list(select, settings);
							} else {
								// we have 'a=', remove
								for (String k : select) {
									trace("remove %s=%s", k, settings.get(k));
									settings.remove(k);
									set = true;
								}
							}
						} else {
							trace("assignment %s=%s", key, value);
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

	enum Alg {
		SHA1, MD5, TIMELESS
	};

	/**
	 * hash a file
	 * 
	 * @throws Exception
	 * @throws NoSuchAlgorithmException
	 */
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

	@Description("Digests a number of files")
	public void _digest(hashOptions o) throws NoSuchAlgorithmException, Exception {
		long start = System.currentTimeMillis();
		long total = 0;
		List<Alg> algs = o.algorithm();
		if (algs == null)
			algs = Arrays.asList(Alg.SHA1);

		for (String s : o._()) {
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
							digest = SHA1.digest(f).digest();
							break;
						case MD5 :
							digest = MD5.digest(f).digest();
							break;

						case TIMELESS :
							Jar j = new Jar(f);
							digest = j.getTimelessDigest();
							break;
					}

					StringBuilder sb = new StringBuilder();
					String del = "";

					if (o.hex() || !o.b64()) {
						sb.append(del).append(Hex.toHexString(digest));
						del = " ";
					}
					if (o.b64()) {
						sb.append(del).append(Base64.encodeBase64(digest));
						del = " ";
					}
					if (o.name()) {
						sb.append(del).append(f.getAbsolutePath());
						del = " ";
					}
					if (o.process()) {
						sb.append(del).append(System.currentTimeMillis() - now).append(" ms ")
								.append(f.length() / 1000).append(" Kb");
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
			out.format("Total %s Mb, %s ms, %s Mb/sec %s files\n", mb, time, (total / time) / 1024, o._().size());
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
		mc.setTrace(isTrace());
		mc.setExceptions(isExceptions());
		mc.setPedantic(isPedantic());
		mc.run(options._().toArray(new String[0]), 1);
		getInfo(mc);
	}

	@Description("Generate autocompletion file for bash")
	public void _generate(Options options) throws Exception {
		File tmp = File.createTempFile("bnd-completion", ".tmp");
		tmp.deleteOnExit();

		try {
			IO.copy(getClass().getResource("bnd-completion.bash"), tmp);

			Sed sed = new Sed(tmp);
			sed.setBackup(false);

			Reporter r = new ReporterAdapter();
			CommandLine c = new CommandLine(r);
			Map<String,Method> commands = c.getCommands(this);
			StringBuilder sb = new StringBuilder();
			for (String commandName : commands.keySet()) {
				sb.append(" " + commandName);
			}
			sb.append(" help");

			sed.replace("%listCommands%", sb.toString().substring(1));
			sed.doIt();
			IO.copy(tmp, out);
		}
		finally {
			tmp.delete();
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
			if (filter.matcher(o.title()).matches()) {
				trace("actionable %s - %s", o, o.title());
				Map<String,Runnable> map = o.actions();
				if (map != null) {
					if (opts._().isEmpty()) {
						out.printf("# %s%n", o.title());
						if (opts.tooltip() && o.tooltip() != null) {
							out.printf("%s%n", o.tooltip());
						}
						out.printf("## actions%n");
						for (String entry : map.keySet()) {
							out.printf("  %s%n", entry);
						}
					} else {
						for (String entry : opts._()) {
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

	static Pattern	BUG_P			= Pattern.compile("#([0-9]+)");
	static Pattern	BND_COMMAND_P	= Pattern.compile("\\[bnd\\s+([\\w\\d]+)\\s*\\]");

	public void _changes(ChangesOptions options) {
		boolean first = true;
		Justif j = new Justif(80, 10);
		Formatter f = j.formatter();

		for (Map.Entry<Version,String[]> e : About.CHANGES.entrySet()) {
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
						j.indent(10, ff.out().toString());
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
		List<File> files = new ArrayList<File>();

		List<String> args = options._();
		if (args.size() == 0) {
			Project p = getProject();
			if (p == null) {
				error("This is not a project directory and you have specified no jar files ...");
				return;
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
			trace("find %s", f);
			Jar jar = new Jar(f);
			try {
				Manifest m = jar.getManifest();
				if (m != null) {
					Domain domain = Domain.domain(m);

					if (options.exports() != null) {
						Parameters ep = domain.getExportPackage();
						for (Glob g : options.exports()) {
							for (Entry<String,Attrs> exp : ep.entrySet()) {
								if (g.matcher(exp.getKey()).matches()) {
									String v = exp.getValue().get(VERSION_ATTRIBUTE);
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
							for (Entry<String,Attrs> imp : ip.entrySet()) {
								if (g.matcher(imp.getKey()).matches()) {
									String v = imp.getValue().get(VERSION_ATTRIBUTE);
									if (v == null)
										v = "0";
									out.printf("<%s: %s-%s%n", f.getPath(), imp.getKey(), v);
								}
							}
						}
					}
				}
			}
			finally {
				jar.close();
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
		if (!out.getParentFile().isDirectory()) {
			error("Output file is not in a valid directory: %s", out.getParentFile());
		}
		Jar jar = new Jar(name);
		addClose(jar);
		List<String> list = options._();
		Collections.reverse(list);

		try {
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
		finally {
			jar.close();
		}
	}

	/**
	 * Show the class versions used in a JAR
	 * 
	 * @throws Exception
	 */

	@Arguments(arg = "<jar-file>...")
	@Description("Show the Execution Environments of a JAR")
	interface EEOptions extends Options {

	}

	public void _ees(Options options) throws Exception {
		for (String path : options._()) {
			File f = getFile(path);
			if (!f.isFile()) {
				error("Not a file");
			} else {
				Jar jar = new Jar(f);
				Analyzer a = new Analyzer(this);
				try {
					a.setJar(jar);
					a.analyze();
					out.printf("%s %s%n", jar.getName(), a.getEEs());
				}
				finally {
					a.close();
				}
			}
		}
	}

	/**
	 * Lets see if we can build in parallel
	 * 
	 * @throws Exception
	 */

	@Description("experimental - parallel build")
	interface ParallelBuildOptions extends buildoptions {

	}

	public void __par(final ParallelBuildOptions options) throws Exception {
		ExecutorService pool = Executors.newCachedThreadPool();
		final AtomicBoolean quit = new AtomicBoolean();

		try {
			final Project p = getProject(options.project());
			final Workspace workspace = p == null || options.full() ? Workspace.getWorkspace(getBase()) : p
					.getWorkspace();

			if (!workspace.exists()) {
				error("cannot find workspace");
				return;
			}

			final Collection<Project> targets = p == null ? workspace.getAllProjects() : p.getDependson();

			final Forker<Project> forker = new Forker<Project>(pool);

			for (final Project dep : targets) {
				forker.doWhen(dep.getDependson(), dep, new Runnable() {

					public void run() {
						System.out.println("start " + dep);
						if (!quit.get()) {

							try {
								dep.compile(false);
								if (!quit.get())
									dep.build();
								if (!dep.isOk())
									quit.set(true);
							}
							catch (Exception e) {
								e.printStackTrace();
							}
							System.out.println("done " + dep);
						}
					}
				});
			}
			System.err.flush();

			forker.start(20000);

			for (Project dep : targets) {
				getInfo(dep, dep + ": ");
			}
			if (p != null && p.isOk() && !options.full()) {
				p.compile(options.test());
				p.build();
				if (options.test() && p.isOk())
					p.test();
				getInfo(p);
			}

			workspace.close();
		}
		finally {
			pool.shutdownNow();
		}
	}

	/**
	 * Force a cache update of the workspace
	 * 
	 * @throws Exception
	 */

	public void _sync(projectOptions options) throws Exception {
		Workspace ws = null;
		Project project = getProject(options.project());
		if (project != null) {
			ws = project.getWorkspace();
		} else {
			File cnf = getFile("cnf");
			if (cnf.isDirectory()) {
				ws = Workspace.getWorkspace(cnf.getParentFile());
			}
		}
		if (ws == null) {
			error("Cannot find workspace, either reside in a project directory, point to a project with --project, or reside in the workspace directory");
			return;
		}

		ws.syncCache();
	}

	/**
	 * From a set of bsns, create a list of urls
	 */

	interface Bsn2UrlOptions extends projectOptions {

	}

	static Pattern	LINE_P	= Pattern.compile("\\s*(([^\\s]#|[^#])+)(\\s*#.*)?");

	public void _bsn2url(Bsn2UrlOptions opts) throws Exception {
		Project p = getProject(opts.project());

		if (p == null) {
			error("You need to be in a project or specify the project with -p/--project");
			return;
		}

		MultiMap<String,Version> revisions = new MultiMap<String,Version>();

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

		List<String> files = opts._();

		for (String f : files) {
			BufferedReader r = IO.reader(getFile(f));
			try {
				String line;
				nextLine: while ((line = r.readLine()) != null) {
					Matcher matcher = LINE_P.matcher(line);
					if (!matcher.matches())
						continue nextLine;

					line = matcher.group(1);

					Parameters bundles = new Parameters(line);
					for (Map.Entry<String,Attrs> entry : bundles.entrySet()) {

						String bsn = entry.getKey();
						VersionRange range = new VersionRange(entry.getValue().getVersion());

						List<Version> versions = revisions.get(bsn);
						if (versions == null) {
							error("No for versions for " + bsn);
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

									out.println(descriptor.url + " #" + descriptor.bsn + ";version="
											+ descriptor.version);
								}
							}
						}

					}

				}
			}
			catch (Exception e) {
				error("failed to create url list from file %s : %s", f, e);
			}
			finally {
				r.close();
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
			if (s.trim().length() == 0)
				s = o.getClass().getName();

			out.printf("%03d %s%n", n++, s);
		}

	}
}

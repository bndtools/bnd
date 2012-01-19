package aQute.bnd.main;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.*;
import java.util.prefs.*;
import java.util.regex.*;
import java.util.zip.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.xpath.*;

import org.w3c.dom.*;

import aQute.bnd.build.*;
import aQute.bnd.maven.*;
import aQute.bnd.maven.support.*;
import aQute.bnd.service.*;
import aQute.bnd.service.RepositoryPlugin.Strategy;
import aQute.bnd.service.action.*;
import aQute.bnd.settings.*;
import aQute.configurable.*;
import aQute.lib.collections.*;
import aQute.lib.deployer.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.Descriptors.PackageRef;
import aQute.lib.osgi.Descriptors.TypeRef;
import aQute.lib.osgi.eclipse.*;
import aQute.lib.tag.*;
import aQute.libg.classdump.*;
import aQute.libg.generics.*;
import aQute.libg.header.*;
import aQute.libg.version.*;

/**
 * Utility to make bundles. Should be areplace for jar and much more.
 * 
 * TODO Add Javadoc comment for this type.
 * 
 * @version $Revision: 1.14 $
 */
public class bnd extends Processor {
	Settings		settings	= new Settings();
	PrintStream		out			= System.out;

	static Pattern	JARCOMMANDS	= Pattern
										.compile("(cv?0?(m|M)?f?)|(uv?0?M?f?)|(xv?f?)|(tv?f?)|(i)");

	static Pattern	COMMAND		= Pattern.compile("\\w[\\w\\d]+");

	interface bndoptions extends Options {
		@Config(description = "Turns errors into warnings so command always succeeds") boolean failok();

		@Config(description = "Report errors pedantically") boolean pedantic();

		@Config(description = "Print out stack traces when there is an unexpected exception") boolean exceptions();

		@Config(description = "Redirect output") File output();

		@Config(description = "Use as base directory") String base();

		@Config(description = "Trace progress") boolean trace();

		@Config(description = "Error/Warning ignore patterns") String[] ignore();

	}

	public static void main(String args[]) throws Exception {
		Command getopt = new Command(System.out);
		bnd main = new bnd();
		ExtList<String> args2 = new ExtList<String>(args);
		getopt.execute(main, "run", args2);
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
		Matcher m = JARCOMMANDS.matcher(arg);
		if (m.matches()) {
			rewriteJarCmd(args);
			return;
		}

		Project project = getProject();
		if (project != null) {
			Action a = project.getActions().get(arg);
			if (a != null) {
				args.add(0, "project");
			}
		}

		m = COMMAND.matcher(args.get(0));
		if (!m.matches()) {
			args.add(0, "do");
		}

	}

	private void rewriteJarCmd(List<String> args) {
		String jarcmd = args.remove(0);

		char cmd = jarcmd.charAt(0);
		switch (cmd) {
		case 'c':
			args.add(0, "create");
			break;

		case 'u':
			args.add(0, "update");
			break;

		case 'x':
			args.add(0, "extract");
			break;

		case 't':
			args.add(0, "type");
			break;

		case 'i':
			args.add(0, "index");
			break;
		}
		int start = 1;
		for (int i = 1; i < jarcmd.length(); i++) {
			switch (jarcmd.charAt(i)) {
			case 'v':
				args.add(start++, "--verbose");
				break;

			case '0':
				args.add(start++, "--nocompression");
				break;

			case 'm':
				args.add(start++, "--manifest");
				start++; // make the manifest file the parameter
				break;

			case 'M':
				args.add(start++, "--nomanifest");
				break;

			case 'f':
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
	void _run(bndoptions options) throws Exception {
		try {
			set(FAIL_OK, options.failok() + "");
			setExceptions(options.exceptions());
			setTrace(options.trace());
			setPedantic(options.pedantic());
			if (options.base() != null)
				setBase(getFile(getBase(), options.base()));

			// And the properties
			for (Entry<String, String> entry : options._properties().entrySet()) {
				setProperty(entry.getKey(), entry.getValue());
			}

			Command getopt = options._command();
			List<String> args = options._();
			rewrite(args);
			System.out.println(" subcmds " + args);

			if (args.isEmpty()) {
				getopt.help(this, null);
			} else {
				String arg = args.remove(0);
				getopt.execute(this, arg, args);

			}
		} catch (Throwable t) {
			error("Failed %s", t, t.getMessage());
		}

		if (!check(options.ignore())) {
			System.err.flush();
			System.out.flush();
			Thread.sleep(1000);
			System.exit(getErrors().size());
		}
	}

	/**
	 * Options for the jar create command.
	 * 
	 */
	interface create extends Options {
		boolean verbose();

		boolean nocompression();

		boolean nomanifest();

		String manifest();

		String file();

		String Cdir();

		String bsn();

		Version version();
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
	public void _create(create options) throws Exception {
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
				out.printf("Adding manifest from %s\n", manifest);

			jar.setManifest(getFile(manifest));
		}

		if (options.nomanifest()) {
			jar.setManifest((Manifest) null);
		} else {
			Analyzer w = new Analyzer(this);
			w.setBase(getBase());
			w.use(this);

			if ( options.bsn() != null)
				w.setBundleSymbolicName(options.bsn());
			if ( options.version() != null)
				w.setBundleVersion(options.version().toString());
			
			File out = w.getOutputFile(options.file());
			// TODO
		}

		String jarFile = options.file();
		if (jarFile == null)
			jar.write(System.out);
		else
			jar.write(jarFile);

		jar.close();

	}

	private void add(Jar jar, File base, String path, boolean report) {
		if (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);

		File f;
		if (path.equals("."))
			f = base;
		else
			f = getFile(base, path);

		out.printf("Adding: %s\n", path);

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
	 * The do command interprets files and does a default action for each file
	 * 
	 * @param project
	 * @param args
	 * @param i
	 * @return
	 * @throws Exception
	 */

	interface dooptions extends Options {
		String output();

		boolean force();
	}

	public void _do(dooptions options) throws Exception {
		for (String path : options._()) {
			if (path.endsWith(Constants.DEFAULT_BND_EXTENSION)) {
				Builder b = new Builder();
				File f = getFile(path);
				b.setProperties(f);
				b.build();
				
				File out = b.getOutputFile(options.output());
				getInfo(b, f.getName());
				if ( isOk()) {
					b.save(out,options.force());
				}
				b.close();
			} else if (path.endsWith(Constants.DEFAULT_JAR_EXTENSION)
					|| path.endsWith(Constants.DEFAULT_BAR_EXTENSION)) {
				File file = getFile(path);
				doPrint(file, MANIFEST);
			} else if (path.endsWith(Constants.DEFAULT_BNDRUN_EXTENSION))
				doRun(path);
			else
				error("Unrecognized file type %s", path);
		}
	}

	/**
	 * Project command, executes actions.
	 */

	interface projectoptions extends Options {
		String project();

		boolean info();
	}

	public void _project(projectoptions options) throws Exception {
		Project project = getProject(options.project());
		System.out.println("project " + project + " " + options.info());
		if (project == null) {
			error("No project available");
			return;
		}

		if (options.info()) {
			out.printf("Nam          %s\n", project.getName());
			out.printf("Actions      %s\n", project.getActions().keySet());
			out.printf("Directory    %s\n", project.getBase());
			out.printf("Depends on   %s\n", project.getDependson());
			out.printf("Sub builders %s\n", project.getSubBuilders());
			return;
		}

		List<String> l = new ArrayList<String>(options._());
		String cmd = null;
		String arg = null;

		if (!l.isEmpty())
			cmd = l.remove(0);
		if (!l.isEmpty())
			arg = l.remove(0);

		if (!l.isEmpty()) {
			error("Extra arguments %s", options._());
			return;
		}

		if (cmd == null) {
			error("No cmd for project");
			return;
		}

		Action a = project.getActions().get(cmd);
		if (a != null) {
			a.execute(project, arg);
			getInfo(project);
			return;
		}
	}

	private void doRun(String path) throws Exception {
		File file = getFile(path);
		if (!file.isFile())
			throw new FileNotFoundException(path);

		File projectDir = file.getParentFile();
		File workspaceDir = projectDir.getParentFile();
		if (workspaceDir == null) {
			workspaceDir = new File(System.getProperty("user.home") + File.separator + ".bnd");
		}
		Workspace ws = Workspace.getWorkspace(workspaceDir);

		File bndbnd = new File(projectDir, Project.BNDFILE);
		Project project;
		if (bndbnd.isFile()) {
			project = new Project(ws, projectDir, bndbnd);
			project.doIncludeFile(file, true, project.getProperties());
		} else
			project = new Project(ws, projectDir, file);

		project.setTrace(isTrace());
		project.setPedantic(isPedantic());
		try {
			project.run();

		} catch (Exception e) {
			error("Failed to run %s: %s", project, e);
		}
		getInfo(project);
	}

	/**
	 * Bump a version number
	 * 
	 * @param args
	 * @param i
	 * @throws Exception
	 */
	interface bumpoptions extends Options {
		String project();
	}

	public void _bump(bumpoptions options) throws Exception {
		Project project = getProject(options.project());

		if (project == null) {
			error("No project found, use -base <dir> bump");
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
			else if (!mask.matches("(+=0){1,3}")) {
				error("Invalid mask for version bump %s, is (minor|major|micro|<mask>), see $version for mask",
						mask);
				return;
			}
		}

		if (mask == null)
			project.bump();
		else
			project.bump(mask);

		getInfo(project);
		out.println(project.getProperty(BUNDLE_VERSION, "No version found"));
	}

	/**
	 * List all deliverables for this workspace.
	 * 
	 */
	interface deliverableOptions extends Options {
		String project();

		boolean limit();
	}

	public void _deliverables(deliverableOptions options) throws Exception {
		Project project = getProject(options.project());
		if (project == null) {
			error("No project");
			return;
		}

		long start = System.currentTimeMillis();
		Collection<Project> projects;
		if (options.limit())
			projects = Arrays.asList(project);
		else
			projects = project.getWorkspace().getAllProjects();

		List<Container> containers = new ArrayList<Container>();

		for (Project p : projects) {
			containers.addAll(p.getDeliverables());
		}
		long duration = System.currentTimeMillis() - start;
		out.println("Took " + duration + " ms");

		for (Container c : containers) {
			Version v = new Version(c.getVersion());
			out.printf("%-40s %8s  %s\n", c.getBundleSymbolicName(), v.getWithoutQualifier(),
					c.getFile());
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
	interface macroOptions extends Options {
		String project();
	}

	public void _macro(macroOptions options) throws Exception {
		Project project = getProject(options.project());

		if (project == null) {

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
		out.println(sb);
	}

	/**
	 * Release the project
	 * 
	 * @param args
	 * @param i
	 * @throws Exception
	 */
	interface releaseOptions extends Options {
		String project();

		boolean test();
	}

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

	interface xrefOptions extends Options {
	}

	public void _xref(xrefOptions options) {
		Analyzer analyzer = new Analyzer();
		MultiMap<TypeRef, TypeRef> table = new MultiMap<TypeRef, TypeRef>();
		Set<TypeRef> set = Create.set();

		for (String arg : options._()) {
			try {
				File file = new File(arg);
				Jar jar = new Jar(file.getName(), file);
				try {
					for (Map.Entry<String, Resource> entry : jar.getResources().entrySet()) {
						String key = entry.getKey();
						Resource r = entry.getValue();
						if (key.endsWith(".class")) {
							TypeRef ref = analyzer.getTypeRefFromPath(key);
							set.add(ref);

							InputStream in = r.openInputStream();
							Clazz clazz = new Clazz(analyzer, key, r);

							// TODO use the proper bcp instead
							// of using the default layout
							Set<TypeRef> s = clazz.parseClassFile();
							table.addAll(ref, s);
							set.addAll(s);
							in.close();
						}
					}
				} finally {
					jar.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		SortedList<TypeRef> labels = new SortedList<TypeRef>(table.keySet());
		for (TypeRef element : labels) {
			Iterator<TypeRef> row = table.get(element).iterator();
			String first = "";
			if (row.hasNext())
				first = row.next().getFQN();
			out.printf("%40s > %s\n", element.getFQN(), first);
			while (row.hasNext()) {
				out.printf("%40s   %s\n", "", row.next().getFQN());
			}
		}
	}

	interface eclipseOptions extends Options {
		String dir();
	}

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
			out.println("Classpath    " + eclipse.getClasspath());
			out.println("Dependents   " + eclipse.getDependents());
			out.println("Sourcepath   " + eclipse.getSourcepath());
			out.println("Output       " + eclipse.getOutput());
			out.println();
		}
	}

	/**
	 * Buildx
	 */
	final static int	BUILD_SOURCES	= 1;
	final static int	BUILD_POM		= 2;
	final static int	BUILD_FORCE		= 4;

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

	public void _buildx(buildxOptions options) throws Exception {
		File output = null;
		if (options.output() == null)
			output = getFile(options.output());

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
				EclipseClasspath ep = new EclipseClasspath(this, getBase().getParentFile(),
						getBase());

				b.addClasspath(ep.getClasspath());
				b.addClasspath(ep.getBootclasspath());
				b.addSourcepath(ep.getSourcepath());
			}

			Jar jar = b.build();

			File outputFile = b.getOutputFile(options.output());
			
			if (options.pom()) {
				Resource r = new PomFromManifest(jar.getManifest());
				jar.putResource("pom.xml", r);
				String path = output.getName().replaceAll("\\.jar$", ".pom");
				if (path.equals(output.getName()))
					path = output.getName() + ".pom";
				File pom = new File(output.getParentFile(), path);
				OutputStream out = new FileOutputStream(pom);
				try {
					r.write(out);
				} finally {
					out.close();
				}
			}

			getInfo(b, b.getPropertiesFile().getName());
			if ( isOk() ) {
				b.save(outputFile,options.force());
			}
			b.close();
		}
	}

	// Find the build order
	// by recursively passing
	// through the builders.
	private void prebuild(List<String> set, List<String> order, List<Builder> builders, String s)
			throws IOException {
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


	/**
	 * View files from JARs
	 * 
	 * We parse the commandline and print each file on it.
	 * 
	 * @param args
	 * @param i
	 * @throws Exception
	 */
	interface viewOptions extends Options {
		String charset();
	}

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

		if (args.isEmpty())
			args.add("*");

		Instructions instructions = new Instructions(args);
		Collection<String> selected = instructions.select(jar.getResources().keySet());
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

	/**
	 * Wrap a jar to a bundle.
	 * 
	 * @param args
	 * @param i
	 * @throws Exception
	 */
	interface wrapOptions extends Options {
		String output();

		String properties();

		List<String> classpath();

		boolean force();

		String bsn();

		Version version();
	}

	public void _wrap(wrapOptions options) throws Exception {
		List<File> classpath = Create.list();
		File properties = getBase();

		if (options.properties() != null) {
			properties = getFile(options.properties());
		}

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

				wrapper.setJar(file);
				if (options.bsn() != null)
					wrapper.setBundleSymbolicName(options.bsn());

				if (options.version() != null)
					wrapper.setBundleVersion(options.version());

				File outputFile = wrapper.getOutputFile(options.output());

				File p = properties;
				if (p.isDirectory()) {
					p = getFile(p, file.getName());
				}
				if (p.isFile())
					wrapper.setProperties(p);

				wrapper.calcManifest();

				if (wrapper.isOk()) {
					wrapper.save(outputFile, options.force());
				}
			} finally {
				wrapper.close();
			}
		}
	}

	/**
	 * Print out a JAR
	 */

	final static int	VERIFY		= 1;

	final static int	MANIFEST	= 2;

	final static int	LIST		= 4;

	final static int	ECLIPSE		= 8;
	final static int	IMPEXP		= 16;
	final static int	USES		= 32;
	final static int	USEDBY		= 64;
	final static int	COMPONENT	= 128;
	final static int	METATYPE	= 256;

	static final int	HEX			= 0;

	interface printOptions extends Options {
		boolean verify();

		boolean manifest();

		boolean list();

		boolean eclipse();

		boolean impexp();

		boolean uses();

		boolean by();

		boolean component();

		boolean typemeta();

		boolean hex();
	}

	public void _print(printOptions options) throws Exception {
		for (String s : options._()) {
			File file = getFile(s);
			if (!file.isFile()) {
				error("File %s does not exist", file);
				continue;
			}
			int opts = 0;
			if (options.verify())
				opts |= VERIFY;

			if (options.manifest())
				opts |= MANIFEST;

			if (options.list())
				opts |= LIST;

			if (options.eclipse())
				opts |= ECLIPSE;

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
				opts = MANIFEST;

			doPrint(file, opts);
		}
	}

	private void doPrint(File file, int options) throws ZipException, IOException, Exception {

		Jar jar = new Jar(file.getName(), file);
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
					warning("JAR has no manifest " + file);
				else {
					out.println("[MANIFEST " + jar.getName() + "]");
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
					print("Import-Package", new TreeMap<String, Attrs>(imports));
					print("Export-Package", new TreeMap<String, Attrs>(exports));
				} else
					warning("File has no manifest");
			}

			if ((options & (USES | USEDBY)) != 0) {
				out.println();
				Analyzer analyzer = new Analyzer();
				analyzer.setPedantic(isPedantic());
				analyzer.setJar(jar);
				analyzer.analyze();
				if ((options & USES) != 0) {
					out.println("[USES]");
					printMultiMap(analyzer.getUses());
					out.println();
				}
				if ((options & USEDBY) != 0) {
					out.println("[USEDBY]");
					printMultiMap(analyzer.getUses().transpose());
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
		} finally {
			jar.close();
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
			format("%-40s %-40s\r\n", new Object[] { key, value });
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
				InputStreamReader ir = new InputStreamReader(r.openInputStream(),
						Constants.DEFAULT_CHARSET);
				OutputStreamWriter or = new OutputStreamWriter(out, Constants.DEFAULT_CHARSET);
				try {
					IO.copy(ir, or);
				} finally {
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

		Map<String, Resource> map = jar.getDirectories().get("OSGI-INF/metatype");
		if (map != null) {
			for (Map.Entry<String, Resource> entry : map.entrySet()) {
				out.println(entry.getKey());
				IO.copy(entry.getValue().openInputStream(), out);
				out.println();
			}
			out.println();
		}
	}

	<T extends Comparable<?>> void printMultiMap(Map<T, ? extends Collection<T>> map) {
		SortedList keys = new SortedList<Object>(map.keySet());
		for (Object key : keys) {
			String name = key.toString();

			SortedList<Object> values = new SortedList<Object>(map.get(key));
			String list = vertical(40, values);
			format("%-40s %s", name, list);
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
	 * 
	 * @param msg
	 * @param ports
	 */

	private void print(String msg, Map<?, ? extends Map<?, ?>> ports) {
		if (ports.isEmpty())
			return;
		out.println(msg);
		for (Entry<?, ? extends Map<?, ?>> entry : ports.entrySet()) {
			Object key = entry.getKey();
			Map<?, ?> clause = Create.copy(entry.getValue());
			clause.remove("uses:");
			format("  %-38s %s\r\n", key.toString().trim(),
					clause.isEmpty() ? "" : clause.toString());
		}
	}

	private void format(String string, Object... objects) {
		if (objects == null || objects.length == 0)
			return;

		StringBuilder sb = new StringBuilder();
		int index = 0;
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i);
			switch (c) {
			case '%':
				String s = objects[index++] + "";
				int width = 0;
				int justify = -1;

				i++;

				c = string.charAt(i++);
				switch (c) {
				case '-':
					justify = -1;
					break;
				case '+':
					justify = 1;
					break;
				case '|':
					justify = 0;
					break;
				default:
					--i;
				}
				c = string.charAt(i++);
				while (c >= '0' && c <= '9') {
					width *= 10;
					width += c - '0';
					c = string.charAt(i++);
				}
				if (c != 's') {
					throw new IllegalArgumentException("Invalid sprintf format:  " + string);
				}

				if (s.length() > width)
					sb.append(s);
				else {
					switch (justify) {
					case -1:
						sb.append(s);
						for (int j = 0; j < width - s.length(); j++)
							sb.append(" ");
						break;

					case 1:
						for (int j = 0; j < width - s.length(); j++)
							sb.append(" ");
						sb.append(s);
						break;

					case 0:
						int spaces = (width - s.length()) / 2;
						for (int j = 0; j < spaces; j++)
							sb.append(" ");
						sb.append(s);
						for (int j = 0; j < width - s.length() - spaces; j++)
							sb.append(" ");
						break;
					}
				}
				break;

			default:
				sb.append(c);
			}
		}
		out.print(sb);
	}

	public void setOut(PrintStream out) {
		this.out = out;
	}

	public Project getProject() throws Exception {
		return getProject(null);
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
			Workspace ws = Workspace.getWorkspace(workspaceDir);
			Project project = ws.getProject(projectDir.getName());
			if (project.isValid()) {
				return project;
			}
		}
		if (where.equals(Project.BNDFILE))
			error("The current directory is not a project directory: %s", getBase());
		else
			error("Project not found: " + f);

		return null;
	}

	/**
	 * Printout all the variables.
	 * 
	 * @param args
	 * @param i
	 * @throws Exception
	 */
	interface debugOptions extends Options {
		String project();
	}

	public void _debug(debugOptions options) throws Exception {
		Processor project = getProject(options.project());
		if (project == null)
			project = this;

		MultiMap<String, String> table = new MultiMap<String, String>();

		for (Iterator<String> i = project.iterator(); i.hasNext();) {
			String key = i.next();
			String s = project.get(key);
			Collection<String> set = split(s);
			table.addAll(key, set);
		}
		printMultiMap(table);
	}

	/**
	 * Manage the repo.
	 * 
	 * <pre>
	 *  repo
	 *      list
	 *      put &lt;file|url&gt;
	 *      get &lt;bsn&gt; (&lt;version&gt;)?
	 *      fetch &lt;file|url&gt;
	 * </pre>
	 */

	public void repo(String args[], int i) throws Exception {
		String bsn = null;
		String version = null;
		List<RepositoryPlugin> repos = new ArrayList<RepositoryPlugin>();
		RepositoryPlugin writable = null;

		Project p = Workspace.getProject(getBase());
		if (p != null) {
			repos.addAll(p.getWorkspace().getRepositories());
			for (Iterator<RepositoryPlugin> rp = repos.iterator(); rp.hasNext();) {
				RepositoryPlugin rpp = rp.next();
				if (rpp.canWrite()) {
					writable = rpp;
					break;
				}
			}
		}

		for (; i < args.length; i++) {
			if ("repos".equals(args[i])) {
				int n = 0;
				for (RepositoryPlugin repo : repos) {
					out.printf("%3d. %s\n", n++, repo);
				}
				return;
			} else if ("list".equals(args[i])) {
				String mask = null;
				if (i < args.length - 1) {
					mask = args[++i];
				}
				repoList(repos, mask);
				return;
			} else if ("--repo".equals(args[i]) || "-r".equals(args[i])) {
				String location = args[++i];
				if (location.equals("maven")) {
					out.println("Maven");
					MavenRemoteRepository maven = new MavenRemoteRepository();
					maven.setProperties(new Attrs());
					maven.setReporter(this);
					repos = Arrays.asList((RepositoryPlugin) maven);
				} else {
					FileRepo repo = new FileRepo();
					repo.setReporter(this);
					repo.setLocation(location);
					repos = Arrays.asList((RepositoryPlugin) repo);
					writable = repo;
				}
			} else if ("spring".equals(args[i])) {
				// if (bsn == null || version == null) {
				// error("--bsn and --version must be set before spring command is used");
				// } else {
				// String url = String
				// .format("http://www.springsource.com/repository/app/bundle/version/download?name=%s&version=%s&type=binary",
				// bsn, version);
				// repoPut(writable, p, url, bsn, version);
				// }
				error("not supported anymore");
				return;
			} else if ("put".equals(args[i])) {
				while (i < args.length - 1) {
					String source = args[++i];
					try {

						URL url = IO.toURL(source, getBase());
						trace("put from %s", url);
						InputStream in = url.openStream();
						try {
							Jar jar = new Jar(url.toString(), in);
							Verifier verifier = new Verifier(jar);
							verifier.verify();
							getInfo(verifier);
							if (isOk()) {
								File put = writable.put(jar);
								trace("stored in %s", put);
							}
						} finally {
							in.close();
						}
					} catch (Exception e) {
						error("putting %s into %s, exception: %s", source, writable, e);
					}
				}
				return;
			} else if ("get".equals(args[i])) {
				if (i < args.length) {
					error("repo get requires a bsn, see repo help");
					return;
				}
				bsn = args[i++];
				if (i < args.length) {
					error("repo get requires a version, see repo help");
					return;
				}
				version = args[i++];

				for (RepositoryPlugin repo : repos) {
					File f = repo.get(bsn, version, Strategy.LOWEST, null);
					if (f != null) {
						if (i < args.length) {
							File out = getFile(args[i++]);
							IO.copy(f, out);
						} else
							out.println(f);

						return;
					}
				}
				error("cannot find %s-%s in %s", bsn, version, repos);
				return;
			}
		}

		if (i < args.length && !"help".equals(args[i]))
			out.println("Unknown repo command: " + args[i]);

		out.println(" bnd repo [--repo|-r ('maven'| <dir>)]*");
		out.println("        repos                          # list the repositories");
		out.println("        list                           # list all content (not always possible)");
		out.println("        get <bsn> <version> <file>?    # get an artifact");
		out.println("        put <file>+                    # put in artifacts");
		out.println("        help");
		return;
	}

	// private void repoPut(RepositoryPlugin writable, Project project, String
	// file, String bsn,
	// String version) throws Exception {
	// Jar jar = null;
	// int n = file.indexOf(':');
	// if (n > 1 && n < 10) {
	// jar = project.getValidJar(new URL(file));
	// } else {
	// File f = getFile(file);
	// if (f.isFile()) {
	// jar = project.getValidJar(f);
	// }
	// }
	//
	// if (jar != null) {
	// Manifest manifest = jar.getManifest();
	// if (bsn != null)
	// manifest.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME,
	// bsn);
	// if (version != null)
	// manifest.getMainAttributes().putValue(Constants.BUNDLE_VERSION, version);
	//
	// writable.put(jar);
	//
	// } else
	// error("There is no such file or url: " + file);
	// }

	void repoList(List<RepositoryPlugin> repos, String mask) throws Exception {
		trace("list repo " + repos + " " + mask);
		Set<String> bsns = new TreeSet<String>();
		for (RepositoryPlugin repo : repos) {
			bsns.addAll(repo.list(mask));
		}

		for (String bsn : bsns) {
			Set<Version> versions = new TreeSet<Version>();
			for (RepositoryPlugin repo : repos) {
				List<Version> result = repo.versions(bsn);
				if (result != null)
					versions.addAll(result);
			}
			out.printf("%-40s %s\n", bsn, versions);
		}
	}

	/**
	 * Patch
	 */

	void patch(String args[], int i) throws Exception {
		for (; i < args.length; i++) {
			if ("create".equals(args[i]) && i + 3 < args.length) {
				createPatch(args[++i], args[++i], args[++i]);
			} else if ("apply".equals(args[i]) && i + 3 < args.length) {
				applyPatch(args[++i], args[++i], args[++i]);
			} else if ("help".equals(args[i])) {
				out.println("patch (create <old> <new> <patch> | patch <old> <patch> <new>)");
			} else
				out.println("Patch does not recognize command? " + Arrays.toString(args));
		}
	}

	void createPatch(String old, String newer, String patch) throws Exception {
		Jar a = new Jar(new File(old));
		Manifest am = a.getManifest();
		Jar b = new Jar(new File(newer));
		Manifest bm = b.getManifest();

		Set<String> delete = newSet();

		for (String path : a.getResources().keySet()) {
			Resource br = b.getResource(path);
			if (br == null) {
				trace("DELETE    %s", path);
				delete.add(path);
			} else {
				Resource ar = a.getResource(path);
				if (isEqual(ar, br)) {
					trace("UNCHANGED %s", path);
					b.remove(path);
				} else
					trace("UPDATE    %s", path);
			}
		}

		bm.getMainAttributes().putValue("Patch-Delete", join(delete, ", "));
		bm.getMainAttributes().putValue("Patch-Version",
				am.getMainAttributes().getValue("Bundle-Version"));

		b.write(new File(patch));
		a.close();
		a.close();
	}

	private boolean isEqual(Resource ar, Resource br) throws Exception {
		InputStream ain = ar.openInputStream();
		try {
			InputStream bin = br.openInputStream();
			try {
				while (true) {
					int an = ain.read();
					int bn = bin.read();
					if (an == bn) {
						if (an == -1)
							return true;
					} else
						return false;
				}
			} finally {
				bin.close();
			}
		} finally {
			ain.close();
		}
	}

	void applyPatch(String old, String patch, String newer) throws Exception {
		Jar a = new Jar(new File(old));
		Jar b = new Jar(new File(patch));
		Manifest bm = b.getManifest();

		String patchDelete = bm.getMainAttributes().getValue("Patch-Delete");
		String patchVersion = bm.getMainAttributes().getValue("Patch-Version");
		if (patchVersion == null) {
			error("To patch, you must provide a patch bundle.\nThe given " + patch
					+ " bundle does not contain the Patch-Version header");
			return;
		}

		Collection<String> delete = split(patchDelete);
		Set<String> paths = new HashSet<String>(a.getResources().keySet());
		paths.removeAll(delete);

		for (String path : paths) {
			Resource br = b.getResource(path);
			if (br == null)
				b.putResource(path, a.getResource(path));
		}

		bm.getMainAttributes().putValue("Bundle-Version", patchVersion);
		b.write(new File(newer));
		a.close();
		b.close();
	}

	/**
	 * Run the tests from a prepared bnd file.
	 * 
	 * @param args
	 * @param i
	 * @throws Exception
	 */
	public void runtests(String args[], int i) throws Exception {
		int errors = 0;
		File cwd = new File("").getAbsoluteFile();
		Workspace ws = new Workspace(cwd);
		File reportDir = getFile("reports");

		IO.delete(reportDir);
		reportDir.mkdirs();

		Tag summary = new Tag("summary");
		summary.addAttribute("date", new Date());
		summary.addAttribute("ws", ws.getBase());

		try {
			boolean hadOne = false;

			for (; i < args.length; i++) {
				if (args[i].startsWith("-reportdir")) {
					reportDir = getFile(args[++i]).getAbsoluteFile();
					if (!reportDir.isDirectory())
						error("reportdir must be a directory " + reportDir);
				} else if (args[i].startsWith("-title")) {
					summary.addAttribute("title", args[++i]);
				} else if (args[i].startsWith("-dir")) {
					cwd = getFile(args[++i]).getAbsoluteFile();
				} else if (args[i].startsWith("-workspace")) {
					File tmp = getFile(args[++i]).getAbsoluteFile();
					ws = Workspace.getWorkspace(tmp);
				} else {
					File f = getFile(args[i]);
					errors += runtTest(f, ws, reportDir, summary);
					hadOne = true;
				}
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
		} catch (Throwable e) {
			e.printStackTrace();
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

		File r = getFile(reportDir + "/summary.xml");
		FileOutputStream out = new FileOutputStream(r);
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, Constants.DEFAULT_CHARSET));
		try {
			summary.print(0, pw);
		} finally {
			pw.close();
			out.close();
		}
		if (errors != 0)
			error("Errors found %s", errors);
	}

	private int runtTest(File testFile, Workspace ws, File reportDir, Tag summary) throws Exception {
		File tmpDir = new File(reportDir, "tmp");
		tmpDir.mkdirs();
		tmpDir.deleteOnExit();

		Tag test = new Tag(summary, "test");
		test.addAttribute("path", testFile.getAbsolutePath());
		if (!testFile.isFile()) {
			error("No bnd file: %s", testFile);
			test.addAttribute("exception", "No bnd file found");
			throw new FileNotFoundException("No bnd file found for " + testFile.getAbsolutePath());
		}

		Project project = new Project(ws, testFile.getAbsoluteFile().getParentFile(),
				testFile.getAbsoluteFile());
		project.setTrace(isTrace());
		project.setProperty(NOBUNDLES, "true");

		ProjectTester tester = project.getProjectTester();

		getInfo(project, project.toString() + ": ");

		if (!isOk())
			throw new IllegalStateException("Errors found while creating the bnd test project "
					+ testFile.getAbsolutePath());

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
			case ProjectLauncher.OK:
				return 0;

			case ProjectLauncher.CANCELED:
				test.addAttribute("failed", "canceled");
				return 1;

			case ProjectLauncher.DUPLICATE_BUNDLE:
				test.addAttribute("failed", "duplicate bundle");
				return 1;

			case ProjectLauncher.ERROR:
				test.addAttribute("failed", "unknown reason");
				return 1;

			case ProjectLauncher.RESOLVE_ERROR:
				test.addAttribute("failed", "resolve error");
				return 1;

			case ProjectLauncher.TIMEDOUT:
				test.addAttribute("failed", "timed out");
				return 1;
			case ProjectLauncher.WARNING:
				test.addAttribute("warning", "true");
				return 1;

			case ProjectLauncher.ACTIVATOR_ERROR:
				test.addAttribute("failed", "activator error");
				return 1;

			default:
				if (errors > 0) {
					test.addAttribute("errors", errors);
					return errors;
				} else {
					test.addAttribute("failed", "unknown reason");
					return 1;
				}
			}
		} catch (Exception e) {
			test.addAttribute("failed", e);
			throw e;
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
			report.addAttribute("coverage-failed", e.getMessage());
		}
	}

	private void doCoverage(Tag report, Document doc, XPath xpath) throws XPathExpressionException {
		int bad = Integer.parseInt(xpath.evaluate("count(//method[count(ref)<2])", doc));
		int all = Integer.parseInt(xpath.evaluate("count(//method)", doc));
		report.addAttribute("coverage-bad", bad);
		report.addAttribute("coverage-all", all);
	}

	private void doHtmlReport(Tag report, File file, Document doc, XPath xpath) throws Exception {
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
			FileWriter out = new FileWriter(html);
			try {
				Transformer transformer = fact.newTransformer(new StreamSource(in));
				transformer.transform(new DOMSource(doc), new StreamResult(out));
				trace("Transformed");
			} finally {
				in.close();
				out.close();
			}
		}
	}

	/**
	 * Extract a file from the JAR
	 */

	public void doExtract(String args[], int i) throws Exception {
		if (i >= args.length) {
			error("No arguments for extract");
			return;
		}

		File f = getFile(args[i++]);
		if (!f.isFile()) {
			error("No JAR file to extract from: %s", f);
			return;
		}

		if (i == args.length) {
			out.println("FILES:");
			doPrint(f, LIST);
			return;
		}
		Jar j = new Jar(f);
		try {
			Writer output = new OutputStreamWriter(out, Constants.DEFAULT_CHARSET);
			while (i < args.length) {
				String path = args[i++];

				Resource r = j.getResource(path);
				if (r == null)
					error("No such resource: %s in %s", path, f);
				else {
					InputStream in = r.openInputStream();
					IO.copy(in, output);
					output.flush();
				}
			}
		} finally {
			j.close();
		}

	}

	void doDot(String args[], int i) throws Exception {
		File out = getFile("graph.gv");
		Builder b = new Builder();

		for (; i < args.length; i++) {
			if ("-o".equals(args[i]))
				out = getFile(args[++i]);
			else if (args[i].startsWith("-"))
				error("Unknown option for dot: %s", args[i]);
			else
				b.addClasspath(getFile(args[i]));
		}
		b.setProperty(EXPORT_PACKAGE, "*");
		b.setPedantic(isPedantic());
		b.build();
		FileWriter os = new FileWriter(out);
		PrintWriter pw = new PrintWriter(os);
		try {
			pw.println("digraph bnd {");
			pw.println("  size=\"6,6\";");
			pw.println("node [color=lightblue2, style=filled,shape=box];");
			for (Entry<PackageRef, List<PackageRef>> uses : b.getUses().entrySet()) {
				for (PackageRef p : uses.getValue()) {
					if (!p.isJava())
						pw.printf("\"%s\" -> \"%s\";\n", uses.getKey(), p);
				}
			}
			pw.println("}");

		} finally {
			pw.close();
		}

	}

	public void global(String args[], int i) throws BackingStoreException {
		Settings settings = new Settings();

		if (args.length == i) {
			for (String key : settings.getKeys())
				out.printf("%-30s %s\n", key, settings.globalGet(key, "<>"));
		} else {
			while (i < args.length) {
				boolean remove = false;
				if ("-remove".equals(args[i])) {
					remove = true;
					i++;
				}
				if (i + 1 == args.length) {
					if (remove)
						settings.globalRemove(args[i]);
					else
						out.printf("%-30s %s\n", args[i], settings.globalGet(args[i], "<>"));
					i++;
				} else {
					settings.globalSet(args[i], args[i + 1]);
					i += 2;
				}
			}
		}
	}

	/**
	 * Merge a bundle with its source.
	 * 
	 * @throws Exception
	 */

	public void doMerge(String args[], int i) throws Exception {
		File out = null;
		// String prefix = "";
		// boolean maven;

		List<Jar> sourcePath = new ArrayList<Jar>();
		while (i < args.length - 1) {
			String arg = args[i++];
			if (arg.equals("-o")) {
				out = getFile(arg);
			} else if (arg.equals("-maven")) {
				// maven = true;
			} else {
				File source = getFile(arg);
				if (source.exists()) {
					Jar jar = new Jar(source);
					addClose(jar);
					sourcePath.add(jar);
				} else {
					error("Sourec file/dir does not exist");
				}
			}
		}
		if (i >= args.length) {
			error("No binary file specified");
			return;
		}

		File binary = getFile(args[i]);
		Jar output = new Jar(binary);
		try {
			Analyzer analyzer = new Analyzer();
			analyzer.setJar(output);
			analyzer.analyze();

			outer: for (Clazz clazz : analyzer.getClassspace().values()) {
				String sourcename = clazz.getSourceFile();
				String path = clazz.getAbsolutePath();
				int n = path.lastIndexOf('/');
				if (n >= 0) {
					path = path.substring(0, n + 1);
				} else
					path = "";

				String cname = path + sourcename;
				for (Jar source : sourcePath) {
					Resource r = source.getResource(cname);
					if (r != null) {
						output.putResource("OSGI-OPT/src/" + cname, r);
						continue outer;
					}
				}
				error("Source not found %s", cname);
			}

			if (out == null) {
				File backup = new File(binary.getAbsolutePath() + ".bak");
				binary.renameTo(backup);
				out = binary;
			}
			output.write(out);
		} finally {
			output.close();
			for (Jar jar : sourcePath)
				jar.close();
		}
	}

	/**
	 * Create lib file on a directory.
	 * 
	 * @throws Exception
	 * 
	 * @throws Exception
	 */

	public void doLib(String args[], int i) throws Exception {
		File out = null;
		List<File> files = new ArrayList<File>();

		while (i < args.length) {
			String arg = args[i++];
			if ("-o".equals(arg)) {
				out = getFile(args[i++]);
			} else if (arg.startsWith("-")) {
				error("Unknown option: %s", arg);
			} else
				files.add(getFile(arg));
		}
		if (files.isEmpty()) {
			error("No files specified for lib");
			return;
		}

		if (out == null) {
			out = getFile(files.get(0).getName() + ".lib");
		}

		this.out.println("Using " + out);
		Writer w = new FileWriter(out);
		try {
			for (File file : files) {
				this.out.println("And using " + file);
				if (file.isDirectory())
					doLib(file, w);
				else if (file.isFile())
					doSingleFileLib(file, w);
				else if (!file.equals(out))
					error("Not a file %s", file);
			}
		} finally {
			w.close();
		}
	}

	public void doLib(File dir, Appendable out) throws Exception {
		for (File file : dir.listFiles()) {
			doSingleFileLib(file, out);
		}
	}

	/**
	 * @param out
	 * @param file
	 * @throws IOException
	 * @throws Exception
	 */
	void doSingleFileLib(File file, Appendable out) throws IOException, Exception {
		Jar jar = new Jar(file);
		String bsn = jar.getBsn();
		this.out.println(bsn);
		String version = jar.getVersion();
		jar.close();
		if (bsn == null) {
			error("No valid bsn for %s", file);
			bsn = "not set";
		}
		if (version == null)
			version = "0";

		Version v = new Version(version);
		v = new Version(v.getMajor(), v.getMinor(), v.getMicro());
		out.append(bsn);
		out.append(";version=" + v + "\n"); // '[" + v + "," + v + "]'\n");
	}

	/**
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	protected Jar getJarFromFileOrURL(String spec) throws IOException, MalformedURLException {
		Jar jar;
		File jarFile = getFile(spec);
		if (jarFile.exists()) {
			jar = new Jar(jarFile);
		} else {
			URL url = new URL(spec);
			InputStream in = url.openStream();
			try {
				jar = new Jar(url.getFile(), in);
			} finally {
				in.close();
			}
		}
		addClose(jar);
		return jar;
	}

}

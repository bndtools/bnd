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
import aQute.lib.collections.*;
import aQute.lib.deployer.*;
import aQute.lib.getopt.*;
import aQute.lib.io.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.Descriptors.PackageRef;
import aQute.lib.osgi.eclipse.*;
import aQute.lib.tag.*;
import aQute.libg.generics.*;
import aQute.libg.header.*;
import aQute.libg.version.*;

/**
 * Utility to make bundles.
 * 
 * TODO Add Javadoc comment for this type.
 * 
 * @version $Revision: 1.14 $
 */
public class bnd extends Processor {
	Settings		settings	= new Settings();
	PrintStream		out			= System.out;
	static boolean	exceptions	= false;

	static boolean	failok		= false;
	private Project	project;

	public static void main(String args[]) {
		bnd main = new bnd();

		try {
			main.run(args);
			if (bnd.failok)
				return;

			System.exit(main.getErrors().size());

		} catch (Exception e) {
			System.err.println("Software error occurred " + e);
			if (exceptions)
				e.printStackTrace();
		}
		System.exit(-1);
	}

	void run(String[] args) throws Exception {
		int i = 0;

		try {
			for (; i < args.length; i++) {
				if ("-failok".equals(args[i])) {
					failok = true;
				} else if ("-exceptions".equals(args[i])) {
					exceptions = true;
				} else if ("-trace".equals(args[i])) {
					setTrace(true);
				} else if ("-pedantic".equals(args[i])) {
					setPedantic(true);
				} else if (args[i].indexOf('=') > 0) {
					String parts[] = args[i].split("\\s*(?!\\\\)=\\s*");
					if (parts.length == 2)
						setProperty(parts[0], parts[1]);
					else
						error("invalid property def: %s", args[i]);
				} else if ("-base".equals(args[i])) {
					setBase(new File(args[++i]).getAbsoluteFile());
					if (!getBase().isDirectory()) {
						out.println("-base must be a valid directory");
					} else if (args[i].startsWith("-"))
						error("Invalid option: ", args[i]);
				} else
					break;
			}

			project = getProject();
			if (project != null) {
				setParent(project);
				project.setPedantic(isPedantic());
				project.setTrace(isTrace());
			}
			trace("project = %s", project);

			if (i >= args.length) {
				if (project != null && project.isValid()) {
					trace("default build of current project");
					project.build();
				} else
					doHelp();
			} else {
				if (!doProject(project, args, i)) {
					if (!doCommand(args, i)) {
						doFiles(args, i);
					}
				}
			}
		} catch (Throwable t) {
			if (exceptions)
				t.printStackTrace();
			error("exception %s", t, t);
		}

		int n = 1;
		switch (getErrors().size()) {
		case 0:
			// System.err.println("No errors");
			break;
		case 1:
			System.err.println("One error");
			break;
		default:
			System.err.println(getErrors().size() + " errors");
		}
		for (String msg : getErrors()) {
			System.err.println(n++ + " : " + msg);
		}
		n = 1;
		switch (getWarnings().size()) {
		case 0:
			// System.err.println("No warnings");
			break;
		case 1:
			System.err.println("One warning");
			break;
		default:
			System.err.println(getWarnings().size() + " warnings");
		}
		for (String msg : getWarnings()) {
			System.err.println(n++ + " : " + msg);
		}

		if (getErrors().size() != 0) {
			System.err.flush();
			System.out.flush();
			Thread.sleep(1000);
			System.exit(getErrors().size());
		}
	}

	boolean doProject(Project project, String[] args, int i) throws Exception {
		if (project != null) {
			trace("project command %s", args[i]);
			Action a = project.getActions().get(args[i]);
			if (a != null) {
				a.execute(project, args[i++]);
				getInfo(project);
				return true;
			}
		}
		return false;
	}

	boolean doCommand(String args[], int i) throws Exception {
		try {
			String cmd = args[i];
			trace("command %s", cmd);
			if ("wrap".equals(args[i])) {
				doWrap(args, ++i);
			} else if ("maven".equals(args[i])) {
				MavenCommand maven = new MavenCommand(this);
				maven.setTrace(isTrace());
				maven.setPedantic(isPedantic());
				maven.run(args, ++i);
				getInfo(maven);
			} else if ("global".equals(args[i])) {
				global(args, ++i);
			} else if ("exec".equals(args[i])) {
				doRun(args[++i]);
			} else if ("print".equals(args[i])) {
				doPrint(args, ++i);
			} else if ("lib".equals(args[i])) {
				doLib(args, ++i);
			} else if ("graph".equals(args[i])) {
				doDot(args, ++i);
			} else if ("create-repo".equals(args[i])) {
				createRepo(args, ++i);
			} else if ("release".equals(args[i])) {
				doRelease(args, ++i);
			} else if ("debug".equals(args[i])) {
				debug(args, ++i);
			} else if ("bump".equals(args[i])) {
				bump(args, ++i);
			} else if ("deliverables".equals(args[i])) {
				deliverables(args, ++i);
			} else if ("view".equals(args[i])) {
				doView(args, ++i);
			} else if ("buildx".equals(args[i])) {
				doBuild(args, ++i);
			} else if ("extract".equals(args[i])) {
				doExtract(args, ++i);
			} else if ("patch".equals(args[i])) {
				patch(args, ++i);
			} else if ("runtests".equals(args[i])) {
				runtests(args, ++i);
			} else if ("xref".equals(args[i])) {
				doXref(args, ++i);
			} else if ("eclipse".equals(args[i])) {
				doEclipse(args, ++i);
			} else if ("repo".equals(args[i])) {
				repo(args, ++i);
			} else if ("package".equals(args[i])) {
				GetOpt.subcmd(new PackageCommand(this), args, ++i, out);
			} else if ("slurp".equals(args[i])) {
				SlurpCommand.slurp(this, args, ++i, out);
			} else if ("diff".equals(args[i])) {
				DiffCommand.diff(this, args, ++i, out);
			} else if ("help".equals(args[i])) {
				doHelp(args, ++i);
			} else if ("macro".equals(args[i])) {
				doMacro(args, ++i);
			} else if ("merge".equals(args[i])) {
				doMerge(args, ++i);
			} else {
				trace("command %s not found", cmd);
				return false;
			}
			trace("command %s executed", cmd);
		} catch (GetOptException e) {
			error(e.getMessage());
		}
		return true;
	}

	boolean doFiles(String args[], int i) throws Exception {
		while (i < args.length) {
			String path = args[i];
			if (path.endsWith(Constants.DEFAULT_BND_EXTENSION))
				doBuild(getFile(path), new File[0], new File[0], null, "",
						new File(path).getParentFile(), 0, new HashSet<File>());
			else if (path.endsWith(Constants.DEFAULT_JAR_EXTENSION)
					|| path.endsWith(Constants.DEFAULT_BAR_EXTENSION))
				doPrint(path, -1);
			else if (path.endsWith(Constants.DEFAULT_BNDRUN_EXTENSION))
				doRun(path);
			else
				error("Unknown file %s", path);
			i++;
		}
		return true;
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

	private void bump(String[] args, int i) throws Exception {
		if (getProject() == null) {
			error("No project found, use -base <dir> bump");
			return;
		}

		String mask = null;
		if (args.length > i) {
			mask = args[i];
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
			getProject().bump();
		else
			getProject().bump(mask);

		out.println(getProject().getProperty(BUNDLE_VERSION, "No version found"));
	}

	/**
	 * Take the current project, calculate its dependencies from the repo and
	 * create a mini-repo from it that can easily be unzipped.
	 * 
	 * @param args
	 * @param i
	 * @throws Exception
	 */
	private void createRepo(String[] args, int i) throws Exception {
		Project project = getProject();
		if (project == null) {
			error("not in a proper bnd project ... " + getBase());
			return;
		}

		File out = getFile(project.getName() + ".repo.jar");
		List<Instruction> skip = Create.list();

		while (i < args.length) {
			if (args[i].equals("-o")) {
				i++;
				if (i < args.length)
					out = getFile(args[i]);
				else
					error("No arg for -out");
			} else if (args[i].equals("-skip")) {
				i++;
				if (i < args.length) {
					Instruction instr = new Instruction(args[i]);
					skip.add(instr);
				} else
					error("No arg for -skip");
			} else
				error("invalid arg for create-repo %s", args[i]);
			i++;
		}

		Jar output = new Jar(project.getName());
		output.setDoNotTouchManifest();
		addClose(output);
		getInfo(project);
		if (isOk()) {
			for (Container c : project.getBuildpath()) {
				addContainer(skip, output, c);
			}
			for (Container c : project.getRunbundles()) {
				addContainer(skip, output, c);
			}
			if (isOk()) {
				try {
					output.write(out);
				} catch (Exception e) {
					error("Could not write mini repo %s", e);
				}
			}
		}
	}

	private void addContainer(Collection<Instruction> skip, Jar output, Container c)
			throws Exception {
		trace("add container " + c);
		String prefix = "cnf/repo/";

		switch (c.getType()) {
		case ERROR:
			error("Dependencies include %s", c.getError());
			return;

		case REPO:
		case EXTERNAL: {
			String name = getName(skip, prefix, c, ".jar");
			if (name != null)
				output.putResource(name, new FileResource(c.getFile()));
			trace("storing %s in %s", c, name);
			break;
		}

		case PROJECT:
			trace("not storing project " + c);
			break;

		case LIBRARY: {
			String name = getName(skip, prefix, c, ".lib");
			if (name != null) {
				output.putResource(name, new FileResource(c.getFile()));
				trace("store library %s", name);
				for (Container child : c.getMembers()) {
					trace("store member %s", child);
					addContainer(skip, output, child);
				}
			}
		}
		}
	}

	String getName(Collection<Instruction> skip, String prefix, Container c, String extension)
			throws Exception {
		Manifest m = c.getManifest();
		try {
			if (m == null) {
				error("No manifest found for %s", c);
				return null;
			}
			String bsn = m.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
			if (bsn == null) {
				error("No bsn in manifest: %s", c);
				return null;
			}
			for (Instruction instr : skip) {
				if (instr.matches(bsn)) {
					if (instr.isNegated()) // - * - = +!
						break;
					else
						return null; // skip it
				}
			}

			int n = bsn.indexOf(';');
			if (n > 0) {
				bsn = bsn.substring(0, n).trim();
			}
			String version = m.getMainAttributes().getValue(BUNDLE_VERSION);
			if (version == null)
				version = "0";

			Version v = new Version(version);
			version = v.getMajor() + "." + v.getMinor() + "." + v.getMicro();
			if (c.getFile() != null && c.getFile().getName().endsWith("-latest.jar"))
				version = "latest";
			return prefix + bsn + "/" + bsn + "-" + version + extension;

		} catch (Exception e) {
			error("Could not store repo file %s", c);
		}
		return null;
	}

	private void deliverables(String[] args, int i) throws Exception {
		Project project = getProject();
		long start = System.currentTimeMillis();
		Collection<Project> projects = project.getWorkspace().getAllProjects();
		List<Container> containers = new ArrayList<Container>();
		for (Project p : projects) {
			containers.addAll(p.getDeliverables());
		}
		long duration = System.currentTimeMillis() - start;
		out.println("Took " + duration + " ms");

		for (Container c : containers) {
			Version v = new Version(c.getVersion());
			out.printf("%-40s %d.%d.%d %s\n", c.getBundleSymbolicName(), v.getMajor(),
					v.getMinor(), v.getMicro(), c.getFile());
		}

	}

	private int doMacro(String[] args, int i) throws Exception {
		String result;
		for (; i < args.length; i++) {
			String cmd = args[i];
			cmd = cmd.replaceAll("@\\{([^}])\\}", "\\${$1}");
			cmd = cmd.replaceAll(":", ";");
			cmd = cmd.replaceAll("[^$](.*)", "\\${$0}");
			result = getReplacer().process(cmd);
			if (result != null && result.length() != 0) {
				Collection<String> parts = split(result);
				for (String s : parts) {
					out.println(s);
				}
			} else
				out.println("No result for " + cmd);

		}
		return i;
	}

	private void doRelease(String[] args, int i) throws Exception {
		Project project = getProject();
		project.release(false);
		getInfo(project);
	}

	/**
	 * Cross reference every class in the jar file to the files it references
	 * 
	 * @param args
	 * @param i
	 */

	private void doXref(String[] args, int i) {
		Analyzer analyzer = new Analyzer();

		for (; i < args.length; i++) {
			try {
				File file = new File(args[i]);
				Jar jar = new Jar(file.getName(), file);
				try {
					for (Map.Entry<String, Resource> entry : jar.getResources().entrySet()) {
						String key = entry.getKey();
						Resource r = entry.getValue();
						if (key.endsWith(".class")) {
							InputStream in = r.openInputStream();
							Clazz clazz = new Clazz(analyzer, key, r);

							// TODO use the proper bcp instead
							// of using the default layout

							out.print(key);
							Set<String> xref = clazz.parseClassFile();
							in.close();
							for (String element : xref) {
								out.print("\t");
								out.print(element);
							}
							out.println();
						}
					}
				} finally {
					jar.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private void doEclipse(String[] args, int i) throws Exception {
		File dir = new File("").getAbsoluteFile();
		if (args.length == i)
			doEclipse(dir);
		else {
			for (; i < args.length; i++) {
				doEclipse(new File(dir, args[i]));
			}
		}
	}

	private void doEclipse(File file) throws Exception {
		if (!file.isDirectory())
			error("Eclipse requires a path to a directory: " + file.getAbsolutePath());
		else {
			File cp = new File(file, ".classpath");
			if (!cp.exists()) {
				error("Cannot find .classpath in project directory: " + file.getAbsolutePath());
			} else {
				EclipseClasspath eclipse = new EclipseClasspath(this, file.getParentFile(), file);
				out.println("Classpath    " + eclipse.getClasspath());
				out.println("Dependents   " + eclipse.getDependents());
				out.println("Sourcepath   " + eclipse.getSourcepath());
				out.println("Output       " + eclipse.getOutput());
				out.println();
			}
		}

	}

	final static int	BUILD_SOURCES	= 1;
	final static int	BUILD_POM		= 2;
	final static int	BUILD_FORCE		= 4;

	private void doBuild(String[] args, int i) throws Exception {
		File classpath[] = new File[0];
		File workspace = null;
		File sourcepath[] = new File[0];
		File output = null;
		String eclipse = "";
		int options = 0;

		for (; i < args.length; i++) {
			if ("-workspace".startsWith(args[i])) {
				workspace = new File(args[++i]);
			} else if ("-classpath".startsWith(args[i])) {
				classpath = getClasspath(args[++i]);
			} else if ("-sourcepath".startsWith(args[i])) {
				String arg = args[++i];
				String spaces[] = arg.split("\\s*,\\s*");
				sourcepath = new File[spaces.length];
				for (int j = 0; j < spaces.length; j++) {
					File f = new File(spaces[j]);
					if (!f.exists())
						error("No such sourcepath entry: " + f.getAbsolutePath());
					sourcepath[j] = f;
				}
			} else if ("-eclipse".startsWith(args[i])) {
				eclipse = args[++i];
			} else if ("-noeclipse".startsWith(args[i])) {
				eclipse = null;
			} else if ("-output".startsWith(args[i])) {
				output = new File(args[++i]);
			} else if ("-sources".startsWith(args[i])) {
				options |= BUILD_SOURCES;
			} else if ("-pom".startsWith(args[i])) {
				options |= BUILD_POM;
			} else if ("-force".startsWith(args[i])) {
				options |= BUILD_FORCE;
			} else {
				if (args[i].startsWith("-"))
					error("Invalid option for bnd: " + args[i]);
				else {
					File properties = new File(args[i]);

					if (!properties.exists())
						error("Cannot find bnd file: " + args[i]);
					else {
						if (workspace == null)
							workspace = properties.getParentFile();

						doBuild(properties, classpath, sourcepath, output, eclipse, workspace,
								options, new HashSet<File>());
					}
					output = null;
				}
			}
		}

	}

	private File[] getClasspath(String string) {
		String spaces[] = string.split("\\s*,\\s*");
		File classpath[] = new File[spaces.length];
		for (int j = 0; j < spaces.length; j++) {
			File f = new File(spaces[j]);
			if (!f.exists())
				error("No such classpath entry: " + f.getAbsolutePath());
			classpath[j] = f;
		}
		return classpath;
	}

	private void doBuild(File properties, File classpath[], File sourcepath[], File output,
			String eclipse, File workspace, int options, Set<File> building) throws Exception {

		properties = properties.getAbsoluteFile();
		if (building.contains(properties)) {
			error("Circular dependency in pre build " + properties);
			return;
		}
		building.add(properties);

		Builder builder = new Builder();
		try {
			builder.setPedantic(isPedantic());
			builder.setProperties(properties);

			if (output == null) {
				String out = builder.getProperty("-output");
				if (out != null) {
					output = getFile(properties.getParentFile(), out);
					if (!output.getName().endsWith(".jar"))
						output.mkdirs();
				} else
					output = properties.getAbsoluteFile().getParentFile();
			}

			String prebuild = builder.getProperty("-prebuild");
			if (prebuild != null)
				prebuild(prebuild, properties.getParentFile(), classpath, sourcepath, output,
						eclipse, workspace, options, building);

			doEclipse(builder, properties, classpath, sourcepath, eclipse, workspace);

			if ((options & BUILD_SOURCES) != 0)
				builder.getProperties().setProperty("-sources", "true");

			if (failok)
				builder.setProperty(Analyzer.FAIL_OK, "true");
			Jar jar = builder.build();
			getInfo(builder);
			if (getErrors().size() > 0 && !failok)
				return;

			String name = builder.getBsn() + DEFAULT_JAR_EXTENSION;

			if (output.isDirectory())
				output = new File(output, name);

			output.getParentFile().mkdirs();

			if ((options & BUILD_POM) != 0) {
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
			jar.setName(output.getName());

			String msg = "";
			if (!output.exists() || output.lastModified() <= jar.lastModified()
					|| (options & BUILD_FORCE) != 0) {
				jar.write(output);
			} else {
				msg = "(not modified)";
			}
			statistics(jar, output, msg);
		} finally {
			builder.close();
		}
	}

	private void prebuild(String prebuild, File base, File[] classpath, File[] sourcepath,
			File output, String eclipse2, File workspace, int options, Set<File> building)
			throws Exception {

		// Force the output a directory
		if (output.isFile())
			output = output.getParentFile();

		Collection<String> parts = Processor.split(prebuild);
		for (String part : parts) {
			File f = new File(part);
			if (!f.exists())
				f = new File(base, part);
			if (!f.exists()) {
				error("Trying to build a non-existent file: " + parts);
				continue;
			}
			try {
				doBuild(f, classpath, sourcepath, output, eclipse2, workspace, options, building);
			} catch (Exception e) {
				error("Trying to build: " + part + " " + e);
			}
		}
	}

	private void statistics(Jar jar, File output, String msg) {
		out.println(jar.getName() + " " + jar.getResources().size() + " " + output.length() + msg);
	}

	/**
	 * @param properties
	 * @param classpath
	 * @param eclipse
	 * @return
	 * @throws IOException
	 */
	void doEclipse(Builder builder, File properties, File[] classpath, File sourcepath[],
			String eclipse, File workspace) throws IOException {
		if (eclipse != null) {
			File project = new File(workspace, eclipse).getAbsoluteFile();
			if (project.exists() && project.isDirectory()) {
				try {

					EclipseClasspath path = new EclipseClasspath(this, project.getParentFile(),
							project);
					List<File> newClasspath = Create.copy(Arrays.asList(classpath));
					newClasspath.addAll(path.getClasspath());
					classpath = (File[]) newClasspath.toArray(classpath);

					List<File> newSourcepath = Create.copy(Arrays.asList(sourcepath));
					newSourcepath.addAll(path.getSourcepath());
					sourcepath = (File[]) newSourcepath.toArray(sourcepath);
				} catch (Exception e) {
					if (eclipse.length() > 0)
						error("Eclipse specified (" + eclipse + ") but getting error processing: "
								+ e);
				}
			} else {
				if (eclipse.length() > 0)
					error("Eclipse specified (" + eclipse + ") but no project directory found");
			}
		}
		builder.setClasspath(classpath);
		builder.setSourcepath(sourcepath);
	}

	private void doHelp() {
		doHelp(new String[0], 0);
	}

	private void doHelp(String[] args, int i) {
		if (args.length <= i) {
			out.println("bnd -failok? -exceptions? ( wrap | print | build | eclipse | xref | view )?");
			out.println("See http://www.aQute.biz/Code/Bnd");
		} else {
			while (args.length > i) {
				if ("wrap".equals(args[i])) {
					out.println("bnd wrap (-output <file|dir>)? (-properties <file>)? <jar-file>");
				} else if ("print".equals(args[i])) {
					out.println("bnd wrap -verify? -manifest? -list? -eclipse <jar-file>");
				} else if ("build".equals(args[i])) {
					out.println("bnd build (-output <file|dir>)? (-classpath <list>)? (-sourcepath <list>)? ");
					out.println("    -eclipse? -noeclipse? -sources? <bnd-file>");
				} else if ("eclipse".equals(args[i])) {
					out.println("bnd eclipse");
				} else if ("view".equals(args[i])) {
					out.println("bnd view <file> <resource-names>+");
				}
				i++;
			}
		}
	}

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

	private void doPrint(String[] args, int i) throws Exception {
		int options = 0;

		for (; i < args.length; i++) {
			if ("-verify".startsWith(args[i]))
				options |= VERIFY;
			else if ("-manifest".startsWith(args[i]))
				options |= MANIFEST;
			else if ("-list".startsWith(args[i]))
				options |= LIST;
			else if ("-eclipse".startsWith(args[i]))
				options |= ECLIPSE;
			else if ("-impexp".startsWith(args[i]))
				options |= IMPEXP;
			else if ("-uses".startsWith(args[i]))
				options |= USES;
			else if ("-usedby".startsWith(args[i]))
				options |= USEDBY;
			else if ("-component".startsWith(args[i]))
				options |= COMPONENT;
			else if ("-metatype".startsWith(args[i]))
				options |= METATYPE;
			else if ("-all".startsWith(args[i]))
				options = -1;
			else {
				if (args[i].startsWith("-"))
					error("Invalid option for print: " + args[i]);
				else
					doPrint(args[i], options);
			}
		}
	}

	public void doPrint(String string, int options) throws Exception {
		File file = new File(string);
		if (!file.exists())
			error("File to print not found: " + string);
		else {
			if (options == 0)
				options = VERIFY | MANIFEST | IMPEXP | USES;
			doPrint(file, options);
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
					SortedSet<String> sorted = new TreeSet<String>();
					for (Object element : manifest.getMainAttributes().keySet()) {
						sorted.add(element.toString());
					}
					for (String key : sorted) {
						Object value = manifest.getMainAttributes().getValue(key);
						format("%-40s %-40s\r\n", new Object[] { key, value });
					}
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
					printMapOfSets(analyzer.getUses());
					out.println();
				}
				if ((options & USEDBY) != 0) {
					out.println("[USEDBY]");
					printMapOfSets(analyzer.getUses().transpose());
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

	void printMapOfSets(Map<? extends Comparable<?>, ? extends Collection<? extends Comparable>> map) {
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
	 * View files from JARs
	 * 
	 * We parse the commandline and print each file on it.
	 * 
	 * @param args
	 * @param i
	 * @throws Exception
	 */
	private void doView(String[] args, int i) throws Exception {
		int options = 0;
		String charset = "UTF-8";
		File output = null;

		for (; i < args.length; i++) {
			if ("-charset".startsWith(args[i]))
				charset = args[++i];
			else if ("-output".startsWith(args[i])) {
				output = new File(args[++i]);
			} else
				break;
		}

		if (i >= args.length) {
			error("Insufficient arguments for view, no JAR");
			return;
		}
		String jar = args[i++];
		if (i >= args.length) {
			error("No Files to view");
			return;
		}

		doView(jar, args, i, charset, options, output);
	}

	private void doView(String jar, String[] args, int i, String charset, int options, File output) {
		File path = new File(jar).getAbsoluteFile();
		File dir = path.getParentFile();
		if (dir == null) {
			dir = new File("");
		}
		if (!dir.exists()) {
			error("No such file: " + dir.getAbsolutePath());
			return;
		}

		String name = path.getName();
		if (name == null)
			name = "META-INF/MANIFEST.MF";

		Instruction instruction = new Instruction(path.getName());

		File[] children = dir.listFiles();
		for (int j = 0; j < children.length; j++) {
			String base = children[j].getName();
			// out.println("Considering: " +
			// children[j].getAbsolutePath() + " " +
			// instruction.getPattern());
			if (instruction.matches(base) ^ instruction.isNegated()) {
				for (; i < args.length; i++) {
					doView(children[j], args[i], charset, options, output);
				}
			}
		}
	}

	private void doView(File file, String resource, String charset, int options, File output) {
		// out.println("doView:" + file.getAbsolutePath() );
		try {
			Instruction instruction = new Instruction(resource);
			FileInputStream fin = new FileInputStream(file);
			ZipInputStream in = new ZipInputStream(fin);
			ZipEntry entry = in.getNextEntry();
			while (entry != null) {
				// out.println("view " + file + ": "
				// + instruction.getPattern() + ": " + entry.getName()
				// + ": " + output + ": "
				// + instruction.matches(entry.getName()));
				if (instruction.matches(entry.getName()) ^ instruction.isNegated())
					doView(entry.getName(), in, charset, options, output);
				in.closeEntry();
				entry = in.getNextEntry();
			}
			in.close();
			fin.close();
		} catch (Exception e) {
			out.println("Can not process: " + file.getAbsolutePath());
			e.printStackTrace();
		}
	}

	private void doView(String name, ZipInputStream in, String charset, int options, File output)
			throws Exception {
		int n = name.lastIndexOf('/');
		name = name.substring(n + 1);

		InputStreamReader rds = new InputStreamReader(in, charset);
		OutputStreamWriter wrt = new OutputStreamWriter(out, Constants.DEFAULT_CHARSET);
		if (output != null)
			if (output.isDirectory())
				wrt = new FileWriter(new File(output, name));
			else
				wrt = new FileWriter(output);

		IO.copy(rds, wrt);
		// rds.close(); also closes the stream which closes our zip it
		// seems
		if (output != null)
			wrt.close();
		else
			wrt.flush();
	}

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

	private void doWrap(String[] args, int i) throws Exception {
		int options = 0;
		File properties = null;
		File output = null;
		File classpath[] = null;
		for (; i < args.length; i++) {
			if ("-output".startsWith(args[i]))
				output = new File(args[++i]);
			else if ("-properties".startsWith(args[i]))
				properties = new File(args[++i]);
			else if ("-classpath".startsWith(args[i])) {
				classpath = getClasspath(args[++i]);
			} else {
				File bundle = new File(args[i]);
				doWrap(properties, bundle, output, classpath, options, null);
			}
		}
	}

	public boolean doWrap(File properties, File bundle, File output, File classpath[], int options,
			Map<String,String> additional) throws Exception {
		if (!bundle.exists()) {
			error("No such file: " + bundle.getAbsolutePath());
			return false;
		} else {
			Analyzer analyzer = new Analyzer();
			try {
				analyzer.setPedantic(isPedantic());
				analyzer.setJar(bundle);
				Jar dot = analyzer.getJar();

				if (properties != null) {
					analyzer.setProperties(properties);
				}
				if (additional != null)
					analyzer.putAll(additional, false);

				if (analyzer.getProperty(Analyzer.IMPORT_PACKAGE) == null)
					analyzer.setProperty(Analyzer.IMPORT_PACKAGE, "*;resolution:=optional");

				if (analyzer.getProperty(Analyzer.BUNDLE_SYMBOLICNAME) == null) {
					Pattern p = Pattern.compile("(" + Verifier.SYMBOLICNAME.pattern()
							+ ")(-[0-9])?.*\\.jar");
					String base = bundle.getName();
					Matcher m = p.matcher(base);
					base = "Untitled";
					if (m.matches()) {
						base = m.group(1);
					} else {
						error("Can not calculate name of output bundle, rename jar or use -properties");
					}
					analyzer.setProperty(Analyzer.BUNDLE_SYMBOLICNAME, base);
				}

				if (analyzer.getProperty(Analyzer.EXPORT_PACKAGE) == null) {
					String export = analyzer.calculateExportsFromContents(dot);
					analyzer.setProperty(Analyzer.EXPORT_PACKAGE, export);
				}

				if (classpath != null)
					analyzer.setClasspath(classpath);

				analyzer.mergeManifest(dot.getManifest());

				//
				// Cleanup the version ..
				//
				String version = analyzer.getProperty(Analyzer.BUNDLE_VERSION);
				if (version != null) {
					version = Builder.cleanupVersion(version);
					analyzer.setProperty(Analyzer.BUNDLE_VERSION, version);
				}

				if (output == null)
					if (properties != null)
						output = properties.getAbsoluteFile().getParentFile();
					else
						output = bundle.getAbsoluteFile().getParentFile();

				String path = bundle.getName();
				if (path.endsWith(DEFAULT_JAR_EXTENSION))
					path = path.substring(0, path.length() - DEFAULT_JAR_EXTENSION.length())
							+ DEFAULT_BAR_EXTENSION;
				else
					path = bundle.getName() + DEFAULT_BAR_EXTENSION;

				if (output.isDirectory())
					output = new File(output, path);

				analyzer.calcManifest();
				Jar jar = analyzer.getJar();
				getInfo(analyzer);
				statistics(jar, output, "");
				File f = File.createTempFile("tmpbnd", ".jar");
				f.deleteOnExit();
				try {
					jar.write(f);
					jar.close();
					if (!f.renameTo(output)) {
						IO.copy(f, output);
					}
				} finally {
					f.delete();
				}
				return getErrors().size() == 0;
			} finally {
				analyzer.close();
			}
		}
	}

	public void setOut(PrintStream out) {
		this.out = out;
	}

	public Project getProject() throws Exception {
		if (project != null)
			return project;

		try {
			project = Workspace.getProject(getBase());
			if (project == null)
				return null;

			if (!project.isValid())
				return null;

			return project;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Printout all the variables.
	 * 
	 * @param args
	 * @param i
	 * @throws Exception
	 */
	public void debug(String args[], int i) throws Exception {
		Project project = getProject();
		out.println("Project: " + project);
		Properties p = project.getFlattenedProperties();
		for (Object k : p.keySet()) {
			String key = (String) k;
			String s = p.getProperty(key);
			Collection<String> l = null;

			if (s.indexOf(',') > 0)
				l = split(s);
			else if (s.indexOf(':') > 0)
				l = split(s, "\\s*:\\s*");
			if (l != null) {
				String del = key;
				for (String ss : l) {
					out.printf("%-40s %s\n", del, ss);
					del = "";
				}
			} else
				out.printf("%-40s %s\n", key, s);
		}
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

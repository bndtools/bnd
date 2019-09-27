package aQute.bnd.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.maven.support.CachedPom;
import aQute.bnd.maven.support.Maven;
import aQute.bnd.maven.support.Pom;
import aQute.bnd.maven.support.Pom.Scope;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Descriptors.PackageRef;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.util.home.Home;
import aQute.lib.collections.LineCollection;
import aQute.lib.io.IO;
import aQute.lib.settings.Settings;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.command.Command;

public class MavenCommand extends Processor {
	private final static Logger	logger		= LoggerFactory.getLogger(MavenCommand.class);
	final Settings				settings	= new Settings(Home.getUserHomeBnd() + "/settings.json");
	File						temp;

	public MavenCommand() {}

	public MavenCommand(Processor p) {
		super(p);
	}

	/**
	 * maven deploy [-url repo] [-passphrase passphrase] [-homedir homedir]
	 * [-keyname keyname] bundle ...
	 *
	 * @param args
	 * @param i
	 * @throws Exception
	 */
	public void run(String args[], int i) throws Exception {
		temp = new File("maven-bundle");

		if (i >= args.length) {
			help();
			return;
		}

		while (i < args.length && args[i].startsWith("-")) {
			String option = args[i];
			logger.debug("option {}", option);
			if (option.equals("-temp"))
				temp = getFile(args[++i]);
			else {
				help();
				error("Invalid option %s", option);
			}
			i++;
		}

		String cmd = args[i++];

		logger.debug("temp dir {}", temp);
		IO.delete(temp);
		IO.mkdirs(temp);
		if (!temp.isDirectory())
			throw new IOException("Cannot create temp directory");

		if (cmd.equals("settings"))
			settings();
		else if (cmd.equals("help"))
			help();
		else if (cmd.equals("bundle"))
			bundle(args, i);
		else if (cmd.equals("view"))
			view(args, i);
		else
			error("No such command %s, type help", cmd);
	}

	private void help() {
		System.err.printf("Usage:%n");
		System.err.printf("  maven %n" //
			+ "  [-temp <dir>]            use as temp directory%n" //
			+ "  settings                 show maven settings%n" //
			+ "  bundle                   turn a bundle into a maven bundle%n" //
			+ "    [-properties <file>]   provide properties, properties starting with javadoc are options for javadoc, like javadoc-tag=...%n"
			+ "    [-javadoc <file|url>]  where to find the javadoc (zip/dir), otherwise generated%n" //
			+ "    [-source <file|url>]   where to find the source (zip/dir), otherwise from OSGI-OPT/src%n" //
			+ "    [-scm <url>]           required scm in pom, otherwise from Bundle-SCM%n" //
			+ "    [-url <url>]           required project url in pom%n" //
			+ "    [-bsn bsn]             overrides bsn%n" //
			+ "    [-version <version>]   overrides version%n" //
			+ "    [-developer <email>]   developer email%n" //
			+ "    [-nodelete]            do not delete temp files%n" //
			+ "    [-passphrase <gpgp passphrase>] signer password%n"//
			+ "        <file|url>%n");
	}

	/**
	 * Show the maven settings
	 *
	 * @throws FileNotFoundException
	 * @throws Exception
	 */
	private void settings() throws FileNotFoundException, Exception {
		File userHome = new File(System.getProperty("user.home"));
		File m2 = new File(userHome, ".m2");
		if (!m2.isDirectory()) {
			error("There is no m2 directory at %s", userHome);
			return;
		}
		File settings = new File(m2, "settings.xml");
		if (!settings.isFile()) {
			error("There is no settings file at '%s'", settings.getAbsolutePath());
			return;
		}
		try (LineCollection lc = new LineCollection(IO.reader(settings))) {
			while (lc.hasNext()) {
				System.err.println(lc.next());
			}
		}
	}

	/**
	 * Create a maven bundle.
	 *
	 * @param args
	 * @param i
	 * @throws Exception
	 */
	private void bundle(String args[], int i) throws Exception {
		List<String> developers = new ArrayList<>();
		Properties properties = new UTF8Properties();

		String scm = null;
		String passphrase = null;
		String javadoc = null;
		String source = null;
		String output = "bundle.jar";
		String url = null;
		String artifact = null;
		String group = null;
		String version = null;
		boolean nodelete = false;

		while (i < args.length && args[i].startsWith("-")) {
			String option = args[i++];
			logger.debug("bundle option {}", option);
			if (option.equals("-scm"))
				scm = args[i++];
			else if (option.equals("-group"))
				group = args[i++];
			else if (option.equals("-artifact"))
				artifact = args[i++];
			else if (option.equals("-version"))
				version = args[i++];
			else if (option.equals("-developer"))
				developers.add(args[i++]);
			else if (option.equals("-passphrase")) {
				passphrase = args[i++];
			} else if (option.equals("-url")) {
				url = args[i++];
			} else if (option.equals("-javadoc"))
				javadoc = args[i++];
			else if (option.equals("-source"))
				source = args[i++];
			else if (option.equals("-output"))
				output = args[i++];
			else if (option.equals("-nodelete"))
				nodelete = true;
			else if (option.startsWith("-properties")) {
				try (InputStream in = IO.stream(Paths.get(args[i++]))) {
					properties.load(in);
				} catch (Exception e) {}
			}
		}

		if (developers.isEmpty()) {
			String email = settings.remove("email");
			if (email == null)
				error(
					"No developer email set, you can set global default email with: bnd global email Peter.Kriens@aQute.biz");
			else
				developers.add(email);
		}

		if (i == args.length) {
			error("too few arguments, no bundle specified");
			return;
		}

		if (i != args.length - 1) {
			error("too many arguments, only one bundle allowed");
			return;
		}

		String input = args[i++];

		Jar binaryJar = getJarFromFileOrURL(input);
		logger.debug("got {}", binaryJar);
		if (binaryJar == null) {
			error("JAR does not exist: %s", input);
			return;
		}

		File original = getFile(temp, "original");
		IO.mkdirs(original);
		binaryJar.expand(original);
		binaryJar.calcChecksums(null);

		Manifest manifest = binaryJar.getManifest();
		logger.debug("got manifest");

		@SuppressWarnings("resource")
		PomFromManifest pom = new PomFromManifest(manifest);

		if (scm != null)
			pom.setSCM(scm);
		if (url != null)
			pom.setURL(url);
		if (artifact != null)
			pom.setArtifact(artifact);
		if (artifact != null)
			pom.setGroup(group);
		if (version != null)
			pom.setVersion(version);
		logger.debug("{}", url);
		for (String d : developers)
			pom.addDeveloper(d);

		Set<String> exports = OSGiHeader.parseHeader(manifest.getMainAttributes()
			.getValue(Constants.EXPORT_PACKAGE))
			.keySet();

		Jar sourceJar;
		if (source == null) {
			logger.debug("Splitting source code");
			sourceJar = new Jar("source");
			for (Map.Entry<String, Resource> entry : binaryJar.getResources()
				.entrySet()) {
				if (entry.getKey()
					.startsWith("OSGI-OPT/src")) {
					sourceJar.putResource(entry.getKey()
						.substring("OSGI-OPT/src/".length()), entry.getValue());
				}
			}
			copyInfo(binaryJar, sourceJar, "source");
		} else {
			sourceJar = getJarFromFileOrURL(source);
		}
		sourceJar.calcChecksums(null);

		Jar javadocJar;
		if (javadoc == null) {
			logger.debug("creating javadoc because -javadoc not used");
			javadocJar = javadoc(getFile(original, "OSGI-OPT/src"), exports, manifest, properties);
			if (javadocJar == null) {
				error("Cannot find source code in OSGI-OPT/src to generate Javadoc");
				return;
			}
			copyInfo(binaryJar, javadocJar, "javadoc");
		} else {
			logger.debug("Loading javadoc externally {}", javadoc);
			javadocJar = getJarFromFileOrURL(javadoc);
		}
		javadocJar.calcChecksums(null);

		addClose(binaryJar);
		addClose(sourceJar);
		addClose(javadocJar);

		logger.debug("creating bundle dir");
		File bundle = new File(temp, "bundle");
		IO.mkdirs(bundle);

		String prefix = pom.getArtifactId() + "-" + pom.getVersion();
		File binaryFile = new File(bundle, prefix + ".jar");
		File sourceFile = new File(bundle, prefix + "-sources.jar");
		File javadocFile = new File(bundle, prefix + "-javadoc.jar");
		File pomFile = new File(bundle, "pom.xml").getAbsoluteFile();
		logger.debug("creating output files {}, {}, {}, and {}", binaryFile, sourceFile, javadocFile, pomFile);

		IO.copy(pom.openInputStream(), pomFile);
		logger.debug("copied pom");

		logger.debug("writing binary {}", binaryFile);
		binaryJar.write(binaryFile);

		logger.debug("writing source {}", sourceFile);
		sourceJar.write(sourceFile);

		logger.debug("writing javadoc {}", javadocFile);
		javadocJar.write(javadocFile);

		sign(binaryFile, passphrase);
		sign(sourceFile, passphrase);
		sign(javadocFile, passphrase);
		sign(pomFile, passphrase);

		logger.debug("create bundle");
		Jar bundleJar = new Jar(bundle);
		addClose(bundleJar);
		File outputFile = getFile(output);
		bundleJar.write(outputFile);
		logger.debug("created bundle {}", outputFile);

		binaryJar.close();
		sourceJar.close();
		javadocJar.close();
		bundleJar.close();
		if (!nodelete)
			IO.delete(temp);
	}

	private void copyInfo(Jar source, Jar dest, String type) throws Exception {
		source.ensureManifest();
		dest.ensureManifest();
		copyInfoResource(source, dest, "LICENSE");
		copyInfoResource(source, dest, "LICENSE.html");
		copyInfoResource(source, dest, "about.html");

		Manifest sm = source.getManifest();
		Manifest dm = dest.getManifest();
		copyInfoHeader(sm, dm, Constants.BUNDLE_DESCRIPTION, "");
		copyInfoHeader(sm, dm, Constants.BUNDLE_VENDOR, "");
		copyInfoHeader(sm, dm, Constants.BUNDLE_COPYRIGHT, "");
		copyInfoHeader(sm, dm, Constants.BUNDLE_DOCURL, "");
		copyInfoHeader(sm, dm, Constants.BUNDLE_LICENSE, "");
		copyInfoHeader(sm, dm, Constants.BUNDLE_NAME, " " + type);
		copyInfoHeader(sm, dm, Constants.BUNDLE_SYMBOLICNAME, "." + type);
		copyInfoHeader(sm, dm, Constants.BUNDLE_VERSION, "");
	}

	private void copyInfoHeader(Manifest sm, Manifest dm, String key, String value) {
		String v = sm.getMainAttributes()
			.getValue(key);
		if (v == null) {
			logger.debug("no source for {}", key);
			return;
		}

		if (dm.getMainAttributes()
			.getValue(key) != null) {
			logger.debug("already have {}", key);
			return;
		}

		dm.getMainAttributes()
			.putValue(key, v + value);
	}

	private void copyInfoResource(Jar source, Jar dest, String type) {
		if (source.getResources()
			.containsKey(type)
			&& !dest.getResources()
				.containsKey(type))
			dest.putResource(type, source.getResource(type));
	}

	/**
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
			try (InputStream in = url.openStream()) {
				jar = new Jar(url.getFile(), in);
			}
		}
		addClose(jar);
		return jar;
	}

	private void sign(File file, String passphrase) throws Exception {
		logger.debug("signing {}", file);
		File asc = new File(file.getParentFile(), file.getName() + ".asc");
		IO.delete(asc);

		Command command = new Command();
		command.setTrace();

		command.add(getProperty("gpg", "gpg"));
		if (passphrase != null)
			command.add("--passphrase", passphrase);
		command.add("-ab", "--sign"); // not the -b!!
		command.add(IO.absolutePath(file));
		StringBuilder stdout = new StringBuilder();
		StringBuilder stderr = new StringBuilder();
		int result = command.execute(stdout, stderr);
		if (result != 0) {
			error("gpg signing %s failed because %s", file, "" + stdout + stderr);
		}
	}

	private Jar javadoc(File source, Set<String> exports, Manifest m, Properties p) throws Exception {
		File tmp = new File(temp, "javadoc");
		IO.mkdirs(tmp);

		Command command = new Command();
		command.add(getJavaExecutable("javadoc"));
		command.add("-quiet");
		command.add("-protected");
		// command.add("-classpath");
		// command.add(IO.absolutePath(binary));
		command.add("-d");
		command.add(IO.absolutePath(tmp));
		command.add("-charset");
		command.add("UTF-8");
		command.add("-sourcepath");
		command.add(IO.absolutePath(source));

		Attributes attr = m.getMainAttributes();
		Properties pp = new UTF8Properties(p);
		set(pp, "-doctitle", description(attr));
		set(pp, "-header", description(attr));
		set(pp, "-windowtitle", name(attr));
		set(pp, "-bottom", copyright(attr));
		set(pp, "-footer", license(attr));

		command.add("-tag");
		command.add("Immutable:t:Immutable");
		command.add("-tag");
		command.add("ThreadSafe:t:ThreadSafe");
		command.add("-tag");
		command.add("NotThreadSafe:t:NotThreadSafe");
		command.add("-tag");
		command.add("GuardedBy:mf:Guarded By:");
		command.add("-tag");
		command.add("security:m:Required Permissions");
		command.add("-tag");
		command.add("noimplement:t:Consumers of this API must not implement this interface");

		for (Enumeration<?> e = pp.propertyNames(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			String value = pp.getProperty(key);

			if (key.startsWith("javadoc")) {
				key = key.substring("javadoc".length());
				removeDuplicateMarker(key);
				command.add(key);
				command.add(value);
			}
		}
		for (String packageName : exports) {
			command.add(packageName);
		}

		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();

		System.err.println(command);

		int result = command.execute(out, err);
		if (result != 0) {
			warning("Error during execution of javadoc command: %s\n******************\n%s", out, err);
		}
		Jar jar = new Jar(tmp);
		addClose(jar);
		return jar;
	}

	/**
	 * Generate a license string
	 *
	 * @param attr
	 */
	private String license(Attributes attr) {
		Parameters map = Processor.parseHeader(attr.getValue(Constants.BUNDLE_LICENSE), null);
		if (map.isEmpty())
			return null;

		StringBuilder sb = new StringBuilder();
		String sep = "Licensed under ";
		for (Entry<String, Attrs> entry : map.entrySet()) {
			sb.append(sep);
			String key = entry.getKey();
			String link = entry.getValue()
				.get("link");
			String description = entry.getValue()
				.get("description");

			if (description == null)
				description = key;

			if (link != null) {
				sb.append("<a href='");
				sb.append(link);
				sb.append("'>");
			}
			sb.append(description);
			if (link != null) {
				sb.append("</a>");
			}
			sep = ",<br/>";
		}

		return sb.toString();
	}

	/**
	 * Generate the copyright statement.
	 *
	 * @param attr
	 */
	private String copyright(Attributes attr) {
		return attr.getValue(Constants.BUNDLE_COPYRIGHT);
	}

	private String name(Attributes attr) {
		String name = attr.getValue(Constants.BUNDLE_NAME);
		if (name == null)
			name = attr.getValue(Constants.BUNDLE_SYMBOLICNAME);
		return name;
	}

	private String description(Attributes attr) {
		String descr = attr.getValue(Constants.BUNDLE_DESCRIPTION);
		if (descr == null)
			descr = attr.getValue(Constants.BUNDLE_NAME);
		if (descr == null)
			descr = attr.getValue(Constants.BUNDLE_SYMBOLICNAME);
		return descr;
	}

	private void set(Properties pp, String option, String defaultValue) {
		String key = "javadoc" + option;
		String existingValue = pp.getProperty(key);
		if (existingValue != null)
			return;

		pp.setProperty(key, defaultValue);
	}

	/**
	 * View - Show the dependency details of an artifact
	 */

	private final static Pattern GROUP_ARTIFACT_VERSION = Pattern.compile("([^+]+)\\+([^+]+)\\+([^+]+)");

	void view(String args[], int i) throws Exception {
		Maven maven = new Maven(getExecutor());

		List<URI> urls = new ArrayList<>();
		Path output = null;
		while (i < args.length && args[i].startsWith("-")) {
			if ("-r".equals(args[i])) {
				URI uri = new URI(args[++i]);
				urls.add(uri);
				System.err.println("URI for repo " + uri);
			} else if ("-o".equals(args[i])) {
				output = Paths.get(args[++i]);
			} else
				throw new IllegalArgumentException("Unknown option: " + args[i]);

			i++;
		}

		URI[] urls2 = urls.toArray(new URI[0]);
		PrintWriter pw = IO.writer(output == null ? System.err : IO.outputStream(output));
		try {
			while (i < args.length) {
				String ref = args[i++];
				pw.println("Ref " + ref);

				Matcher matcher = GROUP_ARTIFACT_VERSION.matcher(ref);
				if (matcher.matches()) {

					String group = matcher.group(1);
					String artifact = matcher.group(2);
					String version = matcher.group(3);
					CachedPom pom = maven.getPom(group, artifact, version, urls2);

					try (Builder a = new Builder()) {
						a.setProperty(Constants.PRIVATEPACKAGE, "*");
						Set<Pom> dependencies = pom.getDependencies(Scope.compile, urls2);
						for (Pom dep : dependencies) {
							System.err.printf("%20s %-20s %10s%n", dep.getGroupId(), dep.getArtifactId(),
								dep.getVersion());
							a.addClasspath(dep.getArtifact());
						}
						pw.println(a.getClasspath());
						a.build();

						TreeSet<PackageRef> sorted = new TreeSet<>(a.getImports()
							.keySet());
						for (PackageRef p : sorted) {
							pw.printf("%-40s\n", p);
						}
						// for ( Map.Entry<String, Set<String>> entry :
						// a.getUses().entrySet()) {
						// String from = entry.getKey();
						// for ( String uses : entry.getValue()) {
						// System.err.printf("%40s %s\n", from, uses);
						// from = "";
						// }
						// }
						a.close();
					}
				} else {
					System.err.println("Wrong, must look like group+artifact+version, is " + ref);
				}
			}
		} finally {
			pw.flush();
			if (output != null) {
				IO.close(pw);
			}
		}
	}

}

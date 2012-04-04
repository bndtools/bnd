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
import aQute.lib.justif.*;
import aQute.libg.filerepo.*;
import aQute.libg.reporter.*;
import aQute.libg.version.*;

/**
 * JPM is the Java package manager. It manages a local repository in the user
 * home directory and/or a global directory. This class is the main entry point
 * for the command line.
 */
public class JavaPackageManager extends ReporterAdapter {
	/**
	 * Install a jar options
	 */
	public interface installOptions extends Options {
		String command();

		boolean nocommand();

		boolean noverify();
	}
	

	/**
	 * Main options
	 */

	interface jpmOptions extends Options {
		String home();

		boolean local();

		String repository();

		boolean exceptions();

		boolean trace();

		boolean pedantic();

		String base();

		String[] ignore();
	}

	/**
	 * Show installed binaries
	 */
	public interface listOptions extends Options {
		String filter();
	}

	/**
	 * Show platform
	 */
	public interface platformOptions extends Options {
	}
	/**
	 * Uninstall a binary.
	 * 
	 */
	public interface uninstallOptions extends Options {
		VersionRange range();
	}

	File			base				= new File(System.getProperty("user.dir"));
	String			repository			= "http://repo.libsync.org";
	File			home;
	Platform		platform			= Platform.getPlatform();
	FileRepo		repo;
	PrintStream		err					= System.err;
	PrintStream		out					= System.out;

	static Pattern	VERSION_PATTERN		= Pattern.compile("[0-9]+(\\.[0-9]+(\\.[0-9]+(\\.[0-9A-Za-z_-]+)?)?)?");
	static Pattern	BSN_PATTERN			= Pattern.compile("[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)*");

	static Pattern	COMMAND_PATTERN		= Pattern.compile("[\\w\\d]*");

	static Pattern	URL_PATTERN			= Pattern.compile("[a-zA-Z][0-9A-Za-z]{1,8}:.+");

	static Pattern	DIGEST_PATTERN		= Pattern.compile("([\\w\\d]+)-Digest",
												Pattern.CASE_INSENSITIVE);

	static Pattern	MAINCLASS_PATTERN	= Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*(\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*");

	/**
	 * Main entry
	 * 
	 * @throws Exception
	 * 
	 */
	public static void main(String args[]) throws Exception {
		JavaPackageManager jpm = new JavaPackageManager();
		jpm.run(args);
	}
	
	/** 
	 * Default constructor
	 * 
	 */

	public JavaPackageManager() {
		super(System.err);
	}
	
	/**
	 * The command line command for install.
	 * 
	 * @param opts
	 *            The options for installing
	 */
	public void _install(installOptions opts) throws Exception {
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

	public void _list(listOptions opts) throws Exception {
		for (String bsn : repo.list(opts.filter())) {
			Set<Version> versions = new TreeSet<Version>(repo.versions(bsn));
			System.out.printf("%-40s %s\n", bsn, versions);
		}
	}

	public void _uninstall(uninstallOptions opts) throws Exception {
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
	 * Really uninstall.
	 * 
	 * @param file
	 * @throws IOException
	 */
	private void uninstall(File file) throws IOException {
		JarFile jar = new JarFile(file);
		try {
			Manifest manifest = jar.getManifest();
			Attributes main = manifest.getMainAttributes();
			String command = main.getValue("System-Command");
			if (command != null) {
				command = command.trim();
				platform.unlink(command);
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
		home = opts.local() ? platform.getLocal() : platform.getGlobal();
		home.mkdirs();
		
		File r;

		if (opts.repository() != null)
			r = IO.getFile(base, opts.repository());
		else
			r = IO.getFile(home, "repo");

		r.mkdirs();

		repo = new FileRepo(r);

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
		if (!check(opts.ignore())) {
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

	private void install(File source, installOptions opts) throws IOException {
		JarFile jar = new JarFile(source);
		Manifest m = jar.getManifest();
		Attributes main = m.getMainAttributes();
		String bsn = main.getValue("Bundle-SymbolicName");
		String version = main.getValue("Bundle-Version");
		String command = main.getValue("System-Command");
		String mainClass = main.getValue("Main-Class");

		if (opts != null && opts.command() != null)
			command = opts.command();

		try {
			String msg = verify(jar, !opts.noverify());
			if (msg != null) {
				error("The JAR %s fails to verify, %s", source, msg);
				return;
			}

			bsn = verify(BSN_PATTERN, bsn);
			version = verify(VERSION_PATTERN, version);
			if (command != null)
				command = verify(COMMAND_PATTERN, command);
			if (mainClass != null)
				mainClass = verify(MAINCLASS_PATTERN, mainClass);

			if (command != null && mainClass == null)
				error("System command is specified but JAR contains no Main-Class attribute for %s",
						source);

			if (!isOk())
				return;
		} finally {
			jar.close();
		}

		Version v = new Version(version);
		File target = repo.put(bsn, v);
		if ( !target.getParentFile().canWrite()) {
			error("Cannot write repo file, try using sudo, %s", target);
			return;
		}
			
		try {
			
			copy(source, target);
			trace("copied %s to %s (%s)", source, target, target.isFile());
			
			if (command != null) {
				platform.link(command, target);
				trace("linked %s to %s", command, target);
			}
			
		} catch(Throwable t) {
			target.delete();
		}
	}

	/**
	 * Main entry for the command line
	 * 
	 * @param args
	 * @throws Exception
	 */
	private void run(String[] args) throws Exception {
		Console console = System.console();
		if (console == null) {

			Icon icon = new ImageIcon(JavaPackageManager.class.getResource("/images/jpm.png"),
					"JPM");
			int answer = JOptionPane.showOptionDialog(null,
					"This is a command line application. Setup?", "Package Manager for Java™",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, icon, null, null);
			if (answer == JOptionPane.OK_OPTION) {
				init();
			}
			return;
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
	private void init() throws Exception {
		String s = System.getProperty("java.class.path");
		if (s == null || s.indexOf(File.pathSeparator) > 0)
			JOptionPane.showMessageDialog(null,
					"Cannot initialize, class path contains multiple entries (" + s
							+ "). Run java-jar <jpm.jar> init");
		else {
			try {
				File f = new File(s).getAbsoluteFile();
				if (f.exists()) {
					CommandLine cl = new CommandLine(this);
					String help = cl.execute(this, "install", Arrays.asList(f.getAbsolutePath()));
					if (help != null)
						JOptionPane.showMessageDialog(null, help);
					else if (!isOk()) {
						StringBuilder sb = new StringBuilder();
						for (String error : getErrors()) {
							sb.append(error);
							sb.append("\n");
						}
						if (getWarnings().size() > 0) {
							sb.append("\nWarnings\n");
							for (String warning : getWarnings()) {
								sb.append(warning);
								sb.append("\n");
							}
						}
						Justif justif = new Justif(60);
						justif.wrap(sb);
						JOptionPane.showMessageDialog(null, sb.toString());						
					} else
						platform.shell("jpm hello");
				}

			} catch (InvocationTargetException e) {
				JOptionPane.showMessageDialog(null, "Failed: " + e.getCause());

			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Failed: " + e);

			}
		}
	}

	/**
	 * Verify that the jar file is correct. This also verifies ok when there are
	 * no checksums or.
	 * 
	 * @throws IOException
	 */
	String verify(JarFile jar, boolean obligatory) throws IOException {
		try {
			Manifest m = jar.getManifest();
			if (m.getEntries().isEmpty())
				return "No name sections";

			for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
				JarEntry je = e.nextElement();
				if ( je.getName().equals("META-INF/MANIFEST.MF") || je.getName().endsWith(".SF") || je.getName().endsWith("/"))
					continue;
				
				Attributes nameSection = m.getAttributes(je.getName());
				if (nameSection == null)
					return "No name section for " + je.getName();

				boolean atLeastOne = false;

				for (Object k : nameSection.keySet()) {
					Matcher matcher = DIGEST_PATTERN.matcher(k.toString());
					if (matcher.matches()) {
						String algorithm = matcher.group(1);
						try {
							MessageDigest md = MessageDigest.getInstance(algorithm);
							String expected = nameSection.getValue((Attributes.Name) k);
							byte digest[] = Base64.decodeBase64(expected);
							copy(jar.getInputStream(je), md);
							if (!Arrays.equals(digest, md.digest()))
								return "Invalid digest for " + k + ", " + expected + " != "
										+ Base64.encodeBase64(md.digest());

							atLeastOne = true;
						} catch (NoSuchAlgorithmException nsae) {
							if (obligatory)
								return "Missing digest algorithm " + algorithm;
						}
					}
				}
				if (obligatory && !atLeastOne)
					return "No digests specified";
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
}

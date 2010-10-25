package aQute.bnd.maven;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import aQute.bnd.settings.*;
import aQute.lib.collections.*;
import aQute.lib.io.*;
import aQute.lib.osgi.*;
import aQute.libg.command.*;
import aQute.libg.header.*;
import aQute.libg.reporter.*;

public class MavenCommand extends Processor {
	final Settings			settings = new Settings();

	String			repository	= "nexus";
	String			url			= "http://oss.sonatype.org/service/local/staging/deploy/maven2";
	String			homedir;
	String			keyname;
	File			temp;
	String			scm;
	String			passphrase;
	Reporter		reporter;
	List<String>	developers	= new ArrayList<String>();

	/**
	 * maven deploy [-url repo] [-passphrase passphrase] [-homedir homedir]
	 * [-keyname keyname] bundle ...
	 * 
	 * @param args
	 * @param i
	 * @throws Exception
	 */
	public void run(String args[], int i) throws Exception {
		temp = File.createTempFile("bnd", "");

		if (i >= args.length) {
			help();
			return;
		}

		String cmd = args[i++];

		while (i < args.length && args[i].startsWith("-")) {
			String option = args[i];
			if (option.equals("-url"))
				repository = args[++i];
			else if (option.equals("-temp"))
				temp = getFile(args[++i]);
			else if (option.equals("-scm"))
				scm = args[++i];
			else if (option.equals("-developer"))
				developers.add( args[++i]);
			else if (option.equals("-passphrase"))
				passphrase = args[++i];
			else if (option.equals("-url"))
				homedir = args[++i];
			else if (option.equals("-keyname"))
				keyname = args[++i];
			else
				error("Invalid command ");
			i++;
		}

		temp.mkdirs();

		if (cmd.equals("settings"))
			settings();
		else if (cmd.equals("help"))
			help();
		else if (cmd.equals("deploy"))
			bundle(args, i);
		else
			error("No such command %s, type help", cmd);
	}

	private void help() {
		System.err.println("Usage:\n");
		System.err
				.println("  deploy [-url repo] [-passphrase passphrase] [-homedir homedir] [-keyname keyname] bundle ...");
		System.err.println("  settings");
	}

	public void settings() throws FileNotFoundException, Exception {
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

		FileReader rdr = new FileReader(settings);

		LineCollection lc = new LineCollection(new BufferedReader(rdr));
		while (lc.hasNext()) {
			System.out.println(lc.next());
		}
	}

	void bundle(String args[], int i) throws Exception {
		if ( developers.isEmpty()) {
			String email = settings.globalGet(Settings.EMAIL, null);
			if ( email == null)
				error("No developer email set, you can set global default email with: bnd global email Peter.Kriens@aQute.biz");
			else
				developers.add(email);
		}
		
		
		while (i < args.length) {
			String jar = args[i++];
			File f = getFile(jar);
			if (!f.isFile()) {
				error("File does not exist: %s", f.getAbsoluteFile());
			} else {
				deploy(f);
			}
		}
	}

	void execute(String args[], int i) throws Exception {
		
		while (i < args.length) {
			String pom = args[i++];
			File f = getFile(pom);
			if (!f.isFile()) {
				error("File does not exist: %s", f.getAbsoluteFile());
			} else {
				deploy(f);
			}
		}
	}

	void deploy(File f) throws Exception {
		Jar jar = new Jar(f);
		File original = getFile(temp, "original");
		original.mkdirs();
		jar.expand(original);

		
		
		try {
			Manifest manifest = jar.getManifest();
			Pom pom = new Pom(manifest);
			if (scm != null)
				pom.setSCM(scm);
			
			for ( String d : developers)
				pom.addDeveloper(d);

			Set<String> exports = OSGiHeader.parseHeader(
					manifest.getMainAttributes().getValue(Constants.EXPORT_PACKAGE)).keySet();
			Jar javadoc = javadoc(getFile(original, "OSGI-OPT/src"), f, exports);
			if (javadoc == null)
				return;

			addClose(javadoc);

			Jar binary = new Jar("binary");
			Jar source = new Jar("source");
			addClose(binary);
			addClose(source);
			for (Map.Entry<String, Resource> entry : jar.getResources().entrySet()) {
				if (!entry.getKey().startsWith("OSGI-OPT"))
					binary.putResource(entry.getKey(), entry.getValue());
				if (entry.getKey().startsWith("OSGI-OPT/src"))
					binary.putResource(entry.getKey().substring("OSGI-OPT/src/".length()), entry
							.getValue());
			}
			String prefix = pom.getArtifactId() + "-" + pom.getVersion();
			File bundle = new File(temp, "bundle");
			bundle.mkdirs();

			File binaryFile = new File(bundle, prefix + ".jar");
			File sourceFile = new File(bundle, prefix + "-sources.jar");
			File javadocFile = new File(bundle, prefix + "-javadoc.jar");
			File pomFile = new File(bundle, "pom.xml").getAbsoluteFile();

			IO.copy(pom.openInputStream(), pomFile);

			sign(pomFile);
			binary.write(binaryFile);
			sign(binaryFile);
			source.write(sourceFile);
			sign(sourceFile);
			javadoc.write(javadocFile);
			sign(javadocFile);

			Jar jarred = new Jar(bundle);
			File jarredFile = new File(temp, "bundle.jar");
			jarred.write(jarredFile);

		} finally {
			jar.close();
		}
	}


	private void sign(File file) throws Exception {
		File asc = new File(file.getParentFile(), file.getName() + ".asc");
		asc.delete();

		Command command = new Command();
		command.setTrace();

		command.add(getProperty("gpgp", "gpg"));
		if (passphrase != null)
			command.add("--passphrase", passphrase);
		command.add("-ab", "--sign");	// not the -b!!
		command.add(file.getAbsolutePath());
		System.out.println(command);
		StringBuffer stdout = new StringBuffer();
		StringBuffer stderr = new StringBuffer();
		int result = command.execute(stdout, stderr);
		if (result != 0) {
			error("gpg signing %s failed because %s", file, "" + stdout + stderr);
		}
	}

	private Jar javadoc(File source, File binary, Set<String> exports) throws Exception {
		File tmp = new File(temp, "javadoc");
		tmp.mkdirs();

		Command command = new Command();
		command.add(getProperty("javadoc", "javadoc"));
		command.add("-quiet");
		command.add("-protected");
		command.add("-classpath");
		command.add(binary.getAbsolutePath());
		command.add("-d");
		command.add(tmp.getAbsolutePath());
		command.add("-charset");
		command.add("UTF-8");
		command.add("-sourcepath");
		command.add(source.getAbsolutePath());

		for (String packageName : exports) {
			command.add(packageName);
		}

		StringBuffer out = new StringBuffer();
		StringBuffer err = new StringBuffer();

		System.out.println(command);

		int result = command.execute(out, err);
		if (result == 0) {
			warning("Error during execution of javadoc command: %s / %s", out, err);
		}
		Jar jar = new Jar(tmp);
		addClose(jar);
		return jar;
	}
	
	
	
	
}

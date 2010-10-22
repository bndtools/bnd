package aQute.bnd.main;

import java.io.*;
import java.util.*;
import java.util.jar.*;

import aQute.lib.collections.*;
import aQute.lib.io.*;
import aQute.lib.osgi.*;
import aQute.libg.command.*;
import aQute.libg.header.*;
import aQute.libg.reporter.*;

public class Maven extends Processor {

	String		repository	= "nexus";
	String		url			= "http://oss.sonatype.org/service/local/staging/deploy/maven2";
	String		homedir;
	String		keyname;
	File		temp;
	String		passphrase;
	Reporter	reporter;

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
			deploy(args, i);
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

	void deploy(String args[], int i) throws Exception {
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

	void deploy(File f) throws Exception {
		Jar jar = new Jar(f);
		File original = getFile(temp, "original");
		original.mkdirs();
		jar.expand(original);

		try {
			Manifest manifest = jar.getManifest();
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

			File binaryFile = new File(temp, "binary.jar");
			File sourceFile = new File(temp, "source.jar");
			File javadocFile = new File(temp, "javadoc.jar");
			File pomFile = new File(temp, "pom.xml").getAbsoluteFile();

			PomResource pom = new PomResource(manifest);
			IO.copy(pom.openInputStream(), pomFile);

			binary.write(binaryFile);
			source.write(sourceFile);
			javadoc.write(javadocFile);

			maven_gpg_sign_and_deploy(binaryFile, null, pomFile);
			maven_gpg_sign_and_deploy(sourceFile, "sources", pomFile);
			maven_gpg_sign_and_deploy(javadocFile, "javadoc", pomFile);

		} finally {
			jar.close();
		}
	}

	// gpg:sign-and-deploy-file \
	// -Durl=http://oss.sonatype.org/service/local/staging/deploy/maven2
	// \
	// -DrepositoryId=sonatype-nexus-staging \
	// -DupdateReleaseInfo=true \
	// -DpomFile=pom.xml \
	// -Dfile=/Ws/bnd/biz.aQute.bndlib/tmp/biz.aQute.bndlib.jar \
	// -Dpassphrase=a1k3v3t5x3

	private void maven_gpg_sign_and_deploy(File file, String classifier, File pomFile)
			throws Exception {
		Command command = new Command();
		command.setTrace();

		command.add(getProperty("mvn", "mvn"));
		command.add("-e");
		command.add("org.apache.maven.plugins:maven-gpg-plugin:1.1:sign-and-deploy-file");
		command.add("-DreleaseInfo=true");
		command.add("-Dfile=" + file.getAbsolutePath());
		command.add("-DrepositoryId=" + repository);
		command.add("-Durl=" + url);
		command.add("-Pgpg");
		optional(command, "gpg.passphrase", passphrase);
		optional(command, "keyname", keyname);
		optional(command, "homedir", homedir);
		optional(command, "classifier", classifier);
		optional(command, "pomFile", pomFile == null ? null : pomFile.getAbsolutePath());

		StringBuffer stdout = new StringBuffer();
		StringBuffer stderr = new StringBuffer();

		System.out.println(command);

		int result = command.execute(stdout, stderr);
		if (result != 0) {
			error("Maven deploy to %s failed to sign and transfer %s because %s", repository, file,
					"" + stdout + stderr);
		}
	}

	private void optional(Command command, String key, String value) {
		if (value == null)
			return;

		command.add("-D" + key + "=" + value);
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

package aQute.bnd.repository.maven.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.Manifest;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Domain;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.version.Version;
import aQute.lib.fileset.FileSet;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.lib.tag.Tag;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.command.Command;

public class Tool extends Processor {

	private static final String	OSGI_OPT_SRC	= "OSGI-OPT/src/";
	private final File			tmp;
	private final File			sources;
	private final File			javadoc;
	private final Domain		manifest;

	public Tool(Processor parent, Jar jar) throws Exception {
		super(parent);
		tmp = Files.createTempDirectory("tool").toFile();
		sources = new File(tmp, "sources");
		javadoc = new File(tmp, "javadoc");
		manifest = Domain.domain(jar.getManifest());

		for (Entry<String,Resource> e : jar.getResources().entrySet()) {
			if (e.getKey().startsWith(OSGI_OPT_SRC)) {
				String path = e.getKey().substring(OSGI_OPT_SRC.length());
				File out = IO.getFile(sources, path);
				out.getParentFile().mkdirs();
				IO.copy(e.getValue().openInputStream(), out);
			}
		}
	}

	public boolean hasSources() {
		return sources.isDirectory();
	}

	public Jar doJavadoc(Map<String,String> options, boolean exportsOnly) throws Exception {

		if (!hasSources())
			return new Jar("empty");

		Command command = new Command();
		command.add(getProperty("javadoc", "javadoc"));
		command.add("-quiet");
		command.add("-protected");
		// command.add("-classpath");
		// command.add(binary.getAbsolutePath());
		command.add("-d");
		command.add(javadoc.getAbsolutePath());
		command.add("-charset");
		command.add("UTF-8");
		command.add("-sourcepath");
		command.add(sources.getAbsolutePath());

		 Properties pp = new UTF8Properties();
		 pp.putAll(options);
		
		 String name = manifest.getBundleName();
		 if (name == null)
		 name = manifest.getBundleSymbolicName().getKey();
		
		 String version = manifest.getBundleVersion();
		 if (version == null)
		 version = Version.LOWEST.toString();
		
		 String bundleDescription = manifest.getBundleDescription();
		
		 if (bundleDescription != null &&
		 !Strings.trim(bundleDescription).isEmpty()) {
		 printOverview(name, version, bundleDescription);
		 }
		
		 set(pp, "-doctitle", name);
		 set(pp, "-windowtitle", name);
		 set(pp, "-header", manifest.getBundleVendor());
		 set(pp, "-bottom", manifest.getBundleCopyright());
		 set(pp, "-footer", manifest.getBundleDocURL());
		
		 command.add("-tag");
		 command.add("Immutable:t:Immutable");
		 command.add("-tag");
		 command.add("ThreadSafe:t:ThreadSafe");
		 command.add("-tag");
		 command.add("NotThreadSafe:t:NotThreadSafe");
		 command.add("-tag");
		 command.add("GuardedBy:mf:\"Guarded By:\"");
		 command.add("-tag");
		 command.add("security:m:\"Required Permissions\"");
		 command.add("-tag");
		command.add("noimplement:t:\"Consumers of this API must not implement this interface\"");
		
		 for (Enumeration< ? > e = pp.propertyNames(); e.hasMoreElements();) {
		 String key = (String) e.nextElement();
		 String value = pp.getProperty(key);
		
		 if (key.startsWith("-")) {
		 //
		 // Allow people to add the same command multiple times
		 // by suffixing it with '.' something
		 //
		 int n = key.lastIndexOf('.');
		 if (n > 0) {
		 key = key.substring(0, n);
		 }
		
		 command.add(key);
		 command.add("\"" + value + "\"");
		 }
		 }
		
		FileSet set = new FileSet(sources, "**.java");
		for (File f : set.getFiles())
			command.add(f.getAbsolutePath());

		if (exportsOnly) {
			Parameters exports = manifest.getExportPackage();
			for (String packageName : exports.keySet()) {
				command.add(packageName);
			}
		}

		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();

		int result = command.execute(out, err);
		if (result != 0) {
			warning("Error during execution of javadoc command: %s\n******************\n%s", out, err);
		}
		Jar jar = new Jar(javadoc);
		jar.setManifest(new Manifest());
		addClose(jar);
		return jar;
	}

	void printOverview(String name, String version, String bundleDescription) throws FileNotFoundException {
		Tag body = new Tag("body");
		new Tag(body, "h1", name);
		new Tag(body, "p", "Version " + version);
		new Tag(body, "p", bundleDescription);

		Tag table = new Tag(body, "table");
		for (String key : manifest) {
			if (key.equalsIgnoreCase(Constants.BUNDLE_DESCRIPTION)
					|| key.equalsIgnoreCase(Constants.BUNDLE_VERSION))
				continue;

			Tag tr = new Tag(table, "tr");
			new Tag(tr, "td", key);
			new Tag(tr, "td", manifest.get(key));
		}

		File overview = new File(sources, "overview.html");
		try (PrintWriter pw = new PrintWriter(overview);) {
			body.print(2, pw);
		}
	}

	private void set(Properties pp, String key, String value) {
		if (value == null)
			return;

		if (pp.containsKey(key))
			return;

		pp.put(key, value);
	}

	public Jar doSource() throws IOException {
		if (!hasSources())
			return new Jar("empty");

		Jar jar = new Jar(sources);
		jar.setManifest(new Manifest());
		return jar;
	}

	public void close() throws IOException {
		try {
			super.close();
		} finally {
			IO.delete(tmp);
		}
	}

}

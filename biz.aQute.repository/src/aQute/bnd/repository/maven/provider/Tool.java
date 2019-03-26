package aQute.bnd.repository.maven.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

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
	private final File			javadocOptions;
	private final Domain		manifest;

	public Tool(Processor parent, Jar jar) throws Exception {
		super(parent);
		tmp = Files.createTempDirectory("tool")
			.toFile();
		sources = new File(tmp, "sources");
		javadoc = new File(tmp, "javadoc");
		javadocOptions = new File(tmp, "javadoc.options");
		manifest = Domain.domain(jar.getManifest());

		for (Entry<String, Resource> e : jar.getResources()
			.entrySet()) {
			if (e.getKey()
				.startsWith(OSGI_OPT_SRC)) {
				String path = e.getKey()
					.substring(OSGI_OPT_SRC.length());
				File out = IO.getFile(sources, path);
				IO.mkdirs(out.getParentFile());
				try (OutputStream os = IO.outputStream(out)) {
					e.getValue()
						.write(os);
				}
			}
		}
	}

	public boolean hasSources() {
		return sources.isDirectory();
	}

	public Jar doJavadoc(Map<String, String> options, boolean exportsOnly) throws Exception {
		if (!hasSources())
			return new Jar("javadoc");

		IO.mkdirs(javadoc);
		List<String> args = new ArrayList<>();
		args.add("-quiet");
		args.add("-protected");
		args.add(String.format("%s '%s'", "-d", fileName(javadoc)));
		args.add("-charset 'UTF-8'");
		args.add(String.format("%s '%s'", "-sourcepath", fileName(sources)));

		Properties pp = new UTF8Properties();
		pp.putAll(options);

		String name = manifest.getBundleName();
		if (name == null)
			name = manifest.getBundleSymbolicName()
				.getKey();

		String version = manifest.getBundleVersion();
		if (version == null)
			version = Version.LOWEST.toString();

		String bundleDescription = manifest.getBundleDescription();

		if (bundleDescription != null && !Strings.trim(bundleDescription)
			.isEmpty()) {
			printOverview(name, version, bundleDescription);
		}

		set(pp, "-doctitle", name);
		set(pp, "-windowtitle", name);
		set(pp, "-header", manifest.getBundleVendor());
		set(pp, "-bottom", manifest.getBundleCopyright());
		set(pp, "-footer", manifest.getBundleDocURL());

		args.add("-tag 'Immutable:t:\"Immutable\"'");
		args.add("-tag 'ThreadSafe:t:\"ThreadSafe\"'");
		args.add("-tag 'NotThreadSafe:t:\"NotThreadSafe\"'");
		args.add("-tag 'GuardedBy:mf:\"Guarded By:\"'");
		args.add("-tag 'security:m:\"Required Permissions\"'");
		args.add("-tag 'noimplement:t:\"Consumers of this API must not implement this interface\"'");

		for (Enumeration<?> e = pp.propertyNames(); e.hasMoreElements();) {
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

				args.add(String.format("%s '%s'", key, escape(value)));
			}
		}

		FileSet set = new FileSet(sources, "**.java");
		for (File f : set.getFiles()) {
			args.add(String.format("'%s'", fileName(f)));
		}

		if (exportsOnly) {
			Parameters exports = manifest.getExportPackage();
			for (String packageName : exports.keySet()) {
				args.add(String.format("'%s'", packageName));
			}
		}

		try (PrintWriter writer = IO.writer(javadocOptions)) {
			for (String arg : args) {
				writer.println(arg);
			}
		}

		Command command = new Command();
		command.add(getProperty("javadoc", "javadoc"));
		command.add("@" + fileName(javadocOptions));
		StringBuilder out = new StringBuilder();
		StringBuilder err = new StringBuilder();
		int result = command.execute(out, err);
		if (result != 0) {
			warning("Error during execution of javadoc command: %s\n******************\n%s", out, err);
		}
		return new Jar(javadoc);
	}

	private String fileName(File f) {
		String result = IO.absolutePath(f);
		return result;
	}

	private String escape(String input) {
		return input.replace("\\", "\\\\")
			.replace(System.getProperty("line.separator"), "\\" + System.getProperty("line.separator"));
	}

	void printOverview(String name, String version, String bundleDescription) throws FileNotFoundException {
		Tag body = new Tag("body");
		new Tag(body, "h1", name);
		new Tag(body, "p", "Version " + version);
		new Tag(body, "p", bundleDescription);

		Tag table = new Tag(body, "table");
		for (String key : manifest) {
			if (key.equalsIgnoreCase(Constants.BUNDLE_DESCRIPTION) || key.equalsIgnoreCase(Constants.BUNDLE_VERSION))
				continue;

			Tag tr = new Tag(table, "tr");
			new Tag(tr, "td", key);
			new Tag(tr, "td", manifest.get(key));
		}

		File overview = new File(sources, "overview.html");
		try (PrintWriter pw = new PrintWriter(overview)) {
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
			return new Jar("sources");

		return new Jar(sources);
	}

	@Override
	public void close() throws IOException {
		try {
			super.close();
		} finally {
			IO.delete(tmp);
		}
	}

}
